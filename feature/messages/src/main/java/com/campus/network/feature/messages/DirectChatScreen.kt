package com.campus.network.feature.messages

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.campus.network.core.ui.components.TransactionSettlementCard
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.SurfaceWhite
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import com.campus.network.core.ui.theme.TextWhite
import com.campus.network.feature.messages.audio.OnDeviceTranscriber
import com.campus.network.feature.messages.audio.TranscriptionState

@Composable
fun DirectChatScreen(
    thread: ConversationThread,
    messages: List<DirectMessageItem>,
    isSettlementDialogOpen: Boolean,
    onEvent: (MessagesEvent) -> Unit,
    initialActivateVoice: Boolean = false,
    onConsumeVoiceActivation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transcriber = remember { OnDeviceTranscriber(context) }
    val transcriptionState by transcriber.state.collectAsState()
    val audioRms by transcriber.audioRms.collectAsState()
    val isSpeechDetected by transcriber.isSpeechDetected.collectAsState()
    var isVoiceRecordingActive by remember { mutableStateOf(false) }

    var inputText by remember { mutableStateOf("") }
    var draftAttachment by remember { mutableStateOf<Int?>(com.campus.network.core.ui.R.drawable.place_photo_1) }
    var showActionMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isVoiceRecordingActive = true
            transcriber.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            transcriber.destroy()
        }
    }

    // Trigger voice recognition automatically when coming from Voice Flow
    LaunchedEffect(initialActivateVoice) {
        if (initialActivateVoice) {
            onConsumeVoiceActivation()
            if (transcriber.hasRecordPermission()) {
                isVoiceRecordingActive = true
                transcriber.startListening()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Live update inputText with speech transcription in real time
    LaunchedEffect(transcriptionState) {
        when (val s = transcriptionState) {
            is TranscriptionState.PartialResult -> {
                inputText = s.text
            }
            is TranscriptionState.Success -> {
                inputText = s.text
            }
            else -> {}
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Floating Top App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.5.dp, Color(0x12000000)),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable {
                            if (isVoiceRecordingActive) {
                                transcriber.cancel()
                                isVoiceRecordingActive = false
                            }
                            onEvent(MessagesEvent.BackToThreads)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center: Circular Avatar with floating Name pill directly attached below
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* Conversation profile details */ }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (thread.avatarInitials.isNotEmpty()) thread.avatarInitials.take(1) else "J",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(0.5.dp, Color(0x18000000))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = thread.peerName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Video Call Button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.5.dp, Color(0x12000000)),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onEvent(MessagesEvent.StartVideoCall(thread)) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. Chat Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(messages, key = { it.id }) { message ->
                    if (message.settlementPayload != null) {
                        TransactionSettlementCard(
                            payload = message.settlementPayload,
                            onConfirmSettlement = { txId ->
                                onEvent(MessagesEvent.ConfirmSettlement(txId))
                            }
                        )
                    } else if (message.isSystem) {
                        Text(
                            text = message.text,
                            fontSize = 11.5.sp,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    } else {
                        DirectChatBubble(message = message)
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // 3. Optional Drafted Attachment Preview Card
            if (draftAttachment != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .size(135.dp)
                ) {
                    Image(
                        painter = painterResource(draftAttachment!!),
                        contentDescription = "Attached Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { draftAttachment = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // 4. Bottom Input Bar (Transforms into Waveform Animation Bar when Voice is active)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVoiceRecordingActive) {
                    // Transformed input bar matching referenced image:
                    // Solid circular blue microphone on left, soft pale-blue pill body, animated dancing waveform, and send action
                    VoiceWaveformInputBar(
                        isListening = isVoiceRecordingActive,
                        isSpeechDetected = isSpeechDetected,
                        audioRms = audioRms,
                        onStopListening = {
                            transcriber.stopListening()
                            isVoiceRecordingActive = false
                        },
                        onSend = {
                            transcriber.stopListening()
                            isVoiceRecordingActive = false
                            if (inputText.isNotBlank()) {
                                onEvent(MessagesEvent.SendMessage(inputText))
                                inputText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Standard Input Mode
                    // Circular '+' button on the left
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { showActionMenu = !showActionMenu }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "More Actions",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Popup quick actions menu
                        DropdownMenu(
                            expanded = showActionMenu,
                            onDismissRequest = { showActionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Send Escrow Settlement") },
                                leadingIcon = {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = BrandBluePrimary)
                                },
                                onClick = {
                                    showActionMenu = false
                                    onEvent(MessagesEvent.ToggleSettlementDialog(true))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Voice Call") },
                                leadingIcon = {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF007AFF))
                                },
                                onClick = {
                                    showActionMenu = false
                                    onEvent(MessagesEvent.StartAudioCall(thread))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attach Cookies Photo") },
                                leadingIcon = {
                                    Icon(Icons.Default.Photo, contentDescription = null, tint = Color(0xFF16A34A))
                                },
                                onClick = {
                                    showActionMenu = false
                                    draftAttachment = com.campus.network.core.ui.R.drawable.place_photo_1
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Pill Capsule Input Bar
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "Add comment or Send",
                                        fontSize = 15.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Normal
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (inputText.isNotBlank() || draftAttachment != null) {
                                                onEvent(MessagesEvent.SendMessage(inputText))
                                                inputText = ""
                                                draftAttachment = null
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (inputText.isEmpty() && draftAttachment == null) {
                                // Mic Button: activates voice dictation with waveform animation bar
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF007AFF))
                                        .clickable {
                                            if (transcriber.hasRecordPermission()) {
                                                isVoiceRecordingActive = true
                                                transcriber.startListening()
                                            } else {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            } else {
                                // Blue Circular Send Button with Up Arrow
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF007AFF))
                                        .clickable {
                                            if (inputText.isNotBlank() || draftAttachment != null) {
                                                onEvent(MessagesEvent.SendMessage(inputText))
                                                inputText = ""
                                                draftAttachment = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(90f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Send Settlement Modal
        if (isSettlementDialogOpen) {
            SendSettlementDialog(
                peerName = thread.peerName,
                onDismiss = { onEvent(MessagesEvent.ToggleSettlementDialog(false)) },
                onSend = { amount, purpose ->
                    onEvent(MessagesEvent.InitiateSettlement(amount, purpose))
                }
            )
        }
    }
}

/**
 * Transformed Voice Waveform Input Bar conforming to referenced image:
 * Solid vibrant blue circular mic button on left, pale sky-blue pill body,
 * and rhythmically animated vertical frequency waveform bars and dots.
 */
@Composable
private fun VoiceWaveformInputBar(
    isListening: Boolean,
    isSpeechDetected: Boolean,
    audioRms: Float,
    onStopListening: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFD6E8FD),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Solid Vibrant Blue Circular Mic Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .clickable(onClick = onStopListening),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Stop Voice Dictation",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center Dynamic Animated Audio Waveform
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                WhatsAppRealtimeWaveform(
                    isRecording = isListening,
                    isSpeechDetected = isSpeechDetected,
                    audioRms = audioRms
                )
            }

            // Right Send Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f)
                )
            }
        }
    }
}

/**
 * Real-time WhatsApp-style audio visualization during voice dictation / recording.
 *
 * In calm state (silence / no speech detected):
 * Displays an evenly spaced sequence of discrete vertical rounded bars at minimum resting height (3.5dp)
 * running across the pill capsule. Never draws a continuous horizontal baseline line end-to-end.
 *
 * When speech is detected:
 * Live vocal amplitude slices push into a rolling queue every ~60ms. Spoken voice causes the
 * vertical bars to dynamically expand up to 26dp in height and stream smoothly across, matching
 * WhatsApp's native waveform scrolling visualizer.
 */
@Composable
private fun WhatsAppRealtimeWaveform(
    isRecording: Boolean,
    isSpeechDetected: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 34,
    color: Color = Color(0xFF007AFF)
) {
    val currentSpeechDetected by rememberUpdatedState(isSpeechDetected)
    val currentRms by rememberUpdatedState(audioRms)

    val liveAmplitudes = remember {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0f) }
        }
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            for (i in 0 until liveAmplitudes.size) {
                liveAmplitudes[i] = 0f
            }
            return@LaunchedEffect
        }

        while (isActive && isRecording) {
            val targetAmp = if (currentSpeechDetected) {
                val baseAmp = (0.22f + currentRms * 0.78f).coerceIn(0.22f, 1f)
                val flutter = (kotlin.random.Random.nextFloat() * 0.16f) - 0.08f
                (baseAmp + flutter).coerceIn(0.15f, 1f)
            } else {
                0f
            }

            if (liveAmplitudes.size >= barCount) {
                liveAmplitudes.removeAt(0)
            }
            liveAmplitudes.add(targetAmp)

            delay(60L)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val totalBars = liveAmplitudes.size
        if (totalBars == 0) return@Canvas

        val barWidthPx = 2.8.dp.toPx()
        val minBarHeightPx = 3.5.dp.toPx()
        val maxBarHeightPx = 26.dp.toPx()
        val centerY = size.height / 2f

        val totalBarWidth = totalBars * barWidthPx
        val spacing = if (totalBars > 1) {
            ((size.width - totalBarWidth) / (totalBars - 1)).coerceAtLeast(1.5f)
        } else {
            0f
        }

        for (i in 0 until totalBars) {
            val amp = liveAmplitudes[i]
            val barHeight = minBarHeightPx + (maxBarHeightPx - minBarHeightPx) * amp
            val x = i * (barWidthPx + spacing) + barWidthPx / 2f

            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Message Bubble conforming to the Apple Messages style.
 */
@Composable
private fun DirectChatBubble(
    message: DirectMessageItem
) {
    if (message.isHandwritten) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = message.text,
                fontFamily = FontFamily.Cursive,
                fontSize = 46.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(end = 8.dp)
            )
            if (message.isDelivered) {
                Text(
                    text = "Delivered",
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                )
            }
        }
    } else if (message.isMine) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (message.replyToText != null) {
                Text(
                    text = "Replay",
                    fontSize = 11.5.sp,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = Color(0xFF007AFF),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }

            if (message.isEdited) {
                Text(
                    text = "Edited",
                    fontSize = 11.5.sp,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                )
            } else if (message.isDelivered) {
                Text(
                    text = "Delivered",
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
                color = Color(0xFFE9E9EB),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFF0F172A),
                    fontSize = 15.5.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun SendSettlementDialog(
    peerName: String,
    onDismiss: () -> Unit,
    onSend: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Send Peer Escrow Settlement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Funds remain locked in campus smart-escrow for $peerName until delivery is verified.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (KES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose (e.g. Graphic design, Tutoring)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && purpose.isNotBlank()) {
                                onSend(amount, purpose.trim())
                            }
                        }
                    ) {
                        Text("Lock & Send", fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                    }
                }
            }
        }
    }
}
