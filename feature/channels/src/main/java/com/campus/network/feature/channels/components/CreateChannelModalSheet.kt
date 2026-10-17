package com.campus.network.feature.channels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pixel-Perfect Channel Creation Modal Sheet.
 * Allows creating text, announcement, rules, or voice channels within a server.
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelModalSheet(
    serverName: String,
    initialCategory: String = "TEXT CHANNELS",
    existingCategories: List<String> = listOf("WELCOME", "TEXT CHANNELS", "DEVELOPMENT", "COMMUNITY", "STUDY JAMS"),
    onDismiss: () -> Unit,
    onCreateChannel: (name: String, category: String, type: String, topic: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var channelName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("text") } // "text", "announcements", "rules", "voice"
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var channelTopic by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header: Close 'X' and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Create Channel",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22),
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "in $serverName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5C6470)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1E1F22),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. CHANNEL TYPE SELECTOR
            Text(
                text = "CHANNEL TYPE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChannelTypeOptionCard(
                    icon = Icons.Default.Tag,
                    title = "Text",
                    description = "Post messages, images, stickers, opinions, and code snippets",
                    isSelected = selectedType == "text",
                    onClick = { selectedType = "text" }
                )

                ChannelTypeOptionCard(
                    icon = Icons.Default.Campaign,
                    title = "Announcements",
                    description = "Broadcast important server updates and scheduled alerts",
                    isSelected = selectedType == "announcements",
                    onClick = { selectedType = "announcements" }
                )

                ChannelTypeOptionCard(
                    icon = Icons.Default.Gavel,
                    title = "Rules & Conduct",
                    description = "Official guidelines, code of conduct, and moderation policies",
                    isSelected = selectedType == "rules",
                    onClick = { selectedType = "rules" }
                )

                ChannelTypeOptionCard(
                    icon = Icons.Default.VolumeUp,
                    title = "Voice Lounge",
                    description = "Hang out together with high-fidelity spatial voice audio",
                    isSelected = selectedType == "voice",
                    onClick = { selectedType = "voice" }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. CHANNEL NAME INPUT
            Text(
                text = "CHANNEL NAME",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val leadingIcon = when (selectedType) {
                        "announcements" -> Icons.Default.Campaign
                        "rules" -> Icons.Default.Gavel
                        "voice" -> Icons.Default.VolumeUp
                        else -> Icons.Default.Tag
                    }

                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(20.dp)
                    )

                    BasicTextField(
                        value = channelName,
                        onValueChange = { input ->
                            // Auto-format channel name to kebab-case
                            channelName = input.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9_-]"), "")
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Medium
                        ),
                        decorationBox = { innerTextField ->
                            if (channelName.isEmpty()) {
                                Text(
                                    text = "new-channel",
                                    fontSize = 15.sp,
                                    color = Color(0xFF949BA4)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (channelName.isNotEmpty()) {
                        IconButton(
                            onClick = { channelName = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF5C6470),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. CATEGORY SELECTION CHIPS
            Text(
                text = "CATEGORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                existingCategories.distinct().forEach { cat ->
                    val isCatSelected = selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isCatSelected) Color(0xFF5865F2) else Color(0xFFF2F3F5),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCatSelected) Color.White else Color(0xFF1E1F22),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. CHANNEL TOPIC / PURPOSE (OPTIONAL)
            Text(
                text = "TOPIC / DESCRIPTION (OPTIONAL)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (channelTopic.isEmpty()) {
                        Text(
                            text = "What is this channel for? Set guidelines or discussion topics...",
                            fontSize = 13.5.sp,
                            color = Color(0xFF949BA4)
                        )
                    }
                    BasicTextField(
                        value = channelTopic,
                        onValueChange = { channelTopic = it },
                        textStyle = TextStyle(
                            fontSize = 13.5.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. ACTION BUTTONS: CANCEL & CREATE CHANNEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E5058)
                    )
                }

                Button(
                    onClick = {
                        val finalName = channelName.ifBlank {
                            when (selectedType) {
                                "announcements" -> "announcements"
                                "rules" -> "rules"
                                "voice" -> "general-voice"
                                else -> "general"
                            }
                        }
                        onCreateChannel(finalName, selectedCategory, selectedType, channelTopic.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Create Channel",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ChannelTypeOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF5865F2) else Color(0xFFE3E5E8),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFF2F4FB) else Color(0xFFF8F9FA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF5865F2) else Color(0xFFE3E5E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else Color(0xFF4E5058),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    color = Color(0xFF5C6470),
                    lineHeight = 16.sp
                )
            }

            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color(0xFF5865F2) else Color(0xFFB5BAC1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5865F2))
                    )
                }
            }
        }
    }
}
