package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StreamComment
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class LiveStreamRow(
    val id: String = "",
    val broadcaster_id: String = "",
    val broadcaster_name: String = "",
    val broadcaster_handle: String = "",
    val broadcaster_avatar_res: Int = 0,
    val broadcaster_avatar_url: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val audience: String = "",
    val is_live: Boolean = true,
    val stream_playback_url: String? = null,
    val viewer_count: Int = 0,
    val peak_viewers: Int = 0,
    val like_count: Int = 0,
    val started_at_epoch_ms: Long = 0L,
    val ended_at_epoch_ms: Long? = null,
    val duration_seconds: Int = 0,
    val server_id: String? = null
)

@Serializable
data class StreamCommentRow(
    val id: String = "",
    val stream_id: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val sender_avatar_url: String? = null,
    val sender_avatar_res: Int = 0,
    val comment_text: String = "",
    val timestamp_epoch_ms: Long = 0L
)

/**
 * Repository for Live Streams, Broadcasters, and Stream Comments.
 * Backed by Room cache and asynchronous worker thread dispatchers.
 */
class LiveStreamRepository(
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val streamDao = database.liveStreamDao()
    private val profileDao = database.streamerProfileDao()
    private val commentDao = database.streamCommentDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

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

        val remoteResult = runCatching {
            supabase.from("campus_live_streams").upsert(
                LiveStreamRow(
                    id = streamId,
                    broadcaster_id = broadcasterId,
                    broadcaster_name = resolvedBroadcasterName,
                    broadcaster_handle = broadcasterHandle,
                    broadcaster_avatar_res = broadcasterAvatarRes,
                    broadcaster_avatar_url = null,
                    title = title,
                    description = description,
                    category = category,
                    audience = audience,
                    is_live = true,
                    stream_playback_url = streamPlaybackUrl,
                    viewer_count = 1,
                    peak_viewers = 1,
                    like_count = 0,
                    started_at_epoch_ms = now,
                    ended_at_epoch_ms = null,
                    duration_seconds = 0,
                    server_id = null
                )
            )
        }

        val syncStatus = if (remoteResult.isSuccess) {
            com.campus.network.core.model.SyncStatus.SYNCED
        } else {
            com.campus.network.core.model.SyncStatus.FAILED
        }
        val finalEntity = pendingEntity.copy(
            syncStatus = syncStatus.name,
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
        runCatching {
            supabase.from("campus_live_streams").update(
                mapOf(
                    "is_live" to false,
                    "ended_at_epoch_ms" to now,
                    "duration_seconds" to durationSeconds,
                    "peak_viewers" to peakViewers,
                    "like_count" to likes
                )
            ) {
                filter {
                    eq("id", streamId)
                }
            }
        }

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

        val remoteResult = runCatching {
            supabase.from("campus_stream_comments").upsert(
                StreamCommentRow(
                    id = comment.id,
                    stream_id = streamId,
                    sender_id = senderId,
                    sender_name = resolvedSenderName,
                    sender_avatar_url = senderAvatarUrl,
                    sender_avatar_res = senderAvatarRes,
                    comment_text = commentText,
                    timestamp_epoch_ms = comment.timestampEpochMs
                )
            )
        }

        val syncStatus = if (remoteResult.isSuccess) {
            com.campus.network.core.model.SyncStatus.SYNCED
        } else {
            com.campus.network.core.model.SyncStatus.FAILED
        }
        val saved = comment.copy(syncStatus = syncStatus.name)
        commentDao.insertComment(saved)
        return@withContext saved.toDomain()
    }

    suspend fun incrementLikes(streamId: String) = withContext(ioDispatcher) {
        streamDao.incrementLikes(streamId)
    }

    suspend fun syncRemoteLiveStreams(sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<LiveStreamRow> = supabase.from("campus_live_streams")
                .select(columns = Columns.ALL) {
                    if (sinceMs > 0L) {
                        filter {
                            gt("started_at_epoch_ms", sinceMs)
                        }
                    }
                    order("started_at_epoch_ms", order = Order.ASCENDING)
                    limit(100)
                }
                .decodeList<LiveStreamRow>()

            var newWatermark = sinceMs
            val broadcasterNames = try {
                profileNameResolver.resolve(rows.map { it.broadcaster_id })
            } catch (error: Exception) {
                emptyMap<String, String>()
            }

            val entities = rows.map { row ->
                val sat = row.started_at_epoch_ms
                if (sat > newWatermark) newWatermark = sat
                LiveStreamEntity(
                    id = row.id,
                    broadcasterId = row.broadcaster_id,
                    broadcasterName = broadcasterNames[row.broadcaster_id.lowercase()].orEmpty().ifBlank { row.broadcaster_name },
                    broadcasterHandle = row.broadcaster_handle.ifBlank { "@broadcaster" },
                    broadcasterAvatarRes = row.broadcaster_avatar_res,
                    broadcasterAvatarUrl = row.broadcaster_avatar_url,
                    title = row.title,
                    description = row.description,
                    category = row.category.ifBlank { "General" },
                    audience = row.audience.ifBlank { "CAMPUS" },
                    isLive = row.is_live,
                    streamPlaybackUrl = row.stream_playback_url,
                    backdropImageRes = 0,
                    backdropImageUrl = null,
                    viewerCount = row.viewer_count,
                    peakViewers = row.peak_viewers,
                    likeCount = row.like_count,
                    startedAtEpochMs = sat,
                    endedAtEpochMs = row.ended_at_epoch_ms,
                    durationSeconds = row.duration_seconds,
                    serverId = row.server_id
                )
            }
            if (entities.isNotEmpty()) {
                streamDao.insertStreams(entities)
            }
            if (sinceMs == 0L) {
                val remoteIds = entities.map { it.id }
                if (remoteIds.isNotEmpty()) {
                    streamDao.deleteGhostStreams(remoteIds)
                } else {
                    streamDao.clearSyncedStreams()
                }
            }
            newWatermark
        }
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
