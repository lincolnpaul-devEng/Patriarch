package com.campus.network.feature.map.domain

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin geospatial, ENU projection, route resampling, and tilt-hysteresis utilities
 * shared between 2D Mapbox rendering and SceneView/ARCore 3D placement.
 */
object GeoSpatialMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun haversineMeters(a: GeoCoordinate, b: GeoCoordinate): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val sinLat = sin(dLat / 2.0)
        val sinLon = sin(dLon / 2.0)
        val h = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon
        val c = 2.0 * atan2(sqrt(h), sqrt((1.0 - h).coerceAtLeast(0.0)))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Initial clockwise bearing from true North in degrees [0, 360).
     */
    fun bearingDegrees(from: GeoCoordinate, to: GeoCoordinate): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val raw = Math.toDegrees(atan2(y, x))
        return ((raw % 360.0) + 360.0) % 360.0
    }

    /**
     * Resamples a polyline into evenly spaced AR waypoints for placing `path_chevron.glb` markers.
     */
    fun resamplePolyline(
        points: List<GeoCoordinate>,
        spacingMeters: Double = 3.0,
        maxWaypoints: Int = 24
    ): List<GeoCoordinate> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1 || spacingMeters <= 0.1 || maxWaypoints <= 1) {
            return listOf(points.first())
        }

        val result = ArrayList<GeoCoordinate>(maxWaypoints.coerceAtLeast(2))
        result.add(points.first())
        var distanceSinceLast = 0.0

        for (i in 0 until points.lastIndex) {
            if (result.size >= maxWaypoints) break
            var segStart = points[i]
            val segEnd = points[i + 1]
            var remainingSegLen = haversineMeters(segStart, segEnd)
            if (remainingSegLen < 0.05) continue

            while (distanceSinceLast + remainingSegLen >= spacingMeters && result.size < maxWaypoints) {
                val stepNeed = spacingMeters - distanceSinceLast
                val fraction = (stepNeed / remainingSegLen).coerceIn(0.0, 1.0)
                val nextPt = GeoCoordinate(
                    latitude = segStart.latitude + (segEnd.latitude - segStart.latitude) * fraction,
                    longitude = segStart.longitude + (segEnd.longitude - segStart.longitude) * fraction
                )
                result.add(nextPt)
                segStart = nextPt
                remainingSegLen -= stepNeed
                distanceSinceLast = 0.0
            }
            distanceSinceLast += remainingSegLen
        }

        val lastPt = points.last()
        if (result.size < maxWaypoints && haversineMeters(result.last(), lastPt) >= spacingMeters * 0.5) {
            result.add(lastPt)
        }
        return result
    }

    /**
     * Converts a target coordinate into right-handed OpenGL / SceneView local EUS/ENU coordinates
     * relative to `origin`:
     *  - +X = East (meters)
     *  - +Y = Up (meters; defaults to 0f unless both coordinates supply altitude)
     *  - -Z = North (meters; i.e., +Z = South)
     */
    fun geoToLocalEnuMeters(origin: GeoCoordinate, target: GeoCoordinate): Triple<Float, Float, Float> {
        val midLatRad = Math.toRadians((origin.latitude + target.latitude) * 0.5)
        val eastMeters = Math.toRadians(target.longitude - origin.longitude) * EARTH_RADIUS_METERS * cos(midLatRad)
        val northMeters = Math.toRadians(target.latitude - origin.latitude) * EARTH_RADIUS_METERS
        val upMeters = if (origin.altitudeMeters != null && target.altitudeMeters != null) {
            (target.altitudeMeters - origin.altitudeMeters).toFloat()
        } else {
            0f
        }
        return Triple(eastMeters.toFloat(), upMeters, (-northMeters).toFloat())
    }

    /**
     * `path_chevron.glb` faces `-Z` at zero rotation (North in OpenGL EUS).
     * Bearing is clockwise from North (0 = North, 90 = East -> yaw = -90 deg).
     */
    fun bearingToGlYawRadians(bearingDeg: Double): Float {
        return Math.toRadians(-bearingDeg).toFloat()
    }

    /**
     * Converts an ENU offset to an ARCore camera-world offset when VPS is unavailable,
     * given the current camera world forward azimuth (degrees clockwise from North).
     * Camera forward azimuth aligns with camera `(forwardX, forwardZ)` projected on XZ.
     */
    fun enuToArLocalWithCompass(
        eastMeters: Float,
        northMeters: Float,
        cameraForwardX: Float,
        cameraForwardZ: Float,
        cameraAzimuthDeg: Float
    ): Pair<Float, Float> {
        // Angle of world North axis relative to AR session +X/+Z frame:
        // World vector for North = (0, 1) ENU; camera azimuth = alpha clockwise from North.
        // In AR space, forwardDir = norm(cameraForwardX, cameraForwardZ), rightDir = (-forwardZ, forwardX).
        val fLen = sqrt((cameraForwardX * cameraForwardX + cameraForwardZ * cameraForwardZ).toDouble())
            .coerceAtLeast(1e-4)
        val fx = (cameraForwardX / fLen).toFloat()
        val fz = (cameraForwardZ / fLen).toFloat()
        val rx = -fz
        val rz = fx

        val azRad = Math.toRadians(cameraAzimuthDeg.toDouble())
        // Component along camera forward and camera right from ENU (east, north):
        val forwardMeters = (northMeters * cos(azRad) + eastMeters * sin(azRad)).toFloat()
        val rightMeters = (-northMeters * sin(azRad) + eastMeters * cos(azRad)).toFloat()

        val arX = forwardMeters * fx + rightMeters * rx
        val arZ = forwardMeters * fz + rightMeters * rz
        return arX to arZ
    }

    /**
     * Hysteresis gate preventing rapid flickering between 2D Map and AR Camera:
     *  - Enters AR when pitch exceeds 45 deg
     *  - Exits AR back to 2D Map when pitch drops below 35 deg
     */
    fun computeTiltHysteresis(
        currentArActive: Boolean,
        pitchDegrees: Float,
        enterThresholdDeg: Float = 45f,
        exitThresholdDeg: Float = 35f
    ): Boolean {
        return when {
            pitchDegrees > enterThresholdDeg -> true
            pitchDegrees < exitThresholdDeg -> false
            else -> currentArActive
        }
    }

    fun formatDistanceShort(meters: Double): String {
        val clamped = meters.coerceAtLeast(0.0)
        return if (clamped < 950.0) {
            "${clamped.roundToInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", clamped / 1000.0)
        }
    }

    fun formatDurationShort(seconds: Double): String {
        val mins = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        return if (mins < 60) {
            "$mins min"
        } else {
            val hrs = mins / 60
            val rem = mins % 60
            if (rem == 0) "${hrs}h" else "${hrs}h ${rem}m"
        }
    }

    fun maneuverFromString(modifier: String?, type: String?): ManeuverDirection {
        val normType = type?.trim()?.lowercase().orEmpty()
        if (normType == "arrive") return ManeuverDirection.ARRIVE
        return when (modifier?.trim()?.lowercase()) {
            "uturn" -> ManeuverDirection.UTURN
            "sharp left" -> ManeuverDirection.SHARP_LEFT
            "left" -> ManeuverDirection.LEFT
            "slight left" -> ManeuverDirection.SLIGHT_LEFT
            "sharp right" -> ManeuverDirection.SHARP_RIGHT
            "right" -> ManeuverDirection.RIGHT
            "slight right" -> ManeuverDirection.SLIGHT_RIGHT
            else -> ManeuverDirection.STRAIGHT
        }
    }
}
