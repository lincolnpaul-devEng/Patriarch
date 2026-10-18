package com.campus.network.feature.map.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.feature.map.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Key Campus Landmark Venues for instant pinpoint centering
 */
data class CampusEventVenuePreset(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

private val defaultCampusEventVenues = listOf(
    CampusEventVenuePreset(
        id = "venue-hub",
        name = "Innovation Hub Auditorium",
        address = "Ground Floor, Innovation Complex, Strathmore",
        latitude = -1.3078,
        longitude = 36.8121
    ),
    CampusEventVenuePreset(
        id = "venue-quad",
        name = "Main Quad Pavilion",
        address = "Central Campus Spine & Quad Grounds",
        latitude = -1.3094,
        longitude = 36.8122
    ),
    CampusEventVenuePreset(
        id = "venue-guild",
        name = "Student Guild Hub",
        address = "Guild Pavilion & Student Center",
        latitude = -1.3082,
        longitude = 36.8118
    ),
    CampusEventVenuePreset(
        id = "venue-science",
        name = "Phase 1 Science & Robotics Labs",
        address = "South Quad Science Complex, Nairobi",
        latitude = -1.3105,
        longitude = 36.8115
    ),
    CampusEventVenuePreset(
        id = "venue-library",
        name = "University Main Library (Level 3)",
        address = "Central Library Silent Zone & Pods",
        latitude = -1.3087,
        longitude = 36.8129
    ),
    CampusEventVenuePreset(
        id = "venue-sports",
        name = "Sports Complex & Field Pavilion",
        address = "West Athletic Arena & Sports Field",
        latitude = -1.3098,
        longitude = 36.8135
    )
)

private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/**
 * Modern Campus Event & Flash Meet Creation Drawer Sheet:
 * Features an interactive live Google Map review where users can tap anywhere on the campus
 * map to place the exact pin location instead of manually typing location names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDrawerSheet(
    prefilledPlace: CampusPlace?,
    userLocation: LatLng?,
    onDismiss: () -> Unit,
    onPublish: (CampusEvent, Boolean, MeetCategory, Int) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val scope = rememberCoroutineScope()

    // Event Format: true = Quick Flash Meet (Radar Sprint), false = Scheduled Campus Event
    var isQuickFlashMeet by remember { mutableStateOf(false) }

    // Core Event Details
    var title by remember { mutableStateOf("") }
    var hostName by remember { mutableStateOf("You (Verified Student)") }
    var description by remember { mutableStateOf("") }

    // Live Map Selected Coordinates & Resolved Names
    var selectedLat by remember {
        mutableDoubleStateOf(prefilledPlace?.latitude ?: userLocation?.latitude ?: -1.3078)
    }
    var selectedLng by remember {
        mutableDoubleStateOf(prefilledPlace?.longitude ?: userLocation?.longitude ?: 36.8121)
    }
    var locationName by remember {
        mutableStateOf(prefilledPlace?.name ?: defaultCampusEventVenues[0].name)
    }
    var fullAddress by remember {
        mutableStateOf(prefilledPlace?.address ?: defaultCampusEventVenues[0].address)
    }
    var isMapFullscreenOpen by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(selectedLat, selectedLng), 16.5f)
    }

    // Category Selector
    val categories = listOf("Tech & AI", "Study Jam", "SafeTrade Swap", "Guild Debate", "Sports & Social")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    // Timing
    val timingPresets = listOf("Today, 6:00 PM", "Tomorrow, 2:00 PM", "Friday, 11:00 AM", "This Weekend")
    var selectedTiming by remember { mutableStateOf(timingPresets[0]) }
    var durationMinutes by remember { mutableIntStateOf(60) }

    // Admission / Pricing
    val admissionPresets = listOf("Free Admission", "Free Swap", "Free Entry", "KES 200", "KES 500")
    var selectedPrice by remember { mutableStateOf(admissionPresets[0]) }

    // Capacity Limit
    val capacityPresets = listOf(25, 50, 100, 200, 500)
    var selectedCapacity by remember { mutableIntStateOf(100) }

    // Banner Artwork Options
    val artworkOptions = listOf(
        R.drawable.event_thumb_founders to "Tech Founders",
        R.drawable.event_thumb_eclipse to "SafeTrade Swap",
        R.drawable.event_thumb_paws to "Campus Guild",
        R.drawable.event_thumb_afterhours to "Study All-Night",
        R.drawable.event_hero_trees to "Campus Quad"
    )
    var selectedArtworkRes by remember { mutableIntStateOf(R.drawable.event_thumb_founders) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Drag Indicator & Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Create Campus Event",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Publish to Campus Events Hub and Live Radar Map",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Format Switcher: Quick Flash Meet vs Scheduled Event
                Text(
                    text = "Event Format",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isQuickFlashMeet = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isQuickFlashMeet) Color.White else Color.Transparent,
                        shadowElevation = if (!isQuickFlashMeet) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (!isQuickFlashMeet) BrandBluePrimary else Color(0xFF64748B),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scheduled Event",
                                fontSize = 13.sp,
                                fontWeight = if (!isQuickFlashMeet) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isQuickFlashMeet) BrandBluePrimary else Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isQuickFlashMeet = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isQuickFlashMeet) Color.White else Color.Transparent,
                        shadowElevation = if (isQuickFlashMeet) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = if (isQuickFlashMeet) Color(0xFF047857) else Color(0xFF64748B),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Flash Meet",
                                fontSize = 13.sp,
                                fontWeight = if (isQuickFlashMeet) FontWeight.Bold else FontWeight.Medium,
                                color = if (isQuickFlashMeet) Color(0xFF047857) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // 2. Event Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    placeholder = {
                        Text(if (isQuickFlashMeet) "e.g., Quick Calculus sprint" else "e.g., GDSC Campus AI Hackathon 2026")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(19.dp))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Category Selector Chips
                Column {
                    Text(
                        text = "Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) BrandBluePrimary else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) BrandBluePrimary else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Host Organization / Organizer
                OutlinedTextField(
                    value = hostName,
                    onValueChange = { hostName = it },
                    label = { Text("Host / Organizing Guild") },
                    placeholder = { Text("e.g., Strathmore Tech Guild & IEEE") },
                    leadingIcon = {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(19.dp))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Date & Time / Duration
                if (isQuickFlashMeet) {
                    Column {
                        Text(
                            text = "Flash Meet Duration (Live Radar Active)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(30, 45, 60, 90, 120).forEach { mins ->
                                val isSelected = mins == durationMinutes
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color(0xFF047857) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { durationMinutes = mins }
                                ) {
                                    Text(
                                        text = "$mins mins",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = "Date & Time",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timingPresets.forEach { timeStr ->
                                val isSelected = timeStr == selectedTiming
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) BrandBluePrimary else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedTiming = timeStr }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = timeStr,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. LIVE MAP REVIEW & PINPOINT PICKER
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Venue & Location Review",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Tap anywhere on the live campus map to drop the pin:",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Live green pulse badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap to Pin",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Venue Chips for instant centering
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defaultCampusEventVenues.forEach { preset ->
                            val isSelected = locationName == preset.name
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BrandBluePrimary else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) BrandBluePrimary else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable {
                                    locationName = preset.name
                                    fullAddress = preset.address
                                    selectedLat = preset.latitude
                                    selectedLng = preset.longitude
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(preset.latitude, preset.longitude),
                                                17.2f
                                            )
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Google Map Card with Live Tap-to-Pin
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, BrandBluePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                properties = MapProperties(isMyLocationEnabled = false),
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = false,
                                    compassEnabled = true,
                                    mapToolbarEnabled = false
                                ),
                                onMapClick = { latLng ->
                                    selectedLat = latLng.latitude
                                    selectedLng = latLng.longitude

                                    // Automatic nearest campus landmark resolution
                                    val nearest = defaultCampusEventVenues.minByOrNull {
                                        calculateDistanceMeters(latLng.latitude, latLng.longitude, it.latitude, it.longitude)
                                    }
                                    if (nearest != null && calculateDistanceMeters(latLng.latitude, latLng.longitude, nearest.latitude, nearest.longitude) < 80) {
                                        locationName = nearest.name
                                        fullAddress = nearest.address
                                    } else {
                                        locationName = "Campus Spot (%.4f, %.4f)".format(latLng.latitude, latLng.longitude)
                                        fullAddress = "Pinned Coordinates on Campus Grounds"
                                    }
                                }
                            ) {
                                // Pinned Event Location Marker
                                Marker(
                                    state = MarkerState(position = LatLng(selectedLat, selectedLng)),
                                    title = locationName,
                                    snippet = "Event Venue (Tap map to move pin)"
                                )

                                // Other landmark guide points
                                defaultCampusEventVenues.forEach { preset ->
                                    if (preset.latitude != selectedLat || preset.longitude != selectedLng) {
                                        Marker(
                                            state = MarkerState(position = LatLng(preset.latitude, preset.longitude)),
                                            title = preset.name,
                                            alpha = 0.65f,
                                            onClick = {
                                                locationName = preset.name
                                                fullAddress = preset.address
                                                selectedLat = preset.latitude
                                                selectedLng = preset.longitude
                                                true
                                            }
                                        )
                                    }
                                }
                            }

                            // Top Map Badge (Coordinates & Fullscreen Expand button)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.75f)
                                ) {
                                    Text(
                                        text = "%.4f° S, %.4f° E".format(selectedLat, selectedLng),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.95f),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier.clickable { isMapFullscreenOpen = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Expand Fullscreen Map",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selected Venue Review Card (Automated from Map Tap)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = locationName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = fullAddress,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 7. Capacity & Admission Pricing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Admission
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admission",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            admissionPresets.forEach { price ->
                                val isSelected = price == selectedPrice
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) BrandBluePrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { selectedPrice = price }
                                ) {
                                    Text(
                                        text = price,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) BrandBluePrimary else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Capacity
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guest Capacity",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            capacityPresets.forEach { cap ->
                                val isSelected = cap == selectedCapacity
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) BrandBluePrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { selectedCapacity = cap }
                                ) {
                                    Text(
                                        text = "$cap guests",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) BrandBluePrimary else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. Event Banner Artwork Selector
                Column {
                    Text(
                        text = "Event Banner Theme",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        artworkOptions.forEach { (resId, label) ->
                            val isSelected = resId == selectedArtworkRes
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedArtworkRes = resId }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) BrandBluePrimary else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = label,
                                        modifier = Modifier.matchParentSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandBluePrimary else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // 9. Description & Instructions
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("About this Event / Instructions") },
                    placeholder = {
                        Text("Share what students should bring, session agenda, and contact instructions.")
                    },
                    minLines = 3,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 10. Action Buttons: Publish Event
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val resolvedLocName = if (locationName.isNotBlank()) locationName else "Campus Quad Grounds"
                            val resolvedAddress = if (fullAddress.isNotBlank()) fullAddress else "Central Campus Spine, Strathmore"
                            val resolvedTimingText = if (isQuickFlashMeet) "Live Now (${durationMinutes}m left)" else selectedTiming

                            val newEvent = CampusEvent(
                                id = "evt_${UUID.randomUUID()}",
                                title = title.trim(),
                                hostName = hostName.ifBlank { "You (Verified Student)" },
                                hostAvatarRes = R.drawable.avatar_andrew,
                                artworkRes = selectedArtworkRes,
                                dateTimeFormatted = resolvedTimingText,
                                locationName = resolvedLocName,
                                fullAddress = resolvedAddress,
                                statusBadge = EventBadgeType.GOING,
                                priceLabel = selectedPrice,
                                isFeatured = false,
                                featuredLabel = null,
                                description = description.ifBlank { "Live student event organized on Campus Network. Check in with peers upon arrival." },
                                isUserRegistered = true,
                                category = selectedCategory,
                                latitude = selectedLat,
                                longitude = selectedLng,
                                guestCount = 1,
                                dateSection = "Your Events"
                            )

                            val meetCategory = when (selectedCategory) {
                                "Tech & AI" -> MeetCategory.PROJECT_HACK
                                "Study Jam" -> MeetCategory.STUDY_JAM
                                "SafeTrade Swap" -> MeetCategory.SAFETRADE_EXCHANGE
                                "Guild Debate" -> MeetCategory.CAMPUS_WALK
                                else -> MeetCategory.SPORTS_PICKUP
                            }

                            onPublish(newEvent, isQuickFlashMeet, meetCategory, durationMinutes)
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isQuickFlashMeet) Color(0xFF047857) else BrandBluePrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isQuickFlashMeet) Icons.Default.Radar else Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isQuickFlashMeet) "Publish Flash Meet ($durationMinutes mins)" else "Publish Campus Event",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Interactive Map Picker Dialog
    if (isMapFullscreenOpen) {
        Dialog(
            onDismissRequest = { isMapFullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val fullCameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(selectedLat, selectedLng), 17.5f)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = fullCameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            compassEnabled = true,
                            myLocationButtonEnabled = false
                        ),
                        onMapClick = { latLng ->
                            selectedLat = latLng.latitude
                            selectedLng = latLng.longitude
                            val nearest = defaultCampusEventVenues.minByOrNull {
                                calculateDistanceMeters(latLng.latitude, latLng.longitude, it.latitude, it.longitude)
                            }
                            if (nearest != null && calculateDistanceMeters(latLng.latitude, latLng.longitude, nearest.latitude, nearest.longitude) < 80) {
                                locationName = nearest.name
                                fullAddress = nearest.address
                            } else {
                                locationName = "Campus Spot (%.4f, %.4f)".format(latLng.latitude, latLng.longitude)
                                fullAddress = "Pinned Coordinates on Campus Grounds"
                            }
                        }
                    ) {
                        Marker(
                            state = MarkerState(position = LatLng(selectedLat, selectedLng)),
                            title = locationName,
                            snippet = "Event Venue (Tap to move pin)"
                        )

                        defaultCampusEventVenues.forEach { preset ->
                            Marker(
                                state = MarkerState(position = LatLng(preset.latitude, preset.longitude)),
                                title = preset.name,
                                alpha = 0.7f,
                                onClick = {
                                    locationName = preset.name
                                    fullAddress = preset.address
                                    selectedLat = preset.latitude
                                    selectedLng = preset.longitude
                                    true
                                }
                            )
                        }
                    }

                    // Top Bar for Fullscreen Map
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tap Map to Place Event Pin",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = locationName,
                                    fontSize = 12.sp,
                                    color = BrandBluePrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { isMapFullscreenOpen = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Done",
                                    tint = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Bottom Confirm Button
                    Button(
                        onClick = { isMapFullscreenOpen = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Confirm Venue Location",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
