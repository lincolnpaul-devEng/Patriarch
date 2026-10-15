package com.campus.network.feature.messages

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.theme.AlertRed
import com.campus.network.core.ui.theme.BorderSubtle
import com.campus.network.core.ui.theme.BrandBlueAction
import com.campus.network.core.ui.theme.BrandBlueLight
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.SurfaceElevated
import com.campus.network.core.ui.theme.SurfaceSubtle
import com.campus.network.core.ui.theme.SurfaceWhite
import com.campus.network.core.ui.theme.TextMuted
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import com.campus.network.core.ui.theme.TextWhite

@Composable
fun MessageThreadsScreen(
    uiState: MessagesUiState,
    onEvent: (MessagesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredThreads = uiState.threads.filter { thread ->
        val matchesCategory = (uiState.selectedCategory == ThreadCategory.ALL) ||
                (thread.category == uiState.selectedCategory)
        val matchesSearch = uiState.searchQuery.isBlank() ||
                thread.peerName.contains(uiState.searchQuery, ignoreCase = true) ||
                thread.lastMessage.contains(uiState.searchQuery, ignoreCase = true) ||
                thread.peerHandle.contains(uiState.searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val onlinePeers = uiState.threads.filter { it.isOnline }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SurfaceSubtle,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val defaultPeer = uiState.threads.firstOrNull()
                    if (defaultPeer != null) {
                        onEvent(MessagesEvent.SelectThread(defaultPeer.id))
                    }
                },
                containerColor = BrandBluePrimary,
                contentColor = TextWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Direct Chat"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Messages",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                        }

                        val totalUnread = uiState.threads.sumOf { it.unreadCount }
                        if (totalUnread > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandBlueLight
                            ) {
                                Text(
                                    text = "$totalUnread unread",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Compact Professional Search Bar (Standard 40.dp Height)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search peers, messages, or keywords...",
                                        fontSize = 13.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { onEvent(MessagesEvent.SearchQueryChanged(it)) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onEvent(MessagesEvent.SearchQueryChanged("")) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ThreadCategory.values()) { category ->
                            val isSelected = (category == uiState.selectedCategory)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) BrandBluePrimary else SurfaceElevated,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    onEvent(MessagesEvent.SelectCategory(category))
                                }
                            ) {
                                Text(
                                    text = category.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextWhite else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Presence Rail (Stream Online Peers)
            if (onlinePeers.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite)
                            .padding(top = 10.dp, bottom = 14.dp)
                    ) {
                        Text(
                            text = "Active Now",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(onlinePeers) { peer ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onEvent(MessagesEvent.SelectThread(peer.id)) }
                                        .width(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(BrandBluePrimary, BrandBlueMid)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = peer.avatarInitials,
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }

                                        // Online status dot
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(SurfaceWhite)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(BrandGreen)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = peer.peerName.split(" ").firstOrNull() ?: peer.peerName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Message Threads (Stream ChannelItem layout)
            if (filteredThreads.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No conversations found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching for a different name or keyword",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(filteredThreads, key = { it.id }) { thread ->
                    ThreadListItem(
                        thread = thread,
                        onClick = { onEvent(MessagesEvent.SelectThread(thread.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreadListItem(
    thread: ConversationThread,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceWhite,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Online Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(BrandBluePrimary, BrandBlueMid)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = thread.avatarInitials,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (thread.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(BrandGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Peer Info & Last Message Snippet
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = thread.peerName,
                            fontSize = 15.sp,
                            fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (thread.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = BrandBlueAction,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = thread.lastTimestamp,
                        fontSize = 11.sp,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (thread.unreadCount > 0) BrandBlueAction else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = thread.lastMessage,
                        fontSize = 13.sp,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (thread.unreadCount > 0) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (thread.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandBluePrimary)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
