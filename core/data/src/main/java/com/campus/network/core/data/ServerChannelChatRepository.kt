package com.campus.network.core.data

import com.campus.network.core.database.dao.ServerChannelMessageDao
import com.campus.network.core.database.entity.ServerChannelMessageEntity
import com.campus.network.core.model.SyncStatus
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONArray

@Serializable
data class ServerChannelMessageRow(
    val id: String = "",
    val server_id: String = "",
    val channel_id: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val sender_avatar_url: String? = null,
    val text: String = "",
    val gif_url: String? = null,
    val image_urls: String? = null,
    val reaction_count: Int = 0,
    val timestamp_epoch_ms: Long = 0L
)

@Serializable
data class ServerChannelTypingRow(
    val channel_id: String = "",
    val user_id: String = "",
    val server_id: String = "",
    val display_name: String = "",
    val avatar_url: String? = null,
    val updated_at_epoch_ms: Long = 0L
)

data class RemoteChannelChatMessage(
    val id: String,
    val serverId: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val text: String,
    val gifUrl: String?,
    val imageUrls: List<String>,
    val reactionCount: Int,
    val timestampEpochMs: Long,
    val isPending: Boolean
)

data class RemoteChannelTypingUser(
    val channelId: String,
    val serverId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val updatedAtEpochMs: Long
)

/**
 * Server-channel chat: Room is offline cache only; Supabase upsert is required once online.
 */
class ServerChannelChatRepository(
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val messageDao: ServerChannelMessageDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    suspend fun upsertLocalPending(
        serverId: String,
        channelId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        text: String,
        gifUrl: String? = null,
        imageUrls: List<String> = emptyList(),
        messageId: String,
        timestampEpochMs: Long,
        isMine: Boolean
    ) = withContext(ioDispatcher) {
        val dao = messageDao ?: return@withContext
        dao.insertMessage(
            ServerChannelMessageEntity(
                id = messageId,
                serverId = serverId,
                channelId = channelId,
                senderId = senderId,
                senderName = senderName.takeIf(String::isRealProfileDisplayName).orEmpty(),
                senderAvatarUrl = senderAvatarUrl,
                text = text,
                gifUrl = gifUrl,
                imageUrlsJson = JSONArray(imageUrls).toString(),
                reactionCount = 0,
                timestampEpochMs = timestampEpochMs,
                isOptimistic = true,
                isDelivered = false,
                isMine = isMine,
                syncStatus = SyncStatus.PENDING.name,
                syncId = java.util.UUID.randomUUID().toString()
            )
        )
    }

    suspend fun loadLocalMessages(channelId: String): List<RemoteChannelChatMessage> =
        withContext(ioDispatcher) {
            val dao = messageDao ?: return@withContext emptyList()
            val messages = dao.getMessagesForChannel(channelId).map { it.toRemote() }
            val names = profileNameResolver.resolve(messages.map { it.senderId })
            messages.map { message ->
                message.copy(senderName = names[message.senderId.lowercase()].orEmpty())
            }
        }

    suspend fun cacheRemoteMessages(
        messages: List<RemoteChannelChatMessage>,
        myUserId: String
    ) = withContext(ioDispatcher) {
        val dao = messageDao ?: return@withContext
        if (messages.isEmpty()) return@withContext
        val entities = messages.map { message ->
            val isMine = myUserId.isNotBlank() &&
                message.senderId.equals(myUserId, ignoreCase = true)
            ServerChannelMessageEntity(
                id = message.id,
                serverId = message.serverId,
                channelId = message.channelId,
                senderId = message.senderId,
                senderName = message.senderName,
                senderAvatarUrl = message.senderAvatarUrl,
                text = message.text,
                gifUrl = message.gifUrl,
                imageUrlsJson = JSONArray(message.imageUrls).toString(),
                reactionCount = message.reactionCount,
                timestampEpochMs = message.timestampEpochMs,
                isOptimistic = false,
                isDelivered = true,
                isMine = isMine
            )
        }
        dao.insertMessages(entities)
    }

    suspend fun markMessageDelivered(messageId: String) = withContext(ioDispatcher) {
        messageDao?.markMessageDelivered(messageId)
    }

    suspend fun markDelivered(messageId: String) = markMessageDelivered(messageId)

    suspend fun markMessageFailed(messageId: String) = withContext(ioDispatcher) {
        messageDao?.updateSyncStatus(messageId, null, com.campus.network.core.model.SyncStatus.FAILED.name)
    }

    suspend fun getPendingMessages(): List<RemoteChannelChatMessage> = withContext(ioDispatcher) {
        val dao = messageDao ?: return@withContext emptyList()
        dao.getPendingMessages().map { it.toRemote() }
    }

    suspend fun sendMessage(
        serverId: String,
        channelId: String,
        senderId: String,
        senderAvatarUrl: String?,
        text: String,
        gifUrl: String? = null,
        imageUrls: List<String> = emptyList(),
        messageId: String = "cmsg_${java.util.UUID.randomUUID()}",
        timestampEpochMs: Long = System.currentTimeMillis()
    ): Result<RemoteChannelChatMessage> = dispatchMessage(
        serverId = serverId,
        channelId = channelId,
        senderId = senderId,
        senderAvatarUrl = senderAvatarUrl,
        text = text,
        gifUrl = gifUrl,
        imageUrls = imageUrls,
        messageId = messageId,
        timestampEpochMs = timestampEpochMs
    )

    suspend fun dispatchMessage(
        serverId: String,
        channelId: String,
        senderId: String,
        senderAvatarUrl: String?,
        text: String,
        gifUrl: String?,
        imageUrls: List<String>,
        messageId: String,
        timestampEpochMs: Long
    ): Result<RemoteChannelChatMessage> = withContext(ioDispatcher) {
        val resolvedSenderName = try {
            profileNameResolver.resolve(listOf(senderId))[senderId.trim().lowercase()]
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        } ?: return@withContext Result.failure(
            IllegalStateException("A Supabase profile name is required to send a channel message")
        )

        runCatching {
            supabase.from("campus_server_channel_messages").upsert(
                ServerChannelMessageRow(
                    id = messageId,
                    server_id = serverId,
                    channel_id = channelId,
                    sender_id = senderId,
                    sender_name = resolvedSenderName,
                    sender_avatar_url = senderAvatarUrl,
                    text = text,
                    gif_url = gifUrl,
                    image_urls = JSONArray(imageUrls).toString(),
                    reaction_count = 0,
                    timestamp_epoch_ms = timestampEpochMs
                )
            )
            RemoteChannelChatMessage(
                id = messageId,
                serverId = serverId,
                channelId = channelId,
                senderId = senderId,
                senderName = resolvedSenderName,
                senderAvatarUrl = senderAvatarUrl,
                text = text,
                gifUrl = gifUrl,
                imageUrls = imageUrls,
                reactionCount = 0,
                timestampEpochMs = timestampEpochMs,
                isPending = false
            )
        }
    }

    suspend fun fetchMessages(
        channelId: String,
        afterEpochMs: Long = 0L,
        limit: Int = 120
    ): Result<List<RemoteChannelChatMessage>> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<ServerChannelMessageRow> = supabase.from("campus_server_channel_messages")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("channel_id", channelId)
                        if (afterEpochMs > 0L) {
                            gt("timestamp_epoch_ms", afterEpochMs)
                        }
                    }
                    order("timestamp_epoch_ms", order = Order.ASCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ServerChannelMessageRow>()

            val names = try {
                profileNameResolver.resolve(rows.map { it.sender_id })
            } catch (error: Exception) {
                return@runCatching emptyList<RemoteChannelChatMessage>()
            }

            rows.map { row ->
                val images = mutableListOf<String>()
                if (!row.image_urls.isNullOrBlank()) {
                    runCatching {
                        val arr = JSONArray(row.image_urls)
                        for (i in 0 until arr.length()) {
                            arr.optString(i).takeIf { it.isNotBlank() }?.let { images.add(it) }
                        }
                    }
                }
                RemoteChannelChatMessage(
                    id = row.id,
                    serverId = row.server_id,
                    channelId = row.channel_id,
                    senderId = row.sender_id,
                    senderName = names[row.sender_id.lowercase()].orEmpty(),
                    senderAvatarUrl = row.sender_avatar_url,
                    text = row.text,
                    gifUrl = row.gif_url,
                    imageUrls = images,
                    reactionCount = row.reaction_count,
                    timestampEpochMs = row.timestamp_epoch_ms,
                    isPending = false
                )
            }
        }
    }

    suspend fun setTyping(
        serverId: String,
        channelId: String,
        userId: String,
        avatarUrl: String?,
        isTyping: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        if (channelId.isBlank() || userId.isBlank()) {
            return@withContext Result.success(Unit)
        }
        runCatching {
            if (!isTyping) {
                supabase.from("campus_channel_typing").delete {
                    filter {
                        eq("channel_id", channelId)
                        eq("user_id", userId)
                    }
                }
            } else {
                val resolvedDisplayName = try {
                    profileNameResolver.resolve(listOf(userId))[userId.trim().lowercase()]
                } catch (error: Exception) {
                    return@withContext Result.failure(error)
                } ?: return@withContext Result.failure(
                    IllegalStateException("A Supabase profile name is required to publish channel typing")
                )
                supabase.from("campus_channel_typing").upsert(
                    ServerChannelTypingRow(
                        channel_id = channelId,
                        user_id = userId,
                        server_id = serverId,
                        display_name = resolvedDisplayName,
                        avatar_url = avatarUrl,
                        updated_at_epoch_ms = System.currentTimeMillis()
                    )
                )
            }
            Unit
        }
    }

    suspend fun fetchTypingForChannel(
        channelId: String,
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L
    ): Result<List<RemoteChannelTypingUser>> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<ServerChannelTypingRow> = supabase.from("campus_channel_typing")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("channel_id", channelId)
                        gt("updated_at_epoch_ms", fresherThanEpochMs)
                    }
                    order("updated_at_epoch_ms", order = Order.DESCENDING)
                    limit(20)
                }
                .decodeList<ServerChannelTypingRow>()
            val names = profileNameResolver.resolve(rows.map { it.user_id })
            rows.map { row ->
                RemoteChannelTypingUser(
                    channelId = row.channel_id,
                    serverId = row.server_id,
                    userId = row.user_id,
                    displayName = names[row.user_id.lowercase()].orEmpty(),
                    avatarUrl = row.avatar_url,
                    updatedAtEpochMs = row.updated_at_epoch_ms
                )
            }
        }
    }

    suspend fun fetchTypingForServer(
        serverId: String,
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L
    ): Result<List<RemoteChannelTypingUser>> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<ServerChannelTypingRow> = supabase.from("campus_channel_typing")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("server_id", serverId)
                        gt("updated_at_epoch_ms", fresherThanEpochMs)
                    }
                    order("updated_at_epoch_ms", order = Order.DESCENDING)
                    limit(40)
                }
                .decodeList<ServerChannelTypingRow>()
            val names = profileNameResolver.resolve(rows.map { it.user_id })
            rows.map { row ->
                RemoteChannelTypingUser(
                    channelId = row.channel_id,
                    serverId = row.server_id,
                    userId = row.user_id,
                    displayName = names[row.user_id.lowercase()].orEmpty(),
                    avatarUrl = row.avatar_url,
                    updatedAtEpochMs = row.updated_at_epoch_ms
                )
            }
        }
    }

    private fun ServerChannelMessageEntity.toRemote(): RemoteChannelChatMessage {
        val images = mutableListOf<String>()
        runCatching {
            val arr = JSONArray(imageUrlsJson)
            for (i in 0 until arr.length()) {
                arr.optString(i).takeIf { it.isNotBlank() }?.let { images.add(it) }
            }
        }
        return RemoteChannelChatMessage(
            id = id,
            serverId = serverId,
            channelId = channelId,
            senderId = senderId,
            senderName = senderName.takeIf(String::isRealProfileDisplayName).orEmpty(),
            senderAvatarUrl = senderAvatarUrl,
            text = text,
            gifUrl = gifUrl,
            imageUrls = images,
            reactionCount = reactionCount,
            timestampEpochMs = timestampEpochMs,
            isPending = isOptimistic || !isDelivered
        )
    }
}
