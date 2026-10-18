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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch

sealed interface NotificationDestination {
    data object None : NotificationDestination
    data class FlashMeet(val latitude: Double, val longitude: Double) : NotificationDestination
    data class DirectChat(val peerId: String, val peerName: String) : NotificationDestination
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
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTooltip by remember { mutableStateOf(true) }

    // Campus tailored notifications matching the reference card style
    val notifications = remember {
        mutableStateListOf(
            CampusNotification(
                id = "1",
                initial = "S.",
                avatarBgColor = Color(0xFFFFD1DC), // Soft pastel matching reference badge
                initialTextColor = Color(0xFF14171A),
                title = "SafePay payment: KES 2,500",
                date = "Dec 16, 2023",
                description = "KES 2,500 released for your TI-84 Plus Calculator. Buyer confirmed handoff pickup at North Quad.",
                isUnread = true,
                destination = NotificationDestination.Marketplace
            ),
            CampusNotification(
                id = "2",
                initial = "G.",
                avatarBgColor = Color(0xFFFFD1DC),
                initialTextColor = Color(0xFF14171A),
                title = "Campus Gig claimed",
                date = "Dec 12, 2023",
                description = "Sarah N. accepted your notes digitization gig. Chat room is ready to coordinate task delivery.",
                isUnread = true,
                destination = NotificationDestination.DirectChat(peerId = "user_sarah_n", peerName = "Sarah Nduta")
            ),
            CampusNotification(
                id = "3",
                initial = "K.",
                avatarBgColor = Color(0xFFFFD1DC),
                initialTextColor = Color(0xFF14171A),
                title = "Strathmore Tech & AI Guild",
                date = "Dec 8, 2023",
                description = "Now you are an active member of Strathmore AI Guild, start earning peer reputation points with every shared solution.",
                isUnread = true,
                destination = NotificationDestination.GuildChannel
            ),
            CampusNotification(
                id = "4",
                initial = "M.",
                avatarBgColor = Color(0xFFFFD1DC),
                initialTextColor = Color(0xFF14171A),
                title = "Flash Meet starting soon",
                date = "Dec 5, 2023",
                description = "Campus Hackathon Pitch Huddle is starting at Innovation Hub Lab 3. 14 students are already checked in.",
                isUnread = false,
                destination = NotificationDestination.FlashMeet(latitude = -1.3078, longitude = 36.8121)
            ),
            CampusNotification(
                id = "5",
                initial = "F.",
                avatarBgColor = Color(0xFFFFD1DC),
                initialTextColor = Color(0xFF14171A),
                title = "CS Finals Study Sprint in 5 min",
                date = "Now",
                description = "Flash Meet CS Finals Study Sprint at Central Quad Pod #4. Tap to view location pin and join now!",
                isUnread = true,
                destination = NotificationDestination.FlashMeet(latitude = -1.3094, longitude = 36.8122)
            )
        )
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
                    item {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        var pendingPushAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            if (granted) {
                                com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.setPushEnabled(context, true)
                                pendingPushAction?.invoke()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Allow notification permission to receive push alerts")
                                }
                            }
                            pendingPushAction = null
                        }

                        val runWithNotificationPermission: (() -> Unit) -> Unit = { action ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    pendingPushAction = action
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    action()
                                }
                            } else {
                                action()
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live Device Notification Shade Trigger Strip (Polling + FCM Preview)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Device Push & Inline Reply Channels",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Tap any channel below to dispatch a live OS notification to your phone shade (includes WhatsApp-style inline reply for messages).",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF014784),
                                            modifier = Modifier.clickable {
                                                runWithNotificationPermission {
                                                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showDirectMessageNotification(
                                                        context = context,
                                                        threadId = "thread_mwangi",
                                                        senderName = "Prof. Mwangi (Supervisor)",
                                                        senderHandle = "@prof_mwangi",
                                                        messageText = "Please review chapter 4 citations before tomorrow's seminar. Reply directly from your notification shade!",
                                                        avatarColorArgb = 0xFF014784.toInt()
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Sent Message Push • Pull down shade to Reply inline!")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "Message (Inline Reply)",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }

                                    item {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.clickable {
                                                runWithNotificationPermission {
                                                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showIncomingCallNotification(
                                                        context = context,
                                                        peerId = "thread_faith",
                                                        callerName = "Faith Wangari",
                                                        callerRoleOrHandle = "Lab Partner • Video Call",
                                                        isVideoCall = true
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Sent Incoming Call Push to device shade")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "Incoming Call",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }

                                    item {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier.clickable {
                                                runWithNotificationPermission {
                                                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showFlashMeetNotification(
                                                        context = context,
                                                        meetId = "meet_cs_finals",
                                                        title = "CS Finals Study Sprint",
                                                        locationName = "Central Quad Pod #4",
                                                        attendeeCount = 14,
                                                        timeRemainingLabel = "Starting in 5 min",
                                                        latitude = -1.3094,
                                                        longitude = 36.8122
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Sent Map Flash-Meet Push to device shade")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "Map Flash-Meet",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }

                                    item {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.clickable {
                                                runWithNotificationPermission {
                                                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showSafePayOrderNotification(
                                                        context = context,
                                                        orderRef = "ORD-8492",
                                                        itemTitle = "TI-84 Plus Calculator",
                                                        amountFormatted = "KES 2,500",
                                                        pickupSpotName = "North Quad SafeZone",
                                                        sellerName = "Brian Ochieng"
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Sent SafePay Escrow Push to device shade")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "SafePay Escrow",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }

                                    item {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFE11D48),
                                            modifier = Modifier.clickable {
                                                runWithNotificationPermission {
                                                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showLiveStreamNotification(
                                                        context = context,
                                                        streamId = "live_robotics",
                                                        hostName = "Strathmore Robotics Lab",
                                                        streamTitle = "Autonomous Drone Quad Flight Test",
                                                        viewerCount = 214
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Sent Live Stream Push to device shade")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "Live Stream",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }

                                    item {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF7C3AED),
                                            modifier = Modifier.clickable {
                                                runWithNotificationPermission {
                                                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showCampusUpdateNotification(
                                                        context = context,
                                                        updateId = "upd_guild",
                                                        guildName = "Strathmore Tech & AI Guild",
                                                        headline = "3 New Guild Activity Updates",
                                                        digestLines = listOf(
                                                            "Sarah N. accepted your notes digitization gig",
                                                            "Hackathon Pitch Huddle check-in opened at Lab 3",
                                                            "You earned +25 peer reputation points"
                                                        )
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Sent Guild Digest Push to device shade")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "Guild Digest",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Section Header: "Previously"
                        Text(
                            text = "Previously",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14171A),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    // Notification Cards
                    items(notifications, key = { it.id }) { item ->
                        NotificationCard(
                            notification = item,
                            onClick = {
                                // Mark as read on click
                                val idx = notifications.indexOfFirst { it.id == item.id }
                                if (idx != -1 && item.isUnread) {
                                    notifications[idx] = item.copy(isUnread = false)
                                }
                                when (val dest = item.destination) {
                                    is NotificationDestination.FlashMeet -> {
                                        onNavigateToMapMeet(dest.latitude, dest.longitude)
                                    }
                                    is NotificationDestination.DirectChat -> {
                                        onNavigateToMessages(dest.peerId, dest.peerName)
                                    }
                                    is NotificationDestination.Marketplace -> {
                                        onNavigateToHub()
                                    }
                                    is NotificationDestination.GuildChannel -> {
                                        onNavigateToMessages(null, null)
                                    }
                                    is NotificationDestination.None -> Unit
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
    modifier: Modifier = Modifier
) {
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
            .clickable { onClick() }
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
            }
        }
    }
}
