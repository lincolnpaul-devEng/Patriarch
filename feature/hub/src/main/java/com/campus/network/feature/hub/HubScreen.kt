package com.campus.network.feature.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import kotlinx.coroutines.launch

// Exact visual styling from the reference screen
private val BrandBluePrimary = Color(0xFF014784)
private val BrandBlueDeep = Color(0xFF0A335C)
private val HeroLocationDark = Color(0xB812151B)
private val AIBarBackground = Color(0xF2FFFFFF)
private val DarkCardTextPrimary = Color(0xFF0F172A)

@Composable
fun HubScreen(
    viewModel: HubViewModel,
    onNavigateToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isAiDialogOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isPostDialogOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FB))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // -------------------------------------------------------------
            // 1. Scenic Hero Section with Transparent Logo & Action Buttons
            // -------------------------------------------------------------
            item {
                HeroScenicHeader(
                    onLogoClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Campus Network: Premium Student Session Active")
                        }
                    },
                    onChatClick = {
                        onNavigateToChat("general")
                    },
                    onNotificationClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Notifications: 2 new campus updates available")
                        }
                    },
                    onLocationClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Current Campus Sector: Cancun / Global Study Hub")
                        }
                    },
                    onAiBarClick = {
                        isAiDialogOpen = true
                    }
                )
            }

            // -------------------------------------------------------------
            // 2. Member Status / Loyalty Card (Deep Blue Gradient)
            // -------------------------------------------------------------
            item {
                MemberLoyaltyCard(
                    memberName = "Ben",
                    memberIdMasked = "•••• 4829",
                    milesCount = "0",
                    loyaltyPoints = "0",
                    onCardClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("AAdvantage® Tier: Campus Verified Explorer")
                        }
                    }
                )
            }

            // -------------------------------------------------------------
            // 3. Featured Stories Carousel (Horizontal Cards)
            // -------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(18.dp))
                FeaturedStoriesCarousel(
                    onStoryClick = { title ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Selected: $title")
                        }
                    }
                )
            }

            // -------------------------------------------------------------
            // 4. Campus Micro-Commerce & Gigs Deck (Integrated Functionality)
            // -------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(24.dp))
                CampusMarketHeader(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    onPostNewClick = { isPostDialogOpen = true }
                )
            }

            // Real-time marketplace items and gigs
            val filteredItems = if (selectedCategory == "All") {
                uiState.marketplaceItems
            } else {
                uiState.marketplaceItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }

            if (filteredItems.isEmpty() && uiState.microGigs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active listings in this category. Be the first to post!",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                items(filteredItems) { item ->
                    MarketItemCard(
                        item = item,
                        onAdd = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Added ${item.title} to inquiry queue")
                            }
                        }
                    )
                }

                items(uiState.microGigs) { gig ->
                    MicroGigCard(
                        gig = gig,
                        onAccept = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Applied for gig: ${gig.title}")
                            }
                        }
                    )
                }
            }
        }

        // Floating Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )

        // AI Travel / Campus Inspiration Dialog
        if (isAiDialogOpen) {
            AiDiscoveryDialog(
                onDismiss = { isAiDialogOpen = false },
                onSearch = { query ->
                    isAiDialogOpen = false
                    scope.launch {
                        snackbarHostState.showSnackbar("AI Query: \"$query\" - Curating campus experiences...")
                    }
                }
            )
        }

        // Post New Item / Gig Dialog
        if (isPostDialogOpen) {
            PostItemDialog(
                onDismiss = { isPostDialogOpen = false },
                onPost = { title, price, category ->
                    viewModel.onEvent(
                        HubEvent.PostItem(
                            title = title,
                            price = price,
                            category = category,
                            condition = "Like New",
                            location = "Main Campus Quad"
                        )
                    )
                    isPostDialogOpen = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Posted $title to Campus Marketplace")
                    }
                }
            )
        }
    }
}

/**
 * Scenic Hero Header Section:
 * Features full-bleed scenic photograph, distant flying birds Canvas layer,
 * the transparent logo replacing the "100" logo, circular glass action buttons,
 * the floating location pill ("Nairobi, Kenya"), and the AI discovery capsule.
 */
@Composable
private fun HeroScenicHeader(
    onLogoClick: () -> Unit,
    onChatClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onLocationClick: () -> Unit,
    onAiBarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
    ) {
        // 1. Scenic Photography Backdrop
        Image(
            painter = painterResource(R.drawable.hero_campus_scenic),
            contentDescription = "Scenic Campus Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Distant Avian Silhouettes & Atmospheric Vignette (Custom Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top status vignette for high-contrast visibility
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.38f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.32f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Avian flight curves in upper center sky (matching reference)
            val birdColor = Color(0x66202428)
            val birdStroke = Stroke(width = 2.2f)

            // Bird 1
            val path1 = Path().apply {
                moveTo(w * 0.41f, h * 0.14f)
                quadraticBezierTo(w * 0.425f, h * 0.128f, w * 0.44f, h * 0.138f)
                quadraticBezierTo(w * 0.455f, h * 0.128f, w * 0.47f, h * 0.14f)
            }
            drawPath(path = path1, color = birdColor, style = birdStroke)

            // Bird 2 (smaller, slightly offset)
            val path2 = Path().apply {
                moveTo(w * 0.485f, h * 0.155f)
                quadraticBezierTo(w * 0.495f, h * 0.145f, w * 0.505f, h * 0.152f)
                quadraticBezierTo(w * 0.515f, h * 0.145f, w * 0.525f, h * 0.155f)
            }
            drawPath(path = path2, color = birdColor, style = birdStroke)
        }

        // 3. Top Header Bar: App Logo & Circular Frosted Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Logo with transparent background (replacing 100 logo)
            Image(
                painter = painterResource(R.drawable.ic_logo_transparent),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onLogoClick() }
            )

            // Right Action Buttons (Chat & Notifications)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Frosted Chat Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .shadow(3.dp, CircleShape)
                        .clickable { onChatClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Messages",
                        tint = Color(0xFF1E2229),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Circular Frosted Notifications Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .shadow(3.dp, CircleShape)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = Color(0xFF1E2229),
                        modifier = Modifier.size(23.dp)
                    )

                    // Small indicator dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = 10.dp)
                            .background(Color(0xFFEA4335), CircleShape)
                    )
                }
            }
        }

        // 4. Floating Location Chip (Right-aligned over scenic view)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HeroLocationDark)
                .clickable { onLocationClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Nairobi, Kenya",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 5. Floating AI Inspiration Capsule (Bottom of Hero Scene)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(28.dp),
            color = AIBarBackground,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAiBarClick() }
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Four-point AI Star Icon
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Sparkle",
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Travel inspiration with AIˢᵐ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkCardTextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open AI",
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Member Status / Loyalty Card:
 * Deep blue gradient card displaying member identity, masked member ID,
 * AAdvantage® miles, and loyalty points with zero rectangular layout artifacts.
 */
@Composable
private fun MemberLoyaltyCard(
    memberName: String,
    memberIdMasked: String,
    milesCount: String,
    loyaltyPoints: String,
    onCardClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandBluePrimary, BrandBlueDeep)
                    )
                )
                .clickable { onCardClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                // Top Member Identity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Aerodynamic Wing Emblem (Canvas drawing)
                    AerodynamicWingEmblem(modifier = Modifier.size(36.dp))

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = memberName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AAdvantage® member #",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = memberIdMasked.ifBlank { "•••• 4829" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Membership",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Soft Ambient Card Separation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Loyalty & Miles Stats
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Miles Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = "Globe",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AAdvantage® miles: ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = milesCount,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Loyalty Points Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Stars,
                            contentDescription = "Loyalty Stars",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Loyalty Points: ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = loyaltyPoints,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aerodynamic Wing Emblem:
 * Precision canvas vector representing the modern brand flight wing in soft sky blue.
 */
@Composable
private fun AerodynamicWingEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val wingColor = Color(0xFF6BB9F8)
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.45f)
            lineTo(w * 0.72f, h * 0.85f)
            lineTo(w * 0.42f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.75f)
            close()
        }
        drawPath(path = path, color = wingColor)

        // Accent streak
        val streak = Path().apply {
            moveTo(w * 0.2f, h * 0.45f)
            lineTo(w * 0.7f, h * 0.6f)
            lineTo(w * 0.55f, h * 0.8f)
            close()
        }
        drawPath(path = streak, color = Color.White.copy(alpha = 0.35f))
    }
}

/**
 * Featured Stories Carousel:
 * Large visual portrait cards side-by-side with dark gradient text overlays.
 */
@Composable
private fun FeaturedStoriesCarousel(onStoryClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Story Card 1: Celebrating Small Businesses / Student Founders
        item {
            StoryCard(
                imageRes = R.drawable.card_student_founder,
                title = "Celebrating small businesses",
                subtitle = "Earn 10,000 AAdvantage® bonus miles for your company. Register for free today",
                onClick = { onStoryClick("Celebrating small businesses") }
            )
        }

        // Story Card 2: International Journey & Study Abroad
        item {
            StoryCard(
                imageRes = R.drawable.card_campus_landmark,
                title = "Start your next journey",
                subtitle = "Earn 15,000 bonus miles with partner programs worldwide. Required enrollment open now.",
                onClick = { onStoryClick("Start your next journey") }
            )
        }

        // Story Card 3: Campus Innovation & Peer Gigs
        item {
            StoryCardVector(
                title = "Campus Venture Deck",
                subtitle = "Connect with student founders, equipment labs, and high-impact campus projects.",
                onClick = { onStoryClick("Campus Venture Deck") }
            )
        }
    }
}

/**
 * Story Card with Photographic Backdrop & Vignette Overlay.
 */
@Composable
private fun StoryCard(
    imageRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(295.dp)
            .height(360.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        ) {
            // Photo Background
            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Dark Vignette for Text Legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.22f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Story Content at Top
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Vector-Styled Story Card for Campus Features.
 */
@Composable
private fun StoryCardVector(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(295.dp)
            .height(360.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        color = Color(0xFF16222F)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF16222F), Color(0xFF0F1722))
                    )
                )
                .clickable { onClick() }
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandBluePrimary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF6BB9F8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Campus Market & Live Gigs Header with Category Filter Chips and "+ Post" Action.
 */
@Composable
private fun CampusMarketHeader(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onPostNewClick: () -> Unit
) {
    val categories = listOf("All", "Hardware", "Study", "Delivery", "Notes")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Campus Marketplace & Gigs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D20)
                )
                Text(
                    text = "Peer commerce, hardware rentals & rapid gigs",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Button(
                onClick = onPostNewClick,
                modifier = Modifier.height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Post", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Category Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) BrandBluePrimary else Color.White)
                        .border(
                            1.dp,
                            if (isSelected) BrandBluePrimary else Color(0xFFD1D5DB),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectCategory(category) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF374151)
                    )
                }
            }
        }
    }
}

/**
 * Individual Marketplace Card.
 */
@Composable
private fun MarketItemCard(
    item: MarketItem,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEBF5FF))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.condition,
                        fontSize = 11.5.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "KES ${item.price.toInt()} • ${item.campusHallLocation}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBluePrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier
                    .height(38.dp)
                    .widthIn(min = 84.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Connect",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Individual MicroGig Card.
 */
@Composable
private fun MicroGigCard(
    gig: MicroGig,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (gig.urgencyLevel == GigUrgency.FLASH_URGENT) Color(0xFFFEE2E2)
                                else Color(0xFFE0F2FE)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (gig.urgencyLevel == GigUrgency.FLASH_URGENT) "URGENT GIG" else gig.tag.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gig.urgencyLevel == GigUrgency.FLASH_URGENT) Color(0xFFDC2626) else BrandBluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = gig.estimatedDuration,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = gig.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Payout: KES ${gig.payoutAmount.toInt()} • Posted by ${gig.posterName}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onAccept,
                modifier = Modifier
                    .height(38.dp)
                    .widthIn(min = 84.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Accept",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * AI Travel & Campus Exploration Dialog.
 */
@Composable
private fun AiDiscoveryDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Campus & Travel Discovery",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Ask for student exchange destinations, quiet study spots, or high-value campus gigs:",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("e.g., Best student travel deals for Fall...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (query.isNotBlank()) onSearch(query)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Explore with AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Post New Item / Gig Dialog.
 */
@Composable
private fun PostItemDialog(
    onDismiss: () -> Unit,
    onPost: (title: String, price: Double, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hardware") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Post to Campus Hub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price / Payout (KES)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: 100.0
                            if (title.isNotBlank()) {
                                onPost(title, price, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Publish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
