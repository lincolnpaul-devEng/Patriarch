package com.campus.network.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.ui.theme.CampusAccentGold
import com.campus.network.core.ui.theme.CampusAlertRed
import com.campus.network.core.ui.theme.CampusPrimary
import kotlinx.coroutines.launch

// Exact visual tokens mapped from the Patriarch theme & profile design
private val DarkCardBg = Color(0xFF0A1F36)
private val DarkCardBorder = Color(0xFF163252)
private val GoldAccent = Color(0xFFEAB308)
private val GoldAccentDark = Color(0xFFD97706)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGreyMuted = Color(0xFF94A3B8)
private val SectionRed = Color(0xFFEF4444)
private val PillPinkBg = Color(0xFFFEE2E2)
private val PillPinkText = Color(0xFFDC2626)
private val PillGreyBg = Color(0xFFF1F5F9)
private val PillGreyText = Color(0xFF64748B)
private val CardBorderLight = Color(0xFFE2E8F0)
private val ActionCircleBg = Color(0xFFF1F5F9)

/**
 * Pixel-Perfect Profile Screen:
 * Faithfully mirrors the luxury dark gold header card, quick action cards,
 * status badge lists, and red-accented section headers of the reference design,
 * 100% free of emojis and styled with professional vector iconography.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val profile = uiState.profile

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // 1. Top Navigation Bar: Back Arrow
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1A1C20),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. Luxury Dark Profile Card with Gold Geometric Pattern
            // -------------------------------------------------------------
            item {
                LuxuryGoldProfileCard(
                    profile = profile,
                    onViewActivityClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Opening Activity Ledger & Campus Streaks")
                        }
                    },
                    onOfferClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Exclusive Offer: 20% off at Strathmore Cafeteria")
                        }
                    }
                )
            }

            // -------------------------------------------------------------
            // 3. Quick Action Cards (Favourites & Money)
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = "Favourites",
                        icon = Icons.Default.FavoriteBorder,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Opening Saved Campus Listings")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionTile(
                        title = "Money",
                        icon = Icons.Default.Payments,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Opening Campus Escrow Wallet: KES 1,450 Available")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // -------------------------------------------------------------
            // 4. Action List Items (App update, Your profile, Your rating)
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionListRow(
                        icon = Icons.Default.Sync,
                        title = "App update available",
                        badgeText = "v17.42.1",
                        badgeBg = PillPinkBg,
                        badgeTextColor = PillPinkText,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Campus Network is up to date: v17.42.1")
                            }
                        }
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionListRow(
                        icon = Icons.Default.Person,
                        title = "Your profile",
                        badgeText = "32% completed",
                        badgeBg = PillPinkBg,
                        badgeTextColor = PillPinkText,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Complete degree major & graduation year to reach 100%")
                            }
                        }
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionListRow(
                        icon = Icons.Default.StarOutline,
                        title = "Your rating",
                        badgeText = "-- / 5",
                        badgeBg = PillGreyBg,
                        badgeTextColor = PillGreyText,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Behavioral Trust Score: 96 / 100 (High Trust)")
                            }
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // 5. Grouped Section: "Food Orders" (with Red Accent Bar)
            // -------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Vertical Red Accent Bar
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SectionRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Food Orders",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF141619)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ActionListRow(
                            icon = Icons.Default.ShoppingBag,
                            title = "Your orders",
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Viewing past orders & textbook trades")
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.FavoriteBorder,
                            title = "Favorite orders",
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Viewing saved regular cafeteria combos")
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.Home,
                            title = "Address book",
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Campus Halls: Strathmore Quad B, Hall 4")
                                }
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. Section: "Private Quest Deck" (Preserving Gamified Consistency)
            // -------------------------------------------------------------
            if (profile != null && profile.privateQuestDeck.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Private Quest Deck",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF141619)
                        )
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        border = BorderStroke(1.dp, CardBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            profile.privateQuestDeck.forEachIndexed { index, quest ->
                                QuestDeckListRow(
                                    quest = quest,
                                    onClaim = {
                                        viewModel.onEvent(ProfileEvent.ClaimQuest(quest.id))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Quest Completed: +${quest.xpReward} XP awarded!")
                                        }
                                    }
                                )
                                if (index < profile.privateQuestDeck.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        thickness = 0.8.dp,
                                        color = Color(0xFFF1F3F6)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Snackbar for interaction feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

// =================================================================
// LUXURY BLACK GOLD PROFILE CARD (Judy's Card)
// =================================================================

@Composable
private fun LuxuryGoldProfileCard(
    profile: StudentProfile?,
    onViewActivityClick: () -> Unit,
    onOfferClick: () -> Unit
) {
    val studentName = profile?.fullName ?: "Judy"
    val studentEmail = profile?.universityEmail ?: "judy.mobbin@gmail.com"
    val initial = studentName.firstOrNull()?.toString() ?: "J"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Geometric Line Pattern Canvas
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(22.dp))
            ) {
                val strokeColor = GoldAccent.copy(alpha = 0.12f)
                val width = size.width
                val height = size.height

                // Draw geometric diamond grid lines on top-right corner
                for (i in 0..5) {
                    val xOffset = width - (i * 50f)
                    drawLine(
                        color = strokeColor,
                        start = Offset(xOffset, 0f),
                        end = Offset(width, height * 0.7f - (i * 20f)),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(width - 250f + (i * 40f), 0f),
                        end = Offset(width - 50f + (i * 40f), height),
                        strokeWidth = 1.2f
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Upper Section: Avatar, Name, Email, View Activity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Creamy Gold Circular Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFEF08A), Color(0xFFFDE047))
                                )
                            )
                            .border(1.5.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF854D0E)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name, Email & Activity Link
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentName,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = studentEmail,
                            fontSize = 13.sp,
                            color = TextGreyMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onViewActivityClick)
                        ) {
                            Text(
                                text = "View activity",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "›",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }
                }

                // Horizontal Divider in Dark Card
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = Color(0xFF24262C)
                )

                // Lower Section: Gold Member Badge & "1 new offer" Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Gold Crown in Circle + "Gold member"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282317)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Crown",
                                tint = GoldAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Gold member",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }

                    // Right: "1 new offer" with Gold Border & Right Chevron
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, GoldAccent, RoundedCornerShape(16.dp))
                            .background(Color(0xFF1F1E1B))
                            .clickable(onClick = onOfferClick)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "1 new offer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// =================================================================
// 2-COLUMN QUICK ACTION TILE (Favourites & Money)
// =================================================================

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, CardBorderLight),
        modifier = modifier
            .height(105.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ActionCircleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF1E242B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15181E)
            )
        }
    }
}

// =================================================================
// ACTION LIST ROW (Standardized Row with Left Circle & Right Badge)
// =================================================================

@Composable
private fun ActionListRow(
    icon: ImageVector,
    title: String,
    badgeText: String? = null,
    badgeBg: Color = PillPinkBg,
    badgeTextColor: Color = PillPinkText,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon in Light Circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ActionCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF454E5B),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF16191E),
            modifier = Modifier.weight(1f)
        )

        // Optional Right Badge Pill
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Right Chevron Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF9EA6B2),
            modifier = Modifier.size(18.dp)
        )
    }
}

// =================================================================
// QUEST DECK LIST ROW (For behavioral consistency in Tab 5)
// =================================================================

@Composable
private fun QuestDeckListRow(
    quest: ConsistencyQuest,
    onClaim: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (quest.isCompleted) Color(0xFFE8F5E9) else ActionCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (quest.isCompleted) Icons.Default.Check else Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = if (quest.isCompleted) Color(0xFF2E7D32) else GoldAccentDark,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16191E)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "+${quest.xpReward} XP Reward",
                fontSize = 11.5.sp,
                color = Color(0xFF6B7280)
            )
        }

        if (quest.isCompleted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Claimed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF16171B))
                    .clickable(onClick = onClaim)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Claim",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }
        }
    }
}
