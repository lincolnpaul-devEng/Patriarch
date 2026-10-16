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
import androidx.compose.runtime.remember
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

    val telecomCallManager = remember(context) {
        CampusTelecomCallManager(context, coroutineScope)
    }
    val messagesViewModel = remember(streamChatEngine) {
        MessagesViewModel(
            streamChatEngine = streamChatEngine,
            telecomCallManager = telecomCallManager
        )
    }
    val messagesUiState by messagesViewModel.uiState.collectAsStateWithLifecycle()

    val academicChannelsRepository = remember(database) {
        AcademicChannelsRepository(database)
    }
    val channelsViewModel = remember(academicChannelsRepository) {
        ChannelsViewModel(academicChannelsRepository)
    }
    val channelsUiState by channelsViewModel.uiState.collectAsStateWithLifecycle()

    // Following standard messaging app practice (WhatsApp, iMessage, Telegram, Discord):
    // The bottom tab navigation bar is hidden on messages screens and channel chats so the
    // conversation gets full screen immersion with the floating action dock elevating with IME.
    val bottomTabRoutes = remember {
        setOf("tab_hub", "tab_map", "tab_channels", "tab_profile")
    }
    val isChannelImmersion = currentRoute == "tab_channels" && channelsUiState.viewMode != ChannelViewMode.CHANNELS_HUB
    val isBottomBarVisible = (currentRoute in bottomTabRoutes) && !isChannelImmersion

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
        CampusNavHost(
            navController = navController,
            database = database,
            streamChatEngine = streamChatEngine,
            messagesViewModel = messagesViewModel,
            channelsViewModel = channelsViewModel,
            modifier = Modifier.padding(
                bottom = if (isBottomBarVisible) paddingValues.calculateBottomPadding() else 0.dp
            )
        )
    }
}
