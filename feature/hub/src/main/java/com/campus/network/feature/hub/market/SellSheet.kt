package com.campus.network.feature.hub.market

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.core.data.RendezvousSpot
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.network.CloudinaryUploader
import com.campus.network.core.ui.components.HomescreenInk
import com.campus.network.core.ui.components.HomescreenOrange
import com.campus.network.core.ui.components.drawerDotGrid
import com.campus.network.feature.hub.HubEvent
import com.campus.network.feature.hub.bestfootball.SquadaOne
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

private val ItemCategories = listOf("Electronics", "Books", "Fashion", "Furniture", "Beauty", "Sports", "Services", "Other")
private val ItemConditions = listOf("NEW", "LIKE NEW", "GOOD", "FAIR")
private val PickupTimings = listOf("IMMEDIATE", "TODAY", "THIS WEEKEND", "FLEXIBLE")
private val GigUrgencies = listOf("NORMAL", "HIGH", "URGENT")
private const val VIEWPORT_FALLBACK_LAT = -1.2921
private const val VIEWPORT_FALLBACK_LNG = 36.8219

private fun normalizeKenyanPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.startsWith("254") -> digits
        digits.startsWith("0") && digits.length == 10 -> "254${digits.drop(1)}"
        else -> digits
    }
}

private fun isValidPayoutPhone(normalized: String) = Regex("^254[17]\\d{8}$").matches(normalized)

/**
 * The rebuilt Sell flow: one orange sheet with two tabs, fed by real rendezvous spots
 * and a real payout phone. No hardcoded pickup points, no auto-filled ratings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellSheet(
    profile: StudentProfile?,
    rendezvousSpots: List<RendezvousSpot>,
    onEvent: (HubEvent) -> Unit,
    onDismiss: () -> Unit,
    onOpenMySelling: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomescreenOrange,
        dragHandle = null
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .drawerDotGrid()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 34.dp)
            ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "SELL ON CAMPUS",
                        fontFamily = SquadaOne,
                        color = HomescreenInk,
                        fontSize = 23.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        if (selectedTab == 0) "List an item with Safe Trade pickup" else "Post a gig students can join",
                        color = HomescreenInk.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onOpenMySelling,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HomescreenInk)
                ) {
                    Icon(
                        Icons.Filled.QrCode,
                        contentDescription = "My Selling",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = HomescreenInk, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            SellTabSwitch(selectedTab) { selectedTab = it }
            Spacer(Modifier.height(16.dp))
            if (selectedTab == 0) {
                SellItemTab(profile, rendezvousSpots, onEvent, onDismiss)
            } else {
                SellGigTab(profile, onEvent, onDismiss)
            }
        }
    }
}

@Composable
private fun SellTabSwitch(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomescreenInk)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("SELL AN ITEM", "POST A GIG").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected == index) HomescreenOrange else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontFamily = SquadaOne,
                    color = if (selected == index) HomescreenInk else Color.White,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SellItemTab(
    profile: StudentProfile?,
    rendezvousSpots: List<RendezvousSpot>,
    onEvent: (HubEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var condition by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var selectedSpotId by remember { mutableStateOf<String?>(null) }
    var pinnedSpot by remember { mutableStateOf<LatLng?>(null) }
    var pickupTiming by remember { mutableStateOf<String?>(null) }
    var payoutPhone by remember { mutableStateOf(profile?.phoneNumber.orEmpty()) }
    var requiresQr by remember { mutableStateOf(true) }
    var releaseCode by remember {
        mutableStateOf("CP-${(100000..999999).random()}")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris -> if (uris.isNotEmpty()) photoUris = (photoUris + uris).distinct().take(5) }

    val viewport = pinnedSpot
        ?: rendezvousSpots.firstOrNull { it.latitude != null && it.longitude != null }
            ?.let { LatLng(it.latitude!!, it.longitude!!) }
        ?: LatLng(VIEWPORT_FALLBACK_LAT, VIEWPORT_FALLBACK_LNG)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(viewport, 15f)
    }
    val uiSettings = remember { MapUiSettings().copy(zoomControlsEnabled = false) }

    SectionLabel("PHOTOS")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(2.dp, HomescreenInk, RoundedCornerShape(14.dp))
                    .clickable {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = "Add photos", tint = HomescreenInk, modifier = Modifier.size(22.dp))
                    Text("+ ADD", fontFamily = SquadaOne, color = HomescreenInk, fontSize = 11.sp)
                }
            }
        }
        items(photoUris) { uri ->
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                AsyncImage(uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick = { photoUris = photoUris - uri },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(HomescreenInk)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
    if (photoUris.isEmpty()) {
        Text(
            "Add up to 5 real photos. Listings without photos get ignored.",
            color = HomescreenInk.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
    }

    Spacer(Modifier.height(14.dp))
    WhiteField(label = "ITEM TITLE", value = title, onValueChange = { title = it }, singleLine = true)
    Spacer(Modifier.height(10.dp))
    WhiteField(
        label = "PRICE (KES)",
        value = priceText,
        onValueChange = { priceText = it.filter { ch -> ch.isDigit() }.take(7) },
        singleLine = true,
        keyboardType = KeyboardType.Number
    )
    Spacer(Modifier.height(14.dp))
    SectionLabel("CATEGORY")
    ChipRow(ItemCategories, category) { category = it }
    Spacer(Modifier.height(14.dp))
    SectionLabel("CONDITION")
    ChipRow(ItemConditions, condition) { condition = it }
    Spacer(Modifier.height(14.dp))
    WhiteField(
        label = "DESCRIPTION",
        value = description,
        onValueChange = { description = it },
        singleLine = false,
        maxLines = 4,
        minHeight = 96.dp
    )

    Spacer(Modifier.height(16.dp))
    SectionLabel("SAFE PICKUP POINT")
    if (rendezvousSpots.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rendezvousSpots.forEach { spot ->
                val isChosen = selectedSpotId == spot.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isChosen) HomescreenInk else Color.White)
                        .clickable {
                            selectedSpotId = if (isChosen) null else spot.id
                            if (!isChosen) pinnedSpot = null
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = if (isChosen) HomescreenOrange else HomescreenInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            spot.name,
                            fontFamily = SquadaOne,
                            color = if (isChosen) Color.White else HomescreenInk,
                            fontSize = 15.sp
                        )
                        val badges = buildList {
                            add(spot.securityLevel.ifBlank { "UNGRADED" })
                            if (spot.hasCctv) add("CCTV")
                            if (spot.hasSecurityDesk) add("SECURITY DESK")
                            if (spot.operatingHours.isNotBlank()) add(spot.operatingHours)
                        }
                        Text(
                            badges.joinToString("  •  "),
                            color = if (isChosen) Color.White.copy(alpha = 0.75f) else HomescreenInk.copy(alpha = 0.65f),
                            fontSize = 10.sp
                        )
                    }
                    if (isChosen) {
                        Text("SELECTED", fontFamily = SquadaOne, color = HomescreenOrange, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    } else {
        Text(
            "No official campus spots are published yet — drop a pin on the map instead.",
            color = HomescreenInk.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
    }
    Text(
        if (pinnedSpot != null) "Map pin selected (seller chosen point)"
        else "Or drop a pin for a seller-chosen meeting point",
        color = HomescreenInk.copy(alpha = 0.7f),
        fontSize = 11.sp
    )
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                pinnedSpot = latLng
                selectedSpotId = null
            }
        ) {
            pinnedSpot?.let { pin ->
                Marker(state = rememberMarkerState(position = pin), title = "Seller chosen point")
            }
            rendezvousSpots.filter { it.latitude != null && it.longitude != null }.forEach { spot ->
                Marker(
                    state = rememberMarkerState(position = LatLng(spot.latitude!!, spot.longitude!!)),
                    title = spot.name
                )
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    SectionLabel("PICKUP TIMING")
    ChipRow(PickupTimings, pickupTiming) { pickupTiming = it }

    Spacer(Modifier.height(16.dp))
    SectionLabel("PAYOUT — M-PESA")
    WhiteField(
        label = "YOUR M-PESA NUMBER",
        value = payoutPhone,
        onValueChange = { payoutPhone = it },
        singleLine = true,
        keyboardType = KeyboardType.Phone,
        supportingText = "Payouts go to your registered Safaricom number on release."
    )

    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.QrCode, contentDescription = null, tint = HomescreenInk, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Safe Trade release QR", fontFamily = SquadaOne, color = HomescreenInk, fontSize = 14.sp)
            Text(
                "Buyer scans your code in person to unlock M-Pesa funds.",
                color = HomescreenInk.copy(alpha = 0.65f),
                fontSize = 10.sp
            )
        }
        Switch(
            checked = requiresQr,
            onCheckedChange = { requiresQr = it },
            colors = SwitchDefaults.colors(
                checkedTrackColor = HomescreenInk,
                checkedThumbColor = HomescreenOrange,
                uncheckedTrackColor = Color(0xFFD9D2E5),
                uncheckedThumbColor = Color.White
            )
        )
    }
    if (requiresQr) {
        Text(
            "Release code: $releaseCode",
            fontFamily = SquadaOne,
            color = HomescreenInk,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }

    errorMessage?.let { message ->
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = Color(0xFFB3261E),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val spot = rendezvousSpots.firstOrNull { it.id == selectedSpotId }
            val errors = buildList {
                if (title.isBlank()) add("Add a title")
                val price = priceText.toDoubleOrNull()
                if (price == null || price <= 0.0) add("Set a real price in KES")
                if (category == null) add("Pick a category")
                if (condition == null) add("Pick a condition")
                if (spot == null && pinnedSpot == null) add("Choose a safe pickup point or drop a map pin")
                if (pickupTiming == null) add("Pick a pickup timing")
                val normalizedPhone = normalizeKenyanPhone(payoutPhone)
                if (!isValidPayoutPhone(normalizedPhone)) add("Enter a valid M-Pesa number (07XX… or 01XX…)")
            }
            if (errors.isNotEmpty()) {
                errorMessage = errors.first()
                return@Button
            }
            errorMessage = null
            isPublishing = true
            scope.launch {
                val uploadedUrls = mutableListOf<String>()
                photoUris.forEach { uri ->
                    CloudinaryUploader().uploadFromUri(context, uri)
                        .onSuccess { uploadedUrls.add(it) }
                        .onFailure { error ->
                            isPublishing = false
                            errorMessage = "Photo upload failed: ${error.message ?: "try again"}"
                        }
                    if (errorMessage != null) return@launch
                }
                val chosenSpot = spot
                val pin = pinnedSpot
                val lat = chosenSpot?.latitude ?: pin?.latitude ?: 0.0
                val lng = chosenSpot?.longitude ?: pin?.longitude ?: 0.0
                onEvent(
                    HubEvent.PostItem(
                        title = title.trim(),
                        price = priceText.toDouble(),
                        category = category ?: "",
                        condition = condition ?: "",
                        location = chosenSpot?.name ?: "Seller chosen point",
                        description = description.trim(),
                        pickupSpotName = chosenSpot?.name ?: "Seller chosen point",
                        pickupSecurityLevel = chosenSpot?.securityLevel?.ifBlank { "SELLER CHOSEN POINT" }
                            ?: "SELLER CHOSEN POINT",
                        pickupLatitude = lat,
                        pickupLongitude = lng,
                        pickupTiming = pickupTiming ?: "",
                        payoutMethod = "MPESA",
                        requiresQrReleaseCode = requiresQr,
                        qrReleaseCode = if (requiresQr) releaseCode else "",
                        imageUrl = uploadedUrls.firstOrNull(),
                        imageUrls = uploadedUrls
                    )
                )
                isPublishing = false
                onDismiss()
            }
        },
        enabled = !isPublishing,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = HomescreenInk,
            disabledContainerColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isPublishing) {
            CircularProgressIndicator(color = HomescreenInk, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("UPLOADING PHOTOS…", fontFamily = SquadaOne, fontSize = 14.sp)
        } else {
            Text("PUBLISH LISTING", fontFamily = SquadaOne, fontSize = 16.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SellGigTab(
    profile: StudentProfile?,
    onEvent: (HubEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var payoutText by remember { mutableStateOf("") }
    var spotsText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf<String?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val bannerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) bannerUri = uri }

    WhiteField(label = "GIG TITLE", value = title, onValueChange = { title = it }, singleLine = true)
    Spacer(Modifier.height(10.dp))
    WhiteField(
        label = "WHAT NEEDS DOING?",
        value = description,
        onValueChange = { description = it },
        singleLine = false,
        maxLines = 4,
        minHeight = 96.dp
    )
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WhiteField(
            label = "PAYOUT (KES)",
            value = payoutText,
            onValueChange = { payoutText = it.filter { ch -> ch.isDigit() }.take(7) },
            singleLine = true,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
        WhiteField(
            label = "STUDENT SPOTS",
            value = spotsText,
            onValueChange = { spotsText = it.filter { ch -> ch.isDigit() }.take(3) },
            singleLine = true,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))
    WhiteField(
        label = "ESTIMATED DURATION",
        value = durationText,
        onValueChange = { durationText = it },
        singleLine = true
    )
    Spacer(Modifier.height(14.dp))
    SectionLabel("URGENCY")
    ChipRow(GigUrgencies, urgency) { urgency = it }
    Spacer(Modifier.height(14.dp))
    SectionLabel("CATEGORY TAG")
    ChipRow(ItemCategories, tag) { tag = it }
    Spacer(Modifier.height(14.dp))
    SectionLabel("BANNER PHOTO (OPTIONAL)")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable {
                bannerPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = HomescreenInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (bannerUri != null) "Banner selected — tap to change" else "Choose a photo from your gallery",
            color = HomescreenInk.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }

    errorMessage?.let { message ->
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = Color(0xFFB3261E),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val spots = spotsText.toIntOrNull()
            val errors = buildList {
                if (title.isBlank()) add("Add a gig title")
                if (description.isBlank()) add("Describe what needs doing")
                val payout = payoutText.toDoubleOrNull()
                if (payout == null || payout <= 0.0) add("Set a real payout in KES")
                if (spots == null || spots < 1) add("Set how many students can join")
                if (durationText.isBlank()) add("Estimate the duration")
                if (urgency == null) add("Pick an urgency level")
                if (tag == null) add("Pick a category tag")
            }
            if (errors.isNotEmpty()) {
                errorMessage = errors.first()
                return@Button
            }
            errorMessage = null
            isPublishing = true
            scope.launch {
                var bannerUrl: String? = null
                bannerUri?.let { uri ->
                    CloudinaryUploader().uploadFromUri(context, uri)
                        .onSuccess { bannerUrl = it }
                        .onFailure { error ->
                            isPublishing = false
                            errorMessage = "Banner upload failed: ${error.message ?: "try again"}"
                        }
                    if (errorMessage != null) return@launch
                }
                onEvent(
                    HubEvent.PostGig(
                        title = title.trim(),
                        description = description.trim(),
                        payout = payoutText.toDouble(),
                        duration = durationText.trim(),
                        tag = tag ?: "",
                        urgency = GigUrgency.valueOf(urgency ?: "NORMAL"),
                        spotsAvailable = spots ?: 1,
                        bannerUrl = bannerUrl
                    )
                )
                isPublishing = false
                onDismiss()
            }
        },
        enabled = !isPublishing,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = HomescreenInk,
            disabledContainerColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isPublishing) {
            CircularProgressIndicator(color = HomescreenInk, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("UPLOADING…", fontFamily = SquadaOne, fontSize = 14.sp)
        } else {
            Text("POST GIG", fontFamily = SquadaOne, fontSize = 16.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontFamily = SquadaOne,
        color = HomescreenInk,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun WhiteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = if (singleLine) 1 else 6,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    supportingText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth().then(if (minHeight > 0.dp) Modifier.height(minHeight + 48.dp) else Modifier)) {
        Text(
            label,
            fontFamily = SquadaOne,
            color = HomescreenInk,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = HomescreenInk,
                focusedTextColor = HomescreenInk,
                unfocusedTextColor = HomescreenInk
            ),
            modifier = Modifier.fillMaxWidth()
        )
        supportingText?.let {
            Text(it, color = HomescreenInk.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            items(options) { option ->
                val isChosen = selected == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isChosen) HomescreenInk else Color.White)
                        .clickable { onSelect(if (isChosen) null else option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        option,
                        fontFamily = SquadaOne,
                        color = if (isChosen) Color.White else HomescreenInk,
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}
