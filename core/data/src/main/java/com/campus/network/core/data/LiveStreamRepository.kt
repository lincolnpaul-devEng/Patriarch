package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StreamComment
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.RealtimeEventType
import com.campus.network.core.network.SupabaseRealtimeChange
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
    val backdrop_image_url: String? = null,
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

@Serializable
data class LiveRoomPresenceRow(
    val user_id: String = "",
    val display_name: String = "",
    val avatar_url: String? = null,
    val stream_id: String? = null,
    val is_hand_raised: Boolean = false,
    val is_muted: Boolean = false,
    val is_camera_on: Boolean = true,
    val is_on_stage: Boolean = false,
    val last_seen_epoch_ms: Long = 0L
)

data class LiveRoomPresence(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val streamId: String? = null,
    val isHandRaised: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isOnStage: Boolean = false,
    val lastSeenEpochMs: Long = 0L
)

@Serializable
private data class IncrementLikesRpcParams(
    val p_stream_id: String,
    val p_delta: Int = 1
)

@Serializable
private data class AdjustViewersRpcParams(
    val p_stream_id: String,
    val p_delta: Int
)

private val realtimePayloadJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/**
 * Repository for Live Streams, Broadcasters, Stream Comments, Likes, and Live Room Presence.
 * Backed by Room cache, Supabase PostgREST RPCs, and real-time Postgres Changes.
 */
class LiveStreamRepository(
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val streamDao = database.liveStreamDao()
    private val commentDao = database.streamCommentDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    private val _roomPresence = MutableStateFlow<List<LiveRoomPresence>>(emptyList())
    val roomPresence: StateFlow<List<LiveRoomPresence>> = _roomPresence.asStateFlow()

    private val _newLiveStreamAlerts = MutableSharedFlow<LiveStream>(extraBufferCapacity = 16)
    val newLiveStreamAlerts: SharedFlow<LiveStream> = _newLiveStreamAlerts.asSharedFlow()

    private val seenLiveStreamIds = ConcurrentHashMap.newKeySet<String>()
    private val hasSeededInitialStreams = AtomicBoolean(false)

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
        streamPlaybackUrl: String? = null,
        broadcasterAvatarUrl: String? = null
    ): LiveStream = withContext(ioDispatcher) {
        val resolvedBroadcasterName = broadcasterDisplayName?.takeIf(String::isRealProfileDisplayName)
            ?: profileNameResolver.resolve(listOf(broadcasterId))[broadcasterId.trim().lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to start a live stream")
        val now = System.currentTimeMillis()
        val streamId = id
        seenLiveStreamIds.add(streamId)

        val syncId = java.util.UUID.randomUUID().toString()
        val pendingEntity = LiveStreamEntity(
            id = streamId,
            broadcasterId = broadcasterId,
            broadcasterName = resolvedBroadcasterName,
            broadcasterHandle = broadcasterHandle,
            broadcasterAvatarRes = broadcasterAvatarRes,
            broadcasterAvatarUrl = broadcasterAvatarUrl,
            title = title,
            description = description,
            category = category,
            audience = audience,
            isLive = true,
            streamPlaybackUrl = streamPlaybackUrl,
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
                    broadcaster_avatar_url = broadcasterAvatarUrl,
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
            streamPlaybackUrl = streamPlaybackUrl
        )
        streamDao.insertStream(finalEntity)

        if (remoteResult.isSuccess) {
            runCatching {
                CampusInboxNotifier(supabase).notifyFriendsOfLiveStream(
                    streamId = streamId,
                    title = title,
                    category = category,
                    viewerCount = 1
                )
            }
        }

        return@withContext finalEntity.toDomain()
    }

    suspend fun endStream(
        streamId: String,
        durationSeconds: Int,
        peakViewers: Int,
        likes: Int
    ) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        seenLiveStreamIds.remove(streamId)
        val serverStream = runCatching {
            supabase.from("campus_live_streams")
                .select(columns = Columns.ALL) {
                    filter { eq("id", streamId) }
                    limit(1)
                }
                .decodeList<LiveStreamRow>()
                .firstOrNull()
        }.getOrNull()
        val finalPeak = maxOf(peakViewers, serverStream?.peak_viewers ?: 0, 1)
        val finalLikes = maxOf(likes, serverStream?.like_count ?: 0)

        runCatching {
            supabase.from("campus_live_streams").update(
                mapOf(
                    "is_live" to false,
                    "ended_at_epoch_ms" to now,
                    "duration_seconds" to durationSeconds,
                    "peak_viewers" to finalPeak,
                    "like_count" to finalLikes
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
            peak = finalPeak,
            likes = finalLikes
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

    suspend fun incrementLikes(streamId: String): Result<Int> = withContext(ioDispatcher) {
        val previousCount = streamDao.getStreamByIdSync(streamId)?.likeCount
        streamDao.incrementLikes(streamId)

        val remoteResult = runCatching {
            supabase.postgrest.rpc(
                "increment_live_stream_likes",
                IncrementLikesRpcParams(p_stream_id = streamId, p_delta = 1)
            ).decodeAs<Int>()
        }

        remoteResult.onSuccess { serverCount ->
            streamDao.updateLikeCount(streamId, serverCount)
        }.onFailure {
            if (previousCount != null) {
                streamDao.updateLikeCount(streamId, previousCount)
            }
        }
        remoteResult
    }

    suspend fun joinStreamAsViewer(streamId: String): Result<Int> = withContext(ioDispatcher) {
        adjustStreamViewerCount(streamId, delta = 1)
    }

    suspend fun leaveStreamAsViewer(streamId: String): Result<Int> = withContext(ioDispatcher) {
        adjustStreamViewerCount(streamId, delta = -1)
    }

    private suspend fun adjustStreamViewerCount(streamId: String, delta: Int): Result<Int> {
        val result = runCatching {
            supabase.postgrest.rpc(
                "adjust_live_stream_viewers",
                AdjustViewersRpcParams(p_stream_id = streamId, p_delta = delta)
            ).decodeAs<Int>()
        }
        result.onSuccess { count ->
            streamDao.updateViewerCount(streamId, count)
        }
        return result
    }

    suspend fun syncRoomPresence(): Result<List<LiveRoomPresence>> = withContext(ioDispatcher) {
        runCatching {
            val cutoff = System.currentTimeMillis() - PRESENCE_TTL_MS
            val rows: List<LiveRoomPresenceRow> = supabase.from("campus_live_room_presence")
                .select(columns = Columns.ALL) {
                    filter { gt("last_seen_epoch_ms", cutoff) }
                    order("last_seen_epoch_ms", order = Order.DESCENDING)
                    limit(50)
                }
                .decodeList()

            val resolved = runCatching {
                profileNameResolver.resolveWithAvatars(rows.map { it.user_id })
            }.getOrDefault(emptyMap())

            val mapped = rows.map { row ->
                val prof = resolved[row.user_id.trim().lowercase()]
                LiveRoomPresence(
                    userId = row.user_id,
                    displayName = prof?.name?.takeIf(String::isRealProfileDisplayName)
                        ?: row.display_name.takeIf(String::isRealProfileDisplayName)
                        .orEmpty(),
                    avatarUrl = row.avatar_url ?: prof?.avatarUrl,
                    streamId = row.stream_id,
                    isHandRaised = row.is_hand_raised,
                    isMuted = row.is_muted,
                    isCameraOn = row.is_camera_on,
                    isOnStage = row.is_on_stage,
                    lastSeenEpochMs = row.last_seen_epoch_ms
                )
            }
            _roomPresence.value = mapped
            mapped
        }
    }

    suspend fun upsertRoomPresence(
        userId: String,
        displayName: String,
        avatarUrl: String?,
        streamId: String? = null,
        isHandRaised: Boolean = false,
        isMuted: Boolean = false,
        isCameraOn: Boolean = true,
        isOnStage: Boolean = false
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("userId is required"))
        }
        val now = System.currentTimeMillis()
        val domainItem = LiveRoomPresence(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
            streamId = streamId,
            isHandRaised = isHandRaised,
            isMuted = isMuted,
            isCameraOn = isCameraOn,
            isOnStage = isOnStage,
            lastSeenEpochMs = now
        )
        _roomPresence.update { current -> mergePresence(current, domainItem) }

        runCatching {
            supabase.from("campus_live_room_presence").upsert(
                LiveRoomPresenceRow(
                    user_id = userId,
                    display_name = displayName,
                    avatar_url = avatarUrl,
                    stream_id = streamId,
                    is_hand_raised = isHandRaised,
                    is_muted = isMuted,
                    is_camera_on = isCameraOn,
                    is_on_stage = isOnStage,
                    last_seen_epoch_ms = now
                ),
                onConflict = "user_id"
            )
            Unit
        }
    }

    suspend fun removeRoomPresence(userId: String): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.success(Unit)
        _roomPresence.update { current ->
            current.filterNot { it.userId.equals(userId, ignoreCase = true) }
        }
        runCatching {
            supabase.from("campus_live_room_presence").delete {
                filter { eq("user_id", userId) }
            }
            Unit
        }
    }

    suspend fun applyRealtimeChange(
        change: SupabaseRealtimeChange,
        currentUserId: String? = null
    ) = withContext(ioDispatcher) {
        when (change.table.lowercase()) {
            "campus_live_streams" -> handleLiveStreamRealtime(change, currentUserId)
            "campus_stream_comments" -> handleStreamCommentRealtime(change)
            "campus_live_room_presence" -> handleRoomPresenceRealtime(change)
        }
    }

    private suspend fun handleLiveStreamRealtime(
        change: SupabaseRealtimeChange,
        currentUserId: String?
    ) {
        if (change.eventType == RealtimeEventType.DELETE) {
            val deletedId = extractJsonField(change.oldRecordJson, "id") ?: return
            seenLiveStreamIds.remove(deletedId)
            streamDao.deleteStreamById(deletedId)
            return
        }
        val jsonStr = change.recordJson ?: return
        val row = runCatching {
            realtimePayloadJson.decodeFromString<LiveStreamRow>(jsonStr)
        }.getOrNull() ?: return
        if (row.id.isBlank()) return

        val resolved = runCatching {
            profileNameResolver.resolveWithAvatars(listOf(row.broadcaster_id))
        }.getOrDefault(emptyMap())[row.broadcaster_id.trim().lowercase()]

        val resolvedAvatarUrl = row.broadcaster_avatar_url
            ?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            ?: resolved?.avatarUrl

        val entity = LiveStreamEntity(
            id = row.id,
            broadcasterId = row.broadcaster_id,
            broadcasterName = resolved?.name.orEmpty().ifBlank { row.broadcaster_name },
            broadcasterHandle = row.broadcaster_handle.ifBlank { "@broadcaster" },
            broadcasterAvatarRes = row.broadcaster_avatar_res,
            broadcasterAvatarUrl = resolvedAvatarUrl,
            title = row.title,
            description = row.description,
            category = row.category.ifBlank { "General" },
            audience = row.audience.ifBlank { "CAMPUS" },
            isLive = row.is_live,
            streamPlaybackUrl = row.stream_playback_url,
            backdropImageRes = 0,
            backdropImageUrl = row.backdrop_image_url,
            viewerCount = row.viewer_count,
            peakViewers = row.peak_viewers,
            likeCount = row.like_count,
            startedAtEpochMs = row.started_at_epoch_ms,
            endedAtEpochMs = row.ended_at_epoch_ms,
            durationSeconds = row.duration_seconds,
            serverId = row.server_id
        )
        streamDao.insertStream(entity)

        if (row.is_live) {
            val isNewlyLive = seenLiveStreamIds.add(row.id)
            val isSelf = !currentUserId.isNullOrBlank() &&
                row.broadcaster_id.equals(currentUserId, ignoreCase = true)
            if (isNewlyLive && !isSelf) {
                _newLiveStreamAlerts.emit(entity.toDomain())
            }
        } else {
            seenLiveStreamIds.remove(row.id)
        }
    }

    private suspend fun handleStreamCommentRealtime(change: SupabaseRealtimeChange) {
        if (change.eventType == RealtimeEventType.DELETE) {
            val deletedId = extractJsonField(change.oldRecordJson, "id") ?: return
            commentDao.deleteCommentById(deletedId)
            return
        }
        val jsonStr = change.recordJson ?: return
        val row = runCatching {
            realtimePayloadJson.decodeFromString<StreamCommentRow>(jsonStr)
        }.getOrNull() ?: return
        if (row.id.isBlank() || row.stream_id.isBlank()) return

        val resolved = runCatching {
            profileNameResolver.resolveWithAvatars(listOf(row.sender_id))
        }.getOrDefault(emptyMap())[row.sender_id.trim().lowercase()]

        commentDao.insertComment(
            StreamCommentEntity(
                id = row.id,
                streamId = row.stream_id,
                senderId = row.sender_id,
                senderName = resolved?.name.orEmpty().ifBlank { row.sender_name },
                senderAvatarUrl = row.sender_avatar_url ?: resolved?.avatarUrl,
                senderAvatarRes = row.sender_avatar_res,
                commentText = row.comment_text,
                timestampEpochMs = row.timestamp_epoch_ms
            )
        )
    }

    private suspend fun handleRoomPresenceRealtime(change: SupabaseRealtimeChange) {
        if (change.eventType == RealtimeEventType.DELETE) {
            val userId = extractJsonField(change.oldRecordJson, "user_id") ?: return
            _roomPresence.update { current ->
                current.filterNot { it.userId.equals(userId, ignoreCase = true) }
            }
            return
        }
        val jsonStr = change.recordJson ?: return
        val row = runCatching {
            realtimePayloadJson.decodeFromString<LiveRoomPresenceRow>(jsonStr)
        }.getOrNull() ?: return
        if (row.user_id.isBlank()) return

        val resolved = runCatching {
            profileNameResolver.resolveWithAvatars(listOf(row.user_id))
        }.getOrDefault(emptyMap())[row.user_id.trim().lowercase()]

        val item = LiveRoomPresence(
            userId = row.user_id,
            displayName = resolved?.name?.takeIf(String::isRealProfileDisplayName)
                ?: row.display_name.takeIf(String::isRealProfileDisplayName)
                .orEmpty(),
            avatarUrl = row.avatar_url ?: resolved?.avatarUrl,
            streamId = row.stream_id,
            isHandRaised = row.is_hand_raised,
            isMuted = row.is_muted,
            isCameraOn = row.is_camera_on,
            isOnStage = row.is_on_stage,
            lastSeenEpochMs = row.last_seen_epoch_ms
        )
        _roomPresence.update { current -> mergePresence(current, item) }
    }

    private fun mergePresence(
        existing: List<LiveRoomPresence>,
        incoming: LiveRoomPresence
    ): List<LiveRoomPresence> {
        val cutoff = System.currentTimeMillis() - PRESENCE_TTL_MS
        return (existing.filterNot {
            it.userId.equals(incoming.userId, ignoreCase = true) ||
                it.lastSeenEpochMs in 1L..cutoff
        } + incoming).sortedByDescending { it.lastSeenEpochMs }
    }

    private fun extractJsonField(jsonStr: String?, key: String): String? {
        if (jsonStr.isNullOrBlank()) return null
        val obj = runCatching {
            realtimePayloadJson.parseToJsonElement(jsonStr) as? JsonObject
        }.getOrNull() ?: return null
        return (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    suspend fun syncRemoteCommentsForStream(streamId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<StreamCommentRow> = supabase.from("campus_stream_comments")
                .select(columns = Columns.ALL) {
                    filter { eq("stream_id", streamId) }
                    order("timestamp_epoch_ms", order = Order.ASCENDING)
                    limit(100)
                }
                .decodeList()
            if (rows.isNotEmpty()) {
                val resolved = runCatching {
                    profileNameResolver.resolveWithAvatars(rows.map { it.sender_id })
                }.getOrDefault(emptyMap())
                val entities = rows.map { row ->
                    val prof = resolved[row.sender_id.lowercase()]
                    StreamCommentEntity(
                        id = row.id,
                        streamId = row.stream_id,
                        senderId = row.sender_id,
                        senderName = prof?.name.orEmpty().ifBlank { row.sender_name },
                        senderAvatarUrl = row.sender_avatar_url ?: prof?.avatarUrl,
                        senderAvatarRes = row.sender_avatar_res,
                        commentText = row.comment_text,
                        timestampEpochMs = row.timestamp_epoch_ms
                    )
                }
                commentDao.insertComments(entities)
            }
        }
    }

    suspend fun syncRemoteLiveStreams(sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<LiveStreamRow> = supabase.from("campus_live_streams")
                .select(columns = Columns.ALL) {
                    if (sinceMs > 0L) {
                        filter {
                            gt("started_at_epoch_ms", sinceMs)
                        }
                        order("started_at_epoch_ms", order = Order.ASCENDING)
                    } else {
                        order("started_at_epoch_ms", order = Order.DESCENDING)
                    }
                    limit(100)
                }
                .decodeList<LiveStreamRow>()

            var newWatermark = sinceMs
            val resolvedProfiles = try {
                profileNameResolver.resolveWithAvatars(rows.map { it.broadcaster_id })
            } catch (_: Exception) {
                emptyMap<String, ResolvedCampusProfile>()
            }

            val entities = rows.map { row ->
                val sat = row.started_at_epoch_ms
                if (sat > newWatermark) newWatermark = sat
                val resolved = resolvedProfiles[row.broadcaster_id.lowercase()]
                val resolvedAvatarUrl = row.broadcaster_avatar_url
                    ?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
                    ?: resolved?.avatarUrl
                LiveStreamEntity(
                    id = row.id,
                    broadcasterId = row.broadcaster_id,
                    broadcasterName = resolved?.name.orEmpty().ifBlank { row.broadcaster_name },
                    broadcasterHandle = row.broadcaster_handle.ifBlank { "@broadcaster" },
                    broadcasterAvatarRes = row.broadcaster_avatar_res,
                    broadcasterAvatarUrl = resolvedAvatarUrl,
                    title = row.title,
                    description = row.description,
                    category = row.category.ifBlank { "General" },
                    audience = row.audience.ifBlank { "CAMPUS" },
                    isLive = row.is_live,
                    streamPlaybackUrl = row.stream_playback_url,
                    backdropImageRes = 0,
                    backdropImageUrl = row.backdrop_image_url,
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

            val now = System.currentTimeMillis()
            val shouldNotify = hasSeededInitialStreams.get()
            entities.forEach { entity ->
                if (entity.isLive) {
                    val isNew = seenLiveStreamIds.add(entity.id)
                    if (shouldNotify && isNew && (now - entity.startedAtEpochMs) <= NEW_STREAM_ALERT_WINDOW_MS) {
                        _newLiveStreamAlerts.emit(entity.toDomain())
                    }
                } else {
                    seenLiveStreamIds.remove(entity.id)
                }
            }
            if (sinceMs == 0L) {
                hasSeededInitialStreams.set(true)
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

    companion object {
        const val LIVE_ROOM_LOBBY_STREAM_ID = "campus_live_room_lobby"
        private const val PRESENCE_TTL_MS = 120_000L
        private const val NEW_STREAM_ALERT_WINDOW_MS = 15L * 60_000L
    }
}
