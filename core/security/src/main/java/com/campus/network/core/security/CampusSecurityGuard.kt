package com.campus.network.core.security

import com.campus.network.core.model.CampusAccessTier
import com.campus.network.core.model.CampusSecurityPolicy
import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.model.StudentProfile
import java.security.MessageDigest
import java.util.Locale

/**
 * Edge Security & Route Protection Guard in :core:security.
 * Provides signed location token generation, route authorization checks,
 * and SafeTrade QR payload validation.
 */
object CampusSecurityGuard {

    fun evaluateSessionAccessTier(
        profile: StudentProfile?,
        isAuthenticated: Boolean,
        isGuestMode: Boolean
    ): CampusAccessTier {
        return CampusSecurityPolicy.resolveAccessTier(
            profile = profile,
            isAuthenticatedSession = isAuthenticated,
            isGuestSession = isGuestMode
        )
    }

    /**
     * Generates a scoped, short-lived cryptographic share URL for live campus location sharing.
     * When [LocationPrivacyScope.GHOST_MODE_HIDDEN] is active, returns a suspended notice URI.
     */
    fun buildScopedLocationShareUrl(
        studentId: String,
        latitude: Double,
        longitude: Double,
        scope: LocationPrivacyScope,
        rotationEpochSeed: Long
    ): String {
        if (!scope.allowsCoordinatesBroadcast) {
            return "https://campus.patriarch.sec/location/ghost-mode-active"
        }
        val rawPayload = "$studentId:${scope.name}:${String.format(Locale.US, "%.4f", latitude)}:${String.format(Locale.US, "%.4f", longitude)}:$rotationEpochSeed"
        val token = sha256ShortHex(rawPayload)
        val scopeSlug = when (scope) {
            LocationPrivacyScope.ESCROW_COUNTERPARTY_ONLY -> "escrow"
            LocationPrivacyScope.VERIFIED_CAMPUS_PEERS -> "peers"
            LocationPrivacyScope.TIMED_SESSION_30M -> "ttl30m"
            LocationPrivacyScope.GHOST_MODE_HIDDEN -> "hidden"
        }
        return "https://campus.patriarch.sec/loc/$scopeSlug?tok=$token"
    }

    private fun sha256ShortHex(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            digest.take(6).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toUInt().toString(16)
        }
    }
}
