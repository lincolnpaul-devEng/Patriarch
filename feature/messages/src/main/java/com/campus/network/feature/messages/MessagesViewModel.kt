package com.campus.network.feature.messages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.campus.network.core.database.dao.ChatMessageDao
import com.campus.network.core.database.entity.ChatMessageEntity
import com.campus.network.core.data.CallSignalingRepository
import com.campus.network.core.data.CampusInboxNotifier
import com.campus.network.core.data.DirectChatTypingRepository
import com.campus.network.core.data.ServerRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.data.SupabaseProfileNameResolver
import com.campus.network.core.model.CallMediaKind
import com.campus.network.core.model.CallSession
import com.campus.network.core.model.CallSessionStatus
import com.campus.network.core.model.FriendStatus
import com.campus.network.core.model.OutgoingCallInvite
import com.campus.network.core.model.PeerTransactionPayload
import com.campus.network.core.model.SettlementStatus
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.cleanDisplayName
import com.campus.network.core.model.cleanProfileDisplayName
import com.campus.network.core.model.firstName
import com.campus.network.core.model.initials
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import com.campus.network.core.network.StreamChatEngine
import com.campus.network.core.network.SupabaseSdkClientProvider
import com.campus.network.core.network.call.CallMediaProvider
import com.campus.network.core.network.call.CallMediaProviderRegistry
import com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
import com.campus.network.feature.messages.audio.CallRingtoneManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.json.JSONArray

@Serializable
data class DirectChatMessageRow(
    val id: String = "",
    val channel_id: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val text: String = "",
    val timestamp_epoch_ms: Long = 0L,
    val is_optimistic: Boolean = false,
    val image_url: String? = null,
    val gif_url: String? = null,
    val reply_to_text: String? = null,
    val is_delivered: Boolean = true,
    val is_read: Boolean = false,
    val reaction: String? = null,
    val settlement_payload_json: String? = null,
    val created_at: String? = null
)

class MessagesViewModel(
    private val streamChatEngine: StreamChatEngine? = null,
    private val telecomCallManager: CampusTelecomCallManager? = null,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val studentProfileRepository: StudentProfileRepository = StudentProfileRepository(sdkClient = supabase),
    private val chatMessageDao: ChatMessageDao? = null,
    private val callSignalingRepository: CallSignalingRepository = CallSignalingRepository(supabase = supabase),
    private val callMediaProvider: CallMediaProvider = CallMediaProviderRegistry.resolve(),
    private val inboxNotifier: CampusInboxNotifier = CampusInboxNotifier(supabase = supabase),
    private val dmTypingRepository: DirectChatTypingRepository = DirectChatTypingRepository(supabase = supabase),
    private val serverRepository: ServerRepository? = null,
    context: Context? = null
) : ViewModel() {

    private val appContext: Context? = context?.applicationContext
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)
    private val remoteProfileNames = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val remoteProfileAvatars = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val ringtoneManager: CallRingtoneManager? = context?.let {
        CallRingtoneManager(it.applicationContext, viewModelScope)
    }

    private var callTimerJob: Job? = null
    private var callSignalPollJob: Job? = null
    private var incomingInvitePollJob: Job? = null
    private var inboxPollJob: Job? = null
    private var dmTypingPollJob: Job? = null
    private var dmTypingHeartbeatJob: Job? = null
    private var activeChannelObservationJob: Job? = null
    private var activeCallSessionId: String? = null
    private val seenIncomingSessionIds = mutableSetOf<String>()
    private val seenRemoteMessageIds = mutableSetOf<String>()
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

    /** Never returns a shared placeholder — blank means identity is unavailable. */
    private fun resolveCurrentUserId(): String = _currentProfile.value?.stableUserId().orEmpty()

    private fun currentProfileName(): String =
        _currentProfile.value?.fullName
            ?.takeIf(String::isRealProfileDisplayName)
            ?.trim()
            .orEmpty()

    private fun profileNameFor(userId: String): String? {
        if (userId.isBlank()) return null
        val trimmed = userId.trim().lowercase()
        return remoteProfileNames[trimmed]?.takeIf(String::isRealProfileDisplayName)
            ?: studentProfileRepository.findKnownProfile(userId)?.fullName?.takeIf(String::isRealProfileDisplayName)
            ?: studentProfileRepository.knownProfiles.value.entries
            .firstOrNull { it.key.equals(trimmed, ignoreCase = true) }
            ?.value
            ?.fullName
            ?.takeIf(String::isRealProfileDisplayName)
            ?.trim()
    }

    private fun profileAvatarFor(userId: String): String? {
        if (userId.isBlank()) return null
        val trimmed = userId.trim().lowercase()
        return remoteProfileAvatars[trimmed]
            ?: studentProfileRepository.findKnownProfile(userId)?.avatarUrl?.takeIf(String::isNotBlank)
            ?: studentProfileRepository.knownProfiles.value.entries
            .firstOrNull { it.key.equals(trimmed, ignoreCase = true) }
            ?.value
            ?.avatarUrl
            ?.takeIf(String::isNotBlank)
    }

    data class CanonicalPeerInfo(
        val peerUserId: String,
        val peerName: String,
        val avatarUrl: String?
    )

    private fun resolvePeerCanonicalIdentity(rawPeerId: String, rawPeerName: String?): CanonicalPeerInfo {
        val cleanName = cleanDisplayName(rawPeerName).ifBlank { cleanDisplayName(rawPeerId) }
        val myId = resolveCurrentUserId()
        val normalizedId = normalizePeerUserId(rawPeerId, myId)

        // 1. Check known profiles by normalized ID, email, UUID, or clean name
        val known = studentProfileRepository.findKnownProfile(normalizedId)
            ?: studentProfileRepository.findKnownProfile(cleanName)
            ?: studentProfileRepository.findKnownProfile(rawPeerId)

        if (known != null) {
            val stableId = known.stableUserId().ifBlank { normalizedId }
            return CanonicalPeerInfo(
                peerUserId = stableId,
                peerName = known.fullName.cleanProfileDisplayName(),
                avatarUrl = known.avatarUrl
            )
        }

        // 2. Check remote cached names & avatars
        val cachedName = remoteProfileNames[normalizedId.lowercase()]
            ?: remoteProfileNames[cleanName.lowercase()]
            ?: cleanName.takeIf { it.isNotBlank() && !looksLikeRawThreadId(it) }
            ?: normalizedId
        val cachedAvatar = remoteProfileAvatars[normalizedId.lowercase()]
            ?: remoteProfileAvatars[cleanName.lowercase()]

        return CanonicalPeerInfo(
            peerUserId = normalizedId,
            peerName = cachedName.cleanProfileDisplayName(),
            avatarUrl = cachedAvatar
        )
    }

    private suspend fun resolveProfileNames(userIds: Collection<String>): Map<String, String> {
        val profiles = profileNameResolver.resolveWithAvatars(userIds)
        remoteProfileNames.putAll(profiles.mapValues { it.value.name })
        profiles.forEach { (userId, profile) ->
            val avatarUrl = profile.avatarUrl
            if (avatarUrl == null) {
                remoteProfileAvatars.remove(userId)
            } else {
                remoteProfileAvatars[userId] = avatarUrl
            }
        }
        return profiles.mapValues { it.value.name }
    }

    private fun mapEntityToMessage(e: ChatMessageEntity, currentUserId: String): DirectMessageItem {
        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(e.timestampEpochMs))
        val isMine = currentUserId.isNotBlank() &&
            e.senderId.isNotBlank() &&
            e.senderId.equals(currentUserId, ignoreCase = true)
        return DirectMessageItem(
            id = e.id,
            senderName = if (isMine) "You" else profileNameFor(e.senderId).orEmpty(),
            senderId = e.senderId,
            isMine = isMine,
            text = e.text,
            timestamp = timeStr,
            // Clock = still queued locally. Delivered/read are separate receipt states.
            isPending = e.isOptimistic,
            isDelivered = e.isDelivered,
            isRead = e.isRead,
            imageUri = e.imageUri,
            imageUrl = e.imageUrl,
            gifUrl = e.gifUrl,
            replyToText = e.replyToText,
            timestampEpochMs = e.timestampEpochMs
        )
    }

    private fun markLocalMessageSent(channelId: String, messageId: String) {
        viewModelScope.launch {
            chatMessageDao?.markMessageDelivered(messageId)
        }
        val activeList = threadMessagesMap[channelId] ?: return
        val updated = activeList.map { msg ->
            if (msg.id == messageId) msg.copy(isPending = false, isDelivered = true) else msg
        }
        threadMessagesMap[channelId] = updated
        if (_uiState.value.selectedThread?.id == channelId) {
            _uiState.update { it.copy(messages = updated) }
        }
    }

    private fun observeChannelMessages(channelId: String, currentUserId: String) {
        val dao = chatMessageDao ?: return
        activeChannelObservationJob?.cancel()
        activeChannelObservationJob = viewModelScope.launch {
            dao.getMessagesForChannel(channelId).collect { entities ->
                if (entities.isNotEmpty()) {
                    val uid = resolveCurrentUserId().ifBlank { currentUserId }
                    val mapped = entities.map { e -> mapEntityToMessage(e, uid) }
                    threadMessagesMap[channelId] = mapped
                    if (_uiState.value.selectedThread?.id == channelId) {
                        _uiState.update { it.copy(messages = mapped) }
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            studentProfileRepository.studentProfile.collect { profile ->
                _currentProfile.value = profile
                val currentUserId = profile.stableUserId()
                // Recompute isMine for in-memory threads when identity becomes available / changes.
                if (currentUserId.isNotBlank()) {
                    registerPushTokenIfPossible(currentUserId)
                    threadMessagesMap.keys.toList().forEach { channelId ->
                        val remapped = threadMessagesMap[channelId]?.map { msg ->
                            if (msg.senderId.isBlank()) msg
                            else msg.copy(isMine = msg.senderId.equals(currentUserId, ignoreCase = true))
                        } ?: return@forEach
                        threadMessagesMap[channelId] = remapped
                    }
                    val selectedId = _uiState.value.selectedThread?.id
                    _uiState.update { current ->
                        current.copy(
                            studentProfile = profile,
                            currentUserNote = profile.statusNote ?: current.currentUserNote,
                            messages = if (selectedId != null) {
                                threadMessagesMap[selectedId] ?: current.messages
                            } else {
                                current.messages
                            }
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            studentProfile = profile,
                            currentUserNote = profile.statusNote ?: current.currentUserNote
                        )
                    }
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

        // Poll Supabase for incoming RINGING invites addressed to this user.
        startIncomingInvitePoll()
        startInboxMessagePoll()
        // Hub-level typing poll so thread list shows green "X is typing…"
        startDmTypingPresencePoll(focusThreadId = null)

        // Drop legacy bogus DM rows (e.g. thread_general from channel-id leaks)
        viewModelScope.launch {
            purgeInvalidDmChannels()
        }

        // Rebuild thread list + real unread counts from local Room cache
        viewModelScope.launch {
            chatMessageDao?.observeAllMessages()?.collect { allMessages ->
                rebuildThreadsFromLocalMessages(allMessages)
            }
        }

        // Observe "Mark as Read" actions from the Android notification shade
        viewModelScope.launch {
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.markReadFlow.collect { markRead ->
                markThreadRead(markRead.threadId)
            }
        }
    }

    /**
     * Channel / guild ids leaked into DMs as `thread_general` etc.
     * Never surface those as conversation peers.
     */
    private fun isInvalidDmThreadId(channelId: String): Boolean {
        val id = channelId.trim().lowercase()
        if (id.isBlank() || id == "thread_" || id == "thread_unknown") return true
        if (id == "general" || id == "thread_general") return true
        if (id.startsWith("ch_")) return true
        if (id.endsWith("_general") && (id.startsWith("thread_") || id.startsWith("ch_"))) return true
        if (id.startsWith("thread_") && id.removePrefix("thread_") in setOf("general", "rules", "announcements")) {
            return true
        }
        return false
    }

    /** True when a display name is still a raw thread/channel id leak. */
    private fun looksLikeRawThreadId(name: String): Boolean {
        val n = name.trim().lowercase()
        if (n.startsWith("thread_") || n.startsWith("dm_") || n.startsWith("ch_") || n.startsWith("mem_")) return true
        if (n == "general" || n.endsWith("_general")) return true
        return false
    }

    private suspend fun purgeInvalidDmChannels() {
        val dao = chatMessageDao ?: return
        val ids = runCatching { dao.getDistinctChannelIds() }.getOrDefault(emptyList())
        ids.filter { isInvalidDmThreadId(it) }.forEach { badId ->
            runCatching { dao.clearChannelMessages(badId) }
            threadMessagesMap.remove(badId)
        }
        _uiState.update { state ->
            state.copy(
                threads = state.threads.filterNot { isInvalidDmThreadId(it.id) || looksLikeRawThreadId(it.peerName) },
                selectedThread = state.selectedThread?.takeUnless {
                    isInvalidDmThreadId(it.id) || looksLikeRawThreadId(it.peerName)
                }
            )
        }
    }

    private fun rebuildThreadsFromLocalMessages(allMessages: List<ChatMessageEntity>) {
        if (allMessages.isEmpty()) {
            _uiState.update { state ->
                state.copy(threads = state.threads.filterNot { isInvalidDmThreadId(it.id) })
            }
            return
        }
        val currentUserId = resolveCurrentUserId()
        val byChannel = allMessages
            .filterNot { isInvalidDmThreadId(it.channelId) }
            .groupBy { it.channelId }
        val existingById = _uiState.value.threads.associateBy { it.id }
        val rebuilt = byChannel.mapNotNull { (channelId, msgs) ->
            val ordered = msgs.sortedBy { it.timestampEpochMs }
            val last = ordered.last()
            val peerMsg = ordered.lastOrNull { msg ->
                currentUserId.isBlank() || !msg.senderId.equals(currentUserId, ignoreCase = true)
            }
            val unread = ordered.count { msg ->
                !msg.isRead &&
                    msg.senderId.isNotBlank() &&
                    currentUserId.isNotBlank() &&
                    !msg.senderId.equals(currentUserId, ignoreCase = true)
            }
            val prior = existingById[channelId]
            val peerUserId = peerMsg?.senderId?.takeIf { it.isNotBlank() } ?: prior?.peerUserId.orEmpty()
            val resolvedName = profileNameFor(peerUserId)
                ?: peerMsg?.senderName?.takeIf(String::isRealProfileDisplayName)
                ?: prior?.peerName?.takeIf(String::isRealProfileDisplayName)
                ?: peerUserId.takeUnless { looksLikeRawThreadId(it) }
                ?: return@mapNotNull null
            val peerHandle = prior?.peerHandle
                ?.takeUnless { looksLikeRawThreadId(it.removePrefix("@")) }
                .orEmpty()
            val initials = prior?.avatarInitials
                ?.takeUnless { it.equals("T", true) && looksLikeRawThreadId(prior.peerName) }
                ?: resolvedName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                    .ifBlank { "P" }
            val timeLabel = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                .format(java.util.Date(last.timestampEpochMs))
            ConversationThread(
                id = channelId,
                peerName = resolvedName,
                peerUserId = peerUserId,
                peerHandle = peerHandle,
                avatarInitials = initials,
                avatarUrl = profileAvatarFor(peerUserId) ?: prior?.avatarUrl,
                lastMessage = last.text.ifBlank {
                    when {
                        !last.gifUrl.isNullOrBlank() -> "[GIF]"
                        !last.imageUrl.isNullOrBlank() || !last.imageUri.isNullOrBlank() -> "[Photo]"
                        else -> "Attachment"
                    }
                },
                lastTimestamp = timeLabel,
                unreadCount = unread,
                isOnline = prior?.isOnline ?: false,
                isVerified = prior?.isVerified ?: false,
                category = prior?.category ?: ThreadCategory.DIRECT,
                serverBadgeText = prior?.serverBadgeText,
                typingLabel = prior?.typingLabel
            )
        }.sortedByDescending { thread ->
            byChannel[thread.id]?.maxOfOrNull { it.timestampEpochMs } ?: 0L
        }

        // Keep any in-memory threads that have no local messages yet (freshly opened)
        val rebuiltIds = rebuilt.map { it.id }.toSet()
        val extras = _uiState.value.threads.mapNotNull { thread ->
            if (thread.id in rebuiltIds || isInvalidDmThreadId(thread.id)) {
                null
            } else {
                profileNameFor(thread.peerUserId)?.let { name ->
                    thread.copy(
                        peerName = name,
                        avatarUrl = profileAvatarFor(thread.peerUserId) ?: thread.avatarUrl
                    )
                }
            }
        }
        val merged = (rebuilt + extras).distinctBy { it.id }
        val deduplicated = mutableListOf<ConversationThread>()
        val seenPeers = mutableSetOf<String>()
        for (thread in merged) {
            val cleanName = cleanDisplayName(thread.peerName).ifBlank { thread.peerName }
            val canonical = resolvePeerCanonicalIdentity(thread.peerUserId, cleanName)
            val canonicalName = canonical.peerName.ifBlank { cleanName }
            val canonicalId = canonical.peerUserId.ifBlank { thread.peerUserId }
            val canonicalAvatar = canonical.avatarUrl
                ?: profileAvatarFor(canonicalId)
                ?: profileAvatarFor(thread.peerUserId)
                ?: profileAvatarFor(canonicalName)
                ?: thread.avatarUrl

            val key = canonicalName.lowercase().takeIf { it.isNotBlank() && it != "campus peer" }
                ?: canonicalId.lowercase().takeIf { it.isNotBlank() }
                ?: thread.id
            if (seenPeers.add(key)) {
                deduplicated.add(
                    thread.copy(
                        peerName = canonicalName,
                        peerUserId = canonicalId.takeUnless { looksLikeRawThreadId(it) } ?: thread.peerUserId,
                        avatarUrl = canonicalAvatar
                    )
                )
            } else {
                val idx = deduplicated.indexOfFirst {
                    val itClean = cleanDisplayName(it.peerName).lowercase()
                    (itClean.isNotBlank() && itClean == canonicalName.lowercase()) ||
                        (it.peerUserId.isNotBlank() && it.peerUserId.equals(canonicalId, ignoreCase = true)) ||
                        (it.peerUserId.isNotBlank() && it.peerUserId.equals(thread.peerUserId, ignoreCase = true))
                }
                if (idx >= 0) {
                    val prior = deduplicated[idx]
                    val mergedUnread = prior.unreadCount + thread.unreadCount
                    val newer = if (thread.lastTimestamp == "Just now" || thread.unreadCount > 0) thread else prior
                    deduplicated[idx] = newer.copy(
                        peerName = canonicalName,
                        peerUserId = canonicalId.takeUnless { looksLikeRawThreadId(it) } ?: prior.peerUserId,
                        unreadCount = mergedUnread,
                        avatarUrl = canonicalAvatar ?: prior.avatarUrl ?: thread.avatarUrl
                    )
                }
            }
        }

        val selectedId = _uiState.value.selectedThread?.id
        _uiState.update { state ->
            val selected = selectedId?.let { id -> deduplicated.find { it.id == id } }
                ?.takeUnless { isInvalidDmThreadId(it.id) }
            state.copy(
                threads = deduplicated,
                selectedThread = selected,
                currentScreen = if (selected == null && state.currentScreen == MessagesScreenMode.CHAT) {
                    MessagesScreenMode.THREADS
                } else {
                    state.currentScreen
                }
            )
        }
    }

    private fun markThreadRead(threadId: String) {
        viewModelScope.launch {
            chatMessageDao?.markChannelMessagesRead(threadId)
            val me = resolveCurrentUserId()
            if (me.isNotBlank()) {
                runCatching {
                    supabase.from("campus_chat_messages").update(
                        mapOf("is_read" to true)
                    ) {
                        filter {
                            eq("channel_id", threadId)
                            neq("sender_id", me)
                            eq("is_read", false)
                        }
                    }
                }
            }
        }
        _uiState.update { state ->
            state.copy(
                threads = state.threads.map { t ->
                    if (t.id == threadId) t.copy(unreadCount = 0) else t
                },
                selectedThread = state.selectedThread?.let {
                    if (it.id == threadId) it.copy(unreadCount = 0) else it
                }
            )
        }
    }

    private fun startInboxMessagePoll() {
        inboxPollJob?.cancel()
        inboxPollJob = viewModelScope.launch {
            while (isActive) {
                pollIncomingDirectMessages()
                delay(8_000L)
            }
        }
    }

    private suspend fun pollIncomingDirectMessages() {
        val me = resolveCurrentUserId()
        if (me.isBlank()) return
        val list = runCatching {
            supabase.from("campus_chat_messages").select {
                order("timestamp_epoch_ms", order = Order.DESCENDING)
                limit(50)
            }.decodeList<DirectChatMessageRow>()
        }.getOrNull() ?: return
        if (list.isEmpty()) return

        val senderIds = list.map { it.sender_id }.filter { it.isNotBlank() }
        val profileNames = runCatching { resolveProfileNames(senderIds) }.getOrDefault(emptyMap())
        val dao = chatMessageDao
        val activeThreadId = _uiState.value.selectedThread?.id
        val viewingChat = _uiState.value.currentScreen == MessagesScreenMode.CHAT

        for (obj in list) {
            val id = obj.id
            val channelId = obj.channel_id
            if (id.isBlank() || channelId.isBlank()) continue
            // Only accept DMs that include this user in the channel id, or already-known threads, or sent by me
            val isKnownThread = _uiState.value.threads.any { it.id == channelId } ||
                threadMessagesMap.containsKey(channelId)
            val isMyDm = channelId.contains(me, ignoreCase = true)
            val isSenderMe = obj.sender_id.equals(me, ignoreCase = true)
            if (!isKnownThread && !isMyDm && !isSenderMe) continue
            if (!seenRemoteMessageIds.add(id)) continue

            val senderId = obj.sender_id
            val isMine = isSenderMe
            val senderName = if (isMine) "You" else (
                profileNames[senderId.lowercase()]?.takeIf(String::isRealProfileDisplayName)
                    ?: obj.sender_name.takeIf(String::isRealProfileDisplayName)
                    ?: "Campus Peer"
            )
            val text = obj.text
            val ts = if (obj.timestamp_epoch_ms > 0L) obj.timestamp_epoch_ms else System.currentTimeMillis()
            val isReadRemote = obj.is_read
            val viewingThis = viewingChat && activeThreadId == channelId
            val entity = ChatMessageEntity(
                id = id,
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestampEpochMs = ts,
                isOptimistic = false,
                imageUrl = obj.image_url?.takeIf { it.isNotBlank() },
                gifUrl = obj.gif_url?.takeIf { it.isNotBlank() },
                replyToText = obj.reply_to_text?.takeIf { it.isNotBlank() },
                isMine = isMine,
                isDelivered = true,
                isRead = viewingThis || isReadRemote
            )
            dao?.insertMessage(entity)

            if (viewingThis) {
                markThreadRead(channelId)
                val current = threadMessagesMap[channelId] ?: emptyList()
                if (current.none { it.id == id }) {
                    val updated = current + mapEntityToMessage(entity, me)
                    threadMessagesMap[channelId] = updated
                    _uiState.update { it.copy(messages = updated) }
                }
            } else if (!isMine && !isReadRemote && senderName.isNotBlank()) {
                appContext?.let { ctx ->
                    CampusSystemNotificationCenter.showDirectMessageNotification(
                        context = ctx,
                        threadId = channelId,
                        senderName = senderName,
                        messageText = text.ifBlank { "Sent you a message" }
                    )
                }
            }
        }
    }

    /** org.json returns the literal "null" for JSON null — never treat that as a real value. */
    private fun org.json.JSONObject.cleanJsonString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "").trim()
        return value.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    /** Peel mem_/thread_ prefixes and server suffixes down to a stable peer user id. */
    private fun normalizePeerUserId(rawPeerId: String, myUserId: String): String {
        var id = rawPeerId.trim()
        if (id.isBlank()) return ""
        if (id.startsWith("dm_")) {
            val without = id.removePrefix("dm_")
            if (myUserId.isNotBlank() && without.contains(myUserId, ignoreCase = true)) {
                return without.replace(myUserId, "", ignoreCase = true).trim('_')
            }
            val parts = without.split("_").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return if (myUserId.isNotBlank() && parts[0].equals(myUserId, ignoreCase = true)) parts[1] else parts[0]
            }
            return without.trim('_')
        }
        id = id.removePrefix("thread_").removePrefix("mem_")
        val srvIdx = id.lastIndexOf("_srv")
        if (srvIdx > 0) id = id.substring(0, srvIdx)
        return id.trim('_')
    }

    private fun resolveDirectThreadId(myUserId: String, peerUserId: String, fallbackPeerId: String): String {
        return when {
            myUserId.isNotBlank() && peerUserId.isNotBlank() ->
                runCatching { com.campus.network.core.model.directMessageChannelId(myUserId, peerUserId) }
                    .getOrElse { "dm_${myUserId}_$peerUserId" }
            peerUserId.isNotBlank() -> "thread_$peerUserId"
            fallbackPeerId.startsWith("dm_") || fallbackPeerId.startsWith("thread_") -> fallbackPeerId
            fallbackPeerId.isNotBlank() -> "thread_${fallbackPeerId.trim()}"
            else -> "thread_unknown"
        }
    }

    private fun resolvePeerUserId(thread: ConversationThread, myUserId: String): String {
        if (thread.peerUserId.isNotBlank()) return thread.peerUserId
        val id = thread.id.trim()
        if (id.startsWith("dm_") && myUserId.isNotBlank()) {
            val without = id.removePrefix("dm_")
            if (without.contains(myUserId, ignoreCase = true)) {
                return without.replace(myUserId, "", ignoreCase = true).trim('_')
            }
        }
        return thread.peerHandle.removePrefix("@").ifBlank { id.removePrefix("thread_") }
    }

    private fun openOrCreateDirectChat(
        event: MessagesEvent.OpenOrCreateDirectChat,
        peerUserId: String,
        threadId: String,
        peerName: String
    ) {
        val canonical = resolvePeerCanonicalIdentity(peerUserId, peerName)
        val safePeerName = canonical.peerName.ifBlank { "Campus Peer" }
        val safePeerUserId = canonical.peerUserId.ifBlank { peerUserId }
        val resolvedAvatar = event.avatarUrl?.takeIf(String::isNotBlank)
            ?: canonical.avatarUrl
            ?: profileAvatarFor(safePeerUserId)
            ?: profileAvatarFor(peerUserId)

        val currentUserId = resolveCurrentUserId()

        val existing = _uiState.value.threads.find {
            it.id == threadId || it.id == event.peerId ||
                it.id == "thread_${event.peerId}" ||
                it.peerUserId.equals(safePeerUserId, ignoreCase = true) ||
                it.peerUserId.equals(peerUserId, ignoreCase = true) ||
                (safePeerName != "Campus Peer" && it.peerName.cleanProfileDisplayName().equals(safePeerName, ignoreCase = true))
        }
        if (existing != null) {
            val canonicalThread = existing.copy(
                peerName = safePeerName,
                peerUserId = safePeerUserId.takeUnless { looksLikeRawThreadId(it) } ?: existing.peerUserId,
                avatarUrl = resolvedAvatar ?: existing.avatarUrl
            )
            CampusSystemNotificationCenter.activeVisibleThreadId = existing.id
            val messages = threadMessagesMap[existing.id] ?: emptyList()
            _uiState.update {
                it.copy(
                    currentScreen = MessagesScreenMode.CHAT,
                    selectedThread = canonicalThread.copy(unreadCount = 0),
                    threads = it.threads.mapNotNull { thread ->
                        if (thread.id == canonicalThread.id) {
                            canonicalThread
                        } else if (
                            (safePeerUserId.isNotBlank() && thread.peerUserId.equals(safePeerUserId, ignoreCase = true)) ||
                            (safePeerName != "Campus Peer" && thread.peerName.cleanProfileDisplayName().equals(safePeerName, ignoreCase = true))
                        ) {
                            null
                        } else {
                            thread
                        }
                    },
                    messages = messages,
                    activeChatTypingLabel = existing.typingLabel
                )
            }
            markThreadRead(existing.id)
            startDmTypingPresencePoll(focusThreadId = existing.id)
            observeChannelMessages(existing.id, currentUserId)
            viewModelScope.launch { syncRemoteMessagesForChannel(existing.id) }
            return
        }

        CampusSystemNotificationCenter.activeVisibleThreadId = threadId
        val initials = safePeerName.split(" ").take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
            .ifBlank { "P" }
        val badge = if (!event.serverTag.isNullOrBlank()) {
            "${event.serverTag.uppercase()} • ${event.serverName ?: "Server"}"
        } else {
            event.serverName
        }
        val newThread = ConversationThread(
            id = threadId,
            peerName = safePeerName,
            peerUserId = safePeerUserId,
            peerHandle = event.peerHandle.orEmpty(),
            avatarInitials = initials,
            avatarUrl = resolvedAvatar,
            lastMessage = "No messages yet",
            lastTimestamp = "Just now",
            unreadCount = 0,
            isOnline = true,
            isVerified = true,
            category = ThreadCategory.DIRECT,
            serverBadgeText = badge
        )
        val messages = threadMessagesMap[threadId] ?: emptyList()
        _uiState.update {
            it.copy(
                currentScreen = MessagesScreenMode.CHAT,
                selectedThread = newThread,
                threads = listOf(newThread) + it.threads.filterNot { thread ->
                    thread.id == threadId ||
                        (safePeerUserId.isNotBlank() && thread.peerUserId.equals(safePeerUserId, ignoreCase = true)) ||
                        (safePeerName != "Campus Peer" && thread.peerName.cleanProfileDisplayName().equals(safePeerName, ignoreCase = true))
                },
                messages = messages,
                activeChatTypingLabel = null
            )
        }
        startDmTypingPresencePoll(focusThreadId = threadId)
        observeChannelMessages(threadId, currentUserId)
        viewModelScope.launch { syncRemoteMessagesForChannel(threadId) }
    }

    private fun applyInlineNotificationReply(
        threadId: String,
        peerName: String,
        replyText: String,
        timestampLabel: String
    ) {
        val currentUserFirstName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You"
        val authorId = resolveCurrentUserId()
        val newMsg = DirectMessageItem(
            id = "msg_inline_${System.currentTimeMillis()}",
            senderName = currentUserFirstName,
            senderId = authorId,
            isMine = authorId.isNotBlank(),
            text = replyText,
            timestamp = timestampLabel,
            isPending = true,
            isDelivered = false,
            isRead = false
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
                    _uiState.update {
                        it.copy(
                            currentScreen = MessagesScreenMode.CHAT,
                            selectedThread = thread.copy(unreadCount = 0),
                            messages = messages,
                            activateVoiceInChat = event.activateVoice || it.isVoiceFlowActive,
                            isVoiceFlowActive = false,
                            activeChatTypingLabel = thread.typingLabel
                        )
                    }
                    markThreadRead(thread.id)
                    startDmTypingPresencePoll(focusThreadId = thread.id)
                    val currentUserId = resolveCurrentUserId()
                    observeChannelMessages(thread.id, currentUserId)
                    // Pull remote history so peer messages arrive with correct senderId / isMine.
                    viewModelScope.launch {
                        syncRemoteMessagesForChannel(thread.id)
                    }
                }
            }

            is MessagesEvent.ConsumeVoiceActivation -> {
                _uiState.update { it.copy(activateVoiceInChat = false) }
            }

            is MessagesEvent.OpenOrCreateDirectChat -> {
                val myId = resolveCurrentUserId()
                val peerUserId = normalizePeerUserId(event.peerId, myId)
                if (isInvalidDmThreadId(event.peerId) || isInvalidDmThreadId("thread_$peerUserId") ||
                    peerUserId.equals("general", true) ||
                    peerUserId.equals("rules", true) ||
                    peerUserId.equals("announcements", true)
                ) {
                    Log.w(TAG, "Refusing bogus DM peerId='${event.peerId}' / peerUserId='$peerUserId'")
                    return
                }

                val canonical = resolvePeerCanonicalIdentity(peerUserId, event.peerName)
                val targetPeerId = canonical.peerUserId.ifBlank { peerUserId }
                val targetPeerName = canonical.peerName.ifBlank {
                    cleanDisplayName(event.peerName).takeIf { it.isNotBlank() } ?: targetPeerId
                }
                val threadId = resolveDirectThreadId(myId, targetPeerId, event.peerId)
                if (isInvalidDmThreadId(threadId)) {
                    Log.w(TAG, "Refusing bogus DM threadId='$threadId'")
                    return
                }

                openOrCreateDirectChat(event, targetPeerId, threadId, targetPeerName)

                viewModelScope.launch {
                    val resolvedMap = resolveProfileNames(listOf(targetPeerId, peerUserId, targetPeerName))
                    val resolvedName = resolvedMap[targetPeerId.lowercase()]
                        ?: resolvedMap[peerUserId.lowercase()]
                        ?: resolvedMap[targetPeerName.lowercase()]
                    val resolvedAvatar = remoteProfileAvatars[targetPeerId.lowercase()]
                        ?: remoteProfileAvatars[peerUserId.lowercase()]
                    if (!resolvedName.isNullOrBlank()) {
                        val cleanResolved = cleanDisplayName(resolvedName)
                        _uiState.update { state ->
                            val updatedThreads = state.threads.map { t ->
                                if (t.id == threadId || t.peerUserId.equals(targetPeerId, ignoreCase = true) ||
                                    t.peerName.cleanProfileDisplayName().equals(cleanResolved, ignoreCase = true)
                                ) {
                                    t.copy(
                                        peerName = cleanResolved,
                                        avatarUrl = resolvedAvatar ?: t.avatarUrl
                                    )
                                } else t
                            }
                            val updatedSelected = if (state.selectedThread?.id == threadId ||
                                state.selectedThread?.peerUserId.equals(targetPeerId, ignoreCase = true) ||
                                state.selectedThread?.peerName?.cleanProfileDisplayName().equals(cleanResolved, ignoreCase = true)
                            ) {
                                state.selectedThread?.copy(
                                    peerName = cleanResolved,
                                    avatarUrl = resolvedAvatar ?: state.selectedThread.avatarUrl
                                )
                            } else state.selectedThread
                            state.copy(threads = updatedThreads, selectedThread = updatedSelected)
                        }
                    }
                }
            }

            is MessagesEvent.BackToThreads -> {
                publishDmTyping(false)
                com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.activeVisibleThreadId = null
                _uiState.update {
                    it.copy(
                        currentScreen = MessagesScreenMode.THREADS,
                        selectedThread = null,
                        activeChatTypingLabel = null
                    )
                }
                startDmTypingPresencePoll(focusThreadId = null)
            }

            is MessagesEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }

            is MessagesEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
            }

            is MessagesEvent.SetDirectChatTyping -> {
                publishDmTyping(event.isTyping)
            }

            is MessagesEvent.SendMessage -> {
                if (event.text.isBlank() && event.imageUri == null && event.imageUrl == null && event.gifUrl == null) return
                val activeThread = _uiState.value.selectedThread ?: return
                val authorId = resolveCurrentUserId()
                if (authorId.isBlank()) {
                    // Refuse to send without a stable identity — otherwise bubbles collide across users.
                    return
                }
                publishDmTyping(false)
                val nowTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                val newMsg = DirectMessageItem(
                    id = "msg_${java.util.UUID.randomUUID()}",
                    senderName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You",
                    senderId = authorId,
                    isMine = true,
                    text = event.text.trim(),
                    timestamp = nowTime,
                    isPending = true,
                    isDelivered = false,
                    isRead = false,
                    imageUri = event.imageUri,
                    imageUrl = event.imageUrl,
                    gifUrl = event.gifUrl,
                    replyToText = event.replyToText,
                    timestampEpochMs = System.currentTimeMillis()
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

                val entity = ChatMessageEntity(
                    id = newMsg.id,
                    channelId = activeThread.id,
                    senderId = authorId,
                    senderName = currentProfileName(),
                    text = newMsg.text,
                    timestampEpochMs = newMsg.timestampEpochMs,
                    isOptimistic = true,
                    imageUri = newMsg.imageUri,
                    imageUrl = newMsg.imageUrl,
                    gifUrl = newMsg.gifUrl,
                    replyToText = newMsg.replyToText,
                    isMine = true,
                    isDelivered = false,
                    isRead = false
                )

                viewModelScope.launch {
                    // Local Room first (offline cache). Supabase remote sync is mandatory when online.
                    chatMessageDao?.insertMessage(entity)
                        ?: run {
                            Log.e(TAG, "chatMessageDao missing — cannot persist DM locally")
                        }
                    val remoteOk = dispatchMessageToRemote(entity)
                    // Stream is supplemental fan-out only — never clears the pending clock by itself.
                    streamChatEngine
                        ?.sendDirectMessage(
                            channelId = activeThread.id,
                            text = event.text.trim()
                        )
                    if (remoteOk) {
                        val peerId = resolvePeerUserId(activeThread, authorId)
                        inboxNotifier.notifyDirectMessage(
                            recipientUserId = peerId,
                            senderUserId = authorId,
                            threadId = activeThread.id,
                            messageText = displaySnippet
                        )
                    }
                }
            }

            is MessagesEvent.DeleteMessage -> {
                val activeThread = _uiState.value.selectedThread ?: return
                val currentList = threadMessagesMap[activeThread.id] ?: _uiState.value.messages
                val updated = currentList.filter { it.id != event.messageId }
                threadMessagesMap[activeThread.id] = updated
                _uiState.update { it.copy(messages = updated) }
                viewModelScope.launch {
                    chatMessageDao?.deleteMessage(event.messageId)
                    runCatching {
                        supabase.from("campus_chat_messages").delete {
                            filter { eq("id", event.messageId) }
                        }
                    }
                }
            }

            is MessagesEvent.ToggleMessageReaction -> {
                val activeThread = _uiState.value.selectedThread ?: return
                val currentList = threadMessagesMap[activeThread.id] ?: _uiState.value.messages
                var chosenReaction: String? = null
                val updated = currentList.map { msg ->
                    if (msg.id == event.messageId) {
                        val newReaction = if (msg.reaction == event.reaction) null else event.reaction
                        chosenReaction = newReaction
                        msg.copy(reaction = newReaction)
                    } else msg
                }
                threadMessagesMap[activeThread.id] = updated
                _uiState.update { it.copy(messages = updated) }
                viewModelScope.launch {
                    runCatching {
                        supabase.from("campus_chat_messages").update(
                            mapOf("reaction" to chosenReaction)
                        ) {
                            filter { eq("id", event.messageId) }
                        }
                    }
                }
            }

            is MessagesEvent.ClearChat -> {
                threadMessagesMap[event.threadId] = emptyList()
                if (_uiState.value.selectedThread?.id == event.threadId) {
                    _uiState.update { it.copy(messages = emptyList()) }
                }
                viewModelScope.launch {
                    chatMessageDao?.clearChannelMessages(event.threadId)
                    runCatching {
                        supabase.from("campus_chat_messages").delete {
                            filter { eq("channel_id", event.threadId) }
                        }
                    }
                }
            }

            is MessagesEvent.InitiateSettlement -> {
                val activeThread = _uiState.value.selectedThread ?: return
                val txUuid = java.util.UUID.randomUUID().toString()
                val txPayload = PeerTransactionPayload(
                    transactionId = "tx-$txUuid",
                    senderName = currentProfileName(),
                    recipientName = activeThread.peerName,
                    amount = event.amount,
                    currency = "KES",
                    purpose = event.purpose,
                    status = SettlementStatus.ESCROW_LOCKED,
                    timestampMs = System.currentTimeMillis(),
                    referenceCode = "ESCROW-${txUuid.take(8).uppercase()}"
                )
                val settlementAuthorId = resolveCurrentUserId()
                val newMsg = DirectMessageItem(
                    id = "msg_${java.util.UUID.randomUUID()}",
                    senderName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You",
                    senderId = settlementAuthorId,
                    isMine = settlementAuthorId.isNotBlank(),
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
                stopCall(remoteNotify = true)
            }

            is MessagesEvent.ToggleMute -> {
                val nextMuted = !(_uiState.value.callState?.isMuted ?: false)
                viewModelScope.launch {
                    callMediaProvider.setMuted(nextMuted)
                }
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isMuted = nextMuted))
                    } ?: state
                }
            }

            is MessagesEvent.ToggleSpeaker -> {
                val nextSpeaker = !(_uiState.value.callState?.isSpeakerOn ?: true)
                telecomCallManager?.setSpeakerOn(nextSpeaker)
                viewModelScope.launch {
                    callMediaProvider.setSpeakerphone(nextSpeaker)
                }
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isSpeakerOn = nextSpeaker))
                    } ?: state
                }
            }

            is MessagesEvent.ToggleVideo -> {
                val nextEnabled = !(_uiState.value.callState?.isVideoEnabled ?: true)
                viewModelScope.launch {
                    callMediaProvider.setVideoEnabled(nextEnabled)
                }
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isVideoEnabled = nextEnabled))
                    } ?: state
                }
            }

            is MessagesEvent.FlipCamera -> {
                viewModelScope.launch {
                    callMediaProvider.flipCamera()
                }
                _uiState.update { state ->
                    state.callState?.let { call ->
                        state.copy(callState = call.copy(isFrontCamera = !call.isFrontCamera))
                    } ?: state
                }
            }

            is MessagesEvent.PresentIncomingCall -> {
                viewModelScope.launch {
                    val peerName = resolveProfileNames(listOf(event.peerId))[event.peerId.lowercase()]
                    if (peerName != null) {
                        presentIncomingCall(
                            sessionId = event.sessionId,
                            peerId = event.peerId,
                            peerName = peerName,
                            isVideo = event.isVideo
                        )
                    }
                }
            }

            is MessagesEvent.AcceptCall -> {
                acceptIncomingCall()
            }

            is MessagesEvent.DeclineCall -> {
                declineIncomingCall()
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

            is MessagesEvent.OpenFindFriends -> {
                _uiState.update {
                    it.copy(
                        currentScreen = MessagesScreenMode.FIND_FRIENDS,
                        findFriendsFeedback = null,
                        isFindFriendsLoading = true
                    )
                }
                refreshDiscoverablePeers()
            }

            is MessagesEvent.FindFriendsQueryChanged -> {
                _uiState.update { it.copy(findFriendsQuery = event.query) }
                refreshDiscoverablePeers(event.query)
            }

            is MessagesEvent.SendCampusFriendRequest -> {
                val myId = resolveCurrentUserId()
                if (myId.isBlank()) return
                viewModelScope.launch {
                    val result = runCatching {
                        (serverRepository ?: error("Friend services are unavailable"))
                            .sendFriendRequest(userId = myId, friendUserId = event.peerUserId)
                    }
                    _uiState.update { state ->
                        if (result.isSuccess) {
                            state.copy(
                                findFriendsFeedback = "Friend request sent to ${result.getOrThrow()}",
                                discoverablePeers = state.discoverablePeers.map { peer ->
                                    if (peer.userId.equals(event.peerUserId, ignoreCase = true)) {
                                        peer.copy(friendStatus = DiscoverFriendStatus.REQUEST_SENT)
                                    } else peer
                                }
                            )
                        } else {
                            state.copy(findFriendsFeedback = "Friend request could not be sent.")
                        }
                    }
                }
            }

            is MessagesEvent.AcceptCampusFriendRequest -> {
                val myId = resolveCurrentUserId()
                if (myId.isBlank()) return
                viewModelScope.launch {
                    val result = runCatching {
                        (serverRepository ?: error("Friend services are unavailable"))
                            .acceptFriendRequest(userId = myId, friendUserId = event.peerUserId)
                    }
                    _uiState.update { state ->
                        if (result.isSuccess) {
                            state.copy(
                                findFriendsFeedback = "You are now friends with ${result.getOrThrow()}",
                                discoverablePeers = state.discoverablePeers.map { peer ->
                                    if (peer.userId.equals(event.peerUserId, ignoreCase = true)) {
                                        peer.copy(friendStatus = DiscoverFriendStatus.FRIENDS)
                                    } else peer
                                }
                            )
                        } else {
                            state.copy(findFriendsFeedback = "Friend request could not be accepted.")
                        }
                    }
                }
            }

            is MessagesEvent.MessageCampusFriend -> {
                onEvent(
                    MessagesEvent.OpenOrCreateDirectChat(
                        peerId = event.peerUserId,
                        peerName = event.peerName,
                        avatarUrl = event.avatarUrl
                    )
                )
            }

            is MessagesEvent.DismissFindFriendsFeedback -> {
                _uiState.update { it.copy(findFriendsFeedback = null) }
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
                        val broadcastAuthorId = resolveCurrentUserId()
                        val newMsg = DirectMessageItem(
                            id = "msg-${System.currentTimeMillis()}-${peerId.hashCode()}",
                            senderName = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You",
                            senderId = broadcastAuthorId,
                            isMine = broadcastAuthorId.isNotBlank(),
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

            is MessagesEvent.FlushPendingMessages -> {
                flushPendingMessages()
            }
        }
    }

    private fun startCall(thread: ConversationThread, type: CallType) {
        val callerId = resolveCurrentUserId()
        if (callerId.isBlank()) {
            Log.w(TAG, "Cannot start call without stable user id")
            return
        }
        // Prefer peer's stable id from thread.id when it is a dm_* channel or raw user id.
        val calleeId = resolveCalleeId(thread)
        if (calleeId.isBlank() || calleeId.equals(callerId, ignoreCase = true)) {
            Log.w(TAG, "Cannot resolve callee id for thread=${thread.id}")
            _uiState.update { state ->
                state.copy(
                    callState = CallUiState(
                        peerName = thread.peerName,
                        peerHandle = thread.peerHandle,
                        peerInitials = thread.avatarInitials,
                        callType = type,
                        status = CallStatus.FAILED,
                        connectionState = "Cannot place call — peer identity missing",
                        connectionQuality = "Signaling failed",
                        mediaProviderReady = callMediaProvider.isConfigured
                    ),
                    currentScreen = if (type == CallType.VIDEO) {
                        MessagesScreenMode.VIDEO_CALL
                    } else {
                        MessagesScreenMode.AUDIO_CALL
                    }
                )
            }
            return
        }

        callTimerJob?.cancel()
        callSignalPollJob?.cancel()
        activeCallSessionId = null

        val mediaReady = callMediaProvider.isConfigured
        val newCallState = CallUiState(
            peerName = thread.peerName,
            peerHandle = thread.peerHandle,
            peerInitials = thread.avatarInitials,
            callType = type,
            status = CallStatus.CALLING,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = true,
            isVideoEnabled = (type == CallType.VIDEO),
            isFrontCamera = true,
            isPeerOnline = thread.isOnline,
            isIncoming = false,
            connectionQuality = if (mediaReady) callMediaProvider.displayName else "Signaling only",
            connectionState = "Calling…",
            mediaProviderReady = mediaReady
        )
        val nextMode = if (type == CallType.VIDEO) {
            MessagesScreenMode.VIDEO_CALL
        } else {
            MessagesScreenMode.AUDIO_CALL
        }
        _uiState.update {
            it.copy(callState = newCallState, currentScreen = nextMode)
        }

        ringtoneManager?.startOutgoingRingback()
        telecomCallManager?.startOutgoingCall(
            peerName = thread.peerName,
            peerHandle = thread.peerHandle,
            isVideo = (type == CallType.VIDEO),
            onDisconnectBySystem = { stopCall(remoteNotify = true) }
        )

        viewModelScope.launch {
            val invite = OutgoingCallInvite(
                channelId = thread.id,
                callerId = callerId,
                callerName = currentProfileName(),
                calleeId = calleeId,
                calleeName = thread.peerName,
                mediaKind = if (type == CallType.VIDEO) CallMediaKind.VIDEO else CallMediaKind.AUDIO
            )
            val created = callSignalingRepository.createOutgoingInvite(invite)
            created.onFailure { err ->
                Log.e(TAG, "Failed to create call session", err)
                ringtoneManager?.stop()
                _uiState.update { state ->
                    state.copy(
                        callState = state.callState?.copy(
                            status = CallStatus.FAILED,
                            connectionState = err.message ?: "Signaling failed"
                        )
                    )
                }
                return@launch
            }
            val session = created.getOrThrow()
            activeCallSessionId = session.id
            seenIncomingSessionIds.add(session.id)
            _uiState.update { state ->
                state.copy(
                    callState = state.callState?.copy(
                        callSessionId = session.id,
                        status = CallStatus.RINGING,
                        connectionState = "Ringing…"
                    )
                )
            }
            startOutgoingSignalPoll(session.id)
            startConnectedDurationTimer()
        }
    }

    private fun resolveCalleeId(thread: ConversationThread): String {
        val myId = resolveCurrentUserId().lowercase()
        val id = thread.id.trim()
        if (id.startsWith("dm_")) {
            val parts = id.removePrefix("dm_").split("_")
            // dm_{a}_{b} where a/b are sorted stable ids (may contain underscores — take remaining)
            // Our channel format is dm_${min}_${max} with sorted full ids joined by single underscore
            // between two ids. Safer: strip known self id from channel.
            val withoutPrefix = id.removePrefix("dm_")
            if (myId.isNotBlank() && withoutPrefix.contains(myId)) {
                val other = withoutPrefix
                    .replace(myId, "")
                    .trim('_')
                if (other.isNotBlank()) return other
            }
        }
        // Marketplace / legacy threads may use peer handle as id — still signal with that key.
        return id.removePrefix("thread_").ifBlank { thread.peerHandle.removePrefix("@") }
    }

    private fun presentIncomingCall(
        sessionId: String,
        peerId: String,
        peerName: String,
        isVideo: Boolean
    ) {
        if (_uiState.value.callState != null &&
            _uiState.value.callState?.callSessionId == sessionId &&
            _uiState.value.callState?.status == CallStatus.INCOMING
        ) {
            return
        }
        callTimerJob?.cancel()
        callSignalPollJob?.cancel()
        activeCallSessionId = sessionId
        seenIncomingSessionIds.add(sessionId)

        val initials = peerName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { "P" }
        val mediaReady = callMediaProvider.isConfigured
        val callState = CallUiState(
            peerName = peerName,
            peerHandle = "@${peerId.take(12)}",
            peerInitials = initials,
            callType = if (isVideo) CallType.VIDEO else CallType.AUDIO,
            status = CallStatus.INCOMING,
            isIncoming = true,
            callSessionId = sessionId,
            isVideoEnabled = isVideo,
            connectionQuality = if (mediaReady) callMediaProvider.displayName else "Signaling only",
            connectionState = "Incoming call",
            mediaProviderReady = mediaReady
        )
        _uiState.update {
            it.copy(
                callState = callState,
                currentScreen = if (isVideo) MessagesScreenMode.VIDEO_CALL else MessagesScreenMode.AUDIO_CALL,
                selectedThread = it.selectedThread ?: ConversationThread(
                    id = peerId,
                    peerName = peerName,
                    peerHandle = "@${peerId.take(12)}",
                    avatarInitials = initials,
                    lastMessage = "Incoming call",
                    lastTimestamp = "Now"
                )
            )
        }
        ringtoneManager?.startIncomingRingtone()
        appContext?.let { ctx ->
            CampusSystemNotificationCenter.showIncomingCallNotification(
                context = ctx,
                peerId = peerId,
                callerName = peerName,
                callerRoleOrHandle = "Campus call",
                isVideoCall = isVideo,
                callSessionId = sessionId
            )
        }
    }

    private fun acceptIncomingCall() {
        val call = _uiState.value.callState ?: return
        val sessionId = call.callSessionId ?: activeCallSessionId
        ringtoneManager?.stop()
        telecomCallManager?.setCallConnected()
        viewModelScope.launch {
            if (!sessionId.isNullOrBlank()) {
                callSignalingRepository.updateStatus(sessionId, CallSessionStatus.ACCEPTED)
                if (callMediaProvider.isConfigured) {
                    callMediaProvider.joinCall(
                        sessionId = sessionId,
                        isVideo = call.callType == CallType.VIDEO,
                        localUserId = resolveCurrentUserId()
                    ).onFailure { err ->
                        Log.w(TAG, "Media join failed (signaling still connected): ${err.message}")
                    }
                }
            }
            _uiState.update { state ->
                state.copy(
                    callState = state.callState?.copy(
                        status = CallStatus.CONNECTED,
                        isIncoming = false,
                        connectionState = if (callMediaProvider.isConfigured) {
                            "Connected • ${callMediaProvider.displayName}"
                        } else {
                            "Connected • signaling (plug CallMediaProvider for peer A/V)"
                        }
                    )
                )
            }
            startConnectedDurationTimer()
        }
    }

    private fun declineIncomingCall() {
        val sessionId = _uiState.value.callState?.callSessionId ?: activeCallSessionId
        ringtoneManager?.stop()
        viewModelScope.launch {
            if (!sessionId.isNullOrBlank()) {
                callSignalingRepository.updateStatus(sessionId, CallSessionStatus.DECLINED)
            }
        }
        stopCall(remoteNotify = false)
    }

    private fun startOutgoingSignalPoll(sessionId: String) {
        callSignalPollJob?.cancel()
        callSignalPollJob = viewModelScope.launch {
            while (isActive) {
                delay(1_500L)
                val session = callSignalingRepository.getSession(sessionId) ?: continue
                when (session.status) {
                    CallSessionStatus.ACCEPTED -> {
                        ringtoneManager?.stop()
                        telecomCallManager?.setCallConnected()
                        if (callMediaProvider.isConfigured) {
                            callMediaProvider.joinCall(
                                sessionId = sessionId,
                                isVideo = _uiState.value.callState?.callType == CallType.VIDEO,
                                localUserId = resolveCurrentUserId()
                            )
                        }
                        _uiState.update { state ->
                            state.copy(
                                callState = state.callState?.copy(
                                    status = CallStatus.CONNECTED,
                                    connectionState = if (callMediaProvider.isConfigured) {
                                        "Connected • ${callMediaProvider.displayName}"
                                    } else {
                                        "Connected • signaling (plug CallMediaProvider for peer A/V)"
                                    }
                                )
                            )
                        }
                        callSignalPollJob?.cancel()
                        return@launch
                    }
                    CallSessionStatus.DECLINED,
                    CallSessionStatus.ENDED,
                    CallSessionStatus.MISSED,
                    CallSessionStatus.FAILED -> {
                        ringtoneManager?.stop()
                        _uiState.update { state ->
                            state.copy(
                                callState = state.callState?.copy(
                                    status = CallStatus.FAILED,
                                    connectionState = "Call ${session.status.name.lowercase()}"
                                )
                            )
                        }
                        delay(1_200L)
                        stopCall(remoteNotify = false)
                        return@launch
                    }
                    CallSessionStatus.RINGING -> Unit
                }
            }
        }
    }

    private fun startIncomingInvitePoll() {
        incomingInvitePollJob?.cancel()
        incomingInvitePollJob = viewModelScope.launch {
            while (isActive) {
                delay(2_000L)
                val me = resolveCurrentUserId()
                if (me.isBlank()) continue
                if (_uiState.value.callState != null) continue
                val ringing = callSignalingRepository.fetchIncomingRinging(me)
                val invite = ringing.firstOrNull { it.id !in seenIncomingSessionIds } ?: continue
                seenIncomingSessionIds.add(invite.id)
                val callerName = resolveProfileNames(listOf(invite.callerId))[invite.callerId.lowercase()]
                    ?: continue
                presentIncomingCall(
                    sessionId = invite.id,
                    peerId = invite.callerId,
                    peerName = callerName,
                    isVideo = invite.mediaKind == CallMediaKind.VIDEO
                )
            }
        }
    }

    private fun startConnectedDurationTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
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

    private fun registerPushTokenIfPossible(userId: String) {
        val ctx = appContext ?: return
        CampusSystemNotificationCenter.setActiveUserId(ctx, userId)
        val token = CampusSystemNotificationCenter.getFcmToken(ctx) ?: return
        viewModelScope.launch {
            callSignalingRepository.registerPushToken(userId, token)
                .onFailure { Log.w(TAG, "Push token register failed: ${it.message}") }
        }
    }

    /**
     * @return true when the message was accepted by Supabase (clears the pending clock).
     */
    private suspend fun dispatchMessageToRemote(entity: ChatMessageEntity): Boolean {
        if (entity.senderId.isBlank()) return false
        val resolvedSenderName = entity.senderName.trim().takeIf { it.isNotBlank() && it.isRealProfileDisplayName() }
            ?: currentProfileName().takeIf { it.isNotBlank() && it.isRealProfileDisplayName() }
            ?: _currentProfile.value?.fullName?.trim()?.takeIf { it.isNotBlank() }
            ?: _currentProfile.value?.firstName?.trim()?.takeIf { it.isNotBlank() }
            ?: "Student"
        val row = DirectChatMessageRow(
            id = entity.id,
            channel_id = entity.channelId,
            sender_id = entity.senderId,
            sender_name = resolvedSenderName,
            text = entity.text,
            timestamp_epoch_ms = entity.timestampEpochMs,
            is_optimistic = false,
            image_url = (entity.imageUrl ?: entity.imageUri)?.takeIf { it.isNotBlank() },
            gif_url = entity.gifUrl?.takeIf { it.isNotBlank() },
            reply_to_text = entity.replyToText?.takeIf { it.isNotBlank() },
            is_delivered = true,
            is_read = false
        )
        val result = runCatching {
            supabase.from("campus_chat_messages").upsert(row)
        }
        if (result.isSuccess) {
            markLocalMessageSent(entity.channelId, entity.id)
            return true
        } else {
            Log.w(TAG, "Failed to upsert direct chat message to Supabase: ${result.exceptionOrNull()?.message}", result.exceptionOrNull())
        }
        return false
    }

    private suspend fun syncRemoteMessagesForChannel(channelId: String) {
        val list = runCatching {
            supabase.from("campus_chat_messages").select {
                filter {
                    eq("channel_id", channelId)
                }
                order("timestamp_epoch_ms", order = Order.ASCENDING)
                limit(200)
            }.decodeList<DirectChatMessageRow>()
        }.onFailure {
            Log.w(TAG, "syncRemoteMessagesForChannel fetch error: ${it.message}", it)
        }.getOrNull() ?: return
        if (list.isEmpty()) return

        val currentUserId = resolveCurrentUserId()
        val senderIds = list.map { it.sender_id }
        val profileNames = runCatching { resolveProfileNames(senderIds) }
            .getOrDefault(emptyMap())
        val entities = mutableListOf<ChatMessageEntity>()
        for (obj in list) {
            val senderId = obj.sender_id
            val isMine = currentUserId.isNotBlank() &&
                senderId.isNotBlank() &&
                senderId.equals(currentUserId, ignoreCase = true)
            entities.add(
                ChatMessageEntity(
                    id = obj.id,
                    channelId = obj.channel_id.ifBlank { channelId },
                    senderId = senderId,
                    senderName = if (isMine) "You" else profileNames[senderId.lowercase()]?.takeIf { it.isNotBlank() } ?: obj.sender_name,
                    text = obj.text,
                    timestampEpochMs = if (obj.timestamp_epoch_ms > 0L) obj.timestamp_epoch_ms else System.currentTimeMillis(),
                    isOptimistic = false,
                    imageUrl = obj.image_url?.takeIf { it.isNotBlank() },
                    gifUrl = obj.gif_url?.takeIf { it.isNotBlank() },
                    replyToText = obj.reply_to_text?.takeIf { it.isNotBlank() },
                    isMine = isMine,
                    isDelivered = obj.is_delivered,
                    isRead = obj.is_read
                )
            )
        }
        chatMessageDao?.let { dao ->
            entities.forEach { dao.insertMessage(it) }
        }
        val mapped = entities.map { e -> mapEntityToMessage(e, currentUserId) }
        threadMessagesMap[channelId] = mapped
        if (_uiState.value.selectedThread?.id == channelId) {
            _uiState.update { it.copy(messages = mapped) }
        }
    }

    fun flushPendingMessages() {
        val dao = chatMessageDao
        if (dao == null) {
            Log.e(TAG, "flushPendingMessages skipped — chatMessageDao not wired")
            return
        }
        viewModelScope.launch {
            val pending = runCatching { dao.getPendingMessages() }.getOrDefault(emptyList())
            for (msg in pending) {
                // Mandatory remote upsert; stays pending until Supabase accepts it.
                dispatchMessageToRemote(msg)
            }
        }
    }

    private fun stopCall(remoteNotify: Boolean = true) {
        val sessionId = activeCallSessionId ?: _uiState.value.callState?.callSessionId
        ringtoneManager?.stop()
        voiceTransformerEngine.stopLoopback()
        telecomCallManager?.endCall()
        callTimerJob?.cancel()
        callTimerJob = null
        callSignalPollJob?.cancel()
        callSignalPollJob = null
        viewModelScope.launch {
            if (remoteNotify && !sessionId.isNullOrBlank()) {
                callSignalingRepository.updateStatus(sessionId, CallSessionStatus.ENDED)
            }
            if (!sessionId.isNullOrBlank() && callMediaProvider.isConfigured) {
                callMediaProvider.leaveCall(sessionId)
            }
        }
        activeCallSessionId = null
        _uiState.update {
            it.copy(
                callState = null,
                currentScreen = if (it.selectedThread != null) MessagesScreenMode.CHAT else MessagesScreenMode.THREADS
            )
        }
    }

    private fun refreshDiscoverablePeers(queryOverride: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFindFriendsLoading = true) }
            val current = _currentProfile.value
            val me = resolveCurrentUserId()
            val myEmail = current?.universityEmail.orEmpty()
            val myStudentId = current?.studentId.orEmpty()
            val friends = runCatching {
                serverRepository?.syncRemoteFriends(me, myEmail).orEmpty()
            }.getOrDefault(emptyList())
            val friendByPeer = friends.associateBy { it.friendUserId.lowercase() }
            val profiles = runCatching {
                studentProfileRepository.refreshKnownProfiles().values.distinctBy {
                    it.stableUserId().ifBlank { it.universityEmail.lowercase() }
                }
            }.getOrDefault(emptyList())
            val q = (queryOverride ?: _uiState.value.findFriendsQuery).trim()
            val peers = profiles.mapNotNull { profile ->
                val userId = profile.stableUserId().ifBlank { profile.universityEmail.trim().lowercase() }
                if (userId.isBlank()) return@mapNotNull null
                if ((me.isNotBlank() && userId.equals(me, ignoreCase = true)) ||
                    (myEmail.isNotBlank() && (userId.equals(myEmail, ignoreCase = true) || profile.universityEmail.equals(myEmail, ignoreCase = true))) ||
                    (myStudentId.isNotBlank() && userId.equals(myStudentId, ignoreCase = true))
                ) {
                    return@mapNotNull null
                }
                val cleanName = cleanDisplayName(profile.fullName)
                if (!cleanName.isRealProfileDisplayName()) {
                    return@mapNotNull null
                }
                if (q.isNotBlank() &&
                    !cleanName.contains(q, ignoreCase = true) &&
                    !profile.universityEmail.contains(q, ignoreCase = true) &&
                    !profile.universityName.contains(q, ignoreCase = true)
                ) {
                    return@mapNotNull null
                }
                val rel = friendByPeer[userId.lowercase()]
                    ?: friendByPeer[profile.universityEmail.lowercase()]
                val status = when (rel?.status) {
                    FriendStatus.FRIENDS -> DiscoverFriendStatus.FRIENDS
                    FriendStatus.PENDING_SENT -> DiscoverFriendStatus.REQUEST_SENT
                    FriendStatus.PENDING_RECEIVED -> DiscoverFriendStatus.REQUEST_RECEIVED
                    else -> DiscoverFriendStatus.NONE
                }
                val avatar = profile.avatarUrl ?: profileAvatarFor(userId) ?: profileAvatarFor(cleanName)
                DiscoverableCampusPeer(
                    userId = userId,
                    fullName = cleanName,
                    handle = "@" + cleanName.lowercase().replace(" ", "")
                        .filter { it.isLetterOrDigit() || it == '_' },
                    universityName = profile.universityName,
                    avatarUrl = avatar,
                    initials = profile.initials.ifBlank { "P" },
                    friendStatus = status
                )
            }.sortedWith(
                compareBy<DiscoverableCampusPeer> {
                    when (it.friendStatus) {
                        DiscoverFriendStatus.REQUEST_RECEIVED -> 0
                        DiscoverFriendStatus.NONE -> 1
                        DiscoverFriendStatus.REQUEST_SENT -> 2
                        DiscoverFriendStatus.FRIENDS -> 3
                        DiscoverFriendStatus.SELF -> 4
                    }
                }.thenBy { it.fullName.lowercase() }
            )
            _uiState.update {
                it.copy(
                    discoverablePeers = peers,
                    isFindFriendsLoading = false
                )
            }
        }
    }

    private fun formatDmTypingLabel(displayName: String): String {
        val first = displayName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val name = first.ifBlank { displayName.ifBlank { "Someone" } }
        return "$name is typing…"
    }

    private fun publishDmTyping(isTyping: Boolean) {
        dmTypingHeartbeatJob?.cancel()
        dmTypingHeartbeatJob = null
        val thread = _uiState.value.selectedThread ?: return
        val profile = _currentProfile.value
        val userId = profile?.stableUserId().orEmpty()
        if (userId.isBlank() || thread.id.isBlank()) return
        val displayName = profile?.fullName
            ?.takeIf(String::isRealProfileDisplayName)
            ?.trim()
            .orEmpty()
        if (displayName.isBlank()) return
        val avatar = profile?.avatarUrl
        if (!isTyping) {
            viewModelScope.launch {
                dmTypingRepository.setTyping(
                    threadId = thread.id,
                    userId = userId,
                    displayName = displayName,
                    avatarUrl = avatar,
                    isTyping = false
                )
            }
            return
        }
        dmTypingHeartbeatJob = viewModelScope.launch {
            while (isActive) {
                dmTypingRepository.setTyping(
                    threadId = thread.id,
                    userId = userId,
                    displayName = displayName,
                    avatarUrl = avatar,
                    isTyping = true
                )
                delay(2_000L)
            }
        }
    }

    private fun startDmTypingPresencePoll(focusThreadId: String?) {
        dmTypingPollJob?.cancel()
        dmTypingPollJob = viewModelScope.launch {
            while (isActive) {
                val me = resolveCurrentUserId()
                val knownThreadIds = _uiState.value.threads.map { it.id }.toSet() +
                    listOfNotNull(focusThreadId, _uiState.value.selectedThread?.id)
                val remote = if (!focusThreadId.isNullOrBlank()) {
                    dmTypingRepository.fetchTypingForThread(focusThreadId).getOrNull().orEmpty()
                } else {
                    dmTypingRepository.fetchRecentTyping().getOrNull().orEmpty()
                        .filter { it.threadId in knownThreadIds }
                }
                val typingNames = runCatching {
                    resolveProfileNames(remote.map { it.userId })
                }.getOrDefault(emptyMap())
                val byThread = remote
                    .filter { !it.userId.equals(me, ignoreCase = true) }
                    .mapNotNull { typer ->
                        typingNames[typer.userId.lowercase()]
                            ?.let { name -> typer.copy(displayName = name) }
                    }
                    .groupBy { it.threadId }
                    .mapValues { (_, users) -> users.firstOrNull() }

                val updatedThreads = _uiState.value.threads.map { thread ->
                    val typer = byThread[thread.id]
                    thread.copy(
                        typingLabel = typer?.let { formatDmTypingLabel(it.displayName) }
                    )
                }
                val focusLabel = focusThreadId
                    ?.let { byThread[it] }
                    ?.let { formatDmTypingLabel(it.displayName) }
                    ?: _uiState.value.selectedThread?.id?.let { selId ->
                        byThread[selId]?.let { formatDmTypingLabel(it.displayName) }
                    }

                _uiState.update { state ->
                    val sel = state.selectedThread
                    state.copy(
                        threads = updatedThreads,
                        selectedThread = sel?.let { s ->
                            updatedThreads.find { it.id == s.id } ?: s.copy(typingLabel = focusLabel)
                        },
                        activeChatTypingLabel = if (state.currentScreen == MessagesScreenMode.CHAT) {
                            focusLabel
                        } else {
                            null
                        }
                    )
                }
                delay(1_500L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ringtoneManager?.stop()
        voiceTransformerEngine.release()
        telecomCallManager?.endCall()
        callTimerJob?.cancel()
        callSignalPollJob?.cancel()
        incomingInvitePollJob?.cancel()
        inboxPollJob?.cancel()
        dmTypingPollJob?.cancel()
        dmTypingHeartbeatJob?.cancel()
        publishDmTyping(false)
    }

    companion object {
        private const val TAG = "MessagesViewModel"
    }
}
