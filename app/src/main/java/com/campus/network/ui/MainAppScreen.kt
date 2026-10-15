package com.campus.network.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
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

    val isBottomBarVisible = currentRoute in Screen.bottomTabs.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
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
            modifier = Modifier.padding(if (isBottomBarVisible) paddingValues else PaddingValues(0.dp))
        )
    }
}
