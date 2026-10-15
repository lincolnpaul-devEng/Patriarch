package com.campus.network.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the 5-Tab cohesive Navigation Graph.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badgeLabel: String? = null
) {
    data object Splash : Screen(
        route = "splash",
        title = "Splash",
        icon = Icons.Default.Home
    )

    data object Onboarding : Screen(
        route = "onboarding",
        title = "Onboarding",
        icon = Icons.Default.Home
    )

    data object OnboardingTransition : Screen(
        route = "onboarding_transition",
        title = "Transition",
        icon = Icons.Default.Home
    )

    data object Auth : Screen(
        route = "auth",
        title = "Auth",
        icon = Icons.Default.Home
    )

    data object Hub : Screen(
        route = "tab_hub",
        title = "Home",
        icon = Icons.Default.Home
    )

    data object Map : Screen(
        route = "tab_map",
        title = "Map",
        icon = Icons.Default.Map,
        badgeLabel = "Live"
    )

    data object Channels : Screen(
        route = "tab_channels",
        title = "Channels",
        icon = Icons.Default.Forum
    )

    data object Messages : Screen(
        route = "tab_messages",
        title = "Messages",
        icon = Icons.Default.Email
    )

    data object Profile : Screen(
        route = "tab_profile",
        title = "Profile",
        icon = Icons.Default.Person
    )

    companion object {
        val bottomTabs = listOf(Hub, Map, Channels, Messages, Profile)
    }
}
