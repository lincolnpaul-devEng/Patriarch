package com.campus.network.feature.channels.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.feature.channels.ServerItem

/**
 * Pixel-Perfect Server Profile Bottom Sheet Drawer matching:
 * - uploaded_media_0_1792144247758.png
 * - uploaded_media_1_1792144247758.png
 *
 * Features:
 * - Server avatar squircle, home icon, server name, category pill
 * - Online / member count indicators
 * - Quick Action Buttons: 6 Boosts, Invite, Notifications
 * - Grouped Card 1: Mark As Read, Browse Channels
 * - Grouped Card 2: Edit Per-server Profile, Server Tag, Show All Channels switch,
 *   Hide Muted Channels switch, Allow Direct Messages switch, Allow Message Requests switch,
 *   Report Server, Leave Server
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography across all UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerProfileDrawer(
    isOpen: Boolean,
    server: ServerItem,
    onDismiss: () -> Unit,
    onInviteClick: () -> Unit,
    onMarkAsReadClick: () -> Unit,
    onBrowseChannelsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onLeaveServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAllChannels by remember { mutableStateOf(true) }
    var hideMutedChannels by remember { mutableStateOf(false) }
    var allowDirectMessages by remember { mutableStateOf(true) }
    var allowMessageRequests by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE3E5E8))
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start
        ) {

                // Server Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(resolveServerDrawableRes(server.iconResName)),
                        contentDescription = server.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Server Name Row with Purple House/Flower Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = server.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22),
                        letterSpacing = (-0.3).sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Pill (e.g. Community Server)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF2F3F5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFF5C6470),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${server.categoryText} Server",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5C6470)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Online / Members Stats
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF23A55A))
                    )
                    Text(
                        text = "1,082 Online",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF5C6470)
                    )
                    Text(
                        text = "·",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80848E)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF80848E))
                    )
                    Text(
                        text = server.memberCountText.ifBlank { "48,076 Members" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF5C6470)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Top Quick Action Squircle Buttons (6 Boosts, Invite, Notifications)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Boosts Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* Boosts info */ }
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF2F3F5)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = "Boosts",
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "6 Boosts",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1F22)
                        )
                    }

                    // Invite Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onInviteClick)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF2F3F5)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Invite",
                                    tint = Color(0xFF1E1F22),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Invite",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1F22)
                        )
                    }

                    // Notifications Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* Notifications settings */ }
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF2F3F5)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color(0xFF1E1F22),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Notifications",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1F22)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Grouped Card 1: Mark As Read, Browse Channels
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8F9FA)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onMarkAsReadClick)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mark As Read",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22)
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onBrowseChannelsClick)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Browse Channels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grouped Card 2: Per-server settings, toggles, report, leave
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8F9FA)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Edit Per-server Profile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onEditProfileClick)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edit Per-server Profile",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22)
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Server Tag with Leaf Vector Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // ZERO EMOJI POLICY: Vector Leaf Icon + Server Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF2F3F5))
                                    .border(1.dp, Color(0xFFE3E5E8), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = Color(0xFFEAB308),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    val tagInitials = server.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                                    Text(
                                        text = tagInitials.ifBlank { "CAMP" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E1F22)
                                    )
                                }
                            }

                            Text(
                                text = "Server Tag",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22)
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Show All Channels Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show All Channels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = showAllChannels,
                                onCheckedChange = { showAllChannels = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF5865F2)
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Hide Muted Channels Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hide Muted Channels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = hideMutedChannels,
                                onCheckedChange = { hideMutedChannels = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF5865F2)
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Allow Direct Messages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow Direct Messages",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E1F22)
                                )
                                Text(
                                    text = "Anyone in the server can message you",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C6470)
                                )
                            }
                            Switch(
                                checked = allowDirectMessages,
                                onCheckedChange = { allowDirectMessages = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF5865F2)
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Allow Message Requests
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow Message Requests",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E1F22)
                                )
                                Text(
                                    text = "Filter messages from members you may not know",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C6470)
                                )
                            }
                            Switch(
                                checked = allowMessageRequests,
                                onCheckedChange = { allowMessageRequests = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF5865F2)
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Report Server
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Report */ }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Report Server",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1F22)
                            )
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // Leave Server
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLeaveServerClick)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Leave Server",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDA373C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
}

private fun resolveServerDrawableRes(name: String): Int {
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
        "avatar_lincoln" -> com.campus.network.core.ui.R.drawable.avatar_lincoln
        else -> com.campus.network.core.ui.R.drawable.server_codesistency
    }
}
