package com.campus.network.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.model.PeerTransactionPayload
import com.campus.network.core.model.SettlementStatus
import com.campus.network.core.network.StreamChatEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val streamChatEngine: StreamChatEngine? = null,
    private val telecomCallManager: CampusTelecomCallManager? = null
) : ViewModel() {

    private var callTimerJob: Job? = null

    private val initialThreads = listOf(
        ConversationThread(
            id = "thread_jane",
            peerName = "Jane Doe",
            peerHandle = "@janedoe",
            avatarInitials = "J",
            lastMessage = "I'll bring cookies",
            lastTimestamp = "9:23 AM",
            unreadCount = 0,
            isOnline = true,
            isVerified = true,
            category = ThreadCategory.DIRECT
        ),
        ConversationThread(
            id = "thread_sarah",
            peerName = "Sarah Nduta",
            peerHandle = "@sarah_n",
            avatarInitials = "SN",
            lastMessage = "Sending peer escrow settlement for the micro-gig.",
            lastTimestamp = "10:09 AM",
            unreadCount = 0,
            isOnline = true,
            isVerified = true,
            category = ThreadCategory.DIRECT
        ),
        ConversationThread(
            id = "thread_mwangi",
            peerName = "Prof. Mwangi (Supervisor)",
            peerHandle = "@prof_mwangi",
            avatarInitials = "PM",
            lastMessage = "Please review chapter 4 citations before tomorrow's seminar.",
            lastTimestamp = "09:40 AM",
            unreadCount = 2,
            isOnline = true,
            isVerified = true,
            category = ThreadCategory.ACADEMIC
        ),
        ConversationThread(
            id = "thread_brian",
            peerName = "Brian Ochieng (Calculators)",
            peerHandle = "@brian_och",
            avatarInitials = "BO",
            lastMessage = "Casio fx-991EX is ready for pickup at Science Complex.",
            lastTimestamp = "Yesterday",
            unreadCount = 0,
            isOnline = false,
            isVerified = false,
            category = ThreadCategory.GIGS
        ),
        ConversationThread(
            id = "thread_guild",
            peerName = "Nairobi Tech Guild",
            peerHandle = "@tech_guild",
            avatarInitials = "TG",
            lastMessage = "Hackathon registration closes at midnight!",
            lastTimestamp = "Yesterday",
            unreadCount = 5,
            isOnline = true,
            isVerified = true,
            category = ThreadCategory.ACADEMIC
        ),
        ConversationThread(
            id = "thread_faith",
            peerName = "Faith Wangari",
            peerHandle = "@faith_w",
            avatarInitials = "FW",
            lastMessage = "Can we do a quick video call regarding the lab presentation slides?",
            lastTimestamp = "Monday",
            unreadCount = 1,
            isOnline = true,
            isVerified = true,
            category = ThreadCategory.DIRECT
        )
    )

    private val threadMessagesMap = mutableMapOf<String, List<DirectMessageItem>>(
        "thread_jane" to listOf(
            DirectMessageItem(
                id = "msg-j0",
                senderName = "Lincoln",
                isMine = true,
                text = "I just got back in town, let's meet!",
                timestamp = "Today 1:30 AM",
                replyToText = "Good!"
            ),
            DirectMessageItem(
                id = "msg-j1",
                senderName = "Jane Doe",
                isMine = false,
                text = "My family are hosting an Eid lunch today, come over to my house!",
                timestamp = "Today 1.43 AM"
            ),
            DirectMessageItem(
                id = "msg-j2",
                senderName = "Lincoln",
                isMine = true,
                text = "Happy Eid!",
                timestamp = "Today 1.44 AM"
            ),
            DirectMessageItem(
                id = "msg-j3",
                senderName = "Lincoln",
                isMine = true,
                text = "Looking forward to see you!!!",
                timestamp = "Today 1.45 AM",
                isEdited = true
            ),
            DirectMessageItem(
                id = "msg-j4",
                senderName = "System",
                isMine = false,
                text = "You unsent a message",
                timestamp = "Today 9.20 AM",
                isSystem = true
            ),
            DirectMessageItem(
                id = "msg-j5",
                senderName = "Lincoln",
                isMine = true,
                text = "I'll bring cookies",
                timestamp = "Today 9.23 AM"
            ),
            DirectMessageItem(
                id = "msg-j6",
                senderName = "Lincoln",
                isMine = true,
                text = "See You!",
                timestamp = "Today 9.24 AM",
                isDelivered = true,
                isHandwritten = true
            )
        ),
        "thread_sarah" to listOf(
            DirectMessageItem(
                id = "msg-1",
                senderName = "Sarah Nduta",
                isMine = false,
                text = "Hey! Finished the IEEE LaTeX formatting for your proposal chapters 1-3.",
                timestamp = "10:05 AM"
            ),
            DirectMessageItem(
                id = "msg-2",
                senderName = "Lincoln",
                isMine = true,
                text = "Awesome work, thanks for fixing the bibtex citations so fast!",
                timestamp = "10:08 AM"
            ),
            DirectMessageItem(
                id = "msg-3",
                senderName = "Lincoln",
                isMine = true,
                text = "Sending peer escrow settlement for the micro-gig.",
                timestamp = "10:09 AM",
                settlementPayload = PeerTransactionPayload(
                    transactionId = "tx-9921",
                    senderName = "Lincoln Kiprotich",
                    recipientName = "Sarah Nduta",
                    amount = 1500.0,
                    currency = "KES",
                    purpose = "LaTeX Thesis Formatting Gig",
                    status = SettlementStatus.ESCROW_LOCKED,
                    timestampMs = System.currentTimeMillis() - (10 * 60 * 1000),
                    referenceCode = "CAMPUS-ESCROW-9921"
                )
            )
        ),
        "thread_mwangi" to listOf(
            DirectMessageItem(
                id = "msg-m1",
                senderName = "Prof. Mwangi",
                isMine = false,
                text = "Lincoln, your methodology section draft is promising. Please incorporate the experimental telemetry data.",
                timestamp = "09:30 AM"
            ),
            DirectMessageItem(
                id = "msg-m2",
                senderName = "Prof. Mwangi",
                isMine = false,
                text = "Please review chapter 4 citations before tomorrow's seminar.",
                timestamp = "09:40 AM"
            )
        ),
        "thread_brian" to listOf(
            DirectMessageItem(
                id = "msg-b1",
                senderName = "Lincoln",
                isMine = true,
                text = "Hi Brian, is the scientific calculator still available for hire?",
                timestamp = "Yesterday 03:15 PM"
            ),
            DirectMessageItem(
                id = "msg-b2",
                senderName = "Brian Ochieng",
                isMine = false,
                text = "Casio fx-991EX is ready for pickup at Science Complex.",
                timestamp = "Yesterday 03:22 PM"
            )
        ),
        "thread_guild" to listOf(
            DirectMessageItem(
                id = "msg-g1",
                senderName = "Nairobi Tech Guild",
                isMine = false,
                text = "Hackathon registration closes at midnight!",
                timestamp = "Yesterday 08:00 PM"
            )
        ),
        "thread_faith" to listOf(
            DirectMessageItem(
                id = "msg-f1",
                senderName = "Faith Wangari",
                isMine = false,
                text = "Can we do a quick video call regarding the lab presentation slides?",
                timestamp = "Monday 04:12 PM"
            )
        )
    )

    private val _uiState = MutableStateFlow(
        MessagesUiState(
            threads = initialThreads
        )
    )
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    val voiceTransformerEngine = com.campus.network.feature.messages.audio.VoiceTransformerEngine(viewModelScope)

    fun onEvent(event: MessagesEvent) {
        when (event) {
            is MessagesEvent.SelectThread -> {
                val thread = _uiState.value.threads.find { it.id == event.threadId }
                if (thread != null) {
                    val messages = threadMessagesMap[thread.id] ?: emptyList()
                    val updatedThreads = _uiState.value.threads.map {
                        if (it.id == thread.id) it.copy(unreadCount = 0) else it
                    }
                    _uiState.update {
                        it.copy(
                            currentScreen = MessagesScreenMode.CHAT,
                            selectedThread = thread.copy(unreadCount = 0),
                            threads = updatedThreads,
                            messages = messages,
                            activateVoiceInChat = event.activateVoice || it.isVoiceFlowActive,
                            isVoiceFlowActive = false
                        )
                    }
                }
            }

            is MessagesEvent.ConsumeVoiceActivation -> {
                _uiState.update { it.copy(activateVoiceInChat = false) }
            }

            is MessagesEvent.BackToThreads -> {
                _uiState.update {
                    it.copy(
                        currentScreen = MessagesScreenMode.THREADS,
                        selectedThread = null
                    )
                }
            }

            is MessagesEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }

            is MessagesEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
            }

            is MessagesEvent.SendMessage -> {
                if (event.text.isBlank() && event.imageUri == null && event.imageUrl == null && event.gifUrl == null) return
                val activeThread = _uiState.value.selectedThread ?: return
                val newMsg = DirectMessageItem(
                    id = "msg-${System.currentTimeMillis()}",
                    senderName = "Lincoln",
                    isMine = true,
                    text = event.text.trim(),
                    timestamp = "Just now",
                    imageUri = event.imageUri,
                    imageUrl = event.imageUrl,
                    gifUrl = event.gifUrl
                )
                val currentList = threadMessagesMap[activeThread.id] ?: emptyList()
                val updatedList = currentList + newMsg
                threadMessagesMap[activeThread.id] = updatedList

                val displaySnippet = when {
                    event.gifUrl != null -> "[GIF]"
                    event.imageUri != null || event.imageUrl != null -> "[Photo]"
                    else -> event.text.trim()
                }

                val updatedThreads = _uiState.value.threads.map {
                    if (it.id == activeThread.id) {
                        it.copy(
                            lastMessage = displaySnippet,
                            lastTimestamp = "Just now"
                        )
                    } else it
                }

                _uiState.update {
                    it.copy(
                        messages = updatedList,
                        threads = updatedThreads
                    )
                }

                viewModelScope.launch {
                    streamChatEngine?.sendDirectMessage(
                        channelId = activeThread.id,
                        text = event.text.trim()
                    )
                }
            }

            is MessagesEvent.InitiateSettlement -> {
                val activeThread = _uiState.value.selectedThread ?: return
                val txPayload = PeerTransactionPayload(
                    transactionId = "tx-${System.currentTimeMillis()}",
                    senderName = "Lincoln Kiprotich",
                    recipientName = activeThread.peerName,
                    amount = event.amount,
                    currency = "KES",
                    purpose = event.purpose,
                    status = SettlementStatus.ESCROW_LOCKED,
                    timestampMs = System.currentTimeMillis(),
                    referenceCode = "ESCROW-${System.currentTimeMillis() % 10000}"
                )
                val newMsg = DirectMessageItem(
                    id = "msg-${System.currentTimeMillis()}",
                    senderName = "Lincoln",
                    isMine = true,
                    text = "Escrow initiated: KES ${event.amount} for ${event.purpose}",
                    timestamp = "Just now",
                    settlementPayload = txPayload
                )
                val currentList = threadMessagesMap[activeThread.id] ?: emptyList()
                val updatedList = currentList + newMsg
                threadMessagesMap[activeThread.id] = updatedList

                _uiState.update {
                    it.copy(
                        messages = updatedList,
                        isSettlementDialogOpen = false
                    )
                }

                viewModelScope.launch {
                    streamChatEngine?.sendTransactionSettlement(
                        channelType = "messaging",
                        channelId = activeThread.id,
                        payload = txPayload
                    )
                }
            }

            is MessagesEvent.ConfirmSettlement -> {
                val activeThread = _uiState.value.selectedThread ?: return
                val currentList = threadMessagesMap[activeThread.id] ?: emptyList()
                val updatedList = currentList.map { msg ->
                    if (msg.settlementPayload?.transactionId == event.transactionId) {
                        msg.copy(
                            settlementPayload = msg.settlementPayload.copy(status = SettlementStatus.SETTLED)
                        )
                    } else {
                        msg
                    }
                }
                threadMessagesMap[activeThread.id] = updatedList
                _uiState.update { it.copy(messages = updatedList) }
            }

            is MessagesEvent.ToggleSettlementDialog -> {
                _uiState.update { it.copy(isSettlementDialogOpen = event.open) }
            }

            is MessagesEvent.StartAudioCall -> {
                startCall(event.thread, CallType.AUDIO)
            }

            is MessagesEvent.StartVideoCall -> {
                startCall(event.thread, CallType.VIDEO)
            }

            is MessagesEvent.EndCall -> {
                stopCall()
            }

            is MessagesEvent.ToggleMute -> {
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isMuted = !call.isMuted))
                    } ?: state
                }
            }

            is MessagesEvent.ToggleSpeaker -> {
                _uiState.update { state ->
                    state.callState?.let { call ->
                        val nextSpeaker = !call.isSpeakerOn
                        telecomCallManager?.setSpeakerOn(nextSpeaker)
                        state.copy(callState = call.copy(isSpeakerOn = nextSpeaker))
                    } ?: state
                }
            }

            is MessagesEvent.ToggleVideo -> {
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isVideoEnabled = !call.isVideoEnabled))
                    } ?: state
                }
            }

            is MessagesEvent.FlipCamera -> {
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isFrontCamera = !call.isFrontCamera))
                    } ?: state
                }
            }

            is MessagesEvent.AcceptCall -> {
                telecomCallManager?.setCallConnected()
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(status = CallStatus.CONNECTED))
                    } ?: state
                }
            }

            is MessagesEvent.SetVoiceFilter -> {
                voiceTransformerEngine.setPreset(event.preset)
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(
                            callState = call.copy(
                                activeVoiceFilter = event.preset,
                                customPitchShift = event.preset.pitchSemiTones,
                                customFormantScale = event.preset.formantScale
                            )
                        )
                    } ?: state
                }
            }

            is MessagesEvent.UpdateCustomPitchAndFormant -> {
                voiceTransformerEngine.setCustomParameters(event.pitchSemiTones, event.formantScale)
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(
                            callState = call.copy(
                                customPitchShift = event.pitchSemiTones,
                                customFormantScale = event.formantScale
                            )
                        )
                    } ?: state
                }
            }

            is MessagesEvent.ToggleSelfMonitoring -> {
                voiceTransformerEngine.toggleSelfMonitoring(event.context)
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(
                            callState = call.copy(
                                isSelfMonitoring = voiceTransformerEngine.isSelfMonitoring.value
                            )
                        )
                    } ?: state
                }
            }

            is MessagesEvent.OpenNewChat -> {
                _uiState.update {
                    it.copy(
                        currentScreen = MessagesScreenMode.NEW_CHAT,
                        isVoiceFlowActive = false
                    )
                }
            }

            is MessagesEvent.OpenVoiceChatSelection -> {
                _uiState.update {
                    it.copy(
                        currentScreen = MessagesScreenMode.NEW_CHAT,
                        isVoiceFlowActive = true
                    )
                }
            }

            is MessagesEvent.OpenAddNote -> {
                _uiState.update { it.copy(currentScreen = MessagesScreenMode.ADD_NOTE) }
            }

            is MessagesEvent.OpenBroadcast -> {
                _uiState.update { it.copy(currentScreen = MessagesScreenMode.BROADCAST) }
            }

            is MessagesEvent.SaveUserNote -> {
                val cleanedNote = if (event.note.isNotBlank()) event.note.trim() else "Add note"
                _uiState.update {
                    it.copy(
                        currentUserNote = cleanedNote,
                        currentScreen = MessagesScreenMode.THREADS
                    )
                }
            }

            is MessagesEvent.SendBroadcast -> {
                val trimmedText = event.text.trim()
                if (trimmedText.isNotBlank()) {
                    event.peerIds.forEach { peerId ->
                        val newMsg = DirectMessageItem(
                            id = "msg-${System.currentTimeMillis()}-${peerId.hashCode()}",
                            senderName = "Lincoln",
                            isMine = true,
                            text = trimmedText,
                            timestamp = "Just now"
                        )
                        val currentList = threadMessagesMap[peerId] ?: emptyList()
                        threadMessagesMap[peerId] = currentList + newMsg
                    }

                    val updatedThreads = _uiState.value.threads.map { thread ->
                        if (thread.id in event.peerIds) {
                            thread.copy(
                                lastMessage = trimmedText,
                                lastTimestamp = "Just now"
                            )
                        } else thread
                    }

                    _uiState.update {
                        it.copy(
                            threads = updatedThreads,
                            currentScreen = MessagesScreenMode.THREADS
                        )
                    }
                } else {
                    _uiState.update { it.copy(currentScreen = MessagesScreenMode.THREADS) }
                }
            }
        }
    }

    private fun startCall(thread: ConversationThread, type: CallType) {
        callTimerJob?.cancel()
        val initialStatus = if (thread.isOnline) CallStatus.RINGING else CallStatus.CALLING
        val newCallState = CallUiState(
            peerName = thread.peerName,
            peerHandle = thread.peerHandle,
            peerInitials = thread.avatarInitials,
            callType = type,
            status = initialStatus,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = true,
            isVideoEnabled = (type == CallType.VIDEO),
            isFrontCamera = true,
            isPeerOnline = thread.isOnline
        )

        val nextMode = if (type == CallType.AUDIO) {
            MessagesScreenMode.AUDIO_CALL
        } else {
            MessagesScreenMode.VIDEO_CALL
        }

        _uiState.update {
            it.copy(
                callState = newCallState,
                currentScreen = nextMode
            )
        }

        telecomCallManager?.startOutgoingCall(
            peerName = thread.peerName,
            peerHandle = thread.peerHandle,
            isVideo = (type == CallType.VIDEO),
            onDisconnectBySystem = {
                stopCall()
            }
        )

        callTimerJob = viewModelScope.launch {
            // The call remains in CALLING (offline peer) or RINGING (online peer)
            // until the remote participant accepts (MessagesEvent.AcceptCall / remote signaling).
            // Duration timer increments strictly while in CONNECTED status.
            while (true) {
                delay(1000L)
                _uiState.update { state ->
                    state.callState?.let { call ->
                        if (call.status == CallStatus.CONNECTED) {
                            state.copy(callState = call.copy(durationSeconds = call.durationSeconds + 1))
                        } else {
                            state
                        }
                    } ?: state
                }
            }
        }
    }

    private fun stopCall() {
        voiceTransformerEngine.stopLoopback()
        telecomCallManager?.endCall()
        callTimerJob?.cancel()
        callTimerJob = null
        _uiState.update {
            it.copy(
                callState = null,
                currentScreen = if (it.selectedThread != null) MessagesScreenMode.CHAT else MessagesScreenMode.THREADS
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceTransformerEngine.release()
        telecomCallManager?.endCall()
        callTimerJob?.cancel()
    }
}
