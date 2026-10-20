package com.campus.network.feature.channels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.feature.channels.components.ChannelChatScreen
import com.campus.network.feature.channels.components.ChannelProfileScreen
import com.campus.network.feature.channels.components.CreateChannelModalSheet
import com.campus.network.feature.channels.components.DiscoverServersPanel
import com.campus.network.feature.channels.components.CourseMateAvatarLineup
import com.campus.network.feature.channels.components.CourseMateDiscoverScreen
import com.campus.network.feature.channels.components.EditServerProfileSheet
import com.campus.network.feature.channels.components.EditServerSettingsSheet
import com.campus.network.feature.channels.components.IndividualStudentProfileScreen
import com.campus.network.feature.channels.components.ServerInviteSheet
import com.campus.network.feature.channels.components.ServerMemberProfileSheet
import com.campus.network.feature.channels.components.ServerMembersSheet
import com.campus.network.feature.channels.components.ServerProfileDrawer
import com.campus.network.feature.channels.components.ServerRulesSheet
import com.campus.network.feature.channels.model.CareerCourse
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.channels.model.ServerRoleType
import com.campus.network.feature.channels.model.InDemandCompany
import com.campus.network.core.ui.components.CampusInlineNetworkAlertCard
import com.campus.network.core.ui.components.MessageThreadRowSkeleton
import com.campus.network.core.ui.components.ShimmerAsyncImage
import com.campus.network.core.ui.components.rememberCampusNetworkState
import com.campus.network.core.ui.theme.BrandBlueInteractive
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGold
import com.campus.network.core.ui.theme.BrandGoldDark
import com.campus.network.core.ui.theme.SurfaceSubtle
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandNavy = BrandBluePrimary
private val BrandGoldColor = BrandGold
private val BrandGoldDarkColor = BrandGoldDark
private val TextPrimaryNavy = TextPrimary
private val TextSecondarySlate = TextSecondary
private val LinkBlue = BrandBlueInteractive
private val SectionDividerBg = SurfaceSubtle

/**
 * Pixel-Perfect Academic Workspace & Career Channels Screen:
 * Complete UI flow with peer course-mate matching and discovery:
 * - CHANNELS_HUB: LinkedIn-inspired Academic Workspace with live course-mate avatar lineups
 * - DISCOVER_CAROUSEL: Full-screen Discover screen matching user reference image
 * - STUDENT_PROFILE: Individual student profile view with Helper rewards, study goals & actions
 * - CHANNEL_CHAT: patriarch-style channel chat session with rich embeds, replies & media drawer
 */
@Composable
fun ChannelsScreen(
    viewModel: ChannelsViewModel,
    onNavigateToChannelChat: (String) -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigateToDirectChat: ((peerId: String, peerName: String, peerHandle: String?, serverName: String?, serverTag: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Android back navigation per view mode
    BackHandler(enabled = uiState.viewMode != ChannelViewMode.CHANNELS_HUB) {
        viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
    }

    BackHandler(enabled = uiState.previewServer != null) {
        viewModel.onEvent(ChannelsEvent.ExitServerPreview)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshServers()
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(ChannelsEvent.ClearFeedback())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AnimatedContent(
            targetState = uiState.viewMode,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ChannelsViewModeTransition"
        ) { mode ->
            when (mode) {
                ChannelViewMode.CHANNELS_HUB -> {
                    patriarchServersHubContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToChannelChat = onNavigateToChannelChat,
                        onNavigateToMessages = onNavigateToMessages,
                        onProfileClick = onNavigateToProfile,
                        onNotificationClick = onNavigateToNotifications
                    )
                }

                ChannelViewMode.CHANNEL_CHAT -> {
                    ChannelChatScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackClick = {
                            viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
                        },
                        onChannelProfileClick = {
                            viewModel.onEvent(ChannelsEvent.OpenChannelProfile)
                        }
                    )
                }

                ChannelViewMode.CHANNEL_PROFILE -> {
                    ChannelProfileScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackClick = {
                            viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
                        }
                    )
                }

                ChannelViewMode.DISCOVER_CAROUSEL -> {
                    CourseMateDiscoverScreen(
                        course = uiState.selectedCourseForDiscovery,
                        mates = uiState.discoverMates,
                        likedIds = uiState.likedStudentIds,
                        onBackClick = {
                            viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
                        },
                        onStudentClick = { student ->
                            viewModel.onEvent(ChannelsEvent.OpenStudentProfile(student))
                        },
                        onToggleLike = { studentId ->
                            viewModel.onEvent(ChannelsEvent.ToggleLikeStudent(studentId))
                        }
                    )
                }

                ChannelViewMode.STUDENT_PROFILE -> {
                    uiState.selectedStudentForProfile?.let { student ->
                        IndividualStudentProfileScreen(
                            student = student,
                            isLiked = uiState.likedStudentIds.contains(student.id),
                            onBackClick = {
                                viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
                            },
                            onConnectClick = {
                                viewModel.onEvent(ChannelsEvent.ToggleLikeStudent(student.id))
                            },
                            onMessageClick = {
                                val channelId = student.sharedCourses.firstOrNull() ?: "course_eng_201"
                                onNavigateToChannelChat(channelId)
                            }
                        )
                    }
                }
            }
        }

        // SERVER MEMBER PROFILE SHEET (Accessible globally across channels hub & channel profile)
        if (uiState.selectedMemberForProfile != null) {
            val targetServer = uiState.selectedServer ?: uiState.previewServer
            val currentStudentId = uiState.studentProfile?.studentId?.ifBlank { uiState.studentProfile?.universityEmail }
            val isCurrentOwner = targetServer != null && currentStudentId != null && currentStudentId.isNotBlank() && targetServer.ownerId == currentStudentId

            ServerMemberProfileSheet(
                member = uiState.selectedMemberForProfile,
                isOwner = isCurrentOwner,
                onUpdateMemberRole = { memberId, newRole ->
                    if (targetServer != null) {
                        viewModel.onEvent(ChannelsEvent.UpdateMemberRole(targetServer.id, memberId, newRole))
                    }
                },
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseMemberProfile)
                },
                onSendFriendRequest = { memberId ->
                    viewModel.onEvent(ChannelsEvent.SendFriendRequest(memberId))
                },
                onCancelFriendRequest = { memberId ->
                    viewModel.onEvent(ChannelsEvent.CancelFriendRequest(memberId))
                },
                onAcceptFriendRequest = { memberId ->
                    viewModel.onEvent(ChannelsEvent.AcceptFriendRequest(memberId))
                },
                onDeclineFriendRequest = { memberId ->
                    viewModel.onEvent(ChannelsEvent.DeclineFriendRequest(memberId))
                },
                onDirectMessage = { member ->
                    viewModel.onEvent(ChannelsEvent.CloseMemberProfile)
                    val currentServerName = uiState.selectedServer?.name
                    if (onNavigateToDirectChat != null) {
                        onNavigateToDirectChat(
                            member.id,
                            member.serverNickname ?: member.name,
                            member.handle,
                            currentServerName,
                            member.serverTag
                        )
                    } else {
                        onNavigateToMessages()
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }
}

/**
 * Pixel-Perfect patriarch Servers Screen matching the reference mobile layout:
 * - 72dp Left Server Rail with honeycomb Home icon, server squircle icons, active pill indicators, and red badges
 * - Pure White Channels Panel with rounded top-start corner (20dp), verified header, search, and channel list
 * - Floating circular steel-blue drawer button (<<) at vertical center right
 * - Floating bottom profile bar with Lincoln Paul avatar, online status, and notification bell
 * ZERO EMOJI POLICY strictly enforced (using Vector Icons for lightning bolt, badges, etc.)
 */
@Composable
private fun patriarchServersHubContent(
    uiState: ChannelsUiState,
    viewModel: ChannelsViewModel,
    onNavigateToChannelChat: (String) -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentServer = uiState.selectedServer ?: uiState.joinedServers.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE3E5E8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // LEFT RAIL (Server Column - Width 72.dp)
            ServerRail(
                servers = uiState.joinedServers,
                selectedServerId = if (uiState.previewServer != null) "" else uiState.selectedServerId,
                isAddServerSelected = uiState.isAddServerSelected || (uiState.joinedServers.isEmpty() && uiState.previewServer == null),
                unreadDmCount = uiState.activeUserUnreadCount,
                onNavigateToMessages = onNavigateToMessages,
                onSelectServer = { serverId ->
                    viewModel.onEvent(ChannelsEvent.SelectServer(serverId))
                },
                onAddServer = {
                    viewModel.onEvent(ChannelsEvent.AddServerClick)
                },
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
            )

            // RIGHT MAIN PANEL (White rounded panel)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 20.dp),
                color = Color.White
            ) {
                val currentUserId = uiState.studentProfile?.studentId?.ifBlank { uiState.studentProfile?.universityEmail }.orEmpty().trim()
                val currentUserEmail = uiState.studentProfile?.universityEmail.orEmpty().trim().lowercase()
                if (uiState.isAddServerSelected || currentServer == null) {
                    DiscoverServersPanel(
                        discoverableServers = uiState.discoverableServers,
                        joinedServerIds = uiState.joinedServerIds,
                        currentUserId = currentUserId,
                        currentUserEmail = currentUserEmail,
                        onSelectServer = { serverId ->
                            viewModel.onEvent(ChannelsEvent.SelectServer(serverId))
                        },
                        onCreateServerSubmit = { name, category ->
                            viewModel.onEvent(ChannelsEvent.CreateServer(name, category))
                        },
                        onCreateServerFullSubmit = { name, category, description, avatarResName, bannerResName, onResult ->
                            viewModel.onEvent(ChannelsEvent.CreateServer(name, category, description, avatarResName, bannerResName, onResult))
                        },
                        onJoinInviteSubmit = { inviteCode ->
                            viewModel.onEvent(ChannelsEvent.JoinServerByInvite(inviteCode))
                        },
                        onPreviewCommunitySubmit = { serverName ->
                            viewModel.onEvent(ChannelsEvent.PreviewCommunityServer(serverName))
                        },
                        loadingPreviewServerName = uiState.loadingPreviewServerName
                    )
                } else {
                    val currentMembers = uiState.serverMembers[currentServer.id].orEmpty()
                    val currentMember = currentMembers.find { it.isSelf }
                    val isOwner = (currentUserId.isNotBlank() && currentServer.ownerId.equals(currentUserId, ignoreCase = true)) ||
                            (currentUserEmail.isNotBlank() && currentServer.ownerId.equals(currentUserEmail, ignoreCase = true))
                    val isAdmin = isOwner || currentMember?.role?.roleType in listOf(ServerRoleType.OWNER, ServerRoleType.ADMIN)
                    val isMember = uiState.joinedServerIds.contains(currentServer.id) || isOwner
                    ServerDetailsPanel(
                        server = currentServer,
                        selectedChannelId = uiState.selectedChannelId,
                        isOwner = isOwner,
                        isAdmin = isAdmin,
                        isMember = isMember,
                        isJoining = uiState.isJoiningServer,
                        onChannelClick = { channel ->
                            viewModel.onEvent(ChannelsEvent.OpenChannelChat(channel))
                        },
                        onSearchQueryChange = { query ->
                            viewModel.onEvent(ChannelsEvent.SearchChannels(query))
                        },
                        onInviteClick = {
                            viewModel.onEvent(ChannelsEvent.OpenServerInviteSheet)
                        },
                        onServerProfileClick = {
                            viewModel.onEvent(ChannelsEvent.OpenServerProfileDrawer)
                        },
                        onBackFromPreview = {
                            viewModel.onEvent(ChannelsEvent.ExitServerPreview)
                        },
                        onJoinServerClick = {
                            viewModel.onEvent(ChannelsEvent.JoinCurrentPreviewServer)
                        },
                        onAddChannelClick = { categoryName ->
                            viewModel.onEvent(ChannelsEvent.OpenCreateChannelSheet(categoryName))
                        }
                    )
                }
            }
        }

        // SERVER PROFILE BOTTOM SHEET DRAWER
        if (currentServer != null) {
            val currentMembers = uiState.serverMembers[currentServer.id].orEmpty()
            val currentMember = currentMembers.find { it.isSelf }
            val currentStudentId = uiState.studentProfile?.studentId?.ifBlank { uiState.studentProfile?.universityEmail }
            val isCurrentOwner = currentStudentId != null && currentStudentId.isNotBlank() && currentServer.ownerId == currentStudentId
            val isCurrentAdmin = isCurrentOwner || currentMember?.role?.roleType in listOf(ServerRoleType.OWNER, ServerRoleType.ADMIN)
            ServerProfileDrawer(
                isOpen = uiState.isServerProfileDrawerOpen,
                server = currentServer,
                userProfile = uiState.perServerProfiles[currentServer.id],
                members = currentMembers,
                isOwner = isCurrentOwner,
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseServerProfileDrawer)
                },
                onInviteClick = {
                    viewModel.onEvent(ChannelsEvent.OpenServerInviteSheet)
                },
                onCreateChannelClick = {
                    viewModel.onEvent(ChannelsEvent.OpenCreateChannelSheet())
                },
                onRulesClick = {
                    viewModel.onEvent(ChannelsEvent.OpenRulesSheet)
                },
                onEditServerSettingsClick = {
                    viewModel.onEvent(ChannelsEvent.OpenEditServerSettings)
                },
                onMarkAsReadClick = {
                    viewModel.onEvent(ChannelsEvent.ClearFeedback("All channels marked as read"))
                },
                onBrowseChannelsClick = {
                    viewModel.onEvent(ChannelsEvent.CloseServerProfileDrawer)
                },
                onEditProfileClick = {
                    viewModel.onEvent(ChannelsEvent.OpenEditServerProfile)
                },
                onMembersClick = {
                    viewModel.onEvent(ChannelsEvent.OpenServerMembersSheet(currentServer.id))
                },
                onLeaveServerClick = {
                    viewModel.onEvent(ChannelsEvent.LeaveServer(currentServer.id))
                },
                onDeleteServerClick = {
                    viewModel.onEvent(ChannelsEvent.DeleteServer(currentServer.id))
                },
                onNotificationSettingChange = { setting ->
                    viewModel.onEvent(ChannelsEvent.UpdateServerNotificationSetting(currentServer.id, setting))
                }
            )
        }

        // CREATE CHANNEL MODAL SHEET
        if (uiState.isCreateChannelSheetOpen && currentServer != null) {
            val existingCats = remember(currentServer.channels) {
                currentServer.channels.map { it.categoryName }.distinct().ifEmpty {
                    listOf("WELCOME", "TEXT CHANNELS", "DEVELOPMENT", "COMMUNITY", "STUDY JAMS")
                }
            }
            CreateChannelModalSheet(
                serverName = currentServer.name,
                initialCategory = uiState.targetChannelCategory,
                existingCategories = existingCats,
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseCreateChannelSheet)
                },
                onCreateChannel = { name, category, type, topic ->
                    viewModel.onEvent(
                        ChannelsEvent.CreateChannel(
                            serverId = currentServer.id,
                            name = name,
                            categoryName = category,
                            channelType = type,
                            topic = topic
                        )
                    )
                }
            )
        }

        // SERVER RULES MANAGEMENT SHEET
        if (uiState.isRulesSheetOpen && currentServer != null) {
            val currentMembers = uiState.serverMembers[currentServer.id].orEmpty()
            val currentMember = currentMembers.find { it.isSelf }
            val currentStudentId = uiState.studentProfile?.studentId?.ifBlank { uiState.studentProfile?.universityEmail }
            val isCurrentOwner = currentStudentId != null && currentStudentId.isNotBlank() && currentServer.ownerId == currentStudentId
            val isCurrentAdmin = isCurrentOwner || currentMember?.role?.roleType in listOf(ServerRoleType.OWNER, ServerRoleType.ADMIN)

            ServerRulesSheet(
                serverName = currentServer.name,
                rules = uiState.currentServerRules,
                canManageRules = isCurrentOwner || isCurrentAdmin,
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseRulesSheet)
                },
                onAddRule = { title, desc ->
                    viewModel.onEvent(
                        ChannelsEvent.CreateServerRule(
                            serverId = currentServer.id,
                            title = title,
                            description = desc
                        )
                    )
                },
                onDeleteRule = { ruleId ->
                    viewModel.onEvent(
                        ChannelsEvent.DeleteServerRule(
                            serverId = currentServer.id,
                            ruleId = ruleId
                        )
                    )
                }
            )
        }

        // SERVER INVITE SHEET
        if (uiState.isServerInviteSheetOpen && currentServer != null) {
            ServerInviteSheet(
                serverName = currentServer.name,
                inviteCode = uiState.currentServerInviteCode.ifEmpty { "${currentServer.name.uppercase().replace(Regex("[^A-Z0-9]"), "-").take(8)}-2026" },
                memberCountText = currentServer.memberCountText,
                avatarResId = getServerDrawableRes(currentServer.iconResName),
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseServerInviteSheet)
                },
                onCopiedFeedback = {
                    viewModel.onEvent(ChannelsEvent.ClearFeedback("Invite link copied to clipboard!"))
                }
            )
        }

        // EDIT PER-SERVER PROFILE SHEET
        if (uiState.isEditServerProfileOpen && currentServer != null) {
            EditServerProfileSheet(
                isOpen = uiState.isEditServerProfileOpen,
                serverId = currentServer.id,
                serverName = currentServer.name,
                initialProfile = uiState.perServerProfiles[currentServer.id],
                defaultNickname = uiState.activeUserName,
                avatarUrl = uiState.activeUserAvatar,
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseEditServerProfile)
                },
                onSaveProfile = { nick, tag, colorHex ->
                    viewModel.onEvent(
                        ChannelsEvent.SavePerServerProfile(
                            serverId = currentServer.id,
                            nickname = nick,
                            tag = tag,
                            tagColorHex = colorHex
                        )
                    )
                }
            )
        }

        // EDIT SERVER SETTINGS SHEET (Owner Only)
        if (uiState.isEditServerSettingsOpen && currentServer != null) {
            EditServerSettingsSheet(
                isOpen = uiState.isEditServerSettingsOpen,
                server = currentServer,
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseEditServerSettings)
                },
                onSaveSettings = { name, description, category, avatarResName, bannerResName ->
                    viewModel.onEvent(
                        ChannelsEvent.UpdateServerSettings(
                            serverId = currentServer.id,
                            name = name,
                            category = category,
                            description = description,
                            avatarResName = avatarResName,
                            bannerResName = bannerResName
                        )
                    )
                }
            )
        }

        // SERVER / CHANNEL MEMBERS SHEET
        if (uiState.isServerMembersSheetOpen && currentServer != null) {
            val serverMembers = uiState.serverMembers[currentServer.id].orEmpty()
            val filteredByChannel = if (uiState.currentMemberFilterChannelId != null) {
                serverMembers.filter { it.channels.contains(uiState.currentMemberFilterChannelId) }
            } else {
                serverMembers
            }
            val channelName = currentServer.channels.firstOrNull { it.id == uiState.currentMemberFilterChannelId }?.name
            ServerMembersSheet(
                isOpen = uiState.isServerMembersSheetOpen,
                serverName = currentServer.name,
                channelName = channelName,
                members = filteredByChannel,
                onDismiss = {
                    viewModel.onEvent(ChannelsEvent.CloseServerMembersSheet)
                },
                onMemberClick = { member ->
                    viewModel.onEvent(ChannelsEvent.OpenMemberProfile(member))
                }
            )
        }

        // FLOATING DRAWER COLLAPSE BUTTON (Right edge, vertically centered)
        if (!uiState.isServerProfileDrawerOpen && !uiState.isAddServerSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer subtle blue halo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0x33003B68))
                )
                // Inner navy blue button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF003B68))
                        .clickable {
                            viewModel.onEvent(ChannelsEvent.ToggleRail)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-6).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Collapse drawer",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // FLOATING USER PROFILE BAR (Bottom centered)
        if (!uiState.isServerProfileDrawerOpen) {
            UserProfileFloatingBar(
                name = uiState.activeUserName,
                avatarUrl = uiState.activeUserAvatar,
                status = uiState.activeUserStatus,
                unreadCount = uiState.activeUserUnreadCount,
                onProfileClick = onProfileClick,
                onNotificationClick = onNotificationClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ServerRail(
    servers: List<ServerItem>,
    selectedServerId: String,
    isAddServerSelected: Boolean,
    unreadDmCount: Int = 0,
    onNavigateToMessages: () -> Unit,
    onSelectServer: (String) -> Unit,
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFE3E5E8))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top DM / Messages Chat Icon (Matching reference image)
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD1D5DB))
                    .clickable(onClick = onNavigateToMessages),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Direct Messages",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (unreadDmCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFDA373C))
                        .border(2.dp, Color(0xFFE3E5E8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = if (unreadDmCount > 99) "99+" else unreadDmCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Gray divider
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .width(32.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFFD1D5DB))
        )

        // Scrollable Server List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                val isSelected = !isAddServerSelected && (server.id == selectedServerId)
                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 16.dp else 24.dp,
                    label = "ServerCornerRadius"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left edge indicator pill
                    val indicatorHeight by animateDpAsState(
                        targetValue = when {
                            isSelected -> 36.dp
                            server.hasUnread -> 8.dp
                            else -> 0.dp
                        },
                        label = "IndicatorHeight"
                    )

                    if (indicatorHeight > 0.dp) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(4.dp)
                                .height(indicatorHeight)
                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .background(Color.White)
                        )
                    }

                    // Server Icon Box with Red Badge
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (server.iconResName.startsWith("http") || server.iconResName.startsWith("content:")) {
                            coil.compose.AsyncImage(
                                model = server.iconResName,
                                contentDescription = server.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(cornerRadius))
                                    .clickable { onSelectServer(server.id) }
                            )
                        } else {
                            Image(
                                painter = painterResource(getServerDrawableRes(server.iconResName)),
                                contentDescription = server.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(cornerRadius))
                                    .clickable { onSelectServer(server.id) }
                            )
                        }

                        // Red mention badge at bottom right
                        if (server.badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 5.dp, y = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFDA373C))
                                    .border(2.dp, Color(0xFFE3E5E8), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "${server.badgeCount}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Add Server Button '+'
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val addCornerRadius by animateDpAsState(
                    targetValue = if (isAddServerSelected) 16.dp else 24.dp,
                    label = "AddCornerRadius"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAddServerSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .background(Color(0xFF1E1F22))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(addCornerRadius))
                            .background(if (isAddServerSelected) Color(0xFF5865F2) else Color(0xFFEBEDEF))
                            .clickable(onClick = onAddServer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add a server",
                            tint = if (isAddServerSelected) Color.White else Color(0xFF23A55A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerDetailsPanel(
    server: ServerItem,
    selectedChannelId: String,
    isOwner: Boolean = false,
    isAdmin: Boolean = false,
    isMember: Boolean = false,
    isJoining: Boolean = false,
    onChannelClick: (ServerChannelItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onInviteClick: () -> Unit,
    onServerProfileClick: () -> Unit,
    onBackFromPreview: () -> Unit = {},
    onJoinServerClick: () -> Unit = {},
    onAddChannelClick: (categoryName: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var collapsedCategories by remember { mutableStateOf(setOf<String>()) }

    val bannerResId: Int? = when (server.bannerResName) {
        "server_banner_clash" -> com.campus.network.core.ui.R.drawable.server_banner_clash
        "server_banner_basi" -> com.campus.network.core.ui.R.drawable.server_banner_basi
        "banner_gradient_cyber" -> com.campus.network.core.ui.R.drawable.banner_gradient_cyber
        "banner_campus_quad" -> com.campus.network.core.ui.R.drawable.banner_campus_quad
        "banner_tech_neon" -> com.campus.network.core.ui.R.drawable.banner_tech_neon
        "banner_welcome_perk" -> com.campus.network.core.ui.R.drawable.banner_welcome_perk
        else -> null
    }
    val isBannerUrl = server.bannerResName?.startsWith("http") == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. OPTIONAL SERVER BANNER (Rendered if server owner created a banner)
        if (isBannerUrl || bannerResId != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp))
            ) {
                if (isBannerUrl) {
                    coil.compose.AsyncImage(
                        model = server.bannerResName,
                        contentDescription = "Server Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (bannerResId != null) {
                    Image(
                        painter = painterResource(id = bannerResId),
                        contentDescription = "Server Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // If in Preview Mode (Matching Image 1): overlay back button and SERVER PREVIEW badge (only if non-member and non-owner)
                if (server.isPreview && !isOwner && !isMember) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onBackFromPreview,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "SERVER PREVIEW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E1F22),
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. SERVER HEADER: Title, Badges, Member Count & Category, Search Row, Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(enabled = !server.isPreview || isOwner || isMember, onClick = onServerProfileClick)
            ) {
                Text(
                    text = server.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF060607),
                    letterSpacing = (-0.3).sp
                )

                // Flower booster badge or verified badge
                if (server.badgeType == "booster") {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE879F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Booster Badge",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else if (server.isVerified) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5865F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Server",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF747F8D),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${server.memberCountText} \u2022 ${server.categoryText}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5C6470)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar + Add Person Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F3F5))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF5C6470),
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearchQueryChange(it)
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF1E1F22),
                                fontWeight = FontWeight.Normal
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        fontSize = 14.sp,
                                        color = Color(0xFF747F8D)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Add Member / Invite Button (only if not previewing, or if member/owner)
                if (!server.isPreview || isOwner || isMember) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF2F3F5))
                            .clickable(onClick = onInviteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Invite Members",
                            tint = Color(0xFF4E5058),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Preview Mode: Solid blurple "Join Server" button (Exact match to Preview button, only if not owner and not member)
            if (server.isPreview && !isOwner && !isMember) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onJoinServerClick,
                    enabled = !isJoining,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isJoining) {
                        com.campus.network.core.ui.components.DiscordBouncingDotsLoader(
                            dotSize = 6.dp,
                            dotSpacing = 5.dp,
                            dotColor = Color.White
                        )
                    } else {
                        Text(
                            text = "Join Server",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (server.boostGoalText != null) {
                // Joined Server with Banner: "Boost Goal" Card (Matching Image 2 BASI)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF7EEFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Boost Goal",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1F22)
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "${server.boostGoalText} >",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9333EA)
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF2F3F5))

        // 3. SERVER GUIDE & CHANNELS & ROLES (Matching Image 1 & 2)
        if (server.hasServerGuide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Server Guide */ }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Signpost,
                    contentDescription = "Server Guide",
                    tint = Color(0xFF4E5058),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Server Guide",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4E5058)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Channels & Roles */ }
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Channels & Roles",
                tint = Color(0xFF4E5058),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Channels & Roles",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E5058)
            )
        }

        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF2F3F5))

        val networkState = rememberCampusNetworkState()
        var isChannelsSyncing by remember(server.id) { mutableStateOf(true) }

        LaunchedEffect(server.id, networkState.isOnline) {
            if (networkState.isOnline) {
                isChannelsSyncing = true
                delay(if (networkState.isSlowNetwork) 680L else 380L)
                isChannelsSyncing = false
            } else {
                isChannelsSyncing = false
            }
        }

        if (!networkState.isOnline) {
            CampusInlineNetworkAlertCard(
                featureName = "Server Channels",
                description = "You can browse cached channels in ${server.name}. Live messages will sync once reconnected.",
                actionLabel = "Reconnect channels",
                onActionClick = { networkState.triggerRetry() },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        // 4. CHANNELS LIST GROUPED BY CATEGORY
        val distinctChannels = remember(server.channels) {
            server.channels.distinctBy { it.id }
        }
        val channelsByCategory = remember(distinctChannels) {
            distinctChannels.groupBy { it.categoryName }
        }

        if (isChannelsSyncing) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp)
            ) {
                items(6) {
                    MessageThreadRowSkeleton()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp)
            ) {
                channelsByCategory.forEach { (categoryName, channelsInCategory) ->
                    val isCollapsed = collapsedCategories.contains(categoryName)
                    item(key = "cat_$categoryName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    collapsedCategories = if (isCollapsed) {
                                        collapsedCategories - categoryName
                                    } else {
                                        collapsedCategories + categoryName
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCollapsed) "Expand $categoryName" else "Collapse $categoryName",
                                tint = Color(0xFF5C6470),
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (isCollapsed) -90f else 0f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = categoryName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C6470),
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isOwner || isAdmin) {
                                IconButton(
                                    onClick = { onAddChannelClick(categoryName) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Channel",
                                        tint = Color(0xFF5C6470),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!isCollapsed) {
                        items(channelsInCategory, key = { "${categoryName}_${it.id}" }) { channel ->
                            val isCurrentSelected = channel.id == selectedChannelId || channel.isSelected
                            val isProminent = channel.isUnread || isCurrentSelected

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrentSelected) Color(0xFFE3E5E8) else Color.Transparent)
                                    .clickable {
                                        onChannelClick(channel)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    when (channel.channelType) {
                                        "rules" -> {
                                            Icon(
                                                imageVector = Icons.Default.AssignmentTurnedIn,
                                                contentDescription = "Rules",
                                                tint = if (isProminent) Color(0xFF1E1F22) else Color(0xFF80848E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        "announcements" -> {
                                            Icon(
                                                imageVector = Icons.Default.Campaign,
                                                contentDescription = "Announcements",
                                                tint = if (isProminent) Color(0xFF1E1F22) else Color(0xFF80848E),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "#",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isProminent) Color(0xFF1E1F22) else Color(0xFF80848E)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(9.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.name,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isProminent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isProminent) Color(0xFF1E1F22) else Color(0xFF5C6470),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!channel.typingLabel.isNullOrBlank()) {
                                            Text(
                                                text = channel.typingLabel,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF23A559),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Per-channel thread unread badge (real count only)
                                    val channelUnread = channel.unreadBadgeCount.coerceAtLeast(
                                        if (channel.isUnread) 1 else 0
                                    )
                                    if (channelUnread > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFDA373C))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (channelUnread > 99) "99+" else "$channelUnread",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else if (channel.badgeText != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFDA373C))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = channel.badgeText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    if (channel.hasBoltIcon) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Special",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
        }
    }
}

@Composable
private fun UserProfileFloatingBar(
    name: String,
    avatarUrl: String?,
    status: String,
    unreadCount: Int,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable(onClick = onProfileClick),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE3E5E8)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar with Online Dot
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                com.campus.network.core.ui.components.CampusStudentAvatar(
                    avatarUrl = avatarUrl,
                    fullName = name,
                    modifier = Modifier.fillMaxSize()
                )

                // Green online status dot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF23A55A))
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // User Name and Status
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22)
                    )

                    // Status Pill with Mascot & OvO (matching reference image)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE9EBEE))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF7A4A28),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "OvO",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = status,
                    fontSize = 12.sp,
                    color = Color(0xFF5C6470)
                )
            }

            // Bell Notification Icon with Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF4E5058),
                    modifier = Modifier.size(22.dp)
                )

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-2).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDA373C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$unreadCount",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun getServerDrawableRes(name: String): Int {
    return when (name) {
        "server_home_cluster" -> com.campus.network.core.ui.R.drawable.server_home_cluster
        "server_codesistency" -> com.campus.network.core.ui.R.drawable.server_codesistency
        "server_avatar2" -> com.campus.network.core.ui.R.drawable.server_avatar2
        "server_w_red" -> com.campus.network.core.ui.R.drawable.server_w_red
        "server_gradient" -> com.campus.network.core.ui.R.drawable.server_gradient
        "server_ai_brain" -> com.campus.network.core.ui.R.drawable.server_ai_brain
        "server_logo2" -> com.campus.network.core.ui.R.drawable.server_logo2
        "server_dragon" -> com.campus.network.core.ui.R.drawable.server_dragon
        "server_curly" -> com.campus.network.core.ui.R.drawable.server_curly
        "server_eureka" -> com.campus.network.core.ui.R.drawable.server_eureka
        "server_nvidia" -> com.campus.network.core.ui.R.drawable.server_nvidia
        "server_owl" -> com.campus.network.core.ui.R.drawable.server_owl
        else -> com.campus.network.core.ui.R.drawable.server_codesistency
    }
}

/**
 * Main Academic Workspace & Channels Hub:
 * Replicates the reference design layout with faculty switching,
 * course listings with course-mate avatar lineups, and recruitment pipeline.
 */
@Composable
private fun ChannelsHubContent(
    uiState: ChannelsUiState,
    viewModel: ChannelsViewModel,
    onBackClick: () -> Unit,
    onOpenDiscovery: (CareerCourse) -> Unit,
    onCourseClick: (CareerCourse) -> Unit,
    onCustomFilterClick: () -> Unit,
    onCompanyClick: (InDemandCompany) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 1. Top Bar: Back Arrow + Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryNavy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Academic Workspace",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryNavy,
                letterSpacing = (-0.3).sp
            )
        }

        // Top Gold Accent Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BrandGold)
        )

        // 2. Faculty Switcher Pill Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.faculties.forEach { faculty ->
                val isSelected = faculty == uiState.selectedFaculty
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) TextPrimaryNavy else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) TextPrimaryNavy else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.clickable {
                        viewModel.onEvent(ChannelsEvent.SelectFaculty(faculty))
                    }
                ) {
                    Text(
                        text = if (faculty.contains("Engineering")) "Engineering & Tech" else faculty.removePrefix("Faculty of ").removePrefix("School of "),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // SECTION 1: Registered Courses & Course-Mates
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Title with Edit Circle Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registered courses & course-mates",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryNavy,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Circular Filter Button
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            onClick = onCustomFilterClick
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Filter registered courses",
                                    tint = TextPrimaryNavy,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle
                    Text(
                        text = "Connect and study with peers in your enrolled faculty courses",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondarySlate
                    )


                    Spacer(modifier = Modifier.height(12.dp))

                    // Sub-Category / Department Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.departments.forEach { dept ->
                            val isDeptSelected = dept == uiState.selectedDepartment
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDeptSelected) Color(0xFFE0F2FE) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDeptSelected) LinkBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.onEvent(ChannelsEvent.SelectDepartment(dept))
                                }
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 12.sp,
                                    fontWeight = if (isDeptSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDeptSelected) LinkBlue else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Academic Workspace Helper Reward Banner
                    AcademicWorkspaceHelperCard(
                        onOpenDiscoveryFirstCourse = {
                            uiState.courses.firstOrNull()?.let { onOpenDiscovery(it) }
                        }
                    )
                }
            }

            // Course Item Rows with Live Avatar Lineup
            items(uiState.courses, key = { it.id }) { course ->
                CourseItemRow(
                    course = course,
                    onClick = { onCourseClick(course) },
                    onOpenDiscovery = { onOpenDiscovery(course) }
                )
                HorizontalDivider(
                    color = Color(0xFFF1F5F9),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Expand / Collapse Action: "Show all 9 courses"
            item {
                val isExpanded = uiState.isCoursesExpanded
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.onEvent(ChannelsEvent.ToggleCoursesExpanded)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isExpanded) "Show less" else "Show all 9 courses",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LinkBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = LinkBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // SECTION SEPARATOR
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(SectionDividerBg)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(BrandGold)
                    )
                }
            }

            // SECTION 2: Companies Where This Role Is In Demand
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Companies where this role is in demand",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryNavy,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Based on new job postings in the past 7 days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondarySlate
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Company Cards
                    uiState.companies.forEach { company ->
                        CompanyDemandItem(
                            company = company,
                            onClick = { onCompanyClick(company) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Academic Workspace Helper card highlighting the peer reward and study buddy incentive system.
 */
@Composable
private fun AcademicWorkspaceHelperCard(
    onOpenDiscoveryFirstCourse: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDiscoveryFirstCourse),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF08A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Academic Workspace Helper",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = "14 Enrolled Helpers",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LinkBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Match with course-mates taking your exact registered courses. Help peers with problem sets to earn karma points and verified Helper endorsements.",
                fontSize = 13.sp,
                color = TextSecondarySlate,
                lineHeight = 18.5.sp
            )
        }
    }
}

/**
 * Single Course Item matching the reference design layout:
 * Left 16:9 thumbnail + Right title, subtitle ("Course · 30m 25s"), viewer count,
 * AND live CourseMateAvatarLineup (o o o o +14 more) for peer matching.
 */
@Composable
private fun CourseItemRow(
    course: CareerCourse,
    onClick: () -> Unit,
    onOpenDiscovery: () -> Unit
) {
    val thumbnailRes = getCourseThumbnailDrawable(course.thumbnailResName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 16:9 Thumbnail
            Image(
                painter = painterResource(thumbnailRes),
                contentDescription = course.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(114.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Right Text Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryNavy,
                    lineHeight = 20.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Text(
                            text = course.courseCode,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LinkBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = course.department,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondarySlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${course.enrolledMatesCount} classmates enrolled",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondarySlate
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Course-Mate Lineup (o o o o +14 more)
        CourseMateAvatarLineup(
            course = course,
            onMoreClick = onOpenDiscovery,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Company where role is in demand row:
 * Logo (DataAnnotation DA logo or initials) + Name + Posts + Trend + Arrow.
 */
@Composable
private fun CompanyDemandItem(
    company: InDemandCompany,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company Logo
            if (company.logoResName == "company_da") {
                Image(
                    painter = painterResource(R.drawable.company_da),
                    contentDescription = company.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = company.initials,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Name & Posts
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.name,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = company.jobPostsCountText,
                    fontSize = 13.5.sp,
                    color = TextSecondarySlate
                )
            }

            // Right: Trend Metric & Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = company.demandMetricText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (company.isPositiveTrend) Color(0xFF16A34A) else Color(0xFF475569)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Resolves course thumbnail drawable.
 */
private fun getCourseThumbnailDrawable(name: String): Int {
    return when (name) {
        "course_thumb_1" -> R.drawable.course_thumb_1
        "course_thumb_2" -> R.drawable.course_thumb_2
        "course_thumb_3" -> R.drawable.course_thumb_3
        "course_thumb_4" -> R.drawable.course_thumb_4
        "course_thumb_5" -> R.drawable.course_thumb_5
        "course_thumb_6" -> R.drawable.course_thumb_6
        else -> R.drawable.course_thumb_1
    }
}
