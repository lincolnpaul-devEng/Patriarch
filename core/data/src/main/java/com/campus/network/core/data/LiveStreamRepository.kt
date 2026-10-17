package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.database.entity.StreamerProfileEntity
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StreamComment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for Live Streams, Broadcasters, and Stream Comments.
 * Backed by Room cache and asynchronous worker thread dispatchers.
 */
class LiveStreamRepository(
    private val database: CampusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val streamDao = database.liveStreamDao()
    private val profileDao = database.streamerProfileDao()
    private val commentDao = database.streamCommentDao()

    fun getAllStreams(): Flow<List<LiveStream>> {
        return streamDao.getAllStreams()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getActiveStreams(): Flow<List<LiveStream>> {
        return streamDao.getActiveStreams()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getStreamById(streamId: String): Flow<LiveStream?> {
        return streamDao.getStreamById(streamId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    fun getCommentsForStream(streamId: String): Flow<List<StreamComment>> {
        return commentDao.getCommentsForStream(streamId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    suspend fun createLiveStream(
        id: String,
        broadcasterId: String,
        broadcasterName: String,
        broadcasterHandle: String,
        broadcasterAvatarRes: Int,
        title: String,
        description: String,
        category: String,
        audience: String,
        streamPlaybackUrl: String? = null
    ): LiveStream = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val entity = LiveStreamEntity(
            id = id,
            broadcasterId = broadcasterId,
            broadcasterName = broadcasterName,
            broadcasterHandle = broadcasterHandle,
            broadcasterAvatarRes = broadcasterAvatarRes,
            title = title,
            description = description,
            category = category,
            audience = audience,
            isLive = true,
            streamPlaybackUrl = streamPlaybackUrl,
            viewerCount = 1,
            peakViewers = 1,
            likeCount = 0,
            startedAtEpochMs = now
        )
        streamDao.insertStream(entity)
        return@withContext entity.toDomain()
    }

    suspend fun endStream(
        streamId: String,
        durationSeconds: Int,
        peakViewers: Int,
        likes: Int
    ) = withContext(ioDispatcher) {
        streamDao.endStream(
            streamId = streamId,
            endedAt = System.currentTimeMillis(),
            duration = durationSeconds,
            peak = peakViewers,
            likes = likes
        )
    }

    suspend fun postComment(
        streamId: String,
        senderId: String,
        senderName: String,
        senderAvatarRes: Int,
        commentText: String
    ) = withContext(ioDispatcher) {
        val comment = StreamCommentEntity(
            id = "c-${System.currentTimeMillis()}",
            streamId = streamId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarRes = senderAvatarRes,
            commentText = commentText,
            timestampEpochMs = System.currentTimeMillis()
        )
        commentDao.insertComment(comment)
    }

    suspend fun incrementLikes(streamId: String) = withContext(ioDispatcher) {
        streamDao.incrementLikes(streamId)
    }

    private fun LiveStreamEntity.toDomain() = LiveStream(
        id = id,
        broadcasterId = broadcasterId,
        broadcasterName = broadcasterName,
        broadcasterHandle = broadcasterHandle,
        broadcasterAvatarRes = broadcasterAvatarRes,
        broadcasterAvatarUrl = broadcasterAvatarUrl,
        title = title,
        description = description,
        category = category,
        audience = audience,
        isLive = isLive,
        streamPlaybackUrl = streamPlaybackUrl,
        backdropImageRes = backdropImageRes,
        backdropImageUrl = backdropImageUrl,
        viewerCount = viewerCount,
        peakViewers = peakViewers,
        likeCount = likeCount,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
        durationSeconds = durationSeconds,
        serverId = serverId
    )

    private fun StreamCommentEntity.toDomain() = StreamComment(
        id = id,
        streamId = streamId,
        senderId = senderId,
        senderName = senderName,
        senderAvatarUrl = senderAvatarUrl,
        senderAvatarRes = senderAvatarRes,
        commentText = commentText,
        timestampEpochMs = timestampEpochMs
    )
}
