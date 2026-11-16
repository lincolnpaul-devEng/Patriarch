package com.campus.network.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.campus.network.AppContainer
import com.campus.network.core.data.CampusDirectoryRepository
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.model.stableUserId
import com.campus.network.core.network.StreamChatEngine
import com.campus.network.feature.channels.ChannelsScreen
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.hub.HubScreen
import com.campus.network.feature.hub.HubViewModel
import com.campus.network.feature.hub.feed.CampusLiveFeedScreen
import com.campus.network.feature.hub.golive.GoLiveScreen
import com.campus.network.feature.hub.notifications.NotificationsScreen
import com.campus.network.feature.hub.notifications.NotificationSettingsScreen
import com.campus.network.feature.map.MapScreen
import com.campus.network.feature.map.MapViewModel
import com.campus.network.feature.messages.MessagesScreen
import com.campus.network.feature.messages.MessagesViewModel
import com.campus.network.feature.profile.ProfileScreen
import com.campus.network.feature.profile.ProfileViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

import com.campus.network.ui.auth.AuthScreen
import com.campus.network.ui.auth.OnboardingScreen
import com.campus.network.ui.auth.OnboardingTransitionScreen
import com.campus.network.ui.auth.SplashScreen
import com.campus.network.ui.auth.VerifyScreen
import com.campus.network.ui.onboarding.OnboardingSurveyScreen
import kotlinx.coroutines.launch

/**
 * Cohesive 5-Tab Jetpack Compose NavHost Navigation Graph.
 * Transitions configured for zero jitter / instantaneous 60+ FPS frame cadence.
 */
@Composable
fun CampusNavHost(
    navController: NavHostController,
    container: AppContainer,
    streamChatEngine: StreamChatEngine?,
    modifier: Modifier = Modifier,
    messagesViewModel: MessagesViewModel? = null,
    channelsViewModel: ChannelsViewModel? = null,
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val database = container.database
    val coroutineScope = rememberCoroutineScope()
    val authClient = remember(context) { com.campus.network.core.network.SupabaseGoogleAuthClient(context) }

    // App-scoped repositories (single instances owned by the AppContainer)
    val studentProfileRepository = container.studentProfileRepository
    val flashMeetRepository = container.flashMeetRepository
    val academicChannelsRepository = container.academicChannelsRepository
    val campusHubRepository = container.hubRepository
    val serverRepository = container.serverRepository
    val liveStreamRepository = container.liveStreamRepository

    // Context-scoped helpers (not app-scoped state)
    val directoryRepository = remember(context) { CampusDirectoryRepository(context.applicationContext) }
    val placesRepository = remember { com.campus.network.core.data.PlacesRepository() }

    // Scoped ViewModels managed by ViewModelProvider to survive configuration changes
    val resolvedMessagesViewModel: MessagesViewModel = messagesViewModel ?: viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MessagesViewModel(
                    streamChatEngine = streamChatEngine,
                    studentProfileRepository = studentProfileRepository,
                    chatMessageDao = database.chatMessageDao(),
                    supabase = com.campus.network.core.network.SupabaseSdkClientProvider.defaultClient,
                    serverRepository = serverRepository,
                    context = context.applicationContext
                ) as T
            }
        }
    )

    val resolvedChannelsViewModel: ChannelsViewModel = channelsViewModel ?: viewModel(
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

    val mapViewModel: MapViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MapViewModel(flashMeetRepository, placesRepository, studentProfileRepository) as T
            }
        }
    )

    val hubViewModel: HubViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HubViewModel(campusHubRepository, studentProfileRepository) as T
            }
        }
    )

    val profileViewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(context, studentProfileRepository) as T
            }
        }
    )

    // Route guard: every navigation (including deep links) is checked against the session access tier.
    val currentProfile by remember { studentProfileRepository.getProfile() }
        .collectAsState(initial = null)
    val isAuthenticated by studentProfileRepository.isAuthenticatedSession.collectAsState()
    val isGuest by studentProfileRepository.isGuestSession.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    androidx.compose.runtime.LaunchedEffect(currentEntry?.destination?.route, isAuthenticated, isGuest, currentProfile) {
        val route = currentEntry?.destination?.route ?: return@LaunchedEffect
        val baseRoute = route.substringBefore("?").substringBefore("/")
        val setupRoutes = setOf("splash", "onboarding", "onboarding_transition", "auth", "verify")
        val surveyComplete = currentProfile?.onboardingPreferences?.hasCompletedSurvey == true
        val tier = com.campus.network.core.security.CampusSecurityGuard.evaluateSessionAccessTier(
            profile = currentProfile,
            isAuthenticated = isAuthenticated,
            isGuestMode = isGuest
        )
        val target = when {
            isAuthenticated && !surveyComplete && baseRoute !in setupRoutes &&
                baseRoute != Screen.OnboardingSurvey.route -> Screen.OnboardingSurvey.route
            isAuthenticated && surveyComplete && baseRoute == Screen.OnboardingSurvey.route -> Screen.Hub.route
            !com.campus.network.core.model.CampusSecurityPolicy.canAccessRoute(route, tier) &&
                tier == com.campus.network.core.model.CampusAccessTier.UNAUTHENTICATED -> Screen.Auth.route
            !com.campus.network.core.model.CampusSecurityPolicy.canAccessRoute(route, tier) -> Screen.Hub.route
            else -> null
        }
        if (target != null) {
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
            if (tier == com.campus.network.core.model.CampusAccessTier.GUEST_READ_ONLY) {
                android.widget.Toast.makeText(
                    context,
                    "Verify your university email to use this feature.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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
                    coroutineScope.launch {
                        val savedSession = authClient.getSavedSession()?.takeIf {
                            runCatching {
                                com.campus.network.core.network.SupabaseSessionStore.validAccessToken(
                                    com.campus.network.BuildConfig.SUPABASE_URL,
                                    com.campus.network.BuildConfig.SUPABASE_ANON_KEY
                                )
                            }.isSuccess
                        }
                        val email = savedSession?.email?.trim()?.ifBlank { null }

                        if (email.isNullOrBlank()) {
                            val targetRoute = if (authClient.hasSeenIntroOnboarding()) {
                                Screen.Auth.route
                            } else {
                                Screen.Onboarding.route
                            }
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                            return@launch
                        }

                        // Sync latest profile state from Supabase
                        studentProfileRepository.syncProfileWithRemote(email)
                        val localProfile = studentProfileRepository.getProfileSync()

                        runCatching {
                            serverRepository.syncRemoteServers()
                            val sId = localProfile?.studentId?.ifBlank { email }.orEmpty()
                            serverRepository.syncRemoteMemberships(sId, email)
                            resolvedChannelsViewModel.refreshServers()
                        }

                        val isSurveyDone =
                            studentProfileRepository.getProfileSync()?.onboardingPreferences?.hasCompletedSurvey == true

                        val targetRoute = if (isSurveyDone) {
                            Screen.Hub.route
                        } else {
                            Screen.OnboardingSurvey.route
                        }

                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
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
                    authClient.setHasSeenIntroOnboarding(true)
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Auth Screen (Sign in with Google / Log in with Email / Join Patriarch program)
        composable(route = Screen.Auth.route) {
            AuthScreen(
                studentProfileRepository = studentProfileRepository,
                onLoginSuccess = { email, fullName, avatarUrl ->
                    profileViewModel.resetDeletionState()
                    val matchedUni = directoryRepository.resolveUniversityFromEmail(email)
                    val countryEntry = matchedUni?.let { u ->
                        directoryRepository.getCountries().firstOrNull { it.code.equals(u.countryCode, ignoreCase = true) }
                    }
                    studentProfileRepository.syncProfileWithRemote(email)
                    studentProfileRepository.applyAuthenticatedUser(
                        fullName = fullName,
                        email = email,
                        avatarUrl = avatarUrl,
                        matchedUniversity = matchedUni,
                        countryName = countryEntry?.name,
                        currencyCode = countryEntry?.currencyCode
                    )
                    studentProfileRepository.setAvatarFallbackIfMissing(avatarUrl)
                    runCatching {
                        serverRepository.syncRemoteServers()
                        val sId = studentProfileRepository.getProfileSync()?.studentId?.ifBlank { email }.orEmpty()
                        serverRepository.syncRemoteMemberships(sId, email)
                        resolvedChannelsViewModel.refreshServers()
                    }

                    val isSurveyDone =
                        studentProfileRepository.getProfileSync()?.onboardingPreferences?.hasCompletedSurvey == true

                    val nextDestination = if (isSurveyDone) Screen.Hub.route else Screen.OnboardingSurvey.route
                    navController.navigate(nextDestination) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToVerify = { target, isPhone ->
                    navController.navigate(Screen.Verify.createRoute(target, isPhone))
                }
            )
        }

        // Verification Screen (Matching reference layout, BrandBlueAuth background, custom 3x4 keypad)
        composable(
            route = Screen.Verify.route,
            arguments = listOf(
                navArgument("target") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("isPhone") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val rawTarget = backStackEntry.arguments?.getString("target") ?: ""
            val target = try {
                java.net.URLDecoder.decode(rawTarget, "UTF-8")
            } catch (e: Exception) {
                rawTarget
            }
            val isPhone = backStackEntry.arguments?.getBoolean("isPhone") ?: false

            VerifyScreen(
                target = target,
                isPhone = isPhone,
                onBack = {
                    navController.popBackStack()
                },
                onVerificationSuccess = { sessionUser ->
                    coroutineScope.launch {
                        val matchedUni = directoryRepository.resolveUniversityFromEmail(sessionUser.email)
                        val countryEntry = matchedUni?.let { u ->
                            directoryRepository.getCountries().firstOrNull { it.code.equals(u.countryCode, ignoreCase = true) }
                        }
                        studentProfileRepository.syncProfileWithRemote(sessionUser.email)
                        studentProfileRepository.applyAuthenticatedUser(
                            fullName = sessionUser.fullName,
                            email = sessionUser.email,
                            avatarUrl = sessionUser.avatarUrl,
                            matchedUniversity = matchedUni,
                            countryName = countryEntry?.name,
                            currencyCode = countryEntry?.currencyCode
                        )
                        studentProfileRepository.setAvatarFallbackIfMissing(sessionUser.avatarUrl)

                        val isSurveyDone =
                            studentProfileRepository.getProfileSync()?.onboardingPreferences?.hasCompletedSurvey == true

                        val nextDestination = if (isSurveyDone) Screen.Hub.route else Screen.OnboardingSurvey.route
                        navController.navigate(nextDestination) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // First-Time User Onboarding Survey Flow (Steps A - K)
        composable(route = Screen.OnboardingSurvey.route) {
            OnboardingSurveyScreen(
                studentProfileRepository = studentProfileRepository,
                directoryRepository = directoryRepository,
                onSurveyCompleted = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.OnboardingSurvey.route) { inclusive = true }
                    }
                }
            )
        }
        // Tab 1: Map (The Flash-Meet & Campus Navigation Interface)
        composable(route = Screen.Map.route) {
            MapScreen(
                viewModel = mapViewModel,
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onToggleBottomBar = onToggleBottomBar,
                onRetrySync = {
                    coroutineScope.launch {
                        DeltaSyncManager.get(context).retryFailedSyncs()
                    }
                }
            )
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
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToDirectChat = { peerId, peerName, peerHandle, serverName, serverTag ->
                    resolvedMessagesViewModel.onEvent(
                        com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                            peerId = peerId,
                            peerName = peerName,
                            peerHandle = peerHandle,
                            serverName = serverName,
                            serverTag = serverTag
                        )
                    )
                    navController.navigate(Screen.Messages.route)
                }
            )
        }

        // Tab 3: Messages (Direct Streams)
        composable(route = Screen.Messages.route) {
            MessagesScreen(
                viewModel = resolvedMessagesViewModel,
                onNavigateBack = {
                    navController.navigate(Screen.Channels.route) {
                        popUpTo(Screen.Channels.route) { inclusive = false }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToServer = { serverId ->
                    if (serverId.isNotBlank()) {
                        resolvedChannelsViewModel.onEvent(
                            com.campus.network.feature.channels.ChannelsEvent.SelectServer(serverId)
                        )
                    }
                    navController.navigate(Screen.Channels.route) {
                        popUpTo(Screen.Channels.route) { inclusive = false }
                    }
                },
                onAddServer = {
                    resolvedChannelsViewModel.onEvent(
                        com.campus.network.feature.channels.ChannelsEvent.AddServerClick
                    )
                    navController.navigate(Screen.Channels.route) {
                        popUpTo(Screen.Channels.route) { inclusive = false }
                    }
                }
            )
        }

        // Tab 4: Homescreen (Best Football Match UI)
        composable(route = Screen.Hub.route) {
            val p = currentProfile
            com.campus.network.feature.hub.bestfootball.BestFootballMatchScreen(
                userFullName = p?.fullName,
                userAvatarUrl = p?.avatarUrl,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToGoLive = {
                    navController.navigate(Screen.GoLive.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToChannels = {
                    navController.navigate(Screen.Channels.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Dedicated Notifications Screen (Reference Image 1)
        composable(route = Screen.Notifications.route) {
            val p = currentProfile
            val activeUserId = p?.studentId?.takeIf { it.isNotBlank() } ?: p?.universityEmail?.takeIf { it.isNotBlank() }.orEmpty()
            val activeEmail = p?.universityEmail.orEmpty()
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.NotificationSettings.route) },
                currentUserId = activeUserId,
                currentUserEmail = activeEmail,
                serverRepository = serverRepository,
                onNavigateToMapMeet = { lat, lng ->
                    mapViewModel.onEvent(com.campus.network.feature.map.MapEvent.FocusCoordinatesOnMap(lat, lng))
                    navController.navigate(Screen.Map.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToMessages = { peerId, peerName ->
                    if (!peerId.isNullOrBlank() && !peerName.isNullOrBlank()) {
                        resolvedMessagesViewModel.onEvent(
                            com.campus.network.feature.messages.MessagesEvent.OpenOrCreateDirectChat(
                                peerId = peerId,
                                peerName = peerName,
                                peerHandle = "@${peerId.trim()}"
                            )
                        )
                    }
                    navController.navigate(Screen.Messages.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToHub = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Notification Settings Screen (Reference Image 2)
        composable(route = Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Tab 5: Profile (Gamified Persona)
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onRetrySync = {
                    coroutineScope.launch {
                        profileViewModel.retryFailedUploads()
                    }
                },
                onSignOut = {
                    coroutineScope.launch {
                        profileViewModel.resetDeletionState()
                        authClient.signOut()
                        studentProfileRepository.signOut()
                        // Wipe every local table so the next sign-in starts from remote truth
                        // (prevents ghost data from a previous account leaking into this one).
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            database.clearAllTables()
                        }
                        resolvedChannelsViewModel.clearSession()
                        profileViewModel.onEvent(com.campus.network.feature.profile.ProfileEvent.SignOut)
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Go Live flow: permissions -> details -> countdown -> live -> summary
        composable(route = Screen.GoLive.route) {
            GoLiveScreen(
                studentProfileRepository = studentProfileRepository,
                liveStreamRepository = liveStreamRepository,
                onExit = { navController.popBackStack() }
            )
        }

        // Dedicated Fullscreen Live Feed Screen (TikTok / Instagram Live Style)
        composable(
            route = Screen.LiveFeed.route,
            arguments = listOf(
                navArgument("storyId") {
                    type = NavType.StringType
                    defaultValue = "founders"
                }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: "founders"
            CampusLiveFeedScreen(
                initialStoryId = storyId,
                studentProfileRepository = studentProfileRepository,
                liveStreamRepository = liveStreamRepository,
                serverRepository = serverRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
