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
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val messagesViewModel = remember { MessagesViewModel(streamChatEngine) }
    val messagesUiState by messagesViewModel.uiState.collectAsStateWithLifecycle()

    // Following standard messaging app practice (WhatsApp, iMessage, Telegram):
    // The bottom tab navigation bar is hidden on messages and call screens so the conversation
    // and redesigned message threads get full screen immersion with their floating action dock.
    val bottomTabRoutes = remember {
        setOf("tab_hub", "tab_map", "tab_channels", "tab_profile")
    }
    val isBottomBarVisible = (currentRoute in bottomTabRoutes)

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
            modifier = Modifier.padding(if (isBottomBarVisible) paddingValues else PaddingValues(0.dp))
        )
    }
}
