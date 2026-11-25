package com.campus.network.notifications

import android.util.Log
import com.campus.network.core.data.SupabaseProfileNameResolver
import com.campus.network.core.ui.notifications.CampusPushType
import com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
import com.campus.network.core.network.SupabaseSdkClientProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging (FCM) Service for Patriarch (`com.campus.network`).
 * Works alongside `CampusNotificationPollManager` to route push payloads into
 * the type-tailored Android system notification channels.
 */
class CampusFirebaseMessagingService : FirebaseMessagingService() {
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CampusSystemNotificationCenter.saveFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val typeStr = data["type"]?.uppercase() ?: CampusPushType.DIRECT_MESSAGE.name
        val type = runCatching { CampusPushType.valueOf(typeStr) }.getOrDefault(CampusPushType.DIRECT_MESSAGE)

        val title = data["title"] ?: remoteMessage.notification?.title ?: "Patriarch Campus"
        val body = data["body"] ?: remoteMessage.notification?.body ?: "You have a new campus update"

        when (type) {
            CampusPushType.DIRECT_MESSAGE -> {
                val threadId = data["threadId"] ?: data["peerId"] ?: return
                val senderId = data["senderId"] ?: data["peerId"] ?: return
                val senderHandle = data["senderHandle"] ?: "@campus_peer"
                notificationScope.launch {
                    val senderName = resolveProfileName(senderId) ?: return@launch
                    CampusSystemNotificationCenter.showDirectMessageNotification(
                        context = applicationContext,
                        threadId = threadId,
                        senderName = senderName,
                        senderHandle = senderHandle,
                        messageText = body
                    )
                }
            }

            CampusPushType.INCOMING_CALL -> {
                val peerId = data["peerId"] ?: data["callerId"] ?: return
                val callerRole = data["callerRole"] ?: "Verified Student"
                val isVideo = data["isVideo"]?.toBooleanStrictOrNull() ?: true
                val callSessionId = data["callSessionId"] ?: data["sessionId"]

                notificationScope.launch {
                    val callerName = resolveProfileName(peerId) ?: return@launch
                    CampusSystemNotificationCenter.showIncomingCallNotification(
                        context = applicationContext,
                        peerId = peerId,
                        callerName = callerName,
                        callerRoleOrHandle = callerRole,
                        isVideoCall = isVideo,
                        callSessionId = callSessionId
                    )
                }
            }

            CampusPushType.FLASH_MEET -> {
                val meetId = data["meetId"]?.takeIf { it.isNotBlank() } ?: return
                val locationName = data["locationName"].orEmpty().ifBlank { "Campus Location" }
                val attendees = data["attendeeCount"]?.toIntOrNull() ?: 1
                val lat = data["latitude"]?.toDoubleOrNull() ?: return
                val lng = data["longitude"]?.toDoubleOrNull() ?: return

                CampusSystemNotificationCenter.showFlashMeetNotification(
                    context = applicationContext,
                    meetId = meetId,
                    title = title,
                    locationName = locationName,
                    attendeeCount = attendees,
                    timeRemainingLabel = data["timeRemaining"].orEmpty().ifBlank { "Starting soon" },
                    latitude = lat,
                    longitude = lng
                )
            }

            CampusPushType.SAFEPAY_ORDER -> {
                val sellerId = data["sellerId"] ?: data["sellerUserId"] ?: return
                val orderRef = data["orderRef"]?.takeIf { it.isNotBlank() } ?: return
                notificationScope.launch {
                    val sellerName = resolveProfileName(sellerId) ?: return@launch
                    CampusSystemNotificationCenter.showSafePayOrderNotification(
                        context = applicationContext,
                        orderRef = orderRef,
                        itemTitle = data["itemTitle"] ?: title,
                        amountFormatted = data["amount"].orEmpty(),
                        pickupSpotName = data["pickupSpot"].orEmpty().ifBlank { "Designated SafeSpot" },
                        sellerName = sellerName,
                        statusHeadline = body
                    )
                }
            }

            CampusPushType.LIVE_STREAM -> {
                val streamId = data["streamId"] ?: data["stream_id"] ?: "live_1"
                val streamTitle = data["streamTitle"] ?: data["title"] ?: body
                val categoryTag = data["category"] ?: "CAMPUS LIVE"
                val viewers = data["viewerCount"]?.toIntOrNull()
                    ?: data["viewer_count"]?.toIntOrNull()
                    ?: 1
                val hostId = data["hostId"] ?: data["broadcasterId"] ?: data["actorUserId"]
                notificationScope.launch {
                    val hostName = if (!hostId.isNullOrBlank()) {
                        resolveProfileName(hostId) ?: data["hostName"].orEmpty().ifBlank { title }
                    } else {
                        data["hostName"].orEmpty().ifBlank { title }
                    }
                    CampusSystemNotificationCenter.showLiveStreamNotification(
                        context = applicationContext,
                        streamId = streamId,
                        hostName = hostName,
                        streamTitle = streamTitle,
                        viewerCount = viewers,
                        categoryTag = categoryTag
                    )
                }
            }

            CampusPushType.MARKET_LISTING -> {
                val itemId = data["itemId"] ?: data["item_id"] ?: return
                val itemTitle = data["itemTitle"] ?: data["title"] ?: title
                val priceLabel = data["priceLabel"] ?: data["price"].orEmpty()
                val category = data["category"].orEmpty()
                val isUpdate = data["isUpdate"]?.toBooleanStrictOrNull()
                    ?: data["is_update"]?.toBooleanStrictOrNull()
                    ?: false
                val sellerId = data["sellerId"] ?: data["sellerUserId"] ?: data["actorUserId"]
                notificationScope.launch {
                    val sellerName = if (!sellerId.isNullOrBlank()) {
                        resolveProfileName(sellerId) ?: data["sellerName"].orEmpty()
                    } else {
                        data["sellerName"].orEmpty()
                    }
                    CampusSystemNotificationCenter.showMarketListingNotification(
                        context = applicationContext,
                        itemId = itemId,
                        title = itemTitle,
                        sellerName = sellerName,
                        priceLabel = priceLabel,
                        category = category,
                        isUpdate = isUpdate
                    )
                }
            }

            CampusPushType.CAMPUS_UPDATE -> {
                CampusSystemNotificationCenter.showCampusUpdateNotification(
                    context = applicationContext,
                    updateId = data["updateId"] ?: "upd_1",
                    guildName = title,
                    headline = body,
                    digestLines = listOf(body)
                )
            }

            CampusPushType.FRIEND_REQUEST -> {
                val peerId = data["peerId"] ?: data["actorUserId"] ?: return
                notificationScope.launch {
                    val peerName = resolveProfileName(peerId) ?: return@launch
                    CampusSystemNotificationCenter.showFriendRequestNotification(
                        context = applicationContext,
                        peerUserId = peerId,
                        peerName = peerName,
                        serverId = data["serverId"],
                        body = body
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        notificationScope.cancel()
        super.onDestroy()
    }

    private suspend fun resolveProfileName(userId: String): String? =
        try {
            SupabaseProfileNameResolver(SupabaseSdkClientProvider.defaultClient)
                .resolve(listOf(userId))[userId.trim().lowercase()]
        } catch (error: Exception) {
            Log.w(TAG, "Could not resolve a notification name from campusprofile", error)
            null
        }

    private companion object {
        const val TAG = "CampusFcmService"
    }
}
