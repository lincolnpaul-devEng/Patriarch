package com.campus.network.feature.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.campus.network.core.ui.components.CampusStudentAvatar
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.StudentProfile
import kotlinx.coroutines.flow.collectLatest
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
 * Production-Grade Profile Screen:
 * - Luxury Dark Gold Card with interactive status note bubble
 * - Status Note & Mood Engine ("Add a note" with presets and expiry)
 * - Profile Identity & Multi-Profile Switcher ("Add Profile" & dual personas)
 * - Complete Profile Editor (bio, university credentials, skills)
 * - Notification Preferences & Privacy Controls
 * - Dedicated User Database Storage (1 GB quota, multi-level color progress, cache cleaning)
 * - Production Danger Zone (Log Out & double-confirmed GDPR Account Deletion)
 * 100% vector iconography adhering to zero-emoji policy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val profile = uiState.profile

    // Dialog & Sheet States
    var showStatusNoteDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMultiProfileSheet by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showUpgradeStorageDialog by remember { mutableStateOf(false) }

    var activeSubScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = activeSubScreen != ProfileSubScreen.NONE) {
        activeSubScreen = ProfileSubScreen.NONE
    }

    when (activeSubScreen) {
        ProfileSubScreen.SAVED_BOOKMARKS -> {
            SavedBookmarksScreen(
                initialItems = emptyList(),
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onOpenItem = { item ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Opening: ${item.title}")
                    }
                }
            )
            return
        }
        ProfileSubScreen.CAMPUS_WALLET -> {
            CampusWalletScreen(
                profile = profile,
                onDeposit = { amount ->
                    viewModel.onEvent(ProfileEvent.DepositWallet(amount))
                },
                onWithdraw = { amount ->
                    viewModel.onEvent(ProfileEvent.WithdrawWallet(amount))
                },
                onReleaseVault = { amount ->
                    viewModel.onEvent(ProfileEvent.ReleaseVault(amount))
                },
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
            return
        }
        ProfileSubScreen.ACTIVITY_LEDGER -> {
            ActivityLedgerScreen(
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
            return
        }
        ProfileSubScreen.TRUST_SCORE -> {
            TrustScoreScreen(
                profile = profile,
                score = profile?.behavioralTrustScore ?: 50,
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE }
            )
            return
        }
        ProfileSubScreen.PEER_TRADES -> {
            PeerTradesScreen(
                initialOrders = emptyList(),
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
            return
        }
        ProfileSubScreen.STORAGE_MANAGER -> {
            StorageManagerScreen(
                initialUsedBytes = profile?.storageUsedBytes ?: 0L,
                initialQuotaBytes = profile?.storageQuotaBytes ?: 1_073_741_824L,
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
            return
        }
        ProfileSubScreen.ACADEMIC_PORTFOLIO -> {
            AcademicPortfolioScreen(
                studentName = profile?.fullName?.ifBlank { "Student" } ?: "Student",
                studentId = profile?.studentId ?: "",
                degreeProgram = profile?.degreeProgram ?: "",
                academicYear = profile?.academicYear ?: "",
                badges = profile?.verifiedSkillBadges ?: emptyList(),
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
            return
        }
        ProfileSubScreen.RENDEZVOUS_POINTS -> {
            RendezvousPointsScreen(
                profile = profile,
                onBackClick = { activeSubScreen = ProfileSubScreen.NONE },
                onSelectSpot = { spotName ->
                    if (profile != null) {
                        viewModel.onEvent(
                            ProfileEvent.UpdateProfile(
                                fullName = profile.fullName,
                                universityName = profile.universityName,
                                degreeProgram = profile.degreeProgram,
                                academicYear = profile.academicYear,
                                campusResidence = profile.campusResidence,
                                phoneNumber = profile.phoneNumber,
                                bio = profile.bio,
                                interests = profile.interests,
                                avatarUrl = profile.avatarUrl
                            )
                        )
                    }
                },
                onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
            return
        }
        ProfileSubScreen.NONE -> {
            // Main profile screen continues below
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // 1. Top Navigation Bar: Back Arrow & Multi-Profile Action
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multi-Profile Switcher Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, CardBorderLight, RoundedCornerShape(20.dp))
                                .clickable { showMultiProfileSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = profile?.personaRole ?: "Undergraduate Student",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Quick Sign Out Button in Header
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2))
                                .clickable { showLogOutDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. Luxury Dark Profile Card with Gold Geometric Pattern & Status Note Pill
            // -------------------------------------------------------------
            item {
                LuxuryGoldProfileCard(
                    profile = profile,
                    onNoteClick = { showStatusNoteDialog = true },
                    onEditClick = { showEditProfileDialog = true },
                    onViewActivityClick = { activeSubScreen = ProfileSubScreen.ACTIVITY_LEDGER },
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
                        onClick = { activeSubScreen = ProfileSubScreen.SAVED_BOOKMARKS },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionTile(
                        title = "Money",
                        icon = Icons.Default.Payments,
                        onClick = { activeSubScreen = ProfileSubScreen.CAMPUS_WALLET },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // -------------------------------------------------------------
            // 4. Action List Items (App update, Edit profile, Multi-Account, Trust Score)
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val completenessScore = calculateProfileCompleteness(profile)
                        val (completenessBg, completenessTextColor) = when {
                            completenessScore >= 80 -> Pair(Color(0xFFDCFCE7), Color(0xFF16A34A))
                            completenessScore >= 40 -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                            else -> Pair(PillPinkBg, PillPinkText)
                        }

                        ActionListRow(
                            icon = Icons.Default.Person,
                            title = "Edit Profile & Credentials",
                            badgeText = "Complete $completenessScore%",
                            badgeBg = completenessBg,
                            badgeTextColor = completenessTextColor,
                            onClick = { showEditProfileDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.School,
                            title = "Verified Academic Portfolio",
                            badgeText = "${profile?.verifiedSkillBadges?.size ?: 0} Badges",
                            badgeBg = PillGreyBg,
                            badgeTextColor = PillGreyText,
                            onClick = { activeSubScreen = ProfileSubScreen.ACADEMIC_PORTFOLIO }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.ManageAccounts,
                            title = "Switch / Add Campus Persona",
                            badgeText = "${(profile?.alternateProfiles?.size ?: 0) + 1} Profiles",
                            badgeBg = PillGreyBg,
                            badgeTextColor = PillGreyText,
                            onClick = { showMultiProfileSheet = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.StarOutline,
                            title = "Behavioral Trust Score",
                            badgeText = "${profile?.behavioralTrustScore ?: 50} / 100",
                            badgeBg = PillGreyBg,
                            badgeTextColor = PillGreyText,
                            onClick = { activeSubScreen = ProfileSubScreen.TRUST_SCORE }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. Dedicated User Storage & Cloud Database Quota (1 GB Quota)
            // -------------------------------------------------------------
            item {
                ProfileStorageCard(
                    usedBytes = profile?.storageUsedBytes ?: 0L,
                    quotaBytes = profile?.storageQuotaBytes ?: 1_073_741_824L,
                    onClearCache = { viewModel.onEvent(ProfileEvent.ClearCache) },
                    onUpgradeStorage = { activeSubScreen = ProfileSubScreen.STORAGE_MANAGER }
                )
            }

            // -------------------------------------------------------------
            // 6. Notification & Privacy Controls
            // -------------------------------------------------------------
            item {
                SectionHeader(title = "Notification Preferences", color = Color(0xFF007AFF))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        ToggleRow(
                            icon = Icons.Default.Notifications,
                            title = "Direct Messages & Mentions",
                            checked = profile?.notificationsEnabled ?: true,
                            onCheckedChange = { checked ->
                                if (profile != null) {
                                    viewModel.onEvent(
                                        ProfileEvent.UpdateNotifications(
                                            notificationsEnabled = checked,
                                            escrowAlertsEnabled = profile.escrowAlertsEnabled,
                                            questRemindersEnabled = profile.questRemindersEnabled
                                        )
                                    )
                                }
                            }
                        )

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F3F6))

                        ToggleRow(
                            icon = Icons.Default.Payments,
                            title = "SafeTrade Payment Alerts",
                            checked = profile?.escrowAlertsEnabled ?: true,
                            onCheckedChange = { checked ->
                                if (profile != null) {
                                    viewModel.onEvent(
                                        ProfileEvent.UpdateNotifications(
                                            notificationsEnabled = profile.notificationsEnabled,
                                            escrowAlertsEnabled = checked,
                                            questRemindersEnabled = profile.questRemindersEnabled
                                        )
                                    )
                                }
                            }
                        )

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F3F6))

                        ToggleRow(
                            icon = Icons.Default.Timer,
                            title = "Campus Quest & Streak Reminders",
                            checked = profile?.questRemindersEnabled ?: true,
                            onCheckedChange = { checked ->
                                if (profile != null) {
                                    viewModel.onEvent(
                                        ProfileEvent.UpdateNotifications(
                                            notificationsEnabled = profile.notificationsEnabled,
                                            escrowAlertsEnabled = profile.escrowAlertsEnabled,
                                            questRemindersEnabled = checked
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Privacy & Trust Settings", color = GoldAccent)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        ToggleRow(
                            icon = Icons.Default.Security,
                            title = "Show Public Trust Score",
                            checked = profile?.isTrustScorePublic ?: true,
                            onCheckedChange = { checked ->
                                if (profile != null) {
                                    viewModel.onEvent(
                                        ProfileEvent.UpdatePrivacy(
                                            isTrustScorePublic = checked,
                                            isOnlineStatusVisible = profile.isOnlineStatusVisible,
                                            activityLedgerVisibility = profile.activityLedgerVisibility
                                        )
                                    )
                                }
                            }
                        )

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F3F6))

                        ToggleRow(
                            icon = Icons.Default.Visibility,
                            title = "Share In-Campus Active Status",
                            checked = profile?.isOnlineStatusVisible ?: true,
                            onCheckedChange = { checked ->
                                if (profile != null) {
                                    viewModel.onEvent(
                                        ProfileEvent.UpdatePrivacy(
                                            isTrustScorePublic = profile.isTrustScorePublic,
                                            isOnlineStatusVisible = checked,
                                            activityLedgerVisibility = profile.activityLedgerVisibility
                                        )
                                    )
                                }
                            }
                        )

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F3F6))

                        ActionListRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Sign Out of Account",
                            badgeText = "Sign out",
                            badgeBg = PillPinkBg,
                            badgeTextColor = PillPinkText,
                            onClick = { showLogOutDialog = true }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 7. Grouped Section: "Campus Commerce & SafeTrades"
            // -------------------------------------------------------------
            item {
                SectionHeader(title = "Campus Commerce & SafeTrades", color = Color(0xFF007AFF))
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
                            title = "SafeTrades & Orders",
                            badgeText = "0 Active",
                            badgeBg = PillPinkBg,
                            badgeTextColor = PillPinkText,
                            onClick = { activeSubScreen = ProfileSubScreen.PEER_TRADES }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.FavoriteBorder,
                            title = "Saved Listings & Watchlist",
                            badgeText = "${profile?.savedListingIds?.size ?: 0} Saved",
                            badgeBg = PillGreyBg,
                            badgeTextColor = PillGreyText,
                            onClick = { activeSubScreen = ProfileSubScreen.SAVED_BOOKMARKS }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.Home,
                            title = "Campus Rendezvous Points",
                            badgeText = profile?.preferredRendezvousPoint?.ifBlank { null } ?: profile?.campusResidence?.ifBlank { null } ?: "Set location",
                            badgeBg = PillGreyBg,
                            badgeTextColor = PillGreyText,
                            onClick = { activeSubScreen = ProfileSubScreen.RENDEZVOUS_POINTS }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 8. Section: "Private Quest Deck"
            // -------------------------------------------------------------
            if (profile != null && profile.privateQuestDeck.isNotEmpty()) {
                item {
                    SectionHeader(title = "Private Quest Deck", color = GoldAccent)
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

            // -------------------------------------------------------------
            // 9. Danger Zone: Log Out & Account Deletion
            // -------------------------------------------------------------
            item {
                SectionHeader(title = "Account Lifecycle & Security", color = SectionRed)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, SectionRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ActionListRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Log Out of Patriarch",
                            badgeText = "Sign out",
                            badgeBg = PillGreyBg,
                            badgeTextColor = PillGreyText,
                            onClick = { showLogOutDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F3F6)
                        )

                        ActionListRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account (Irreversible)",
                            badgeText = "Danger",
                            badgeBg = PillPinkBg,
                            badgeTextColor = PillPinkText,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }
        }

        // Floating Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }

    // =================================================================
    // DIALOGS & BOTTOM SHEETS
    // =================================================================

    // 1. Set Status Note Dialog
    if (showStatusNoteDialog) {
        SetStatusNoteDialog(
            currentNote = profile?.statusNote.orEmpty(),
            currentExpiry = profile?.statusNoteExpiry ?: "Keep until updated",
            onDismiss = { showStatusNoteDialog = false },
            onSave = { note, emoji, expiry ->
                viewModel.onEvent(ProfileEvent.SetStatusNote(note, emoji, expiry))
                showStatusNoteDialog = false
            },
            onClear = {
                viewModel.onEvent(ProfileEvent.ClearStatusNote)
                showStatusNoteDialog = false
            }
        )
    }

    // 2. Edit Profile Drawer Sheet
    if (showEditProfileDialog) {
        val targetProfile = profile ?: com.campus.network.core.model.StudentProfile()
        EditProfileDrawerSheet(
            profile = targetProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, university, program, year, residence, phone, bio, interests, avatarUrl ->
                viewModel.saveProfile(
                    fullName = name,
                    universityName = university,
                    degreeProgram = program,
                    academicYear = year,
                    campusResidence = residence,
                    phoneNumber = phone,
                    bio = bio,
                    interests = interests,
                    avatarUrl = avatarUrl
                )
            }
        )
    }

    // 3. Multi-Profile Switcher & Creator Sheet
    if (showMultiProfileSheet && profile != null) {
        MultiProfileSheet(
            activeProfile = profile,
            onDismiss = { showMultiProfileSheet = false },
            onSwitch = { targetId ->
                viewModel.onEvent(ProfileEvent.SwitchProfile(targetId))
                showMultiProfileSheet = false
            },
            onAddProfile = { newProfile ->
                viewModel.onEvent(ProfileEvent.AddProfile(newProfile))
                showMultiProfileSheet = false
            }
        )
    }

    // 4. Log Out Confirmation Dialog
    if (showLogOutDialog) {
        AlertDialog(
            onDismissRequest = { showLogOutDialog = false },
            title = {
                Text("Log Out of Patriarch?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "You will need to re-authenticate with your university credentials. Offline cached files will remain until expired.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Delete Account Dialog (Production GDPR Double-Confirmed)
    if (showDeleteAccountDialog && profile != null) {
        DeleteAccountDialog(
            studentId = profile.studentId,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirmDelete = { reason ->
                viewModel.onEvent(ProfileEvent.DeleteAccount(reason))
                showDeleteAccountDialog = false
            }
        )
    }

    // 6. Upgrade Storage Dialog
    if (showUpgradeStorageDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeStorageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF007AFF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Expand Dedicated Cloud Storage", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Every verified student receives 1 GB of free cloud database storage. Need more space for video assignments and research datasets?",
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, CardBorderLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("+1 GB Student Storage Tier", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("High-availability peer CDN backup", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Text("Free (Campus Merit)", fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(ProfileEvent.UpgradeStorage)
                        showUpgradeStorageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    )
                ) {
                    Text("Claim +1 GB Upgrade", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeStorageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =================================================================
// SECTION HEADER COMPOSABLE
// =================================================================

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF141619)
        )
    }
}

// =================================================================
// LUXURY BLACK GOLD PROFILE CARD WITH INTERACTIVE STATUS NOTE
// =================================================================

@Composable
private fun LuxuryGoldProfileCard(
    profile: StudentProfile?,
    onNoteClick: () -> Unit,
    onEditClick: () -> Unit,
    onViewActivityClick: () -> Unit,
    onOfferClick: () -> Unit
) {
    val studentName = profile?.fullName?.ifBlank { "Student" } ?: "Student"
    val studentEmail = profile?.universityEmail ?: ""
    val initial = studentName.firstOrNull()?.toString()?.uppercase() ?: "S"
    val hasNote = !profile?.statusNote.isNullOrBlank()

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
                // Upper Section: Avatar, Name, Email, Status Note Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Luxury Circular Avatar with Note indicator badge & photo support
                    Box(modifier = Modifier.size(68.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.5.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CampusStudentAvatar(
                                avatarUrl = profile?.avatarUrl,
                                fullName = studentName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Edit badge button on avatar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.dp, GoldAccent, CircleShape)
                                .clickable(onClick = onEditClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = GoldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name, Email & Activity Link
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentName,
                            fontSize = 21.sp,
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
                                text = "View activity ledger",
                                fontSize = 12.sp,
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

                // Interactive Status Note Pill (Instagram / WhatsApp thought bubble style)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.8.dp, if (hasNote) GoldAccent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onNoteClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasNote) Icons.Default.CloudDone else Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (hasNote) GoldAccent else TextGreyMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasNote) profile!!.statusNote!! else "Add a note or campus status...",
                                fontSize = 13.sp,
                                fontWeight = if (hasNote) FontWeight.Medium else FontWeight.Normal,
                                color = if (hasNote) TextWhite else TextGreyMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (hasNote) {
                            Text(
                                text = "Edit",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Divider in Dark Card
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = Color(0xFF24262C)
                )

                // Lower Section: Gold Member Badge & "1 new offer" Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282317)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Crown",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Gold member",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }

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
// DEDICATED USER STORAGE CARD WITH MULTI-LEVEL COLOR CODED PROGRESS
// =================================================================

@Composable
private fun ProfileStorageCard(
    usedBytes: Long,
    quotaBytes: Long,
    onClearCache: () -> Unit,
    onUpgradeStorage: () -> Unit
) {
    val usedMb = usedBytes.toFloat() / (1024f * 1024f)
    val quotaMb = quotaBytes.toFloat() / (1024f * 1024f)
    val percentage = if (quotaBytes > 0) (usedBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0f, 1f) else 0f

    // Color code at every level:
    // Green (Safe: < 60%)
    // Orange (Warning: 60% - 85%)
    // Red (Critical: > 85%)
    val progressColor = when {
        percentage < 0.60f -> Color(0xFF22C55E) // Green Safe
        percentage < 0.85f -> Color(0xFFF59E0B) // Orange Warning
        else -> Color(0xFFEF4444) // Red Critical
    }

    val statusText = when {
        percentage < 0.60f -> "Safe Capacity"
        percentage < 0.85f -> "Moderate Usage"
        else -> "Storage Critical"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Dedicated Database Storage & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dedicated Cloud Storage",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Allocated per student database tier",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Usage numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.1f MB used", usedMb),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = String.format("Quota: %.1f GB (%.0f%%)", quotaMb / 1024f, percentage * 100f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progressive Multi-Level Linear Indicator
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color(0xFFF1F5F9),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Clear Cache & Upgrade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(
                        imageVector = Icons.Default.CleanHands,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clear Cache",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }

                Button(
                    onClick = onUpgradeStorage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Upgrade (+1 GB)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// =================================================================
// TOGGLE ROW COMPOSABLE
// =================================================================

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ActionCircleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}

// =================================================================
// 1. SET STATUS NOTE DIALOG (Add a Note)
// =================================================================

@Composable
private fun SetStatusNoteDialog(
    currentNote: String,
    currentExpiry: String,
    onDismiss: () -> Unit,
    onSave: (note: String, emoji: String, expiry: String) -> Unit,
    onClear: () -> Unit
) {
    var noteText by remember { mutableStateOf(currentNote) }
    var selectedExpiry by remember { mutableStateOf(currentExpiry) }
    val maxChars = 80

    val presetNotes = listOf(
        "Studying at Quad",
        "Looking for Study Partner",
        "Focus Mode / DND",
        "Available for Gigs",
        "In Class"
    )

    val expiryOptions = listOf("4 Hours", "End of Day", "Keep until updated")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Campus Status Note",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Share what you're working on with peers in your course and study channels.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= maxChars) noteText = it },
                    placeholder = { Text("e.g. Grinding finals in Quad library...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = CardBorderLight
                    ),
                    supportingText = {
                        Text(
                            text = "${noteText.length}/$maxChars",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )

                Text(
                    text = "Quick Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                // Presets chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetNotes) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (noteText == preset) Color(0xFF0F172A) else PillGreyBg)
                                .clickable { noteText = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (noteText == preset) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Text(
                    text = "Auto-Clear Duration",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                // Expiry options
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    expiryOptions.forEach { opt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedExpiry == opt) Color(0xFF007AFF) else PillGreyBg)
                                .clickable { selectedExpiry = opt }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = opt,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedExpiry == opt) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(noteText, "Status", selectedExpiry) },
                enabled = noteText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White
                )
            ) {
                Text("Save Note", color = Color.White)
            }
        },
        dismissButton = {
            Row {
                if (currentNote.isNotBlank()) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = SectionRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// =================================================================
// 2. FULL PROFILE EDITOR DRAWER SHEET (Edit Profile & Avatar)
// =================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDrawerSheet(
    profile: StudentProfile,
    onDismiss: () -> Unit,
    onSave: suspend (
        name: String,
        university: String,
        program: String,
        year: String,
        residence: String,
        phone: String,
        bio: String,
        interests: List<String>,
        avatarUrl: String?
    ) -> Result<Unit>
) {
    var fullName by remember { mutableStateOf(profile.fullName) }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl ?: "") }
    var universityName by remember { mutableStateOf(profile.universityName) }
    var degreeProgram by remember { mutableStateOf(profile.degreeProgram) }
    var academicYear by remember { mutableStateOf(profile.academicYear) }
    var campusResidence by remember { mutableStateOf(profile.campusResidence) }
    var phoneNumber by remember { mutableStateOf(profile.phoneNumber) }
    var bio by remember { mutableStateOf(profile.bio) }
    val interests = remember { mutableStateListOf(*profile.interests.toTypedArray()) }
    var newInterestInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val sheetScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Profile & Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Update your verified student persona and avatar",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar Editor Section (Upload via gallery picker only - persisted to Cloudinary)
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val uploadScope = rememberCoroutineScope()
                    var isUploadingAvatar by remember { mutableStateOf(false) }
                    var avatarUploadError by remember { mutableStateOf<String?>(null) }

                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            isUploadingAvatar = true
                            avatarUploadError = null
                            uploadScope.launch {
                                val result = com.campus.network.core.network.CloudinaryUploader().uploadFromUri(context, uri)
                                isUploadingAvatar = false
                                if (result.isSuccess) {
                                    avatarUrl = result.getOrThrow()
                                } else {
                                    avatarUploadError = "Upload failed: ${result.exceptionOrNull()?.localizedMessage ?: "Network error"}"
                                }
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, CardBorderLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isUploadingAvatar) imagePickerLauncher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, GoldAccent, CircleShape)
                                    .clickable { if (!isUploadingAvatar) imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                CampusStudentAvatar(
                                    avatarUrl = avatarUrl.ifBlank { null },
                                    fullName = fullName.ifBlank { "Student" },
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isUploadingAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x99000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = GoldAccent,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x4D000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Upload Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Profile Avatar Photo",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isUploadingAvatar) "Uploading to Cloud..." else "Tap avatar to select from gallery",
                                    fontSize = 12.5.sp,
                                    color = if (isUploadingAvatar) Color(0xFF007AFF) else Color(0xFF64748B)
                                )
                                if (avatarUploadError != null) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = avatarUploadError!!,
                                        fontSize = 11.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { if (!isUploadingAvatar) imagePickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    enabled = !isUploadingAvatar,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isUploadingAvatar) "Uploading..." else "Select Photo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = profile.studentId,
                        onValueChange = {},
                        label = { Text("Student ID (Verified)") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = universityName,
                        onValueChange = { universityName = it },
                        label = { Text("University / Campus") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = degreeProgram,
                        onValueChange = { degreeProgram = it },
                        label = { Text("Degree / Program") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("Academic Year / Semester") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = campusResidence,
                        onValueChange = { campusResidence = it },
                        label = { Text("Campus Residence / Quad Hall") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number (Escrow SMS)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 200) bio = it },
                        label = { Text("Bio / Academic Focus") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("${bio.length}/200") }
                    )
                }

                item {
                    Text("Interests & Verified Skills", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(interests) { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PillGreyBg)
                                    .clickable { interests.remove(skill) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("$skill ✕", fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = newInterestInput,
                            onValueChange = { newInterestInput = it },
                            placeholder = { Text("Add skill tag...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newInterestInput.isNotBlank()) {
                                    interests.add(newInterestInput.trim())
                                    newInterestInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (saveError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(10.dp)
                ) {
                    Text(
                        text = saveError!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions Bottom Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                }

                Button(
                    onClick = {
                        if (!isSaving) {
                            sheetScope.launch {
                                isSaving = true
                                saveError = null
                                val result = onSave(
                                    fullName,
                                    universityName,
                                    degreeProgram,
                                    academicYear,
                                    campusResidence,
                                    phoneNumber,
                                    bio,
                                    interests.toList(),
                                    avatarUrl.ifBlank { null }
                                )
                                isSaving = false
                                if (result.isSuccess) {
                                    onDismiss()
                                } else {
                                    saveError = result.exceptionOrNull()?.localizedMessage
                                        ?: "Failed to save profile changes. Please check network connection and try again."
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCardBg,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// =================================================================
// 3. MULTI-PROFILE & DUAL PERSONA SHEET (Add Profile)
// =================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiProfileSheet(
    activeProfile: StudentProfile,
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit,
    onAddProfile: (StudentProfile) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isAddingNew by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newId by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("Club Executive / Campus Guild") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAddingNew) "Create Secondary Student Persona" else "Switch Campus Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                if (!isAddingNew) {
                    TextButton(onClick = { isAddingNew = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Profile")
                    }
                }
            }

            if (!isAddingNew) {
                // Active profile item
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, Color(0xFF007AFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeProfile.fullName.firstOrNull()?.toString() ?: "L",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeProfile.fullName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text(activeProfile.personaRole, fontSize = 12.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0F2FE))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                        }
                    }
                }

                // Alternate profiles list
                activeProfile.alternateProfiles.forEach { alt ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorderLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSwitch(alt.studentId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = alt.fullName.firstOrNull()?.toString() ?: "A",
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alt.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(alt.personaRole, fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            OutlinedButton(
                                onClick = { onSwitch(alt.studentId) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Switch", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // Add New Persona Form
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Student (Guild Rep)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newId,
                    onValueChange = { newId = it },
                    label = { Text("Campus ID / Code") },
                    placeholder = { Text("e.g. GUILD-VP-2026") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Official Campus Email") },
                    placeholder = { Text("e.g. guild.vp@strathmore.edu") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Select Persona Role", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    listOf(
                        "Club Executive / Campus Guild",
                        "Teaching Assistant / Course Tutor",
                        "Campus Researcher"
                    ).forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newRole = role }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = newRole == role,
                                onClick = { newRole = role },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(role, fontSize = 13.5.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isAddingNew = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            if (newName.isNotBlank() && newId.isNotBlank()) {
                                onAddProfile(
                                    StudentProfile(
                                        studentId = newId.trim(),
                                        fullName = newName.trim(),
                                        universityEmail = newEmail.ifBlank { activeProfile.universityEmail },
                                        universityName = activeProfile.universityName,
                                        degreeProgram = activeProfile.degreeProgram,
                                        academicYear = activeProfile.academicYear,
                                        consistencyStreakDays = 1,
                                        behavioralTrustScore = 95,
                                        verifiedSkillBadges = emptyList(),
                                        privateQuestDeck = emptyList(),
                                        personaRole = newRole
                                    )
                                )
                            }
                        },
                        enabled = newName.isNotBlank() && newId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Persona", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// =================================================================
// 4. PRODUCTION-GRADE DELETE ACCOUNT DIALOG (Double-Confirmed GDPR)
// =================================================================

@Composable
private fun DeleteAccountDialog(
    studentId: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (reason: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Graduating / Completed Studies") }
    var confirmationInput by remember { mutableStateOf("") }
    val isConfirmed = confirmationInput.trim().equals("DELETE", ignoreCase = false) ||
            confirmationInput.trim().equals(studentId, ignoreCase = true)

    val reasons = listOf(
        "Graduating / Completed Studies",
        "Transferring to Another Institution",
        "Privacy & Data Concerns",
        "Duplicate Account"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SectionRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account Permanently", fontWeight = FontWeight.Bold, fontSize = 17.5.sp, color = SectionRed)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This action is irreversible. All student data, badges, and records will be purged immediately:",
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Complete forfeiture of Campus Merit Points & Trust Score", fontSize = 11.5.sp, color = SectionRed)
                        Text("• Immediate cancellation of SafePay wallet & pending settlements", fontSize = 11.5.sp, color = SectionRed)
                        Text("• Permanent ejection from all enrolled academic course channels", fontSize = 11.5.sp, color = SectionRed)
                    }
                }

                Text(
                    text = "Primary Reason for Leaving",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = SectionRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(reason, fontSize = 12.5.sp, color = Color(0xFF1E293B))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "To confirm, type DELETE or your Student ID ($studentId):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = confirmationInput,
                    onValueChange = { confirmationInput = it },
                    placeholder = { Text("Type DELETE") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SectionRed,
                        unfocusedBorderColor = CardBorderLight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(selectedReason) },
                enabled = isConfirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SectionRed,
                    disabledContainerColor = SectionRed.copy(alpha = 0.35f)
                )
            ) {
                Text("Permanently Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF16191E),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

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
                    color = badgeTextColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

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

private fun calculateProfileCompleteness(profile: StudentProfile?): Int {
    if (profile == null) return 0
    var score = 0
    if (profile.fullName.isNotBlank()) score += 15
    if (profile.universityEmail.isNotBlank()) score += 10
    if (profile.universityName.isNotBlank()) score += 15
    if (profile.degreeProgram.isNotBlank()) score += 10
    if (profile.academicYear.isNotBlank()) score += 10
    if (profile.campusResidence.isNotBlank()) score += 10
    if (profile.phoneNumber.isNotBlank()) score += 10
    if (profile.bio.isNotBlank()) score += 10
    if (!profile.avatarUrl.isNullOrBlank()) score += 10
    return score.coerceIn(0, 100)
}
