package com.campus.network.feature.channels.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.feature.channels.ChannelsEvent
import com.campus.network.feature.channels.ChannelsUiState
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.channels.model.ServerMemberItem
import com.campus.network.feature.channels.model.ServerRoleType

/**
 * Pixel-Perfect Channel Profile / Details Screen matching uploaded_media_1792143197305.png
 * Features:
 * - Top App Bar: Back arrow, Search, Notifications, Settings
 * - Channel Header: Squircle Hash icon, channel name, 'Text Channel' subtitle
 * - Channel Topic / Description: 'A place where people who want to make music for the game are free to post their work!'
 * - Horizontal Tabs: Members, Media, Pins, Threads, Links, Files with blue active underline indicator
 * - Members tab: 'Invite Members' card button, Online & Offline member roster
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography across all UI.
 */
@Composable
fun ChannelProfileScreen(
    uiState: ChannelsUiState,
    viewModel: ChannelsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = uiState.activeChatChannel
    val channelName = activeChannel?.name ?: "game-music"
    var selectedTab by remember { mutableStateOf(uiState.activeChannelProfileTab) }

    BackHandler {
        onBackClick()
    }

    val channelDescription = when (channelName) {
        "game-music" -> "A place where people who want to make music for the game are free to post their work!"
        "role-subscriptions" -> "Assign and manage server roles, notifications, and contributor access."
        "poll-chat" -> "Community polls, soundtrack voting, and release discussions."
        "redis-chat-app" -> "Redis real-time pub/sub streams, socket performance benchmarks, and cluster tuning."
        "mern-github-app" -> "Full-stack GraphQL APIs, React state management, and pull request reviews."
        else -> "A dedicated campus channel for collaboration, discussion, and project sharing."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. TOP APP BAR (Back, Search, Notifications, Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { /* Search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = { /* Notifications */ }) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = { /* Settings */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Channel Settings",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. CHANNEL SQUIRCLE ICON & TITLE SECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Squircle Hash Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F3F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )
            }

            // Channel Name & Subtitle
            Column {
                Text(
                    text = channelName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )
                Text(
                    text = "Text Channel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF5C6470)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. CHANNEL TOPIC / DESCRIPTION
        Text(
            text = channelDescription,
            fontSize = 14.5.sp,
            color = Color(0xFF4E5058),
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. HORIZONTAL TABS (Members, Media, Pins, Threads, Links, Files)
        val tabList = listOf("Members", "Media", "Pins", "Threads", "Links", "Files")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            tabList.forEach { tabName ->
                val isSelected = selectedTab == tabName
                Column(
                    modifier = Modifier
                        .clickable {
                            selectedTab = tabName
                            viewModel.onEvent(ChannelsEvent.SelectChannelProfileTab(tabName))
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabName,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF5865F2) else Color(0xFF5C6470)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Active indicator bar
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(if (isSelected) 54.dp else 0.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) Color(0xFF5865F2) else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

        // 5. TAB CONTENT
        when (selectedTab) {
            "Members" -> {
                val currentServerId = uiState.activeChatServer?.id ?: uiState.selectedServerId
                val serverMemberList = uiState.serverMembers[currentServerId].orEmpty()
                MembersTabContent(
                    channelName = channelName,
                    members = serverMemberList,
                    onMemberClick = { member ->
                        viewModel.onEvent(ChannelsEvent.OpenMemberProfile(member))
                    },
                    onInviteClick = {
                        viewModel.onEvent(ChannelsEvent.OpenServerInviteSheet)
                    }
                )
            }
            "Media" -> {
                MediaTabContent()
            }
            "Pins" -> {
                PinsTabContent()
            }
            "Threads" -> {
                ThreadsTabContent(channelName = channelName)
            }
            "Links" -> {
                LinksTabContent()
            }
            "Files" -> {
                FilesTabContent()
            }
            else -> {
                val currentServerId = uiState.activeChatServer?.id ?: uiState.selectedServerId
                val serverMemberList = uiState.serverMembers[currentServerId].orEmpty()
                MembersTabContent(
                    channelName = channelName,
                    members = serverMemberList,
                    onMemberClick = { member ->
                        viewModel.onEvent(ChannelsEvent.OpenMemberProfile(member))
                    },
                    onInviteClick = {
                        viewModel.onEvent(ChannelsEvent.OpenServerInviteSheet)
                    }
                )
            }
        }
    }
}

/**
 * Dynamic Members tab matching reference layout.
 * Top card: 'Invite Members' with user-plus icon and chevron
 * Followed by categorized members by Server Role Hierarchy.
 */
@Composable
private fun MembersTabContent(
    channelName: String,
    members: List<ServerMemberItem>,
    onMemberClick: (ServerMemberItem) -> Unit,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channelMembers = members.filter { it.channels.isEmpty() || it.channels.contains(channelName) }
    val owners = channelMembers.filter { it.role.roleType == ServerRoleType.OWNER }
    val admins = channelMembers.filter { it.role.roleType == ServerRoleType.ADMIN || it.role.roleType == ServerRoleType.MODERATOR }
    val contributors = channelMembers.filter { it.role.roleType == ServerRoleType.CONTRIBUTOR }
    val onlineMembers = channelMembers.filter { it.role.roleType == ServerRoleType.MEMBER && it.isOnline }
    val offlineMembers = channelMembers.filter { it.role.roleType == ServerRoleType.MEMBER && !it.isOnline }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Invite Members Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onInviteClick),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Invite Members",
                        tint = Color(0xFF1E1F22),
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Invite Members",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1F22),
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (owners.isNotEmpty()) {
            item {
                Text(
                    text = "SERVER OWNER — ${owners.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(owners, key = { "cp_owner_${it.id}" }) { member ->
                ServerMemberRow(member = member, onClick = { onMemberClick(member) })
            }
        }

        if (admins.isNotEmpty()) {
            item {
                Text(
                    text = "ADMINS & MODS — ${admins.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(admins, key = { "cp_admin_${it.id}" }) { member ->
                ServerMemberRow(member = member, onClick = { onMemberClick(member) })
            }
        }

        if (contributors.isNotEmpty()) {
            item {
                Text(
                    text = "CONTRIBUTORS — ${contributors.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(contributors, key = { "cp_contrib_${it.id}" }) { member ->
                ServerMemberRow(member = member, onClick = { onMemberClick(member) })
            }
        }

        if (onlineMembers.isNotEmpty()) {
            item {
                Text(
                    text = "ONLINE — ${onlineMembers.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(onlineMembers, key = { "cp_online_${it.id}" }) { member ->
                ServerMemberRow(member = member, onClick = { onMemberClick(member) })
            }
        }

        if (offlineMembers.isNotEmpty()) {
            item {
                Text(
                    text = "OFFLINE — ${offlineMembers.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                )
            }
            items(offlineMembers, key = { "cp_offline_${it.id}" }) { member ->
                ServerMemberRow(member = member, onClick = { onMemberClick(member) })
            }
        }
    }
}

@Composable
private fun MediaTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Shared Media (14)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F22)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(com.campus.network.core.ui.R.drawable.place_photo_1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Image(
                painter = painterResource(com.campus.network.core.ui.R.drawable.place_photo_2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Image(
                painter = painterResource(com.campus.network.core.ui.R.drawable.cat_tech_gear),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun PinsTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = Color(0xFF5865F2),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Pinned Messages",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F22)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, Color(0xFFE3E5E8))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "RobLoach · 03/11/2026",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF23A55A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Official Civ VII Soundtrack & Unciv Sound Engine mod instructions are pinned here.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF313338)
                )
            }
        }
    }
}

@Composable
private fun ThreadsTabContent(channelName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = Color(0xFF5865F2),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Active Threads",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F22)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, Color(0xFFE3E5E8))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Tame Impala vs Hans Zimmer Civ Themes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Started by Ath · 18 messages · Active 2h ago",
                    fontSize = 12.sp,
                    color = Color(0xFF5C6470)
                )
            }
        }
    }
}

@Composable
private fun LinksTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = Color(0xFF5865F2),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Shared Links",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F22)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, Color(0xFFE3E5E8))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "https://github.com/Kurwizimi/Civ-VII-Music-Pack",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0066CC)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "GitHub repository for Civ VII sound pack integration into UnCiv.",
                    fontSize = 12.sp,
                    color = Color(0xFF5C6470)
                )
            }
        }
    }
}

@Composable
private fun FilesTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF5865F2),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Shared Files",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F22)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, Color(0xFFE3E5E8))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF5C6470),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Civ-VII-Music-Pack-v2.1.zip",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1F22)
                    )
                    Text(
                        text = "42.5 MB · Uploaded by RobLoach",
                        fontSize = 12.sp,
                        color = Color(0xFF80848E)
                    )
                }
            }
        }
    }
}
