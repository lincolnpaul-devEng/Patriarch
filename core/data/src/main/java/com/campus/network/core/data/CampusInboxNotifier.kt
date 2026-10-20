package com.campus.network.core.data

import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

/**
 * Enqueues inbox rows into `campus_notifications` so the recipient's poller / FCM
 * path can surface system notifications for DMs and friend requests.
 */
class CampusInboxNotifier(
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun notifyDirectMessage(
        recipientUserId: String,
        senderUserId: String,
        senderName: String,
        threadId: String,
        messageText: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(senderUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        val preview = messageText.trim().ifBlank { "Sent you a message" }.take(180)
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = senderUserId,
            actorName = senderName.ifBlank { "Campus peer" },
            type = TYPE_DIRECT_MESSAGE,
            title = senderName.ifBlank { "New message" },
            body = preview,
            threadId = threadId
        )
    }

    suspend fun notifyFriendRequest(
        recipientUserId: String,
        senderUserId: String,
        senderName: String,
        serverId: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(senderUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = senderUserId,
            actorName = senderName.ifBlank { "Campus peer" },
            type = TYPE_FRIEND_REQUEST,
            title = "Friend request",
            body = "${senderName.ifBlank { "A campus peer" }} sent you a friend request",
            threadId = null,
            serverId = serverId
        )
    }

    suspend fun notifyFriendRequestAccepted(
        recipientUserId: String,
        actorUserId: String,
        actorName: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(actorUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = actorUserId,
            actorName = actorName.ifBlank { "Campus peer" },
            type = TYPE_FRIEND_REQUEST_ACCEPTED,
            title = "Friend request accepted",
            body = "${actorName.ifBlank { "A campus peer" }} accepted your friend request",
            threadId = null,
            serverId = null
        )
    }

    private suspend fun insertNotification(
        recipientUserId: String,
        actorUserId: String,
        actorName: String,
        type: String,
        title: String,
        body: String,
        threadId: String?,
        serverId: String? = null
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("id", "notif_${UUID.randomUUID()}")
            put("recipient_user_id", recipientUserId)
            put("actor_user_id", actorUserId)
            put("actor_name", actorName)
            put("type", type)
            put("title", title)
            put("body", body)
            if (!threadId.isNullOrBlank()) put("thread_id", threadId)
            if (!serverId.isNullOrBlank()) put("server_id", serverId)
            put("is_read", false)
            put("created_at_epoch_ms", now)
        }.toString()
        return supabaseClient.insertRecord("campus_notifications", payload).map { }
    }

    companion object {
        const val TYPE_DIRECT_MESSAGE = "DIRECT_MESSAGE"
        const val TYPE_FRIEND_REQUEST = "FRIEND_REQUEST"
        const val TYPE_FRIEND_REQUEST_ACCEPTED = "FRIEND_REQUEST_ACCEPTED"
        const val TYPE_CHANNEL_MESSAGE = "CHANNEL_MESSAGE"
    }
}
