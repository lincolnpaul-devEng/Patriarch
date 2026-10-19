@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.campus.network.feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.ui.components.CountdownTimerPill
import com.campus.network.feature.map.components.BrandOnboardingNavy
import com.campus.network.feature.map.components.CategoryDock
import com.campus.network.feature.map.components.MapFilterRow
import com.campus.network.feature.map.components.MapLoadingPulseOverlay
import com.campus.network.feature.map.components.MapTopControls
import com.campus.network.feature.map.components.ActiveFlashMeetLiveBanner
import com.campus.network.feature.map.components.CampusFriend
import com.campus.network.feature.map.components.FlashMeetPerspectivePill
import com.campus.network.feature.map.components.MyLocationBottomSheet
import com.campus.network.feature.map.components.MyLocationCloseButton
import com.campus.network.feature.map.components.MyLocationMapPin
import com.campus.network.feature.map.components.PlaceDetailSheet
import com.campus.network.feature.map.components.PlaceMarkerCallout
import com.campus.network.core.ui.components.CampusSuccessSnackBanner
import com.campus.network.feature.map.components.ShareLocationDrawerSheet
import com.campus.network.feature.map.components.TriggerFlashMeetFriendsSheet
import com.campus.network.feature.map.components.defaultCampusFriends
import com.campus.network.feature.map.events.CreateEventDrawerSheet
import com.campus.network.feature.map.events.EventDetailScreen
import com.campus.network.feature.map.events.EventsScreen
import com.campus.network.feature.map.events.MapNavigationMode
import com.campus.network.feature.map.events.sampleCampusEvents
import androidx.compose.animation.core.tween
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Sheet drawer progression step
enum class MyLocationSheetStep {
    LOCATION_REVIEW,
    SELECT_FRIENDS
}

// Fallback campus centroid when device location is still querying
private val DefaultCampusCentroid = LatLng(-1.3094, 36.8122)

private fun findNearestPlaceAddress(lat: Double, lng: Double, places: List<CampusPlace>): String {
    val nearest = places.minByOrNull { p ->
        val dLat = p.latitude - lat
        val dLng = p.longitude - lng
        dLat * dLat + dLng * dLng
    }
    return nearest?.let { "${it.name}, ${it.address}" }
        ?: "Strathmore Academic Complex, Ole Sangale Rd, Nairobi, Kenya"
}

/**
 * Unified Map Screen:
 * Dynamic Device Location Awareness (Mombasa, Kisumu, Nairobi, etc.),
 * Live Campus Flash Meets, Verified SafeTrade Checkpoints,
 * Campus Events Hub, and Staff-Level UI Layout.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = uiState.navigationMode != MapNavigationMode.MAP) {
        when (uiState.navigationMode) {
            MapNavigationMode.EVENT_DETAIL -> viewModel.onEvent(MapEvent.BackToEvents)
            MapNavigationMode.EVENTS_LIST -> viewModel.onEvent(MapEvent.ShowMap)
            MapNavigationMode.MAP -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.navigationMode,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            label = "MapNavigationModeTransition"
        ) { mode ->
            when (mode) {
                MapNavigationMode.EVENTS_LIST -> {
                    EventsScreen(
                        events = uiState.events,
                        onEventClick = { viewModel.onEvent(MapEvent.SelectEvent(it)) },
                        onNavigateToMap = { viewModel.onEvent(MapEvent.ShowMap) },
                        onCreateEventClick = {
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
                            onBackClick = { viewModel.onEvent(MapEvent.BackToEvents) },
                            onNavigateToLocationOnMap = { viewModel.onEvent(MapEvent.FocusEventLocationOnMap(it)) },
                            onToggleRegister = { viewModel.onEvent(MapEvent.ToggleRegisterEvent(it)) }
                        )
                    } else {
                        viewModel.onEvent(MapEvent.BackToEvents)
                    }
                }

                MapNavigationMode.MAP -> {
                    MapExplorationContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToMessages = onNavigateToMessages,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onToggleBottomBar = onToggleBottomBar,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
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
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onToggleBottomBar: (Boolean) -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // Dynamic Location Permission State
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultCampusCentroid, 15.2f)
    }

    var activeMeetLat by rememberSaveable { mutableStateOf(-1.3094) }
    var activeMeetLng by rememberSaveable { mutableStateOf(36.8122) }

    // Helper to query actual hardware GPS / Network location
    fun detectAndCenterLocation(
        animate: Boolean = true,
        zoomLevel: Float = 18.0f,
        onLocationFound: ((LatLng) -> Unit)? = null
    ) {
        try {
            val enabledProviders = listOfNotNull(
                "fused".takeIf { runCatching { locationManager.isProviderEnabled("fused") }.getOrDefault(false) },
                LocationManager.NETWORK_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) },
                LocationManager.GPS_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) },
                LocationManager.PASSIVE_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) }.getOrDefault(false) }
            )

            var bestLoc: Location? = null
            for (provider in enabledProviders) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null && (bestLoc == null || loc.time > bestLoc.time)) {
                    bestLoc = loc
                }
            }

            if (bestLoc != null) {
                val userLatLng = LatLng(bestLoc.latitude, bestLoc.longitude)
                viewModel.onEvent(MapEvent.UpdateUserLocation(userLatLng.latitude, userLatLng.longitude))
                activeMeetLat = userLatLng.latitude
                activeMeetLng = userLatLng.longitude
                onLocationFound?.invoke(userLatLng)
                if (animate) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel),
                            600
                        )
                    }
                }
            } else {
                // If device has no cached location fix yet, default to campus centroid
                val defaultPos = LatLng(activeMeetLat, activeMeetLng)
                viewModel.onEvent(MapEvent.UpdateUserLocation(defaultPos.latitude, defaultPos.longitude))
                if (animate) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(defaultPos, zoomLevel),
                            600
                        )
                    }
                }
            }

            // Also query immediate fresh fixes across active hardware providers
            val activeProviders = listOfNotNull(
                "fused".takeIf { runCatching { locationManager.isProviderEnabled("fused") }.getOrDefault(false) },
                LocationManager.NETWORK_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) },
                LocationManager.GPS_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) }
            )

            for (provider in activeProviders) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    locationManager.getCurrentLocation(
                        provider,
                        null,
                        androidx.core.content.ContextCompat.getMainExecutor(context)
                    ) { freshLoc ->
                        if (freshLoc != null) {
                            val freshLatLng = LatLng(freshLoc.latitude, freshLoc.longitude)
                            viewModel.onEvent(MapEvent.UpdateUserLocation(freshLatLng.latitude, freshLatLng.longitude))
                            activeMeetLat = freshLatLng.latitude
                            activeMeetLng = freshLatLng.longitude
                            onLocationFound?.invoke(freshLatLng)
                            if (animate) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(freshLatLng, zoomLevel),
                                        500
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(freshLoc: Location) {
                            runCatching { locationManager.removeUpdates(this) }
                            val freshLatLng = LatLng(freshLoc.latitude, freshLoc.longitude)
                            viewModel.onEvent(MapEvent.UpdateUserLocation(freshLatLng.latitude, freshLatLng.longitude))
                            activeMeetLat = freshLatLng.latitude
                            activeMeetLng = freshLatLng.longitude
                            onLocationFound?.invoke(freshLatLng)
                            if (animate) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(freshLatLng, zoomLevel),
                                        500
                                    )
                                }
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    }
                    locationManager.requestLocationUpdates(
                        provider,
                        1000L,
                        1f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (_: SecurityException) {
            // Location permission revoked
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            detectAndCenterLocation(animate = true)
        }
    }

    // Query location dynamically on entry
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            detectAndCenterLocation(animate = true)
        }
    }

    // Focus on event location if navigated from Event Detail
    LaunchedEffect(uiState.focusedEventLocation) {
        uiState.focusedEventLocation?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 16.2f),
                1000
            )
        }
    }

    // React to feedback messages
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(MapEvent.ClearFeedback(null))
        }
    }

    // Pre-filled Meet creation state (from "Host Meet Here")
    var prefilledPlace by remember { mutableStateOf<CampusPlace?>(null) }

    // "My Location" Review Mode & Shared Flash Meet State
    var isMyLocationMode by rememberSaveable { mutableStateOf(false) }
    var myLocationStep by rememberSaveable { mutableStateOf(MyLocationSheetStep.LOCATION_REVIEW) }
    var myLocationName by rememberSaveable { mutableStateOf("Me") }
    var myLocationAddress by rememberSaveable { mutableStateOf("Strathmore Academic Complex, Ole Sangale Rd, Nairobi, Kenya") }

    // Live Shared Flash Meet state
    var isFlashMeetLive by rememberSaveable { mutableStateOf(false) }
    var activeMeetTitle by rememberSaveable { mutableStateOf("Flash Meet") }
    var activeMeetLocation by rememberSaveable { mutableStateOf("Campus Hub") }
    var activeMeetInvitedCount by rememberSaveable { mutableStateOf(0) }
    var isFriendPerspective by rememberSaveable { mutableStateOf(false) }
    var showShareLocationSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showShareLocationSheet) {
        if (showShareLocationSheet) {
            detectAndCenterLocation(animate = true, zoomLevel = 18.0f)
        }
    }

    BackHandler(enabled = isMyLocationMode || isFlashMeetLive || showShareLocationSheet || uiState.selectedPlace != null) {
        if (showShareLocationSheet) {
            showShareLocationSheet = false
        } else if (uiState.selectedPlace != null) {
            viewModel.onEvent(MapEvent.SelectPlace(null))
        } else if (isMyLocationMode) {
            if (myLocationStep == MyLocationSheetStep.SELECT_FRIENDS) {
                myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
            } else {
                isMyLocationMode = false
            }
        } else if (isFlashMeetLive) {
            isFlashMeetLive = false
        }
    }

    // Rule: All drawer sheets in the app must completely hide the bottom tab navigation.
    val isAnyDrawerActive = isMyLocationMode || (uiState.selectedPlace != null) || uiState.isCreateDialogVisible || showShareLocationSheet
    LaunchedEffect(isAnyDrawerActive) {
        onToggleBottomBar(!isAnyDrawerActive)
    }

    DisposableEffect(Unit) {
        onDispose {
            onToggleBottomBar(true)
        }
    }

    // Dynamic reverse geocoding when camera rests in "My Location" mode
    LaunchedEffect(cameraPositionState.isMoving, isMyLocationMode) {
        if (isMyLocationMode && !cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            withContext(Dispatchers.IO) {
                val resolved = try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(target.latitude, target.longitude, 1)
                    if (!list.isNullOrEmpty()) {
                        val addr = list[0]
                        val parts = listOfNotNull(addr.featureName, addr.thoroughfare, addr.subLocality, addr.locality)
                            .filter { it.isNotBlank() }
                            .distinct()
                        if (parts.isNotEmpty()) parts.joinToString(", ") else addr.getAddressLine(0) ?: ""
                    } else {
                        findNearestPlaceAddress(target.latitude, target.longitude, uiState.places)
                    }
                } catch (e: Exception) {
                    findNearestPlaceAddress(target.latitude, target.longitude, uiState.places)
                }
                withContext(Dispatchers.Main) {
                    if (resolved.isNotBlank()) {
                        myLocationAddress = resolved
                    }
                }
            }
        }
    }

    // Map Loading Pulse Animation state when user enters Map screen
    var isMapPulseLoading by remember { mutableStateOf(true) }
    var isGoogleMapTilesLoaded by remember { mutableStateOf(false) }
    var minimumPulseElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1150L)
        minimumPulseElapsed = true
        if (isGoogleMapTilesLoaded) {
            isMapPulseLoading = false
        } else {
            // Maximum fallback so map is never blocked if offline
            delay(850L)
            isMapPulseLoading = false
        }
    }

    LaunchedEffect(isGoogleMapTilesLoaded, minimumPulseElapsed) {
        if (isGoogleMapTilesLoaded && minimumPulseElapsed) {
            isMapPulseLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Google Maps Integration Layer
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(
                bottom = if (isMyLocationMode) {
                    if (myLocationStep == MyLocationSheetStep.SELECT_FRIENDS) 320.dp else 220.dp
                } else if (showShareLocationSheet) {
                    320.dp
                } else if (isFlashMeetLive) {
                    110.dp
                } else {
                    0.dp
                }
            ),
            properties = remember(hasLocationPermission) {
                MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    isBuildingEnabled = true,
                    isTrafficEnabled = false
                )
            },
            uiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                )
            },
            onMapLoaded = {
                isGoogleMapTilesLoaded = true
            },
            onMapClick = { latLng ->
                if (isMyLocationMode) {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLng(latLng), 400)
                    }
                } else {
                    viewModel.onEvent(MapEvent.SelectPlace(null))
                    viewModel.onEvent(MapEvent.SelectMeet(null))
                }
            }
        ) {
            if (!isMyLocationMode) {
                // Render Discoverable Places with Custom Academic Marker Callouts
                uiState.places.forEach { place ->
                    val markerState = rememberMarkerState(
                        key = place.id,
                        position = LatLng(place.latitude, place.longitude)
                    )

                    MarkerComposable(
                        keys = arrayOf<Any>(place.id, uiState.selectedPlace?.id ?: ""),
                        state = markerState,
                        onClick = {
                            viewModel.onEvent(MapEvent.SelectPlace(place))
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(place.latitude - 0.003, place.longitude),
                                        16.0f
                                    ),
                                    600
                                )
                            }
                            true
                        }
                    ) {
                        PlaceMarkerCallout(
                            place = place,
                            onClick = {
                                viewModel.onEvent(MapEvent.SelectPlace(place))
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(place.latitude - 0.003, place.longitude),
                                            16.0f
                                        ),
                                        600
                                    )
                                }
                            }
                        )
                    }
                }

                // Render Active Flash Meets
                uiState.liveMeets.forEach { meet ->
                    Marker(
                        state = MarkerState(position = LatLng(meet.latitude, meet.longitude)),
                        title = meet.title,
                        snippet = "${meet.category.name.replace('_', ' ')} • at ${meet.locationName}",
                        onClick = {
                            viewModel.onEvent(MapEvent.SelectMeet(meet))
                            true
                        }
                    )
                }

                // Render Triggered Live Flash Meet or Active Location Sharing with pulsating marker & user avatar
                if (isFlashMeetLive || showShareLocationSheet) {
                    val pinPosition = LatLng(activeMeetLat, activeMeetLng)
                    val activeMarkerState = rememberMarkerState(
                        key = "active_shared_meet",
                        position = pinPosition
                    )
                    LaunchedEffect(pinPosition) {
                        activeMarkerState.position = pinPosition
                    }
                    MarkerComposable(
                        keys = arrayOf(isFriendPerspective, pinPosition.latitude, pinPosition.longitude, uiState.studentName, uiState.studentInitials),
                        state = activeMarkerState,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.92f)
                    ) {
                        val calloutLabel = if (isFriendPerspective) uiState.studentName else "Me"
                        MyLocationMapPin(
                            label = calloutLabel,
                            showAvatar = true,
                            avatarInitials = uiState.studentInitials,
                            isPulsing = true
                        )
                    }
                }
            }
        }

        // 1b. Jetpack Compose Loading Pulse Animation Overlay while map tiles & live meets initialize
        AnimatedVisibility(
            visible = isMapPulseLoading,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            MapLoadingPulseOverlay()
        }

        // 2. Floating Top Controls (Friends, Events Toggle, Messages/Alerts)
        AnimatedVisibility(
            visible = !isMapPulseLoading && uiState.selectedPlace == null && !isMyLocationMode && !isFlashMeetLive,
            enter = fadeIn(tween(300)),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            MapTopControls(
                onAddFriendClick = {
                    isMyLocationMode = true
                    myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                    detectAndCenterLocation(animate = true, zoomLevel = 18.0f)
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onEventsClick = {
                    viewModel.onEvent(MapEvent.ShowEventsList)
                },
                onMessagesClick = {
                    showShareLocationSheet = true
                    detectAndCenterLocation(animate = true, zoomLevel = 18.0f)
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onNotificationsClick = onNavigateToNotifications
            )
        }

        // 3. Floating Bottom Controls: Filter Strip + Category Dock
        AnimatedVisibility(
            visible = !isMapPulseLoading && uiState.selectedPlace == null && !isMyLocationMode && !isFlashMeetLive,
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
                // Filter Row (Audience, Campus Region switcher, Open Now, GPS FAB)
                MapFilterRow(
                    audienceFilter = uiState.audienceFilter,
                    isOpenNowOnly = uiState.isOpenNowOnly,
                    selectedRegion = uiState.selectedRegion,
                    onAudienceSelected = { viewModel.onEvent(MapEvent.FilterAudience(it)) },
                    onToggleOpenNow = { viewModel.onEvent(MapEvent.ToggleOpenNow) },
                    onRegionSelected = { region ->
                        viewModel.onEvent(MapEvent.FilterRegion(region))
                        coroutineScope.launch {
                            isMapPulseLoading = true
                            val target = when (region) {
                                "Mombasa" -> LatLng(-4.0435, 39.6682)
                                "Kisumu" -> LatLng(-0.0917, 34.7680)
                                "Nairobi" -> LatLng(-1.3094, 36.8122)
                                else -> uiState.userLocation ?: DefaultCampusCentroid
                            }
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(target, 14.8f),
                                750
                            )
                            isMapPulseLoading = false
                            snackbarHostState.showSnackbar("Switched to $region Hub")
                        }
                    },
                    onRecenterClick = {
                        if (hasLocationPermission) {
                            detectAndCenterLocation(animate = true, zoomLevel = 17.5f)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Centered on your device GPS location")
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

                // Frosted Horizontal Category Dock
                CategoryDock(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.onEvent(MapEvent.FilterCategory(it)) }
                )
            }
        }

        // 4. Quick Add Campus Event & Flash Meet FAB (Elevated cleanly above control dock)
        AnimatedVisibility(
            visible = !isMapPulseLoading && uiState.selectedPlace == null && !isMyLocationMode && !isFlashMeetLive,
            enter = fadeIn(tween(300)),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 156.dp, end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    prefilledPlace = null
                    viewModel.onEvent(MapEvent.ToggleCreateDialog(true))
                },
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

        // 5. Top-Left Close Button ("My Location" mode)
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

        // 6. Center Pin with Speech Bubble Callout ("Me") and Ground Anchor Dot
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

        // 7. "My Location" Review & "Trigger Flash Meet" Friends Selection Sheet Drawer
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
                            onNextClick = {
                                myLocationStep = MyLocationSheetStep.SELECT_FRIENDS
                            }
                        )
                    }
                    MyLocationSheetStep.SELECT_FRIENDS -> {
                        TriggerFlashMeetFriendsSheet(
                            locationName = myLocationName,
                            address = myLocationAddress,
                            friends = defaultCampusFriends,
                            onBackClick = {
                                myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                            },
                            onCloseClick = {
                                isMyLocationMode = false
                                myLocationStep = MyLocationSheetStep.LOCATION_REVIEW
                            },
                            onShareFlashMeet = { title, category, duration, selectedFriends ->
                                val target = cameraPositionState.position.target
                                activeMeetLat = target.latitude
                                activeMeetLng = target.longitude
                                activeMeetTitle = title
                                activeMeetLocation = myLocationName
                                activeMeetInvitedCount = selectedFriends.size
                                isFlashMeetLive = true

                                viewModel.onEvent(
                                    MapEvent.CreateMeet(
                                        title = title,
                                        description = "Flash Meet hosted by ${uiState.studentName} with ${selectedFriends.size} campus friends at $myLocationName",
                                        category = category,
                                        locationName = myLocationName,
                                        durationMinutes = duration,
                                        latitude = target.latitude,
                                        longitude = target.longitude
                                    )
                                )

                                isMyLocationMode = false
                                myLocationStep = MyLocationSheetStep.LOCATION_REVIEW

                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(target, 17.5f),
                                        800
                                    )
                                    snackbarHostState.showSnackbar(
                                        "Flash Meet shared! Notifications dispatched to ${selectedFriends.joinToString { it.name }}."
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // 8. Top Floating Perspective Switcher Pill (My View: "Me" vs Friends View: "Lincoln Paul")
        AnimatedVisibility(
            visible = isFlashMeetLive && !isMyLocationMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 14.dp)
        ) {
            FlashMeetPerspectivePill(
                isFriendPerspective = isFriendPerspective,
                onPerspectiveChanged = { isFriendPerspective = it },
                friendPerspectiveName = uiState.studentName
            )
        }

        // 9. Bottom Active Live Flash Meet Banner with "End Meet"
        AnimatedVisibility(
            visible = isFlashMeetLive && !isMyLocationMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            ActiveFlashMeetLiveBanner(
                title = activeMeetTitle,
                locationName = activeMeetLocation,
                invitedCount = activeMeetInvitedCount,
                onEndMeet = {
                    isFlashMeetLive = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Flash Meet ended.")
                    }
                }
            )
        }

        // 10. Share Location Drawer Sheet (Triggered by top-right Send icon)
        if (showShareLocationSheet) {
            var shareScope by remember { mutableStateOf(com.campus.network.core.model.LocationPrivacyScope.VERIFIED_CAMPUS_PEERS) }
            val scopeSlug = when (shareScope) {
                com.campus.network.core.model.LocationPrivacyScope.ESCROW_COUNTERPARTY_ONLY -> "escrow"
                com.campus.network.core.model.LocationPrivacyScope.VERIFIED_CAMPUS_PEERS -> "peers"
                com.campus.network.core.model.LocationPrivacyScope.TIMED_SESSION_30M -> "ttl30m"
                com.campus.network.core.model.LocationPrivacyScope.GHOST_MODE_HIDDEN -> "hidden"
            }
            val preciseShareUrl = if (!shareScope.allowsCoordinatesBroadcast) {
                "Sharing paused (Ghost Mode)"
            } else {
                "https://campus.patriarch.sec/loc/$scopeSlug?lat=${String.format(java.util.Locale.US, "%.5f", activeMeetLat)}&lng=${String.format(java.util.Locale.US, "%.5f", activeMeetLng)}"
            }
            ShareLocationDrawerSheet(
                userName = uiState.studentFirstName,
                shareUrl = preciseShareUrl,
                scope = shareScope,
                onScopeChange = { shareScope = it },
                onDismiss = { showShareLocationSheet = false },
                onEndSharing = {
                    isFlashMeetLive = false
                    showShareLocationSheet = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Location sharing ended")
                    }
                }
            )
        }

        // 5. Place Detail Drawer / Sheet
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
                                "Meet me at ${place.name} (${place.campusRegion}): ${place.address} on Patriarch Campus Network."
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                    },
                    onDirections = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${place.latitude},${place.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Walking directions to ${place.name}: ${place.address}")
                            }
                        }
                    },
                    onHostMeetHere = {
                        prefilledPlace = place
                        viewModel.onEvent(MapEvent.SelectPlace(null))
                        viewModel.onEvent(MapEvent.ToggleCreateDialog(true))
                    }
                )
            }
        }

        // 6. Flash Meetup Selected Detail Card Overlay with Interactive Join/Leave
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
                MeetDetailCard(
                    meet = meet,
                    isJoined = isJoined,
                    onJoinToggle = { viewModel.onEvent(MapEvent.ToggleJoinMeet(meet.id)) },
                    onDirections = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${meet.latitude},${meet.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Meetup at: ${meet.locationName}")
                            }
                        }
                    },
                    onDismiss = { viewModel.onEvent(MapEvent.SelectMeet(null)) }
                )
            }
        }

        // 7. Create Campus Event & Flash Meet Drawer Sheet
        if (uiState.isCreateDialogVisible) {
            CreateEventDrawerSheet(
                prefilledPlace = prefilledPlace,
                userLocation = uiState.userLocation,
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
    }
}

/**
 * Interactive Flash Meetup Detail Card:
 * Shows title, countdown pill, location, attendee count, directions, and instant Join/Leave toggle.
 */
@Composable
private fun MeetDetailCard(
    meet: FlashMeet,
    isJoined: Boolean,
    onJoinToggle: () -> Unit,
    onDirections: () -> Unit,
    onDismiss: () -> Unit
) {
    val attendeeTotal = meet.attendeeCount + (if (isJoined) 1 else 0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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

            // Location & Host Row
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "by ${meet.creatorName}",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false
                )
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

            // Footer: Countdown + Live Attendees Counter
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
                        text = "$attendeeTotal / ${meet.maxAttendees} Attending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isJoined) Color(0xFF16A34A) else Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Join/Leave + Directions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onJoinToggle,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isJoined) Color(0xFF16A34A) else BrandOnboardingNavy,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = if (isJoined) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isJoined) "Attending (Leave)" else "Join Meetup",
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
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Directions",
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
