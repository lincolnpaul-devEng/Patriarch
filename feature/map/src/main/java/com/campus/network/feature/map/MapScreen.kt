package com.campus.network.feature.map

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.ui.components.CountdownTimerPill
import com.campus.network.core.ui.theme.CampusAlertRed
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

private val InitialMapCenter = LatLng(40.7380, -73.9860)

/**
 * Unified Map Screen UI Flow:
 * Supports exploration on Google Maps, live flash meets,
 * exact-mirror Events Screen, and Event Detail Screen.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
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
                                snackbarHostState.showSnackbar("Create Event: Opening campus event builder")
                            }
                        },
                        onNotificationsClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("You have no unread event notifications")
                            }
                        }
                    )
                }

                MapNavigationMode.EVENT_DETAIL -> {
                    val currentEvent = uiState.selectedEvent ?: sampleCampusEvents[4]
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
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(InitialMapCenter, 13.6f)
    }

    // Focus on event location if navigated from Event Detail
    LaunchedEffect(uiState.focusedEventLocation) {
        uiState.focusedEventLocation?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 15.2f),
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Google Maps Integration Layer
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = remember {
                MapProperties(
                    isMyLocationEnabled = false,
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
            // Render Discoverable Places with Custom Marker Callouts
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
                                    LatLng(place.latitude - 0.004, place.longitude),
                                    15.2f
                                ),
                                700
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
                                        LatLng(place.latitude - 0.004, place.longitude),
                                        15.2f
                                    ),
                                    700
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
                    snippet = "${meet.category.name.replace('_', ' ')} • ${meet.locationName}",
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
                        snackbarHostState.showSnackbar("Invite campus friends to your Patriarch circle")
                    }
                },
                onEventsClick = {
                    viewModel.onEvent(MapEvent.ShowEventsList)
                },
                onMessagesClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Opening Patriarch direct channels")
                    }
                },
                onNotificationsClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("No unread alerts")
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
                // Filter Row (Audience dropdown, "open now", GPS FAB)
                MapFilterRow(
                    audienceFilter = uiState.audienceFilter,
                    isOpenNowOnly = uiState.isOpenNowOnly,
                    onAudienceSelected = { viewModel.onEvent(MapEvent.FilterAudience(it)) },
                    onToggleOpenNow = { viewModel.onEvent(MapEvent.ToggleOpenNow) },
                    onRecenterClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(InitialMapCenter, 13.6f),
                                800
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
                onClick = { viewModel.onEvent(MapEvent.ToggleCreateDialog(true)) },
                shape = CircleShape,
                containerColor = BrandOnboardingNavy,
                contentColor = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Flash Meet",
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
                            putExtra(Intent.EXTRA_TEXT, "Check out ${place.name} on Patriarch: ${place.address}")
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
                                snackbarHostState.showSnackbar("Directions to ${place.name}: ${place.address}")
                            }
                        }
                    },
                    onSite = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Opening website for ${place.name}")
                        }
                    },
                    onSocial = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Connecting to @${place.name.lowercase().replace(" ", "")} on Instagram")
                        }
                    }
                )
            }
        }

        // 6. Flash Meetup Selected Detail Card Overlay
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
                MeetDetailCard(
                    meet = meet,
                    onDismiss = { viewModel.onEvent(MapEvent.SelectMeet(null)) }
                )
            }
        }

        // 7. Create Meetup Dialog
        if (uiState.isCreateDialogVisible) {
            CreateMeetDialog(
                onDismiss = { viewModel.onEvent(MapEvent.ToggleCreateDialog(false)) },
                onCreate = { title, desc, cat, loc, dur ->
                    viewModel.onEvent(MapEvent.CreateMeet(title, desc, cat, loc, dur))
                }
            )
        }
    }
}

@Composable
private fun MeetDetailCard(
    meet: FlashMeet,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meet.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${meet.category.name.replace('_', ' ')} • at ${meet.locationName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = meet.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownTimerPill(
                    expiresAtEpochMs = meet.expiresAtEpochMs
                )

                Text(
                    text = "Created by ${meet.creatorName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateMeetDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, com.campus.network.core.model.MeetCategory, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(com.campus.network.core.model.MeetCategory.STUDY_JAM) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Launch Flash Meet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Visible on campus radar for next 60 minutes",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you doing?") },
                    placeholder = { Text("e.g., Quick Calculus sprint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details / Note") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreate(title, description, selectedCategory, location, 60)
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = BrandOnboardingNavy
                        )
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
