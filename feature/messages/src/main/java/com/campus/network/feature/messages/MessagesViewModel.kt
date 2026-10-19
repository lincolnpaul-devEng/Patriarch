package com.campus.network.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.model.PeerTransactionPayload
import com.campus.network.core.model.SettlementStatus
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.firstName
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
    private val telecomCallManager: CampusTelecomCallManager? = null,
    private val studentProfileRepository: StudentProfileRepository = StudentProfileRepository()
) : ViewModel() {

    private var callTimerJob: Job? = null
    private val _currentProfile = MutableStateFlow<StudentProfile?>(null)

    private val initialThreads = emptyList<ConversationThread>()

    private val threadMessagesMap = mutableMapOf<String, List<DirectMessageItem>>()

    private val _uiState = MutableStateFlow(
        MessagesUiState(
            threads = initialThreads
        )
    )
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    val voiceTransformerEngine = com.campus.network.feature.messages.audio.VoiceTransformerEngine(viewModelScope)

    init {
        viewModelScope.launch {
            studentProfileRepository.getProfile().collect { profile ->
                _currentProfile.value = profile
                _uiState.update { current ->
                    current.copy(
                        studentProfile = profile,
                        currentUserNote = profile.statusNote ?: current.currentUserNote
                    )
                }
            }
        }

        // Apply any inline replies that were sent from the notification shade while the ViewModel was inactive
        com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
            .consumeAllPendingInlineReplies()
            .forEach { replyEvent ->
                applyInlineNotificationReply(
                    threadId = replyEvent.threadId,
                    peerName = replyEvent.peerName,
                    replyText = replyEvent.replyText,
                    timestampLabel = replyEvent.timestampLabel
                )
            }

        // Observe live WhatsApp-style inline replies sent from the Android notification shade
        viewModelScope.launch {
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.inlineRepliesFlow.collect { replyEvent ->
                com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.consumePendingInlineReplies(replyEvent.threadId)
                applyInlineNotificationReply(
                    threadId = replyEvent.threadId,
                    peerName = replyEvent.peerName,
                    replyText = replyEvent.replyText,
                    timestampLabel = replyEvent.timestampLabel
                )
            }
        }

        // Observe "Mark as Read" actions from the Android notification shade
        viewModelScope.launch {
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.markReadFlow.collect { markRead ->
                _uiState.update { state ->
                    state.copy(
                        threads = state.threads.map { t ->
                            if (t.id == markRead.threadId) t.copy(unreadCount = 0) else t
                        }
                    )
                }
            }
        }
    }

    private fun applyInlineNotificationReply(
        threadId: String,
        peerName: String,
        replyText: String,
        timestampLabel: String
    ) {
        val currentUserFirstName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You"
        val newMsg = DirectMessageItem(
            id = "msg_inline_${System.currentTimeMillis()}",
            senderName = currentUserFirstName,
            isMine = true,
            text = replyText,
            timestamp = timestampLabel
        )
        val existingList = threadMessagesMap[threadId] ?: emptyList()
        val updatedMessages = existingList + newMsg
        threadMessagesMap[threadId] = updatedMessages

        _uiState.update { state ->
            val updatedThreads = state.threads.map { t ->
                if (t.id == threadId || t.peerName.equals(peerName, ignoreCase = true)) {
                    t.copy(
                        lastMessage = "You: $replyText",
                        lastTimestamp = "Just now",
                        unreadCount = 0
                    )
                } else {
                    t
                }
            }
            val isViewingThisThread = state.selectedThread?.id == threadId
            state.copy(
                threads = updatedThreads,
                messages = if (isViewingThisThread) updatedMessages else state.messages
            )
        }
    }

    fun onEvent(event: MessagesEvent) {
        when (event) {
            is MessagesEvent.SelectThread -> {
                val thread = _uiState.value.threads.find { it.id == event.threadId }
                if (thread != null) {
                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.activeVisibleThreadId = thread.id
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

            is MessagesEvent.OpenOrCreateDirectChat -> {
                val existing = _uiState.value.threads.find {
                    it.id == event.peerId || it.id == "thread_${event.peerId}" || it.peerName.equals(event.peerName, ignoreCase = true)
                }
                if (existing != null) {
                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.activeVisibleThreadId = existing.id
                    val messages = threadMessagesMap[existing.id] ?: emptyList()
                    val updatedThreads = _uiState.value.threads.map {
                        if (it.id == existing.id) it.copy(unreadCount = 0) else it
                    }
                    _uiState.update {
                        it.copy(
                            currentScreen = MessagesScreenMode.CHAT,
                            selectedThread = existing.copy(unreadCount = 0),
                            threads = updatedThreads,
                            messages = messages
                        )
                    }
                } else {
                    val newThreadId = if (event.peerId.startsWith("thread_")) event.peerId else "thread_${event.peerId}"
                    com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.activeVisibleThreadId = newThreadId
                    val initials = event.avatarInitials ?: event.peerName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                    val badge = if (!event.serverTag.isNullOrBlank()) {
                        "${event.serverTag.uppercase()} • ${event.serverName ?: "Server"}"
                    } else if (!event.serverName.isNullOrBlank()) {
                        event.serverName
                    } else null
                    val newThread = ConversationThread(
                        id = newThreadId,
                        peerName = event.peerName,
                        peerHandle = event.peerHandle ?: "@${event.peerName.lowercase().replace(" ", "")}",
                        avatarInitials = initials.ifBlank { "M" },
                        lastMessage = if (!event.serverName.isNullOrBlank()) "Direct chat from ${event.serverName}" else "Started direct chat from server",
                        lastTimestamp = "Just now",
                        unreadCount = 0,
                        isOnline = true,
                        isVerified = true,
                        category = ThreadCategory.DIRECT,
                        serverBadgeText = badge
                    )
                    val welcomeMsg = listOf(
                        DirectMessageItem(
                            id = "msg_${System.currentTimeMillis()}",
                            senderName = event.peerName,
                            isMine = false,
                            text = if (!event.serverName.isNullOrBlank()) "Hey Lincoln! Connected from ${event.serverName} server." else "Hey Lincoln! Connected from campus server.",
                            timestamp = "Just now"
                        )
                    )
                    threadMessagesMap[newThreadId] = welcomeMsg
                    _uiState.update {
                        it.copy(
                            currentScreen = MessagesScreenMode.CHAT,
                            selectedThread = newThread,
                            threads = listOf(newThread) + it.threads,
                            messages = welcomeMsg
                        )
                    }
                }
            }

            is MessagesEvent.BackToThreads -> {
                com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.activeVisibleThreadId = null
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
                    senderName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You",
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
                    senderName = _currentProfile.value?.fullName?.ifBlank { "Student" } ?: "Student",
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
                    senderName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You",
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
                            senderName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You",
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
