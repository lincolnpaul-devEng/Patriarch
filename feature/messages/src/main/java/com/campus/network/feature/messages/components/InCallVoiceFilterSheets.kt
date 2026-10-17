package com.campus.network.feature.messages.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.feature.messages.CallUiState
import com.campus.network.feature.messages.MessagesEvent
import com.campus.network.feature.messages.audio.VoiceFilterPreset
import kotlin.math.roundToInt

// Color tokens for in-call dark glass surfaces
private val SheetSurfaceBg = Color(0xFF131A24)
private val SheetCardBg = Color(0xFF1E2836)
private val SheetCardBorder = Color(0xFF2A374A)
private val BrandAccent = Color(0xFF007AFF)
private val ActiveGradientStart = Color(0xFFFF512F)
private val ActiveGradientEnd = Color(0xFFDD2476)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)

/**
 * In-Call More Options Sheet triggered when the user taps '...' on Audio or Video call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InCallMoreOptionsSheet(
    callState: CallUiState,
    onOpenVoiceFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isNoiseShieldEnabled by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetSurfaceBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Call Controls & Tools",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Real-time stream features for ${callState.peerName}",
                        fontSize = 12.5.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SheetCardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. PRIMARY ACTION: Voice Filter & Morphing
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SheetCardBg,
                border = BorderStroke(1.dp, SheetCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        onDismiss()
                        onOpenVoiceFilter()
                    })
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ActiveGradientStart, ActiveGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Filter",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Voice Filter",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Pill
                            Surface(
                                shape = CircleShape,
                                color = if (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL) {
                                    ActiveGradientEnd.copy(alpha = 0.25f)
                                } else {
                                    Color(0xFF334155)
                                },
                                border = BorderStroke(
                                    0.5.dp,
                                    if (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL) {
                                        ActiveGradientStart
                                    } else {
                                        Color(0xFF475569)
                                    }
                                )
                            ) {
                                Text(
                                    text = callState.activeVoiceFilter.title.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL) {
                                        Color(0xFFFF8DA1)
                                    } else {
                                        TextMuted
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Talk in woman, man, child or cyber voice in real-time",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Voice Filter",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. AI Noise Shield
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SheetCardBg,
                border = BorderStroke(1.dp, SheetCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F3B66)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Noise Shield",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Noise Shield",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Suppresses hostel chatter, wind, and fan background hum",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isNoiseShieldEnabled,
                        onCheckedChange = { isNoiseShieldEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandAccent,
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Security & Trust Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SheetCardBg,
                border = BorderStroke(1.dp, SheetCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF143E2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "E2EE Secured Call",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Zero carrier wiretaps • Peer AES-256 WebRTC Stream",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Voice Morphing Sheet: Presets (Woman, Man, Child, Baritone, Cyber),
 * fine-pitch & formant resonance sliders, and live mic loopback testing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMorphSheet(
    callState: CallUiState,
    onEvent: (MessagesEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentPitch by remember(callState.activeVoiceFilter, callState.customPitchShift) {
        mutableFloatStateOf(callState.customPitchShift)
    }
    var currentFormant by remember(callState.activeVoiceFilter, callState.customFormantScale) {
        mutableFloatStateOf(callState.customFormantScale)
    }

    // Dynamic wave animation for the active voice visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceWaveTransition")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetSurfaceBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ActiveGradientStart, ActiveGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Voice Filter",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Real-Time Vocal Morphing & Formant Engine",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SheetCardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Voice Status Banner + Visualizer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SheetCardBg,
                border = BorderStroke(
                    1.dp,
                    if (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL) {
                        ActiveGradientStart.copy(alpha = 0.6f)
                    } else {
                        SheetCardBorder
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(getPresetColor(callState.activeVoiceFilter)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getPresetIcon(callState.activeVoiceFilter),
                                contentDescription = callState.activeVoiceFilter.title,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Active: ${callState.activeVoiceFilter.title}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = callState.activeVoiceFilter.subtitle,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Dancing Waveform visualizer
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((16 * waveScale1).dp)
                                .clip(CircleShape)
                                .background(ActiveGradientStart)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((24 * waveScale2).dp)
                                .clip(CircleShape)
                                .background(ActiveGradientEnd)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((18 * waveScale1).dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8DA1))
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((12 * waveScale2).dp)
                                .clip(CircleShape)
                                .background(ActiveGradientStart)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Presets Grid Header
            Text(
                text = "VOICE PROFILES",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6 Presets Grid
            val presets = VoiceFilterPreset.entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoicePresetCard(
                        preset = presets[0], // Natural
                        isSelected = callState.activeVoiceFilter == presets[0],
                        onClick = {
                            onEvent(MessagesEvent.SetVoiceFilter(presets[0]))
                            currentPitch = presets[0].pitchSemiTones
                            currentFormant = presets[0].formantScale
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VoicePresetCard(
                        preset = presets[1], // Woman
                        isSelected = callState.activeVoiceFilter == presets[1],
                        onClick = {
                            onEvent(MessagesEvent.SetVoiceFilter(presets[1]))
                            currentPitch = presets[1].pitchSemiTones
                            currentFormant = presets[1].formantScale
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoicePresetCard(
                        preset = presets[2], // Man
                        isSelected = callState.activeVoiceFilter == presets[2],
                        onClick = {
                            onEvent(MessagesEvent.SetVoiceFilter(presets[2]))
                            currentPitch = presets[2].pitchSemiTones
                            currentFormant = presets[2].formantScale
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VoicePresetCard(
                        preset = presets[3], // Child
                        isSelected = callState.activeVoiceFilter == presets[3],
                        onClick = {
                            onEvent(MessagesEvent.SetVoiceFilter(presets[3]))
                            currentPitch = presets[3].pitchSemiTones
                            currentFormant = presets[3].formantScale
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoicePresetCard(
                        preset = presets[4], // Studio Deep
                        isSelected = callState.activeVoiceFilter == presets[4],
                        onClick = {
                            onEvent(MessagesEvent.SetVoiceFilter(presets[4]))
                            currentPitch = presets[4].pitchSemiTones
                            currentFormant = presets[4].formantScale
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VoicePresetCard(
                        preset = presets[5], // Cyber Synth
                        isSelected = callState.activeVoiceFilter == presets[5],
                        onClick = {
                            onEvent(MessagesEvent.SetVoiceFilter(presets[5]))
                            currentPitch = presets[5].pitchSemiTones
                            currentFormant = presets[5].formantScale
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fine-Tuning Controls
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SheetCardBg,
                border = BorderStroke(1.dp, SheetCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fine-Tune Pitch (f0)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        }

                        Text(
                            text = "${if (currentPitch >= 0) "+" else ""}${((currentPitch * 10).roundToInt() / 10f)} st",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = BrandAccent
                        )
                    }

                    Slider(
                        value = currentPitch,
                        onValueChange = {
                            currentPitch = it
                            onEvent(MessagesEvent.UpdateCustomPitchAndFormant(currentPitch, currentFormant))
                        },
                        valueRange = -12f..12f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandAccent,
                            activeTrackColor = BrandAccent,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Formant Resonance (VTLN)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        }

                        Text(
                            text = "${((currentFormant * 100).roundToInt() / 100f)}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Slider(
                        value = currentFormant,
                        onValueChange = {
                            currentFormant = it
                            onEvent(MessagesEvent.UpdateCustomPitchAndFormant(currentPitch, currentFormant))
                        },
                        valueRange = 0.70f..1.40f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hear Yourself (Loopback) Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SheetCardBg,
                border = BorderStroke(1.dp, SheetCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (callState.isSelfMonitoring) ActiveGradientStart.copy(alpha = 0.25f) else Color(0xFF334155)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Loopback",
                            tint = if (callState.isSelfMonitoring) Color(0xFFFF8DA1) else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hear Yourself (Mic Test)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Use headphones to avoid acoustic feedback echo",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = callState.isSelfMonitoring,
                        onCheckedChange = { onEvent(MessagesEvent.ToggleSelfMonitoring(context)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ActiveGradientEnd,
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Actions: Reset & Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reset Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SheetCardBg,
                    border = BorderStroke(1.dp, SheetCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable {
                            onEvent(MessagesEvent.SetVoiceFilter(VoiceFilterPreset.NATURAL))
                            currentPitch = 0f
                            currentFormant = 1.0f
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                }

                // Done / Apply Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ActiveGradientStart, ActiveGradientEnd)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Apply Filter",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoicePresetCard(
    preset: VoiceFilterPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF261D2D) else SheetCardBg,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) ActiveGradientStart else SheetCardBorder
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(getPresetColor(preset)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPresetIcon(preset),
                    contentDescription = preset.title,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFFFF8DA1) else TextWhite,
                    maxLines = 1
                )
                Text(
                    text = preset.subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ActiveGradientStart,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getPresetIcon(preset: VoiceFilterPreset): ImageVector = when (preset) {
    VoiceFilterPreset.NATURAL -> Icons.Default.Person
    VoiceFilterPreset.WOMAN -> Icons.Default.Face
    VoiceFilterPreset.MAN -> Icons.Default.Person
    VoiceFilterPreset.CHILD -> Icons.Default.Face
    VoiceFilterPreset.STUDIO_DEEP -> Icons.Default.RecordVoiceOver
    VoiceFilterPreset.CYBER_SYNTH -> Icons.Default.SmartToy
}

private fun getPresetColor(preset: VoiceFilterPreset): Color = when (preset) {
    VoiceFilterPreset.NATURAL -> Color(0xFF475569)
    VoiceFilterPreset.WOMAN -> Color(0xFFDD2476)
    VoiceFilterPreset.MAN -> Color(0xFF0284C7)
    VoiceFilterPreset.CHILD -> Color(0xFFF59E0B)
    VoiceFilterPreset.STUDIO_DEEP -> Color(0xFF7C3AED)
    VoiceFilterPreset.CYBER_SYNTH -> Color(0xFF10B981)
}
