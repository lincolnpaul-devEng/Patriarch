package com.campus.network.core.network.call

/**
 * Pluggable peer media socket (WebRTC / Stream Video / custom SFU).
 *
 * Signaling is handled separately via CallSignalingRepository.
 * Until a real media provider is configured, UI must not claim peer A/V is live.
 */
interface CallMediaProvider {
    val providerId: String
    val displayName: String
    val isConfigured: Boolean

    /** Join or create a media room for an accepted call session. */
    suspend fun joinCall(sessionId: String, isVideo: Boolean, localUserId: String): Result<Unit>

    suspend fun leaveCall(sessionId: String): Result<Unit>

    suspend fun setMuted(muted: Boolean)

    suspend fun setVideoEnabled(enabled: Boolean)

    suspend fun flipCamera()

    suspend fun setSpeakerphone(enabled: Boolean)
}

/** Default: signaling + ringtones work; peer audio/video not yet plugged in. */
class UnconfiguredCallMediaProvider : CallMediaProvider {
    override val providerId: String = "unconfigured"
    override val displayName: String = "Media provider not connected"
    override val isConfigured: Boolean = false

    override suspend fun joinCall(sessionId: String, isVideo: Boolean, localUserId: String): Result<Unit> =
        Result.failure(
            IllegalStateException(
                "No CallMediaProvider configured. Plug Stream Video / WebRTC behind CallMediaProvider " +
                    "to enable peer audio/video. Signaling and ringtones still work."
            )
        )

    override suspend fun leaveCall(sessionId: String): Result<Unit> = Result.success(Unit)

    override suspend fun setMuted(muted: Boolean) = Unit

    override suspend fun setVideoEnabled(enabled: Boolean) = Unit

    override suspend fun flipCamera() = Unit

    override suspend fun setSpeakerphone(enabled: Boolean) = Unit
}

object CallMediaProviderRegistry {
    @Volatile
    private var override: CallMediaProvider? = null

    fun install(provider: CallMediaProvider?) {
        override = provider
    }

    fun resolve(): CallMediaProvider = override ?: UnconfiguredCallMediaProvider()
}
