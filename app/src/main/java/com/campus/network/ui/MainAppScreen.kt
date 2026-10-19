package com.campus.network.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.campus.network.core.data.AcademicChannelsRepository
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.messages.CampusTelecomCallManager
import com.campus.network.feature.messages.MessagesScreenMode
import com.campus.network.feature.messages.MessagesViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.campus.network.core.ui.components.CampusOfflineTopBanner
import com.campus.network.core.ui.components.rememberCampusNetworkState
import com.campus.network.ui.navigation.CampusBottomNavBar
import com.campus.network.ui.navigation.CampusNavHost
import com.campus.network.ui.navigation.Screen

@Composable
fun MainAppScreen(
    database: CampusDatabase,
    streamChatEngine: StreamChatEngine?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val networkState = rememberCampusNetworkState()

    val studentProfileRepository = remember(database) {
        com.campus.network.core.data.StudentProfileRepository(
            database = database,
            supabaseClient = com.campus.network.core.network.SupabaseClientProvider()
        )
    }

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
                    studentProfileRepository = studentProfileRepository
                ) as T
            }
        }
    )

    val academicChannelsRepository = remember(database) {
        AcademicChannelsRepository(database)
    }
    val serverRepository = remember(database) {
        com.campus.network.core.data.ServerRepository(database)
    }
    val channelsViewModel: ChannelsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChannelsViewModel(academicChannelsRepository, serverRepository, studentProfileRepository) as T
            }
        }
    )
    val channelsUiState by channelsViewModel.uiState.collectAsStateWithLifecycle()

    var isScreenHidingBottomBar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Route taps on Android System Notifications directly to the target screen
    androidx.compose.runtime.LaunchedEffect(navController) {
        com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.deepLinkFlow.collect { deepLink ->
            when (deepLink) {
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenDirectChat -> {
                    messagesViewModel.onEvent(
                        com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                            peerId = deepLink.threadId,
                            peerName = deepLink.peerName
                        )
                    )
                    navController.navigate(Screen.Messages.route) {
                        launchSingleTop = true
                    }
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenIncomingCall -> {
                    messagesViewModel.onEvent(
                        com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                            peerId = deepLink.peerId,
                            peerName = deepLink.peerName
                        )
                    )
                    messagesViewModel.uiState.value.selectedThread?.let { activeThread ->
                        if (deepLink.isVideo) {
                            messagesViewModel.onEvent(
                                com.campus.network.feature.messages.MessagesEvent.StartVideoCall(activeThread)
                            )
                        } else {
                            messagesViewModel.onEvent(
                                com.campus.network.feature.messages.MessagesEvent.StartAudioCall(activeThread)
                            )
                        }
                    }
                    navController.navigate(Screen.Messages.route) {
                        launchSingleTop = true
                    }
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenFlashMeetOnMap -> {
                    navController.navigate("tab_map?lat=${deepLink.latitude.toFloat()}&lng=${deepLink.longitude.toFloat()}") {
                        launchSingleTop = true
                    }
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenSafePayMarketplace -> {
                    navController.navigate(Screen.Hub.route) {
                        launchSingleTop = true
                    }
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenLiveStream -> {
                    navController.navigate(Screen.LiveFeed.createRoute(deepLink.streamId ?: "founders")) {
                        launchSingleTop = true
                    }
                }
                is com.campus.network.core.ui.notifications.CampusNotificationDeepLink.OpenGuildChannels -> {
                    navController.navigate(Screen.Channels.route) {
                        launchSingleTop = true
                    }
                }
            }
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.consumeDeepLink()
        }
    }

    // Following standard messaging app practice (WhatsApp, iMessage, Telegram, patriarch):
    // The bottom tab navigation bar is hidden on messages screens, channel chats, and whenever
    // any drawer sheet is open so the user gets full immersion.
    val bottomTabRoutes = remember {
        setOf("tab_hub", "tab_map", "tab_channels", "tab_profile")
    }
    val isChannelImmersion = currentRoute == "tab_channels" && channelsUiState.viewMode != ChannelViewMode.CHANNELS_HUB
    val isBottomBarVisible = (currentRoute in bottomTabRoutes) && !isChannelImmersion && !isScreenHidingBottomBar

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically { height -> height } + fadeIn(),
                exit = slideOutVertically { height -> height } + fadeOut()
            ) {
                CampusBottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            CampusNavHost(
                navController = navController,
                database = database,
                streamChatEngine = streamChatEngine,
                messagesViewModel = messagesViewModel,
                channelsViewModel = channelsViewModel,
                studentProfileRepository = studentProfileRepository,
                onToggleBottomBar = { visible -> isScreenHidingBottomBar = !visible },
                modifier = Modifier.padding(
                    bottom = if (isBottomBarVisible) paddingValues.calculateBottomPadding() else 0.dp
                )
            )

            // Global Floating Top Offline Toast Banner (Pattern A)
            CampusOfflineTopBanner(
                isOnline = networkState.isOnline,
                showRestoredBanner = networkState.showRestoredBanner,
                onRetryClick = { networkState.triggerRetry() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
