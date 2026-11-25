package com.campus.network.core.network.call

import android.content.Context
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mutex-guarded shared host for the singleton [StreamVideo] SDK client so 1:1 calls and
 * Live Room broadcasts share one authenticated client per signed-in user without racing.
 */
object StreamVideoClientHost {
    private val mutex = Mutex()

    @Volatile
    private var connectedUserId: String? = null

    suspend fun ensureClient(
        appContext: Context,
        apiKey: String,
        userId: String,
        tokenFetcher: suspend (userId: String) -> Result<String>,
        displayNameResolver: suspend (userId: String) -> String = { it }
    ): StreamVideo = mutex.withLock {
        val normalizedId = userId.trim()
        require(apiKey.isNotBlank()) { "STREAM_API_KEY is blank" }
        require(normalizedId.isNotBlank()) { "userId required for Stream Video" }

        if (StreamVideo.isInstalled && connectedUserId == normalizedId) {
            return@withLock StreamVideo.instance()
        }
        if (StreamVideo.isInstalled) {
            runCatching { StreamVideo.removeClient() }
            connectedUserId = null
        }

        val token = tokenFetcher(normalizedId).getOrThrow()
        val name = displayNameResolver(normalizedId).ifBlank { normalizedId }
        val user = User(
            id = StreamVideoCallMediaProvider.sanitizeUserId(normalizedId),
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
        connectedUserId = normalizedId
        client
    }
}
