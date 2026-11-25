package com.campus.network.feature.map.data

import com.campus.network.feature.map.domain.ActiveNavigationSession
import com.campus.network.feature.map.domain.ArRouteStep
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.ManeuverDirection
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Shared singleton controller wrapping Mapbox Navigation SDK v3 (`MapboxNavigationApp`),
 * Mapbox Directions v5 HTTP API, and OpenStreetMap OSRM Foot/Road router for
 * walking route calculation along real paths and roads, GPS sidewalk snapping,
 * live turn-by-turn progress, and arrival detection.
 */
@Singleton
class WalkingNavigationController @Inject constructor(
    @Named("mapboxAccessToken") private val accessToken: String
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var httpRouteJob: Job? = null
    private var cachedRoadSteps: List<ArRouteStep> = emptyList()

    private val _navigationSession = MutableStateFlow<ActiveNavigationSession?>(null)
    val navigationSession: StateFlow<ActiveNavigationSession?> = _navigationSession.asStateFlow()

    private val _routingStatus = MutableStateFlow<String?>(null)
    val routingStatus: StateFlow<String?> = _routingStatus.asStateFlow()

    private val observersRegistered = AtomicBoolean(false)
    private var activeDestinationMeta: Triple<String, String, GeoCoordinate>? = null
    private var currentRouteRequestId: Long? = null

    private val routesObserver = RoutesObserver { result ->
        val primary = result.navigationRoutes.firstOrNull()
        val meta = activeDestinationMeta ?: return@RoutesObserver
        if (primary != null) {
            applyNavigationRoute(primary, meta.first, meta.second, meta.third)
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) = Unit

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhanced = locationMatcherResult.enhancedLocation
            val snapped = GeoCoordinate(
                latitude = enhanced.latitude,
                longitude = enhanced.longitude,
                altitudeMeters = enhanced.altitude
            )
            _navigationSession.update { current ->
                current?.let { session ->
                    val updatedWaypoints = if (session.isFallbackStraightLine) {
                        GeoSpatialMath.resamplePolyline(listOf(snapped, session.destination), spacingMeters = 3.0)
                    } else {
                        session.sampledArWaypoints
                    }
                    val remaining = if (session.isFallbackStraightLine) {
                        GeoSpatialMath.haversineMeters(snapped, session.destination)
                    } else {
                        session.distanceRemainingMeters
                    }
                    session.copy(
                        snappedLocation = snapped,
                        sampledArWaypoints = updatedWaypoints,
                        distanceRemainingMeters = remaining,
                        hasArrived = session.hasArrived || remaining < 9.0
                    )
                }
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { progress ->
        val meta = activeDestinationMeta ?: return@RouteProgressObserver
        val step = extractCurrentStep(progress)
        val remaining = progress.distanceRemaining.toDouble().coerceAtLeast(0.0)
        val durationSec = progress.durationRemaining.coerceAtLeast(0.0)

        val upcomingCoords = extractUpcomingCoordinates(progress, meta.third)
        _navigationSession.update { current ->
            val base = current ?: ActiveNavigationSession(
                destinationName = meta.first,
                destinationSubtitle = meta.second,
                destination = meta.third
            )
            base.copy(
                currentStep = step ?: base.currentStep,
                distanceRemainingMeters = remaining,
                durationRemainingSeconds = durationSec,
                sampledArWaypoints = if (upcomingCoords.isNotEmpty()) {
                    GeoSpatialMath.resamplePolyline(upcomingCoords, spacingMeters = 3.0, maxWaypoints = 20)
                } else {
                    base.sampledArWaypoints
                },
                hasArrived = base.hasArrived || remaining < 9.0
            )
        }
    }

    private val offRouteObserver = OffRouteObserver { offRoute ->
        _navigationSession.update { current ->
            current?.copy(isOffRoute = offRoute)
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) = Unit
        override fun onNextRouteLegStart(routeLegProgress: com.mapbox.navigation.base.trip.model.RouteLegProgress) = Unit
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            _navigationSession.update { current ->
                current?.copy(
                    hasArrived = true,
                    distanceRemainingMeters = 0.0,
                    durationRemainingSeconds = 0.0,
                    currentStep = ArRouteStep(
                        instruction = "You have arrived",
                        secondaryText = current.destinationName,
                        direction = ManeuverDirection.ARRIVE,
                        distanceMeters = 0.0,
                        maneuverCoordinate = current.destination
                    )
                )
            }
        }
    }

    fun startWalkingRoute(
        origin: GeoCoordinate,
        destination: GeoCoordinate,
        destinationName: String,
        destinationSubtitle: String
    ) {
        activeDestinationMeta = Triple(destinationName, destinationSubtitle, destination)
        cachedRoadSteps = emptyList()
        val directDist = GeoSpatialMath.haversineMeters(origin, destination)
        val initialBearing = GeoSpatialMath.bearingDegrees(origin, destination)

        // Do NOT seed a 2-point straight line across the land; keep routeGeometry empty until the
        // road-snapped polyline arrives from Mapbox Directions / OSRM (~200-400ms).
        _navigationSession.value = ActiveNavigationSession(
            destinationName = destinationName,
            destinationSubtitle = destinationSubtitle,
            destination = destination,
            routeGeometry = emptyList(),
            sampledArWaypoints = emptyList(),
            currentStep = ArRouteStep(
                instruction = "Routing to $destinationName...",
                secondaryText = "Snapping walking path to roads & walkways",
                direction = ManeuverDirection.STRAIGHT,
                distanceMeters = directDist,
                maneuverCoordinate = destination
            ),
            distanceRemainingMeters = directDist,
            durationRemainingSeconds = (directDist / 1.35).coerceAtLeast(30.0),
            snappedLocation = origin,
            isFallbackStraightLine = true
        )

        // 1. Always fetch road/footpath-snapped route via Mapbox Directions v5 + OSRM Foot/Road API
        // so the polyline follows real paths and roads even if MapboxNavigationApp is still attaching.
        httpRouteJob?.cancel()
        httpRouteJob = controllerScope.launch {
            fetchAndApplyRoadSnappedRoute(origin, destination, destinationName, destinationSubtitle)
        }

        // 2. Also attach MapboxNavigationApp v3 trip session when available for live turn-by-turn telemetry.
        val nav = runCatching { MapboxNavigationApp.current() }.getOrNull() ?: return
        ensureObserversRegistered(nav)
        cancelPendingRequest(nav)

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions(DirectionsCriteria.PROFILE_WALKING)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .coordinatesList(
                listOf(
                    Point.fromLngLat(origin.longitude, origin.latitude),
                    Point.fromLngLat(destination.longitude, destination.latitude)
                )
            )
            .steps(true)
            .bannerInstructions(true)
            .build()

        currentRouteRequestId = nav.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    currentRouteRequestId = null
                    val primary = routes.firstOrNull() ?: return
                    nav.setNavigationRoutes(routes)
                    runCatching { nav.startTripSession() }
                    applyNavigationRoute(primary, destinationName, destinationSubtitle, destination)
                    _routingStatus.value = null
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    currentRouteRequestId = null
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    currentRouteRequestId = null
                }
            }
        )
    }

    fun updateUserLocationFallback(origin: GeoCoordinate) {
        val current = _navigationSession.value ?: return
        val distToDest = GeoSpatialMath.haversineMeters(origin, current.destination)

        val geom = current.routeGeometry
        if (!current.isFallbackStraightLine && geom.size >= 2) {
            // Find closest vertex on the road-snapped polyline to the user's real GPS position
            var closestIdx = 0
            var minVertexDist = Double.MAX_VALUE
            for (i in geom.indices) {
                val d = GeoSpatialMath.haversineMeters(origin, geom[i])
                if (d < minVertexDist) {
                    minVertexDist = d
                    closestIdx = i
                }
            }

            // If user deviated > 55m from the road polyline, re-calculate road route from current GPS
            if (minVertexDist > 55.0 && httpRouteJob?.isActive != true) {
                httpRouteJob = controllerScope.launch {
                    fetchAndApplyRoadSnappedRoute(
                        origin = origin,
                        destination = current.destination,
                        destinationName = current.destinationName,
                        destinationSubtitle = current.destinationSubtitle
                    )
                }
                return
            }

            val remainingPath = listOf(origin) + geom.subList(
                (closestIdx + 1).coerceAtMost(geom.lastIndex),
                geom.size
            )
            var remainingRoadMeters = 0.0
            for (i in 0 until remainingPath.lastIndex) {
                remainingRoadMeters += GeoSpatialMath.haversineMeters(remainingPath[i], remainingPath[i + 1])
            }

            val upcomingStep = cachedRoadSteps.firstOrNull { step ->
                val mc = step.maneuverCoordinate ?: return@firstOrNull false
                GeoSpatialMath.haversineMeters(origin, mc) > 8.0
            } ?: current.currentStep

            val stepDist = upcomingStep?.maneuverCoordinate?.let {
                GeoSpatialMath.haversineMeters(origin, it)
            } ?: remainingRoadMeters

            _navigationSession.value = current.copy(
                sampledArWaypoints = GeoSpatialMath.resamplePolyline(
                    remainingPath,
                    spacingMeters = 3.0,
                    maxWaypoints = 20
                ),
                distanceRemainingMeters = remainingRoadMeters,
                durationRemainingSeconds = (remainingRoadMeters / 1.35).coerceAtLeast(15.0),
                hasArrived = distToDest < 10.0,
                snappedLocation = geom[closestIdx],
                currentStep = upcomingStep?.copy(distanceMeters = stepDist)
            )
        }
    }

    fun stopNavigation() {
        httpRouteJob?.cancel()
        httpRouteJob = null
        cachedRoadSteps = emptyList()
        activeDestinationMeta = null
        _routingStatus.value = null
        val nav = runCatching { MapboxNavigationApp.current() }.getOrNull()
        if (nav != null) {
            cancelPendingRequest(nav)
            runCatching {
                nav.setNavigationRoutes(emptyList())
                nav.stopTripSession()
            }
            unregisterObservers(nav)
        }
        _navigationSession.value = null
    }

    private fun fetchAndApplyRoadSnappedRoute(
        origin: GeoCoordinate,
        destination: GeoCoordinate,
        destinationName: String,
        destinationSubtitle: String
    ) {
        val coordPair = String.format(
            Locale.US,
            "%.6f,%.6f;%.6f,%.6f",
            origin.longitude,
            origin.latitude,
            destination.longitude,
            destination.latitude
        )

        val candidates = ArrayList<String>()
        if (accessToken.isNotBlank()) {
            candidates.add(
                "https://api.mapbox.com/directions/v5/mapbox/walking/$coordPair" +
                    "?alternatives=false&continue_straight=false&geometries=polyline6&overview=full&steps=true&access_token=$accessToken"
            )
        }
        candidates.add(
            "https://router.project-osrm.org/route/v1/foot/$coordPair?overview=full&geometries=polyline6&steps=true"
        )
        if (accessToken.isNotBlank()) {
            candidates.add(
                "https://api.mapbox.com/directions/v5/mapbox/driving/$coordPair" +
                    "?alternatives=false&geometries=polyline6&overview=full&steps=true&access_token=$accessToken"
            )
        }
        candidates.add(
            "https://router.project-osrm.org/route/v1/driving/$coordPair?overview=full&geometries=polyline6&steps=true"
        )

        var bestCoords: List<GeoCoordinate> = emptyList()
        var bestSteps: List<ArRouteStep> = emptyList()
        var bestDistance = 0.0
        var bestDuration = 0.0

        for (url in candidates) {
            val parsed = runCatching { requestDirectionsJson(url, destinationName) }.getOrNull() ?: continue
            val (coords, steps, distDur) = parsed
            // Prefer a route that has real intermediate road/path vertices (> 2 points)
            if (coords.size > bestCoords.size) {
                bestCoords = coords
                bestSteps = steps
                bestDistance = distDur.first
                bestDuration = distDur.second
            }
            if (bestCoords.size >= 4) break
        }

        if (bestCoords.size >= 2) {
            cachedRoadSteps = bestSteps
            val firstStep = bestSteps.firstOrNull() ?: ArRouteStep(
                instruction = "Follow path toward $destinationName",
                secondaryText = destinationSubtitle,
                direction = ManeuverDirection.STRAIGHT,
                distanceMeters = bestDistance,
                maneuverCoordinate = bestCoords.getOrNull(1) ?: destination
            )

            _navigationSession.update { prev ->
                // Do not overwrite if MapboxNavigation SDK already supplied an even denser route
                if (prev != null && !prev.isFallbackStraightLine && prev.routeGeometry.size > bestCoords.size) {
                    prev
                } else {
                    ActiveNavigationSession(
                        destinationName = destinationName,
                        destinationSubtitle = destinationSubtitle,
                        destination = destination,
                        routeGeometry = bestCoords,
                        sampledArWaypoints = GeoSpatialMath.resamplePolyline(
                            bestCoords,
                            spacingMeters = 3.0,
                            maxWaypoints = 20
                        ),
                        currentStep = firstStep,
                        distanceRemainingMeters = bestDistance,
                        durationRemainingSeconds = bestDuration,
                        hasArrived = bestDistance < 9.0,
                        isOffRoute = false,
                        snappedLocation = bestCoords.firstOrNull() ?: origin,
                        isFallbackStraightLine = false
                    )
                }
            }
        }
    }

    private fun requestDirectionsJson(
        urlStr: String,
        destinationName: String
    ): Triple<List<GeoCoordinate>, List<ArRouteStep>, Pair<Double, Double>>? {
        val conn = (URL(urlStr).openConnection() as? HttpURLConnection) ?: return null
        val body = try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 7_000
            conn.readTimeout = 7_000
            conn.setRequestProperty("User-Agent", "CampusNetworkAndroid/1.0")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        } ?: return null

        val root = JSONObject(body)
        val routes = root.optJSONArray("routes") ?: return null
        val primary = routes.optJSONObject(0) ?: return null
        val encodedGeom = primary.optString("geometry")
        val fullCoords = ArrayList<GeoCoordinate>()
        if (encodedGeom.isNotBlank()) {
            LineString.fromPolyline(encodedGeom, Constants.PRECISION_6)
                .coordinates()
                .forEach { pt -> fullCoords.add(GeoCoordinate(pt.latitude(), pt.longitude())) }
        }

        val parsedSteps = ArrayList<ArRouteStep>()
        val legs = primary.optJSONArray("legs")
        val firstLeg = legs?.optJSONObject(0)
        val stepsArr = firstLeg?.optJSONArray("steps")
        if (stepsArr != null) {
            for (i in 0 until stepsArr.length()) {
                val stepObj = stepsArr.optJSONObject(i) ?: continue
                val stepGeom = stepObj.optString("geometry")
                if (fullCoords.size < 3 && stepGeom.isNotBlank()) {
                    LineString.fromPolyline(stepGeom, Constants.PRECISION_6)
                        .coordinates()
                        .forEach { pt -> fullCoords.add(GeoCoordinate(pt.latitude(), pt.longitude())) }
                }

                val maneuver = stepObj.optJSONObject("maneuver")
                val type = maneuver?.optString("type")?.takeIf { it.isNotBlank() }
                val modifier = maneuver?.optString("modifier")?.takeIf { it.isNotBlank() }
                val streetName = stepObj.optString("name").trim()
                val rawInstruction = maneuver?.optString("instruction")?.trim().orEmpty()
                val stepDist = stepObj.optDouble("distance", 0.0)
                val locArr = maneuver?.optJSONArray("location")
                val maneuverCoord = if (locArr != null && locArr.length() >= 2) {
                    GeoCoordinate(latitude = locArr.optDouble(1), longitude = locArr.optDouble(0))
                } else {
                    null
                }

                val direction = GeoSpatialMath.maneuverFromString(modifier, type)
                val instruction = when {
                    rawInstruction.isNotBlank() -> rawInstruction
                    type == "arrive" -> "Arrive at $destinationName"
                    streetName.isNotBlank() && modifier != null -> "Turn $modifier onto $streetName"
                    streetName.isNotBlank() -> "Continue along $streetName"
                    modifier != null -> "Turn $modifier"
                    else -> "Continue on walkway"
                }

                if (stepDist > 1.0 || type == "arrive") {
                    parsedSteps.add(
                        ArRouteStep(
                            instruction = instruction,
                            secondaryText = streetName.takeIf { it.isNotBlank() },
                            direction = direction,
                            distanceMeters = stepDist,
                            maneuverCoordinate = maneuverCoord
                        )
                    )
                }
            }
        }

        if (fullCoords.size < 2) return null
        val totalDistance = primary.optDouble("distance", 0.0).coerceAtLeast(1.0)
        val rawDuration = primary.optDouble("duration", 0.0)
        // Ensure walking pace (~1.35 m/s) even if driving profile fallback was used for rural road geometry
        val walkingDuration = (totalDistance / 1.35).coerceAtLeast(rawDuration)
        return Triple(fullCoords, parsedSteps, totalDistance to walkingDuration)
    }

    private fun applyNavigationRoute(
        route: NavigationRoute,
        destinationName: String,
        destinationSubtitle: String,
        destination: GeoCoordinate
    ) {
        val encoded = route.directionsRoute.geometry()
        val coords = if (!encoded.isNullOrBlank()) {
            LineString.fromPolyline(encoded, Constants.PRECISION_6)
                .coordinates()
                .map { GeoCoordinate(it.latitude(), it.longitude()) }
        } else {
            emptyList()
        }
        val firstStep = route.directionsRoute.legs()?.firstOrNull()?.steps()?.firstOrNull()
        val maneuver = firstStep?.maneuver()
        val firstCoord = maneuver?.location()?.let { GeoCoordinate(it.latitude(), it.longitude()) }
        val stepModel = ArRouteStep(
            instruction = maneuver?.instruction() ?: "Walk toward $destinationName",
            secondaryText = firstStep?.name()?.takeIf { it.isNotBlank() },
            direction = GeoSpatialMath.maneuverFromString(maneuver?.modifier(), maneuver?.type()),
            distanceMeters = firstStep?.distance() ?: route.directionsRoute.distance(),
            maneuverCoordinate = firstCoord
        )

        val totalDist = route.directionsRoute.distance()
        val totalDur = route.directionsRoute.duration()
        _navigationSession.update { prev ->
            ActiveNavigationSession(
                destinationName = destinationName,
                destinationSubtitle = destinationSubtitle,
                destination = destination,
                routeGeometry = coords.ifEmpty { prev?.routeGeometry.orEmpty() },
                sampledArWaypoints = GeoSpatialMath.resamplePolyline(
                    coords.ifEmpty { prev?.routeGeometry.orEmpty() },
                    spacingMeters = 3.0,
                    maxWaypoints = 20
                ),
                currentStep = stepModel,
                distanceRemainingMeters = totalDist,
                durationRemainingSeconds = totalDur,
                hasArrived = totalDist < 9.0,
                isOffRoute = false,
                snappedLocation = prev?.snappedLocation,
                isFallbackStraightLine = coords.isEmpty()
            )
        }
    }

    private fun extractCurrentStep(progress: RouteProgress): ArRouteStep? {
        val banner = progress.bannerInstructions?.primary()
        val stepProgress = progress.currentLegProgress?.currentStepProgress
        val step = stepProgress?.step
        val maneuver = step?.maneuver()
        val stepRemaining = stepProgress?.distanceRemaining?.toDouble()
            ?: banner?.let { progress.currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble() }
            ?: progress.distanceRemaining.toDouble()

        val text = banner?.text()
            ?: maneuver?.instruction()
            ?: return null
        val secondary = progress.bannerInstructions?.secondary()?.text()
            ?: step?.name()?.takeIf { it.isNotBlank() }
        val direction = GeoSpatialMath.maneuverFromString(
            banner?.modifier() ?: maneuver?.modifier(),
            banner?.type() ?: maneuver?.type()
        )
        val maneuverCoord = maneuver?.location()?.let { GeoCoordinate(it.latitude(), it.longitude()) }
        return ArRouteStep(
            instruction = text,
            secondaryText = secondary,
            direction = direction,
            distanceMeters = stepRemaining.coerceAtLeast(0.0),
            maneuverCoordinate = maneuverCoord
        )
    }

    private fun extractUpcomingCoordinates(
        progress: RouteProgress,
        destination: GeoCoordinate
    ): List<GeoCoordinate> {
        val leg = progress.currentLegProgress ?: return emptyList()
        val steps = leg.routeLeg?.steps().orEmpty()
        val currentIdx = leg.currentStepProgress?.stepIndex ?: 0
        if (steps.isEmpty() || currentIdx !in steps.indices) return emptyList()

        val points = ArrayList<GeoCoordinate>()
        for (idx in currentIdx..steps.lastIndex) {
            val geom = steps[idx].geometry() ?: continue
            val decoded = LineString.fromPolyline(geom, Constants.PRECISION_6).coordinates()
            decoded.forEach { pt ->
                points.add(GeoCoordinate(pt.latitude(), pt.longitude()))
            }
            if (points.size >= 40) break
        }
        if (points.isNotEmpty()) {
            points.add(destination)
        }
        return points
    }

    private fun ensureObserversRegistered(nav: MapboxNavigation) {
        if (observersRegistered.compareAndSet(false, true)) {
            nav.registerRoutesObserver(routesObserver)
            nav.registerLocationObserver(locationObserver)
            nav.registerRouteProgressObserver(routeProgressObserver)
            nav.registerOffRouteObserver(offRouteObserver)
            nav.registerArrivalObserver(arrivalObserver)
        }
    }

    private fun unregisterObservers(nav: MapboxNavigation) {
        if (observersRegistered.compareAndSet(true, false)) {
            runCatching { nav.unregisterRoutesObserver(routesObserver) }
            runCatching { nav.unregisterLocationObserver(locationObserver) }
            runCatching { nav.unregisterRouteProgressObserver(routeProgressObserver) }
            runCatching { nav.unregisterOffRouteObserver(offRouteObserver) }
            runCatching { nav.unregisterArrivalObserver(arrivalObserver) }
        }
    }

    private fun cancelPendingRequest(nav: MapboxNavigation) {
        currentRouteRequestId?.let { id ->
            runCatching { nav.cancelRouteRequest(id) }
            currentRouteRequestId = null
        }
    }
}
