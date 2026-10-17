package com.campus.network.feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.campus.network.feature.map.components.MapTopControls
import com.campus.network.feature.map.components.PlaceDetailSheet
import com.campus.network.feature.map.components.PlaceMarkerCallout
import com.campus.network.feature.map.events.EventDetailScreen
import com.campus.network.feature.map.events.EventsScreen
import com.campus.network.feature.map.events.MapNavigationMode
import com.campus.network.feature.map.events.sampleCampusEvents
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
import kotlinx.coroutines.launch

// Fallback campus centroid when device location is still querying
private val DefaultCampusCentroid = LatLng(-1.3094, 36.8122)

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
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Submit event proposal to Campus Guild Board")
                            }
                        },
                        onNotificationsClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("All campus alerts are up to date")
                            }
                        }
                    )
                }

                MapNavigationMode.EVENT_DETAIL -> {
                    val currentEvent = uiState.selectedEvent ?: sampleCampusEvents[0]
                    EventDetailScreen(
                        event = currentEvent,
                        onBackClick = { viewModel.onEvent(MapEvent.BackToEvents) },
                        onNavigateToLocationOnMap = { viewModel.onEvent(MapEvent.FocusEventLocationOnMap(it)) },
                        onToggleRegister = { viewModel.onEvent(MapEvent.ToggleRegisterEvent(it)) }
                    )
                }

                MapNavigationMode.MAP -> {
                    MapExplorationContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToMessages = onNavigateToMessages,
                        onNavigateToProfile = onNavigateToProfile,
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
        )
    }
}

@Composable
private fun MapExplorationContent(
    viewModel: MapViewModel,
    uiState: MapUiState,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit,
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

    // Helper to query actual hardware GPS / Network location
    fun detectAndCenterLocation(animate: Boolean = true) {
        try {
            val providers = listOf(
                LocationManager.FUSED_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )
            var bestLoc: Location? = null
            for (provider in providers) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null && (bestLoc == null || loc.time > bestLoc.time)) {
                    bestLoc = loc
                }
            }

            if (bestLoc != null) {
                val userLatLng = LatLng(bestLoc.latitude, bestLoc.longitude)
                viewModel.onEvent(MapEvent.UpdateUserLocation(userLatLng.latitude, userLatLng.longitude))
                if (animate) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 15.8f),
                            800
                        )
                    }
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Google Maps Integration Layer
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
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
            onMapClick = {
                viewModel.onEvent(MapEvent.SelectPlace(null))
                viewModel.onEvent(MapEvent.SelectMeet(null))
            }
        ) {
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
        }

        // 2. Floating Top Controls (Friends, Events Toggle, Messages/Alerts)
        AnimatedVisibility(
            visible = uiState.selectedPlace == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            MapTopControls(
                onAddFriendClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Invite campus peers to your Patriarch study circle")
                    }
                },
                onEventsClick = {
                    viewModel.onEvent(MapEvent.ShowEventsList)
                },
                onMessagesClick = onNavigateToMessages,
                onNotificationsClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Zero unread notifications. All campus channels synchronized.")
                    }
                }
            )
        }

        // 3. Floating Bottom Controls: Filter Strip + Category Dock
        AnimatedVisibility(
            visible = uiState.selectedPlace == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
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
                            val target = when (region) {
                                "Mombasa" -> LatLng(-4.0435, 39.6682)
                                "Kisumu" -> LatLng(-0.0917, 34.7680)
                                "Nairobi" -> LatLng(-1.3094, 36.8122)
                                else -> uiState.userLocation ?: DefaultCampusCentroid
                            }
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(target, 14.8f),
                                800
                            )
                            snackbarHostState.showSnackbar("Switched to $region Hub")
                        }
                    },
                    onRecenterClick = {
                        if (hasLocationPermission) {
                            detectAndCenterLocation(animate = true)
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

        // 4. Quick Add Flash Meet FAB
        AnimatedVisibility(
            visible = uiState.selectedPlace == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 120.dp, end = 16.dp)
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
                    contentDescription = "Create Flash Meet",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
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

        // 7. Create Meetup Dialog
        if (uiState.isCreateDialogVisible) {
            CreateMeetDialog(
                prefilledPlace = prefilledPlace,
                userLocation = uiState.userLocation,
                onDismiss = {
                    prefilledPlace = null
                    viewModel.onEvent(MapEvent.ToggleCreateDialog(false))
                },
                onCreate = { title, desc, cat, loc, dur, lat, lng ->
                    viewModel.onEvent(MapEvent.CreateMeet(title, desc, cat, loc, dur, lat, lng))
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

/**
 * Staff-Level Flash Meet Creator:
 * Allows category chip selection, pre-fills spot name or coordinates, and publishes to Room DB.
 */
@Composable
private fun CreateMeetDialog(
    prefilledPlace: CampusPlace?,
    userLocation: LatLng?,
    onDismiss: () -> Unit,
    onCreate: (String, String, MeetCategory, String, Int, Double?, Double?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(prefilledPlace?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(MeetCategory.STUDY_JAM) }
    var durationMinutes by remember { mutableStateOf(60) }

    val targetLat = prefilledPlace?.latitude ?: userLocation?.latitude
    val targetLng = prefilledPlace?.longitude ?: userLocation?.longitude

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Launch Campus Flash Meet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Visible to campus peers on radar for next $durationMinutes minutes",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips Selector
                Text(
                    text = "Select Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MeetCategory.values().forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) BrandOnboardingNavy else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.name.replace('_', ' '),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you doing?") },
                    placeholder = { Text("e.g., Quick Calculus sprint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note / Instructions") },
                    placeholder = { Text("e.g., Table 4 near power outlets") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("e.g., Quad B Hall 4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val locName = if (location.isNotBlank()) location else "Campus Grounds"
                                onCreate(title, description, selectedCategory, locName, durationMinutes, targetLat, targetLng)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandOnboardingNavy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Publish Meetup", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
