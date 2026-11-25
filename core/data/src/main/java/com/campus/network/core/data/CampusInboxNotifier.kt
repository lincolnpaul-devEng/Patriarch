package com.campus.network.core.data

import android.util.Log
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
private data class NotifyLiveStreamParams(
    val p_stream_id: String,
    val p_title: String,
    val p_category: String = "CAMPUS LIVE",
    val p_viewer_count: Int = 1
)

@Serializable
private data class NotifyMarketListingParams(
    val p_item_id: String,
    val p_title: String,
    val p_category: String = "",
    val p_price: Double = 0.0,
    val p_currency: String = "KES",
    val p_image_url: String? = null,
    val p_is_update: Boolean = false
)

@Serializable
private data class RecordMarketSearchParams(
    val p_query: String,
    val p_category: String? = null
)

@Serializable
data class CampusNotificationRow(
    val id: String,
    val recipient_user_id: String,
    val actor_user_id: String,
    val actor_name: String,
    val type: String,
    val title: String,
    val body: String,
    val thread_id: String? = null,
    val server_id: String? = null,
    val payload_json: JsonObject = JsonObject(emptyMap()),
    val is_read: Boolean = false,
    val created_at_epoch_ms: Long
)

/**
 * Enqueues inbox rows into `campus_notifications` so the recipient's poller / FCM
 * path can surface system notifications for DMs, friend requests, Flash Meets,
 * live streams, and market listings matched to search interests.
 */
class CampusInboxNotifier(
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "CampusInboxNotifier"
        const val TYPE_DIRECT_MESSAGE = "DIRECT_MESSAGE"
        const val TYPE_FRIEND_REQUEST = "FRIEND_REQUEST"
        const val TYPE_FRIEND_REQUEST_ACCEPTED = "FRIEND_REQUEST_ACCEPTED"
        const val TYPE_CHANNEL_MESSAGE = "CHANNEL_MESSAGE"
        const val TYPE_FLASH_MEET = "FLASH_MEET"
        const val TYPE_LIVE_STREAM = "LIVE_STREAM"
        const val TYPE_MARKET_LISTING = "MARKET_LISTING"
    }

    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    suspend fun notifyDirectMessage(
        recipientUserId: String,
        senderUserId: String,
        threadId: String,
        messageText: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(senderUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        val senderName = profileNameResolver.resolve(listOf(senderUserId))[senderUserId.lowercase()]
            ?: return@withContext Result.failure(
                IllegalStateException("Sender profile has no real full_name in Supabase")
            )
        val preview = messageText.trim().ifBlank { "Sent you a message" }.take(180)
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = senderUserId,
            actorName = senderName,
            type = TYPE_DIRECT_MESSAGE,
            title = senderName,
            body = preview,
            threadId = threadId
        )
    }

    suspend fun notifyFriendRequest(
        recipientUserId: String,
        senderUserId: String,
        serverId: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(senderUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        val senderName = profileNameResolver.resolve(listOf(senderUserId))[senderUserId.lowercase()]
            ?: return@withContext Result.failure(
                IllegalStateException("Sender profile has no real full_name in Supabase")
            )
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = senderUserId,
            actorName = senderName,
            type = TYPE_FRIEND_REQUEST,
            title = "Friend request",
            body = "$senderName sent you a friend request",
            threadId = null,
            serverId = serverId
        )
    }

    suspend fun notifyFriendRequestAccepted(
        recipientUserId: String,
        actorUserId: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(actorUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        val actorName = profileNameResolver.resolve(listOf(actorUserId))[actorUserId.lowercase()]
            ?: return@withContext Result.failure(
                IllegalStateException("Actor profile has no real full_name in Supabase")
            )
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = actorUserId,
            actorName = actorName,
            type = TYPE_FRIEND_REQUEST_ACCEPTED,
            title = "Friend request accepted",
            body = "$actorName accepted your friend request",
            threadId = null,
            serverId = null
        )
    }

    suspend fun notifyFlashMeetInvite(
        recipientUserId: String,
        hostUserId: String,
        meetId: String,
        title: String,
        locationName: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> = withContext(ioDispatcher) {
        if (recipientUserId.isBlank() || recipientUserId.equals(hostUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        val hostName = profileNameResolver.resolve(listOf(hostUserId))[hostUserId.lowercase()]
            ?: return@withContext Result.failure(
                IllegalStateException("Host profile has no real full_name in Supabase")
            )
        val payload = buildJsonObject {
            put("meet_id", meetId)
            put("location", locationName)
            put("latitude", latitude)
            put("longitude", longitude)
        }
        insertNotification(
            recipientUserId = recipientUserId,
            actorUserId = hostUserId,
            actorName = hostName,
            type = TYPE_FLASH_MEET,
            title = title,
            body = "$hostName invited you to Flash Meet at $locationName",
            threadId = meetId,
            payloadJson = payload
        )
    }

    suspend fun notifyFlashMeetJoined(
        hostUserId: String,
        joinerUserId: String,
        meetId: String,
        title: String,
        locationName: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> = withContext(ioDispatcher) {
        if (hostUserId.isBlank() || hostUserId.equals(joinerUserId, ignoreCase = true)) {
            return@withContext Result.success(Unit)
        }
        val joinerName = profileNameResolver.resolve(listOf(joinerUserId))[joinerUserId.lowercase()]
            ?: return@withContext Result.failure(
                IllegalStateException("Joiner profile has no real full_name in Supabase")
            )
        val payload = buildJsonObject {
            put("meet_id", meetId)
            put("location", locationName)
            put("latitude", latitude)
            put("longitude", longitude)
        }
        insertNotification(
            recipientUserId = hostUserId,
            actorUserId = joinerUserId,
            actorName = joinerName,
            type = TYPE_FLASH_MEET,
            title = title,
            body = "$joinerName joined your Flash Meet: $title",
            threadId = meetId,
            payloadJson = payload
        )
    }

    /**
     * Fans out a LIVE_STREAM inbox row to every accepted friend of the broadcaster.
     * Recipients' pollers post the system notification with a Watch Live deep link.
     */
    suspend fun notifyFriendsOfLiveStream(
        streamId: String,
        title: String,
        category: String = "CAMPUS LIVE",
        viewerCount: Int = 1
    ): Result<Unit> = withContext(ioDispatcher) {
        if (streamId.isBlank()) return@withContext Result.success(Unit)
        runCatching {
            supabase.postgrest.rpc(
                "notify_friends_of_live_stream",
                NotifyLiveStreamParams(
                    p_stream_id = streamId.trim(),
                    p_title = title.trim().ifBlank { "Campus Live Stream" },
                    p_category = category.trim().ifBlank { "CAMPUS LIVE" },
                    p_viewer_count = viewerCount.coerceAtLeast(1)
                )
            )
            Unit
        }.onFailure { error ->
            Log.e(TAG, "notifyFriendsOfLiveStream failed: ${error.message}", error)
        }
    }

    /**
     * Fans out MARKET_LISTING alerts to users whose recent market searches /
     * favorite categories match the listing title or category.
     */
    suspend fun notifyMarketInterestMatches(
        itemId: String,
        title: String,
        category: String,
        price: Double,
        currency: String = "KES",
        imageUrl: String? = null,
        isUpdate: Boolean = false
    ): Result<Unit> = withContext(ioDispatcher) {
        if (itemId.isBlank() || title.isBlank()) return@withContext Result.success(Unit)
        runCatching {
            supabase.postgrest.rpc(
                "notify_market_interest_matches",
                NotifyMarketListingParams(
                    p_item_id = itemId.trim(),
                    p_title = title.trim(),
                    p_category = category.trim(),
                    p_price = price,
                    p_currency = currency.trim().ifBlank { "KES" },
                    p_image_url = imageUrl?.takeIf { it.isNotBlank() },
                    p_is_update = isUpdate
                )
            )
            Unit
        }.onFailure { error ->
            Log.e(TAG, "notifyMarketInterestMatches failed: ${error.message}", error)
        }
    }

    /** Persists a market search term so future listings can target this user. */
    suspend fun recordMarketSearchInterest(
        query: String,
        category: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext Result.success(Unit)
        runCatching {
            supabase.postgrest.rpc(
                "record_market_search_interest",
                RecordMarketSearchParams(
                    p_query = clean,
                    p_category = category?.trim()?.takeIf { it.isNotBlank() }
                )
            )
            Unit
        }.onFailure { error ->
            Log.w(TAG, "recordMarketSearchInterest failed: ${error.message}")
        }
    }

    private suspend fun insertNotification(
        recipientUserId: String,
        actorUserId: String,
        actorName: String,
        type: String,
        title: String,
        body: String,
        threadId: String?,
        serverId: String? = null,
        payloadJson: JsonObject = JsonObject(emptyMap())
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        supabase.from("campus_notifications").insert(
            CampusNotificationRow(
                id = "notif_${UUID.randomUUID()}",
                recipient_user_id = recipientUserId,
                actor_user_id = actorUserId,
                actor_name = actorName,
                type = type,
                title = title,
                body = body,
                thread_id = threadId,
                server_id = serverId,
                payload_json = payloadJson,
                is_read = false,
                created_at_epoch_ms = now
            )
        )
        Unit
    }
}
