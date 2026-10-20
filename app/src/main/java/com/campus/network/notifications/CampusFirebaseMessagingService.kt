package com.campus.network.notifications

import com.campus.network.core.ui.notifications.CampusPushType
import com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging (FCM) Service for Patriarch (`com.campus.network`).
 * Works alongside `CampusNotificationPollManager` to route push payloads into
 * the type-tailored Android system notification channels.
 */
class CampusFirebaseMessagingService : FirebaseMessagingService() {

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
                val threadId = data["threadId"] ?: data["peerId"] ?: "thread_mwangi"
                val senderName = data["senderName"] ?: data["peerName"] ?: title
                val senderHandle = data["senderHandle"] ?: "@campus_peer"
                val conversationTitle = data["conversationTitle"]

                CampusSystemNotificationCenter.showDirectMessageNotification(
                    context = applicationContext,
                    threadId = threadId,
                    senderName = senderName,
                    senderHandle = senderHandle,
                    messageText = body,
                    conversationTitle = conversationTitle
                )
            }

            CampusPushType.INCOMING_CALL -> {
                val peerId = data["peerId"] ?: data["callerId"] ?: "unknown_peer"
                val callerName = data["callerName"] ?: title
                val callerRole = data["callerRole"] ?: "Verified Student"
                val isVideo = data["isVideo"]?.toBooleanStrictOrNull() ?: true
                val callSessionId = data["callSessionId"] ?: data["sessionId"]

                CampusSystemNotificationCenter.showIncomingCallNotification(
                    context = applicationContext,
                    peerId = peerId,
                    callerName = callerName,
                    callerRoleOrHandle = callerRole,
                    isVideoCall = isVideo,
                    callSessionId = callSessionId
                )
            }

            CampusPushType.FLASH_MEET -> {
                val meetId = data["meetId"] ?: "meet_1"
                val locationName = data["locationName"] ?: "Central Quad Pod #4"
                val attendees = data["attendeeCount"]?.toIntOrNull() ?: 14
                val lat = data["latitude"]?.toDoubleOrNull() ?: -1.3094
                val lng = data["longitude"]?.toDoubleOrNull() ?: 36.8122

                CampusSystemNotificationCenter.showFlashMeetNotification(
                    context = applicationContext,
                    meetId = meetId,
                    title = title,
                    locationName = locationName,
                    attendeeCount = attendees,
                    timeRemainingLabel = data["timeRemaining"] ?: "Starting in 5 min",
                    latitude = lat,
                    longitude = lng
                )
            }

            CampusPushType.SAFEPAY_ORDER -> {
                CampusSystemNotificationCenter.showSafePayOrderNotification(
                    context = applicationContext,
                    orderRef = data["orderRef"] ?: "ORD-4829",
                    itemTitle = data["itemTitle"] ?: title,
                    amountFormatted = data["amount"] ?: "KES 2,500",
                    pickupSpotName = data["pickupSpot"] ?: "North Quad SafeZone",
                    sellerName = data["sellerName"] ?: "Brian Ochieng",
                    statusHeadline = body
                )
            }

            CampusPushType.LIVE_STREAM -> {
                CampusSystemNotificationCenter.showLiveStreamNotification(
                    context = applicationContext,
                    streamId = data["streamId"] ?: "live_1",
                    hostName = data["hostName"] ?: title,
                    streamTitle = body,
                    viewerCount = data["viewerCount"]?.toIntOrNull() ?: 128
                )
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
                CampusSystemNotificationCenter.showFriendRequestNotification(
                    context = applicationContext,
                    peerUserId = data["peerId"] ?: data["actorUserId"] ?: "unknown_peer",
                    peerName = data["senderName"] ?: data["peerName"] ?: title,
                    serverId = data["serverId"],
                    body = body
                )
            }
        }
    }
}
