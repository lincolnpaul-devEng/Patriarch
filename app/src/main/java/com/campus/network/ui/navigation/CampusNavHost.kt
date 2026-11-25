package com.campus.network.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
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
import com.campus.network.feature.hub.HubViewModel
import com.campus.network.feature.hub.feed.CampusLiveFeedScreen
import com.campus.network.feature.hub.golive.GoLiveScreen
import com.campus.network.feature.hub.liveroom.LiveRoomScreen
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

internal enum class SafeBackAction {
    IGNORE_STALE,
    POP_BACK_STACK,
    NAVIGATE_FALLBACK
}

internal fun resolveSafeBackAction(
    fromBaseRoute: String,
    currentRoute: String?,
    previousRoute: String?,
    fallbackRoute: String
): SafeBackAction {
    val normalizedFromBase = fromBaseRoute.substringBefore("?").substringBefore("/")
    val currentBase = currentRoute?.substringBefore("?")?.substringBefore("/")
    if (currentBase != null && currentBase != normalizedFromBase) {
        return SafeBackAction.IGNORE_STALE
    }
    val prevBase = previousRoute?.substringBefore("?")?.substringBefore("/")
    val blockedBackTargets = setOf(
        Screen.Splash.route,
        Screen.Onboarding.route,
        Screen.OnboardingTransition.route
    )
    val fallbackBase = fallbackRoute.substringBefore("?").substringBefore("/")
    val isBlockedAuthReturn = fallbackBase != Screen.Auth.route && prevBase == Screen.Auth.route
    return if (prevBase != null && prevBase !in blockedBackTargets && !isBlockedAuthReturn) {
        SafeBackAction.POP_BACK_STACK
    } else {
        SafeBackAction.NAVIGATE_FALLBACK
    }
}

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
    mapViewModel: MapViewModel? = null,
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val database = container.database
    val coroutineScope = rememberCoroutineScope()
    val authClient = remember(context) { com.campus.network.core.network.SupabaseGoogleAuthClient(context) }

    // App-scoped repositories (single instances owned by the AppContainer)
    val studentProfileRepository = container.studentProfileRepository
    val academicChannelsRepository = container.academicChannelsRepository
    val campusHubRepository = container.hubRepository
    val rendezvousRepository = container.rendezvousRepository
    val serverRepository = container.serverRepository
    val liveStreamRepository = container.liveStreamRepository

    // Context-scoped helpers (not app-scoped state)
    val directoryRepository = remember(context) { CampusDirectoryRepository(context.applicationContext) }

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

    val resolvedMapViewModel: MapViewModel = mapViewModel
        ?: androidx.hilt.navigation.compose.hiltViewModel()

    val marketViewModel: HubViewModel = viewModel(
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
    val campusStatsRepository = container.campusStatsRepository
    var campusStats by remember { mutableStateOf(com.campus.network.core.data.CampusStatsRepository.CampusStats.Empty) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        campusStats = campusStatsRepository.fetchStats()
            .getOrDefault(com.campus.network.core.data.CampusStatsRepository.CampusStats.Empty)
    }
    val escrowOrdersForProfile: suspend () -> List<com.campus.network.core.model.EscrowOrder> = {
        val uid = currentProfile?.stableUserId().orEmpty()
        if (uid.isBlank()) emptyList() else campusHubRepository.fetchEscrowOrdersForUser(uid)
    }
    val isAuthenticated by studentProfileRepository.isAuthenticatedSession.collectAsState()
    val isGuest by studentProfileRepository.isGuestSession.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    androidx.compose.runtime.LaunchedEffect(currentEntry?.destination?.route, isAuthenticated, isGuest, currentProfile) {
        val route = currentEntry?.destination?.route ?: return@LaunchedEffect
        val baseRoute = route.substringBefore("?").substringBefore("/")
        val setupRoutes = setOf("splash", "onboarding", "onboarding_transition", "auth", "verify")
        val isProfileLoaded = currentProfile != null &&
            (currentProfile?.universityEmail?.isNotBlank() == true || currentProfile?.studentId?.isNotBlank() == true)
        val surveyComplete = currentProfile?.onboardingPreferences?.hasCompletedSurvey == true
        val tier = com.campus.network.core.security.CampusSecurityGuard.evaluateSessionAccessTier(
            profile = currentProfile,
            isAuthenticated = isAuthenticated,
            isGuestMode = isGuest
        )
        val target = when {
            isAuthenticated && isProfileLoaded && !surveyComplete && baseRoute !in setupRoutes &&
                baseRoute != Screen.OnboardingSurvey.route -> Screen.OnboardingSurvey.route
            isAuthenticated && isProfileLoaded && surveyComplete && baseRoute == Screen.OnboardingSurvey.route -> Screen.Hub.route
            !com.campus.network.core.model.CampusSecurityPolicy.canAccessRoute(route, tier) &&
                tier == com.campus.network.core.model.CampusAccessTier.UNAUTHENTICATED -> Screen.Auth.route
            !com.campus.network.core.model.CampusSecurityPolicy.canAccessRoute(route, tier) -> Screen.Hub.route
            else -> null
        }
        if (target != null && baseRoute != target.substringBefore("?")) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
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

    fun navigateBackSafely(
        fromBaseRoute: String,
        fallbackRoute: String = Screen.Hub.route
    ) {
        val action = resolveSafeBackAction(
            fromBaseRoute = fromBaseRoute,
            currentRoute = navController.currentBackStackEntry?.destination?.route,
            previousRoute = navController.previousBackStackEntry?.destination?.route,
            fallbackRoute = fallbackRoute
        )
        when (action) {
            SafeBackAction.IGNORE_STALE -> Unit
            SafeBackAction.POP_BACK_STACK -> {
                if (!navController.popBackStack()) {
                    navController.navigate(fallbackRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            SafeBackAction.NAVIGATE_FALLBACK -> {
                navController.navigate(fallbackRoute) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
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
                            if (navController.currentBackStackEntry?.destination?.route != Screen.Splash.route) {
                                return@launch
                            }
                            val targetRoute = if (authClient.hasSeenIntroOnboarding()) {
                                Screen.Auth.route
                            } else {
                                Screen.Onboarding.route
                            }
                            navController.navigate(targetRoute) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
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

                        if (navController.currentBackStackEntry?.destination?.route != Screen.Splash.route) {
                            return@launch
                        }

                        val isSurveyDone =
                            studentProfileRepository.getProfileSync()?.onboardingPreferences?.hasCompletedSurvey == true

                        val targetRoute = if (isSurveyDone) {
                            Screen.Hub.route
                        } else {
                            Screen.OnboardingSurvey.route
                        }

                        navController.navigate(targetRoute) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Onboarding Screen (Notification permissions with scenic bridge mockup)
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onNextClick = {
                    navController.navigate(Screen.OnboardingTransition.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Second Onboarding Screen: 3D Balloon Typography & Kinetic Rocket Launch
        composable(route = Screen.OnboardingTransition.route) {
            OnboardingTransitionScreen(
                onTransitionComplete = {
                    authClient.setHasSeenIntroOnboarding(true)
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
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
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
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
            BackHandler {
                navigateBackSafely("verify", fallbackRoute = Screen.Auth.route)
            }
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
                    navigateBackSafely("verify", fallbackRoute = Screen.Auth.route)
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
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
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
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        // Tab 1: Map
        composable(route = Screen.Map.route) {
            com.campus.network.ui.MainSwipeablePagerScreen(
                currentProfile = currentProfile,
                mapViewModel = resolvedMapViewModel,
                channelsViewModel = resolvedChannelsViewModel,
                messagesViewModel = resolvedMessagesViewModel,
                profileViewModel = profileViewModel,
                navController = navController,
                serverRepository = serverRepository,
                studentProfileRepository = studentProfileRepository,
                context = context,
                escrowOrdersLoader = escrowOrdersForProfile,
                campusStats = campusStats,
                initialPage = 2
            )
        }

        // Tab 2: Channels (Servers)
        composable(route = Screen.Channels.route) {
            com.campus.network.ui.MainSwipeablePagerScreen(
                currentProfile = currentProfile,
                mapViewModel = resolvedMapViewModel,
                channelsViewModel = resolvedChannelsViewModel,
                messagesViewModel = resolvedMessagesViewModel,
                profileViewModel = profileViewModel,
                navController = navController,
                serverRepository = serverRepository,
                studentProfileRepository = studentProfileRepository,
                context = context,
                escrowOrdersLoader = escrowOrdersForProfile,
                campusStats = campusStats,
                initialPage = 1
            )
        }

        // Tab 3: Messages
        composable(route = Screen.Messages.route) {
            com.campus.network.ui.MainSwipeablePagerScreen(
                currentProfile = currentProfile,
                mapViewModel = resolvedMapViewModel,
                channelsViewModel = resolvedChannelsViewModel,
                messagesViewModel = resolvedMessagesViewModel,
                profileViewModel = profileViewModel,
                navController = navController,
                serverRepository = serverRepository,
                studentProfileRepository = studentProfileRepository,
                context = context,
                escrowOrdersLoader = escrowOrdersForProfile,
                campusStats = campusStats,
                initialPage = 3
            )
        }

        // Tab 4: Homescreen (Main Swipeable Pager starting on Home)
        composable(route = Screen.Hub.route) {
            com.campus.network.ui.MainSwipeablePagerScreen(
                currentProfile = currentProfile,
                mapViewModel = resolvedMapViewModel,
                channelsViewModel = resolvedChannelsViewModel,
                messagesViewModel = resolvedMessagesViewModel,
                profileViewModel = profileViewModel,
                navController = navController,
                serverRepository = serverRepository,
                studentProfileRepository = studentProfileRepository,
                context = context,
                escrowOrdersLoader = escrowOrdersForProfile,
                campusStats = campusStats,
                initialPage = 0
            )
        }

        // Dedicated Notifications Screen (Reference Image 1)
        composable(route = Screen.Notifications.route) {
            BackHandler {
                navigateBackSafely(Screen.Notifications.route, fallbackRoute = Screen.Hub.route)
            }
            val p = currentProfile
            val activeUserId = p?.studentId?.takeIf { it.isNotBlank() } ?: p?.universityEmail?.takeIf { it.isNotBlank() }.orEmpty()
            val activeEmail = p?.universityEmail.orEmpty()
            NotificationsScreen(
                onNavigateBack = {
                    navigateBackSafely(Screen.Notifications.route, fallbackRoute = Screen.Hub.route)
                },
                onNavigateToSettings = { navController.navigate(Screen.NotificationSettings.route) },
                currentUserId = activeUserId,
                currentUserEmail = activeEmail,
                serverRepository = serverRepository,
                onNavigateToMapMeet = { lat, lng ->
                    resolvedMapViewModel.onEvent(com.campus.network.feature.map.MapEvent.FocusCoordinatesOnMap(lat, lng))
                    navController.navigate(Screen.Map.route) {
                        popUpTo(0) { inclusive = true }
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
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToHub = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLiveStream = { streamId ->
                    navController.navigate(Screen.LiveFeed.createRoute(streamId.ifBlank { "founders" })) {
                        launchSingleTop = true
                    }
                },
                onNavigateToMarket = { itemId ->
                    navController.navigate(Screen.Market.createRoute(itemId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToChannels = {
                    navController.navigate(Screen.Channels.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Notification Settings Screen (Reference Image 2)
        composable(route = Screen.NotificationSettings.route) {
            BackHandler {
                navigateBackSafely(
                    fromBaseRoute = Screen.NotificationSettings.route,
                    fallbackRoute = Screen.Notifications.route
                )
            }
            NotificationSettingsScreen(
                onNavigateBack = {
                    navigateBackSafely(
                        fromBaseRoute = Screen.NotificationSettings.route,
                        fallbackRoute = Screen.Notifications.route
                    )
                }
            )
        }

        // Market: replaces the deleted snack store + dead HubScreen marketplace with the real campus marketplace.
        composable(
            route = Screen.Market.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            BackHandler {
                navigateBackSafely(Screen.Market.route, fallbackRoute = Screen.Hub.route)
            }
            val deepLinkItemId = backStackEntry.arguments?.getString("itemId").orEmpty()
            LaunchedEffect(deepLinkItemId) {
                if (deepLinkItemId.isNotBlank()) {
                    marketViewModel.openMarketItemFromNotification(deepLinkItemId)
                }
            }
            com.campus.network.feature.hub.market.MarketScreen(
                viewModel = marketViewModel,
                rendezvousRepository = rendezvousRepository,
                onNavigateBack = {
                    navigateBackSafely(Screen.Market.route, fallbackRoute = Screen.Hub.route)
                },
                onNavigateToMySelling = { navController.navigate(Screen.MySelling.route) },
                onOpenProduct = { /* Phase 3: product detail + checkout flow */ },
                onOpenGig = { /* Phase 3: gig detail flow */ }
            )
        }

        composable(route = Screen.MySelling.route) {
            BackHandler {
                navigateBackSafely(Screen.MySelling.route, fallbackRoute = Screen.Market.createRoute())
            }
            com.campus.network.feature.hub.market.MySellingScreen(
                viewModel = marketViewModel,
                repository = campusHubRepository,
                onBack = {
                    navigateBackSafely(Screen.MySelling.route, fallbackRoute = Screen.Market.createRoute())
                }
            )
        }

        // Tab 5: Profile
        composable(route = Screen.Profile.route) {
            com.campus.network.ui.MainSwipeablePagerScreen(
                currentProfile = currentProfile,
                mapViewModel = resolvedMapViewModel,
                channelsViewModel = resolvedChannelsViewModel,
                messagesViewModel = resolvedMessagesViewModel,
                profileViewModel = profileViewModel,
                navController = navController,
                serverRepository = serverRepository,
                studentProfileRepository = studentProfileRepository,
                context = context,
                escrowOrdersLoader = escrowOrdersForProfile,
                campusStats = campusStats,
                initialPage = 4
            )
        }

        // Live Room: stage + community grid wired to active streams and campus peers
        composable(route = Screen.LiveRoom.route) {
            BackHandler {
                navigateBackSafely(Screen.LiveRoom.route, fallbackRoute = Screen.Hub.route)
            }
            LiveRoomScreen(
                studentProfileRepository = studentProfileRepository,
                liveStreamRepository = liveStreamRepository,
                onNavigateBack = {
                    navigateBackSafely(Screen.LiveRoom.route, fallbackRoute = Screen.Hub.route)
                },
                onNavigateToCreateLive = {
                    navController.navigate(Screen.GoLive.route) {
                        launchSingleTop = true
                    }
                },
                onOpenLiveStream = { streamId ->
                    navController.navigate(Screen.LiveFeed.createRoute(streamId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Go Live flow: permissions -> details -> countdown -> live -> summary
        composable(route = Screen.GoLive.route) {
            BackHandler {
                navigateBackSafely(Screen.GoLive.route, fallbackRoute = Screen.Hub.route)
            }
            GoLiveScreen(
                studentProfileRepository = studentProfileRepository,
                liveStreamRepository = liveStreamRepository,
                onExit = {
                    navigateBackSafely(Screen.GoLive.route, fallbackRoute = Screen.Hub.route)
                }
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
            BackHandler {
                navigateBackSafely("campus_live_feed", fallbackRoute = Screen.Hub.route)
            }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: "founders"
            CampusLiveFeedScreen(
                initialStoryId = storyId,
                studentProfileRepository = studentProfileRepository,
                liveStreamRepository = liveStreamRepository,
                serverRepository = serverRepository,
                onNavigateBack = {
                    navigateBackSafely("campus_live_feed", fallbackRoute = Screen.Hub.route)
                }
            )
        }
    }
}
