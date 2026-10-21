package com.campus.network.feature.hub.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface NotificationDestination {
    data object None : NotificationDestination
    data class FlashMeet(val latitude: Double, val longitude: Double) : NotificationDestination
    data class DirectChat(val peerId: String, val peerName: String) : NotificationDestination
    /** Pending inbound friend request — Accept / Decline inline (not a fake DM). */
    data class FriendRequest(val peerUserId: String, val peerName: String) : NotificationDestination
    data object Marketplace : NotificationDestination
    data object GuildChannel : NotificationDestination
}

data class CampusNotification(
    val id: String,
    val initial: String,
    val avatarBgColor: Color,
    val initialTextColor: Color,
    val title: String,
    val date: String,
    val description: String,
    val isUnread: Boolean,
    val destination: NotificationDestination = NotificationDestination.None
)

/**
 * Screen matching Reference Image 1 precisely:
 * - Top bar with Back arrow, centered "Notifications" title, and Settings gear icon.
 * - Black tooltip popover pointing up to the gear: "Customize your notifications!".
 * - "Previously" section header.
 * - Rounded notification cards with circular avatar badge, title with date & blue unread dot, and body text.
 * - Footer: "Missing notifications? Go to historical notifications."
 */
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMapMeet: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onNavigateToMessages: (peerId: String?, peerName: String?) -> Unit = { _, _ -> },
    onNavigateToHub: () -> Unit = {},
    currentUserId: String = "",
    currentUserEmail: String = "",
    serverRepository: com.campus.network.core.data.ServerRepository? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTooltip by remember { mutableStateOf(true) }
    val cleanUserIdForActions = currentUserId.trim().ifBlank {
        currentUserEmail.trim().lowercase()
    }

    // Real campus notifications list loaded from backend and local database
    val notifications = remember { mutableStateListOf<CampusNotification>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId, currentUserEmail) {
        val cleanUserId = currentUserId.trim()
        val cleanEmail = currentUserEmail.trim().lowercase()
        if (cleanUserId.isBlank() && cleanEmail.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        withContext(Dispatchers.IO) {
            val list = mutableListOf<CampusNotification>()

            // 1. Sync + load pending friend requests (Accept lives here — not in a fake DM)
            if (serverRepository != null && cleanUserId.isNotBlank()) {
                runCatching {
                    serverRepository.syncRemoteFriends(cleanUserId, cleanEmail)
                    val requests = serverRepository.getPendingReceivedFriendRequests(cleanUserId, cleanEmail)
                        .firstOrNull()
                        .orEmpty()
                    requests.forEach { req ->
                        val peerId = req.friendUserId.trim()
                        if (peerId.isBlank()) return@forEach
                        val initial = req.friendDisplayName.trim().take(1).uppercase().ifBlank { "P" }
                        val dateStr = formatNotificationDate(req.lastInteractionEpochMs)
                        list.add(
                            CampusNotification(
                                id = "fr_${req.id}",
                                initial = initial,
                                avatarBgColor = generateNotificationAvatarColor(req.friendDisplayName),
                                initialTextColor = Color.White,
                                title = req.friendDisplayName,
                                date = dateStr,
                                description = "${req.friendDisplayName} sent you a friend request.",
                                isUnread = true,
                                destination = NotificationDestination.FriendRequest(
                                    peerUserId = peerId,
                                    peerName = req.friendDisplayName
                                )
                            )
                        )
                    }
                }
            }

            // 2. Load campus_notifications from Supabase
            runCatching {
                val supabaseClient = com.campus.network.core.network.SupabaseClientProvider()
                val filter = when {
                    cleanUserId.isNotBlank() && cleanEmail.isNotBlank() ->
                        "or=(recipient_user_id.eq.$cleanUserId,recipient_user_id.eq.$cleanEmail)"
                    cleanUserId.isNotBlank() -> "recipient_user_id=eq.$cleanUserId"
                    else -> "recipient_user_id=eq.$cleanEmail"
                }
                val res = supabaseClient.queryTable("campus_notifications?$filter&order=created_at.desc&limit=30")
                res.onSuccess { jsonStr ->
                    runCatching {
                        val arr = org.json.JSONArray(jsonStr)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optString("id")
                            val type = obj.optString("type", "DIRECT_MESSAGE").uppercase()
                            val title = obj.optString("title", "Campus Alert")
                            val body = obj.optString("body", "")
                            val isRead = obj.optBoolean("is_read", false)
                            val actorName = obj.optString("actor_name", "Peer")
                            val actorUserId = obj.optString("actor_user_id", "")
                            val threadId = obj.optString("thread_id", "")
                            val createdAtMs = obj.optLong("created_at_epoch_ms", System.currentTimeMillis())

                            val initial = actorName.trim().take(1).uppercase().ifBlank { "C" }
                            val dateStr = formatNotificationDate(createdAtMs)
                            val dest = when (type) {
                                "FRIEND_REQUEST" -> {
                                    if (actorUserId.isNotBlank()) {
                                        NotificationDestination.FriendRequest(actorUserId, actorName)
                                    } else {
                                        NotificationDestination.None
                                    }
                                }
                                "DIRECT_MESSAGE" -> NotificationDestination.DirectChat(
                                    threadId.ifBlank { actorUserId },
                                    actorName
                                )
                                "FLASH_MEET" -> NotificationDestination.FlashMeet(-1.3094, 36.8122)
                                else -> NotificationDestination.None
                            }

                            // Skip duplicate friend-request rows already loaded from campus_friends
                            val isDupFriend = type == "FRIEND_REQUEST" && (
                                list.any {
                                    it.destination is NotificationDestination.FriendRequest &&
                                        (it.destination as NotificationDestination.FriendRequest)
                                            .peerUserId.equals(actorUserId, ignoreCase = true)
                                } || list.any { it.id == "fr_$actorUserId:$cleanUserId" || it.id == id }
                            )
                            if (!isDupFriend) {
                                list.add(
                                    CampusNotification(
                                        id = id,
                                        initial = initial,
                                        avatarBgColor = generateNotificationAvatarColor(actorName),
                                        initialTextColor = Color.White,
                                        title = title,
                                        date = dateStr,
                                        description = body,
                                        isUnread = !isRead,
                                        destination = dest
                                    )
                                )
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                notifications.clear()
                notifications.addAll(list)
                isLoading = false
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF6F7F9) // Light grayish off-white matching Reference Image 1
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    // Left Back Arrow
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF14171A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Centered Title
                    Text(
                        text = "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14171A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Right Settings Gear Icon
                    IconButton(
                        onClick = {
                            showTooltip = false
                            onNavigateToSettings()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Notification Settings",
                            tint = Color(0xFF14171A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Tooltip Popover positioned underneath the Settings gear (Image 1)
                AnimatedVisibility(
                    visible = showTooltip,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showTooltip = false
                                    onNavigateToSettings()
                                },
                            horizontalAlignment = Alignment.End
                        ) {
                            // Triangle arrow pointer pointing UP towards the Settings gear
                            Canvas(
                                modifier = Modifier
                                    .padding(end = 14.dp)
                                    .size(width = 14.dp, height = 7.dp)
                            ) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(path, color = Color(0xFF191B1F))
                            }

                            // Dark Tooltip Bubble
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF191B1F))
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Customize your notifications!",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Notification Content List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (notifications.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 56.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "No notifications yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "When you receive peer messages, study alerts, or marketplace updates, they'll show up here.",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Previously",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14171A),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // Notification Cards
                    items(notifications, key = { it.id }) { item ->
                        NotificationCard(
                            notification = item,
                            onClick = {
                                // Friend-request cards handle Accept/Decline themselves
                                if (item.destination is NotificationDestination.FriendRequest) return@NotificationCard
                                // Mark as read on click
                                val idx = notifications.indexOfFirst { it.id == item.id }
                                if (idx != -1 && item.isUnread) {
                                    notifications[idx] = item.copy(isUnread = false)
                                    if (!item.id.startsWith("fr_")) {
                                        scope.launch(Dispatchers.IO) {
                                            runCatching {
                                                val client = com.campus.network.core.network.SupabaseClientProvider()
                                                client.updateRecord("campus_notifications?id=eq.${item.id}", """{"is_read":true}""")
                                            }
                                        }
                                    }
                                }
                                when (val dest = item.destination) {
                                    is NotificationDestination.FlashMeet -> {
                                        onNavigateToMapMeet(dest.latitude, dest.longitude)
                                    }
                                    is NotificationDestination.DirectChat -> {
                                        onNavigateToMessages(dest.peerId, dest.peerName)
                                    }
                                    is NotificationDestination.FriendRequest -> Unit
                                    is NotificationDestination.Marketplace -> {
                                        onNavigateToHub()
                                    }
                                    is NotificationDestination.GuildChannel -> {
                                        onNavigateToMessages(null, null)
                                    }
                                    is NotificationDestination.None -> Unit
                                }
                            },
                            onAcceptFriend = { peerUserId, peerName ->
                                scope.launch {
                                    val repo = serverRepository
                                    if (repo == null || cleanUserIdForActions.isBlank()) {
                                        snackbarHostState.showSnackbar("Sign in to accept friend requests")
                                        return@launch
                                    }
                                    withContext(Dispatchers.IO) {
                                        repo.acceptFriendRequest(
                                            userId = cleanUserIdForActions,
                                            friendUserId = peerUserId,
                                            userDisplayName = peerName
                                        )
                                    }
                                    notifications.removeAll {
                                        val dest = it.destination
                                        dest is NotificationDestination.FriendRequest &&
                                            dest.peerUserId.equals(peerUserId, ignoreCase = true)
                                    }
                                    snackbarHostState.showSnackbar("You are now friends with $peerName")
                                }
                            },
                            onDeclineFriend = { peerUserId, peerName ->
                                scope.launch {
                                    val repo = serverRepository
                                    if (repo == null || cleanUserIdForActions.isBlank()) {
                                        snackbarHostState.showSnackbar("Sign in to decline friend requests")
                                        return@launch
                                    }
                                    withContext(Dispatchers.IO) {
                                        repo.declineOrRemoveFriend(
                                            userId = cleanUserIdForActions,
                                            friendUserId = peerUserId
                                        )
                                    }
                                    notifications.removeAll {
                                        val dest = it.destination
                                        dest is NotificationDestination.FriendRequest &&
                                            dest.peerUserId.equals(peerUserId, ignoreCase = true)
                                    }
                                    snackbarHostState.showSnackbar("Declined request from $peerName")
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(28.dp))

                        // Footer matching Reference Image 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Missing notifications?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF14171A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Go to historical notifications.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF007AFF),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("All historical campus archives loaded.")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }

            // Snackbar Host for interactive feedback
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) { data ->
                com.campus.network.core.ui.components.CampusSuccessSnackBanner(
                    message = data.visuals.message,
                    onDismiss = { data.dismiss() }
                )
            }
        }
    }
}

/**
 * Individual Notification Card modeled precisely after Reference Image 1:
 * - Rounded white card container with subtle border.
 * - Circular avatar badge with initial.
 * - Title + Date + Unread blue dot.
 * - Multiline body text.
 */
@Composable
private fun NotificationCard(
    notification: CampusNotification,
    onClick: () -> Unit,
    onAcceptFriend: (peerUserId: String, peerName: String) -> Unit = { _, _ -> },
    onDeclineFriend: (peerUserId: String, peerName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val friendRequest = notification.destination as? NotificationDestination.FriendRequest
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(0.6.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
            .clickable(enabled = friendRequest == null) { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Circular Avatar Badge on Left
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(notification.avatarBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.initial,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = notification.initialTextColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Content Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Header row: Title + Date + Unread Blue Dot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14171A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = " • ${notification.date}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF657786),
                        maxLines = 1
                    )

                    if (notification.isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Multiline description text
                Text(
                    text = notification.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 19.sp,
                    color = Color(0xFF14171A)
                )

                if (friendRequest != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onAcceptFriend(friendRequest.peerUserId, friendRequest.peerName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23A55A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Accept",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onDeclineFriend(friendRequest.peerUserId, friendRequest.peerName)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Decline",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatNotificationDate(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m ago"
        diff < 86_400_000L -> "${(diff / 3_600_000L).coerceAtLeast(1)}h ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(epochMs))
    }
}

private fun generateNotificationAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF16A34A), // Green
        Color(0xFF9333EA), // Purple
        Color(0xFFD97706), // Amber
        Color(0xFFDC2626), // Red
        Color(0xFF0D9488)  // Teal
    )
    val hash = kotlin.math.abs(name.hashCode())
    return colors[hash % colors.size]
}
