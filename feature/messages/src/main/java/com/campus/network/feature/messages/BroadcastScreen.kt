package com.campus.network.feature.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dedicated server-styled Broadcast Message Composer screen.
 * Enables sending an announcement memo to multiple selected chats simultaneously.
 */
@Composable
fun BroadcastScreen(
    threads: List<ConversationThread>,
    onBack: () -> Unit,
    onSendBroadcast: (List<String>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedPeerIds = remember { mutableStateListOf<String>() }

    val filteredContacts = threads.filter {
        searchQuery.isBlank() ||
            it.peerName.contains(searchQuery, ignoreCase = true) ||
            it.peerHandle.contains(searchQuery, ignoreCase = true)
    }

    val canSend = messageText.isNotBlank() && selectedPeerIds.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 1. Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2F3F5))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFF1E1F22),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "New Broadcast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1F22)
                        )
                        Text(
                            text = "${selectedPeerIds.size} of ${threads.size} selected",
                            fontSize = 12.sp,
                            color = if (selectedPeerIds.isNotEmpty()) Color(0xFF5865F2) else Color(0xFF5C6470)
                        )
                    }
                }

                // Select All / Clear Toggle
                TextButton(
                    onClick = {
                        if (selectedPeerIds.size == threads.size) {
                            selectedPeerIds.clear()
                        } else {
                            selectedPeerIds.clear()
                            selectedPeerIds.addAll(threads.map { it.id })
                        }
                    }
                ) {
                    Text(
                        text = if (selectedPeerIds.size == threads.size) "Clear" else "Select All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5865F2)
                    )
                }
            }

            HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F4FF),
                border = BorderStroke(1.dp, Color(0xFFD4DEFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFF5865F2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recipients will receive your message as a direct 1-on-1 message in their chat stream.",
                        fontSize = 12.sp,
                        color = Color(0xFF313338),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Broadcast Memo Composer Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF2F3F5),
                border = BorderStroke(1.dp, Color(0xFFE3E5E8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BROADCAST MESSAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5865F2),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BasicTextField(
                        value = messageText,
                        onValueChange = { if (it.length <= 500) messageText = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF1E1F22),
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF5865F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        decorationBox = { innerTextField ->
                            if (messageText.isEmpty()) {
                                Text(
                                    text = "Type your announcement or memo to send to all selected contacts...",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF80848E),
                                    lineHeight = 19.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${messageText.length} / 500",
                            fontSize = 11.sp,
                            color = Color(0xFF80848E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Contact Search Field
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5),
                border = BorderStroke(1.dp, Color(0xFFE3E5E8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
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
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search contacts by name or @handle...",
                                fontSize = 13.5.sp,
                                color = Color(0xFF80848E)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                color = Color(0xFF1E1F22)
                            ),
                            cursorBrush = SolidColor(Color(0xFF5865F2)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Contacts List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.id }) { thread ->
                    val isSelected = thread.id in selectedPeerIds
                    BroadcastRecipientRow(
                        thread = thread,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) {
                                selectedPeerIds.remove(thread.id)
                            } else {
                                selectedPeerIds.add(thread.id)
                            }
                        }
                    )
                }
            }

            // 6. Bottom Sticky Send Button
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (canSend) {
                            onSendBroadcast(selectedPeerIds.toList(), messageText)
                        }
                    },
                    enabled = canSend,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5865F2),
                        disabledContainerColor = Color(0xFFE3E5E8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (canSend) Color.White else Color(0xFF80848E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPeerIds.isEmpty()) "Select at least 1 contact"
                            else "Send to ${selectedPeerIds.size} ${if (selectedPeerIds.size == 1) "Chat" else "Chats"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canSend) Color.White else Color(0xFF80848E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastRecipientRow(
    thread: ConversationThread,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFF0F4FF) else Color(0xFFF8F9FA),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF5865F2) else Color(0xFFE3E5E8)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF5865F2) else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) Color(0xFF5865F2) else Color(0xFFB5BAC1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5865F2), Color(0xFF7289DA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thread.avatarInitials,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and handle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.peerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1F22),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = thread.peerHandle,
                    fontSize = 12.sp,
                    color = Color(0xFF5C6470),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (thread.isOnline) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF23A55A))
                )
            }
        }
    }
}
