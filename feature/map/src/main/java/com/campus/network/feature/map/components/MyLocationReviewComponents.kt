package com.campus.network.feature.map.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.ui.theme.BrandBluePrimary

/**
 * Campus Friend data model for Flash Meet friend targeting
 */
data class CampusFriend(
    val id: String,
    val name: String,
    val handle: String,
    val faculty: String,
    val avatarInitials: String,
    val avatarBg: Color,
    val isOnline: Boolean = true
)

/**
 * Top-left circular close button to exit "My Location" mode.
 */
@Composable
fun MyLocationCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(46.dp),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Custom Red Teardrop Location Marker with speech bubble callout, optional avatar in head,
 * and radar pulse ring animation radiating around the ground anchor dot.
 */
@Composable
fun MyLocationMapPin(
    label: String = "Me",
    showAvatar: Boolean = false,
    avatarInitials: String = "LP",
    isPulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // 1. Callout Speech Bubble showing location name or friend name
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.ifBlank { "Me" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Downward pointing arrow/triangle for the callout bubble
        Canvas(modifier = Modifier.size(width = 10.dp, height = 5.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = Color.White)
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 2. Red Teardrop Pin with Inner White Circle OR User Avatar
        Box(
            modifier = Modifier.size(width = 44.dp, height = 54.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pinWidth = size.width
                val pinHeight = size.height
                val headRadius = pinWidth / 2f
                val pinColor = Color(0xFFDC2626) // Vibrant red

                val pinPath = Path().apply {
                    // Top circular dome (semicircle)
                    arcTo(
                        rect = Rect(
                            left = 0f,
                            top = 0f,
                            right = pinWidth,
                            bottom = pinWidth
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    // Right curved flank tapering down to bottom tip
                    cubicTo(
                        x1 = pinWidth,
                        y1 = headRadius + (pinHeight - headRadius) * 0.42f,
                        x2 = headRadius + headRadius * 0.45f,
                        y2 = pinHeight - 2.dp.toPx(),
                        x3 = headRadius,
                        y3 = pinHeight
                    )
                    // Left curved flank tapering up from bottom tip
                    cubicTo(
                        x1 = headRadius - headRadius * 0.45f,
                        y1 = pinHeight - 2.dp.toPx(),
                        x2 = 0f,
                        y2 = headRadius + (pinHeight - headRadius) * 0.42f,
                        x3 = 0f,
                        y3 = headRadius
                    )
                    close()
                }

                drawPath(path = pinPath, color = pinColor)

                // When avatar is not shown, render classic white inner circle
                if (!showAvatar) {
                    drawCircle(
                        color = Color.White,
                        radius = headRadius * 0.36f,
                        center = Offset(headRadius, headRadius)
                    )
                }
            }

            // User Avatar Circle inside the red pin head
            if (showAvatar) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFEF08A), Color(0xFFFDE047))
                            )
                        )
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarInitials,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF854D0E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 3. Ground Anchor with Radar Pulse Animation
        if (isPulsing) {
            val infiniteTransition = rememberInfiniteTransition(label = "markerPulse")
            val pulseProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseProgress"
            )

            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2f

                    // Primary expanding ripple wave
                    val r1 = maxRadius * pulseProgress
                    val a1 = (1f - pulseProgress).coerceIn(0f, 1f) * 0.65f
                    drawCircle(
                        color = Color(0xFFDC2626).copy(alpha = a1),
                        radius = r1
                    )

                    // Secondary phase ripple wave
                    val p2 = (pulseProgress + 0.5f) % 1f
                    val r2 = maxRadius * p2
                    val a2 = (1f - p2).coerceIn(0f, 1f) * 0.35f
                    drawCircle(
                        color = Color(0xFFDC2626).copy(alpha = a2),
                        radius = r2
                    )

                    // Center solid ground anchor dot
                    drawCircle(
                        color = Color(0xFFDC2626),
                        radius = 3.dp.toPx()
                    )
                }
            }
        } else {
            // Static Ground Anchor Red Dot
            Canvas(modifier = Modifier.size(5.dp)) {
                drawCircle(
                    color = Color(0xFFDC2626),
                    radius = size.width / 2f
                )
            }
        }
    }
}

/**
 * Step 1: Bottom Sheet card for "My Location" review and confirmation.
 */
@Composable
fun MyLocationBottomSheet(
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    address: String,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            // Header title: "My Location*"
            Text(
                text = buildAnnotatedString {
                    append("My Location")
                    withStyle(SpanStyle(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)) {
                        append("*")
                    }
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Location Name input container (defaults to "Me")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = locationName,
                        onValueChange = onLocationNameChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        ),
                        cursorBrush = SolidColor(BrandBluePrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onNextClick() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address Subtitle
            Text(
                text = address.ifBlank { "Strathmore Academic Complex, Ole Sangale Rd, Nairobi, Kenya" },
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Next Action Button (themed in BrandBluePrimary)
            Button(
                onClick = onNextClick,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBluePrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Step 2: Friends Selection Drawer Sheet to trigger and share Flash Meet.
 */
@Composable
fun TriggerFlashMeetFriendsSheet(
    locationName: String,
    address: String,
    friends: List<CampusFriend> = emptyList(),
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onShareFlashMeet: (String, MeetCategory, Int, List<CampusFriend>) -> Unit,
    modifier: Modifier = Modifier
) {
    var meetTitle by remember { mutableStateOf("$locationName Flash Meet") }
    var selectedCategory by remember { mutableStateOf(MeetCategory.STUDY_JAM) }
    var durationMinutes by remember { mutableStateOf(45) }
    val selectedFriendIds = remember { mutableStateListOf<String>().apply { addAll(friends.take(3).map { it.id }) } }

    val categories = listOf(
        MeetCategory.STUDY_JAM to "Study Jam",
        MeetCategory.PROJECT_HACK to "Project Hack",
        MeetCategory.CAMPUS_WALK to "Campus Walk",
        MeetCategory.SPORTS_PICKUP to "Sports Pickup",
        MeetCategory.SAFETRADE_EXCHANGE to "SafeTrade"
    )

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 18.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Top Navigation & Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("Trigger Flash Meet")
                            withStyle(SpanStyle(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)) {
                                append("*")
                            }
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(onClick = onCloseClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location Recap Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$locationName • $address",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meet Title input
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = meetTitle,
                    onValueChange = { meetTitle = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    ),
                    cursorBrush = SolidColor(BrandBluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) BrandBluePrimary else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Friends Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select Friends to Notify (${selectedFriendIds.size}/${friends.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = if (selectedFriendIds.size == friends.size) "Deselect All" else "Select All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBluePrimary,
                    modifier = Modifier.clickable {
                        if (selectedFriendIds.size == friends.size) {
                            selectedFriendIds.clear()
                        } else {
                            selectedFriendIds.clear()
                            selectedFriendIds.addAll(friends.map { it.id })
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (friends.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No campus peers discovered yet. As students join, they will appear here.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Friends List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                items(friends, key = { it.id }) { friend ->
                    val isChecked = selectedFriendIds.contains(friend.id)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedFriendIds.remove(friend.id)
                                else selectedFriendIds.add(friend.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Circular Avatar with Online Indicator
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = friend.avatarBg,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = friend.avatarInitials,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (friend.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = friend.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${friend.handle} • ${friend.faculty}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedFriendIds.add(friend.id)
                                    else selectedFriendIds.remove(friend.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandBluePrimary,
                                    uncheckedColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Share Flash Meet
            Button(
                onClick = {
                    val selectedList = friends.filter { selectedFriendIds.contains(it.id) }
                    onShareFlashMeet(meetTitle, selectedCategory, durationMinutes, selectedList)
                },
                enabled = selectedFriendIds.isNotEmpty(),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBluePrimary,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Share Flash Meet (${selectedFriendIds.size} Friends)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Top floating perspective switcher: My View ("Me") vs Friends View (Student Name).
 */
@Composable
fun FlashMeetPerspectivePill(
    isFriendPerspective: Boolean,
    onPerspectiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    friendPerspectiveName: String = "Campus Peer"
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            // My View
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (!isFriendPerspective) BrandBluePrimary else Color.Transparent)
                    .clickable { onPerspectiveChanged(false) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "My View: Me",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!isFriendPerspective) Color.White else Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Friends View
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isFriendPerspective) BrandBluePrimary else Color.Transparent)
                    .clickable { onPerspectiveChanged(true) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Friends View: $friendPerspectiveName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFriendPerspective) Color.White else Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Active Live Flash Meet banner on the map after triggering.
 */
@Composable
fun ActiveFlashMeetLiveBanner(
    title: String,
    locationName: String,
    invitedCount: Int,
    onEndMeet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing Green Live Badge
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "LIVE FLASH MEET • $invitedCount NOTIFIED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4ADE80)
                    )
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Pulsing at $locationName",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onEndMeet,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
            ) {
                Text("End Meet", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
