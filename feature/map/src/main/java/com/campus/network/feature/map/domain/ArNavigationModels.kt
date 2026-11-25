package com.campus.network.feature.map.domain

import androidx.compose.runtime.Immutable

/**
 * Lightweight, SDK-agnostic WGS84 coordinate model used across 2D Mapbox,
 * Mapbox Navigation, and ARCore Geospatial / ENU projection layers.
 */
@Immutable
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null
)

enum class ArViewMode {
    MAP_2D,
    AR_CAMERA
}

enum class TiltSwitchPolicy {
    AUTO_TILT,
    LOCKED_2D,
    LOCKED_AR
}

enum class GeospatialAnchorMode(val badgeLabel: String) {
    VPS_GEOSPATIAL("VPS Precise"),
    GPS_COMPASS_FALLBACK("GPS + Compass"),
    INITIALIZING("Calibrating AR"),
    AR_UNAVAILABLE("2D Guided")
}

@Immutable
data class DeviceOrientationState(
    val pitchDegrees: Float = 0f,
    val azimuthDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isArPitchActive: Boolean = false
)

@Immutable
data class SearchSuggestionItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val distanceMeters: Double? = null,
    val coordinate: GeoCoordinate? = null,
    val isCampusPlace: Boolean = false,
    val placeId: String? = null
)

enum class ManeuverDirection {
    STRAIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    UTURN,
    ARRIVE
}

@Immutable
data class ArRouteStep(
    val instruction: String,
    val secondaryText: String? = null,
    val direction: ManeuverDirection = ManeuverDirection.STRAIGHT,
    val distanceMeters: Double = 0.0,
    val maneuverCoordinate: GeoCoordinate? = null
)

@Immutable
data class ActiveNavigationSession(
    val destinationName: String,
    val destinationSubtitle: String,
    val destination: GeoCoordinate,
    val routeGeometry: List<GeoCoordinate> = emptyList(),
    val sampledArWaypoints: List<GeoCoordinate> = emptyList(),
    val currentStep: ArRouteStep? = null,
    val distanceRemainingMeters: Double = 0.0,
    val durationRemainingSeconds: Double = 0.0,
    val hasArrived: Boolean = false,
    val isOffRoute: Boolean = false,
    val snappedLocation: GeoCoordinate? = null,
    val isFallbackStraightLine: Boolean = false
)
