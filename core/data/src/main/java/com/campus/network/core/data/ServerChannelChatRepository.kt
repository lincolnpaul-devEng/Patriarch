package com.campus.network.core.data

import com.campus.network.core.database.dao.ServerChannelMessageDao
import com.campus.network.core.database.entity.ServerChannelMessageEntity
import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID

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
    /** True while waiting for mandatory remote upsert to succeed. */
    val isPending: Boolean = false
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
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
    private val messageDao: ServerChannelMessageDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

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
                senderName = senderName.ifBlank { "Member" },
                senderAvatarUrl = senderAvatarUrl,
                text = text,
                gifUrl = gifUrl,
                imageUrlsJson = JSONArray(imageUrls).toString(),
                reactionCount = 0,
                timestampEpochMs = timestampEpochMs,
                isOptimistic = true,
                isDelivered = false,
                isMine = isMine
            )
        )
    }

    suspend fun loadLocalMessages(channelId: String): List<RemoteChannelChatMessage> =
        withContext(ioDispatcher) {
            val dao = messageDao ?: return@withContext emptyList()
            dao.getMessagesForChannel(channelId).map { it.toRemote() }
        }

    suspend fun cacheRemoteMessages(
        messages: List<RemoteChannelChatMessage>,
        myUserId: String
    ) = withContext(ioDispatcher) {
        val dao = messageDao ?: return@withContext
        if (messages.isEmpty()) return@withContext
        dao.insertMessages(
            messages.map { msg ->
                ServerChannelMessageEntity(
                    id = msg.id,
                    serverId = msg.serverId,
                    channelId = msg.channelId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    senderAvatarUrl = msg.senderAvatarUrl,
                    text = msg.text,
                    gifUrl = msg.gifUrl,
                    imageUrlsJson = JSONArray(msg.imageUrls).toString(),
                    reactionCount = msg.reactionCount,
                    timestampEpochMs = msg.timestampEpochMs,
                    isOptimistic = false,
                    isDelivered = true,
                    isMine = myUserId.isNotBlank() &&
                        msg.senderId.equals(myUserId, ignoreCase = true)
                )
            }
        )
    }

    suspend fun markDelivered(messageId: String) = withContext(ioDispatcher) {
        messageDao?.markMessageDelivered(messageId)
    }

    suspend fun getPendingMessages(): List<RemoteChannelChatMessage> = withContext(ioDispatcher) {
        val dao = messageDao ?: return@withContext emptyList()
        dao.getPendingMessages().map { it.toRemote() }
    }

    /**
     * Mandatory remote upsert. Callers must keep the message pending until this succeeds.
     */
    suspend fun sendMessage(
        serverId: String,
        channelId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        text: String,
        gifUrl: String? = null,
        imageUrls: List<String> = emptyList(),
        messageId: String = "cmsg_${UUID.randomUUID()}",
        timestampEpochMs: Long = System.currentTimeMillis()
    ): Result<RemoteChannelChatMessage> = withContext(ioDispatcher) {
        if (channelId.isBlank() || senderId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("channelId and senderId required"))
        }
        val imagesJson = JSONArray(imageUrls)
        val payload = JSONObject().apply {
            put("id", messageId)
            put("server_id", serverId)
            put("channel_id", channelId)
            put("sender_id", senderId)
            put("sender_name", senderName.ifBlank { "Member" })
            if (!senderAvatarUrl.isNullOrBlank()) put("sender_avatar_url", senderAvatarUrl)
            put("text", text)
            if (!gifUrl.isNullOrBlank()) put("gif_url", gifUrl)
            put("image_urls", imagesJson)
            put("reaction_count", 0)
            put("timestamp_epoch_ms", timestampEpochMs)
        }.toString()

        supabaseClient.upsertRecord("campus_server_channel_messages", payload, onConflict = "id")
            .map {
                RemoteChannelChatMessage(
                    id = messageId,
                    serverId = serverId,
                    channelId = channelId,
                    senderId = senderId,
                    senderName = senderName,
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
        val encoded = URLEncoder.encode(channelId, Charsets.UTF_8.name())
        val afterFilter = if (afterEpochMs > 0L) {
            "&timestamp_epoch_ms=gt.$afterEpochMs"
        } else {
            ""
        }
        val result = supabaseClient.queryTable(
            "campus_server_channel_messages?channel_id=eq.$encoded" +
                "$afterFilter&order=timestamp_epoch_ms.asc&limit=$limit"
        )
        result.mapCatching { raw -> parseMessages(raw) }
    }

    suspend fun setTyping(
        serverId: String,
        channelId: String,
        userId: String,
        displayName: String,
        avatarUrl: String?,
        isTyping: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        if (channelId.isBlank() || userId.isBlank()) {
            return@withContext Result.success(Unit)
        }
        val encodedChannel = URLEncoder.encode(channelId, Charsets.UTF_8.name())
        val encodedUser = URLEncoder.encode(userId, Charsets.UTF_8.name())
        if (!isTyping) {
            return@withContext supabaseClient.deleteRecord(
                "campus_channel_typing?channel_id=eq.$encodedChannel&user_id=eq.$encodedUser"
            ).map { }
        }
        val payload = JSONObject().apply {
            put("channel_id", channelId)
            put("user_id", userId)
            put("server_id", serverId)
            put("display_name", displayName.ifBlank { "Member" })
            if (!avatarUrl.isNullOrBlank()) put("avatar_url", avatarUrl)
            put("updated_at_epoch_ms", System.currentTimeMillis())
        }.toString()
        supabaseClient.upsertRecord(
            "campus_channel_typing",
            payload,
            onConflict = "channel_id,user_id"
        ).map { }
    }

    suspend fun fetchTypingForChannel(
        channelId: String,
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L
    ): Result<List<RemoteChannelTypingUser>> = withContext(ioDispatcher) {
        val encoded = URLEncoder.encode(channelId, Charsets.UTF_8.name())
        val result = supabaseClient.queryTable(
            "campus_channel_typing?channel_id=eq.$encoded" +
                "&updated_at_epoch_ms=gt.$fresherThanEpochMs&order=updated_at_epoch_ms.desc&limit=20"
        )
        result.mapCatching { raw -> parseTyping(raw) }
    }

    suspend fun fetchTypingForServer(
        serverId: String,
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L
    ): Result<List<RemoteChannelTypingUser>> = withContext(ioDispatcher) {
        val encoded = URLEncoder.encode(serverId, Charsets.UTF_8.name())
        val result = supabaseClient.queryTable(
            "campus_channel_typing?server_id=eq.$encoded" +
                "&updated_at_epoch_ms=gt.$fresherThanEpochMs&order=updated_at_epoch_ms.desc&limit=40"
        )
        result.mapCatching { raw -> parseTyping(raw) }
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
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            text = text,
            gifUrl = gifUrl,
            imageUrls = images,
            reactionCount = reactionCount,
            timestampEpochMs = timestampEpochMs,
            isPending = isOptimistic || !isDelivered
        )
    }

    private fun parseMessages(raw: String): List<RemoteChannelChatMessage> {
        val array = JSONArray(raw)
        val out = mutableListOf<RemoteChannelChatMessage>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val images = mutableListOf<String>()
            val imageArr = obj.optJSONArray("image_urls")
            if (imageArr != null) {
                for (j in 0 until imageArr.length()) {
                    imageArr.optString(j).takeIf { it.isNotBlank() }?.let { images.add(it) }
                }
            }
            out.add(
                RemoteChannelChatMessage(
                    id = obj.getString("id"),
                    serverId = obj.optString("server_id", ""),
                    channelId = obj.optString("channel_id", ""),
                    senderId = obj.optString("sender_id", ""),
                    senderName = obj.optString("sender_name", "Member"),
                    senderAvatarUrl = obj.optString("sender_avatar_url").ifBlank { null },
                    text = obj.optString("text", ""),
                    gifUrl = obj.optString("gif_url").ifBlank { null },
                    imageUrls = images,
                    reactionCount = obj.optInt("reaction_count", 0),
                    timestampEpochMs = obj.optLong("timestamp_epoch_ms", System.currentTimeMillis()),
                    isPending = false
                )
            )
        }
        return out
    }

    private fun parseTyping(raw: String): List<RemoteChannelTypingUser> {
        val array = JSONArray(raw)
        val out = mutableListOf<RemoteChannelTypingUser>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            out.add(
                RemoteChannelTypingUser(
                    channelId = obj.optString("channel_id", ""),
                    serverId = obj.optString("server_id", ""),
                    userId = obj.optString("user_id", ""),
                    displayName = obj.optString("display_name", "Member"),
                    avatarUrl = obj.optString("avatar_url").ifBlank { null },
                    updatedAtEpochMs = obj.optLong("updated_at_epoch_ms", 0L)
                )
            )
        }
        return out
    }
}
