package com.campus.network.feature.messages

import android.Manifest
import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campus.network.core.ui.components.HybridEmojiReactionDrawer
import com.campus.network.core.ui.components.KeyboardMediaTextField
import com.campus.network.core.ui.components.TransactionSettlementCard
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.SurfaceWhite
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import com.campus.network.feature.messages.audio.OnDeviceTranscriber
import com.campus.network.feature.messages.audio.TranscriptionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectChatScreen(
    thread: ConversationThread,
    messages: List<DirectMessageItem>,
    isSettlementDialogOpen: Boolean,
    onEvent: (MessagesEvent) -> Unit,
    initialActivateVoice: Boolean = false,
    onConsumeVoiceActivation: () -> Unit = {},
    /** Real unread across other threads — shown on back only when > 0. */
    otherThreadsUnreadCount: Int = 0,
    /** Green "X is typing…" under the chat title when the peer is composing. */
    typingLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val transcriber = remember { OnDeviceTranscriber(context) }
    val transcriptionState by transcriber.state.collectAsState()
    val audioRms by transcriber.audioRms.collectAsState()
    val isSpeechDetected by transcriber.isSpeechDetected.collectAsState()
    var isVoiceRecordingActive by remember { mutableStateOf(false) }

    var inputText by remember { mutableStateOf("") }
    var draftAttachment by remember { mutableStateOf<Int?>(null) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isEmojiDrawerOpen by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }

    // Production States
    var isProfileSheetOpen by remember { mutableStateOf(false) }
    var lightboxMediaUri by remember { mutableStateOf<String?>(null) }
    var replyingToMessage by remember { mutableStateOf<DirectMessageItem?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<DirectMessageItem?>(null) }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val resolvedTypingLabel = typingLabel ?: thread.typingLabel

    // Publish typing presence while composer has text
    LaunchedEffect(inputText) {
        val typing = inputText.isNotBlank()
        onEvent(MessagesEvent.SetDirectChatTyping(typing))
        if (typing) {
            while (isActive && inputText.isNotBlank()) {
                delay(2_000L)
                onEvent(MessagesEvent.SetDirectChatTyping(true))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onEvent(MessagesEvent.SetDirectChatTyping(false))
        }
    }

    BackHandler(enabled = isEmojiDrawerOpen || isVoiceRecordingActive) {
        if (isVoiceRecordingActive) {
            transcriber.cancel()
            isVoiceRecordingActive = false
        } else if (isEmojiDrawerOpen) {
            isEmojiDrawerOpen = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
        }
    }

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

    // Scroll to bottom (top of inverted LazyColumn) on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val networkState = com.campus.network.core.ui.components.rememberCampusNetworkState()

    LaunchedEffect(networkState.isOnline) {
        if (networkState.isOnline) {
            onEvent(MessagesEvent.FlushPendingMessages)
        }
    }

    val reversedMessages = remember(messages) { messages.asReversed() }

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
                // Back button with real unread badge (hidden when zero)
                Box {
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
                    if (otherThreadsUnreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDA373C))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (otherThreadsUnreadCount > 99) {
                                    "99+"
                                } else {
                                    "$otherThreadsUnreadCount"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // Center: Circular Avatar with floating Name pill & Online Presence
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { isProfileSheetOpen = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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
                            fontSize = 17.sp
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
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = thread.peerName,
                                fontSize = 12.sp,
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
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Typing / Online / Offline indicator under chat title
                    if (!networkState.isOnline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEAB308))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Waiting for network...",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD97706)
                            )
                        }
                    } else if (!resolvedTypingLabel.isNullOrBlank()) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = resolvedTypingLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF23A559),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Online",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }

                // Right Top Bar: Side-by-Side Voice Call and Video Call Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Call Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 3.dp,
                        border = BorderStroke(0.5.dp, Color(0x12000000)),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onEvent(MessagesEvent.StartAudioCall(thread)) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Voice Call",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
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
            }

            // 2. Chat Message Stream (Inverted LazyColumn matching WhatsApp/Telegram standards)
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                items(reversedMessages, key = { it.id }) { message ->
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
                        DirectChatBubble(
                            message = message,
                            onMediaClick = { uri -> lightboxMediaUri = uri },
                            onLongClick = { selectedMessageForMenu = message }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // 3. Reply Preview Bar (Renders directly above bottom input when replying)
            AnimatedVisibility(
                visible = replyingToMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                replyingToMessage?.let { target ->
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(Color(0xFF007AFF), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (target.isMine) "You" else thread.peerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF007AFF)
                                )
                                Text(
                                    text = target.text.ifBlank { "Attachment" },
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                                    .clickable { replyingToMessage = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            val bottomInsetsModifier = if (isEmojiDrawerOpen) {
                Modifier.windowInsetsPadding(WindowInsets.ime)
            } else {
                Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            }

            // 4. Bottom Input Bar (Transforms into Waveform Animation Bar when Voice is active)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(bottomInsetsModifier)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVoiceRecordingActive) {
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
                                onEvent(
                                    MessagesEvent.SendMessage(
                                        text = inputText.trim(),
                                        replyToText = replyingToMessage?.text
                                    )
                                )
                                inputText = ""
                                replyingToMessage = null
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
                                text = { Text("Photo Gallery") },
                                leadingIcon = {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF16A34A))
                                },
                                onClick = {
                                    showActionMenu = false
                                    photoPickerLauncher.launch("image/*")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Integrated Input Window with Embedded Media Preview
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            // Embedded Media Preview Chip inside input window
                            if (selectedMediaUri != null || draftAttachment != null) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 2.dp, top = 2.dp, bottom = 6.dp)
                                        .size(68.dp)
                                    ) {
                                    if (selectedMediaUri != null) {
                                        AsyncImage(
                                            model = selectedMediaUri,
                                            contentDescription = "Selected media",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFF1F5F9))
                                        )
                                    } else if (draftAttachment != null) {
                                        Image(
                                            painter = painterResource(draftAttachment!!),
                                            contentDescription = "Draft photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable {
                                                draftAttachment = null
                                                selectedMediaUri = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 36.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Emoji & GIF Access Button
                                Icon(
                                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                                    contentDescription = "Reactions & GIFs",
                                    tint = if (isEmojiDrawerOpen) Color(0xFF007AFF) else Color(0xFF64748B),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable {
                                            if (!isEmojiDrawerOpen) {
                                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                                keyboardController?.hide()
                                                isEmojiDrawerOpen = true
                                            } else {
                                                isEmojiDrawerOpen = false
                                            }
                                        }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Rich KeyboardMediaTextField with Gboard GIF/image commit support
                                KeyboardMediaTextField(
                                    value = inputText,
                                    onValueChange = {
                                        inputText = it
                                        if (isEmojiDrawerOpen) {
                                            isEmojiDrawerOpen = false
                                        }
                                    },
                                    hint = "Add comment or Send",
                                    onMediaReceived = { uri ->
                                        selectedMediaUri = uri
                                        if (isEmojiDrawerOpen) {
                                            isEmojiDrawerOpen = false
                                        }
                                    },
                                    onSend = {
                                        if (inputText.isNotBlank() || selectedMediaUri != null || draftAttachment != null) {
                                            onEvent(
                                                MessagesEvent.SendMessage(
                                                    text = inputText.trim(),
                                                    imageUri = selectedMediaUri?.toString(),
                                                    replyToText = replyingToMessage?.text
                                                )
                                            )
                                            inputText = ""
                                            selectedMediaUri = null
                                            draftAttachment = null
                                            replyingToMessage = null
                                            isEmojiDrawerOpen = false
                                        }
                                    },
                                    onFocusRequested = {
                                        if (isEmojiDrawerOpen) {
                                            isEmojiDrawerOpen = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 34.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                if (inputText.isEmpty() && selectedMediaUri == null && draftAttachment == null) {
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
                                                if (inputText.isNotBlank() || selectedMediaUri != null || draftAttachment != null) {
                                                    onEvent(
                                                        MessagesEvent.SendMessage(
                                                            text = inputText.trim(),
                                                            imageUri = selectedMediaUri?.toString(),
                                                            replyToText = replyingToMessage?.text
                                                        )
                                                    )
                                                    inputText = ""
                                                    selectedMediaUri = null
                                                    draftAttachment = null
                                                    replyingToMessage = null
                                                    isEmojiDrawerOpen = false
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

            // 5. Drawer for Direct Chat (docks at bottom of screen below input bar)
            AnimatedVisibility(
                visible = isEmojiDrawerOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                HybridEmojiReactionDrawer(
                    onEmojiPicked = { emoji ->
                        inputText += emoji
                    },
                    onGifPicked = { gif ->
                        onEvent(
                            MessagesEvent.SendMessage(
                                text = "",
                                gifUrl = gif.originalUrl,
                                replyToText = replyingToMessage?.text
                            )
                        )
                        replyingToMessage = null
                        isEmojiDrawerOpen = false
                    }
                )
            }
        }

        // Fullscreen Lightbox Media Viewer Modal
        lightboxMediaUri?.let { uri ->
            MediaLightboxDialog(
                mediaUri = uri,
                onDismiss = { lightboxMediaUri = null }
            )
        }

        // Peer Contact Profile Sheet
        if (isProfileSheetOpen) {
            PeerProfileBottomSheet(
                thread = thread,
                onDismiss = { isProfileSheetOpen = false },
                onVoiceCall = { onEvent(MessagesEvent.StartAudioCall(thread)) },
                onVideoCall = { onEvent(MessagesEvent.StartVideoCall(thread)) },
                onClearChat = { onEvent(MessagesEvent.ClearChat(thread.id)) }
            )
        }

        // Long-press Actions & Reactions Sheet
        selectedMessageForMenu?.let { targetMsg ->
            MessageActionBottomSheet(
                message = targetMsg,
                onDismiss = { selectedMessageForMenu = null },
                onReactionSelected = { reactionKey ->
                    onEvent(MessagesEvent.ToggleMessageReaction(targetMsg.id, reactionKey))
                },
                onReplySelected = {
                    replyingToMessage = targetMsg
                },
                onDeleteSelected = {
                    onEvent(MessagesEvent.DeleteMessage(targetMsg.id))
                }
            )
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
 * Message Receipt Tick rendering WhatsApp / Telegram delivery and read receipts:
 * - Pending / queued (offline): clock icon
 * - Sent: single tick
 * - Delivered: double gray tick
 * - Read: double cyan / blue tick
 */
@Composable
private fun MessageReceiptTick(
    isPending: Boolean = false,
    isDelivered: Boolean,
    isRead: Boolean,
    tint: Color
) {
    if (isPending) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Pending / Queued",
            tint = tint.copy(alpha = 0.75f),
            modifier = Modifier.size(12.dp)
        )
    } else if (!isDelivered && !isRead) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Sent",
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
    } else {
        val tickTint = if (isRead) Color(0xFF60A5FA) else tint
        Box(
            modifier = Modifier.width(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = tickTint,
                modifier = Modifier.size(13.dp)
            )
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isRead) "Read" else "Delivered",
                tint = tickTint,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(13.dp)
            )
        }
    }
}

/**
 * Renders vector reaction indicator badges adhering strictly to ZERO EMOJI POLICY.
 */
@Composable
private fun ReactionVectorIcon(reaction: String, size: Int = 14) {
    when (reaction) {
        "THUMBS_UP" -> Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = Color(0xFF007AFF), modifier = Modifier.size(size.dp))
        "HEART" -> Icon(Icons.Default.Favorite, contentDescription = "Love", tint = Color(0xFFEF4444), modifier = Modifier.size(size.dp))
        "STAR" -> Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFF59E0B), modifier = Modifier.size(size.dp))
        "CHECK" -> Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF10B981), modifier = Modifier.size(size.dp))
        else -> Icon(Icons.Default.ThumbUp, contentDescription = "Reaction", tint = Color(0xFF007AFF), modifier = Modifier.size(size.dp))
    }
}

/**
 * Formats epoch timestamp into clean local time (e.g. "10:32 AM").
 */
private fun formatMessageTime(epochMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(epochMs))
    } catch (_: Exception) {
        "Just now"
    }
}

/**
 * Message Bubble conforming to production messaging standards (Apple Messages / WhatsApp).
 * Supports timestamps, compact receipts, full-screen image viewing, and long-press actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectChatBubble(
    message: DirectMessageItem,
    onMediaClick: (String) -> Unit,
    onLongClick: () -> Unit
) {
    val timeString = formatMessageTime(message.timestampEpochMs)
    val mediaData = message.imageUri ?: message.imageUrl ?: message.gifUrl ?: message.imageResId?.toString()

    if (message.isHandwritten) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                ),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp, top = 2.dp)
            ) {
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.width(4.dp))
                MessageReceiptTick(
                    isPending = message.isPending,
                    isDelivered = message.isDelivered,
                    isRead = message.isRead,
                    tint = Color(0xFF8E8E93)
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
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = Color(0xFF007AFF),
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .combinedClickable(
                        onClick = {
                            if (mediaData != null) {
                                onMediaClick(mediaData)
                            }
                        },
                        onLongClick = onLongClick
                    )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Quoted replied message preview
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x2EFFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.replyToText,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Media Attachment
                    if (mediaData != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaData)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                        )
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Text & Status Row
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Bottom info: Time + Receipt Checkmarks
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageReceiptTick(
                            isPending = message.isPending,
                            isDelivered = message.isDelivered,
                            isRead = message.isRead,
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Reaction Badge docked below bubble
            if (message.reaction != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .padding(top = 2.dp, end = 6.dp)
                        .clickable(onClick = onLongClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReactionVectorIcon(message.reaction, size = 12)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "1",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    } else {
        // Incoming message from peer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
                color = Color(0xFFE9E9EB),
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .combinedClickable(
                        onClick = {
                            if (mediaData != null) {
                                onMediaClick(mediaData)
                            }
                        },
                        onLongClick = onLongClick
                    )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Quoted replied message preview
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x1F000000),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(Color(0xFF007AFF), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.replyToText,
                                    color = Color(0xFF0F172A),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Media Attachment
                    if (mediaData != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaData)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE2E8F0))
                        )
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Text & Status Row
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Bottom info: Time
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            fontSize = 10.5.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }

            // Reaction Badge docked below bubble
            if (message.reaction != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .padding(top = 2.dp, start = 6.dp)
                        .clickable(onClick = onLongClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReactionVectorIcon(message.reaction, size = 12)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "1",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen Lightbox Dialog for displaying pictures and GIFs.
 */
@Composable
private fun MediaLightboxDialog(
    mediaUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Fullscreen preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )

            // Close button top-right
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .clickable(onClick = onDismiss)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Long-press Action Sheet with quick reactions dock and contextual options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionBottomSheet(
    message: DirectMessageItem,
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit,
    onReplySelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // 1. Quick Reactions Dock (Strictly Vector Icons under ZERO EMOJI POLICY)
            Text(
                text = "Quick Reactions",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    "THUMBS_UP" to Icons.Default.ThumbUp to Color(0xFF007AFF),
                    "HEART" to Icons.Default.Favorite to Color(0xFFEF4444),
                    "STAR" to Icons.Default.Star to Color(0xFFF59E0B),
                    "CHECK" to Icons.Default.CheckCircle to Color(0xFF10B981)
                ).forEach { item ->
                    val (keyAndIcon, color) = item
                    val (reactionKey, icon) = keyAndIcon
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                onReactionSelected(reactionKey)
                                onDismiss()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = reactionKey,
                                tint = color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Action Menu items: Reply
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onReplySelected()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Reply",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Reply",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
            }

            // Copy Text
            if (message.text.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(message.text))
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Copy",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Copy Text",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Delete (if mine)
            if (message.isMine) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeleteSelected()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Delete Message",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

/**
 * Peer Contact Profile Sheet (WhatsApp / Telegram style peer overview).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeerProfileBottomSheet(
    thread: ConversationThread,
    onDismiss: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onClearChat: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large circular avatar
            Box(
                modifier = Modifier
                    .size(76.dp)
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
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = thread.peerName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Online • Campus Verified Student",
                    fontSize = 12.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
            }

            if (thread.serverBadgeText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = thread.serverBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Quick Buttons: Voice Call, Video Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onDismiss()
                        onVoiceCall()
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE0E7FF),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Audio Call", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onDismiss()
                        onVideoCall()
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDBEAFE),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Video Call", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Clear chat button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClearChat()
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Chat History", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Transformed Voice Waveform Input Bar:
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
 */
@Composable
private fun WhatsAppRealtimeWaveform(
    isRecording: Boolean,
    isSpeechDetected: Boolean,
    audioRms: Float,
    barCount: Int = 38,
    color: Color = Color(0xFF007AFF)
) {
    val liveAmplitudes = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.05f) } } }
    val currentRms by rememberUpdatedState(audioRms)
    val currentSpeechDetected by rememberUpdatedState(isSpeechDetected)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        var step = 0
        while (isActive) {
            val nextAmp: Float = if (currentSpeechDetected) {
                val normalized = ((currentRms + 2f) / 12f).coerceIn(0.15f, 1.0f)
                val variance = kotlin.random.Random.nextFloat() * 0.25f - 0.12f
                (normalized + variance).coerceIn(0.1f, 1.0f)
            } else {
                val wave = kotlin.math.sin((step * 0.2f).toDouble()).toFloat() * 0.03f
                (idlePulse + wave).coerceIn(0.04f, 0.15f)
            }

            if (liveAmplitudes.size >= barCount) {
                liveAmplitudes.removeAt(0)
            }
            liveAmplitudes.add(nextAmp)
            step++
            delay(55L)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalBars = liveAmplitudes.size
        if (totalBars == 0) return@Canvas

        val barWidthPx = 3.dp.toPx()
        val minBarHeightPx = 4.dp.toPx()
        val maxBarHeightPx = (size.height * 0.78f).coerceAtLeast(minBarHeightPx + 4f)

        val totalAvailableWidth = size.width
        val spacing = if (totalBars > 1) {
            ((totalAvailableWidth - (totalBars * barWidthPx)) / (totalBars - 1)).coerceAtLeast(2.dp.toPx())
        } else 0f

        val centerY = size.height / 2f

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
                    text = "Send Safe Payment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Money is held safely for $peerName until you confirm the pickup.",
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
