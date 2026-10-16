package com.campus.network.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.campus.network.core.data.AcademicChannelsRepository
import com.campus.network.core.data.CampusHubRepository
import com.campus.network.core.data.FlashMeetRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
import com.campus.network.feature.channels.ChannelsScreen
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.hub.HubScreen
import com.campus.network.feature.hub.HubViewModel
import com.campus.network.feature.map.MapScreen
import com.campus.network.feature.map.MapViewModel
import com.campus.network.feature.messages.MessagesScreen
import com.campus.network.feature.messages.MessagesViewModel
import com.campus.network.feature.profile.ProfileScreen
import com.campus.network.feature.profile.ProfileViewModel

import com.campus.network.ui.auth.AuthScreen
import com.campus.network.ui.auth.OnboardingScreen
import com.campus.network.ui.auth.OnboardingTransitionScreen
import com.campus.network.ui.auth.SplashScreen

/**
 * Cohesive 5-Tab Jetpack Compose NavHost Navigation Graph.
 * Transitions configured for zero jitter / instantaneous 60+ FPS frame cadence.
 */
@Composable
fun CampusNavHost(
    navController: NavHostController,
    database: CampusDatabase,
    streamChatEngine: StreamChatEngine?,
    modifier: Modifier = Modifier,
    messagesViewModel: MessagesViewModel = remember { MessagesViewModel(streamChatEngine) },
    channelsViewModel: ChannelsViewModel? = null
) {
    // Repositories backed by local database cache and worker thread dispatchers
    val flashMeetRepository = remember { FlashMeetRepository(database) }
    val placesRepository = remember { com.campus.network.core.data.PlacesRepository() }
    val academicChannelsRepository = remember { AcademicChannelsRepository(database) }
    val campusHubRepository = remember { CampusHubRepository(database) }
    val studentProfileRepository = remember { StudentProfileRepository() }

    // ViewModels with UDF patterns
    val mapViewModel = remember { MapViewModel(flashMeetRepository, placesRepository) }
    val resolvedChannelsViewModel = channelsViewModel ?: remember { ChannelsViewModel(academicChannelsRepository) }
    val hubViewModel = remember { HubViewModel(campusHubRepository) }
    val profileViewModel = remember { ProfileViewModel(studentProfileRepository) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        // Splash Screen (Brand navy blue canvas with Patriarch logo)
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Screen (Notification permissions with scenic bridge mockup)
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onNextClick = {
                    navController.navigate(Screen.OnboardingTransition.route)
                }
            )
        }

        // Second Onboarding Screen: 3D Balloon Typography & Kinetic Rocket Launch
        composable(route = Screen.OnboardingTransition.route) {
            OnboardingTransitionScreen(
                onTransitionComplete = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Auth Screen (Log in / Continue as guest / Join Patriarch program)
        composable(route = Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        // Tab 1: Map (The Flash-Meet Interface)
        composable(route = Screen.Map.route) {
            MapScreen(viewModel = mapViewModel)
        }

        // Tab 2: Channels (The Academic Workspace / Servers)
        composable(route = Screen.Channels.route) {
            ChannelsScreen(
                viewModel = resolvedChannelsViewModel,
                onNavigateToChannelChat = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        // Tab 3: Messages (Direct Streams)
        composable(route = Screen.Messages.route) {
            MessagesScreen(
                viewModel = messagesViewModel,
                onNavigateBack = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.Hub.route) { inclusive = false }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        // Tab 4: Hub Layer (Campus Micro-Commerce)
        composable(route = Screen.Hub.route) {
            HubScreen(
                viewModel = hubViewModel,
                onNavigateToChat = {
                    navController.navigate(Screen.Messages.route)
                }
            )
        }

        // Tab 5: Profile (Gamified Persona)
        composable(route = Screen.Profile.route) {
            ProfileScreen(viewModel = profileViewModel)
        }
    }
}
