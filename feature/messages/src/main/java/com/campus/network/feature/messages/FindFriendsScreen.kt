package com.campus.network.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Campus-style directory to find students with accounts and send / accept friend requests.
 */
@Composable
fun FindFriendsScreen(
    uiState: MessagesUiState,
    onEvent: (MessagesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.findFriendsFeedback) {
        if (!uiState.findFriendsFeedback.isNullOrBlank()) {
            kotlinx.coroutines.delay(2_400L)
            onEvent(MessagesEvent.DismissFindFriendsFeedback())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Find Friends",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Students on Campus Network",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B)
                )
            }
            Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = null,
                tint = Color(0xFF5865F2),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
            )
        }

        Surface(
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = uiState.findFriendsQuery,
                    onValueChange = { onEvent(MessagesEvent.FindFriendsQueryChanged(it)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (uiState.findFriendsQuery.isBlank()) {
                            Text(
                                text = "Search by name or campus email",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                )
            }
        }

        uiState.findFriendsFeedback?.let { feedback ->
            Text(
                text = feedback,
                color = Color(0xFF166534),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDCFCE7))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        when {
            uiState.isFindFriendsLoading && uiState.discoverablePeers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF5865F2))
                }
            }
            uiState.discoverablePeers.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No campus peers found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try another search, or ask classmates to join Campus Network.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "${uiState.discoverablePeers.size} students",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(uiState.discoverablePeers, key = { it.userId }) { peer ->
                        CampusPeerFriendCard(
                            peer = peer,
                            onAddFriend = {
                                onEvent(
                                    MessagesEvent.SendCampusFriendRequest(
                                        peerUserId = peer.userId,
                                        peerName = peer.fullName
                                    )
                                )
                            },
                            onAccept = {
                                onEvent(
                                    MessagesEvent.AcceptCampusFriendRequest(
                                        peerUserId = peer.userId,
                                        peerName = peer.fullName
                                    )
                                )
                            },
                            onMessage = {
                                onEvent(
                                    MessagesEvent.MessageCampusFriend(
                                        peerUserId = peer.userId,
                                        peerName = peer.fullName
                                    )
                                )
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CampusPeerFriendCard(
    peer: DiscoverableCampusPeer,
    onAddFriend: () -> Unit,
    onAccept: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                        )
                    )
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!peer.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = peer.avatarUrl,
                        contentDescription = peer.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = peer.initials.take(2),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = peer.handle,
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (peer.universityName.isNotBlank()) {
                    Text(
                        text = peer.universityName,
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (peer.friendStatus) {
                DiscoverFriendStatus.NONE -> {
                    Button(
                        onClick = onAddFriend,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                DiscoverFriendStatus.REQUEST_SENT -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Pending", fontSize = 12.5.sp, color = Color(0xFF64748B))
                    }
                }
                DiscoverFriendStatus.REQUEST_RECEIVED -> {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23A55A)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                DiscoverFriendStatus.FRIENDS -> {
                    Button(
                        onClick = onMessage,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Message", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                DiscoverFriendStatus.SELF -> Unit
            }
        }
    }
}
