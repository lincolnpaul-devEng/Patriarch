package com.campus.network.feature.messages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentScreen != MessagesScreenMode.THREADS) {
        when (uiState.currentScreen) {
            MessagesScreenMode.AUDIO_CALL, MessagesScreenMode.VIDEO_CALL -> {
                viewModel.onEvent(MessagesEvent.EndCall)
            }
            MessagesScreenMode.CHAT -> {
                viewModel.onEvent(MessagesEvent.BackToThreads)
            }
            MessagesScreenMode.THREADS -> {
                // Let system handle default back
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
                        onEvent = viewModel::onEvent
                    )
                }

                MessagesScreenMode.CHAT -> {
                    val thread = uiState.selectedThread
                    if (thread != null) {
                        DirectChatScreen(
                            thread = thread,
                            messages = uiState.messages,
                            isSettlementDialogOpen = uiState.isSettlementDialogOpen,
                            onEvent = viewModel::onEvent
                        )
                    } else {
                        MessageThreadsScreen(
                            uiState = uiState,
                            onEvent = viewModel::onEvent
                        )
                    }
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
                                onEvent = viewModel::onEvent
                            )
                        } else {
                            MessageThreadsScreen(
                                uiState = uiState,
                                onEvent = viewModel::onEvent
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
                                onEvent = viewModel::onEvent
                            )
                        } else {
                            MessageThreadsScreen(
                                uiState = uiState,
                                onEvent = viewModel::onEvent
                            )
                        }
                    }
                }
            }
        }
    }
}
