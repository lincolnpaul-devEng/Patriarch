@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.campus.network.feature.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.FriendLiveLocation
import com.campus.network.core.ui.components.CampusStudentAvatar
import com.campus.network.core.ui.components.CampusSuccessSnackBanner
import com.campus.network.core.ui.components.CountdownTimerPill
import com.campus.network.feature.map.components.ActiveFlashMeetLiveBanner
import com.campus.network.feature.map.components.BrandOnboardingNavy
import com.campus.network.feature.map.components.CategoryDock
import com.campus.network.feature.map.components.MapFilterRow
import com.campus.network.feature.map.components.MapLoadingPulseOverlay
import com.campus.network.feature.map.components.MapTopControls
import com.campus.network.feature.map.components.MyLocationBottomSheet
import com.campus.network.feature.map.components.MyLocationCloseButton
import com.campus.network.feature.map.components.MyLocationMapPin
import com.campus.network.feature.map.components.PlaceDetailSheet
import com.campus.network.feature.map.components.ShareLocationDrawerSheet
import com.campus.network.feature.map.components.TriggerFlashMeetFriendsSheet
import com.campus.network.feature.map.domain.ActiveNavigationSession
import com.campus.network.feature.map.domain.ArViewMode
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.SearchSuggestionItem
import com.campus.network.feature.map.domain.TiltSwitchPolicy
import com.campus.network.feature.map.events.CreateEventDrawerSheet
import com.campus.network.feature.map.events.EventDetailScreen
import com.campus.network.feature.map.events.EventsScreen
import com.campus.network.feature.map.events.MapNavigationMode
import com.campus.network.feature.map.events.formatCoordinateHemisphere
import com.campus.network.feature.map.ui.ar.ArNavigationCameraView
import com.campus.network.feature.map.ui.ar.CheckeredFlagVectorIcon
import com.campus.network.feature.map.ui.ar.HyperSquareIconButton
import com.campus.network.feature.map.ui.ar.IsometricDestinationBoxIllustration
import com.campus.network.feature.map.ui.ar.formatHyperArManeuver
import com.campus.network.feature.map.ui.ar.iconForManeuver
import com.campus.network.feature.map.ui.mapbox.MapboxCampusMapView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MyLocationSheetStep {
    LOCATION_REVIEW,
    SELECT_FRIENDS
}

private fun findNearestPlaceAddress(lat: Double, lng: Double, places: List<CampusPlace>): String {
    val target = GeoCoordinate(lat, lng)
    val nearestWithin500m = places
        .map { place ->
            place to GeoSpatialMath.haversineMeters(target, GeoCoordinate(place.latitude, place.longitude))
        }
        .filter { (_, distanceMeters) -> distanceMeters <= 500.0 }
        .minByOrNull { (_, distanceMeters) -> distanceMeters }
        ?.first
    return nearestWithin500m?.let { "${it.name}, ${it.address}" }
        ?: formatCoordinateHemisphere(lat, lng)
}

private fun formatUpdatedAgo(updatedAtEpochMs: Long): String {
    if (updatedAtEpochMs <= 0L) return "Live now"
    val diffSec = ((System.currentTimeMillis() - updatedAtEpochMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 15L -> "Live now"
        diffSec < 60L -> "${diffSec}s ago"
        else -> "${diffSec / 60L}m ago"
    }
}

private fun initialsForName(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "U"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

/**
 * AR-Driven Mapbox Navigation & Campus Social Radar Screen:
 *  - Mapbox Maps SDK v11 2D/3D Standard Vector Map (`< 35 deg` pitch)
 *  - Mapbox Search v2 POI & Campus Landmark Autocomplete
 *  - Mapbox Navigation v3 Turn-by-Turn Walking Engine
 *  - ARCore Geospatial API + SceneView 3D GLB Overlay (`> 45 deg` pitch tilt-to-switch)
 *  - Live Friend Streaming, Flash Meets, Verified SafeTrade Spots & Events Hub.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    isActivePage: Boolean = true,
    hasUnreadNotifications: Boolean = false,
    onNavigateToMessages: () -> Unit = {},
    onOpenDirectChat: (peerId: String, peerName: String) -> Unit = { _, _ -> onNavigateToMessages() },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {},
    onRetrySync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var prefilledPlace by remember { mutableStateOf<CampusPlace?>(null) }
    val mapContext = androidx.compose.ui.platform.LocalContext.current

    // Session dwell tracker: queues vibe check if user actively explores the map and leaves
    com.campus.network.core.ui.components.MoodSessionTracker(
        source = com.campus.network.core.ui.components.MoodSource.MAP,
        isActive = isActivePage
    )

    // Trigger mood check-in when user completes or cancels walking navigation
    val hadActiveNav = remember { mutableStateOf(false) }
    LaunchedEffect(uiState.activeNavigation) {
        if (uiState.activeNavigation != null) {
            hadActiveNav.value = true
        } else if (hadActiveNav.value) {
            hadActiveNav.value = false
            com.campus.network.core.ui.components.MoodCheckIn.request(
                mapContext,
                com.campus.network.core.ui.components.MoodSource.MAP
            )
        }
    }

    // Stream TYPE_ROTATION_VECTOR pitch/azimuth ONLY when Map tab is active in HorizontalPager
    LaunchedEffect(isActivePage) {
        if (!isActivePage) return@LaunchedEffect
        viewModel.observeDeviceOrientation().collect { orientation ->
            viewModel.onEvent(MapEvent.UpdateOrientation(orientation))
        }
    }

    BackHandler(enabled = isActivePage && uiState.navigationMode != MapNavigationMode.MAP) {
        when (uiState.navigationMode) {
            MapNavigationMode.EVENT_DETAIL -> viewModel.onEvent(MapEvent.BackToEvents)
            MapNavigationMode.EVENTS_LIST -> viewModel.onEvent(MapEvent.ShowMap)
            MapNavigationMode.MAP -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.navigationMode,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "MapNavigationModeTransition"
        ) { mode ->
            when (mode) {
                MapNavigationMode.EVENTS_LIST -> {
                    EventsScreen(
                        events = uiState.events,
                        onEventClick = { viewModel.onEvent(MapEvent.SelectEvent(it)) },
                        onNavigateToMap = { viewModel.onEvent(MapEvent.ShowMap) },
                        onCreateEventClick = {
                            prefilledPlace = null
                            viewModel.onEvent(MapEvent.ToggleCreateDialog(true))
                        },
                        onNotificationsClick = onNavigateToNotifications,
                        avatarUrl = uiState.studentAvatarUrl,
                        userName = uiState.studentName
                    )
                }

                MapNavigationMode.EVENT_DETAIL -> {
                    val currentEvent = uiState.selectedEvent
                    if (currentEvent != null) {
                        EventDetailScreen(
                            event = currentEvent,
                            currentUserId = uiState.stableCurrentUserId,
                            isActivePage = isActivePage,
                            onBackClick = { viewModel.onEvent(MapEvent.BackToEvents) },
                            onNavigateToLocationOnMap = {
                                viewModel.onEvent(
                                    MapEvent.StartWalkingNavigation(
                                        destination = GeoCoordinate(it.latitude, it.longitude),
                                        title = it.title,
                                        subtitle = it.locationName
                                    )
                                )
                            },
                            onToggleRegister = { viewModel.onEvent(MapEvent.ToggleRegisterEvent(it)) },
                            onContactHost = { hostId, hostName ->
                                onOpenDirectChat(hostId, hostName)
                            },
                            onDeleteEvent = { eventId ->
                                viewModel.onEvent(MapEvent.DeleteCampusEvent(eventId))
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            viewModel.onEvent(MapEvent.BackToEvents)
                        }
                    }
                }

                MapNavigationMode.MAP -> {
                    MapExplorationContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        isActivePage = isActivePage,
                        hasUnreadNotifications = hasUnreadNotifications,
                        onNavigateToMessages = onNavigateToMessages,
                        onOpenDirectChat = onOpenDirectChat,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onToggleBottomBar = onToggleBottomBar,
                        snackbarHostState = snackbarHostState,
                        onRetrySync = onRetrySync,
                        onOpenCreateAtPlace = { place ->
                            prefilledPlace = place
                            if (place != null) {
                                viewModel.onEvent(MapEvent.SelectPlace(null))
                            }
                            viewModel.onEvent(MapEvent.ToggleCreateDialog(true))
                        }
                    )
                }
            }
        }

        if (uiState.isCreateDialogVisible) {
            CreateEventDrawerSheet(
                prefilledPlace = prefilledPlace,
                userLocation = uiState.userLocation,
                locationClient = viewModel.liveLocationClient,
                onDismiss = {
                    prefilledPlace = null
                    viewModel.onEvent(MapEvent.ToggleCreateDialog(false))
                },
                onPublish = { newEvent, isFlashMeet, category, durationMins ->
                    viewModel.onEvent(
                        MapEvent.CreateCampusEvent(
                            event = newEvent,
                            alsoPublishAsFlashMeet = isFlashMeet,
                            flashMeetCategory = category,
                            durationMinutes = durationMins
                        )
                    )
                    prefilledPlace = null
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp)
        ) { data ->
            CampusSuccessSnackBanner(
                message = data.visuals.message,
                onDismiss = { data.dismiss() }
            )
        }
    }
}

@Composable
private fun MapExplorationContent(
    viewModel: MapViewModel,
    uiState: MapUiState,
    isActivePage: Boolean = true,
    hasUnreadNotifications: Boolean = false,
    onNavigateToMessages: () -> Unit,
    onOpenDirectChat: (peerId: String, peerName: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onToggleBottomBar: (Boolean) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    onRetrySync: () -> Unit,
    onOpenCreateAtPlace: (CampusPlace?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationClient = viewModel.liveLocationClient
    val latestUiState by rememberUpdatedState(uiState)

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val initialCoord = uiState.userLocation ?: uiState.focusedEventLocation
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            if (initialCoord != null) {
                center(Point.fromLngLat(initialCoord.longitude, initialCoord.latitude))
                zoom(16.2)
                pitch(40.0)
            } else {
                center(Point.fromLngLat(0.0, 0.0))
                zoom(2.2)
                pitch(0.0)
            }
        }
    }

    var currentMapCenter by remember {
        mutableStateOf(initialCoord ?: GeoCoordinate(0.0, 0.0))
    }
    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }

    suspend fun recenterToDeviceLocation(zoomLevel: Double = 17.2): Boolean {
        if (!hasLocationPermission) return false
        val loc = locationClient.getLastOrCurrentLocation()
        val target = when {
            loc != null -> GeoCoordinate(loc.latitude, loc.longitude)
            latestUiState.userLocation != null -> latestUiState.userLocation!!
            else -> return false
        }
        currentMapCenter = target
        viewModel.onEvent(MapEvent.UpdateUserLocation(target.latitude, target.longitude))
        mapViewportState.flyTo(
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(target.longitude, target.latitude))
                .zoom(zoomLevel)
                .pitch(if (latestUiState.activeNavigation != null) 48.0 else 38.0)
                .build(),
            animationOptions = MapAnimationOptions.Builder().duration(650L).build()
        )
        return true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            coroutineScope.launch {
                recenterToDeviceLocation()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val isHighAccuracyNeeded = uiState.activeNavigation != null || uiState.arViewMode == ArViewMode.AR_CAMERA
    LaunchedEffect(hasLocationPermission, isHighAccuracyNeeded) {
        if (!hasLocationPermission) return@LaunchedEffect
        locationClient.observeLocationUpdates(highAccuracy = isHighAccuracyNeeded).collect { loc ->
            val target = GeoCoordinate(loc.latitude, loc.longitude)
            currentMapCenter = target
            viewModel.onEvent(MapEvent.UpdateUserLocation(target.latitude, target.longitude))
            if (!hasCenteredOnUser && latestUiState.focusedEventLocation == null) {
                hasCenteredOnUser = true
                coroutineScope.launch {
                    mapViewportState.flyTo(
                        cameraOptions = CameraOptions.Builder()
                            .center(Point.fromLngLat(target.longitude, target.latitude))
                            .zoom(16.5)
                            .pitch(40.0)
                            .build(),
                        animationOptions = MapAnimationOptions.Builder().duration(600L).build()
                    )
                }
            }
            if (latestUiState.isSharingLocation && latestUiState.locationPrivacyScope.allowsCoordinatesBroadcast) {
                viewModel.onEvent(
                    MapEvent.PublishLiveCoordinates(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                        heading = if (loc.hasBearing()) loc.bearing else null
                    )
                )
            }
        }
    }

    LaunchedEffect(uiState.focusedEventLocation) {
        uiState.focusedEventLocation?.let { target ->
            currentMapCenter = target
            mapViewportState.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(target.longitude, target.latitude))
                    .zoom(16.6)
                    .pitch(42.0)
                    .build(),
                animationOptions = MapAnimationOptions.Builder().duration(800L).build()
            )
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (msg.contains("Tap to Retry")) "Tap to Retry" else null,
                withDismissAction = msg.contains("Tap to Retry")
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onRetrySync()
            }
            viewModel.onEvent(MapEvent.ClearFeedback(null))
        }
    }

    var isMyLocationMode by rememberSaveable { mutableStateOf(false) }
    var myLocationStep by rememberSaveable { mutableStateOf(MyLocationSheetStep.LOCATION_REVIEW) }
    var myLocationName by rememberSaveable { mutableStateOf("Me") }
    var myLocationAddress by rememberSaveable { mutableStateOf("Resolving address...") }
    var showShareLocationSheet by rememberSaveable { mutableStateOf(false) }

    val activeHostedMeet = uiState.activeHostedMeet
    val isHostingLiveMeet = activeHostedMeet != null

    LaunchedEffect(showShareLocationSheet) {
        if (showShareLocationSheet && hasLocationPermission) {
            recenterToDeviceLocation(zoomLevel = 17.8)
        }
    }

    LaunchedEffect(isMyLocationMode, currentMapCenter.latitude, currentMapCenter.longitude) {
        if (!isMyLocationMode) return@LaunchedEffect
        val center = currentMapCenter
        if (center.latitude == 0.0 && center.longitude == 0.0) {
            myLocationAddress = "Waiting for device GPS location..."
            return@LaunchedEffect
        }
        val resolved = locationClient.reverseGeocode(center.latitude, center.longitude)
            ?.takeIf { it.isNotBlank() }
            ?: findNearestPlaceAddress(center.latitude, center.longitude, latestUiState.places)
        myLocationAddress = resolved
    }

    BackHandler(
        enabled = isActivePage && (
            uiState.arViewMode == ArViewMode.AR_CAMERA ||
                uiState.isSearchBarActive ||
                isMyLocationMode ||
                showShareLocationSheet ||
                uiState.selectedPlace != null ||
                uiState.selectedMeet != null ||
                uiState.selectedFriendLocation != null
            )
    ) {
        when {
            uiState.arViewMode == ArViewMode.AR_CAMERA -> {
                viewModel.onEvent(
                    MapEvent.SetTiltSwitchPolicy(
                        if (uiState.deviceOrientation.pitchDegrees < 35f) TiltSwitchPolicy.AUTO_TILT
                        else TiltSwitchPolicy.LOCKED_2D
                    )
                )
            }
            uiState.isSearchBarActive -> viewModel.onEvent(MapEvent.SetSearchBarActive(false))
            showShareLocationSheet -> showShareLocationSheet = false
            uiState.selectedFriendLocation != null -> viewModel.onEvent(MapEvent.SelectFriendLocation(null))
            uiState.selectedMeet != null -> viewModel.onEvent(MapEvent.SelectMeet(null))
            uiState.selectedPlace != null -> viewModel.onEvent(MapEvent.SelectPlace(null))
            isMyLocationMode -> {
                if (myLocationStep == MyLocationSheetStep.SELECT_FRIENDS) {
                    myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                } else {
                    isMyLocationMode = false
                }
            }
        }
    }

    val isFullOverlayActive = (uiState.arViewMode == ArViewMode.AR_CAMERA) ||
        (uiState.activeNavigation != null) ||
        isMyLocationMode ||
        (uiState.selectedPlace != null) ||
        showShareLocationSheet

    LaunchedEffect(isFullOverlayActive) {
        onToggleBottomBar(!isFullOverlayActive)
    }

    DisposableEffect(Unit) {
        onDispose {
            onToggleBottomBar(true)
        }
    }

    var isMapPulseLoading by remember { mutableStateOf(true) }
    var isMapboxTilesLoaded by remember { mutableStateOf(false) }
    var minimumPulseElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(950L)
        minimumPulseElapsed = true
        if (!isMapboxTilesLoaded) {
            delay(850L)
            isMapPulseLoading = false
        }
    }

    LaunchedEffect(isMapboxTilesLoaded, minimumPulseElapsed) {
        if (isMapboxTilesLoaded && minimumPulseElapsed) {
            isMapPulseLoading = false
        }
    }

    val showArCamera = isActivePage &&
        uiState.arViewMode == ArViewMode.AR_CAMERA &&
        !isMyLocationMode &&
        !showShareLocationSheet &&
        uiState.selectedPlace == null

    Crossfade(
        targetState = showArCamera,
        animationSpec = tween(durationMillis = 360),
        label = "ArVsMapCrossfade",
        modifier = Modifier.fillMaxSize()
    ) { inArMode ->
        if (inArMode) {
            ArNavigationCameraView(
                userLocation = uiState.userLocation,
                orientationState = uiState.deviceOrientation,
                activeNavigation = uiState.activeNavigation,
                geospatialMode = uiState.geospatialMode,
                tiltSwitchPolicy = uiState.tiltSwitchPolicy,
                onGeospatialModeChanged = { viewModel.onEvent(MapEvent.UpdateGeospatialMode(it)) },
                onSwitchTo2DMap = {
                    viewModel.onEvent(
                        MapEvent.SetTiltSwitchPolicy(
                            if (uiState.deviceOrientation.pitchDegrees < 35f) TiltSwitchPolicy.AUTO_TILT
                            else TiltSwitchPolicy.LOCKED_2D
                        )
                    )
                },
                onToggleTiltPolicy = { viewModel.onEvent(MapEvent.CycleTiltPolicy) },
                onStopNavigation = { viewModel.onEvent(MapEvent.StopWalkingNavigation) },
                onOpenMessages = onNavigateToMessages,
                onOpenDestinationSearch = {
                    viewModel.onEvent(MapEvent.SetTiltSwitchPolicy(TiltSwitchPolicy.LOCKED_2D))
                    viewModel.onEvent(MapEvent.SetSearchBarActive(true))
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Mapbox 2D/3D Standard Vector Map
                MapboxCampusMapView(
                    places = uiState.places,
                    liveMeets = uiState.liveMeets,
                    liveFriendLocations = uiState.liveFriendLocations,
                    selectedFriendUserId = uiState.selectedFriendLocation?.userId,
                    activeNavigation = uiState.activeNavigation,
                    isMyLocationMode = isMyLocationMode,
                    hasLocationPermission = hasLocationPermission,
                    viewportState = mapViewportState,
                    onMapLoaded = { isMapboxTilesLoaded = true },
                    onCameraCenterIdle = { centerCoord ->
                        currentMapCenter = centerCoord
                        if (isMyLocationMode) {
                            coroutineScope.launch {
                                val resolved = locationClient.reverseGeocode(centerCoord.latitude, centerCoord.longitude)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: findNearestPlaceAddress(centerCoord.latitude, centerCoord.longitude, uiState.places)
                                myLocationAddress = resolved
                            }
                        }
                    },
                    onMapClick = { tappedCoord ->
                        if (uiState.isSearchBarActive) {
                            viewModel.onEvent(MapEvent.SetSearchBarActive(false))
                        } else if (isMyLocationMode) {
                            currentMapCenter = tappedCoord
                            coroutineScope.launch {
                                mapViewportState.flyTo(
                                    cameraOptions = CameraOptions.Builder()
                                        .center(Point.fromLngLat(tappedCoord.longitude, tappedCoord.latitude))
                                        .build(),
                                    animationOptions = MapAnimationOptions.Builder().duration(450L).build()
                                )
                            }
                        } else {
                            viewModel.onEvent(MapEvent.SelectPlace(null))
                            viewModel.onEvent(MapEvent.SelectMeet(null))
                            viewModel.onEvent(MapEvent.SelectFriendLocation(null))
                        }
                    },
                    onPlaceClick = { place ->
                        viewModel.onEvent(MapEvent.SelectFriendLocation(null))
                        viewModel.onEvent(MapEvent.SelectMeet(null))
                        viewModel.onEvent(MapEvent.SelectPlace(place))
                        coroutineScope.launch {
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder()
                                    .center(Point.fromLngLat(place.longitude, place.latitude - 0.002))
                                    .zoom(16.4)
                                    .pitch(45.0)
                                    .build(),
                                animationOptions = MapAnimationOptions.Builder().duration(600L).build()
                            )
                        }
                    },
                    onMeetClick = { meet ->
                        viewModel.onEvent(MapEvent.SelectPlace(null))
                        viewModel.onEvent(MapEvent.SelectFriendLocation(null))
                        viewModel.onEvent(MapEvent.SelectMeet(meet))
                    },
                    onFriendClick = { friendLoc ->
                        viewModel.onEvent(MapEvent.SelectPlace(null))
                        viewModel.onEvent(MapEvent.SelectMeet(null))
                        viewModel.onEvent(MapEvent.SelectFriendLocation(friendLoc))
                        coroutineScope.launch {
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder()
                                    .center(Point.fromLngLat(friendLoc.longitude, friendLoc.latitude))
                                    .zoom(16.8)
                                    .pitch(0.0)
                                    .build(),
                                animationOptions = MapAnimationOptions.Builder().duration(600L).build()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Loading Pulse Overlay
                AnimatedVisibility(
                    visible = isMapPulseLoading,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    MapLoadingPulseOverlay()
                }

                // 2. Top Controls + Search Bar (when browsing) OR Hyper AR 2D Navigation HUD (when navigating)
                AnimatedVisibility(
                    visible = !isMapPulseLoading && uiState.selectedPlace == null && !isMyLocationMode,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    val activeNav = uiState.activeNavigation
                    if (activeNav != null && !uiState.isSearchBarActive) {
                        Hyper2dTopManeuverOverlay(
                            session = activeNav,
                            onCancelNavigation = { viewModel.onEvent(MapEvent.StopWalkingNavigation) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MapTopControls(
                                onFlashMeetClick = {
                                    isMyLocationMode = true
                                    myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                                    if (hasLocationPermission) {
                                        coroutineScope.launch { recenterToDeviceLocation(zoomLevel = 17.8) }
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onEventsClick = { viewModel.onEvent(MapEvent.ShowEventsList) },
                                onShareLocationClick = {
                                    showShareLocationSheet = true
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onMessagesClick = onNavigateToMessages,
                                onNotificationsClick = onNavigateToNotifications,
                                isSharingLocation = uiState.isSharingLocation && uiState.locationPrivacyScope.allowsCoordinatesBroadcast,
                                hasUnreadNotifications = hasUnreadNotifications
                            )

                            // Floating Google + Mapbox Search & POI Lookup Bar
                            MapboxDestinationSearchBar(
                                query = uiState.searchQuery,
                                isActive = uiState.isSearchBarActive,
                                isSearching = uiState.isSearchingDestinations,
                                suggestions = uiState.searchSuggestions,
                                onQueryChange = { viewModel.onEvent(MapEvent.UpdateSearchQuery(it)) },
                                onSubmitSearch = { viewModel.onEvent(MapEvent.SubmitDestinationSearch(it)) },
                                onActivate = { viewModel.onEvent(MapEvent.SetSearchBarActive(true)) },
                                onClear = { viewModel.onEvent(MapEvent.ClearDestinationSearch) },
                                onSelectSuggestion = { viewModel.onEvent(MapEvent.SelectSearchSuggestion(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // 2b. Hyper AR 2D Bottom Destination Card + 3 Corner Square Buttons (frame_01 & frame_09)
                AnimatedVisibility(
                    visible = !isMapPulseLoading &&
                        uiState.activeNavigation != null &&
                        !uiState.isSearchBarActive &&
                        uiState.selectedPlace == null &&
                        !isMyLocationMode,
                    enter = fadeIn(tween(260)) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    uiState.activeNavigation?.let { activeNav ->
                        Hyper2dBottomNavigationDeck(
                            session = activeNav,
                            onOpenListClick = { viewModel.onEvent(MapEvent.SetSearchBarActive(true)) },
                            onLaunchArClick = { viewModel.onEvent(MapEvent.ToggleArManualMode) },
                            onRecenterClick = {
                                if (hasLocationPermission) {
                                    coroutineScope.launch { recenterToDeviceLocation(zoomLevel = 17.5) }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            onGotItClick = {
                                if (activeNav.hasArrived) {
                                    viewModel.onEvent(MapEvent.StopWalkingNavigation)
                                } else {
                                    viewModel.onEvent(MapEvent.ToggleArManualMode)
                                }
                            }
                        )
                    }
                }

                // 3. Bottom Filter Strip + Category Dock (hidden while active 2D navigation deck is shown)
                AnimatedVisibility(
                    visible = !isMapPulseLoading &&
                        uiState.activeNavigation == null &&
                        !uiState.isSearchBarActive &&
                        uiState.selectedPlace == null &&
                        uiState.selectedMeet == null &&
                        uiState.selectedFriendLocation == null &&
                        !isMyLocationMode &&
                        !isHostingLiveMeet,
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MapFilterRow(
                            audienceFilter = uiState.audienceFilter,
                            isOpenNowOnly = uiState.isOpenNowOnly,
                            selectedRegion = uiState.selectedRegion,
                            availableRegions = uiState.availableRegions,
                            onAudienceSelected = { viewModel.onEvent(MapEvent.FilterAudience(it)) },
                            onToggleOpenNow = { viewModel.onEvent(MapEvent.ToggleOpenNow) },
                            onRegionSelected = { region ->
                                viewModel.onEvent(MapEvent.FilterRegion(region))
                                coroutineScope.launch {
                                    val target = if (region == "All Hubs") {
                                        uiState.userLocation ?: uiState.regionCentroids.values.firstOrNull()
                                    } else {
                                        uiState.regionCentroids[region] ?: uiState.userLocation
                                    }
                                    if (target != null) {
                                        mapViewportState.flyTo(
                                            cameraOptions = CameraOptions.Builder()
                                                .center(Point.fromLngLat(target.longitude, target.latitude))
                                                .zoom(15.4)
                                                .pitch(40.0)
                                                .build(),
                                            animationOptions = MapAnimationOptions.Builder().duration(700L).build()
                                        )
                                    }
                                    snackbarHostState.showSnackbar("Filtered to $region")
                                }
                            },
                            onRecenterClick = {
                                if (hasLocationPermission) {
                                    coroutineScope.launch {
                                        val centered = recenterToDeviceLocation(zoomLevel = 17.2)
                                        if (centered) {
                                            snackbarHostState.showSnackbar("Centered on your device GPS location")
                                        } else {
                                            snackbarHostState.showSnackbar("Waiting for GPS fix — check that Location is turned on")
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CategoryDock(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.onEvent(MapEvent.FilterCategory(it)) }
                        )
                    }
                }

                // 4. Floating "AR Camera" + Quick Add Event FABs (Bottom Right, hidden during active 2D navigation deck)
                AnimatedVisibility(
                    visible = !isMapPulseLoading &&
                        uiState.activeNavigation == null &&
                        !uiState.isSearchBarActive &&
                        uiState.selectedPlace == null &&
                        uiState.selectedMeet == null &&
                        uiState.selectedFriendLocation == null &&
                        !isMyLocationMode &&
                        !isHostingLiveMeet,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 156.dp, end = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.5.dp, Color(0xFF00D4FF)),
                            color = Color(0xFF0A66FF),
                            modifier = Modifier.clickable {
                                viewModel.onEvent(MapEvent.ToggleArManualMode)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = "Launch AR View",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AR View",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { onOpenCreateAtPlace(null) },
                            shape = CircleShape,
                            containerColor = BrandOnboardingNavy,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Campus Event or Flash Meet",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Top-Left Close Button ("My Location" mode)
                AnimatedVisibility(
                    visible = isMyLocationMode,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 16.dp)
                ) {
                    MyLocationCloseButton(
                        onClick = {
                            isMyLocationMode = false
                            myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                        }
                    )
                }

                // Center Pin during "My Location" / Flash Meet target selection
                AnimatedVisibility(
                    visible = isMyLocationMode,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (myLocationStep == MyLocationSheetStep.SELECT_FRIENDS) 340.dp else 220.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        MyLocationMapPin(
                            label = myLocationName,
                            showAvatar = myLocationStep == MyLocationSheetStep.SELECT_FRIENDS,
                            avatarInitials = uiState.studentInitials,
                            isPulsing = myLocationStep == MyLocationSheetStep.SELECT_FRIENDS,
                            modifier = Modifier.offset(y = (-46).dp)
                        )
                    }
                }

                // "My Location" Review & "Trigger Flash Meet" Friends Selection Drawer
                AnimatedVisibility(
                    visible = isMyLocationMode,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedContent(
                        targetState = myLocationStep,
                        transitionSpec = {
                            if (targetState == MyLocationSheetStep.SELECT_FRIENDS) {
                                (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                )
                            } else {
                                (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(
                                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                )
                            }
                        },
                        label = "myLocationStepTransition"
                    ) { step ->
                        when (step) {
                            MyLocationSheetStep.LOCATION_REVIEW -> {
                                MyLocationBottomSheet(
                                    locationName = myLocationName,
                                    onLocationNameChange = { myLocationName = it },
                                    address = myLocationAddress,
                                    onNextClick = { myLocationStep = MyLocationSheetStep.SELECT_FRIENDS }
                                )
                            }
                            MyLocationSheetStep.SELECT_FRIENDS -> {
                                TriggerFlashMeetFriendsSheet(
                                    locationName = myLocationName,
                                    address = myLocationAddress,
                                    friends = uiState.friends,
                                    onBackClick = { myLocationStep = MyLocationSheetStep.LOCATION_REVIEW },
                                    onCloseClick = {
                                        isMyLocationMode = false
                                        myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                                    },
                                    onShareFlashMeet = { title, category, duration, selectedFriends ->
                                        val target = currentMapCenter
                                        val hostDisplay = uiState.studentName.ifBlank { "Campus Host" }
                                        viewModel.onEvent(
                                            MapEvent.CreateMeet(
                                                title = title,
                                                description = "Flash Meet hosted by $hostDisplay at $myLocationName",
                                                category = category,
                                                locationName = myLocationName,
                                                durationMinutes = duration,
                                                latitude = target.latitude,
                                                longitude = target.longitude,
                                                invitedFriendIds = selectedFriends.map { it.id }
                                            )
                                        )
                                        isMyLocationMode = false
                                        myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                                        coroutineScope.launch {
                                            mapViewportState.flyTo(
                                                cameraOptions = CameraOptions.Builder()
                                                    .center(Point.fromLngLat(target.longitude, target.latitude))
                                                    .zoom(17.2)
                                                    .build(),
                                                animationOptions = MapAnimationOptions.Builder().duration(700L).build()
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Active Live Flash Meet Banner
                AnimatedVisibility(
                    visible = activeHostedMeet != null && !isMyLocationMode && uiState.selectedPlace == null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    activeHostedMeet?.let { hosted ->
                        ActiveFlashMeetLiveBanner(
                            title = hosted.title,
                            locationName = hosted.locationName,
                            invitedCount = hosted.attendeeCount,
                            onEndMeet = { viewModel.onEvent(MapEvent.EndMeet(hosted.id)) }
                        )
                    }
                }

                if (showShareLocationSheet) {
                    ShareLocationDrawerSheet(
                        userName = uiState.studentFirstName,
                        shareUrl = uiState.activeShareUrl,
                        isSharingActive = uiState.isSharingLocation,
                        scope = uiState.locationPrivacyScope,
                        onScopeChange = { newScope ->
                            viewModel.onEvent(MapEvent.UpdateLocationScope(newScope))
                        },
                        onStartSharing = {
                            viewModel.onEvent(MapEvent.StartLocationSharing())
                        },
                        onDismiss = { showShareLocationSheet = false },
                        onEndSharing = {
                            viewModel.onEvent(MapEvent.StopLocationSharing)
                            showShareLocationSheet = false
                        }
                    )
                }

                // Place Detail Sheet -> Directions triggers built-in Mapbox + AR Walking Guidance
                AnimatedVisibility(
                    visible = uiState.selectedPlace != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    uiState.selectedPlace?.let { place ->
                        val isSaved = uiState.savedPlaceIds.contains(place.id)
                        val hasBeen = uiState.beenPlaceIds.contains(place.id)

                        PlaceDetailSheet(
                            place = place,
                            isSavedToTry = isSaved,
                            hasBeen = hasBeen,
                            currentUserName = uiState.studentName,
                            currentUserAvatarUrl = uiState.studentAvatarUrl,
                            onDismiss = { viewModel.onEvent(MapEvent.SelectPlace(null)) },
                            onToggleSavedToTry = { viewModel.onEvent(MapEvent.ToggleSavedToTry(place.id)) },
                            onToggleBeen = { viewModel.onEvent(MapEvent.ToggleBeen(place.id)) },
                            onSubmitComment = { comment ->
                                viewModel.onEvent(MapEvent.SubmitFeedback(place.id, comment))
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Meet me at ${place.name} (${place.campusRegion}): ${place.address} on Patriarch."
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                            },
                            onDirections = {
                                viewModel.onEvent(
                                    MapEvent.StartWalkingNavigation(
                                        destination = GeoCoordinate(place.latitude, place.longitude),
                                        title = place.name,
                                        subtitle = place.address
                                    )
                                )
                            },
                            onHostMeetHere = { onOpenCreateAtPlace(place) }
                        )
                    }
                }

                // Flash Meetup Detail Card Overlay
                AnimatedVisibility(
                    visible = uiState.selectedMeet != null && uiState.selectedPlace == null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    uiState.selectedMeet?.let { meet ->
                        val isJoined = uiState.joinedMeetIds.contains(meet.id)
                        val isHost = uiState.stableCurrentUserId.isNotBlank() &&
                            meet.creatorUserId.equals(uiState.stableCurrentUserId, ignoreCase = true)
                        MeetDetailCard(
                            meet = meet,
                            isHost = isHost,
                            isJoined = isJoined,
                            onJoinToggle = {
                                if (isHost) {
                                    viewModel.onEvent(MapEvent.EndMeet(meet.id))
                                } else {
                                    viewModel.onEvent(MapEvent.ToggleJoinMeet(meet.id))
                                }
                            },
                            onDirections = {
                                viewModel.onEvent(
                                    MapEvent.StartWalkingNavigation(
                                        destination = GeoCoordinate(meet.latitude, meet.longitude),
                                        title = meet.title,
                                        subtitle = meet.locationName
                                    )
                                )
                            },
                            onDismiss = { viewModel.onEvent(MapEvent.SelectMeet(null)) }
                        )
                    }
                }

                // Selected Friend Live Location Card Overlay
                AnimatedVisibility(
                    visible = uiState.selectedFriendLocation != null &&
                        uiState.selectedPlace == null &&
                        uiState.selectedMeet == null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    uiState.selectedFriendLocation?.let { loc ->
                        FriendLocationDetailCard(
                            location = loc,
                            onDirections = {
                                viewModel.onEvent(
                                    MapEvent.StartWalkingNavigation(
                                        destination = GeoCoordinate(loc.latitude, loc.longitude),
                                        title = loc.displayName,
                                        subtitle = "Friend live location"
                                    )
                                )
                            },
                            onMessageFriend = {
                                viewModel.onEvent(MapEvent.SelectFriendLocation(null))
                                onOpenDirectChat(loc.userId, loc.displayName)
                            },
                            onDismiss = { viewModel.onEvent(MapEvent.SelectFriendLocation(null)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapboxDestinationSearchBar(
    query: String,
    isActive: Boolean,
    isSearching: Boolean,
    suggestions: List<SearchSuggestionItem>,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onActivate: () -> Unit,
    onClear: () -> Unit,
    onSelectSuggestion: (SearchSuggestionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!isActive && query.isBlank()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.94f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onActivate)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search destinations",
                        tint = Color(0xFF0A66FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search any place, address, or landmark...",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search any place, street, or landmark...", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(query) }),
                        leadingIcon = {
                            IconButton(onClick = { onSubmitSearch(query) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF0A66FF))
                            }
                        },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = onClear) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (suggestions.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(suggestions, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectSuggestion(item) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (item.isCampusPlace) Color(0xFFEFF6FF)
                                                else Color(0xFFF1F5F9)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = if (item.isCampusPlace) Color(0xFF0A66FF) else Color(0xFF475569),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    item.distanceMeters?.let { dist ->
                                        Text(
                                            text = GeoSpatialMath.formatDistanceShort(dist),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0A66FF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hyper2dTopManeuverOverlay(
    session: ActiveNavigationSession,
    onCancelNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasArrived = session.hasArrived || session.distanceRemainingMeters <= 5.5
    val parsed = remember(session.currentStep, session.distanceRemainingMeters, hasArrived) {
        formatHyperArManeuver(session, hasArrived)
    }

    val pillColor = when {
        hasArrived -> Color(0xFF16A34A)
        parsed.isImmediateNow -> Color(0xFF1967D2)
        else -> Color(0xFF25324B)
    }

    Column(modifier = modifier) {
        // 1. Top Rounded Maneuver Pill (matching uploaded_media_2, frame_01 & frame_09)
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = pillColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (parsed.isArrivalOrFinalApproach) {
                    CheckeredFlagVectorIcon(
                        tint = if (parsed.isImmediateNow) Color.White else Color(0xFFA8B5CD),
                        modifier = Modifier.size(23.dp)
                    )
                } else {
                    Icon(
                        imageVector = iconForManeuver(parsed.direction),
                        contentDescription = parsed.direction.name,
                        tint = if (parsed.isImmediateNow) Color.White else Color(0xFFA8B5CD),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(parsed.boldPrefix)
                        }
                        if (parsed.secondaryAction.isNotEmpty()) {
                            withStyle(
                                SpanStyle(
                                    color = if (parsed.isImmediateNow) Color.White else Color(0xFFA8B5CD),
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(parsed.secondaryAction.replace("\n", " "))
                            }
                        }
                    },
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Top-Left Rounded-Square Close "X" Button directly below maneuver pill (frame_01 & frame_09)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xE6E8EEF8),
            shadowElevation = 3.dp,
            modifier = Modifier
                .size(42.dp)
                .clickable(onClick = onCancelNavigation)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel navigation",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun Hyper2dBottomNavigationDeck(
    session: ActiveNavigationSession,
    onOpenListClick: () -> Unit,
    onLaunchArClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onGotItClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Floating Corner Action Squares above the destination card (frame_01 & frame_09)
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                HyperSquareIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Search destinations",
                    onClick = onOpenListClick
                )
                HyperSquareIconButton(
                    icon = Icons.Default.CropFree,
                    contentDescription = "Switch to AR Camera",
                    onClick = onLaunchArClick
                )
            }

            HyperSquareIconButton(
                icon = Icons.Default.NearMe,
                contentDescription = "Recenter on my location",
                onClick = onRecenterClick,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // 2. Bottom Destination Card + Full-Width Royal Blue "Got it" Pill Button (frame_01 & frame_09)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF5EEF2FA),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            IsometricDestinationBoxIllustration(
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.destinationName,
                            color = Color(0xFF0F172A),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.destinationSubtitle.ifBlank {
                                GeoSpatialMath.formatDistanceShort(session.distanceRemainingMeters)
                            },
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (session.hasArrived) Color(0xFF16A34A) else Color(0xFF1264D8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(onClick = onGotItClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Got it",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendLocationDetailCard(
    location: FriendLiveLocation,
    onDirections: () -> Unit,
    onMessageFriend: () -> Unit,
    onDismiss: () -> Unit
) {
    val speedKmh = location.speedKmh ?: 0f
    val isMoving = speedKmh >= 2.0f
    val statusWording = if (isMoving) {
        "${speedKmh.toInt().coerceAtLeast(2)} km/h · Moving now"
    } else {
        val hoursMins = com.campus.network.feature.map.ui.mapbox.formatZenlyOrbitalDuration(
            stationarySinceEpochMs = location.stationarySinceEpochMs,
            updatedAtEpochMs = location.updatedAtEpochMs,
            expiresAtEpochMs = location.expiresAtEpochMs
        )
        "$hoursMins · ${formatUpdatedAgo(location.updatedAtEpochMs)}"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CampusStudentAvatar(
                        avatarUrl = location.avatarUrl,
                        fullName = location.displayName,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = location.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = statusWording,
                            fontSize = 12.sp,
                            color = if (isMoving) Color(0xFF111318) else Color(0xFF7C3AED),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatCoordinateHemisphere(location.latitude, location.longitude),
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMessageFriend,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOnboardingNavy,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                OutlinedButton(
                    onClick = onDirections,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AR Walk", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MeetDetailCard(
    meet: FlashMeet,
    isHost: Boolean,
    isJoined: Boolean,
    onJoinToggle: () -> Unit,
    onDirections: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = meet.category.name.replace('_', ' '),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = meet.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = meet.locationName,
                    fontSize = 12.5.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (meet.creatorName.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "by ${meet.creatorName}",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = meet.description,
                fontSize = 12.5.sp,
                color = Color(0xFF475569),
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownTimerPill(expiresAtEpochMs = meet.expiresAtEpochMs)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = if (isJoined) Color(0xFF16A34A) else Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${meet.attendeeCount} / ${meet.maxAttendees} Attending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isJoined) Color(0xFF16A34A) else Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onJoinToggle,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isHost -> Color(0xFFDC2626)
                            isJoined -> Color(0xFF16A34A)
                            else -> BrandOnboardingNavy
                        },
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = when {
                            isHost -> Icons.Default.Close
                            isJoined -> Icons.Default.Check
                            else -> Icons.Default.Add
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isHost -> "End Meetup"
                            isJoined -> "Attending (Leave)"
                            else -> "Join Meetup"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = onDirections,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AR Walk",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
