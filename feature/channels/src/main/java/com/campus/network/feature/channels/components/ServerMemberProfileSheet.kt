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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.feature.channels.model.FriendRequestStatus
import com.campus.network.feature.channels.model.ServerMemberItem
import com.campus.network.feature.channels.model.ServerRoleType

/**
 * Pixel-Perfect Server Member Profile Card & Identity Sheet.
 * Features:
 * - Server Banner Header & Avatar with online presence dot
 * - Member Identification: Display Name, Per-server Nickname, Handle, Server Tag
 * - Server Role Badges: Owner (Gold Star), Admin/Mod (Shield), Contributor, Member
 * - In-Server Friend Request Button (Add Friend / Request Sent / Friends)
 * - Direct Chat Routing button (navigates to Direct Messages)
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerMemberProfileSheet(
    member: ServerMemberItem?,
    onDismiss: () -> Unit,
    onSendFriendRequest: (memberId: String) -> Unit,
    onCancelFriendRequest: (memberId: String) -> Unit,
    onAcceptFriendRequest: (memberId: String) -> Unit = {},
    onDeclineFriendRequest: (memberId: String) -> Unit = {},
    onDirectMessage: (member: ServerMemberItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (member == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. TOP BANNER (Header with gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(member.role.colorHex).copy(alpha = 0.85f),
                                Color(0xFF1E1F22)
                            )
                        )
                    )
            ) {
                // Close button in top-right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. AVATAR & PRESENCE (Overlapping banner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-36).dp)
            ) {
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFE3E5E8))
                            .border(3.dp, Color.White, RoundedCornerShape(22.dp)),
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
                            val resId = resolveMemberAvatarDrawable(member.avatarResName)
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = member.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Online Status Dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (member.isOnline) Color(0xFF23A55A) else Color(0xFF80848E))
                            .border(2.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // 3. MEMBER IDENTITY DETAILS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .padding(horizontal = 20.dp)
            ) {
                // Name & Server Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.serverNickname ?: member.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E1F22),
                        letterSpacing = (-0.3).sp
                    )

                    // Per-Server Tag
                    if (!member.serverTag.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(member.serverTagColorHex).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(member.serverTagColorHex)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = Color(member.serverTagColorHex),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = member.serverTag.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(member.serverTagColorHex)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Handle (@username)
                Text(
                    text = member.handle,
                    fontSize = 14.sp,
                    color = Color(0xFF5C6470),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Server Role Badge
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color(member.role.colorHex).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(member.role.colorHex).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val roleIcon = when (member.role.roleType) {
                            ServerRoleType.OWNER -> Icons.Default.Star
                            ServerRoleType.ADMIN, ServerRoleType.MODERATOR -> Icons.Default.Shield
                            ServerRoleType.CONTRIBUTOR -> Icons.Default.MilitaryTech
                            ServerRoleType.MEMBER -> Icons.Default.Eco
                        }
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = Color(member.role.colorHex),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = member.role.name,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(member.role.colorHex)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ACTION BUTTONS ROW (Friend Request & Direct Message)
                val isSelf = member.name.contains("Lincoln", ignoreCase = true)

                if (!isSelf) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // In-Server Friend Request Button
                        when (member.friendStatus) {
                            FriendRequestStatus.NONE -> {
                                Button(
                                    onClick = { onSendFriendRequest(member.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Friend",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            FriendRequestStatus.REQUEST_SENT -> {
                                OutlinedButton(
                                    onClick = { onCancelFriendRequest(member.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5C6470)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3E5E8)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF5865F2),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Request Sent",
                                        color = Color(0xFF1E1F22),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            FriendRequestStatus.REQUEST_RECEIVED -> {
                                Button(
                                    onClick = { onAcceptFriendRequest(member.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23A55A)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Accept",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onDeclineFriendRequest(member.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text(
                                        text = "Decline",
                                        color = Color(0xFFE11D48),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            FriendRequestStatus.FRIENDS -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF23A55A).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF23A55A)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF23A55A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Friends",
                                            color = Color(0xFF23A55A),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Direct Message Button -> Routes directly into Direct Chat
                        Button(
                            onClick = {
                                onDirectMessage(member)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F3F5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFF1E1F22),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Message",
                                color = Color(0xFF1E1F22),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                Spacer(modifier = Modifier.height(14.dp))

                // ABOUT ME & BIO
                Text(
                    text = "ABOUT ME",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = member.bio,
                    fontSize = 14.sp,
                    color = Color(0xFF1E1F22),
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // MEMBER SINCE
                Text(
                    text = "SERVER MEMBER SINCE",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6470),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = member.joinedDate,
                    fontSize = 13.5.sp,
                    color = Color(0xFF1E1F22)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun resolveMemberAvatarDrawable(name: String?): Int {
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
