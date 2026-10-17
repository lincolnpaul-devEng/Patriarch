package com.campus.network.feature.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.theme.BrandBlueAction
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary

/**
 * Main Message Threads Screen with Dark Cosmic/Radial Gradient Aesthetic.
 * Fully follows WhatsApp, patriarch, and Instagram patterns:
 * - Direct full-screen navigation for Add Note, New Chat, Broadcast, and Voice Studio (no dialogs).
 * - Central avatar tap navigates directly to Profile screen to edit avatar and bio.
 * - Floating dock triggers dedicated screens.
 */
@Composable
fun MessageThreadsScreen(
    uiState: MessagesUiState,
    onEvent: (MessagesEvent) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // UI interactive states
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }

    val filteredThreads = when (selectedTab) {
        1 -> uiState.threads.filter { it.unreadCount > 0 }
        2 -> uiState.threads.filter { it.category != ThreadCategory.DIRECT }
        else -> uiState.threads
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        val centerX = constraints.maxWidth.toFloat() / 2f

        // 1. Top Atmospheric Radial Glow Spotlight (Mirroring reference background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A).copy(alpha = 0.55f),
                            Color(0xFF0284C7).copy(alpha = 0.25f),
                            Color(0xFF030712).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, 0f),
                        radius = constraints.maxWidth.toFloat() * 0.95f
                    )
                )
        )

        // Subtle secondary deep indigo accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4338CA).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, 40f),
                        radius = constraints.maxWidth.toFloat() * 0.6f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 2. Custom App Bar: Back button | "Message" title | More options (...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular frosted Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Message",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                // More Options Button with Dropdown
                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                            .clickable { showMoreOptionsMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreOptionsMenu,
                        onDismissRequest = { showMoreOptionsMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF131C28))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Broadcast", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = BrandBlueAction, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showMoreOptionsMenu = false
                                onEvent(MessagesEvent.OpenBroadcast)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Status Note", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = BrandBlueAction, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showMoreOptionsMenu = false
                                onEvent(MessagesEvent.OpenAddNote)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Profile Avatar", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlueAction, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showMoreOptionsMenu = false
                                onNavigateToProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark All as Read", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showMoreOptionsMenu = false
                                uiState.threads.forEach { thread ->
                                    if (thread.unreadCount > 0) {
                                        onEvent(MessagesEvent.SelectThread(thread.id))
                                        onEvent(MessagesEvent.BackToThreads)
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archived Chats", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showMoreOptionsMenu = false
                                selectedTab = 2
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Blocked Contacts", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showMoreOptionsMenu = false
                            }
                        )
                    }
                }
            }

            // 3. Floating Stories & "Add note" Celestial Arc
            StoriesCelestialArc(
                noteText = uiState.currentUserNote,
                onAddNoteClick = { onEvent(MessagesEvent.OpenAddNote) },
                onAvatarClick = onNavigateToProfile,
                threads = uiState.threads,
                onSelectThread = { threadId -> onEvent(MessagesEvent.SelectThread(threadId)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Segmented Filter Tabs: Inbox | Unread | Requests
            val totalUnread = uiState.threads.sumOf { it.unreadCount }
            val inboxLabel = if (totalUnread > 0) "Inbox $totalUnread" else "Inbox 26"

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF131C28),
                border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterTabPill(
                        label = inboxLabel,
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabPill(
                        label = "Unread",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabPill(
                        label = "Requests",
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Message Threads List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                items(filteredThreads, key = { it.id }) { thread ->
                    MessageThreadRow(
                        thread = thread,
                        onClick = { onEvent(MessagesEvent.SelectThread(thread.id)) }
                    )
                }
            }
        }

        // 6. Floating Action Dock (Bottom Center: + New | Mic | Send)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF0F1725).copy(alpha = 0.95f),
                border = BorderStroke(1.2.dp, Color(0x40FFFFFF)),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // + New Action: opens WhatsApp/patriarch-style Contact Picker screen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onEvent(MessagesEvent.OpenNewChat) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Mic Icon Button: opens chat selection for voice message
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onEvent(MessagesEvent.OpenVoiceChatSelection) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Send / Paper Plane Icon Button: opens Broadcast screen
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222E40))
                            .clickable { onEvent(MessagesEvent.OpenBroadcast) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Quick Broadcast",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5-Avatar Celestial Arc with the central user avatar, "Add note" gradient speech bubble,
 * and "+" circular badge.
 * Tapping the "Add note" bubble opens AddNoteScreen.
 * Tapping the central user avatar or "+" badge navigates to the Profile screen to edit avatar and bio.
 */
@Composable
private fun StoriesCelestialArc(
    noteText: String,
    onAddNoteClick: () -> Unit,
    onAvatarClick: () -> Unit,
    threads: List<ConversationThread>,
    onSelectThread: (String) -> Unit
) {
    val peer0 = threads.getOrNull(0)
    val peer1 = threads.getOrNull(1)
    val peer2 = threads.getOrNull(2)
    val peer3 = threads.getOrNull(3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outermost Left Avatar (offset slightly higher)
            FloatingAvatarItem(
                initials = peer0?.avatarInitials ?: "JW",
                size = 46,
                yOffset = -8,
                ringColors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF38BDF8)),
                onClick = { peer0?.let { onSelectThread(it.id) } }
            )

            // Inner Left Avatar (offset slightly lower)
            FloatingAvatarItem(
                initials = peer1?.avatarInitials ?: "GH",
                size = 42,
                yOffset = 8,
                ringColors = listOf(Color(0xFFEC4899), Color(0xFF818CF8), Color(0xFFEC4899)),
                onClick = { peer1?.let { onSelectThread(it.id) } }
            )

            // Center User Profile with "Add note" Speech Bubble
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                // "Add note" gradient pill bubble (Interactive)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onAddNoteClick)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF5B67EA), Color(0xFF0284C7))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = noteText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Downward-pointing speech bubble tail
                    Canvas(modifier = Modifier.size(width = 8.dp, height = 5.dp)) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width / 2f, size.height)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path = path, color = Color(0xFF0284C7))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Center Avatar with glowing ring and "+" badge
                Box(contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF818CF8),
                                        Color(0xFFE879F9),
                                        Color(0xFF38BDF8)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandBluePrimary, BrandBlueMid)
                                )
                            )
                            .clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "L",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Circular "+" badge to edit profile avatar
                    Box(
                        modifier = Modifier
                            .offset(y = 6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFF030712), CircleShape)
                            .clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Edit Profile Avatar",
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Inner Right Avatar (offset slightly lower)
            FloatingAvatarItem(
                initials = peer2?.avatarInitials ?: "KW",
                size = 42,
                yOffset = 8,
                ringColors = listOf(Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFF10B981)),
                onClick = { peer2?.let { onSelectThread(it.id) } }
            )

            // Outermost Right Avatar (offset slightly higher)
            FloatingAvatarItem(
                initials = peer3?.avatarInitials ?: "AM",
                size = 46,
                yOffset = -8,
                ringColors = listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFFF59E0B)),
                onClick = { peer3?.let { onSelectThread(it.id) } }
            )
        }
    }
}

@Composable
private fun FloatingAvatarItem(
    initials: String,
    size: Int,
    yOffset: Int,
    ringColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(y = yOffset.dp)
            .size(size.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                brush = Brush.sweepGradient(ringColors),
                shape = CircleShape
            )
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E2D44), Color(0xFF0F172A))
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size / 3).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun FilterTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF5B67EA), Color(0xFF0284C7))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun MessageThreadRow(
    thread: ConversationThread,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (thread.isOnline) Color(0xFF10B981) else Color(0x33FFFFFF),
                    shape = CircleShape
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thread.avatarInitials,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Peer Name & Message Preview
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = thread.peerName,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = thread.lastMessage,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Timestamp & Notification Bell Button
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = thread.lastTimestamp,
                fontSize = 11.5.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131B26)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (thread.unreadCount > 0) Icons.Default.NotificationsNone else Icons.Default.NotificationsOff,
                    contentDescription = "Notification",
                    tint = if (thread.unreadCount > 0) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
