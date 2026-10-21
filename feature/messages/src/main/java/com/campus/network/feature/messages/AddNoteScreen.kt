package com.campus.network.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dedicated Share a Note screen matching the server light theme:
 * Pure white background, #1E1F22 typography, #5865F2 accent, #F2F3F5 input cards.
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography across all UI.
 */
@Composable
fun AddNoteScreen(
    currentNote: String,
    fullName: String = "",
    avatarUrl: String? = null,
    onBack: () -> Unit,
    onShareNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteText by remember {
        mutableStateOf(if (currentNote != "Add note") currentNote else "")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Text(
                    text = "New Note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )

                Button(
                    onClick = { onShareNote(noteText) },
                    enabled = noteText.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5865F2),
                        disabledContainerColor = Color(0xFF5865F2).copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "Share",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

            HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                // Center Interactive Avatar with Dynamic Thought Bubble
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Thought Bubble above Avatar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFF2F3F5),
                            border = BorderStroke(1.dp, Color(0xFFE3E5E8)),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (noteText.isEmpty()) {
                                    Text(
                                        text = "Share a thought...",
                                        fontSize = 15.sp,
                                        color = Color(0xFF80848E),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                BasicTextField(
                                    value = noteText,
                                    onValueChange = { if (it.length <= 60) noteText = it },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E1F22),
                                        textAlign = TextAlign.Center
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF5865F2)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Bubble Caret
                        Canvas(modifier = Modifier.size(width = 12.dp, height = 8.dp)) {
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width / 2f, size.height)
                                lineTo(size.width, 0f)
                                close()
                            }
                            drawPath(path = path, color = Color(0xFFF2F3F5))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // User Avatar
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, Color(0xFF5865F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        com.campus.network.core.ui.components.CampusStudentAvatar(
                            avatarUrl = avatarUrl,
                            fullName = fullName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${noteText.length} / 60",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF80848E)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Sharing Audience Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, Color(0xFFE3E5E8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEBF0FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color(0xFF5865F2),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Shared with: Campus Peers",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1F22)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your note is visible at the top of friends' message feeds.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C6470)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF80848E),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Replies to your note will start a direct chat with you.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF80848E)
                            )
                        }
                    }
                }
            }
        }
    }
}
