package com.campus.network.feature.messages

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.theme.AlertRed
import com.campus.network.core.ui.theme.BrandBlueAction
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.TextMuted
import com.campus.network.core.ui.theme.TextWhite
import com.campus.network.feature.messages.audio.VoiceFilterPreset
import com.campus.network.feature.messages.components.InCallMoreOptionsSheet
import com.campus.network.feature.messages.components.VoiceMorphSheet

@Composable
fun AudioCallScreen(
    callState: CallUiState,
    onEvent: (MessagesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMoreOptionsOpen by remember { mutableStateOf(false) }
    var isVoiceFilterOpen by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "AcousticPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070E18),
                        Color(0xFF0E1726),
                        Color(0xFF09111D)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Minimize (Left) | Peer Name & Encryption (Center) | Add Participant (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .clickable { onEvent(MessagesEvent.EndCall) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloseFullscreen,
                        contentDescription = "Minimize Call",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Peer Name, Call Status & End-to-end Encrypted indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = callState.peerName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (callState.status) {
                            CallStatus.CALLING -> "Calling"
                            CallStatus.RINGING -> "Ringing"
                            CallStatus.INCOMING -> "Incoming call"
                            CallStatus.CONNECTED -> formatCallDuration(callState.durationSeconds)
                            CallStatus.FAILED -> callState.connectionState.ifBlank { "Call ended" }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (callState.status == CallStatus.CONNECTED) BrandGreen else Color(0xCCFFFFFF)
                    )
                    if (callState.connectionState.isNotBlank() && callState.status != CallStatus.CONNECTED) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = callState.connectionState,
                            fontSize = 11.sp,
                            color = Color(0x99FFFFFF)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = Color(0xB3FFFFFF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "End-to-end encrypted",
                            fontSize = 12.sp,
                            color = Color(0xB3FFFFFF)
                        )
                    }

                    if (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDD2476).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF512F)),
                            modifier = Modifier.clickable { isVoiceFilterOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8DA1),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voice: ${callState.activeVoiceFilter.title}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8DA1)
                                )
                            }
                        }
                    }
                }

                // Add Participant Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .clickable { /* Add participant */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Participant",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Center: Prominent Large Circular Avatar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Subtle Ambient Wave Ring
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(BrandBlueAction.copy(alpha = pulseAlpha))
                )

                // Large Primary Avatar Circle (Mirroring reference layout)
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E2D44),
                                    Color(0xFF131D2E)
                                )
                            )
                        )
                        .border(3.dp, Color(0x3338BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState.peerInitials.isNotBlank()) {
                        Text(
                            text = callState.peerInitials,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = callState.peerName,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(110.dp)
                        )
                    }
                }
            }

            // 3. Bottom controls — Answer/Decline for incoming, otherwise in-call grid
            if (callState.status == CallStatus.INCOMING || callState.isIncoming) {
                IncomingCallActions(
                    onDecline = { onEvent(MessagesEvent.DeclineCall) },
                    onAccept = { onEvent(MessagesEvent.AcceptCall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF101924),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallGridButton(
                                icon = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                label = "Speaker",
                                isActive = callState.isSpeakerOn,
                                onClick = { onEvent(MessagesEvent.ToggleSpeaker) }
                            )

                            CallGridButton(
                                icon = if (callState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                label = "Video",
                                isActive = callState.isVideoEnabled,
                                onClick = { onEvent(MessagesEvent.ToggleVideo) }
                            )

                            CallGridButton(
                                icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (callState.isMuted) "Unmute" else "Mute",
                                isActive = callState.isMuted,
                                onClick = { onEvent(MessagesEvent.ToggleMute) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallGridButton(
                                icon = Icons.Default.MoreHoriz,
                                label = "More",
                                isActive = isMoreOptionsOpen || isVoiceFilterOpen || (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL),
                                onClick = { isMoreOptionsOpen = true }
                            )

                            EndCallButton(
                                onClick = { onEvent(MessagesEvent.EndCall) }
                            )

                            CallGridButton(
                                icon = Icons.Default.PresentToAll,
                                label = "Share",
                                isActive = false,
                                onClick = { }
                            )
                        }
                    }
                }
            }
        }

        // In-Call More Options Sheet
        if (isMoreOptionsOpen) {
            InCallMoreOptionsSheet(
                callState = callState,
                onOpenVoiceFilter = {
                    isMoreOptionsOpen = false
                    isVoiceFilterOpen = true
                },
                onDismiss = { isMoreOptionsOpen = false }
            )
        }

        // Dedicated Voice Morphing Sheet
        if (isVoiceFilterOpen) {
            VoiceMorphSheet(
                callState = callState,
                onEvent = onEvent,
                onDismiss = { isVoiceFilterOpen = false }
            )
        }
    }
}

@Composable
private fun CallGridButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isActive) BrandBlueAction else Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextWhite,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextWhite
        )
    }
}

@Composable
private fun EndCallButton(
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AlertRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = TextWhite,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "End",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextWhite
        )
    }
}

@Composable
fun IncomingCallActions(
    onDecline: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AlertRed)
                    .clickable(onClick = onDecline),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    tint = TextWhite,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Decline", fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(BrandGreen)
                    .clickable(onClick = onAccept),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Answer",
                    tint = TextWhite,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Answer", fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Medium)
        }
    }
}

fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
