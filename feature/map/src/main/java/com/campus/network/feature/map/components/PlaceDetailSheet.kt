package com.campus.network.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.campus.network.core.ui.components.CampusStudentAvatar
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.model.SavedByUser
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.feature.map.R
import kotlin.math.cos
import kotlin.math.sin

private val SavesBadgeBlue = Color(0xFF007AFF)
private val DarkSearchNavy = Color(0xFF0F172A)
private val StarburstBadgeGold = Color(0xFFF59E0B)

/**
 * Staff-Level Place Detail Drawer / Sheet:
 * Displays complete campus location intelligence, security level,
 * peer-to-peer SafeTrade verification, Wi-Fi/Power amenities,
 * social saves by classmates, community thoughts, and one-tap flash meet creation.
 */
@Composable
fun PlaceDetailSheet(
    place: CampusPlace,
    isSavedToTry: Boolean,
    hasBeen: Boolean,
    onDismiss: () -> Unit,
    onToggleSavedToTry: () -> Unit,
    onToggleBeen: () -> Unit,
    onSubmitComment: (String) -> Unit,
    onShare: () -> Unit,
    onDirections: () -> Unit,
    onHostMeetHere: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

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

    Column(modifier = modifier.fillMaxSize()) {
        // 1. Top Floating Dark Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                color = DarkSearchNavy,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = "Category",
                        tint = badgeColor,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = place.name,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Detail",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. White Bottom Sheet Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                ) {
                    // Header Row: Title & Actions ("Save Bookmark", "Check-in")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = badgeColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = place.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${place.campusRegion} Campus · ${place.neighborhood}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right: "bookmark" & "been" buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // "bookmark"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "save",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = if (isSavedToTry) SavesBadgeBlue.copy(alpha = 0.12f) else Color.White,
                                    border = BorderStroke(1.5.dp, if (isSavedToTry) SavesBadgeBlue else Color(0xFFE2E8F0)),
                                    onClick = onToggleSavedToTry
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSavedToTry) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Save Bookmark",
                                            tint = if (isSavedToTry) SavesBadgeBlue else Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // "been"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "been",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = if (hasBeen) BrandGreen else Color.White,
                                    border = BorderStroke(1.5.dp, if (hasBeen) BrandGreen else Color(0xFFE2E8F0)),
                                    onClick = onToggleBeen
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Mark as been",
                                            tint = if (hasBeen) Color.White else Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campus Amenities & SafeTrade Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (place.isVerifiedSafeTrade) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SafeTrade Checkpoint", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF), maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(place.hoursText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A), maxLines = 1, softWrap = false)
                            }
                        }

                        if (place.hasWifi) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Wi-Fi", fontSize = 10.5.sp, color = Color(0xFF475569), maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        if (place.hasPowerOutlets) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Power, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Power", fontSize = 10.5.sp, color = Color(0xFF475569), maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security & Supervision Notice
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Security: ${place.securityLevel}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Photography Carousel
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(com.campus.network.core.ui.R.drawable.place_photo_1),
                                contentDescription = "Campus Facility",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )

                            Image(
                                painter = painterResource(com.campus.network.core.ui.R.drawable.place_photo_2),
                                contentDescription = "Study Area",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )

                            Image(
                                painter = painterResource(com.campus.network.core.ui.R.drawable.place_photo_3),
                                contentDescription = "Atmosphere",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Editorial & Facility Description
                    Text(
                        text = "${place.address} — ${place.description}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF334155),
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. "SAVED BY" Social Proof Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Title & Metric Counters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CAMPUS PEER COMMUNITY",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MetricItem(
                                        icon = Icons.Default.Bookmark,
                                        count = "${place.savesCount}",
                                        tint = SavesBadgeBlue
                                    )
                                    MetricItem(
                                        icon = Icons.Default.Star,
                                        count = "${place.stats.stars}",
                                        tint = Color(0xFFEAB308)
                                    )
                                    MetricItem(
                                        icon = Icons.Default.ThumbUp,
                                        count = "${place.stats.likes}",
                                        tint = Color(0xFF16A34A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Horizontal List of Avatars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                place.savedByUsers.forEach { user ->
                                    UserAvatarItem(user = user)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Community Input Bar ("Leave a tip or review")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CampusStudentAvatar(
                            avatarUrl = null,
                            fullName = "",
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text(
                                    text = "Leave study tip or notes on this spot...",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            trailingIcon = {
                                if (commentText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onSubmitComment(commentText)
                                            commentText = ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = SavesBadgeBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SavesBadgeBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 6. Action Dock: Host Meet Here, Directions, Share
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onHostMeetHere,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSearchNavy,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.4f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Host Meet Here", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        ActionPillButton(
                            label = "Directions",
                            icon = Icons.Default.Explore,
                            onClick = onDirections,
                            modifier = Modifier.weight(1.1f)
                        )

                        ActionPillButton(
                            label = "Share",
                            icon = Icons.Default.Share,
                            onClick = onShare,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single Avatar item in Saved By row.
 */
@Composable
private fun UserAvatarItem(user: SavedByUser) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            CampusStudentAvatar(
                avatarUrl = null,
                fullName = user.username,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )

            // Status Badge Overlay
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (user.isBookmarked) Icons.Default.Bookmark else Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = user.username,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Metric Item in Saved By section.
 */
@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = count,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Action Pill Button in bottom dock.
 */
@Composable
private fun ActionPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
