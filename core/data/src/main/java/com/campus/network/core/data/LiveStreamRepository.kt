package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.database.entity.StreamerProfileEntity
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StreamComment
import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for Live Streams, Broadcasters, and Stream Comments.
 * Backed by Room cache and asynchronous worker thread dispatchers.
 */
class LiveStreamRepository(
    private val database: CampusDatabase,
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
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

        val remotePayload = JSONObject().apply {
            put("id", id)
            put("broadcaster_id", broadcasterId)
            put("broadcaster_name", broadcasterName)
            put("broadcaster_handle", broadcasterHandle)
            put("broadcaster_avatar_res", broadcasterAvatarRes)
            put("title", title)
            put("description", description)
            put("category", category)
            put("audience", audience)
            put("is_live", true)
            put("stream_playback_url", streamPlaybackUrl)
            put("viewer_count", 1)
            put("peak_viewers", 1)
            put("like_count", 0)
            put("started_at_epoch_ms", now)
        }.toString()

        val remoteResult = supabaseClient.upsertRecord("campus_live_streams", remotePayload, onConflict = "id")
        if (remoteResult.isFailure) {
            throw remoteResult.exceptionOrNull() ?: Exception("Failed to create live stream on cloud")
        }

        streamDao.insertStream(entity)
        return@withContext entity.toDomain()
    }

    suspend fun endStream(
        streamId: String,
        durationSeconds: Int,
        peakViewers: Int,
        likes: Int
    ) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val updatePayload = JSONObject().apply {
            put("is_live", false)
            put("ended_at_epoch_ms", now)
            put("duration_seconds", durationSeconds)
            put("peak_viewers", peakViewers)
            put("like_count", likes)
        }.toString()

        supabaseClient.updateRecord("campus_live_streams?id=eq.$streamId", updatePayload)

        streamDao.endStream(
            streamId = streamId,
            endedAt = now,
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
        commentText: String,
        senderAvatarUrl: String? = null
    ) = withContext(ioDispatcher) {
        val comment = StreamCommentEntity(
            id = "c-${System.currentTimeMillis()}",
            streamId = streamId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            senderAvatarRes = senderAvatarRes,
            commentText = commentText,
            timestampEpochMs = System.currentTimeMillis()
        )

        val commentPayload = JSONObject().apply {
            put("id", comment.id)
            put("stream_id", streamId)
            put("sender_id", senderId)
            put("sender_name", senderName)
            put("sender_avatar_url", senderAvatarUrl)
            put("sender_avatar_res", senderAvatarRes)
            put("comment_text", commentText)
            put("timestamp_epoch_ms", comment.timestampEpochMs)
        }.toString()

        supabaseClient.upsertRecord("campus_stream_comments", commentPayload, onConflict = "id")

        commentDao.insertComment(comment)
    }

    suspend fun incrementLikes(streamId: String) = withContext(ioDispatcher) {
        streamDao.incrementLikes(streamId)
    }

    suspend fun syncRemoteLiveStreams(): Result<Unit> = withContext(ioDispatcher) {
        val queryResult = supabaseClient.queryTable("campus_live_streams?order=started_at_epoch_ms.desc&limit=30")
        if (queryResult.isFailure) {
            return@withContext Result.failure(queryResult.exceptionOrNull() ?: Exception("Failed to fetch live streams"))
        }
        val raw = queryResult.getOrNull() ?: "[]"
        val array = JSONArray(raw)
        val entities = mutableListOf<LiveStreamEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            entities.add(
                LiveStreamEntity(
                    id = obj.getString("id"),
                    broadcasterId = obj.getString("broadcaster_id"),
                    broadcasterName = obj.optString("broadcaster_name", "Broadcaster"),
                    broadcasterHandle = obj.optString("broadcaster_handle", "@broadcaster"),
                    broadcasterAvatarRes = obj.optInt("broadcaster_avatar_res", 0),
                    broadcasterAvatarUrl = obj.optString("broadcaster_avatar_url", "").ifBlank { null },
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "General"),
                    audience = obj.optString("audience", "CAMPUS"),
                    isLive = obj.optBoolean("is_live", true),
                    streamPlaybackUrl = obj.optString("stream_playback_url", "").ifBlank { null },
                    backdropImageRes = obj.optInt("backdrop_image_res", 0),
                    backdropImageUrl = obj.optString("backdrop_image_url", "").ifBlank { null },
                    viewerCount = obj.optInt("viewer_count", 0),
                    peakViewers = obj.optInt("peak_viewers", 0),
                    likeCount = obj.optInt("like_count", 0),
                    startedAtEpochMs = obj.optLong("started_at_epoch_ms", System.currentTimeMillis()),
                    endedAtEpochMs = if (obj.has("ended_at_epoch_ms") && !obj.isNull("ended_at_epoch_ms")) obj.getLong("ended_at_epoch_ms") else null,
                    durationSeconds = obj.optInt("duration_seconds", 0),
                    serverId = obj.optString("server_id", "").ifBlank { null }
                )
            )
        }
        if (entities.isNotEmpty()) {
            streamDao.insertStreams(entities)
        }
        Result.success(Unit)
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
