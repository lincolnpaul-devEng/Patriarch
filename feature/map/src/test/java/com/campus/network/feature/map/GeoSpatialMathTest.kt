package com.campus.network.feature.map

import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.ManeuverDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class GeoSpatialMathTest {

    @Test
    fun `computeTiltHysteresis prevents flickering inside 35 to 45 degree deadband`() {
        // Starting in 2D map (false)
        assertFalse(GeoSpatialMath.computeTiltHysteresis(currentArActive = false, pitchDegrees = 20f))
        assertFalse(GeoSpatialMath.computeTiltHysteresis(currentArActive = false, pitchDegrees = 40f))
        // Crosses enter threshold (> 45 deg) -> enters AR
        assertTrue(GeoSpatialMath.computeTiltHysteresis(currentArActive = false, pitchDegrees = 48f))

        // Inside deadband while in AR -> stays in AR
        assertTrue(GeoSpatialMath.computeTiltHysteresis(currentArActive = true, pitchDegrees = 40f))
        assertTrue(GeoSpatialMath.computeTiltHysteresis(currentArActive = true, pitchDegrees = 36f))
        // Drops below exit threshold (< 35 deg) -> returns to 2D map
        assertFalse(GeoSpatialMath.computeTiltHysteresis(currentArActive = true, pitchDegrees = 32f))
    }

    @Test
    fun `resamplePolyline produces evenly spaced waypoints capped cleanly`() {
        val start = GeoCoordinate(-1.3094, 36.8122)
        // Roughly 111m North
        val end = GeoCoordinate(-1.3084, 36.8122)
        val resampled = GeoSpatialMath.resamplePolyline(
            points = listOf(start, end),
            spacingMeters = 5.0,
            maxWaypoints = 12
        )
        assertEquals(12, resampled.size)
        val stepDist = GeoSpatialMath.haversineMeters(resampled[0], resampled[1])
        assertTrue("Expected ~5m spacing, was $stepDist", stepDist in 4.7..5.3)
    }

    @Test
    fun `geoToLocalEnuMeters maps North to negative Z and East to positive X`() {
        val origin = GeoCoordinate(0.0, 0.0)
        val northTarget = GeoCoordinate(0.0001, 0.0)
        val (nx, ny, nz) = GeoSpatialMath.geoToLocalEnuMeters(origin, northTarget)
        assertTrue("Expected near-zero X for due North, was $nx", kotlin.math.abs(nx) < 0.01f)
        assertEquals(0f, ny, 0.001f)
        assertTrue("Expected negative Z for due North, was $nz", nz < -10f)

        val eastTarget = GeoCoordinate(0.0, 0.0001)
        val (ex, _, ez) = GeoSpatialMath.geoToLocalEnuMeters(origin, eastTarget)
        assertTrue("Expected positive X for due East, was $ex", ex > 10f)
        assertTrue("Expected near-zero Z for due East, was $ez", kotlin.math.abs(ez) < 0.01f)
    }

    @Test
    fun `bearingDegrees and bearingToGlYawRadians align path_chevron -Z axis with route direction`() {
        val origin = GeoCoordinate(-1.3094, 36.8122)
        val north = GeoCoordinate(-1.3084, 36.8122)
        val east = GeoCoordinate(-1.3094, 36.8132)

        val northBearing = GeoSpatialMath.bearingDegrees(origin, north)
        assertTrue("North bearing expected ~0, was $northBearing", northBearing < 1.0 || northBearing > 359.0)
        assertEquals(0f, GeoSpatialMath.bearingToGlYawRadians(0.0), 0.001f)

        val eastBearing = GeoSpatialMath.bearingDegrees(origin, east)
        assertEquals(90.0, eastBearing, 1.0)
        assertEquals((-PI / 2.0).toFloat(), GeoSpatialMath.bearingToGlYawRadians(90.0), 0.005f)
    }

    @Test
    fun `formatDistanceShort and maneuverFromString format expected values`() {
        assertEquals("32 m", GeoSpatialMath.formatDistanceShort(32.4))
        assertEquals("1.3 km", GeoSpatialMath.formatDistanceShort(1260.0))
        assertEquals("6 min", GeoSpatialMath.formatDurationShort(340.0))
        assertEquals(ManeuverDirection.RIGHT, GeoSpatialMath.maneuverFromString("right", "turn"))
        assertEquals(ManeuverDirection.SLIGHT_LEFT, GeoSpatialMath.maneuverFromString("slight left", "turn"))
        assertEquals(ManeuverDirection.ARRIVE, GeoSpatialMath.maneuverFromString(null, "arrive"))
    }
}
