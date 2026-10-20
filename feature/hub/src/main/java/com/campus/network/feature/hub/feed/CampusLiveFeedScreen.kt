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
import com.campus.network.core.ui.components.CampusStudentAvatar
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    val broadcasterAvatarRes: Int = 0,
    val broadcasterAvatarUrl: String? = null,
    val viewerCount: String = "1",
    val isLive: Boolean = true,
    val title: String,
    val description: String,
    val backdropImageRes: Int = 0,
    val backdropImageUrl: String? = null,
    val initialComments: List<LiveStoryComment> = emptyList(),
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
    val servers: List<StreamerServer> = emptyList(),
    val isMe: Boolean = false
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
    val avatarRes: Int = 0
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
    val userAvatarRes: Int = 0,
    val commentText: String,
    val gifUrl: String? = null,
    val avatarUrl: String? = null
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
val defaultCampusLiveStories: List<CampusLiveFeedStory> = emptyList()

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
    studentProfileRepository: com.campus.network.core.data.StudentProfileRepository? = null,
    liveStreamRepository: com.campus.network.core.data.LiveStreamRepository? = null,
    serverRepository: com.campus.network.core.data.ServerRepository? = null,
    onNavigateBack: () -> Unit
) {
    val activeProfile by (studentProfileRepository?.studentProfile?.collectAsState(initial = null) ?: remember { mutableStateOf<com.campus.network.core.model.StudentProfile?>(null) })
    val realActiveStreams by (liveStreamRepository?.getActiveStreams()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val realAllStreams by (liveStreamRepository?.getAllStreams()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val realServers by (serverRepository?.getServers()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    val combinedStories = remember(stories, realActiveStreams, activeProfile) {
        val streamStories = realActiveStreams.map { s ->
            val isMyStream = s.broadcasterId == (activeProfile?.studentId ?: "") ||
                    (activeProfile != null && activeProfile?.fullName?.isNotBlank() == true && s.broadcasterName == activeProfile?.fullName)
            CampusLiveFeedStory(
                id = s.id,
                broadcasterName = s.broadcasterName,
                broadcasterHandle = s.broadcasterHandle,
                broadcasterAvatarUrl = s.broadcasterAvatarUrl ?: (if (isMyStream) activeProfile?.avatarUrl else null),
                viewerCount = s.viewerCount.coerceAtLeast(1).toString(),
                isLive = s.isLive,
                title = s.title,
                description = s.description,
                backdropImageRes = if (s.backdropImageRes != 0) s.backdropImageRes else R.drawable.card_student_founder,
                backdropImageUrl = s.backdropImageUrl,
                initialComments = emptyList(),
                location = if (isMyStream) (activeProfile?.universityName?.ifBlank { "Kenya" } ?: "Kenya") else "Kenya",
                isMe = isMyStream
            )
        }
        val remainingDefaults = stories.filter { def -> realActiveStreams.none { it.id == def.id } }
        streamStories + remainingDefaults
    }

    val initialIndex = if (combinedStories.isEmpty()) 0 else combinedStories.indexOfFirst { it.id.equals(initialStoryId, ignoreCase = true) }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { combinedStories.size }
    )
    var profileStory by remember { mutableStateOf<CampusLiveFeedStory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (combinedStories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .statusBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Text(
                        text = "No Live Broadcasts Right Now",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "No students are currently broadcasting live on campus. Check back later or go live from the Hub!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Hub", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val story = combinedStories[pageIndex]
                SingleLiveFeedPage(
                    story = story,
                    activeProfile = activeProfile,
                    liveStreamRepository = liveStreamRepository,
                    onClose = onNavigateBack,
                    onBroadcasterClick = { profileStory = story }
                )
            }
        }

        profileStory?.let { story ->
            StreamerProfileScreen(
                story = story,
                activeProfile = activeProfile,
                userServers = realServers,
                userStreams = realAllStreams,
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
    activeProfile: com.campus.network.core.model.StudentProfile? = null,
    liveStreamRepository: com.campus.network.core.data.LiveStreamRepository? = null,
    onClose: () -> Unit,
    onBroadcasterClick: () -> Unit
) {
    val comments = remember(story.id) { mutableStateListOf<LiveStoryComment>().apply { addAll(story.initialComments) } }
    var commentText by remember { mutableStateOf("") }
    var likeCount by remember { mutableIntStateOf(0) }
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

    if (liveStreamRepository != null) {
        LaunchedEffect(story.id) {
            liveStreamRepository.getCommentsForStream(story.id).collect { list ->
                list.forEach { c ->
                    if (comments.none { it.id == c.id }) {
                        comments.add(
                            LiveStoryComment(
                                id = c.id,
                                userName = c.senderName,
                                userAvatarRes = 0,
                                commentText = c.commentText,
                                avatarUrl = c.senderAvatarUrl
                            )
                        )
                    }
                }
            }
        }
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
        if (liveStreamRepository != null) {
            coroutineScope.launch {
                liveStreamRepository.incrementLikes(story.id)
            }
        }
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
            val authorName = activeProfile?.fullName?.ifBlank { "You" } ?: "You"
            val authorAvatar = activeProfile?.avatarUrl
            val trimmed = commentText.trim()
            val commentId = "user_${System.currentTimeMillis()}"
            comments.add(
                LiveStoryComment(
                    id = commentId,
                    userName = authorName,
                    userAvatarRes = 0,
                    commentText = trimmed,
                    avatarUrl = authorAvatar
                )
            )
            if (liveStreamRepository != null) {
                coroutineScope.launch {
                    liveStreamRepository.postComment(
                        streamId = story.id,
                        senderId = activeProfile?.studentId ?: "user_self",
                        senderName = authorName,
                        senderAvatarRes = 0,
                        commentText = trimmed,
                        senderAvatarUrl = authorAvatar
                    )
                }
            }
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
                    CampusStudentAvatar(
                        avatarUrl = story.broadcasterAvatarUrl,
                        fullName = story.broadcasterName,
                        fontSize = 14.sp,
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
                            val authorName = activeProfile?.fullName?.ifBlank { "You" } ?: "You"
                            val authorAvatar = activeProfile?.avatarUrl
                            comments.add(
                                LiveStoryComment(
                                    id = "user_${System.currentTimeMillis()}",
                                    userName = authorName,
                                    userAvatarRes = 0,
                                    commentText = "",
                                    gifUrl = gif.originalUrl,
                                    avatarUrl = authorAvatar
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
        CampusStudentAvatar(
            avatarUrl = comment.avatarUrl,
            fullName = comment.userName,
            fontSize = 10.sp,
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
