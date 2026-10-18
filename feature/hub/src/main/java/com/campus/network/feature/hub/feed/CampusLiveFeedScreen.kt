package com.campus.network.feature.hub.feed

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.core.ui.components.HybridEmojiReactionDrawer
import com.campus.network.core.ui.components.StreamCogLoaderOverlay
import com.campus.network.core.ui.components.rememberCampusNetworkState
import com.campus.network.feature.hub.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Data Model for a Campus Live Stream / Interactive Vertical Story.
 */
data class CampusLiveFeedStory(
    val id: String,
    val broadcasterName: String,
    val broadcasterHandle: String,
    val broadcasterAvatarRes: Int,
    val viewerCount: String,
    val isLive: Boolean = true,
    val title: String,
    val description: String,
    val backdropImageRes: Int,
    val initialComments: List<LiveStoryComment>,
    // Streamer profile details
    val location: String = "Nairobi, KE",
    val joinedDate: String = "",
    val hoursLive: Int = 0,
    val followers: String = "0",
    val topTopics: List<StreamTopic> = emptyList(),
    val bestDay: String = "-",
    val streamsPerWeek: Int = 0,
    /** 8 weeks x 7 days of streaming activity, levels 0..3 (56 values). */
    val weeklyActivity: List<Int> = List(56) { 0 },
    val pastStreams: List<PastStream> = emptyList(),
    val servers: List<StreamerServer> = emptyList()
)

enum class StreamTopic { TECH, ACADEMICS, MARKET, POLITICS, MUSIC }

data class PastStream(
    val title: String,
    val whenLabel: String,
    val viewers: String,
    val duration: String
)

data class StreamerServer(
    val name: String,
    val role: String,
    val members: String,
    val category: String,
    val description: String,
    val avatarRes: Int
)

/** Deterministic 8-week activity grid (levels 0..3) for a streamer. */
private fun activityGrid(seed: Int, busy: Float): List<Int> {
    val rnd = kotlin.random.Random(seed)
    return List(56) {
        val r = rnd.nextFloat()
        when {
            r > 1f - busy * 0.25f -> 3
            r > 1f - busy * 0.6f -> 2
            r > 1f - busy -> 1
            else -> 0
        }
    }
}

data class LiveStoryComment(
    val id: String,
    val userName: String,
    val userAvatarRes: Int,
    val commentText: String,
    val gifUrl: String? = null
)

/**
 * Floating Heart Particle Model.
 */
private data class HeartParticle(
    val id: Long,
    val startX: Float,
    val scale: Float,
    val color: Color
)

/**
 * Curated Campus Feeds & Livestreams (Starting with Student Founders).
 */
val defaultCampusLiveStories = listOf(
    // 1. Celebrating Student Founders (First Item)
    CampusLiveFeedStory(
        id = "founders",
        broadcasterName = "Sara.HM",
        broadcasterHandle = "sara.hm",
        broadcasterAvatarRes = com.campus.network.core.ui.R.drawable.avatar_eliza,
        viewerCount = "1250",
        isLive = true,
        title = "Celebrating Student Founders Demo Day",
        description = "Live pitch: Building peer solar charging stations across student hostels. Ask questions in comments!",
        backdropImageRes = R.drawable.card_student_founder,
        location = "Strathmore University, Nairobi",
        joinedDate = "12th Jan 2025",
        hoursLive = 76,
        followers = "4,057",
        topTopics = listOf(StreamTopic.TECH, StreamTopic.ACADEMICS, StreamTopic.MARKET),
        bestDay = "Friday",
        streamsPerWeek = 6,
        weeklyActivity = activityGrid(11, 0.7f),
        pastStreams = listOf(
            PastStream("Founders Demo Day: Solar Charging Pitch", "Live now", "1.2k watching", "Live"),
            PastStream("How we validated hostel solar demand", "3 days ago", "2.1k views", "48 min"),
            PastStream("Pitch deck teardown with mentors", "1 week ago", "1.6k views", "1 h 05 min"),
            PastStream("Campus startup funding Q&A", "2 weeks ago", "980 views", "36 min")
        ),
        servers = listOf(
            StreamerServer("Strathmore Founders Guild", "Owner", "2,140 members", "Entrepreneurship", "Student founders sharing pitches, co-founder searches and demo-day prep.", com.campus.network.core.ui.R.drawable.avatar_eliza),
            StreamerServer("Solar Hostels Collective", "Admin", "380 members", "Sustainability", "Planning peer-run solar charging stations across student hostels.", com.campus.network.core.ui.R.drawable.server_avatar2)
        ),
        initialComments = listOf(
            LiveStoryComment("c1", "Davidsom.198041", com.campus.network.core.ui.R.drawable.avatar_andrew, "You have a gift that fills the air with joy and harmony. Keep sharing your incredible talent!"),
            LiveStoryComment("c2", "Samansky", com.campus.network.core.ui.R.drawable.avatar_clin, "Can u sing a track of Adel?"),
            LiveStoryComment("c3", "Hossein.nd", com.campus.network.core.ui.R.drawable.student_june, "Amazing! singing our hart out"),
            LiveStoryComment("c4", "Sadaf_Sk", com.campus.network.core.ui.R.drawable.avatar_me, "Very nice voice"),
            LiveStoryComment("c5", "Brian_Tech", com.campus.network.core.ui.R.drawable.student_andrew, "Where can we get the demo slides?")
        )
    ),
    // 2. Guild Presidential Debate 2026
    CampusLiveFeedStory(
        id = "guild_debate",
        broadcasterName = "UoN.Guild",
        broadcasterHandle = "uon.guild",
        broadcasterAvatarRes = com.campus.network.core.ui.R.drawable.avatar_junebog,
        viewerCount = "1840",
        isLive = true,
        title = "Guild Presidential Debate 2026",
        description = "Live from Taifa Hall Auditorium. Real-time candidate cross-examinations and student policy polling.",
        backdropImageRes = R.drawable.hero_campus_scenic,
        location = "University of Nairobi",
        joinedDate = "3rd Sep 2024",
        hoursLive = 120,
        followers = "12.8k",
        topTopics = listOf(StreamTopic.POLITICS, StreamTopic.ACADEMICS),
        bestDay = "Wednesday",
        streamsPerWeek = 4,
        weeklyActivity = activityGrid(22, 0.55f),
        pastStreams = listOf(
            PastStream("Guild Presidential Debate 2026", "Live now", "1.8k watching", "Live"),
            PastStream("Candidate Q&A: Hostels & Fees", "4 days ago", "5.4k views", "1 h 20 min"),
            PastStream("Guild budget town hall", "2 weeks ago", "3.2k views", "55 min")
        ),
        servers = listOf(
            StreamerServer("UoN Student Guild", "Official", "9,600 members", "Student Government", "Official announcements, polls and election coverage for UoN students.", com.campus.network.core.ui.R.drawable.avatar_junebog),
            StreamerServer("Taifa Hall Debates", "Moderator", "1,250 members", "Debate", "Weekly debate nights and policy discussions.", com.campus.network.core.ui.R.drawable.server_avatar2)
        ),
        initialComments = listOf(
            LiveStoryComment("g1", "Kevin.Dev", com.campus.network.core.ui.R.drawable.student_clin, "What about 24-hour library power backup?"),
            LiveStoryComment("g2", "Amina_M", com.campus.network.core.ui.R.drawable.avatar_eliza, "Taifa hall is completely full! Audio is crystal clear"),
            LiveStoryComment("g3", "Cherono_Law", com.campus.network.core.ui.R.drawable.avatar_ath, "The rebuttal on campus health facilities was sharp"),
            LiveStoryComment("g4", "Waweru_CS", com.campus.network.core.ui.R.drawable.student_rk, "Voting opens midnight on the guild portal")
        )
    ),
    // 3. GDSC AI Hackathon: Final Pitches
    CampusLiveFeedStory(
        id = "gdsc_hackathon",
        broadcasterName = "GDSC.Build",
        broadcasterHandle = "gdsc.build",
        broadcasterAvatarRes = com.campus.network.core.ui.R.drawable.avatar_ath,
        viewerCount = "890",
        isLive = true,
        title = "GDSC AI Hackathon: Final Pitches",
        description = "Top 6 finalist student teams showcase live on-device Android AI models. Voting is now live!",
        backdropImageRes = R.drawable.event_hero_trees,
        location = "Strathmore University, Nairobi",
        joinedDate = "8th Feb 2025",
        hoursLive = 58,
        followers = "3,920",
        topTopics = listOf(StreamTopic.TECH, StreamTopic.ACADEMICS),
        bestDay = "Saturday",
        streamsPerWeek = 3,
        weeklyActivity = activityGrid(33, 0.5f),
        pastStreams = listOf(
            PastStream("GDSC AI Hackathon: Final Pitches", "Live now", "890 watching", "Live"),
            PastStream("Intro to on-device Android AI", "5 days ago", "2.4k views", "1 h 10 min"),
            PastStream("Compose workshop: building a feed", "3 weeks ago", "1.9k views", "50 min")
        ),
        servers = listOf(
            StreamerServer("GDSC Strathmore", "Lead", "1,780 members", "Technology", "Workshops, hackathons and study jams for student developers.", com.campus.network.core.ui.R.drawable.avatar_ath),
            StreamerServer("Android Builders", "Admin", "640 members", "Mobile Dev", "Kotlin and Compose help, code reviews and project showcases.", com.campus.network.core.ui.R.drawable.server_avatar2)
        ),
        initialComments = listOf(
            LiveStoryComment("h1", "Techie_99", com.campus.network.core.ui.R.drawable.student_rk, "That on-device LLM latency is incredible!"),
            LiveStoryComment("h2", "Wanjiku.Codes", com.campus.network.core.ui.R.drawable.avatar_me, "Team Strathmore has my vote!"),
            LiveStoryComment("h3", "Alex.AI", com.campus.network.core.ui.R.drawable.avatar_andrew, "Repo link pinned in discord channel"),
            LiveStoryComment("h4", "Mutiso_Eng", com.campus.network.core.ui.R.drawable.student_clin, "How is the local cache indexed?")
        )
    ),
    // 4. SafeTrade Quad Rendezvous & Flea Market
    CampusLiveFeedStory(
        id = "safetrade_quad",
        broadcasterName = "SafeTrade.Hub",
        broadcasterHandle = "safetrade.hub",
        broadcasterAvatarRes = com.campus.network.core.ui.R.drawable.avatar_clin,
        viewerCount = "620",
        isLive = true,
        title = "SafeTrade Quad Meetup & Market",
        description = "Streaming live from the Student Center Quad. Free item checkups, textbook drop-offs, and safe payments.",
        backdropImageRes = R.drawable.card_campus_landmark,
        location = "Student Center Quad, UoN",
        joinedDate = "21st Mar 2025",
        hoursLive = 92,
        followers = "2,310",
        topTopics = listOf(StreamTopic.MARKET, StreamTopic.ACADEMICS),
        bestDay = "Monday",
        streamsPerWeek = 5,
        weeklyActivity = activityGrid(44, 0.65f),
        pastStreams = listOf(
            PastStream("SafeTrade Quad Meetup & Market", "Live now", "620 watching", "Live"),
            PastStream("Textbook swap day highlights", "2 days ago", "1.3k views", "40 min"),
            PastStream("How SafePay protects your money", "1 week ago", "2.0k views", "25 min")
        ),
        servers = listOf(
            StreamerServer("SafeTrade Campus Market", "Owner", "3,050 members", "Marketplace", "Verified student buy/sell listings with protected payments.", com.campus.network.core.ui.R.drawable.avatar_clin),
            StreamerServer("Textbook Exchange", "Admin", "1,120 members", "Study Resources", "Swap and donate textbooks, calculators and class notes.", com.campus.network.core.ui.R.drawable.server_avatar2)
        ),
        initialComments = listOf(
            LiveStoryComment("s1", "Chris_Mech", com.campus.network.core.ui.R.drawable.student_andrew, "Are there still scientific calculators available?"),
            LiveStoryComment("s2", "Grace_N", com.campus.network.core.ui.R.drawable.avatar_eliza, "Just traded my engineering mechanics book in 5 mins!"),
            LiveStoryComment("s3", "Dan_Finance", com.campus.network.core.ui.R.drawable.avatar_junebog, "SafePay released payment immediately on handoff"),
            LiveStoryComment("s4", "Esther_B", com.campus.network.core.ui.R.drawable.student_june, "Canopy open until 6:00 PM today")
        )
    )
)

/**
 * TikTok / Instagram Live Fullscreen Campus Feed Screen.
 * Implements vertical swipe navigation between broadcasts, real-time comment stream,
 * glassy bottom dock, floating heart particles, and high-end broadcast overlays.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CampusLiveFeedScreen(
    initialStoryId: String = "founders",
    stories: List<CampusLiveFeedStory> = defaultCampusLiveStories,
    onNavigateBack: () -> Unit
) {
    val initialIndex = stories.indexOfFirst { it.id.equals(initialStoryId, ignoreCase = true) }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { stories.size }
    )
    var profileStory by remember { mutableStateOf<CampusLiveFeedStory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val story = stories[pageIndex]
            SingleLiveFeedPage(
                story = story,
                onClose = onNavigateBack,
                onBroadcasterClick = { profileStory = story }
            )
        }

        profileStory?.let { story ->
            StreamerProfileScreen(
                story = story,
                onBack = { profileStory = null }
            )
        }
    }
}

/**
 * Single Fullscreen Live Stream / Story View matching referenced UI.
 */
@Composable
private fun SingleLiveFeedPage(
    story: CampusLiveFeedStory,
    onClose: () -> Unit,
    onBroadcasterClick: () -> Unit
) {
    val comments = remember(story.id) { mutableStateListOf<LiveStoryComment>().apply { addAll(story.initialComments) } }
    var commentText by remember { mutableStateOf("") }
    var likeCount by remember { mutableIntStateOf(342) }
    var isLiked by remember { mutableStateOf(false) }
    var isEmojiDrawerOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val commentListState = rememberLazyListState()
    val interactionSource = remember { MutableInteractionSource() }

    BackHandler(enabled = isEmojiDrawerOpen) {
        isEmojiDrawerOpen = false
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is FocusInteraction.Focus) {
                if (isEmojiDrawerOpen) {
                    isEmojiDrawerOpen = false
                }
            }
        }
    }

    // Floating heart particles
    val heartParticles = remember { mutableStateListOf<HeartParticle>() }

    fun spawnHeart() {
        likeCount++
        isLiked = true
        val randomX = Random.nextFloat() * 80f - 40f
        val randomScale = 0.8f + Random.nextFloat() * 0.5f
        val heartColors = listOf(
            Color(0xFFFF512F),
            Color(0xFFDD2476),
            Color.White.copy(alpha = 0.9f),
            Color(0xFFFF8DA1)
        )
        val particle = HeartParticle(
            id = System.currentTimeMillis() + Random.nextLong(1000),
            startX = randomX,
            scale = randomScale,
            color = heartColors.random()
        )
        heartParticles.add(particle)
    }

    fun submitComment() {
        if (commentText.isNotBlank()) {
            comments.add(
                LiveStoryComment(
                    id = "user_${System.currentTimeMillis()}",
                    userName = "You",
                    userAvatarRes = com.campus.network.core.ui.R.drawable.avatar_me,
                    commentText = commentText.trim()
                )
            )
            commentText = ""
            focusManager.clearFocus()
            keyboardController?.hide()
            isEmojiDrawerOpen = false
            coroutineScope.launch {
                delay(100)
                commentListState.animateScrollToItem(comments.size - 1)
            }
        }
    }

    val networkState = rememberCampusNetworkState()
    var isStreamOptimizing by remember(story.id) { mutableStateOf(true) }

    LaunchedEffect(story.id, networkState.isOnline, networkState.isSlowNetwork) {
        isStreamOptimizing = true
        val bufferMs = if (networkState.isSlowNetwork) 1450L else 950L
        delay(bufferMs)
        if (networkState.isOnline) {
            isStreamOptimizing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { spawnHeart() },
                    onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isEmojiDrawerOpen = false
                    }
                )
            }
    ) {
        // 1. Fullscreen Photographic / Stream Backdrop
        Image(
            painter = painterResource(story.backdropImageRes),
            contentDescription = story.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. High-End Editorial Vignette Gradients (Top & Bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.70f),
                            0.20f to Color.Black.copy(alpha = 0.25f),
                            0.45f to Color.Transparent,
                            0.70f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // 2b. Cog Animated Loader Overlay ("Optimizing video playback...") matching Paramount+ reference
        AnimatedVisibility(
            visible = isStreamOptimizing || !networkState.isOnline,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(350)),
            modifier = Modifier.fillMaxSize()
        ) {
            StreamCogLoaderOverlay(
                message = if (!networkState.isOnline) {
                    "Waiting for network to resume stream..."
                } else {
                    "Optimizing video playback..."
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Floating Heart Particles Engine (Drifting up along the right edge)
        heartParticles.forEach { particle ->
            FloatingHeart(
                particle = particle,
                onFinished = { heartParticles.remove(particle) }
            )
        }

        val bottomInsetsModifier = if (isEmojiDrawerOpen) {
            Modifier.windowInsetsPadding(WindowInsets.ime)
        } else {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        }

        // 4. Screen Layout Stack
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ==========================================
            // TOP BAR: Broadcaster Info & Live Badges (Matching Reference)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Broadcaster Avatar & Username (tap opens streamer profile)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(onClick = onBroadcasterClick)
                ) {
                    Image(
                        painter = painterResource(story.broadcasterAvatarRes),
                        contentDescription = story.broadcasterName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = story.broadcasterName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "@${story.broadcasterHandle}",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                    }
                }

                // Top Right: Viewers Pill, Gradient LIVE Pill & Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Viewer Count Pill (Translucent dark glass)
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = story.viewerCount,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Bright Gradient LIVE Pill (Matching reference image)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF512F), Color(0xFFDD2476))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ==========================================
            // BOTTOM SECTION: Live Chat Overlay & Action Dock
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(bottomInsetsModifier)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                // Broadcast Title / Caption Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = story.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = story.description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Live Comments Stream (Bottom Left matching reference image)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(min = 60.dp, max = 150.dp)
                ) {
                    LazyColumn(
                        state = commentListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            LiveCommentRow(comment = comment)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Bottom Dock: Glassmorphic Input + Send + Like
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Glassmorphic Input Field with integrated Emoji & GIF access
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emoji / GIF Trigger Icon Button (matching Chat Screen behaviour)
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = "Emojis & GIFs",
                                tint = if (isEmojiDrawerOpen) Color(0xFFFF512F) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(23.dp)
                                    .clickable {
                                        if (!isEmojiDrawerOpen) {
                                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                            imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                            keyboardController?.hide()
                                            isEmojiDrawerOpen = true
                                        } else {
                                            isEmojiDrawerOpen = false
                                        }
                                    }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = commentText,
                                onValueChange = {
                                    commentText = it
                                    if (isEmojiDrawerOpen) {
                                        isEmojiDrawerOpen = false
                                    }
                                },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color.White),
                                interactionSource = interactionSource,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = { submitComment() }
                                ),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (commentText.isEmpty()) {
                                        Text(
                                            text = "Add a Comment ...",
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Send Action (Paper Plane icon)
                    IconButton(
                        onClick = { submitComment() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Comment",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Like / Heart Button (Spawns floating hearts)
                    IconButton(
                        onClick = { spawnHeart() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLiked) Color(0xFFDD2476).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.20f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF512F) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 5. Hybrid Emoji & GIF Drawer (Docks cleanly at bottom)
            // ==========================================
            AnimatedVisibility(
                visible = isEmojiDrawerOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    HybridEmojiReactionDrawer(
                        onEmojiPicked = { emoji ->
                            commentText += emoji
                        },
                        onGifPicked = { gif ->
                            comments.add(
                                LiveStoryComment(
                                    id = "user_${System.currentTimeMillis()}",
                                    userName = "You",
                                    userAvatarRes = com.campus.network.core.ui.R.drawable.avatar_me,
                                    commentText = "",
                                    gifUrl = gif.originalUrl
                                )
                            )
                            isEmojiDrawerOpen = false
                            coroutineScope.launch {
                                delay(100)
                                commentListState.animateScrollToItem(comments.size - 1)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Single Comment Item matching the reference image:
 * Small circular avatar + Username in white bold + Comment text in white.
 */
@Composable
private fun LiveCommentRow(comment: LiveStoryComment) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Image(
            painter = painterResource(comment.userAvatarRes),
            contentDescription = comment.userName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.userName,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            if (comment.commentText.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = comment.commentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 16.sp
                )
            }
            if (!comment.gifUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                AsyncImage(
                    model = comment.gifUrl,
                    contentDescription = "GIF Comment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

/**
 * Floating Heart Particle Animation:
 * Rises vertically along the bottom right and gently fades out.
 */
@Composable
private fun FloatingHeart(
    particle: HeartParticle,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(particle.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
        onFinished()
    }

    val currentProgress = progress.value
    val yOffset = (-340 * currentProgress).dp
    val xWobble = (kotlin.math.sin(currentProgress * 6.28f * 1.5f) * 18f).dp
    val alpha = (1f - currentProgress).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 70.dp, end = 26.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = particle.color.copy(alpha = alpha),
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (particle.startX + xWobble.value).dp.roundToPx(),
                        y = yOffset.roundToPx()
                    )
                }
                .size((26 * particle.scale).dp)
        )
    }
}
