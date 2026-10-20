package com.campus.network.feature.messages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToServer: ((String) -> Unit)? = null,
    onAddServer: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentScreen != MessagesScreenMode.THREADS) {
        when (uiState.currentScreen) {
            MessagesScreenMode.AUDIO_CALL, MessagesScreenMode.VIDEO_CALL -> {
                viewModel.onEvent(MessagesEvent.EndCall)
            }
            MessagesScreenMode.CHAT,
            MessagesScreenMode.NEW_CHAT,
            MessagesScreenMode.ADD_NOTE,
            MessagesScreenMode.BROADCAST -> {
                viewModel.onEvent(MessagesEvent.BackToThreads)
            }
            MessagesScreenMode.THREADS -> {
                onNavigateBack()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
                if (targetState == MessagesScreenMode.CHAT && initialState == MessagesScreenMode.THREADS) {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                } else if (targetState == MessagesScreenMode.THREADS && initialState == MessagesScreenMode.CHAT) {
                    (slideInHorizontally { width -> -width / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                } else if (targetState in listOf(MessagesScreenMode.NEW_CHAT, MessagesScreenMode.BROADCAST) &&
                    initialState == MessagesScreenMode.THREADS
                ) {
                    (slideInVertically { height -> height / 2 } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height / 3 } + fadeOut())
                } else if (targetState == MessagesScreenMode.THREADS &&
                    initialState in listOf(MessagesScreenMode.NEW_CHAT, MessagesScreenMode.BROADCAST)
                ) {
                    (slideInVertically { height -> -height / 3 } + fadeIn())
                        .togetherWith(slideOutVertically { height -> height / 2 } + fadeOut())
                } else if (targetState == MessagesScreenMode.ADD_NOTE && initialState == MessagesScreenMode.THREADS) {
                    (slideInVertically { height -> -height / 4 } + fadeIn())
                        .togetherWith(fadeOut())
                } else if (targetState == MessagesScreenMode.THREADS && initialState == MessagesScreenMode.ADD_NOTE) {
                    fadeIn().togetherWith(slideOutVertically { height -> -height / 4 } + fadeOut())
                } else if (targetState == MessagesScreenMode.AUDIO_CALL || targetState == MessagesScreenMode.VIDEO_CALL) {
                    fadeIn().togetherWith(fadeOut())
                } else {
                    fadeIn().togetherWith(fadeOut())
                }
            },
            label = "MessagesScreenTransition"
        ) { screenMode ->
            when (screenMode) {
                MessagesScreenMode.THREADS -> {
                    MessageThreadsScreen(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onNavigateBack = onNavigateBack,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToServer = onNavigateToServer,
                        onAddServer = onAddServer
                    )
                }

                MessagesScreenMode.CHAT -> {
                    val thread = uiState.selectedThread
                    if (thread != null) {
                        DirectChatScreen(
                            thread = thread,
                            messages = uiState.messages,
                            isSettlementDialogOpen = uiState.isSettlementDialogOpen,
                            onEvent = viewModel::onEvent,
                            initialActivateVoice = uiState.activateVoiceInChat,
                            onConsumeVoiceActivation = {
                                viewModel.onEvent(MessagesEvent.ConsumeVoiceActivation)
                            },
                            otherThreadsUnreadCount = uiState.threads
                                .filter { it.id != thread.id }
                                .sumOf { it.unreadCount },
                            typingLabel = uiState.activeChatTypingLabel
                        )
                    } else {
                        MessageThreadsScreen(
                            uiState = uiState,
                            onEvent = viewModel::onEvent,
                            onNavigateBack = onNavigateBack,
                            onNavigateToProfile = onNavigateToProfile,
                            onNavigateToServer = onNavigateToServer,
                            onAddServer = onAddServer
                        )
                    }
                }

                MessagesScreenMode.NEW_CHAT -> {
                    NewChatScreen(
                        threads = uiState.threads,
                        onBack = { viewModel.onEvent(MessagesEvent.BackToThreads) },
                        onSelectPeer = { threadId ->
                            viewModel.onEvent(
                                MessagesEvent.SelectThread(
                                    threadId = threadId,
                                    activateVoice = uiState.isVoiceFlowActive
                                )
                            )
                        },
                        onStartGroupChat = { peerIds ->
                            if (uiState.isVoiceFlowActive) {
                                val firstPeer = peerIds.firstOrNull() ?: uiState.threads.firstOrNull()?.id
                                if (firstPeer != null) {
                                    viewModel.onEvent(
                                        MessagesEvent.SelectThread(
                                            threadId = firstPeer,
                                            activateVoice = true
                                        )
                                    )
                                }
                            } else {
                                viewModel.onEvent(MessagesEvent.OpenBroadcast)
                            }
                        }
                    )
                }

                MessagesScreenMode.ADD_NOTE -> {
                    AddNoteScreen(
                        currentNote = uiState.currentUserNote,
                        fullName = uiState.studentProfile?.fullName?.ifBlank { "Student" } ?: "Student",
                        avatarUrl = uiState.studentProfile?.avatarUrl,
                        onBack = { viewModel.onEvent(MessagesEvent.BackToThreads) },
                        onShareNote = { note ->
                            viewModel.onEvent(MessagesEvent.SaveUserNote(note))
                        }
                    )
                }

                MessagesScreenMode.BROADCAST -> {
                    BroadcastScreen(
                        threads = uiState.threads,
                        onBack = { viewModel.onEvent(MessagesEvent.BackToThreads) },
                        onSendBroadcast = { peerIds, text ->
                            viewModel.onEvent(MessagesEvent.SendBroadcast(peerIds, text))
                        }
                    )
                }

                MessagesScreenMode.AUDIO_CALL -> {
                    val call = uiState.callState
                    if (call != null) {
                        AudioCallScreen(
                            callState = call,
                            onEvent = viewModel::onEvent
                        )
                    } else {
                        val thread = uiState.selectedThread
                        if (thread != null) {
                            DirectChatScreen(
                                thread = thread,
                                messages = uiState.messages,
                                isSettlementDialogOpen = uiState.isSettlementDialogOpen,
                                onEvent = viewModel::onEvent,
                                initialActivateVoice = uiState.activateVoiceInChat,
                                onConsumeVoiceActivation = {
                                    viewModel.onEvent(MessagesEvent.ConsumeVoiceActivation)
                                },
                                otherThreadsUnreadCount = uiState.threads
                                    .filter { it.id != thread.id }
                                    .sumOf { it.unreadCount },
                                typingLabel = uiState.activeChatTypingLabel
                            )
                        } else {
                            MessageThreadsScreen(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = onNavigateBack,
                                onNavigateToProfile = onNavigateToProfile
                            )
                        }
                    }
                }

                MessagesScreenMode.VIDEO_CALL -> {
                    val call = uiState.callState
                    if (call != null) {
                        VideoCallScreen(
                            callState = call,
                            onEvent = viewModel::onEvent
                        )
                    } else {
                        val thread = uiState.selectedThread
                        if (thread != null) {
                            DirectChatScreen(
                                thread = thread,
                                messages = uiState.messages,
                                isSettlementDialogOpen = uiState.isSettlementDialogOpen,
                                onEvent = viewModel::onEvent,
                                initialActivateVoice = uiState.activateVoiceInChat,
                                onConsumeVoiceActivation = {
                                    viewModel.onEvent(MessagesEvent.ConsumeVoiceActivation)
                                },
                                otherThreadsUnreadCount = uiState.threads
                                    .filter { it.id != thread.id }
                                    .sumOf { it.unreadCount },
                                typingLabel = uiState.activeChatTypingLabel
                            )
                        } else {
                            MessageThreadsScreen(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = onNavigateBack,
                                onNavigateToProfile = onNavigateToProfile
                            )
                        }
                    }
                }
            }
        }
    }
}
