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
import com.campus.network.core.ui.theme.BrandBlueAction
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary

/**
 * Dedicated WhatsApp/Telegram-style Broadcast Message Composer screen.
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070E1A),
                        Color(0xFF0F1A2A),
                        Color(0xFF030712)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
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
                            .background(Color(0x26FFFFFF))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "New Broadcast",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${selectedPeerIds.size} of ${threads.size} selected",
                            fontSize = 12.sp,
                            color = if (selectedPeerIds.isNotEmpty()) BrandBlueAction else Color(0xFF94A3B8)
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
                        color = BrandBlueAction
                    )
                }
            }

            // 2. Info Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x1A38BDF8),
                border = BorderStroke(1.dp, Color(0x3338BDF8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = BrandBlueAction,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recipients will receive your message as a direct 1-on-1 message in their chat stream.",
                        fontSize = 12.sp,
                        color = Color(0xFFE0E7FF),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Broadcast Memo Composer Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF131C28),
                border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BROADCAST MESSAGE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueAction,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BasicTextField(
                        value = messageText,
                        onValueChange = { if (it.length <= 500) messageText = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = Color.White,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(BrandBlueAction),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        decorationBox = { innerTextField ->
                            if (messageText.isEmpty()) {
                                Text(
                                    text = "Type your announcement or memo to send to all selected contacts...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 20.sp
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
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Contact Search Field
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF131C28),
                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(BrandBlueAction),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search contacts by name or handle...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlueAction,
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (canSend) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPeerIds.isEmpty()) "Select at least 1 contact"
                            else "Send to ${selectedPeerIds.size} ${if (selectedPeerIds.size == 1) "Chat" else "Chats"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canSend) Color.White else Color(0xFF64748B)
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
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0x2638BDF8) else Color(0xFF131C28),
        border = BorderStroke(
            1.dp,
            if (isSelected) BrandBlueAction else Color(0x1AFFFFFF)
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
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) BrandBlueAction else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) BrandBlueAction else Color(0xFF64748B),
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = thread.peerHandle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (thread.isOnline) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
            }
        }
    }
}
