package com.campus.network.core.network.call

import android.content.Context
import android.util.Log
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stream Video backend for [CallMediaProvider].
 *
 * Uses the same Stream API key + JWT edge function as Stream Chat.
 * Campus signaling ([CallSignalingRepository]) still owns ring/accept/decline;
 * this provider only joins the media room after accept.
 */
class StreamVideoCallMediaProvider(
    private val appContext: Context,
    private val apiKey: String,
    private val tokenFetcher: suspend (userId: String) -> Result<String>,
    private val displayNameResolver: suspend (userId: String) -> String = { it }
) : CallMediaProvider {

    override val providerId: String = "stream-video"
    override val displayName: String = "Stream Video"
    override val isConfigured: Boolean = apiKey.isNotBlank()

    private val mutex = Mutex()

    @Volatile
    private var connectedUserId: String? = null

    @Volatile
    var activeCall: Call? = null
        private set

    override suspend fun joinCall(
        sessionId: String,
        isVideo: Boolean,
        localUserId: String
    ): Result<Unit> = mutex.withLock {
        if (!isConfigured) {
            return Result.failure(IllegalStateException("STREAM_API_KEY is blank"))
        }
        if (localUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("localUserId required for Stream Video"))
        }
        val callId = sanitizeCallId(sessionId)
        return runCatching {
            val client = ensureClient(localUserId)
            val call = client.call(CALL_TYPE, callId)
            val joinResult = call.join(create = true)
            if (joinResult.isFailure) {
                throw joinResult.errorOrNull()?.let { Exception(it.message) }
                    ?: IllegalStateException("Stream join failed")
            }
            call.microphone.setEnabled(true)
            call.camera.setEnabled(isVideo)
            call.speaker.setEnabled(true)
            activeCall = call
            Log.i(TAG, "Joined Stream call type=$CALL_TYPE id=$callId video=$isVideo")
        }
    }

    override suspend fun leaveCall(sessionId: String): Result<Unit> = mutex.withLock {
        return runCatching {
            val call = activeCall
            if (call != null) {
                runCatching { call.leave() }
                activeCall = null
            }
            Unit
        }
    }

    override suspend fun setMuted(muted: Boolean) {
        activeCall?.microphone?.setEnabled(!muted)
    }

    override suspend fun setVideoEnabled(enabled: Boolean) {
        activeCall?.camera?.setEnabled(enabled)
    }

    override suspend fun flipCamera() {
        activeCall?.camera?.flip()
    }

    override suspend fun setSpeakerphone(enabled: Boolean) {
        activeCall?.speaker?.setEnabled(enabled)
    }

    private suspend fun ensureClient(userId: String): StreamVideo {
        val existingId = connectedUserId
        if (StreamVideo.isInstalled && existingId == userId) {
            return StreamVideo.instance()
        }
        if (StreamVideo.isInstalled) {
            runCatching { StreamVideo.removeClient() }
            connectedUserId = null
        }
        val token = tokenFetcher(userId).getOrThrow()
        val name = displayNameResolver(userId).ifBlank { userId }
        val user = User(
            id = sanitizeUserId(userId),
            name = name,
            role = "user"
        )
        val client = StreamVideoBuilder(
            context = appContext.applicationContext,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = token
        ).build()
        connectedUserId = userId
        return client
    }

    companion object {
        private const val TAG = "StreamVideoMedia"
        private const val CALL_TYPE = "default"

        /** Stream call ids: alphanumeric, underscore, hyphen. */
        fun sanitizeCallId(raw: String): String {
            val cleaned = raw.trim().lowercase()
                .replace(Regex("[^a-z0-9_\\-]"), "-")
                .trim('-')
                .take(64)
            return cleaned.ifBlank { "campus-call" }
        }

        fun sanitizeUserId(raw: String): String {
            val cleaned = raw.trim()
                .replace(Regex("[^a-zA-Z0-9_\\-@.]"), "_")
                .take(255)
            return cleaned.ifBlank { "anon" }
        }
    }
}
