package com.campus.network.core.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class CampusPushType {
    DIRECT_MESSAGE,
    INCOMING_CALL,
    FLASH_MEET,
    SAFEPAY_ORDER,
    LIVE_STREAM,
    CAMPUS_UPDATE,
    FRIEND_REQUEST
}

data class CampusInlineReplyEvent(
    val threadId: String,
    val peerName: String,
    val replyText: String,
    val timestampLabel: String
)

data class CampusMarkReadEvent(
    val threadId: String
)

sealed interface CampusNotificationDeepLink {
    data class OpenDirectChat(val threadId: String, val peerName: String) : CampusNotificationDeepLink
    data class OpenIncomingCall(
        val peerId: String,
        val peerName: String,
        val isVideo: Boolean,
        val callSessionId: String? = null
    ) : CampusNotificationDeepLink
    data class OpenFlashMeetOnMap(val latitude: Double, val longitude: Double, val title: String) : CampusNotificationDeepLink
    data class OpenSafePayMarketplace(val itemId: String?, val orderRef: String?) : CampusNotificationDeepLink
    data class OpenLiveStream(val streamId: String?) : CampusNotificationDeepLink
    data class OpenGuildChannels(val channelId: String?) : CampusNotificationDeepLink
    data class OpenFriendRequest(
        val peerId: String,
        val peerName: String,
        val serverId: String? = null
    ) : CampusNotificationDeepLink
}

data class QueuedNotificationBubble(
    val senderName: String,
    val text: String,
    val timestampMs: Long,
    val isSelf: Boolean = false
)

/**
 * Central Android System Notification Engine for Patriarch.
 * Supports:
 * 1. WhatsApp-Style Direct & Guild Message Notifications (`MessagingStyle` + `RemoteInput` Inline Reply + `Mark as Read`)
 * 2. Incoming Audio & Video Call Notifications (`CallStyle.forIncomingCall` with Answer & Decline)
 * 3. Map Flash-Meet Proximity Notifications (`BigTextStyle` with "Open on Map" & "Check In")
 * 4. Marketplace SafePay Escrow Notifications (`BigPictureStyle` with "Scan QR" & "Message Seller")
 * 5. Campus Live Stream Broadcast Notifications (`BigPictureStyle` with "Watch Live")
 * 6. Academic & Guild Update Digest Notifications (`InboxStyle`)
 */
object CampusSystemNotificationCenter {

    const val CHANNEL_DIRECT_MESSAGES = "campus_channel_messages"
    const val CHANNEL_INCOMING_CALLS = "campus_channel_calls"
    const val CHANNEL_FLASH_MEETS = "campus_channel_flash_meets"
    const val CHANNEL_SAFEPAY_MARKET = "campus_channel_safepay"
    const val CHANNEL_LIVE_STREAMS = "campus_channel_live_streams"
    const val CHANNEL_CAMPUS_UPDATES = "campus_channel_updates"

    const val ACTION_INLINE_REPLY = "com.campus.network.action.INLINE_REPLY"
    const val ACTION_MARK_AS_READ = "com.campus.network.action.MARK_AS_READ"
    const val ACTION_DECLINE_CALL = "com.campus.network.action.DECLINE_CALL"
    const val ACTION_CHECK_IN_MEET = "com.campus.network.action.CHECK_IN_MEET"

    const val KEY_TEXT_REPLY = "key_whatsapp_style_inline_reply"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_PUSH_TYPE = "extra_push_type"
    const val EXTRA_THREAD_ID = "extra_thread_id"
    const val EXTRA_PEER_NAME = "extra_peer_name"
    const val EXTRA_PEER_HANDLE = "extra_peer_handle"
    const val EXTRA_IS_VIDEO_CALL = "extra_is_video_call"
    const val EXTRA_CALL_SESSION_ID = "extra_call_session_id"
    const val EXTRA_LATITUDE = "extra_latitude"
    const val EXTRA_LONGITUDE = "extra_longitude"
    const val EXTRA_MEET_TITLE = "extra_meet_title"
    const val EXTRA_ORDER_REF = "extra_order_ref"
    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_STREAM_ID = "extra_stream_id"
    const val EXTRA_CHANNEL_ID = "extra_channel_id"
    const val EXTRA_SERVER_ID = "extra_server_id"
    const val EXTRA_PEER_USER_ID = "extra_peer_user_id"

    private const val GROUP_KEY_MESSAGES = "com.campus.network.GROUP_DIRECT_MESSAGES"
    private const val MESSAGE_GROUP_SUMMARY_ID = 1000

    private const val PREFS_NAME = "campus_notification_prefs"
    private const val KEY_PUSH_ENABLED = "push_notifications_enabled"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"
    private const val KEY_ACTIVE_USER_ID = "active_user_id_for_inbox"

    /**
     * Tracks the threadId of the Direct Chat currently open and visible on screen.
     * When non-null, incoming messages for that exact thread do not trigger a noisy heads-up banner.
     */
    @Volatile
    var activeVisibleThreadId: String? = null

    private val threadHistories = ConcurrentHashMap<String, MutableList<QueuedNotificationBubble>>()
    private val threadSenderNames = ConcurrentHashMap<String, String>()
    private val pendingInlineReplies = ConcurrentHashMap<String, MutableList<CampusInlineReplyEvent>>()

    /**
     * In-memory LruCaches for notification avatar and banner bitmaps so repeated messages
     * or inline replies reuse pre-rendered Bitmaps in O(1) with zero GC churn.
     */
    private val avatarBitmapCache = object : android.util.LruCache<String, Bitmap>(24) {}
    private val bannerBitmapCache = object : android.util.LruCache<String, Bitmap>(8) {}

    private val _inlineRepliesFlow = MutableSharedFlow<CampusInlineReplyEvent>(extraBufferCapacity = 32)
    val inlineRepliesFlow: SharedFlow<CampusInlineReplyEvent> = _inlineRepliesFlow.asSharedFlow()

    private val _markReadFlow = MutableSharedFlow<CampusMarkReadEvent>(extraBufferCapacity = 32)
    val markReadFlow: SharedFlow<CampusMarkReadEvent> = _markReadFlow.asSharedFlow()

    // replay = 1 ensures cold-start notification taps emitted in MainActivity.onCreate()
    // are buffered until MainAppScreen mounts its LaunchedEffect collector.
    private val _deepLinkFlow = MutableSharedFlow<CampusNotificationDeepLink>(
        replay = 1,
        extraBufferCapacity = 16
    )
    val deepLinkFlow: SharedFlow<CampusNotificationDeepLink> = _deepLinkFlow.asSharedFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun consumeDeepLink() {
        _deepLinkFlow.resetReplayCache()
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_DIRECT_MESSAGES,
                "Direct & Guild Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant messages and mentions with inline reply support"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 180, 90, 180)
            },
            NotificationChannel(
                CHANNEL_INCOMING_CALLS,
                "Incoming Audio & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen and heads-up alerts for incoming peer calls"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
            },
            NotificationChannel(
                CHANNEL_FLASH_MEETS,
                "Map Flash-Meets & Campus Radar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live study sprints, hackathons, and campus quad meetups near you"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_SAFEPAY_MARKET,
                "SafePay Escrow & Marketplace Orders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Escrow payment locks, QR handoff verification, and buyer messages"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_LIVE_STREAMS,
                "Campus Live Streams & Stories",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when verified students or guilds go live on campus"
            },
            NotificationChannel(
                CHANNEL_CAMPUS_UPDATES,
                "Academic & Guild Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Course-mate matches, helper reputation rewards, and server announcements"
            }
        )

        manager.createNotificationChannels(channels)
    }

    fun isPushEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PUSH_ENABLED, true)
    }

    fun setPushEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PUSH_ENABLED, enabled)
            .apply()
    }

    fun saveFcmToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    fun getFcmToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    fun setActiveUserId(context: Context, userId: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_USER_ID, userId?.trim().orEmpty())
            .apply()
    }

    fun getActiveUserId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_USER_ID, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun consumePendingInlineReplies(threadId: String): List<CampusInlineReplyEvent> {
        return pendingInlineReplies.remove(threadId) ?: emptyList()
    }

    fun consumeAllPendingInlineReplies(): List<CampusInlineReplyEvent> {
        val all = mutableListOf<CampusInlineReplyEvent>()
        val keys = pendingInlineReplies.keys.toList()
        for (k in keys) {
            pendingInlineReplies.remove(k)?.let { all.addAll(it) }
        }
        return all
    }

    /**
     * 1. WHATSAPP-STYLE DIRECT MESSAGE & GUILD MENTION NOTIFICATION
     * Uses `NotificationCompat.MessagingStyle` with stacked conversation history,
     * circular sender avatar, inline `RemoteInput` Reply button, and `Mark as Read`.
     */
    fun showDirectMessageNotification(
        context: Context,
        threadId: String,
        senderName: String,
        senderHandle: String = "",
        messageText: String,
        conversationTitle: String? = null,
        avatarColorArgb: Int = 0xFF014784.toInt(),
        isSilentUpdate: Boolean = false
    ) {
        if (!canPostNotifications(context)) return
        // Suppress duplicate heads-up notification if the user is actively viewing this exact chat thread
        if (!isSilentUpdate && activeVisibleThreadId == threadId) return

        ensureChannelsCreated(context)

        val notificationId = threadId.hashCode().let { if (it < 0) -it else it } + 1001
        threadSenderNames[threadId] = senderName

        val history = threadHistories.getOrPut(threadId) { mutableListOf() }
        synchronized(history) {
            history.add(
                QueuedNotificationBubble(
                    senderName = senderName,
                    text = messageText,
                    timestampMs = System.currentTimeMillis(),
                    isSelf = false
                )
            )
            if (history.size > 6) {
                history.removeAt(0)
            }
        }

        postMessagingStyleNotification(
            context = context,
            notificationId = notificationId,
            threadId = threadId,
            peerName = senderName,
            peerHandle = senderHandle,
            conversationTitle = conversationTitle,
            avatarColorArgb = avatarColorArgb,
            history = history.toList(),
            isSilentUpdate = isSilentUpdate
        )
    }

    private fun postMessagingStyleNotification(
        context: Context,
        notificationId: Int,
        threadId: String,
        peerName: String,
        peerHandle: String,
        conversationTitle: String?,
        avatarColorArgb: Int,
        history: List<QueuedNotificationBubble>,
        isSilentUpdate: Boolean
    ) {
        val selfAvatar = createCircularAvatarBitmap("You", 0xFF0284C7.toInt())
        val peerAvatar = createCircularAvatarBitmap(peerName, avatarColorArgb)

        val selfPerson = Person.Builder()
            .setName("You")
            .setKey("self_user")
            .setIcon(IconCompat.createWithBitmap(selfAvatar))
            .build()

        val peerPerson = Person.Builder()
            .setName(peerName)
            .setKey(threadId)
            .setIcon(IconCompat.createWithBitmap(peerAvatar))
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(selfPerson)
            .setConversationTitle(conversationTitle)
            .setGroupConversation(conversationTitle != null)

        for (bubble in history) {
            val sender = if (bubble.isSelf) selfPerson else peerPerson
            messagingStyle.addMessage(bubble.text, bubble.timestampMs, sender)
        }

        // 1. Tap Notification -> Open Direct Chat Thread in MainActivity
        val openChatIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.DIRECT_MESSAGE.name)
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_PEER_NAME, peerName)
            putExtra(EXTRA_PEER_HANDLE, peerHandle)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. WhatsApp-Style Inline Reply Action with RemoteInput
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to $peerName...")
            .build()

        val replyBroadcastIntent = Intent().apply {
            setClassName(context.packageName, "com.campus.network.notifications.CampusNotificationActionReceiver")
            action = ACTION_INLINE_REPLY
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_PEER_NAME, peerName)
            putExtra(EXTRA_PEER_HANDLE, peerHandle)
        }

        val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10_000,
            replyBroadcastIntent,
            replyFlags
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()

        // 3. "Mark as Read" Action
        val markReadBroadcastIntent = Intent().apply {
            setClassName(context.packageName, "com.campus.network.notifications.CampusNotificationActionReceiver")
            action = ACTION_MARK_AS_READ
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_PEER_NAME, peerName)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20_000,
            markReadBroadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Mark as Read",
            markReadPendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setLargeIcon(peerAvatar)
            .setColor(0xFF014784.toInt())
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(isSilentUpdate)
            .addAction(replyAction)
            .addAction(markReadAction)

        notifySafely(context, notificationId, builder.build())
        postOrUpdateMessageGroupSummary(context)
    }

    private fun postOrUpdateMessageGroupSummary(context: Context) {
        val activeCount = threadHistories.size
        if (activeCount < 2) {
            if (activeCount == 0) {
                NotificationManagerCompat.from(context).cancel(MESSAGE_GROUP_SUMMARY_ID)
            }
            return
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("$activeCount active conversations")
            .setSummaryText("Patriarch Campus Messages")

        var totalMessages = 0
        for ((tId, bubbles) in threadHistories) {
            val peer = threadSenderNames[tId] ?: "Peer"
            val lastBubble = synchronized(bubbles) { bubbles.lastOrNull() }
            if (lastBubble != null) {
                totalMessages += synchronized(bubbles) { bubbles.size }
                inboxStyle.addLine("$peer: ${lastBubble.text}")
            }
        }

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setColor(0xFF014784.toInt())
            .setContentTitle("$activeCount conversations")
            .setContentText("$totalMessages new messages")
            .setStyle(inboxStyle)
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .build()

        notifySafely(context, MESSAGE_GROUP_SUMMARY_ID, summaryNotification)
    }

    /**
     * Called by `CampusNotificationActionReceiver` when the user types an inline reply
     * inside the Android notification shade (WhatsApp-style).
     */
    fun onInlineReplySubmitted(
        context: Context,
        notificationId: Int,
        threadId: String,
        peerName: String,
        peerHandle: String,
        replyText: String
    ) {
        val timeLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        val event = CampusInlineReplyEvent(
            threadId = threadId,
            peerName = peerName,
            replyText = replyText,
            timestampLabel = "Today $timeLabel"
        )

        // Store in pending queue & emit to live SharedFlow so MessagesViewModel updates immediately
        pendingInlineReplies.getOrPut(threadId) { mutableListOf() }.add(event)
        _inlineRepliesFlow.tryEmit(event)

        // Append user's own reply bubble to the notification's MessagingStyle conversation
        val history = threadHistories.getOrPut(threadId) { mutableListOf() }
        synchronized(history) {
            history.add(
                QueuedNotificationBubble(
                    senderName = "You",
                    text = replyText,
                    timestampMs = System.currentTimeMillis(),
                    isSelf = true
                )
            )
            if (history.size > 6) {
                history.removeAt(0)
            }
        }

        // Re-post the notification silently so the Android inline reply spinner stops and displays "You: <replyText>"
        postMessagingStyleNotification(
            context = context,
            notificationId = notificationId,
            threadId = threadId,
            peerName = peerName,
            peerHandle = peerHandle,
            conversationTitle = null,
            avatarColorArgb = 0xFF014784.toInt(),
            history = history.toList(),
            isSilentUpdate = true
        )
    }

    fun onMarkThreadAsRead(context: Context, notificationId: Int, threadId: String) {
        threadHistories.remove(threadId)
        threadSenderNames.remove(threadId)
        _markReadFlow.tryEmit(CampusMarkReadEvent(threadId = threadId))
        NotificationManagerCompat.from(context).cancel(notificationId)
        postOrUpdateMessageGroupSummary(context)
    }

    /**
     * 2. INCOMING AUDIO & VIDEO CALL NOTIFICATION (`CallStyle.forIncomingCall`)
     */
    fun showIncomingCallNotification(
        context: Context,
        peerId: String,
        callerName: String,
        callerRoleOrHandle: String,
        isVideoCall: Boolean,
        callSessionId: String? = null
    ) {
        if (!canPostNotifications(context)) return
        ensureChannelsCreated(context)

        val notificationId = 2001 + (peerId.hashCode().let { if (it < 0) -it else it } % 900)
        val callerAvatar = createCircularAvatarBitmap(callerName, 0xFF059669.toInt())

        val callerPerson = Person.Builder()
            .setName(callerName)
            .setKey(peerId)
            .setIcon(IconCompat.createWithBitmap(callerAvatar))
            .setImportant(true)
            .build()

        // Answer Intent -> Opens MainActivity in Incoming Call Mode (Accept/Decline — not outgoing dial)
        val answerIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.INCOMING_CALL.name)
            putExtra(EXTRA_THREAD_ID, peerId)
            putExtra(EXTRA_PEER_NAME, callerName)
            putExtra(EXTRA_IS_VIDEO_CALL, isVideoCall)
            if (!callSessionId.isNullOrBlank()) putExtra(EXTRA_CALL_SESSION_ID, callSessionId)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline Intent -> Cancels Call Notification via BroadcastReceiver
        val declineIntent = Intent().apply {
            setClassName(context.packageName, "com.campus.network.notifications.CampusNotificationActionReceiver")
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_THREAD_ID, peerId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callTypeLabel = if (isVideoCall) "Incoming Campus Video Call" else "Incoming Campus Audio Call"

        val builder = NotificationCompat.Builder(context, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setLargeIcon(callerAvatar)
            .setContentTitle(callerName)
            .setContentText("$callTypeLabel • $callerRoleOrHandle")
            .setColor(0xFF059669.toInt())
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(answerPendingIntent)
            .setFullScreenIntent(answerPendingIntent, true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson,
                    declinePendingIntent,
                    answerPendingIntent
                ).setIsVideo(isVideoCall)
            )

        notifySafely(context, notificationId, builder.build())
    }

    /**
     * 3. MAP FLASH-MEET & PROXIMITY RADAR NOTIFICATION (`BigTextStyle` + Map Deep-Link)
     */
    fun showFlashMeetNotification(
        context: Context,
        meetId: String,
        title: String,
        locationName: String,
        attendeeCount: Int,
        timeRemainingLabel: String,
        latitude: Double,
        longitude: Double
    ) {
        if (!canPostNotifications(context)) return
        ensureChannelsCreated(context)

        val notificationId = 3000 + (meetId.hashCode().let { if (it < 0) -it else it } % 900)
        val pinBadgeBitmap = createCircularAvatarBitmap("FM", 0xFF0284C7.toInt())

        val openMapIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.FLASH_MEET.name)
            putExtra(EXTRA_LATITUDE, latitude)
            putExtra(EXTRA_LONGITUDE, longitude)
            putExtra(EXTRA_MEET_TITLE, title)
        }
        val openMapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openMapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val checkInIntent = Intent().apply {
            setClassName(context.packageName, "com.campus.network.notifications.CampusNotificationActionReceiver")
            action = ACTION_CHECK_IN_MEET
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_MEET_TITLE, title)
        }
        val checkInPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 500,
            checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val detailedBody = "$locationName • $attendeeCount students checked in ($timeRemainingLabel). Tap to view live pin on the campus radar map."

        val builder = NotificationCompat.Builder(context, CHANNEL_FLASH_MEETS)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setLargeIcon(pinBadgeBitmap)
            .setColor(0xFF0284C7.toInt())
            .setContentTitle("Flash-Meet: $title")
            .setContentText("$locationName • $attendeeCount students active")
            .setSubText("Campus Radar")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Flash-Meet Starting: $title")
                    .bigText(detailedBody)
                    .setSummaryText("$attendeeCount peers nearby")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(openMapPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_dialog_map,
                "Open on Map",
                openMapPendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Check In",
                checkInPendingIntent
            )

        notifySafely(context, notificationId, builder.build())
    }

    /**
     * 4. SAFEPAY ESCROW & MARKETPLACE ORDER NOTIFICATION (`BigPictureStyle`)
     */
    fun showSafePayOrderNotification(
        context: Context,
        orderRef: String,
        itemTitle: String,
        amountFormatted: String,
        pickupSpotName: String,
        sellerName: String,
        statusHeadline: String = "Escrow Locked • Ready for QR Handoff"
    ) {
        if (!canPostNotifications(context)) return
        ensureChannelsCreated(context)

        val notificationId = 4000 + (orderRef.hashCode().let { if (it < 0) -it else it } % 900)
        val avatarBitmap = createCircularAvatarBitmap("SP", 0xFF059669.toInt())
        val bannerBitmap = createRichBannerBitmap(
            badgeLabel = "SAFEPAY ESCROW • $orderRef",
            headline = itemTitle,
            subline = "$amountFormatted • Pickup at $pickupSpotName",
            startColorArgb = 0xFF014784.toInt(),
            endColorArgb = 0xFF059669.toInt()
        )

        val openOrderIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.SAFEPAY_ORDER.name)
            putExtra(EXTRA_ORDER_REF, orderRef)
            putExtra(EXTRA_PEER_NAME, sellerName)
        }
        val openOrderPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openOrderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chatSellerIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.DIRECT_MESSAGE.name)
            putExtra(EXTRA_THREAD_ID, "thread_brian")
            putExtra(EXTRA_PEER_NAME, sellerName)
        }
        val chatSellerPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 300,
            chatSellerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SAFEPAY_MARKET)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setLargeIcon(avatarBitmap)
            .setColor(0xFF059669.toInt())
            .setSubText("SafePay Escrow")
            .setContentTitle("$statusHeadline ($amountFormatted)")
            .setContentText("$itemTitle • Meet $sellerName at $pickupSpotName")
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .setBigContentTitle("$statusHeadline • $amountFormatted")
                    .setSummaryText("$itemTitle • Pickup at $pickupSpotName")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openOrderPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_camera,
                "Scan QR Code",
                openOrderPendingIntent
            )
            .addAction(
                android.R.drawable.sym_action_chat,
                "Message Seller",
                chatSellerPendingIntent
            )

        notifySafely(context, notificationId, builder.build())
    }

    /**
     * 5. LIVE CAMPUS STREAM & BROADCAST NOTIFICATION (`BigPictureStyle`)
     */
    fun showLiveStreamNotification(
        context: Context,
        streamId: String,
        hostName: String,
        streamTitle: String,
        viewerCount: Int,
        categoryTag: String = "CAMPUS LIVE"
    ) {
        if (!canPostNotifications(context)) return
        ensureChannelsCreated(context)

        val notificationId = 5000 + (streamId.hashCode().let { if (it < 0) -it else it } % 900)
        val hostAvatar = createCircularAvatarBitmap(hostName, 0xFFE11D48.toInt())
        val streamBanner = createRichBannerBitmap(
            badgeLabel = "LIVE NOW • $viewerCount WATCHING",
            headline = streamTitle,
            subline = "Hosted by $hostName • $categoryTag",
            startColorArgb = 0xFF0F172A.toInt(),
            endColorArgb = 0xFFE11D48.toInt()
        )

        val watchIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.LIVE_STREAM.name)
            putExtra(EXTRA_STREAM_ID, streamId)
        }
        val watchPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            watchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_STREAMS)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setLargeIcon(hostAvatar)
            .setColor(0xFFE11D48.toInt())
            .setSubText("LIVE • $viewerCount watching")
            .setContentTitle("$hostName is live: $streamTitle")
            .setContentText("Tap to join the live campus broadcast ($viewerCount students watching)")
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(streamBanner)
                    .setBigContentTitle("$hostName is LIVE on Campus")
                    .setSummaryText("$streamTitle • $viewerCount students watching")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(watchPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "Watch Live",
                watchPendingIntent
            )

        notifySafely(context, notificationId, builder.build())
    }

    /**
     * 6. ACADEMIC & GUILD UPDATES DIGEST NOTIFICATION (`InboxStyle`)
     */
    fun showCampusUpdateNotification(
        context: Context,
        updateId: String,
        guildName: String,
        headline: String,
        digestLines: List<String>
    ) {
        if (!canPostNotifications(context)) return
        ensureChannelsCreated(context)

        val notificationId = 6000 + (updateId.hashCode().let { if (it < 0) -it else it } % 900)
        val guildAvatar = createCircularAvatarBitmap(guildName, 0xFF5865F2.toInt())

        val openGuildIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.CAMPUS_UPDATE.name)
            putExtra(EXTRA_CHANNEL_ID, updateId)
        }
        val openGuildPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openGuildIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(guildName)
            .setSummaryText("${digestLines.size} new academic updates")
        digestLines.forEach { line -> inboxStyle.addLine(line) }

        val builder = NotificationCompat.Builder(context, CHANNEL_CAMPUS_UPDATES)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setLargeIcon(guildAvatar)
            .setColor(0xFF5865F2.toInt())
            .setSubText(guildName)
            .setContentTitle(headline)
            .setContentText(digestLines.firstOrNull() ?: headline)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openGuildPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "Open Channels",
                openGuildPendingIntent
            )

        notifySafely(context, notificationId, builder.build())
    }

    /**
     * Friend request from a server member — opens Notifications so the user can Accept / Decline.
     */
    fun showFriendRequestNotification(
        context: Context,
        peerUserId: String,
        peerName: String,
        serverId: String? = null,
        body: String? = null
    ) {
        if (!canPostNotifications(context)) return
        ensureChannelsCreated(context)

        val notificationId = 7100 + (peerUserId.hashCode().let { if (it < 0) -it else it } % 800)
        val avatar = createCircularAvatarBitmap(peerName, 0xFF22C55E.toInt())
        val openIntent = createMainActivityIntent(context).apply {
            putExtra(EXTRA_PUSH_TYPE, CampusPushType.FRIEND_REQUEST.name)
            putExtra(EXTRA_PEER_USER_ID, peerUserId)
            putExtra(EXTRA_PEER_NAME, peerName)
            if (!serverId.isNullOrBlank()) putExtra(EXTRA_SERVER_ID, serverId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CAMPUS_UPDATES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setLargeIcon(avatar)
            .setColor(0xFF22C55E.toInt())
            .setContentTitle("Friend request")
            .setContentText(body ?: "$peerName sent you a friend request")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body ?: "$peerName wants to connect on Campus")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        notifySafely(context, notificationId, builder.build())
    }

    fun emitDeepLinkFromIntent(intent: Intent?) {
        val typeStr = intent?.getStringExtra(EXTRA_PUSH_TYPE) ?: return
        val type = runCatching { CampusPushType.valueOf(typeStr) }.getOrNull() ?: return

        val deepLink = when (type) {
            CampusPushType.DIRECT_MESSAGE -> {
                val threadId = intent.getStringExtra(EXTRA_THREAD_ID) ?: "thread_mwangi"
                val peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Prof. Mwangi"
                CampusNotificationDeepLink.OpenDirectChat(threadId = threadId, peerName = peerName)
            }
            CampusPushType.INCOMING_CALL -> {
                val peerId = intent.getStringExtra(EXTRA_THREAD_ID) ?: "thread_faith"
                val peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Faith Wangari"
                val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, true)
                val sessionId = intent.getStringExtra(EXTRA_CALL_SESSION_ID)
                CampusNotificationDeepLink.OpenIncomingCall(
                    peerId = peerId,
                    peerName = peerName,
                    isVideo = isVideo,
                    callSessionId = sessionId
                )
            }
            CampusPushType.FLASH_MEET -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, -1.3094)
                val lng = intent.getDoubleExtra(EXTRA_LONGITUDE, 36.8122)
                val title = intent.getStringExtra(EXTRA_MEET_TITLE) ?: "CS Finals Study Sprint"
                CampusNotificationDeepLink.OpenFlashMeetOnMap(latitude = lat, longitude = lng, title = title)
            }
            CampusPushType.SAFEPAY_ORDER -> {
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
                val orderRef = intent.getStringExtra(EXTRA_ORDER_REF)
                CampusNotificationDeepLink.OpenSafePayMarketplace(itemId = itemId, orderRef = orderRef)
            }
            CampusPushType.LIVE_STREAM -> {
                val streamId = intent.getStringExtra(EXTRA_STREAM_ID)
                CampusNotificationDeepLink.OpenLiveStream(streamId = streamId)
            }
            CampusPushType.CAMPUS_UPDATE -> {
                val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
                CampusNotificationDeepLink.OpenGuildChannels(channelId = channelId)
            }
            CampusPushType.FRIEND_REQUEST -> {
                val peerId = intent.getStringExtra(EXTRA_PEER_USER_ID)
                    ?: intent.getStringExtra(EXTRA_THREAD_ID)
                    ?: "unknown_peer"
                val peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Campus peer"
                val serverId = intent.getStringExtra(EXTRA_SERVER_ID)
                CampusNotificationDeepLink.OpenFriendRequest(
                    peerId = peerId,
                    peerName = peerName,
                    serverId = serverId
                )
            }
        }
        _deepLinkFlow.tryEmit(deepLink)
    }

    private fun createMainActivityIntent(context: Context): Intent {
        return Intent().apply {
            setClassName(context.packageName, "com.campus.network.MainActivity")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (!isPushEnabled(context)) return false
        return hasNotificationPermission(context)
    }

    private fun notifySafely(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun createCircularAvatarBitmap(name: String, bgColorArgb: Int): Bitmap {
        val cacheKey = "${name.trim().lowercase(Locale.US)}_$bgColorArgb"
        synchronized(avatarBitmapCache) {
            avatarBitmapCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return cached
            }
        }

        val sizePx = 144
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColorArgb
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint)

        val initials = name
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "P" }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = textPaint.fontMetrics
        val baselineY = (sizePx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(initials, sizePx / 2f, baselineY, textPaint)

        synchronized(avatarBitmapCache) {
            avatarBitmapCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    private fun createRichBannerBitmap(
        badgeLabel: String,
        headline: String,
        subline: String,
        startColorArgb: Int,
        endColorArgb: Int
    ): Bitmap {
        val cacheKey = "${badgeLabel}_${headline}_${subline}_${startColorArgb}_$endColorArgb"
        synchronized(bannerBitmapCache) {
            bannerBitmapCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return cached
            }
        }

        val width = 800
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                startColorArgb, endColorArgb,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 28f, 28f, bgPaint)

        // Badge pill
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
        }
        canvas.drawRoundRect(RectF(40f, 38f, 440f, 92f), 27f, 27f, pillPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(badgeLabel.take(30), 62f, 73f, badgeTextPaint)

        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(headline.take(32), 40f, 185f, headlinePaint)

        val sublinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(subline.take(46), 40f, 248f, sublinePaint)

        synchronized(bannerBitmapCache) {
            bannerBitmapCache.put(cacheKey, bitmap)
        }
        return bitmap
    }
}
