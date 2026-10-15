package com.campus.network.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import com.campus.network.feature.map.R
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.SavedByUser
import kotlin.math.cos
import kotlin.math.sin

private val SavesBadgePink = Color(0xFFF43F5E)
private val DarkSearchNavy = Color(0xFF0F172A)
private val StarburstPink = Color(0xFFFF71B2)

/**
 * Pixel-Perfect Place Detail Drawer / Sheet:
 * Faithful realization of reference image #2 with Patriarch styling:
 * Top dark search bar, place header with "to try" / "been", photo carousel
 * with starburst badge, editorial blurb, "SAVED BY" social stats & avatars,
 * feedback input, and bottom action pill bar.
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
    onSite: () -> Unit,
    onSocial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

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
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = place.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

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
                    // Header Row: Title & Actions ("to try", "been")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val iconRes = getMarkerIconDrawable(place.iconResName)
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(34.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = place.name.uppercase(),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.3).sp
                            )
                        }

                        // Right: "to try" & "been" buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // "to try"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "to try",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (isSavedToTry) BrandOnboardingNavy.copy(alpha = 0.12f) else Color.White,
                                    border = BorderStroke(1.5.dp, if (isSavedToTry) BrandOnboardingNavy else Color(0xFFE2E8F0)),
                                    onClick = onToggleSavedToTry
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSavedToTry) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Save to try",
                                            tint = if (isSavedToTry) BrandOnboardingNavy else Color(0xFF334155),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // "been"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "been",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (hasBeen) BrandOnboardingNavy else Color.White,
                                    border = BorderStroke(1.5.dp, if (hasBeen) BrandOnboardingNavy else Color(0xFFE2E8F0)),
                                    onClick = onToggleBeen
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Mark as been",
                                            tint = if (hasBeen) Color.White else Color(0xFF334155),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metadata Row: Saves Badge & Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SavesBadgePink
                        ) {
                            Text(
                                text = "${place.savesCount} SAVES",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "${place.subtitle} \u00B7 ${place.priceTier}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ranking Tag & Operating Hours
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SavesBadgePink)
                        )

                        Text(
                            text = place.rankingTag,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "open ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLiveGreen
                        )
                        Text(
                            text = "${place.hoursText.removePrefix("open ")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Photography Carousel with Starburst Badge
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Photo 1
                            Image(
                                painter = painterResource(R.drawable.place_photo_1),
                                contentDescription = "Interior",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )

                            // Photo 2 with Starburst Badge
                            Box(
                                modifier = Modifier
                                    .width(190.dp)
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.place_photo_2),
                                    contentDescription = "Food and Coffee",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Starburst Badge Overlay
                                StarburstBadge(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                )
                            }

                            // Photo 3
                            Image(
                                painter = painterResource(R.drawable.place_photo_3),
                                contentDescription = "Atmosphere",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Neighborhood and Editorial Description
                    Text(
                        text = "${place.neighborhood} \u00B7 ${place.address} \u00B7 ${place.description}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                                    text = "SAVED BY",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MetricItem(
                                        icon = Icons.Default.Bookmark,
                                        count = "${place.stats.saves}",
                                        tint = BrandOnboardingNavy
                                    )
                                    MetricItem(
                                        icon = Icons.Default.Star,
                                        count = "${place.stats.stars}",
                                        tint = Color(0xFFEAB308)
                                    )
                                    MetricItem(
                                        icon = Icons.Default.ThumbUp,
                                        count = "${place.stats.likes}",
                                        tint = Color(0xFF3B82F6)
                                    )
                                    MetricItem(
                                        icon = Icons.Default.ThumbDown,
                                        count = "${place.stats.dislikes}",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Horizontal List of Avatars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                place.savedByUsers.forEach { user ->
                                    UserAvatarItem(user = user)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Community Input Bar ("what do you think?")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avatar_me),
                            contentDescription = "My profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text("what do you think?", fontSize = 13.5.sp, color = Color(0xFF94A3B8))
                            },
                            trailingIcon = {
                                if (commentText.isNotBlank()) {
                                    IconButton(onClick = {
                                        onSubmitComment(commentText)
                                        commentText = ""
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = BrandOnboardingNavy,
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
                                focusedBorderColor = BrandOnboardingNavy,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 6. Action Dock: share, directions, site, ig
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPillButton(
                            label = "share",
                            icon = Icons.Default.Share,
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        )
                        ActionPillButton(
                            label = "directions",
                            icon = Icons.Default.Explore,
                            onClick = onDirections,
                            modifier = Modifier.weight(1.3f)
                        )
                        ActionPillButton(
                            label = "site",
                            icon = Icons.Default.Language,
                            onClick = onSite,
                            modifier = Modifier.weight(0.9f)
                        )
                        ActionPillButton(
                            label = "ig",
                            icon = Icons.Default.CameraAlt,
                            onClick = onSocial,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Geometric Pink Starburst Badge sticker.
 */
@Composable
private fun StarburstBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numPoints = 16
            val outerRadius = size.minDimension / 2f
            val innerRadius = outerRadius * 0.82f
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            val path = Path()
            val angleStep = (Math.PI * 2) / (numPoints * 2)

            for (i in 0 until (numPoints * 2)) {
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val angle = i * angleStep
                val x = centerX + (r * cos(angle)).toFloat()
                val y = centerY + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            // Fill
            drawPath(path, color = StarburstPink)
            // Stroke
            drawPath(path, color = Color.Black, style = Stroke(width = 2.dp.toPx()))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "POPULAR",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "#4",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                lineHeight = 14.sp
            )
            Text(
                text = "bakery",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

/**
 * Single Avatar item in Saved By row.
 */
@Composable
private fun UserAvatarItem(user: SavedByUser) {
    val avatarRes = getAvatarDrawable(user.avatarResName)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(54.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = user.username,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )

            // Status Badge Overlay
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(BrandOnboardingNavy)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (user.isBookmarked) Icons.Default.Bookmark else Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.username,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            maxLines = 1
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
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
    }
}
