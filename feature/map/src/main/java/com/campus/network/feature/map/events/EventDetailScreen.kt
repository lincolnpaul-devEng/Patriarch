package com.campus.network.feature.map.events

import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.components.CampusStudentAvatar
import com.campus.network.core.ui.components.HomescreenPurple
import com.campus.network.core.ui.components.homescreenDotGrid
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.SurfaceWhite
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextWhite
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.ui.mapbox.MapboxMiniPreviewMap

@Composable
fun EventDetailScreen(
    event: CampusEvent,
    currentUserId: String = "",
    onBackClick: () -> Unit,
    onNavigateToLocationOnMap: (CampusEvent) -> Unit,
    onToggleRegister: (String) -> Unit,
    onContactHost: (hostId: String, hostName: String) -> Unit = { _, _ -> },
    onDeleteEvent: (String) -> Unit = {},
    isActivePage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }
    val isCreator = currentUserId.isNotBlank() &&
        event.creatorId.isNotBlank() &&
        event.creatorId.equals(currentUserId, ignoreCase = true)

    BackHandler(enabled = isActivePage, onBack = onBackClick)

    fun shareEvent() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Join me at ${event.title} (${event.dateTimeFormatted}) — ${event.locationName}: ${event.fullAddress}"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
    }

    fun addToCalendar() {
        val beginMs = event.startTimeEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        val endMs = beginMs + 60L * 60_000L
        val calIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, "${event.locationName}, ${event.fullAddress}")
            putExtra(CalendarContract.Events.DESCRIPTION, event.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMs)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
        }
        runCatching { context.startActivity(calIntent) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomescreenPurple)
            .homescreenDotGrid()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Top Navigation Bar (Back Arrow & Share Pill)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { shareEvent() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 2. Event Artwork Banner Card (only renders featured strip when event is actually featured)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .shadow(16.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Image(
                            painter = painterResource(id = event.artworkRes),
                            contentDescription = event.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        val featuredText = event.featuredLabel?.takeIf { event.isFeatured && it.isNotBlank() }
                        if (featuredText != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0xCC000000), Color(0xF2090D16))
                                        )
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xCCFFFFFF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = featuredText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xCCFFFFFF),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xCCFFFFFF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Event Title, Date, Capacity & Price Badge
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = event.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${event.dateTimeFormatted} · ${event.guestCount}/${event.maxCapacity} spots",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        softWrap = false
                    )

                    if (event.priceLabel != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2616A34A)
                        ) {
                            Text(
                                text = event.priceLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 4. Action Buttons Row (Register, Contact, More)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (event.isUserRegistered) BrandGreen else SurfaceWhite,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable { onToggleRegister(event.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (event.isUserRegistered) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Register",
                                tint = if (event.isUserRegistered) TextWhite else TextPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (event.isUserRegistered) "Going" else "Register",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (event.isUserRegistered) TextWhite else TextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF272B33),
                        border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable {
                                onContactHost(event.creatorId, event.hostName.ifBlank { "Event Host" })
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Contact",
                                tint = TextWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contact",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF272B33),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable { showMoreMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More",
                                    tint = TextWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "More",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to Calendar") },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    addToCalendar()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Event") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    shareEvent()
                                }
                            )
                            if (isCreator) {
                                DropdownMenuItem(
                                    text = { Text("Cancel Event", color = Color(0xFFDC2626)) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onDeleteEvent(event.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }

            // 5. "Location" Section Header & Real Mapbox Preview Card
            item {
                val eventCoord = remember(event.latitude, event.longitude) {
                    GeoCoordinate(event.latitude, event.longitude)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Location",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = event.locationName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = event.fullAddress,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MapboxMiniPreviewMap(
                                center = eventCoord,
                                markerTitle = event.locationName,
                                zoomLevel = 16.0,
                                onMapTap = { onNavigateToLocationOnMap(event) },
                                modifier = Modifier.fillMaxSize()
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xCC0F172A),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .clickable { onNavigateToLocationOnMap(event) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = "Explore Map",
                                        tint = TextWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "View on Map",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // 6. "Hosted By" Section (real organizer data — no hardcoded follower count)
            item {
                val resolvedHost = event.hostName.ifBlank { "Campus Organizer" }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Hosted By",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x1AFFFFFF),
                        border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                CampusStudentAvatar(
                                    avatarUrl = null,
                                    fullName = resolvedHost,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = resolvedHost,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "${event.category} Organizer",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (!isCreator && event.creatorId.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x33FFFFFF),
                                    modifier = Modifier.clickable {
                                        onContactHost(event.creatorId, resolvedHost)
                                    }
                                ) {
                                    Text(
                                        text = "Message",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // 7. "About Event" Description & Real Attendee Preview Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "About Event",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = event.description,
                        fontSize = 14.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (event.attendeeNames.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                                event.attendeeNames.take(3).forEach { guestName ->
                                    CampusStudentAvatar(
                                        avatarUrl = null,
                                        fullName = guestName,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, Color(0xFF0F172A), CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                        }

                        Text(
                            text = when {
                                event.guestCount <= 0 -> "Be the first to register"
                                event.attendeeNames.isNotEmpty() -> {
                                    val extra = (event.guestCount - event.attendeeNames.take(3).size).coerceAtLeast(0)
                                    if (extra > 0) {
                                        "${event.attendeeNames.first()} and $extra others registered"
                                    } else {
                                        "${event.guestCount} registered"
                                    }
                                }
                                else -> "${event.guestCount} registered"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
