package com.campus.network.feature.channels.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.feature.channels.model.FriendRequestStatus
import com.campus.network.feature.channels.model.ServerMemberItem
import com.campus.network.feature.channels.model.ServerRoleType

/**
 * Pixel-Perfect Server / Channel Members Roster Sheet.
 * Displays all members categorized by Server Role Hierarchy:
 * - SERVER OWNER
 * - ADMINS & MODERATORS
 * - CORE CONTRIBUTORS
 * - ONLINE MEMBERS
 * - OFFLINE
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerMembersSheet(
    isOpen: Boolean,
    serverName: String,
    channelName: String? = null,
    members: List<ServerMemberItem>,
    onDismiss: () -> Unit,
    onMemberClick: (member: ServerMemberItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = members.filter { member ->
        val matchesQuery = searchQuery.isBlank() ||
                member.name.contains(searchQuery, ignoreCase = true) ||
                (member.serverNickname?.contains(searchQuery, ignoreCase = true) == true) ||
                member.handle.contains(searchQuery, ignoreCase = true) ||
                (member.serverTag?.contains(searchQuery, ignoreCase = true) == true)

        val matchesChannel = channelName == null || member.channels.isEmpty() || member.channels.contains(channelName)
        matchesQuery && matchesChannel
    }

    val owners = filteredMembers.filter { it.role.roleType == ServerRoleType.OWNER }
    val admins = filteredMembers.filter { it.role.roleType == ServerRoleType.ADMIN || it.role.roleType == ServerRoleType.MODERATOR }
    val contributors = filteredMembers.filter { it.role.roleType == ServerRoleType.CONTRIBUTOR }
    val onlineMembers = filteredMembers.filter { it.role.roleType == ServerRoleType.MEMBER && it.isOnline }
    val offlineMembers = filteredMembers.filter { it.role.roleType == ServerRoleType.MEMBER && !it.isOnline }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (channelName != null) "#$channelName Members" else "$serverName Members",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22)
                    )
                    Text(
                        text = "${filteredMembers.size} members in roster",
                        fontSize = 12.5.sp,
                        color = Color(0xFF5C6470)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF5C6470),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Categorized List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (owners.isNotEmpty()) {
                    item {
                        MemberSectionHeader(title = "SERVER OWNER — ${owners.size}")
                    }
                    items(owners, key = { "owner_${it.id}" }) { member ->
                        ServerMemberRow(member = member, onClick = { onMemberClick(member) })
                    }
                }

                if (admins.isNotEmpty()) {
                    item {
                        MemberSectionHeader(title = "ADMINS & MODS — ${admins.size}")
                    }
                    items(admins, key = { "admin_${it.id}" }) { member ->
                        ServerMemberRow(member = member, onClick = { onMemberClick(member) })
                    }
                }

                if (contributors.isNotEmpty()) {
                    item {
                        MemberSectionHeader(title = "CONTRIBUTORS — ${contributors.size}")
                    }
                    items(contributors, key = { "contrib_${it.id}" }) { member ->
                        ServerMemberRow(member = member, onClick = { onMemberClick(member) })
                    }
                }

                if (onlineMembers.isNotEmpty()) {
                    item {
                        MemberSectionHeader(title = "ONLINE — ${onlineMembers.size}")
                    }
                    items(onlineMembers, key = { "online_${it.id}" }) { member ->
                        ServerMemberRow(member = member, onClick = { onMemberClick(member) })
                    }
                }

                if (offlineMembers.isNotEmpty()) {
                    item {
                        MemberSectionHeader(title = "OFFLINE — ${offlineMembers.size}")
                    }
                    items(offlineMembers, key = { "offline_${it.id}" }) { member ->
                        ServerMemberRow(member = member, onClick = { onMemberClick(member) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF5C6470),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun ServerMemberRow(
    member: ServerMemberItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with Presence indicator
        Box(modifier = Modifier.size(38.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3E5E8)),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUrl?.startsWith("http") == true) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val resId = resolveMemberRowAvatarDrawable(member.avatarResName)
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Status Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(if (member.isOnline) Color(0xFF23A55A) else Color(0xFF80848E))
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        // Name, Tag, Role Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = member.serverNickname ?: member.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(member.role.colorHex)
                )

                // Server Tag
                if (!member.serverTag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(member.serverTagColorHex).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(member.serverTagColorHex))
                    ) {
                        Text(
                            text = member.serverTag.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(member.serverTagColorHex),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Hierarchy Icon for Owner / Mod
                when (member.role.roleType) {
                    ServerRoleType.OWNER -> {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Server Owner",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    ServerRoleType.ADMIN, ServerRoleType.MODERATOR -> {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Moderator",
                            tint = Color(0xFF23A55A),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    ServerRoleType.CONTRIBUTOR -> {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = "Contributor",
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    else -> Unit
                }
            }

            Text(
                text = member.role.name,
                fontSize = 12.sp,
                color = Color(0xFF5C6470),
                fontWeight = FontWeight.Normal
            )
        }

        // Friend Status badge
        when (member.friendStatus) {
            FriendRequestStatus.FRIENDS -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Friends",
                    tint = Color(0xFF23A55A),
                    modifier = Modifier.size(16.dp)
                )
            }
            FriendRequestStatus.REQUEST_SENT -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Request Sent",
                    tint = Color(0xFF5865F2),
                    modifier = Modifier.size(16.dp)
                )
            }
            else -> Unit
        }
    }
}

private fun resolveMemberRowAvatarDrawable(name: String?): Int {
    return when (name) {
        "avatar_lincoln" -> com.campus.network.core.ui.R.drawable.avatar_lincoln
        "avatar_ath" -> com.campus.network.core.ui.R.drawable.avatar_ath
        "server_avatar2" -> com.campus.network.core.ui.R.drawable.server_avatar2
        "server_dragon" -> com.campus.network.core.ui.R.drawable.server_dragon
        "server_eureka" -> com.campus.network.core.ui.R.drawable.server_eureka
        "server_curly" -> com.campus.network.core.ui.R.drawable.server_curly
        "server_owl" -> com.campus.network.core.ui.R.drawable.server_owl
        "server_codesistency" -> com.campus.network.core.ui.R.drawable.server_codesistency
        "avatar_deleted_user" -> com.campus.network.core.ui.R.drawable.avatar_deleted_user
        else -> com.campus.network.core.ui.R.drawable.server_avatar2
    }
}
