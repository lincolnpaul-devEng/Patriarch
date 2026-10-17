package com.campus.network.feature.map.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.AudienceFilter
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.feature.map.R

val BrandOnboardingNavy = BrandBluePrimary
val BrandOnboardingMid = BrandBlueMid
val EmeraldLiveGreen = BrandGreen
val AmberPinDot = Color(0xFFF59E0B)

/**
 * Top floating bar: Add Friend on the left, Events center pill, Send + Notification Bell on the right.
 */
@Composable
fun MapTopControls(
    onAddFriendClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEventsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile / Add Friend Frosted Pill
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            IconButton(onClick = onAddFriendClick) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Friends",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center: Events Screen Trigger Pill
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
            modifier = Modifier.clickable(onClick = onEventsClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Events",
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Campus Events",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Right: Dual Pill (Send + Bell with unread dot)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMessagesClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Direct Messages",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onNotificationsClick, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Active Green/Red Dot Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 9.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                }
            }
        }
    }
}

/**
 * Filter strip resting above the category dock:
 * "everyone" audience dropdown, campus region switcher (Nairobi, Mombasa, Kisumu), "open now" status pill, and GPS re-center FAB.
 */
@Composable
fun MapFilterRow(
    audienceFilter: AudienceFilter,
    isOpenNowOnly: Boolean,
    selectedRegion: String = "All Hubs",
    onAudienceSelected: (AudienceFilter) -> Unit,
    onToggleOpenNow: () -> Unit,
    onRegionSelected: (String) -> Unit = {},
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAudienceMenuOpen by remember { mutableStateOf(false) }
    var isRegionMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // 1. Campus Region Switcher ("Nairobi", "Mombasa", "Kisumu")
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { isRegionMenuOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedRegion,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isRegionMenuOpen,
                    onDismissRequest = { isRegionMenuOpen = false }
                ) {
                    listOf("All Hubs", "Nairobi", "Mombasa", "Kisumu").forEach { region ->
                        DropdownMenuItem(
                            text = { Text(region, fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                            onClick = {
                                onRegionSelected(region)
                                isRegionMenuOpen = false
                            }
                        )
                    }
                }
            }

            // 2. Audience Filter Dropdown ("everyone")
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { isAudienceMenuOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = audienceFilter.label,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isAudienceMenuOpen,
                    onDismissRequest = { isAudienceMenuOpen = false }
                ) {
                    AudienceFilter.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                            onClick = {
                                onAudienceSelected(option)
                                isAudienceMenuOpen = false
                            }
                        )
                    }
                }
            }

            // 3. "Open Now" Toggle Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isOpenNowOnly) Color(0xFFF0FDF4) else Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                border = BorderStroke(
                    1.dp,
                    if (isOpenNowOnly) EmeraldLiveGreen else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.clickable { onToggleOpenNow() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOpenNowOnly) EmeraldLiveGreen else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "open now",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOpenNowOnly) EmeraldLiveGreen else Color(0xFF0F172A),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 4. Re-Center Dynamic Device Location FAB
        FloatingActionButton(
            onClick = onRecenterClick,
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            containerColor = Color.White.copy(alpha = 0.95f),
            contentColor = BrandOnboardingNavy,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My GPS Location",
                tint = BrandOnboardingNavy,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Floating frosted horizontal category dock with academic icons:
 * Top Picks, Study Zones, SafeTrade Spots, Academic Labs, Campus Guild, Events & Stages.
 */
@Composable
fun CategoryDock(
    selectedCategory: PlaceCategory,
    onCategorySelected: (PlaceCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaceCategory.values().forEach { category ->
                val isSelected = category == selectedCategory
                val iconVector = getCategoryVector(category)
                val iconTint = getCategoryColor(category)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onCategorySelected(category)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) iconTint.copy(alpha = 0.16f)
                                else Color(0xFFF8FAFC)
                            )
                            .border(
                                1.dp,
                                if (isSelected) iconTint.copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = category.label,
                            tint = if (isSelected) iconTint else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = category.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false
                    )

                    // Active dot indicator
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(iconTint)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Map Pin Marker Callout:
 * Displays a clean, high-contrast academic badge + readable text pill without food assets.
 */
@Composable
fun PlaceMarkerCallout(
    place: CampusPlace,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = when {
        place.isVerifiedSafeTrade -> Color(0xFF007AFF)
        place.category == PlaceCategory.STUDY_ZONES -> Color(0xFF16A34A)
        place.category == PlaceCategory.ACADEMIC_LABS -> Color(0xFF8B5CF6)
        place.category == PlaceCategory.EVENTS -> Color(0xFFEC4899)
        else -> BrandOnboardingNavy
    }

    val badgeIcon = when {
        place.isVerifiedSafeTrade -> Icons.Default.Security
        place.category == PlaceCategory.STUDY_ZONES -> Icons.AutoMirrored.Filled.MenuBook
        place.category == PlaceCategory.ACADEMIC_LABS -> Icons.Default.Computer
        place.category == PlaceCategory.GUILD_SERVICES -> Icons.Default.AccountBalance
        place.category == PlaceCategory.EVENTS -> Icons.Default.Event
        else -> Icons.Default.Star
    }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular White Badge with Activity Dot
        Box(contentAlignment = Alignment.TopStart) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = place.name,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Status Indicator Dot (Green if open, Grey if closed)
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (place.isOpenNow) Color(0xFF22C55E) else Color(0xFF94A3B8))
                    .border(1.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // High-Contrast Pill Banner with Title + Descriptor
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                if (place.isVerifiedSafeTrade) {
                    Text(
                        text = "SAFE-TRADE SPOT",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF007AFF),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = place.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${place.campusRegion} · ${place.subtitle}",
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getCategoryVector(category: PlaceCategory): ImageVector {
    return when (category) {
        PlaceCategory.TOP_PICKS -> Icons.Default.Star
        PlaceCategory.STUDY_ZONES -> Icons.AutoMirrored.Filled.MenuBook
        PlaceCategory.RENDEZVOUS -> Icons.Default.Security
        PlaceCategory.ACADEMIC_LABS -> Icons.Default.Computer
        PlaceCategory.GUILD_SERVICES -> Icons.Default.AccountBalance
        PlaceCategory.EVENTS -> Icons.Default.Event
    }
}

private fun getCategoryColor(category: PlaceCategory): Color {
    return when (category) {
        PlaceCategory.TOP_PICKS -> Color(0xFFEAB308) // Amber
        PlaceCategory.STUDY_ZONES -> Color(0xFF16A34A) // Emerald Green
        PlaceCategory.RENDEZVOUS -> Color(0xFF007AFF) // Royal Blue SafeTrade
        PlaceCategory.ACADEMIC_LABS -> Color(0xFF8B5CF6) // Violet
        PlaceCategory.GUILD_SERVICES -> Color(0xFF0D9488) // Teal
        PlaceCategory.EVENTS -> Color(0xFFEC4899) // Rose
    }
}

fun getAvatarDrawable(name: String): Int {
    return when (name) {
        "avatar_eliza" -> R.drawable.avatar_eliza
        "avatar_clin" -> R.drawable.avatar_clin
        "avatar_junebog" -> R.drawable.avatar_junebog
        "avatar_andrew" -> R.drawable.avatar_andrew
        "avatar_me" -> R.drawable.avatar_me
        else -> R.drawable.avatar_eliza
    }
}

fun getPhotoDrawable(name: String): Int {
    return when (name) {
        "place_photo_1" -> R.drawable.place_photo_1
        "place_photo_2" -> R.drawable.place_photo_2
        "place_photo_3" -> R.drawable.place_photo_3
        else -> R.drawable.place_photo_1
    }
}
