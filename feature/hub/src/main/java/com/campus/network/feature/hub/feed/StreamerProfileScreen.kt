package com.campus.network.feature.hub.feed

import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.campus.network.core.ui.components.CampusStudentAvatar
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Dimensions below were measured from the reference mockup (screen ~326px wide)
 * and converted to dp against a 393dp-wide baseline (scale ~1.205).
 */
private val ScreenBg = Color(0xFF050505)
private val CardBg = Color(0xFF181818)
private val HeatCardBg = Color(0xFF141414)
private val TabTrackBg = Color(0xFF1C1C1C)
private val TabSelectedBg = Color(0xFF4A4A4A)
private val MutedText = Color(0xFF8E8E8E)
private val BioText = Color(0xFFA6A6A6)
private val Orange = Color(0xFFFFA24A)
private val DeepOrange = Color(0xFFFF5714)
private val HorizontalPad = 16.dp

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Streamer profile, opened by tapping the broadcaster avatar on the live feed.
 * Shown as an overlay so the stream's pager position is preserved on back.
 */
@Composable
fun StreamerProfileScreen(
    story: CampusLiveFeedStory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    activeProfile: com.campus.network.core.model.StudentProfile? = null,
    userServers: List<com.campus.network.core.model.Server> = emptyList(),
    userStreams: List<com.campus.network.core.model.LiveStream> = emptyList()
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableIntStateOf(1) }
    val tabs = listOf("Streams", "Statistic", "Servers")

    val isMe = story.isMe ||
        (activeProfile != null && story.broadcasterId.equals(activeProfile.stableUserId(), ignoreCase = true))

    val displayName = story.broadcasterName.takeIf(String::isRealProfileDisplayName).orEmpty()
    val displayAvatarUrl = if (isMe) activeProfile?.avatarUrl else story.broadcasterAvatarUrl
    val displayLocation = if (isMe && activeProfile != null && activeProfile.universityName.isNotBlank()) activeProfile.universityName else story.location.ifBlank { "Kenya" }
    val displayJoinedDate = if (isMe && activeProfile != null && activeProfile.academicYear.isNotBlank()) activeProfile.academicYear else story.joinedDate.ifBlank { "Oct 2026" }
    val displayBio = if (isMe) "Campus Verified Scholar" else if (story.description.isNotBlank()) story.description else "Campus Verified Scholar"
    val displayFollowers = if (isMe) "0" else story.followers
    val displayHoursLive = if (isMe) (userStreams.sumOf { it.durationSeconds } / 3600) else story.hoursLive
    val displayViewers = story.viewerCount

    val displayPastStreams = if (isMe) {
        userStreams.map { s ->
            PastStream(
                title = s.title,
                whenLabel = "Recorded",
                viewers = "${s.peakViewers} peak",
                duration = if (s.isLive) "Live" else "${(s.durationSeconds / 60).coerceAtLeast(1)} min"
            )
        }
    } else story.pastStreams

    val displayServers = if (isMe) {
        userServers.map { s ->
            StreamerServer(
                name = s.name,
                role = if (s.ownerId == activeProfile?.studentId) "Owner" else "Member",
                members = "${s.memberCount} ${if (s.memberCount == 1) "member" else "members"}",
                category = s.category,
                description = s.description,
                avatarRes = 0
            )
        }
    } else story.servers

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        // ===== Header: cover (232dp) + overlapping avatar (146dp @ top 142dp) =====
        Box(modifier = Modifier.fillMaxWidth().height(288.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
                    .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
                    .background(Color(0xFF121212))
            ) {
                Image(
                    painter = painterResource(story.backdropImageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = GrayscaleFilter,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
            }

            // Back button (replaces the dashboard action): 38dp glass circle
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = HorizontalPad, top = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Avatar: 146dp outer, 2.5dp silver ring, 8dp inset -> 130dp photo
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 142.dp)
                    .size(146.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0C0C))
                    .border(
                        2.5.dp,
                        Brush.verticalGradient(listOf(Color(0xFFD6D6D6), Color(0xFF4A4A4A))),
                        CircleShape
                    )
                    .padding(8.dp)
            ) {
                CampusStudentAvatar(
                    avatarUrl = displayAvatarUrl,
                    fullName = displayName,
                    fontSize = 32.sp,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }

        // ===== Name =====
        Text(
            text = displayName,
            color = Color.White,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )

        // ===== Location / joined chips (20dp tall, 26dp apart) =====
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(Icons.Default.LocationOn, Color(0xFF6B7CFF), displayLocation)
            Spacer(Modifier.width(26.dp))
            InfoChip(Icons.Default.AccessTime, Color(0xFFFFD84A), "Joined $displayJoinedDate")
        }

        // ===== Bio (12sp / 17sp, centered, ~300dp wide) =====
        Text(
            text = displayBio,
            color = BioText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp)
                .padding(horizontal = 46.dp)
        )

        // ===== Segmented tabs (44dp track, 38dp selected pill) =====
        Row(
            modifier = Modifier
                .padding(top = 21.dp)
                .padding(horizontal = HorizontalPad)
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(TabTrackBg)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(if (selected) 1.2f else 1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (selected) TabSelectedBg else Color.Transparent)
                        .clickable { selectedTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else Color(0xFFE6E6E6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> StreamsTab(displayPastStreams)
            2 -> ServersTab(displayServers)
            else -> {
            // ===== Ring stats (100dp rings, space-between) =====
            Row(
                modifier = Modifier
                    .padding(top = 23.dp)
                    .padding(horizontal = HorizontalPad)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                RingStat(label = "Hours Live", icon = Icons.Default.Videocam) {
                    TickDialRing(value = displayHoursLive.toString(), unit = "h")
                }
                RingStat(label = "Viewers", icon = Icons.Default.Visibility) {
                    WaveRing(value = displayViewers.toIntOrNull()?.let(::compactCount) ?: displayViewers)
                }
                RingStat(label = "Followers", icon = Icons.Default.Group) {
                    ArcRing(value = displayFollowers)
                }
            }

            // ===== Divider (1px) =====
            Box(
                modifier = Modifier
                    .padding(top = 25.dp)
                    .padding(horizontal = HorizontalPad)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF242424))
            )

            // ===== Trend =====
            Text(
                text = "Streaming trend for the last 8 weeks",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(horizontal = HorizontalPad)
            )

            Row(
                modifier = Modifier
                    .padding(top = 13.dp)
                    .padding(horizontal = HorizontalPad)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Widths measured 112 : 107 : 117
                TrendCard(label = "Top topics", weight = 112f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        story.topTopics.forEach {
                            Icon(it.icon(), null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                TrendCard(label = "Best day", weight = 107f) { TrendValue(story.bestDay) }
                TrendCard(label = "Per week", weight = 117f) { TrendValue(story.streamsPerWeek.toString()) }
            }

            // ===== Consistency heatmap: 8 weeks x 7 days =====
            val heat = story.weeklyActivity
            Column(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 24.dp)
                    .padding(horizontal = HorizontalPad)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(HeatCardBg)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                heat.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        week.forEach { level ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(19.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(heatColor(level))
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

private fun compactCount(n: Int): String =
    if (n >= 1000) "%.1fk".format(n / 1000f) else n.toString()

/** Heat levels: 0 = idle, 1 = light, 2 = active, 3 = peak. */
private fun heatColor(level: Int): Color = when (level) {
    0 -> Color(0xFF333333)
    1 -> Color(0xFF6A6A6A)
    2 -> Orange
    else -> DeepOrange
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, color = Color(0xFFD9D9D9), fontSize = 12.5.sp, maxLines = 1)
    }
}

@Composable
private fun RingStat(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ring: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ring()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF6F6F6F), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = MutedText, fontSize = 12.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TrendCard(
    label: String,
    weight: Float,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(weight)
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedText, fontSize = 11.sp, maxLines = 1)
        content()
    }
}

@Composable
private fun TrendValue(text: String) {
    Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
}

@Composable
private fun RingValue(value: String, unit: String? = null) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.White)) {
                append(value)
            }
            if (unit != null) {
                withStyle(SpanStyle(fontSize = 15.sp, color = MutedText)) { append(unit) }
            }
        },
        maxLines = 1
    )
}

/** Ring 1: 60-tick dial; the ticks leading into 12 o'clock fade to periwinkle. */
@Composable
private fun TickDialRing(value: String, unit: String) {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val ticks = 60
            val outer = size.minDimension / 2f
            val accent = Color(0xFF8C8CFF)
            for (i in 0 until ticks) {
                val frac = i / ticks.toFloat()
                val angle = 2.0 * PI * frac - PI / 2
                val len = if (i % 5 == 0) 8.dp.toPx() else 5.dp.toPx()
                val color = if (frac >= 0.8f) {
                    lerp(Color.White, accent, (frac - 0.8f) / 0.2f)
                } else {
                    Color.White.copy(alpha = 0.9f)
                }
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                drawLine(
                    color = color,
                    start = center + Offset(dx * (outer - len), dy * (outer - len)),
                    end = center + Offset(dx * outer, dy * outer),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        RingValue(value, unit)
    }
}

/** Ring 2: circle with a wavy orange fill rising from the bottom. */
@Composable
private fun WaveRing(value: String) {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val circle = Path().apply { addOval(Rect(center, r)) }
            clipPath(circle) {
                val baseY = size.height * 0.56f
                val amp = 5.dp.toPx()
                val wave = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, baseY)
                    var x = 0f
                    while (x <= size.width) {
                        lineTo(x, baseY + sin(x / size.width * 2f * PI.toFloat() * 1.5f) * amp)
                        x += 2f
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    wave,
                    Brush.verticalGradient(
                        listOf(Color(0xFFD9733A), Color(0xFF4F1D0A)),
                        startY = baseY,
                        endY = size.height
                    )
                )
            }
            drawCircle(
                brush = Brush.verticalGradient(listOf(Color(0xFF6E4A35), Color(0xFFE2762B))),
                radius = r - 1.dp.toPx(),
                style = Stroke(2.dp.toPx())
            )
        }
        Box(Modifier.offset(y = (-8).dp)) { RingValue(value) }
    }
}

/** Ring 3: dark track with a tan-to-orange arc sweeping over the top. */
@Composable
private fun ArcRing(value: String) {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(
                color = Color(0xFF1E1E1E),
                radius = size.minDimension / 2f - inset,
                style = Stroke(1.dp.toPx())
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0.45f to Color(0xFFB08A66),
                    0.75f to Color(0xFFD8895A),
                    1f to Color(0xFFE0541A),
                    center = center
                ),
                startAngle = 165f,
                sweepAngle = 195f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        RingValue(value)
    }
}

private fun StreamTopic.icon() = when (this) {
    StreamTopic.TECH -> Icons.Default.Code
    StreamTopic.ACADEMICS -> Icons.Default.School
    StreamTopic.MARKET -> Icons.Default.Storefront
    StreamTopic.POLITICS -> Icons.Default.HowToVote
    StreamTopic.MUSIC -> Icons.Default.MusicNote
}

/** Streams tab: the streamer's live broadcast and recent streams. */
@Composable
private fun StreamsTab(pastStreams: List<PastStream>) {
    Column(
        modifier = Modifier
            .padding(top = 25.dp, bottom = 24.dp)
            .padding(horizontal = HorizontalPad)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Streams", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (pastStreams.isEmpty()) {
            Text("No streams yet", color = MutedText, fontSize = 12.5.sp)
        }
        pastStreams.forEach { stream ->
            val live = stream.duration == "Live"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (live) DeepOrange else TabSelectedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stream.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text("${stream.whenLabel} \u2022 ${stream.viewers}", color = MutedText, fontSize = 11.sp, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
                Text(stream.duration, color = if (live) Orange else MutedText, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Servers tab: campus servers the streamer belongs to, with role and details. */
@Composable
private fun ServersTab(servers: List<StreamerServer>) {
    Column(
        modifier = Modifier
            .padding(top = 25.dp, bottom = 24.dp)
            .padding(horizontal = HorizontalPad)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Member of ${servers.size} ${if (servers.size == 1) "server" else "servers"}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (servers.isEmpty()) {
            Text("Not in any servers yet", color = MutedText, fontSize = 12.5.sp)
        }
        servers.forEach { server ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (server.avatarRes != 0) {
                        Image(
                            painter = painterResource(server.avatarRes),
                            contentDescription = server.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        CampusStudentAvatar(
                            avatarUrl = null,
                            fullName = server.name,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(server.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text("${server.members} \u2022 ${server.category}", color = MutedText, fontSize = 11.5.sp, maxLines = 1)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = server.role,
                        color = Color.Black,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Orange)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(server.description, color = BioText, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
