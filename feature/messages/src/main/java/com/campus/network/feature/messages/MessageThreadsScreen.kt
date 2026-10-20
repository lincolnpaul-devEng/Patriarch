package com.campus.network.feature.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.components.CampusInlineNetworkAlertCard
import com.campus.network.core.ui.components.MessageThreadRowSkeleton
import com.campus.network.core.ui.components.rememberCampusNetworkState
import com.campus.network.core.ui.theme.BrandBlueInteractive
import kotlinx.coroutines.delay

/**
 * Pixel-Perfect Direct Messages Hub matching the patriarch / Discord mobile server UI layout:
 * - 72dp Left Server Rail with active DM Chat Bubble icon and server navigation
 * - Pure White Main Panel with rounded top-start corner (20dp)
 * - Cohesive server theme: #1E1F22 text, #5865F2 blurple accent, #E3E5E8 rail, #23A55A online green dot
 * - Interactive search, Active Now horizontal friends lineup with Lincoln note, segmented filter tabs
 * - Direct conversation threads list with server tags, verified badges, and unread red count pills
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography across all UI.
 */
@Composable
fun MessageThreadsScreen(
    uiState: MessagesUiState,
    onEvent: (MessagesEvent) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToServer: ((String) -> Unit)? = null,
    onAddServer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }

    val filteredThreads = remember(uiState.threads, selectedTab, searchQuery) {
        uiState.threads.filter { thread ->
            val matchesTab = when (selectedTab) {
                1 -> thread.unreadCount > 0
                2 -> thread.category != ThreadCategory.DIRECT
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    thread.peerName.contains(searchQuery, ignoreCase = true) ||
                    thread.peerHandle.contains(searchQuery, ignoreCase = true) ||
                    thread.lastMessage.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    val activeFriends = remember(uiState.threads) {
        uiState.threads.filter { it.isOnline }.ifEmpty { uiState.threads.take(5) }
    }

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
            // LEFT RAIL (People & Quick Presence Column - Width 72.dp)
            MessagePeopleRail(
                currentUserNote = uiState.currentUserNote,
                fullName = uiState.studentProfile?.fullName?.ifBlank { "Student" } ?: "Student",
                avatarUrl = uiState.studentProfile?.avatarUrl,
                activeFriends = activeFriends,
                onNavigateToServers = {
                    onNavigateToServer?.invoke("")
                },
                onAddNoteClick = { onEvent(MessagesEvent.OpenAddNote) },
                onSelectFriend = { threadId -> onEvent(MessagesEvent.SelectThread(threadId)) },
                onNewChatClick = { onEvent(MessagesEvent.OpenNewChat) },
                onProfileClick = onNavigateToProfile,
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
            )

            // RIGHT MAIN PANEL (White rounded panel matching server layout)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 20.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Direct Messages Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Direct Messages",
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1F22),
                                letterSpacing = (-0.3).sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Search toggle button
                                IconButton(
                                    onClick = { isSearchExpanded = !isSearchExpanded },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (isSearchExpanded) Color(0xFF5865F2) else Color(0xFF1E1F22),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // New Chat Button
                                IconButton(
                                    onClick = { onEvent(MessagesEvent.OpenNewChat) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "New Chat",
                                        tint = Color(0xFF1E1F22),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // More Options Menu (...)
                                Box {
                                    IconButton(
                                        onClick = { showMoreOptionsMenu = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = Color(0xFF1E1F22),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMoreOptionsMenu,
                                        onDismissRequest = { showMoreOptionsMenu = false },
                                        modifier = Modifier
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFE3E5E8), RoundedCornerShape(12.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("New Broadcast", color = Color(0xFF1E1F22), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                onEvent(MessagesEvent.OpenBroadcast)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add Status Note", color = Color(0xFF1E1F22), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                onEvent(MessagesEvent.OpenAddNote)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Edit Profile", color = Color(0xFF1E1F22), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                onNavigateToProfile()
                                            }
                                        )
                                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))
                                        DropdownMenuItem(
                                            text = { Text("Mark All as Read", color = Color(0xFF1E1F22), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF5C6470), modifier = Modifier.size(18.dp))
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
                                            text = { Text("Archived Chats", color = Color(0xFF1E1F22), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF5C6470), modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                selectedTab = 2
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Blocked Contacts", color = Color(0xFF1E1F22), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFF5C6470), modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

                        // 2. Interactive Search Bar
                        AnimatedVisibility(
                            visible = isSearchExpanded || searchQuery.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF2F3F5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E1F22)
                                        ),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Find or start a conversation",
                                                    fontSize = 13.5.sp,
                                                    color = Color(0xFF80848E)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF5C6470),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { searchQuery = "" }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Segmented Filter Tabs: Inbox | Unread | Requests
                        val totalUnread = uiState.threads.sumOf { it.unreadCount }
                        val inboxLabel = if (totalUnread > 0) "Inbox ($totalUnread)" else "Inbox"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF2F3F5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ServerFilterTabPill(
                                    label = inboxLabel,
                                    isSelected = selectedTab == 0,
                                    badgeCount = 0,
                                    onClick = { selectedTab = 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                ServerFilterTabPill(
                                    label = "Unread",
                                    isSelected = selectedTab == 1,
                                    badgeCount = totalUnread,
                                    onClick = { selectedTab = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                                ServerFilterTabPill(
                                    label = "Requests",
                                    isSelected = selectedTab == 2,
                                    badgeCount = 0,
                                    onClick = { selectedTab = 2 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val networkState = rememberCampusNetworkState()
                        var isThreadsSyncing by remember { mutableStateOf(true) }
                        var retryCount by remember { mutableIntStateOf(0) }

                        LaunchedEffect(selectedTab, retryCount, networkState.isOnline) {
                            if (networkState.isOnline) {
                                isThreadsSyncing = true
                                delay(if (networkState.isSlowNetwork) 750L else 420L)
                                isThreadsSyncing = false
                            } else {
                                isThreadsSyncing = false
                            }
                        }

                        if (!networkState.isOnline) {
                            CampusInlineNetworkAlertCard(
                                featureName = "Direct Messages",
                                description = "Showing cached conversations. New direct messages will sync as soon as you reconnect.",
                                actionLabel = "Retry connection",
                                onActionClick = {
                                    networkState.triggerRetry()
                                    retryCount++
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        // 5. Message Threads Stream
                        if (isThreadsSyncing) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                            ) {
                                items(6) {
                                    MessageThreadRowSkeleton()
                                    HorizontalDivider(
                                        thickness = 0.6.dp,
                                        color = Color(0xFFF2F3F5),
                                        modifier = Modifier.padding(start = 72.dp)
                                    )
                                }
                            }
                        } else if (filteredThreads.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF2F3F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forum,
                                            contentDescription = null,
                                            tint = Color(0xFF80848E),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "No direct messages",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E1F22)
                                    )
                                    Text(
                                        text = "Start a chat with a course-mate or server member.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF5C6470),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp)
                            ) {
                                items(filteredThreads, key = { it.id }) { thread ->
                                    ServerMessageThreadRow(
                                        thread = thread,
                                        onClick = { onEvent(MessagesEvent.SelectThread(thread.id)) }
                                    )
                                    HorizontalDivider(
                                        thickness = 0.6.dp,
                                        color = Color(0xFFF2F3F5),
                                        modifier = Modifier.padding(start = 72.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Floating Action Button: Blurple Server FAB at Bottom Right
                    FloatingActionButton(
                        onClick = { onEvent(MessagesEvent.OpenNewChat) },
                        containerColor = Color(0xFF5865F2),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 18.dp, bottom = 18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New Message",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 72dp Left Rail matching Discord/server layout.
 * Displays online people and presence instead of server list:
 * - Top-most icon: Server icon (Groups) for switching to servers
 * - Lincoln Paul user profile with "+ Note" & Add Note action
 * - Vertically stacked online friends with #23A55A green presence indicators and unread badges
 * - "+" Button to start a new direct chat
 * - Lincoln profile avatar at the bottom
 */
@Composable
private fun MessagePeopleRail(
    currentUserNote: String,
    fullName: String = "Student",
    avatarUrl: String? = null,
    activeFriends: List<ConversationThread>,
    onNavigateToServers: () -> Unit,
    onAddNoteClick: () -> Unit,
    onSelectFriend: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFE3E5E8))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Server Icon (Switches user to Server Hub)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onNavigateToServers),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF5865F2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Switch to Servers",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
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

        // 2. User Profile with "+ Note" / Add Note Action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddNoteClick)
                .padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF5865F2), CircleShape)
                ) {
                    com.campus.network.core.ui.components.CampusStudentAvatar(
                        avatarUrl = avatarUrl,
                        fullName = fullName,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Plus badge in bottom-end corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5865F2))
                        .border(1.5.dp, Color(0xFFE3E5E8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Note",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (currentUserNote.isNotBlank()) currentUserNote else "Add Note",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (currentUserNote.isNotBlank()) Color(0xFF1E1F22) else Color(0xFF5C6470),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(56.dp)
            )
        }

        // 3. Scrollable Online People / Friends List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(activeFriends, key = { it.id }) { friend ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFriend(friend.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF5865F2), Color(0xFF7289DA))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friend.avatarInitials,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Green online dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFE3E5E8), CircleShape)
                                .background(Color(0xFF23A55A))
                        )
                    }
                }
            }

            // 4. Quick "+" Add New Chat button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNewChatClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFD1D5DB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = Color(0xFF5865F2),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 5. Bottom User Avatar with Profile Navigation
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            com.campus.network.core.ui.components.CampusStudentAvatar(
                avatarUrl = avatarUrl,
                fullName = fullName,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFE3E5E8), CircleShape)
                    .background(Color(0xFF23A55A))
            )
        }
    }
}

/**
 * Clean Server-style Filter Tab Pill with unread red badge support.
 */
@Composable
private fun ServerFilterTabPill(
    label: String,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(9.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    val isSingleDigit = badgeCount in 1..9
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDA373C))
                            .padding(horizontal = if (isSingleDigit) 0.dp else 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else "$badgeCount",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5C6470)
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    val isSingleDigit = badgeCount in 1..9
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDA373C))
                            .padding(horizontal = if (isSingleDigit) 0.dp else 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else "$badgeCount",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pixel-Perfect Message Thread Row matching server channel/member design.
 */
@Composable
private fun ServerMessageThreadRow(
    thread: ConversationThread,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Avatar (48.dp) with online presence dot
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5865F2), Color(0xFF3B82F6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thread.avatarInitials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (thread.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .background(Color(0xFF23A55A))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Column: Peer Name, Verified, Server Badge, and Last Message
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = thread.peerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (thread.isVerified) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF5865F2),
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (thread.serverBadgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF2F3F5))
                            .border(0.8.dp, Color(0xFFE3E5E8), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = thread.serverBadgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5865F2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = thread.lastMessage,
                fontSize = 13.sp,
                color = Color(0xFF5C6470),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Timestamp & Red Unread Badge
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = thread.lastTimestamp,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF80848E)
            )

            if (thread.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                val isSingleDigit = thread.unreadCount in 1..9
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDA373C))
                        .padding(horizontal = if (isSingleDigit) 0.dp else 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (thread.unreadCount > 99) "99+" else "${thread.unreadCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
        else -> com.campus.network.core.ui.R.drawable.server_codesistency
    }
}
