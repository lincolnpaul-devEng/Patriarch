package com.campus.network.feature.channels.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.feature.channels.model.PerServerProfile

/**
 * Pixel-Perfect Per-Server Profile & Custom Server Tag Customizer Modal Sheet.
 * Matching patriarch's server identity options.
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerProfileSheet(
    isOpen: Boolean,
    serverId: String,
    serverName: String,
    initialProfile: PerServerProfile?,
    defaultNickname: String = "Lincoln Kiprotich",
    avatarUrl: String? = null,
    onDismiss: () -> Unit,
    onSaveProfile: (nickname: String, tag: String, tagColorHex: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nickname by remember(initialProfile, defaultNickname) {
        mutableStateOf(initialProfile?.nickname ?: defaultNickname)
    }
    var tag by remember(initialProfile) {
        mutableStateOf(initialProfile?.tag ?: "DEV")
    }
    var tagColorHex by remember(initialProfile) {
        mutableLongStateOf(initialProfile?.tagColorHex ?: 0xFF5865F2)
    }

    val availableColors = remember {
        listOf(
            0xFF5865F2, // Blurple
            0xFF23A55A, // Emerald Green
            0xFFEAB308, // Gold
            0xFFE11D48, // Crimson Red
            0xFFA855F7, // Violet
            0xFF0284C7  // Cyan
        )
    }

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Server Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )

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

            Text(
                text = "Customize your identity specifically for $serverName. Your global profile remains unchanged.",
                fontSize = 13.5.sp,
                color = Color(0xFF5C6470),
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // LIVE PREVIEW CARD
            Text(
                text = "PROFILE PREVIEW",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF2F3F5),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3E5E8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar Squircle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF003B68)),
                        contentAlignment = Alignment.Center
                    ) {
                        com.campus.network.core.ui.components.CampusStudentAvatar(
                            avatarUrl = avatarUrl,
                            fullName = nickname.ifBlank { defaultNickname },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = nickname.ifBlank { defaultNickname },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1F22)
                            )

                            // Per-Server Custom Tag Pill
                            if (tag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(tagColorHex).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = Color(tagColorHex)
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
                                            tint = Color(tagColorHex),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = tag.uppercase().take(4),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(tagColorHex)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Role badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Owner",
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Server Owner",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEAB308)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SERVER NICKNAME INPUT
            Text(
                text = "SERVER NICKNAME",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(32) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (nickname.isNotEmpty()) {
                        IconButton(
                            onClick = { nickname = "" },
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

            // SERVER TAG INPUT (Up to 4 chars)
            Text(
                text = "SERVER TAG (MAX 4 CHARS)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = tag,
                        onValueChange = { tag = it.uppercase().take(4) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${tag.length}/4",
                        fontSize = 12.sp,
                        color = Color(0xFF80848E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // TAG COLOR SELECTOR
            Text(
                text = "TAG ACCENT COLOR",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableColors.forEach { colorVal ->
                    val isSelected = tagColorHex == colorVal
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal))
                            .clickable { tagColorHex = colorVal }
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, Color(0xFF1E1F22), CircleShape)
                                } else Modifier
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SAVE BUTTON
            Button(
                onClick = {
                    val finalNickname = nickname.trim().ifBlank { defaultNickname }
                    val finalTag = tag.trim().uppercase().ifBlank { "DEV" }
                    onSaveProfile(finalNickname, finalTag, tagColorHex)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Save Server Profile",
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
