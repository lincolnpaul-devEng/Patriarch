package com.campus.network.feature.messages

import com.campus.network.core.model.PeerTransactionPayload
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.firstName
import com.campus.network.core.model.initials

enum class MessagesScreenMode {
    THREADS,
    CHAT,
    AUDIO_CALL,
    VIDEO_CALL,
    NEW_CHAT,
    ADD_NOTE,
    BROADCAST
}

enum class ThreadCategory(val label: String) {
    ALL("All"),
    DIRECT("Direct"),
    ACADEMIC("Academic"),
    GIGS("Campus Gigs")
}

data class ConversationThread(
    val id: String,
    val peerName: String,
    val peerHandle: String,
    val avatarInitials: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false,
    val category: ThreadCategory = ThreadCategory.DIRECT,
    val serverBadgeText: String? = null
)

data class DirectMessageItem(
    val id: String,
    val senderName: String,
    /** Stable sender identity used to recompute isMine on every device. */
    val senderId: String = "",
    val isMine: Boolean,
    val text: String,
    val timestamp: String,
    val settlementPayload: PeerTransactionPayload? = null,
    val isPending: Boolean = false,
    val isDelivered: Boolean = true,
    val isRead: Boolean = true,
    val isEdited: Boolean = false,
    val isSystem: Boolean = false,
    val isHandwritten: Boolean = false,
    val imageResId: Int? = null,
    val imageUri: String? = null,
    val imageUrl: String? = null,
    val gifUrl: String? = null,
    val replyToText: String? = null,
    val reaction: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus(val label: String) {
    /** Local user is dialing; waiting for signaling ack. */
    CALLING("Calling"),
    /** Outgoing: remote device is ringing. */
    RINGING("Ringing"),
    /** Incoming invite — show Answer / Decline. */
    INCOMING("Incoming"),
    CONNECTED("Connected"),
    FAILED("Failed")
}

data class CallUiState(
    val peerName: String,
    val peerHandle: String,
    val peerInitials: String,
    val callType: CallType,
    val status: CallStatus = CallStatus.CALLING,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isPeerOnline: Boolean = true,
    /** Honest media status — never claim WebRTC until CallMediaProvider is configured. */
    val connectionQuality: String = "Signaling only",
    val connectionState: String = "Connecting…",
    val callSessionId: String? = null,
    val isIncoming: Boolean = false,
    val mediaProviderReady: Boolean = false,
    val activeVoiceFilter: com.campus.network.feature.messages.audio.VoiceFilterPreset = com.campus.network.feature.messages.audio.VoiceFilterPreset.NATURAL,
    val customPitchShift: Float = 0f,
    val customFormantScale: Float = 1.0f,
    val isSelfMonitoring: Boolean = false
)

data class MessagesUiState(
    val currentScreen: MessagesScreenMode = MessagesScreenMode.THREADS,
    val threads: List<ConversationThread> = emptyList(),
    val selectedThread: ConversationThread? = null,
    val selectedCategory: ThreadCategory = ThreadCategory.ALL,
    val searchQuery: String = "",
    val messages: List<DirectMessageItem> = emptyList(),
    val isSettlementDialogOpen: Boolean = false,
    val callState: CallUiState? = null,
    val currentUserNote: String = "Add note",
    val isVoiceFlowActive: Boolean = false,
    val activateVoiceInChat: Boolean = false,
    val studentProfile: StudentProfile? = null
) {
    val studentName: String
        get() = studentProfile?.fullName?.ifBlank { "Student" } ?: "Student"

    val studentFirstName: String
        get() = studentProfile?.firstName?.ifBlank { "Student" } ?: "Student"

    val studentInitials: String
        get() = studentProfile?.initials ?: "S"
}

sealed interface MessagesEvent {
    data class SelectThread(val threadId: String, val activateVoice: Boolean = false) : MessagesEvent
    data class OpenOrCreateDirectChat(
        val peerId: String,
        val peerName: String,
        val peerHandle: String? = null,
        val avatarInitials: String? = null,
        val serverName: String? = null,
        val serverTag: String? = null
    ) : MessagesEvent
    object ConsumeVoiceActivation : MessagesEvent
    object BackToThreads : MessagesEvent
    data class SearchQueryChanged(val query: String) : MessagesEvent
    data class SelectCategory(val category: ThreadCategory) : MessagesEvent
    data class SendMessage(
        val text: String,
        val imageUri: String? = null,
        val imageUrl: String? = null,
        val gifUrl: String? = null,
        val replyToText: String? = null
    ) : MessagesEvent
    data class DeleteMessage(val messageId: String) : MessagesEvent
    data class ToggleMessageReaction(val messageId: String, val reaction: String) : MessagesEvent
    data class ClearChat(val threadId: String) : MessagesEvent
    data class InitiateSettlement(val amount: Double, val purpose: String) : MessagesEvent
    data class ConfirmSettlement(val transactionId: String) : MessagesEvent
    data class ToggleSettlementDialog(val open: Boolean) : MessagesEvent
    data class StartAudioCall(val thread: ConversationThread) : MessagesEvent
    data class StartVideoCall(val thread: ConversationThread) : MessagesEvent
    /** Present an incoming invite (from poll, FCM deep link, or notification Answer). */
    data class PresentIncomingCall(
        val sessionId: String,
        val peerId: String,
        val peerName: String,
        val isVideo: Boolean
    ) : MessagesEvent
    object EndCall : MessagesEvent
    object DeclineCall : MessagesEvent
    object ToggleMute : MessagesEvent
    object ToggleSpeaker : MessagesEvent
    object ToggleVideo : MessagesEvent
    object FlipCamera : MessagesEvent
    object AcceptCall : MessagesEvent
    data class SetVoiceFilter(val preset: com.campus.network.feature.messages.audio.VoiceFilterPreset) : MessagesEvent
    data class UpdateCustomPitchAndFormant(val pitchSemiTones: Float, val formantScale: Float) : MessagesEvent
    data class ToggleSelfMonitoring(val context: android.content.Context) : MessagesEvent
    object OpenNewChat : MessagesEvent
    object OpenAddNote : MessagesEvent
    object OpenBroadcast : MessagesEvent
    object OpenVoiceChatSelection : MessagesEvent
    data class SaveUserNote(val note: String) : MessagesEvent
    data class SendBroadcast(val peerIds: List<String>, val text: String) : MessagesEvent
    data object FlushPendingMessages : MessagesEvent
}
