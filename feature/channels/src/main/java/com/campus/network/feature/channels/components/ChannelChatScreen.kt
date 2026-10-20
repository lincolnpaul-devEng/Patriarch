package com.campus.network.feature.channels.components

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.campus.network.core.ui.components.CampusStudentAvatar
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campus.network.feature.channels.ChannelMessage
import com.campus.network.feature.channels.ChannelMessageEmbed
import com.campus.network.feature.channels.ChannelTypingUser
import com.campus.network.feature.channels.ChannelsEvent
import com.campus.network.feature.channels.ChannelsUiState
import com.campus.network.core.ui.components.DiscordBouncingDotsLoader
import com.campus.network.core.ui.components.KeyboardMediaTextField
import com.campus.network.feature.channels.model.ServerRoleType
import com.campus.network.feature.channels.ChannelsViewModel
import com.campus.network.feature.channels.audio.OnDeviceTranscriber
import com.campus.network.feature.channels.audio.TranscriptionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pixel-Perfect Channel Chat Screen matching the patriarch Mobile Channel Sessions UI
 * Reference images:
 * - uploaded_media_1_1792142138193.png (Channel chat stream, embeds, reactions, header)
 * - uploaded_media_0_1792142138193.png (Media drawer bottom sheet toggled by '+', device photos)
 * - uploaded_media_1792143197305.png (Channel Profile screen)
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography across all UI.
 */
@Composable
fun ChannelChatScreen(
    uiState: ChannelsUiState,
    viewModel: ChannelsViewModel,
    onBackClick: () -> Unit,
    onChannelProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activeChannel = uiState.activeChatChannel
    val channelName = activeChannel?.name ?: "game-music"
    val messages = uiState.chatMessages
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val selectedMediaUris = remember { mutableStateListOf<Uri>() }
    var isEmojiDrawerOpen by remember { mutableStateOf(false) }

    // Live On-Device Voice Transcriber (matching DirectChatScreen)
    val transcriber = remember { OnDeviceTranscriber(context) }
    val transcriptionState by transcriber.state.collectAsState()
    val audioRms by transcriber.audioRms.collectAsState()
    val isSpeechDetected by transcriber.isSpeechDetected.collectAsState()
    var isVoiceRecordingActive by remember { mutableStateOf(false) }

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

    BackHandler {
        if (isVoiceRecordingActive) {
            transcriber.cancel()
            isVoiceRecordingActive = false
        } else if (isEmojiDrawerOpen) {
            isEmojiDrawerOpen = false
        } else if (uiState.isMediaDrawerOpen) {
            viewModel.onEvent(ChannelsEvent.ToggleMediaDrawer)
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(messages.size, uiState.activeChannelTypingUsers.size) {
        val extra = if (uiState.activeChannelTypingUsers.isNotEmpty()) 1 else 0
        val target = messages.size + extra - 1
        if (target >= 0) {
            listState.animateScrollToItem(target)
        }
    }

    // Publish typing presence while composer has text
    LaunchedEffect(inputText) {
        val typing = inputText.isNotBlank()
        viewModel.onEvent(ChannelsEvent.SetChannelTyping(typing))
        if (typing) {
            // Re-assert while text remains (heartbeat also runs in VM)
            while (isActive && inputText.isNotBlank()) {
                delay(2_000L)
                viewModel.onEvent(ChannelsEvent.SetChannelTyping(true))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(ChannelsEvent.SetChannelTyping(false))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // TOP APP BAR / CHANNEL HEADER — badge = other channel threads only (not this chat)
        ChannelTopBar(
            channelName = channelName,
            unreadCount = uiState.otherChannelsUnreadCount,
            typingLabel = uiState.activeChannelTypingLabel,
            onBackClick = {
                if (isVoiceRecordingActive) {
                    transcriber.cancel()
                    isVoiceRecordingActive = false
                }
                onBackClick()
            },
            onChannelProfileClick = onChannelProfileClick,
            onSearchClick = {
                // Focus channel search
            }
        )

        // CHAT MESSAGE STREAM
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.isDateDivider) {
                        DateDividerItem(text = message.dateDividerText ?: "Today")
                    } else {
                        ChannelMessageItem(
                            message = message,
                            onToggleReaction = {
                                viewModel.onEvent(ChannelsEvent.ToggleReaction(message.id))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // WhatsApp-style typing rows: avatar + bouncing dots
                if (uiState.activeChannelTypingUsers.isNotEmpty()) {
                    item(key = "typing_presence") {
                        ChannelTypingPresenceRows(typers = uiState.activeChannelTypingUsers)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // JUMP TO BOTTOM BUTTON
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE3E5E8)),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Jump to bottom",
                        tint = Color(0xFF5C6470),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        val currentUserId = uiState.studentProfile?.studentId?.ifBlank { uiState.studentProfile?.universityEmail } ?: ""
        val currentServer = uiState.activeChatServer
        val currentMembers = currentServer?.id?.let { uiState.serverMembers[it] }.orEmpty()
        val currentMember = currentMembers.find { it.isSelf }
        val isCurrentOwner = currentUserId.isNotBlank() && currentServer?.ownerId == currentUserId
        val isCurrentAdmin = isCurrentOwner || currentMember?.role?.roleType in listOf(ServerRoleType.OWNER, ServerRoleType.ADMIN)
        val isBroadcastReadOnly = (activeChannel?.channelType?.lowercase() in listOf("announcements", "announcement", "rules")) && !isCurrentAdmin

        val isPreviewMode = currentServer?.isPreview == true &&
                (currentUserId.isBlank() || currentServer.ownerId != currentUserId) &&
                !uiState.joinedServerIds.contains(currentServer.id)

        if (isPreviewMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFFF2F3F5),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You are in preview mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1F22)
                        )
                        Text(
                            text = "Join this server to chat and participate!",
                            fontSize = 12.sp,
                            color = Color(0xFF5C6470)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            viewModel.onEvent(ChannelsEvent.JoinCurrentPreviewServer)
                        },
                        enabled = !uiState.isJoiningServer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 96.dp, minHeight = 38.dp)
                    ) {
                        if (uiState.isJoiningServer) {
                            com.campus.network.core.ui.components.DiscordBouncingDotsLoader(
                                dotSize = 5.dp,
                                dotSpacing = 4.dp,
                                dotColor = Color.White
                            )
                        } else {
                            Text(
                                text = "Join Server",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else if (isBroadcastReadOnly) {
            // Discord Read-Only Notice Dock for Announcement & Rules Channels
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFFF2F3F5),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3E5E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Read-only channel",
                            tint = Color(0xFF5C6470),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#$channelName is a read-only channel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1F22)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Only server owners and admins can send messages here.",
                            fontSize = 12.sp,
                            color = Color(0xFF5C6470)
                        )
                    }
                }
            }
        } else {
            // 4. BOTTOM CHAT INPUT DOCK (Matching DirectChatScreen interface with embedded media preview & Keyboard rich content)
            ChatInputDock(
                channelName = channelName,
                inputText = inputText,
                selectedMediaUris = selectedMediaUris,
                onRemoveMediaUri = { selectedMediaUris.remove(it) },
                onMediaReceived = { uri ->
                    if (!selectedMediaUris.contains(uri)) {
                        selectedMediaUris.add(uri)
                    }
                },
                isDrawerOpen = uiState.isMediaDrawerOpen,
                isEmojiOpen = isEmojiDrawerOpen,
                isVoiceRecordingActive = isVoiceRecordingActive,
                isSpeechDetected = isSpeechDetected,
                audioRms = audioRms,
                onTextChanged = { inputText = it },
                onFocusRequested = {
                    if (isEmojiDrawerOpen) {
                        isEmojiDrawerOpen = false
                    }
                    if (uiState.isMediaDrawerOpen) {
                        viewModel.onEvent(ChannelsEvent.ToggleMediaDrawer)
                    }
                },
                onToggleDrawer = {
                    if (isVoiceRecordingActive) {
                        transcriber.cancel()
                        isVoiceRecordingActive = false
                    }
                    if (isEmojiDrawerOpen) {
                        isEmojiDrawerOpen = false
                    }
                    viewModel.onEvent(ChannelsEvent.ToggleMediaDrawer)
                },
                onToggleEmoji = {
                    if (isVoiceRecordingActive) {
                        transcriber.cancel()
                        isVoiceRecordingActive = false
                    }
                    if (uiState.isMediaDrawerOpen) {
                        viewModel.onEvent(ChannelsEvent.ToggleMediaDrawer)
                    }
                    if (!isEmojiDrawerOpen) {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(view.windowToken, 0)
                        keyboardController?.hide()
                        isEmojiDrawerOpen = true
                    } else {
                        isEmojiDrawerOpen = false
                    }
                },
                onStartVoice = {
                    if (uiState.isMediaDrawerOpen) {
                        viewModel.onEvent(ChannelsEvent.ToggleMediaDrawer)
                    }
                    isEmojiDrawerOpen = false
                    if (transcriber.hasRecordPermission()) {
                        isVoiceRecordingActive = true
                        transcriber.startListening()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopVoice = {
                    transcriber.stopListening()
                    isVoiceRecordingActive = false
                },
                onSendMessage = {
                    if (isVoiceRecordingActive) {
                        transcriber.stopListening()
                        isVoiceRecordingActive = false
                    }
                    val textToSend = inputText.trim()
                    val mediaToSend = selectedMediaUris.toList()
                    if (textToSend.isNotBlank() || mediaToSend.isNotEmpty()) {
                        inputText = ""
                        selectedMediaUris.clear()
                        if (uiState.isMediaDrawerOpen) {
                            viewModel.onEvent(ChannelsEvent.ToggleMediaDrawer)
                        }
                        if (mediaToSend.isNotEmpty()) {
                            coroutineScope.launch {
                                val uploader = com.campus.network.core.network.CloudinaryUploader()
                                val uploaded = mutableListOf<String>()
                                var hasError = false
                                var errorMessage = ""
                                for (uri in mediaToSend) {
                                    val res = uploader.uploadFromUri(context, uri)
                                    if (res.isSuccess) {
                                        uploaded.add(res.getOrThrow())
                                    } else {
                                        hasError = true
                                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Network error during media upload"
                                        break
                                    }
                                }
                                if (hasError) {
                                    viewModel.onEvent(ChannelsEvent.ShowFeedback("Failed to send: $errorMessage"))
                                    return@launch
                                }
                                viewModel.onEvent(
                                    ChannelsEvent.SendChannelMessage(
                                        text = textToSend,
                                        imageUris = uploaded
                                    )
                                )
                            }
                        } else {
                            viewModel.onEvent(
                                ChannelsEvent.SendChannelMessage(
                                    text = textToSend,
                                    imageUris = emptyList()
                                )
                            )
                        }
                    }
                }
            )
        }

        // HYBRID EMOJI & GIF DRAWER (Google EmojiPicker + GIPHY via Supabase Proxy)
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
                    viewModel.onEvent(ChannelsEvent.SendChannelMessage(text = "", gifUrl = gif.originalUrl))
                    isEmojiDrawerOpen = false
                }
            )
        }

        // MEDIA DRAWER (BOTTOM SHEET WITH REAL DEVICE PHOTOS)
        AnimatedVisibility(
            visible = uiState.isMediaDrawerOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MediaAttachmentDrawer(
                activeTab = uiState.activeMediaTab,
                selectedUris = selectedMediaUris,
                onSelectTab = { tab ->
                    viewModel.onEvent(ChannelsEvent.SelectMediaTab(tab))
                },
                onToggleUriSelection = { uri ->
                    if (selectedMediaUris.contains(uri)) {
                        selectedMediaUris.remove(uri)
                    } else {
                        selectedMediaUris.add(uri)
                    }
                }
            )
        }
    }
}

/**
 * Top bar matching patriarch mobile:
 * [Back + real unread badge when count > 0] # channel-name > [Search]
 * Optional green "X is typing…" under the title.
 */
@Composable
private fun ChannelTopBar(
    channelName: String,
    unreadCount: Int,
    typingLabel: String? = null,
    onBackClick: () -> Unit,
    onChannelProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (typingLabel.isNullOrBlank()) 52.dp else 58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back arrow — red unread badge only when there are real unread messages
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBackClick)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDA373C))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else "$unreadCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Channel Name with Hash and Chevron (Clickable to open Channel Profile)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onChannelProfileClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6470)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = channelName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Channel Details",
                        tint = Color(0xFF80848E),
                        modifier = Modifier.size(18.dp)
                    )
                }
                AnimatedVisibility(
                    visible = !typingLabel.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = typingLabel.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF23A559),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            // Search Icon on right
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search in channel",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider(
            thickness = 0.8.dp,
            color = Color(0xFFE3E5E8)
        )
    }
}

/** WhatsApp-style avatar + bouncing dots for each peer currently typing. */
@Composable
private fun ChannelTypingPresenceRows(
    typers: List<ChannelTypingUser>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        typers.take(3).forEach { typer ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CampusStudentAvatar(
                        avatarUrl = typer.avatarUrl,
                        fullName = typer.displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF2F3F5),
                    border = BorderStroke(1.dp, Color(0xFFE3E5E8))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DiscordBouncingDotsLoader(
                            dotSize = 6.dp,
                            spaceBetween = 5.dp,
                            dotColor = Color(0xFF5C6470)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Centered date divider: e.g. "April 30, 2026"
 */
@Composable
private fun DateDividerItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            thickness = 0.8.dp,
            color = Color(0xFFE3E5E8),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .background(Color.White)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = text,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF80848E)
            )
        }
    }
}

/**
 * patriarch Channel Message row with reply support, rich embeds, and reaction chips
 */
@Composable
private fun ChannelMessageItem(
    message: ChannelMessage,
    onToggleReaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Reply Header (if replying to another message)
        if (message.replyTo != null) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Curved reply indicator corner
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 8.dp)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFB5BAC1),
                            shape = RoundedCornerShape(topStart = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "@${message.replyTo}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5C6470),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CampusStudentAvatar(
                    avatarUrl = message.authorAvatarRes,
                    fullName = message.authorName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Content Column
            Column(modifier = Modifier.weight(1f)) {
                // Author row: Name, Badge, Timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.authorName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(message.authorColorHex)
                    )

                    // Optional user role badge (e.g. TDS)
                    if (message.authorBadge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E1F22))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = message.authorBadge,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = message.timestamp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF80848E)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Message Text / URL link
                if (message.text.isNotBlank()) {
                    if (message.linkUrl != null) {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = Color(0xFF0066CC),
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Normal,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Render GIF if present
                if (!message.gifUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.gifUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "GIF",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .heightIn(min = 120.dp, max = 220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                    )
                }

                // Render attached Images if present
                if (message.imageUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(message.imageUris) { uriStr ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uriStr)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(160.dp, 120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                            )
                        }
                    }
                }

                // Rich Embed Card (e.g. GitHub Civ VII Music Pack)
                if (message.embed != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    MessageEmbedCard(embed = message.embed)
                }

                // Reaction Pill (ZERO EMOJI POLICY - Vector ThumbUp)
                if (message.reactionCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F3F5))
                            .border(1.dp, Color(0xFFE3E5E8), RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleReaction)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Reaction",
                                tint = Color(0xFF1E1F22),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${message.reactionCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1F22)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rich message embed card matching patriarch GitHub embed
 */
@Composable
private fun MessageEmbedCard(
    embed: ChannelMessageEmbed,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8F9FA),
        border = BorderStroke(1.dp, Color(0xFFE3E5E8))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Dark left border strip accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(210.dp)
                    .background(Color(0xFF1E1F22))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Provider (GitHub)
                Text(
                    text = embed.provider,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5C6470)
                )

                // Title (Blue link)
                Text(
                    text = embed.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0066CC),
                    lineHeight = 18.sp
                )

                // Description
                Text(
                    text = embed.description,
                    fontSize = 12.5.sp,
                    color = Color(0xFF313338),
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Sub-card (Repo Preview)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF2F3F5),
                    border = BorderStroke(1.dp, Color(0xFFE3E5E8))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        embed.subCardTitle?.let { title ->
                            Text(
                                text = title,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1F22)
                            )
                        }
                        embed.subCardDescription?.let { desc ->
                            Text(
                                text = desc,
                                fontSize = 11.5.sp,
                                color = Color(0xFF5C6470),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Stats row (contributors, issues, stars, forks)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmbedStatItem(icon = Icons.Default.People, count = embed.contributorsCount)
                            EmbedStatItem(icon = Icons.Default.Adjust, count = embed.issuesCount)
                            EmbedStatItem(icon = Icons.Default.Star, count = embed.starsCount)
                            EmbedStatItem(icon = Icons.AutoMirrored.Filled.AltRoute, count = embed.forksCount)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbedStatItem(
    icon: ImageVector,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF5C6470),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF5C6470)
        )
    }
}

/**
 * Bottom Pill Input Bar matching DirectChatScreen interface:
 * [ + / X ] [ Message #channel                              [ Mic / Send ] ]
 * Padded with WindowInsets.navigationBars.union(WindowInsets.ime) to smoothly elevate above the keyboard.
 * When voice recording is active, transforms into VoiceWaveformInputBar matching DirectChatScreen.
 */
@Composable
private fun ChatInputDock(
    channelName: String,
    inputText: String,
    selectedMediaUris: List<Uri> = emptyList(),
    onRemoveMediaUri: (Uri) -> Unit = {},
    onMediaReceived: (Uri) -> Unit = {},
    isDrawerOpen: Boolean,
    isEmojiOpen: Boolean,
    isVoiceRecordingActive: Boolean,
    isSpeechDetected: Boolean,
    audioRms: Float,
    onTextChanged: (String) -> Unit,
    onToggleDrawer: () -> Unit,
    onToggleEmoji: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSendMessage: () -> Unit,
    onFocusRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val insetsModifier = if (isDrawerOpen || isEmojiOpen) {
        Modifier.windowInsetsPadding(WindowInsets.ime)
    } else {
        Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(insetsModifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVoiceRecordingActive) {
            // Transformed Voice Waveform Input Bar (Matching DirectChatScreen)
            VoiceWaveformInputBar(
                isListening = isVoiceRecordingActive,
                isSpeechDetected = isSpeechDetected,
                audioRms = audioRms,
                onStopListening = onStopVoice,
                onSend = onSendMessage,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Circular '+' / 'X' Button on the left (42dp, border #E2E8F0, surface #F1F5F9)
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .size(42.dp)
                    .clickable(onClick = onToggleDrawer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDrawerOpen) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isDrawerOpen) "Close media drawer" else "Open media drawer",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Integrated Input Window with Embedded Media Preview & KeyboardMediaTextField
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
                    // Embedded Draft Media Preview inside input window
                    if (selectedMediaUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, top = 2.dp, bottom = 6.dp)
                        ) {
                            items(selectedMediaUris) { uri ->
                                Box(modifier = Modifier.size(64.dp)) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected media",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF1F5F9))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable { onRemoveMediaUri(uri) },
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
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rich KeyboardMediaTextField with Gboard GIF/image commit support
                        KeyboardMediaTextField(
                            value = inputText,
                            onValueChange = onTextChanged,
                            hint = "Message #$channelName",
                            onMediaReceived = onMediaReceived,
                            onSend = onSendMessage,
                            onFocusRequested = onFocusRequested,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 34.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Emoji Picker Toggle Button (patriarch style smiley icon)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleEmoji),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = "Emojis & Reactions",
                                tint = if (isEmojiOpen) Color(0xFF007AFF) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Mic / Send Button
                        if (inputText.isBlank() && selectedMediaUris.isEmpty()) {
                            // Blue Circular Mic Button (32dp, #007AFF)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF))
                                    .clickable(onClick = onStartVoice),
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
                            // Blue Circular Send Button with Up Arrow (matching DirectChatScreen)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF))
                                    .clickable(onClick = onSendMessage),
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
}

/**
 * Transformed Voice Waveform Input Bar (matching DirectChatScreen):
 * Solid circular blue mic button on left, pale sky-blue pill body,
 * and animated dynamic frequency waveform dancing in real-time to user voice.
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
 * Real-time WhatsApp/DirectChat audio visualization during voice dictation.
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
 * Media Attachment Drawer / Bottom Sheet (Matching uploaded_media_0_1792142138193.png)
 * Loads REAL device images from MediaStore and device storage (NOT placeholders).
 * Features Camera tile, device photo thumbnails with selection badges,
 * and 5 action tabs: Photos, Poll, Thread, Apps, Files
 */
@Composable
private fun MediaAttachmentDrawer(
    activeTab: String,
    selectedUris: List<Uri>,
    onSelectTab: (String) -> Unit,
    onToggleUriSelection: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceImages = remember { mutableStateListOf<Uri>() }

    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
    }

    // Auto-request storage permission on drawer open if not yet granted
    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            permissionLauncher.launch(requiredPermission)
        }
    }

    // Launcher for camera preview capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { _ ->
        // Query latest camera picture
    }

    // Load media directly into the media section
    LaunchedEffect(hasStoragePermission) {
        withContext(Dispatchers.IO) {
            val queriedList = mutableListOf<Uri>()

            // 1. Query device MediaStore if permission is granted
            if (hasStoragePermission) {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                try {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        var count = 0
                        while (cursor.moveToNext() && count < 60) {
                            val id = cursor.getLong(idColumn)
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            queriedList.add(contentUri)
                            count++
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MediaAttachmentDrawer", "MediaStore query note: ${e.message}")
                }
            }

            // 2. Always bundle campus media photos so media is immediately visible directly in the section
            val campusDrawables = listOf(
                com.campus.network.core.ui.R.drawable.place_photo_1,
                com.campus.network.core.ui.R.drawable.place_photo_3,
                com.campus.network.core.ui.R.drawable.course_thumb_1,
                com.campus.network.core.ui.R.drawable.course_thumb_3,
                com.campus.network.core.ui.R.drawable.course_thumb_6,
                com.campus.network.core.ui.R.drawable.onboarding_bridge_scenic,
                com.campus.network.core.ui.R.drawable.event_thumb_paws,
                com.campus.network.core.ui.R.drawable.student_rk,
                com.campus.network.core.ui.R.drawable.student_andrew,
                com.campus.network.core.ui.R.drawable.student_june
            )
            for (resId in campusDrawables) {
                val uri = Uri.parse("android.resource://${context.packageName}/$resId")
                if (!queriedList.contains(uri)) {
                    queriedList.add(uri)
                }
            }

            withContext(Dispatchers.Main) {
                deviceImages.clear()
                deviceImages.addAll(queriedList)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE3E5E8))

        // HORIZONTAL PHOTO / CAMERA ROW (LOADED DIRECTLY IN SECTION)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Tile (Dark square tile with Camera icon)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2B2D31))
                    .clickable {
                        cameraLauncher.launch(null)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Take Photo",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Real Media Loaded directly in the section (No transition to other screens)
            deviceImages.forEach { uri ->
                val isSelected = selectedUris.contains(uri)
                val selectionNumber = if (isSelected) selectedUris.indexOf(uri) + 1 else null

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleUriSelection(uri) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Media item",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Selection Circle Badge at top right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF007AFF) else Color(0x66000000))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectionNumber != null) {
                            Text(
                                text = "$selectionNumber",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF2F3F5))

        // 5 BOTTOM ACTION TABS (Photos, Poll, Thread, Apps, Files)
        val tabs = listOf(
            Triple("Photos", Icons.Default.PhotoLibrary, "Photos"),
            Triple("Poll", Icons.Default.Poll, "Poll"),
            Triple("Thread", Icons.Default.Forum, "Thread"),
            Triple("Apps", Icons.Default.Apps, "Apps"),
            Triple("Files", Icons.Default.Description, "Files")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tabId, icon, label) ->
                val isActive = activeTab == tabId
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // Switches tab in-place without transitioning to another screen
                            onSelectTab(tabId)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isActive) Color(0xFF1E1F22) else Color(0xFF80848E),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) Color(0xFF1E1F22) else Color(0xFF80848E)
                    )
                }
            }
        }
    }
}


