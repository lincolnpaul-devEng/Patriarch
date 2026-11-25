package com.campus.network.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.campus.network.AppContainer
import com.campus.network.core.model.LiveStream
import com.campus.network.core.network.StreamChatEngine
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.messages.CampusTelecomCallManager
import com.campus.network.feature.messages.MessagesScreenMode
import com.campus.network.feature.messages.MessagesViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.network.core.ui.components.CampusOfflineTopBanner
import com.campus.network.core.ui.components.rememberCampusNetworkState
import com.campus.network.ui.navigation.CampusBottomNavBar
import com.campus.network.ui.navigation.CampusNavHost
import com.campus.network.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun MainAppScreen(
    container: AppContainer,
    streamChatEngine: StreamChatEngine?
) {
    val database = container.database
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val networkState = rememberCampusNetworkState()

    val studentProfileRepository = container.studentProfileRepository

    val telecomCallManager = remember(context) {
        CampusTelecomCallManager(context, coroutineScope)
    }
    val messagesViewModel: MessagesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MessagesViewModel(
                    streamChatEngine = streamChatEngine,
                    telecomCallManager = telecomCallManager,
                    studentProfileRepository = studentProfileRepository,
                    chatMessageDao = database.chatMessageDao(),
                    supabase = container.supabaseClient,
                    serverRepository = container.serverRepository,
                    context = context.applicationContext
                ) as T
            }
        }
    )

    val academicChannelsRepository = container.academicChannelsRepository
    val serverRepository = container.serverRepository
    val mapViewModel: com.campus.network.feature.map.MapViewModel =
        androidx.hilt.navigation.compose.hiltViewModel()
    val channelsViewModel: ChannelsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChannelsViewModel(
                    repository = academicChannelsRepository,
                    serverRepository = serverRepository,
                    studentProfileRepository = studentProfileRepository,
                    serverChannelMessageDao = database.serverChannelMessageDao()
                ) as T
            }
        }
    )
    val currentProfile by remember { studentProfileRepository.getProfile() }
        .collectAsStateWithLifecycle(initialValue = null)
    val activeUserId = remember(currentProfile) {
        currentProfile?.studentId?.takeIf { it.isNotBlank() }
            ?: currentProfile?.universityEmail?.takeIf { it.isNotBlank() }
            .orEmpty()
    }
    val activeEmail = remember(currentProfile) {
        currentProfile?.universityEmail.orEmpty().trim().lowercase()
    }

    androidx.compose.runtime.LaunchedEffect(activeUserId, activeEmail) {
        if (activeUserId.isNotBlank()) {
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.setActiveUserId(context, activeUserId)
            com.campus.network.notifications.CampusNotificationPollManager.setRecipientUserId(
                context,
                activeUserId,
                activeEmail
            )
            com.campus.network.notifications.CampusNotificationPollManager.triggerNextPolledNotification(context)
            container.liveStreamRealtimeCoordinator.start(
                currentUserIdProvider = { activeUserId },
                onOtherUserWentLive = { stream ->
                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.showLiveStreamNotification(
                        context = context,
                        streamId = stream.id,
                        hostName = stream.broadcasterName.ifBlank { "Campus peer" },
                        streamTitle = stream.title,
                        viewerCount = stream.viewerCount.coerceAtLeast(1),
                        categoryTag = stream.category.uppercase()
                    )
                }
            )
            container.mapRealtimeCoordinator.start(
                currentUserIdProvider = { activeUserId },
                friendIdsProvider = { mapViewModel.uiState.value.friends.map { it.id }.toSet() }
            )
        }
    }

    var incomingLiveAlert by remember { mutableStateOf<LiveStream?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        container.liveStreamRealtimeCoordinator.incomingLiveStreams.collect { stream ->
            incomingLiveAlert = stream
        }
    }

    androidx.compose.runtime.LaunchedEffect(incomingLiveAlert?.id) {
        if (incomingLiveAlert != null) {
            delay(7_000L)
            incomingLiveAlert = null
        }
    }

    val pendingFriendsCount by remember(activeUserId) {
        if (activeUserId.isNotBlank()) {
            serverRepository.getPendingReceivedFriendRequestsCount(activeUserId)
        } else {
            kotlinx.coroutines.flow.flowOf(0)
        }
    }.collectAsStateWithLifecycle(initialValue = 0)

    val channelsUiState by channelsViewModel.uiState.collectAsStateWithLifecycle()
    val messagesUiState by messagesViewModel.uiState.collectAsStateWithLifecycle()
    val channelsTabUnread = channelsUiState.activeUserUnreadCount
    val messagesTabUnread = messagesUiState.threads.sumOf { it.unreadCount } + pendingFriendsCount

    var isScreenHidingBottomBar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Route taps on Android System Notifications directly to the target screen
    androidx.compose.runtime.LaunchedEffect(navController) {
        fun navigateDeepLinkToTab(route: String) {
            navController.navigate(route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }

        fun navigateDeepLinkToOverlay(route: String) {
            val currentBase = navController.currentBackStackEntry?.destination?.route
                ?.substringBefore("?")
                ?.substringBefore("/")
            val isFromSplash = currentBase == null || currentBase == Screen.Splash.route
            if (isFromSplash) {
                // Anchor Hub underneath cold-start overlay deep links so Back returns to Hub
                navController.navigate(Screen.Hub.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            navController.navigate(route) {
                launchSingleTop = true
            }
        }

        com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.deepLinkFlow.collect { deepLink ->
            when (deepLink) {
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenDirectChat -> {
                    messagesViewModel.onEvent(
                        com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                            peerId = deepLink.threadId,
                            peerName = deepLink.peerName
                        )
                    )
                    navigateDeepLinkToTab(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenIncomingCall -> {
                    // Answer path must present INCOMING UI + ringtone accept — never Start*Call (outgoing).
                    messagesViewModel.onEvent(
                        com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                            peerId = deepLink.peerId,
                            peerName = deepLink.peerName
                        )
                    )
                    messagesViewModel.onEvent(
                        com.campus.network.feature.messages.MessagesEvent.PresentIncomingCall(
                            sessionId = deepLink.callSessionId?.takeIf { it.isNotBlank() }
                                ?: deepLink.peerId,
                            peerId = deepLink.peerId,
                            peerName = deepLink.peerName,
                            isVideo = deepLink.isVideo
                        )
                    )
                    navigateDeepLinkToTab(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenFlashMeetOnMap -> {
                    mapViewModel.onEvent(
                        com.campus.network.feature.map.MapEvent.FocusCoordinatesOnMap(
                            deepLink.latitude,
                            deepLink.longitude
                        )
                    )
                    navigateDeepLinkToTab(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenSharedLocation -> {
                    mapViewModel.onEvent(
                        com.campus.network.feature.map.MapEvent.RedeemLocationShareToken(
                            deepLink.token
                        )
                    )
                    navigateDeepLinkToTab(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenSafePayMarketplace -> {
                    navigateDeepLinkToOverlay(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenLiveStream -> {
                    navigateDeepLinkToOverlay(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenGuildChannels -> {
                    navigateDeepLinkToTab(resolveNotificationDeepLinkRoute(deepLink))
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenFriendRequest -> {
                    // Friend requests are accepted in Notifications — never invent a fake DM thread.
                    navigateDeepLinkToOverlay(resolveNotificationDeepLinkRoute(deepLink))
                }
            }
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.consumeDeepLink()
        }
    }

    // Following standard messaging app practice (WhatsApp, iMessage, Telegram, patriarch):
    // The bottom tab navigation bar is hidden on messages screens, channel chats, and whenever
    // any drawer sheet is open so the user gets full immersion.
    // Keep bottom tabs visible on Servers hub + Messages threads so unread badges stay discoverable.
    // Hide only during immersive channel chat / deep message screens (same as WhatsApp / Discord).
    val bottomTabRoutes = remember {
        setOf("tab_map", "tab_channels", "tab_messages", "tab_profile")
    }
    val isChannelImmersion = currentRoute == "tab_channels" && channelsUiState.viewMode != ChannelViewMode.CHANNELS_HUB
    val isMessagesImmersion = currentRoute == "tab_messages" &&
        messagesUiState.currentScreen != MessagesScreenMode.THREADS
    val isBottomBarVisible = (currentRoute in bottomTabRoutes) &&
        !isChannelImmersion &&
        !isMessagesImmersion &&
        !isScreenHidingBottomBar

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { _ ->
        com.campus.network.core.ui.components.HomescreenRoundedFrame(modifier = Modifier.fillMaxSize()) {
            CampusNavHost(
                navController = navController,
                container = container,
                streamChatEngine = streamChatEngine,
                messagesViewModel = messagesViewModel,
                channelsViewModel = channelsViewModel,
                mapViewModel = mapViewModel,
                onToggleBottomBar = { /* bottom nav removed */ },
                modifier = Modifier.fillMaxSize()
            )

            // Global Floating Top Offline Toast Banner (Pattern A)
            CampusOfflineTopBanner(
                isOnline = networkState.isOnline,
                showRestoredBanner = networkState.showRestoredBanner,
                onRetryClick = { networkState.triggerRetry() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Real-time Live Broadcast Alert Pill (appears when another student goes live)
            val liveAlert = incomingLiveAlert
            val isInLiveBroadcastOrFeed = currentRoute == Screen.GoLive.route ||
                currentRoute?.startsWith("campus_live_feed") == true
            AnimatedVisibility(
                visible = liveAlert != null && !isInLiveBroadcastOrFeed,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                val alert = liveAlert ?: return@AnimatedVisibility
                val hostLabel = alert.broadcasterName.ifBlank { "Campus peer" }
                Surface(
                    shape = CircleShape,
                    color = Color(0xEC16161C),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6F23C))
                        )
                        Text(
                            text = "LIVE · $hostLabel: ${alert.title}",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "Watch",
                            color = Color(0xFF111111),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFE6F23C))
                                .clickable {
                                    incomingLiveAlert = null
                                    navController.navigate(Screen.LiveFeed.createRoute(alert.id)) {
                                        launchSingleTop = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss live alert",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { incomingLiveAlert = null }
                        )
                    }
                }
            }

            // Global Mood Tracker Mascot Overlay ("What's your vibe today?")
            com.campus.network.core.ui.components.MoodCheckInOverlay(
                canPresent = !isInLiveBroadcastOrFeed
            )
        }
    }
}

internal fun resolveNotificationDeepLinkRoute(
    deepLink: com.campus.network.core.ui.notifications.CampusNotificationDeepLink
): String = when (deepLink) {
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenDirectChat ->
        Screen.Messages.route
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenIncomingCall ->
        Screen.Messages.route
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenFlashMeetOnMap ->
        Screen.Map.route
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenSharedLocation ->
        Screen.Map.route
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenSafePayMarketplace ->
        Screen.Market.createRoute(deepLink.itemId)
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenLiveStream -> {
        val streamId = deepLink.streamId?.trim().orEmpty().ifBlank { "founders" }
        Screen.LiveFeed.createRoute(streamId)
    }
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenGuildChannels ->
        Screen.Channels.route
    is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenFriendRequest ->
        Screen.Notifications.route
}
