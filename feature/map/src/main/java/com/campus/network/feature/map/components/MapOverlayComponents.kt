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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
 * Top floating bar: Add Friend on the left, Send + Notification Bell on the right.
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
                    text = "Events",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
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
                        contentDescription = "Messages",
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

                    // Active Red Dot Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 9.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
            }
        }
    }
}

/**
 * Filter strip resting above the category dock:
 * "everyone" dropdown, "open now" status pill, and GPS re-center FAB.
 */
@Composable
fun MapFilterRow(
    audienceFilter: AudienceFilter,
    isOpenNowOnly: Boolean,
    onAudienceSelected: (AudienceFilter) -> Unit,
    onToggleOpenNow: () -> Unit,
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAudienceMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 1. Audience Filter Dropdown ("everyone")
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { isAudienceMenuOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = audienceFilter.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isAudienceMenuOpen,
                    onDismissRequest = { isAudienceMenuOpen = false }
                ) {
                    AudienceFilter.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, fontWeight = FontWeight.Medium) },
                            onClick = {
                                onAudienceSelected(option)
                                isAudienceMenuOpen = false
                            }
                        )
                    }
                }
            }

            // 2. "open now" Toggle Pill
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clock illustration matching reference design
                    Image(
                        painter = painterResource(R.drawable.ic_clock_open_now),
                        contentDescription = "Clock",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "open now",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOpenNowOnly) EmeraldLiveGreen else Color(0xFF0F172A)
                    )
                }
            }
        }

        // 3. Re-Center GPS Navigation FAB
        FloatingActionButton(
            onClick = onRecenterClick,
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            containerColor = Color.White.copy(alpha = 0.95f),
            contentColor = BrandOnboardingNavy,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = "Recenter Map",
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Floating frosted horizontal category dock:
 * top picks, eat, cafes, bars, events, go out, shops.
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
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.94f),
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
                val iconDrawable = getCategoryDrawable(category)

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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) BrandOnboardingNavy.copy(alpha = 0.12f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(iconDrawable),
                            contentDescription = category.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = category.label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandOnboardingNavy else Color(0xFF334155)
                    )

                    // Active dot indicator
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(BrandOnboardingNavy)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Map Pin Marker Callout:
 * Shows circular white badge with icon + high-contrast text pill.
 */
@Composable
fun PlaceMarkerCallout(
    place: CampusPlace,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRes = getMarkerIconDrawable(place.iconResName)

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
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = place.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Amber / Green Indicator Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (place.isOpenNow) AmberPinDot else Color(0xFF94A3B8))
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // High-Contrast Pill Banner with Title + Descriptor
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 4.dp,
            border = BorderStroke(0.8.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (place.likedByFriend != null) {
                    Text(
                        text = "@${place.likedByFriend} liked",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOnboardingNavy
                    )
                }

                Text(
                    text = place.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )

                Text(
                    text = place.subtitle,
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Returns corresponding category drawable.
 */
private fun getCategoryDrawable(category: PlaceCategory): Int {
    return when (category) {
        PlaceCategory.TOP_PICKS -> R.drawable.cat_top_picks
        PlaceCategory.EAT -> R.drawable.cat_eat
        PlaceCategory.CAFES -> R.drawable.cat_cafes
        PlaceCategory.BARS -> R.drawable.cat_bars
        PlaceCategory.EVENTS -> R.drawable.cat_events
        PlaceCategory.GO_OUT -> R.drawable.cat_go_out
        PlaceCategory.SHOPS -> R.drawable.cat_shops
    }
}

/**
 * Resolves marker icon drawable.
 */
fun getMarkerIconDrawable(name: String): Int {
    return when (name) {
        "pin_cookie" -> R.drawable.pin_cookie
        "pin_steak" -> R.drawable.pin_steak
        "pin_bakery" -> R.drawable.pin_bakery
        "pin_pasta" -> R.drawable.pin_pasta
        "pin_dumpling" -> R.drawable.pin_dumpling
        "pin_chilli" -> R.drawable.pin_chilli
        "pin_plate" -> R.drawable.pin_plate
        "pin_eliza" -> R.drawable.pin_eliza
        "pin_event_stage" -> R.drawable.pin_event_stage
        else -> R.drawable.pin_bakery
    }
}

/**
 * Resolves avatar drawable.
 */
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

/**
 * Resolves photo drawable.
 */
fun getPhotoDrawable(name: String): Int {
    return when (name) {
        "place_photo_1" -> R.drawable.place_photo_1
        "place_photo_2" -> R.drawable.place_photo_2
        "place_photo_3" -> R.drawable.place_photo_3
        else -> R.drawable.place_photo_1
    }
}
