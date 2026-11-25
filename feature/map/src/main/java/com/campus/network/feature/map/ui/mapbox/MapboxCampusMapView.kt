package com.campus.network.feature.map.ui.mapbox

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.FriendLiveLocation
import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.ui.components.CampusStudentAvatar
import com.campus.network.feature.map.components.PlaceMarkerCallout
import com.campus.network.feature.map.domain.ActiveNavigationSession
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Interactive 2D/3D Mapbox Vector Map powered by `MapboxStandardStyle` (with 3D buildings),
 * active walking route glow polylines, live location puck, and all Campus ViewAnnotations.
 */
@Composable
fun MapboxCampusMapView(
    places: List<CampusPlace>,
    liveMeets: List<FlashMeet>,
    liveFriendLocations: List<FriendLiveLocation>,
    selectedFriendUserId: String? = null,
    activeNavigation: ActiveNavigationSession?,
    isMyLocationMode: Boolean,
    hasLocationPermission: Boolean,
    viewportState: MapViewportState,
    onMapLoaded: () -> Unit,
    onCameraCenterIdle: (GeoCoordinate) -> Unit,
    onMapClick: (GeoCoordinate) -> Unit,
    onPlaceClick: (CampusPlace) -> Unit,
    onMeetClick: (FlashMeet) -> Unit,
    onFriendClick: (FriendLiveLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnMapClick by rememberUpdatedState(onMapClick)
    val latestOnIdle by rememberUpdatedState(onCameraCenterIdle)
    val latestOnMapLoaded by rememberUpdatedState(onMapLoaded)
    val latestMyLocationMode by rememberUpdatedState(isMyLocationMode)

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewportState,
        style = { MapboxStandardStyle() }
    ) {
        MapEffect(Unit) { mapView ->
            mapView.mapboxMap.subscribeMapLoaded {
                latestOnMapLoaded()
            }
            mapView.mapboxMap.addOnMapClickListener { point ->
                latestOnMapClick(GeoCoordinate(point.latitude(), point.longitude()))
                true
            }
            mapView.mapboxMap.addOnMoveListener(object : com.mapbox.maps.plugin.gestures.OnMoveListener {
                override fun onMoveBegin(detector: com.mapbox.android.gestures.MoveGestureDetector) = Unit
                override fun onMove(detector: com.mapbox.android.gestures.MoveGestureDetector): Boolean = false
                override fun onMoveEnd(detector: com.mapbox.android.gestures.MoveGestureDetector) {
                    if (latestMyLocationMode) {
                        val center = mapView.mapboxMap.cameraState.center
                        latestOnIdle(GeoCoordinate(center.latitude(), center.longitude()))
                    }
                }
            })
        }

        MapEffect(hasLocationPermission) { mapView ->
            mapView.location.updateSettings {
                enabled = hasLocationPermission
                locationPuck = createDefault2DPuck(withBearing = true)
                puckBearing = PuckBearing.COURSE
                puckBearingEnabled = true
                pulsingEnabled = true
            }
        }

        // Active Walking Route Corridor (Hyper AR dotted royal-blue waypoint trail)
        if (activeNavigation != null && activeNavigation.routeGeometry.size >= 2) {
            val dottedWaypoints = GeoSpatialMath.resamplePolyline(
                points = activeNavigation.routeGeometry,
                spacingMeters = 4.5,
                maxWaypoints = 90
            ).map { Point.fromLngLat(it.longitude, it.latitude) }

            WalkingRouteGlowPolyline(
                points = dottedWaypoints,
                glowWidth = 10.0,
                coreWidth = 4.5
            )

            ViewAnnotation(
                options = viewAnnotationOptions {
                    geometry(
                        Point.fromLngLat(
                            activeNavigation.destination.longitude,
                            activeNavigation.destination.latitude
                        )
                    )
                    annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                    allowOverlap(true)
                }
            ) {
                DestinationBeaconMarker(title = activeNavigation.destinationName)
            }
        }

        if (!isMyLocationMode) {
            // 1. Discoverable Campus Places
            places.forEach { place ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(place.longitude, place.latitude))
                        annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                        allowOverlap(true)
                    }
                ) {
                    PlaceMarkerCallout(
                        place = place,
                        onClick = { onPlaceClick(place) }
                    )
                }
            }

            // 2. Active Flash Meets
            liveMeets.forEach { meet ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(meet.longitude, meet.latitude))
                        annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                        allowOverlap(true)
                    }
                ) {
                    FlashMeetMapMarker(
                        meet = meet,
                        onClick = { onMeetClick(meet) }
                    )
                }
            }

            // 3. Live Friend Locations (1:1 Pinterest / Zenly circular rotating orbital duration dial & live km/h speedometer)
            val groupedPeers = remember(liveFriendLocations) {
                groupNearbyFriendLocations(liveFriendLocations)
            }
            liveFriendLocations.forEach { friendLoc ->
                val isFocused = !selectedFriendUserId.isNullOrBlank() &&
                    friendLoc.userId.equals(selectedFriendUserId, ignoreCase = true)
                val clusterCount = groupedPeers[friendLoc.userId] ?: 1

                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(friendLoc.longitude, friendLoc.latitude))
                        annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                        allowOverlap(true)
                        selected(isFocused)
                    }
                ) {
                    ZenlyFriendMapMarker(
                        friendLocation = friendLoc,
                        isFocused = isFocused,
                        coLocatedCount = clusterCount,
                        onClick = { onFriendClick(friendLoc) }
                    )
                }
            }
        }
    }
}

/**
 * Compact non-scrolling Mapbox card used inside the AR HUD overview arc and EventDetailScreen.
 */
@Composable
fun MapboxMiniPreviewMap(
    center: GeoCoordinate,
    markerTitle: String? = null,
    routeGeometry: List<GeoCoordinate> = emptyList(),
    userLocation: GeoCoordinate? = null,
    zoomLevel: Double = 16.0,
    onMapTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val latestOnTap by rememberUpdatedState(onMapTap)
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(center.longitude, center.latitude))
            zoom(zoomLevel)
            pitch(35.0)
        }
    }

    LaunchedEffect(center.latitude, center.longitude, zoomLevel) {
        viewportState.flyTo(
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(center.longitude, center.latitude))
                .zoom(zoomLevel)
                .pitch(35.0)
                .build(),
            animationOptions = MapAnimationOptions.Builder()
                .duration(600L)
                .build()
        )
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewportState,
        style = { MapboxStandardStyle() }
    ) {
        MapEffect(Unit) { mapView ->
            mapView.mapboxMap.addOnMapClickListener {
                latestOnTap?.invoke()
                latestOnTap != null
            }
        }

        MapEffect(userLocation != null) { mapView ->
            if (userLocation != null) {
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearing = PuckBearing.COURSE
                    puckBearingEnabled = true
                }
            }
        }

        if (routeGeometry.size >= 2) {
            val pts = routeGeometry.map { Point.fromLngLat(it.longitude, it.latitude) }
            WalkingRouteGlowPolyline(
                points = pts,
                glowWidth = 10.0,
                coreWidth = 5.0
            )
        }

        ViewAnnotation(
            options = viewAnnotationOptions {
                geometry(Point.fromLngLat(center.longitude, center.latitude))
                annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                allowOverlap(true)
            }
        ) {
            DestinationBeaconMarker(title = markerTitle ?: "Target")
        }
    }
}

/**
 * Interactive venue picker built on Mapbox Compose for CreateEventDrawerSheet.
 */
@Composable
fun MapboxVenuePickerMap(
    selectedLat: Double,
    selectedLng: Double,
    selectedTitle: String,
    presetVenues: List<Triple<String, Double, Double>>,
    viewportState: MapViewportState,
    onMapClick: (Double, Double) -> Unit,
    onPresetPinClick: (String, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnClick by rememberUpdatedState(onMapClick)

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewportState,
        style = { MapboxStandardStyle() }
    ) {
        MapEffect(Unit) { mapView ->
            mapView.mapboxMap.addOnMapClickListener { point ->
                latestOnClick(point.latitude(), point.longitude())
                true
            }
        }

        // Primary selected venue pin
        ViewAnnotation(
            options = viewAnnotationOptions {
                geometry(Point.fromLngLat(selectedLng, selectedLat))
                annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                allowOverlap(true)
            }
        ) {
            DestinationBeaconMarker(title = selectedTitle)
        }

        // Other campus landmark quick pins
        presetVenues.forEach { (name, lat, lng) ->
            if (lat != selectedLat || lng != selectedLng) {
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(lng, lat))
                        annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                        allowOverlap(true)
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable { onPresetPinClick(name, lat, lng) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = name,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = name,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkingRouteGlowPolyline(
    points: List<Point>,
    glowWidth: Double,
    coreWidth: Double
) {
    // Subtle soft path guide + crisp royal-blue route dots matching Hyper AR 2D map
    PolylineAnnotation(points = points) {
        lineJoin = LineJoin.ROUND
        lineColor = Color(0x281D63DC)
        lineWidth = glowWidth * 0.5
    }
    points.forEach { pt ->
        CircleAnnotation(point = pt) {
            circleRadius = coreWidth
            circleColor = Color(0xFF1D63DC)
            circleStrokeWidth = 1.2
            circleStrokeColor = Color.White
        }
    }
}

@Composable
private fun FlashMeetMapMarker(
    meet: FlashMeet,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
        shadowElevation = 6.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = meet.title,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = meet.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${meet.attendeeCount}/${meet.maxAttendees} · ${meet.locationName}",
                    fontSize = 9.sp,
                    color = Color(0xFF7DD3FC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DestinationBeaconMarker(title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0A66FF),
            border = BorderStroke(1.5.dp, Color.White),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF))
        )
    }
}

/**
 * 1:1 Pinterest / Zenly Live Friend Profile Display on the 2D Map (`Pinterest video #89649848826823148`):
 * - Translucent frosted circular halo with a fine outer hairline border & subtle bottom tick mark (`|`).
 * - Rotating curved orbital duration wording around the halo ring:
 *     - Primary bold black arc (`#111318`) showing how long the friend has been at that spot (e.g. `"1 HR, 3 MIN"`, `"2 HR, 54 MIN"`, `"14 MIN"`).
 *     - Two ghostly pale grey echo arcs (`#111318` at 24% alpha) separated by `•` dots completing the 360° orbit.
 * - Moving speedometer mode (`speedKmh >= 2.0f`):
 *     - Large bold italic speed number (`"4"`, `"5"`, `"6"`, `"7"`) + `"km/h"` floating directly above the circular frame.
 *     - Concentric expanding sonar ripples around the circular frame while moving.
 * - Inner circular portrait frame with crisp white border, warm golden-orange or clean white backdrop,
 *   and bottom-left activity badge pill (`💻`, `🏠`, `🎓`, or co-located count `💻 21`).
 */
@Composable
fun ZenlyFriendMapMarker(
    friendLocation: FriendLiveLocation,
    isFocused: Boolean = false,
    coLocatedCount: Int = 1,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedKmh = friendLocation.speedKmh ?: 0f
    val isMoving = speedKmh >= 2.0f
    val durationLabel = remember(
        friendLocation.stationarySinceEpochMs,
        friendLocation.updatedAtEpochMs,
        friendLocation.expiresAtEpochMs
    ) {
        formatZenlyOrbitalDuration(
            stationarySinceEpochMs = friendLocation.stationarySinceEpochMs,
            updatedAtEpochMs = friendLocation.updatedAtEpochMs,
            expiresAtEpochMs = friendLocation.expiresAtEpochMs
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ZenlyFriendOrbit_${friendLocation.userId}")
    val orbitAngleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngleDeg"
    )
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RipplePhase"
    )

    val outerDiameter: Dp = if (isFocused) 114.dp else 88.dp
    val avatarDiameter: Dp = if (isFocused) 72.dp else 54.dp

    val usesWarmGoldBackdrop = remember(friendLocation.userId) {
        abs(friendLocation.userId.hashCode()) % 2 == 0
    }
    val avatarBackdropBrush = if (usesWarmGoldBackdrop) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFB340), Color(0xFFF5881F))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        // 1. Live Moving Speedometer Readout above the circular frame ("4 km/h", "6 km/h", "7 km/h")
        if (isMoving) {
            val displaySpeed = speedKmh.roundToInt().coerceAtLeast(2)
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.offset(y = 4.dp)
            ) {
                Text(
                    text = displaySpeed.toString(),
                    color = Color(0xFF111318),
                    fontSize = if (isFocused) 34.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    lineHeight = if (isFocused) 34.sp else 24.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "km/h",
                    color = Color(0xFF111318),
                    fontSize = if (isFocused) 14.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = if (isFocused) 4.dp else 2.dp)
                )
            }
        }

        // 2. Circular Frosted Orbital Halo + Inner Portrait Disc + Corner Activity Badge
        Box(
            modifier = Modifier.size(outerDiameter),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerRadius = size.minDimension / 2f - 2.dp.toPx()
                val innerAvatarRadius = (avatarDiameter.toPx()) / 2f

                // A. Translucent frosted glass annular halo ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.62f),
                    radius = outerRadius,
                    center = Offset(cx, cy)
                )

                // B. Outer crisp hairline ring border
                drawCircle(
                    color = Color(0xFF94A3B8).copy(alpha = 0.55f),
                    radius = outerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.1.dp.toPx())
                )

                // C. Inner subtle ring border around the portrait
                drawCircle(
                    color = Color(0xFFCBD5E1).copy(alpha = 0.65f),
                    radius = innerAvatarRadius + 2.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )

                // D. Bottom vertical calibration tick mark ("|") at 6 o'clock (exact match to frames 01-25 & 70-95)
                drawLine(
                    color = Color(0xFF475569).copy(alpha = 0.65f),
                    start = Offset(cx, cy + innerAvatarRadius + 3.dp.toPx()),
                    end = Offset(cx, cy + outerRadius + 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (isMoving) {
                    // E1. Moving state: concentric sonar ripples inside/around the frosted halo (frames 65-95)
                    val ringSpan = outerRadius - innerAvatarRadius
                    for (waveIndex in 0..1) {
                        val phase = (ripplePhase + waveIndex * 0.5f) % 1f
                        val r = innerAvatarRadius + ringSpan * phase
                        val alpha = (1f - phase).coerceIn(0f, 1f) * 0.55f
                        drawCircle(
                            color = Color(0xFF64748B).copy(alpha = alpha),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
                } else {
                    // E2. Stationary state: rotating curved duration text ("1 HR, 3 MIN") + 2 ghostly echo arcs + dots
                    val textRadius = (innerAvatarRadius + outerRadius) / 2f - 3.2.dp.toPx()
                    val textSizePx = if (isFocused) 10.5.dp.toPx() else 8.4.dp.toPx()

                    val circlePath = AndroidPath().apply {
                        addCircle(cx, cy, textRadius, AndroidPath.Direction.CW)
                    }

                    val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(235, 17, 19, 24)
                        textSize = textSizePx
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        letterSpacing = 0.08f
                        textAlign = Paint.Align.CENTER
                    }

                    val ghostPaint = Paint(primaryPaint).apply {
                        color = android.graphics.Color.argb(58, 17, 19, 24)
                    }

                    // 3 segments around 360 degrees (0°, 120°, 240°): 1 primary bold black + 2 ghost echoes
                    val segments = listOf(
                        0f to primaryPaint,
                        120f to ghostPaint,
                        240f to ghostPaint
                    )

                    segments.forEach { (offsetDeg, paint) ->
                        rotate(degrees = orbitAngleDeg + offsetDeg, pivot = Offset(cx, cy)) {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawTextOnPath(
                                    durationLabel,
                                    circlePath,
                                    0f,
                                    textSizePx * 0.36f,
                                    paint
                                )
                            }
                        }
                        // Separator dot "•" halfway between each 120° text arc (+60°)
                        rotate(degrees = orbitAngleDeg + offsetDeg + 60f, pivot = Offset(cx, cy)) {
                            drawCircle(
                                color = Color(0xFF111318).copy(alpha = 0.24f),
                                radius = 1.4.dp.toPx(),
                                center = Offset(cx, cy - textRadius)
                            )
                        }
                    }
                }
            }

            // 3. Central Circular Friend Portrait with Crisp White Rim & Warm Golden/White Backdrop
            Box(
                modifier = Modifier
                    .size(avatarDiameter)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(avatarBackdropBrush)
                    .border(2.2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CampusStudentAvatar(
                    avatarUrl = friendLocation.avatarUrl,
                    fullName = friendLocation.displayName,
                    backgroundColor = Color.Transparent,
                    initialsColor = if (usesWarmGoldBackdrop) Color(0xFF111318) else Color(0xFF0F172A),
                    fontSize = if (isFocused) 20.sp else 15.sp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 4. Bottom-Left Activity / Status Badge Pill (matching purple laptop / pink home badges in frames 30-50)
            val isPinkBadge = remember(friendLocation.userId) {
                abs(friendLocation.userId.hashCode()) % 3 == 1
            }
            val badgeBgColor = when {
                friendLocation.privacyScope == LocationPrivacyScope.ESCROW_COUNTERPARTY_ONLY -> Color(0xFF7C3AED)
                isPinkBadge -> Color(0xFFEC4899)
                else -> Color(0xFF7C3AED)
            }
            val badgeIcon = when {
                isPinkBadge -> Icons.Default.Home
                abs(friendLocation.userId.hashCode()) % 3 == 2 -> Icons.Default.School
                else -> Icons.Default.LaptopMac
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = if (isFocused) 14.dp else 10.dp,
                        y = if (isFocused) (-14).dp else (-10).dp
                    )
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(badgeBgColor)
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(
                        horizontal = if (coLocatedCount > 1) 6.dp else 4.dp,
                        vertical = 3.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = friendLocation.displayName,
                        tint = Color.White,
                        modifier = Modifier.size(if (isFocused) 12.dp else 10.dp)
                    )
                    if (coLocatedCount > 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = coLocatedCount.toString(),
                            color = Color.White,
                            fontSize = if (isFocused) 10.sp else 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats how long a friend has been at their shared location in the exact uppercase wording
 * from the Pinterest reference video:
 *  - `"1 HR, 3 MIN"`
 *  - `"2 HR, 54 MIN"`
 *  - `"2 HR, 32 MIN"`
 *  - `"14 MIN"`
 *  - `"JUST NOW"`
 */
internal fun formatZenlyOrbitalDuration(
    stationarySinceEpochMs: Long,
    updatedAtEpochMs: Long,
    expiresAtEpochMs: Long,
    nowEpochMs: Long = System.currentTimeMillis()
): String {
    val anchorMs = when {
        stationarySinceEpochMs in 1L..nowEpochMs -> stationarySinceEpochMs
        expiresAtEpochMs > nowEpochMs -> (expiresAtEpochMs - 30L * 60_000L).coerceAtMost(nowEpochMs)
        updatedAtEpochMs in 1L..nowEpochMs -> updatedAtEpochMs
        else -> nowEpochMs
    }
    val totalMinutes = ((nowEpochMs - anchorMs) / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60L
    val mins = totalMinutes % 60L
    return when {
        hours >= 1L && mins > 0L -> "$hours HR, $mins MIN"
        hours >= 1L -> "$hours HR, 0 MIN"
        else -> "$mins MIN"
    }
}

/**
 * Counts how many friends are co-located within ~45 meters of each friend so the purple pill
 * can display the group count (e.g. `💻 21` in frame 30) when peers gather at the same spot.
 */
private fun groupNearbyFriendLocations(locations: List<FriendLiveLocation>): Map<String, Int> {
    if (locations.size <= 1) return locations.associate { it.userId to 1 }
    return locations.associate { target ->
        val count = locations.count { other ->
            val dist = GeoSpatialMath.haversineMeters(
                GeoCoordinate(target.latitude, target.longitude),
                GeoCoordinate(other.latitude, other.longitude)
            )
            dist <= 45.0
        }.coerceAtLeast(1)
        target.userId to count
    }
}
