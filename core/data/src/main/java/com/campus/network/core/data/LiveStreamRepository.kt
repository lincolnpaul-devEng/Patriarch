package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.database.entity.StreamerProfileEntity
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StreamComment
import com.campus.network.core.model.isRealProfileDisplayName
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
    private val profileNameResolver = SupabaseProfileNameResolver(supabaseClient)

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
        broadcasterHandle: String,
        broadcasterAvatarRes: Int,
        title: String,
        description: String,
        category: String,
        audience: String,
        broadcasterDisplayName: String? = null,
        streamPlaybackUrl: String? = null
    ): LiveStream = withContext(ioDispatcher) {
        val resolvedBroadcasterName = broadcasterDisplayName?.takeIf(String::isRealProfileDisplayName)
            ?: profileNameResolver.resolve(listOf(broadcasterId))[broadcasterId.trim().lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to start a live stream")
        val now = System.currentTimeMillis()
        val streamId = id

        val syncId = java.util.UUID.randomUUID().toString()
        val pendingEntity = LiveStreamEntity(
            id = streamId,
            broadcasterId = broadcasterId,
            broadcasterName = resolvedBroadcasterName,
            broadcasterHandle = broadcasterHandle,
            broadcasterAvatarRes = broadcasterAvatarRes,
            broadcasterAvatarUrl = null,
            title = title,
            description = description,
            category = category,
            audience = audience,
            isLive = true,
            streamPlaybackUrl = null,
            backdropImageRes = 0,
            backdropImageUrl = null,
            viewerCount = 1,
            peakViewers = 1,
            likeCount = 0,
            startedAtEpochMs = now,
            serverId = null,
            syncStatus = com.campus.network.core.model.SyncStatus.PENDING.name,
            syncId = syncId
        )
        streamDao.insertStream(pendingEntity)

        val remotePayload = JSONObject().apply {
            put("id", streamId)
            put("broadcaster_id", broadcasterId)
            put("broadcaster_name", resolvedBroadcasterName)
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
            .requireSavedRow()

        val finalEntity = pendingEntity.copy(
            syncStatus = syncStatusFor(remoteResult).name,
            streamPlaybackUrl = if (remoteResult.isSuccess) streamPlaybackUrl else null
        )
        streamDao.insertStream(finalEntity)

        return@withContext finalEntity.toDomain()
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
        senderAvatarRes: Int,
        commentText: String,
        senderAvatarUrl: String? = null,
        senderDisplayName: String? = null
    ): StreamComment = withContext(ioDispatcher) {
        val resolvedSenderName = senderDisplayName?.takeIf(String::isRealProfileDisplayName)
            ?: profileNameResolver.resolve(listOf(senderId))[senderId.trim().lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to post a stream comment")
        val syncId = java.util.UUID.randomUUID().toString()
        val comment = StreamCommentEntity(
            id = "comment-$syncId",
            streamId = streamId,
            senderId = senderId,
            senderName = resolvedSenderName,
            senderAvatarUrl = senderAvatarUrl,
            senderAvatarRes = senderAvatarRes,
            commentText = commentText,
            timestampEpochMs = System.currentTimeMillis(),
            syncStatus = com.campus.network.core.model.SyncStatus.PENDING.name,
            syncId = syncId
        )
        commentDao.insertComment(comment)

        val commentPayload = JSONObject().apply {
            put("id", comment.id)
            put("stream_id", streamId)
            put("sender_id", senderId)
            put("sender_name", resolvedSenderName)
            put("sender_avatar_url", senderAvatarUrl)
            put("sender_avatar_res", senderAvatarRes)
            put("comment_text", commentText)
            put("timestamp_epoch_ms", comment.timestampEpochMs)
        }.toString()

        val remoteResult = supabaseClient.upsertRecord("campus_stream_comments", commentPayload, onConflict = "id")
            .requireSavedRow()
        val saved = comment.copy(syncStatus = syncStatusFor(remoteResult).name)
        commentDao.insertComment(saved)
        return@withContext saved.toDomain()
    }

    suspend fun incrementLikes(streamId: String) = withContext(ioDispatcher) {
        streamDao.incrementLikes(streamId)
    }

    suspend fun syncRemoteLiveStreams(sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        val deltaFilter = if (sinceMs > 0L) "&started_at_epoch_ms=gt.$sinceMs" else ""
        val queryResult = supabaseClient.queryTable("campus_live_streams?order=started_at_epoch_ms.asc&limit=100$deltaFilter")
        if (queryResult.isFailure) {
            return@withContext Result.failure(queryResult.exceptionOrNull() ?: Exception("Failed to fetch live streams"))
        }
        var newWatermark = sinceMs
        val raw = queryResult.getOrNull() ?: "[]"
        val array = JSONArray(raw)
        val broadcasterNames = try {
            profileNameResolver.resolve((0 until array.length()).map { array.getJSONObject(it).optString("broadcaster_id") })
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        }
        val entities = mutableListOf<LiveStreamEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val sat = obj.optLong("started_at_epoch_ms", System.currentTimeMillis())
            if (sat > newWatermark) newWatermark = sat
            entities.add(
                LiveStreamEntity(
                    id = obj.getString("id"),
                    broadcasterId = obj.getString("broadcaster_id"),
                    broadcasterName = broadcasterNames[obj.optString("broadcaster_id").lowercase()].orEmpty(),
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
                    startedAtEpochMs = sat,
                    endedAtEpochMs = if (obj.has("ended_at_epoch_ms") && !obj.isNull("ended_at_epoch_ms")) obj.getLong("ended_at_epoch_ms") else null,
                    durationSeconds = obj.optInt("duration_seconds", 0),
                    serverId = obj.optString("server_id", "").ifBlank { null }
                )
            )
        }
        if (entities.isNotEmpty()) {
            streamDao.insertStreams(entities)
        }
        Result.success(newWatermark)
    }

    private fun LiveStreamEntity.toDomain() = LiveStream(
        id = id,
        broadcasterId = broadcasterId,
        broadcasterName = broadcasterName.takeIf(String::isRealProfileDisplayName).orEmpty(),
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
        serverId = serverId,
        syncStatus = syncStatus,
        syncId = syncId
    )

    private fun StreamCommentEntity.toDomain() = StreamComment(
        id = id,
        streamId = streamId,
        senderId = senderId,
        senderName = senderName.takeIf(String::isRealProfileDisplayName).orEmpty(),
        senderAvatarUrl = senderAvatarUrl,
        senderAvatarRes = senderAvatarRes,
        commentText = commentText,
        timestampEpochMs = timestampEpochMs,
        syncStatus = syncStatus,
        syncId = syncId
    )
}
