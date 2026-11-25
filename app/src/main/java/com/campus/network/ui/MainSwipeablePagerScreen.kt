package com.campus.network.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.data.ServerRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.ui.components.HomescreenBackground
import com.campus.network.core.ui.components.ScreenNavIndicatorDots
import com.campus.network.feature.channels.ChannelsScreen
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.hub.bestfootball.BestFootballMatchScreen
import com.campus.network.feature.hub.bestfootball.UnreadFriendMessage
import com.campus.network.feature.messages.ThreadCategory
import com.campus.network.feature.map.MapScreen
import com.campus.network.feature.map.MapViewModel
import com.campus.network.feature.messages.MessagesScreen
import com.campus.network.feature.messages.MessagesViewModel
import com.campus.network.feature.profile.ProfileScreen
import com.campus.network.feature.profile.ProfileViewModel
import com.campus.network.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainSwipeablePagerScreen(
    currentProfile: StudentProfile?,
    mapViewModel: MapViewModel,
    channelsViewModel: ChannelsViewModel,
    messagesViewModel: MessagesViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavHostController,
    serverRepository: ServerRepository,
    studentProfileRepository: StudentProfileRepository,
    context: Context,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    escrowOrdersLoader: suspend () -> List<com.campus.network.core.model.EscrowOrder> = { emptyList() },
    campusStats: com.campus.network.core.data.CampusStatsRepository.CampusStats =
        com.campus.network.core.data.CampusStatsRepository.CampusStats.Empty
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage) { 5 }
    val messagesUiState by messagesViewModel.uiState.collectAsState()
    val unreadFriendMessages = messagesUiState.threads
        .asSequence()
        .filter { it.category == ThreadCategory.DIRECT && it.unreadCount > 0 && it.peerName.isNotBlank() }
        .sortedByDescending { it.unreadCount }
        .map { thread ->
            UnreadFriendMessage(
                id = thread.id,
                name = thread.peerName,
                avatarUrl = thread.avatarUrl,
                unreadCount = thread.unreadCount
            )
        }
        .toList()

    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch { pagerState.animateScrollToPage(0) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = true,
        beyondViewportPageCount = 2,
        key = { page -> page }
    ) { page ->
        when (page) {
            0 -> {
                // Page 0: Homescreen (BestFootballMatchScreen)
                BestFootballMatchScreen(
                    userFullName = currentProfile?.fullName,
                    userAvatarUrl = currentProfile?.avatarUrl,
                    campusStats = campusStats,
                    unreadFriendMessages = unreadFriendMessages,
                    isActivePage = pagerState.currentPage == 0,
                    onNavigateToProfile = {
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                    },
                    onNavigateToGoLive = {
                        navController.navigate(Screen.LiveRoom.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToChannels = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onNavigateToMap = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onNavigateToMessages = {
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    },
                    onNavigateToMarket = {
                        navController.navigate(Screen.Market.createRoute()) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            1 -> {
                // Page 1: Channels (Servers)
                HomescreenBackground {
                    ChannelsScreen(
                        viewModel = channelsViewModel,
                        isActivePage = pagerState.currentPage == 1,
                        onNavigateToChannelChat = {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onNavigateToMessages = {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onNavigateToProfile = {
                            coroutineScope.launch { pagerState.animateScrollToPage(4) }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onNavigateToDirectChat = { peerId: String, peerName: String, peerHandle: String?, serverName: String?, serverTag: String? ->
                            messagesViewModel.onEvent(
                                com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                                    peerId = peerId,
                                    peerName = peerName,
                                    peerHandle = peerHandle,
                                    serverName = serverName,
                                    serverTag = serverTag
                                )
                            )
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )

                    ScreenNavIndicatorDots(
                        selectedPage = pagerState.currentPage,
                        onSelectPage = { targetPage ->
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            2 -> {
                // Page 2: Map
                HomescreenBackground {
                    MapScreen(
                        viewModel = mapViewModel,
                        isActivePage = pagerState.currentPage == 2,
                        hasUnreadNotifications = unreadFriendMessages.isNotEmpty(),
                        onNavigateToMessages = {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onOpenDirectChat = { peerId, peerName ->
                            if (peerId.isNotBlank()) {
                                messagesViewModel.onEvent(
                                    com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                                        peerId = peerId,
                                        peerName = peerName,
                                        peerHandle = "@${peerId.trim()}"
                                    )
                                )
                            }
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onNavigateToProfile = {
                            coroutineScope.launch { pagerState.animateScrollToPage(4) }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onToggleBottomBar = {},
                        onRetrySync = {
                            coroutineScope.launch {
                                DeltaSyncManager.get(context).retryFailedSyncs()
                            }
                        }
                    )

                    ScreenNavIndicatorDots(
                        selectedPage = pagerState.currentPage,
                        onSelectPage = { targetPage ->
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            3 -> {
                // Page 3: Messages
                HomescreenBackground {
                    MessagesScreen(
                        viewModel = messagesViewModel,
                        isActivePage = pagerState.currentPage == 3,
                        onNavigateBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onNavigateToProfile = {
                            coroutineScope.launch { pagerState.animateScrollToPage(4) }
                        },
                        onNavigateToServer = { serverId: String ->
                            if (serverId.isNotBlank()) {
                                channelsViewModel.onEvent(
                                    com.campus.network.feature.channels.ChannelsEvent.SelectServer(serverId)
                                )
                            }
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onAddServer = {
                            channelsViewModel.onEvent(
                                com.campus.network.feature.channels.ChannelsEvent.AddServerClick
                            )
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )

                    ScreenNavIndicatorDots(
                        selectedPage = pagerState.currentPage,
                        onSelectPage = { targetPage ->
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            4 -> {
                // Page 4: Profile
                HomescreenBackground {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        escrowOrdersLoader = escrowOrdersLoader,
                        isActivePage = pagerState.currentPage == 4,
                        onBackClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onRetrySync = {
                            coroutineScope.launch {
                                profileViewModel.retryFailedUploads()
                            }
                        },
                        onSignOut = {
                            coroutineScope.launch {
                                profileViewModel.resetDeletionState()
                                val authClient = com.campus.network.core.network.SupabaseGoogleAuthClient(context)
                                authClient.signOut()
                                studentProfileRepository.signOut()
                                serverRepository.clearLocalCache()
                                channelsViewModel.clearSession()
                                profileViewModel.onEvent(com.campus.network.feature.profile.ProfileEvent.SignOut)
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )

                    ScreenNavIndicatorDots(
                        selectedPage = pagerState.currentPage,
                        onSelectPage = { targetPage ->
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
