package com.campus.network.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.AcademicChannelsRepository
import com.campus.network.core.data.RemoteChannelChatMessage
import com.campus.network.core.data.ServerChannelChatRepository
import com.campus.network.core.data.ServerRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.AcademicChannel
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.firstName
import com.campus.network.core.model.ServerChannelType
import com.campus.network.core.model.ServerRule
import com.campus.network.core.model.stableUserId
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.feature.channels.model.CareerCourse
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.channels.model.CourseMate
import com.campus.network.feature.channels.model.FriendRequestStatus
import com.campus.network.feature.channels.model.InDemandCompany
import com.campus.network.feature.channels.model.PerServerProfile
import com.campus.network.feature.channels.model.ServerMemberItem
import com.campus.network.feature.channels.model.ServerMemberRole
import com.campus.network.feature.channels.model.ServerRoleType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ServerChannelItem(
    val id: String,
    val name: String,
    val isUnread: Boolean = false,
    val badgeText: String? = null,
    val hasBoltIcon: Boolean = false,
    val isSelected: Boolean = false,
    val channelType: String = "text", // "text", "rules", "announcements"
    val categoryName: String = "Text Channels",
    val unreadBadgeCount: Int = 0,
    /** Green subtitle when a peer is typing in this channel thread. */
    val typingLabel: String? = null
)

data class ChannelMessageEmbed(
    val provider: String = "GitHub",
    val title: String,
    val description: String,
    val subCardTitle: String? = null,
    val subCardDescription: String? = null,
    val contributorsCount: Int = 2,
    val issuesCount: Int = 0,
    val starsCount: Int = 2,
    val forksCount: Int = 1
)

data class ChannelMessage(
    val id: String,
    val authorName: String,
    val authorId: String = "",
    val authorAvatarRes: String? = null,
    val authorColorHex: Long = 0xFF1E1F22,
    val authorBadge: String? = null,
    val timestamp: String,
    val text: String,
    val linkUrl: String? = null,
    val gifUrl: String? = null,
    val imageUris: List<String> = emptyList(),
    val embed: ChannelMessageEmbed? = null,
    val replyTo: String? = null,
    val reactionCount: Int = 0,
    val isDateDivider: Boolean = false,
    val dateDividerText: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isMine: Boolean = false
)

data class ChannelTypingUser(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null
)

data class ServerItem(
    val id: String,
    val name: String,
    val memberCountText: String = "",
    val memberCount: Int = 1,
    val categoryText: String = "Community",
    val isVerified: Boolean = false,
    val badgeType: String = "verified", // "verified", "booster"
    val iconResName: String,
    val badgeCount: Int = 0,
    val hasUnread: Boolean = false,
    val bannerResName: String? = null,
    val isPreview: Boolean = false,
    val boostGoalText: String? = null,
    val hasServerGuide: Boolean = false,
    val ownerId: String = "",
    val channels: List<ServerChannelItem> = emptyList()
)

data class ChannelsUiState(
    val viewMode: ChannelViewMode = ChannelViewMode.CHANNELS_HUB,
    val servers: List<ServerItem> = emptyList(),
    val joinedServers: List<ServerItem> = emptyList(),
    val discoverableServers: List<ServerItem> = emptyList(),
    val joinedServerIds: Set<String> = emptySet(),
    val isJoiningServer: Boolean = false,
    val loadingPreviewServerName: String? = null,
    val previewServer: ServerItem? = null,
    val selectedServerId: String = "",
    val selectedServer: ServerItem? = null,
    val selectedChannelId: String = "",
    val isRailCollapsed: Boolean = false,
    val activeUserName: String = "",
    val activeUserAvatar: String? = null,
    val activeUserStatus: String = "Online",
    /** Total unread across all joined channel threads (for hub / rail indicators). */
    val activeUserUnreadCount: Int = 0,
    /**
     * Unread outside the channel currently open in chat — used for the in-chat back badge.
     * Zero while not in [ChannelViewMode.CHANNEL_CHAT].
     */
    val otherChannelsUnreadCount: Int = 0,
    val activeChatServer: ServerItem? = null,
    val activeChatChannel: ServerChannelItem? = null,
    val studentProfile: StudentProfile? = null,
    val chatMessages: List<ChannelMessage> = emptyList(),
    /** Peers currently typing in the open channel (excludes self). */
    val activeChannelTypingUsers: List<ChannelTypingUser> = emptyList(),
    /** Green header line e.g. "Lincoln is typing…" */
    val activeChannelTypingLabel: String? = null,
    val isMediaDrawerOpen: Boolean = false,
    val activeMediaTab: String = "Photos",
    val selectedFaculty: String = "Faculty of Engineering & Technology",
    val faculties: List<String> = listOf(
        "Faculty of Engineering & Technology",
        "School of Computing & Informatics",
        "Faculty of Applied Sciences"
    ),
    val selectedDepartment: String = "All",
    val departments: List<String> = emptyList(),
    val courses: List<CareerCourse> = emptyList(),
    val isCoursesExpanded: Boolean = false,
    val companies: List<InDemandCompany> = emptyList(),
    val selectedCourseForDiscovery: CareerCourse? = null,
    val selectedStudentForProfile: CourseMate? = null,
    val discoverMates: List<CourseMate> = emptyList(),
    val likedStudentIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val activeChannel: AcademicChannel? = null,
    val feedbackMessage: String? = null,
    val activeChannelProfileTab: String = "Members",
    val isServerProfileDrawerOpen: Boolean = false,
    val isAddServerSelected: Boolean = false,
    val isCreateChannelSheetOpen: Boolean = false,
    val isRulesSheetOpen: Boolean = false,
    val isServerInviteSheetOpen: Boolean = false,
    val targetChannelCategory: String = "TEXT CHANNELS",
    val currentServerRules: List<ServerRule> = emptyList(),
    val currentServerInviteCode: String = "",
    val serverMembers: Map<String, List<ServerMemberItem>> = emptyMap(),
    val perServerProfiles: Map<String, PerServerProfile> = emptyMap(),
    val selectedMemberForProfile: ServerMemberItem? = null,
    val isEditServerProfileOpen: Boolean = false,
    val isEditServerSettingsOpen: Boolean = false,
    val isServerMembersSheetOpen: Boolean = false,
    val currentMemberFilterChannelId: String? = null
)

sealed interface ChannelsEvent {
    data class SelectServer(val serverId: String) : ChannelsEvent
    data class SelectChannel(val channelId: String) : ChannelsEvent
    data object ToggleRail : ChannelsEvent
    data class SearchChannels(val query: String) : ChannelsEvent
    data object AddServerClick : ChannelsEvent
    data class CreateServer(
        val name: String,
        val category: String = "Community",
        val description: String = "",
        val avatarResName: String = "server_gradient",
        val bannerResName: String? = null,
        val onResult: ((Result<Unit>) -> Unit)? = null
    ) : ChannelsEvent
    data class JoinServerByInvite(val inviteCode: String) : ChannelsEvent
    data class PreviewCommunityServer(val serverName: String) : ChannelsEvent
    data object ExitServerPreview : ChannelsEvent
    data object JoinCurrentPreviewServer : ChannelsEvent
    data class OpenChannelChat(val channel: ServerChannelItem) : ChannelsEvent
    data object CloseChannelChat : ChannelsEvent
    data object OpenChannelProfile : ChannelsEvent
    data object CloseChannelProfile : ChannelsEvent
    data class SelectChannelProfileTab(val tabName: String) : ChannelsEvent
    data object ToggleServerProfileDrawer : ChannelsEvent
    data object OpenServerProfileDrawer : ChannelsEvent
    data object CloseServerProfileDrawer : ChannelsEvent
    data object ToggleMediaDrawer : ChannelsEvent
    data class SelectMediaTab(val tabName: String) : ChannelsEvent
    data class SendChannelMessage(
        val text: String,
        val gifUrl: String? = null,
        val imageUris: List<String> = emptyList()
    ) : ChannelsEvent
    /** Local composer typing heartbeat for channel members. */
    data class SetChannelTyping(val isTyping: Boolean) : ChannelsEvent
    /** Flush offline-queued channel messages to Supabase (mandatory remote sync). */
    data object FlushPendingChannelMessages : ChannelsEvent
    data class ToggleReaction(val messageId: String) : ChannelsEvent
    data class SelectFaculty(val facultyName: String) : ChannelsEvent
    data class SelectDepartment(val departmentName: String) : ChannelsEvent
    data object ToggleCoursesExpanded : ChannelsEvent
    data class Search(val query: String) : ChannelsEvent
    data class SelectCourse(val course: CareerCourse) : ChannelsEvent
    data class OpenDiscovery(val course: CareerCourse) : ChannelsEvent
    data class OpenStudentProfile(val student: CourseMate) : ChannelsEvent
    data object BackToPreviousMode : ChannelsEvent
    data class ToggleLikeStudent(val studentId: String) : ChannelsEvent
    data class ShowFeedback(val message: String) : ChannelsEvent
    data class ClearFeedback(val message: String? = null) : ChannelsEvent
    data object ClearSelectedChannel : ChannelsEvent
    data class OpenCreateChannelSheet(val categoryName: String = "TEXT CHANNELS") : ChannelsEvent
    data object CloseCreateChannelSheet : ChannelsEvent
    data class CreateChannel(
        val serverId: String,
        val name: String,
        val categoryName: String = "TEXT CHANNELS",
        val channelType: String = "text",
        val topic: String = ""
    ) : ChannelsEvent
    data object OpenRulesSheet : ChannelsEvent
    data object CloseRulesSheet : ChannelsEvent
    data class CreateServerRule(val serverId: String, val title: String, val description: String) : ChannelsEvent
    data class DeleteServerRule(val serverId: String, val ruleId: String) : ChannelsEvent
    data object OpenServerInviteSheet : ChannelsEvent
    data object CloseServerInviteSheet : ChannelsEvent
    data class OpenMemberProfile(val member: ServerMemberItem) : ChannelsEvent
    data object CloseMemberProfile : ChannelsEvent
    data class SendFriendRequest(val memberId: String) : ChannelsEvent
    data class CancelFriendRequest(val memberId: String) : ChannelsEvent
    data class AcceptFriendRequest(val memberId: String) : ChannelsEvent
    data class DeclineFriendRequest(val memberId: String) : ChannelsEvent
    data object OpenEditServerProfile : ChannelsEvent
    data object CloseEditServerProfile : ChannelsEvent
    data class SavePerServerProfile(
        val serverId: String,
        val nickname: String,
        val tag: String,
        val tagColorHex: Long
    ) : ChannelsEvent
    data object OpenEditServerSettings : ChannelsEvent
    data object CloseEditServerSettings : ChannelsEvent
    data class UpdateServerSettings(
        val serverId: String,
        val name: String,
        val category: String,
        val description: String,
        val avatarResName: String,
        val bannerResName: String? = null
    ) : ChannelsEvent
    data class UpdateMemberRole(
        val serverId: String,
        val memberId: String,
        val newRole: ServerRoleType
    ) : ChannelsEvent
    data class OpenServerMembersSheet(val serverId: String, val channelId: String? = null) : ChannelsEvent
    data object CloseServerMembersSheet : ChannelsEvent
    data class DeleteServer(val serverId: String) : ChannelsEvent
    data class LeaveServer(val serverId: String) : ChannelsEvent
    data class UpdateServerNotificationSetting(val serverId: String, val setting: String) : ChannelsEvent
}

class ChannelsViewModel(
    private val repository: AcademicChannelsRepository,
    private val serverRepository: ServerRepository? = null,
    private val studentProfileRepository: StudentProfileRepository = StudentProfileRepository(),
    private val channelChatRepository: ServerChannelChatRepository = ServerChannelChatRepository()
) : ViewModel() {

    constructor(
        repository: AcademicChannelsRepository,
        serverRepository: ServerRepository?,
        studentProfileRepository: StudentProfileRepository,
        serverChannelMessageDao: com.campus.network.core.database.dao.ServerChannelMessageDao
    ) : this(
        repository = repository,
        serverRepository = serverRepository,
        studentProfileRepository = studentProfileRepository,
        channelChatRepository = ServerChannelChatRepository(
            messageDao = serverChannelMessageDao
        )
    )

    private val _currentProfile = MutableStateFlow<StudentProfile?>(null)
    private var channelMessagePollJob: Job? = null
    private var channelTypingPollJob: Job? = null
    private var typingHeartbeatJob: Job? = null
    private val seenRemoteChannelMessageIds = mutableSetOf<String>()
    private val channelMessageWatermarks = mutableMapOf<String, Long>()
    private val _typingByChannelId = MutableStateFlow<Map<String, List<ChannelTypingUser>>>(emptyMap())
    private val _friendRelationships = MutableStateFlow<Map<String, com.campus.network.core.model.FriendRelationship>>(emptyMap())
    private val _previewServerId = MutableStateFlow<String?>(null)
    private val _joinedServerIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isJoiningServer = MutableStateFlow(false)
    private val _loadingPreviewServerName = MutableStateFlow<String?>(null)

    private val _viewMode = MutableStateFlow(ChannelViewMode.CHANNELS_HUB)
    private val _selectedServerId = MutableStateFlow("")
    private val _selectedChannelId = MutableStateFlow("")
    private val _isRailCollapsed = MutableStateFlow(false)
    private val _serverSearchQuery = MutableStateFlow("")
    private val _isServerProfileDrawerOpen = MutableStateFlow(false)
    private val _isAddServerSelected = MutableStateFlow(false)
    private val _activeChatChannel = MutableStateFlow<ServerChannelItem?>(null)
    private val _isMediaDrawerOpen = MutableStateFlow(false)
    private val _activeMediaTab = MutableStateFlow("Photos")
    private val _channelProfileTab = MutableStateFlow("Members")
    private val _channelMessages = MutableStateFlow<Map<String, List<ChannelMessage>>>(emptyMap())
    // typingByChannelId declared above with poll jobs
    private val _selectedFaculty = MutableStateFlow("Faculty of Engineering & Technology")
    private val _selectedDepartment = MutableStateFlow("All")
    private val _isCoursesExpanded = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedChannel = MutableStateFlow<AcademicChannel?>(null)
    private val _selectedCourseForDiscovery = MutableStateFlow<CareerCourse?>(null)
    private val _selectedStudentForProfile = MutableStateFlow<CourseMate?>(null)
    private val _likedStudentIds = MutableStateFlow<Set<String>>(emptySet())
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _isCreateChannelSheetOpen = MutableStateFlow(false)
    private val _isRulesSheetOpen = MutableStateFlow(false)
    private val _isServerInviteSheetOpen = MutableStateFlow(false)
    private val _targetChannelCategory = MutableStateFlow("TEXT CHANNELS")
    private val _currentServerRules = MutableStateFlow<List<ServerRule>>(emptyList())
    private val _currentServerInviteCode = MutableStateFlow("CODESISTENCY-2026")
    private val _serverMembers = MutableStateFlow<Map<String, List<ServerMemberItem>>>(emptyMap())
    private val _perServerProfiles = MutableStateFlow<Map<String, PerServerProfile>>(emptyMap())
    private val _selectedMemberForProfile = MutableStateFlow<ServerMemberItem?>(null)
    private val _isEditServerProfileOpen = MutableStateFlow(false)
    private val _isEditServerSettingsOpen = MutableStateFlow(false)
    private val _isServerMembersSheetOpen = MutableStateFlow(false)
    private val _currentMemberFilterChannelId = MutableStateFlow<String?>(null)

    // Master Servers & Channels Catalog (Matching patriarch Layout & Campus Community)
    private val codesistencyChannels = emptyList<ServerChannelItem>()

    private val initialMasterServersList = emptyList<ServerItem>()

    private val _serversList = MutableStateFlow<List<ServerItem>>(emptyList())

    // Master Course-Mates Catalog
    private val sampleCourseMates = emptyList<CourseMate>()

    private val allEngineeringCourses = emptyList<CareerCourse>()

    private val companiesList = emptyList<InDemandCompany>()

    init {
        _channelMessages.value = emptyMap()
        loadDefaultRules()
        seedDefaultServerMembers()
        // Push any offline-queued channel messages as soon as the VM is alive
        flushPendingChannelMessages()

        viewModelScope.launch {
            studentProfileRepository.getProfile().collect { profile ->
                _currentProfile.value = profile
                val studentId = profile.studentId.ifBlank { profile.universityEmail }
                val fullName = studentProfileRepository.knownProfiles.value[studentId]
                    ?.fullName
                    ?.takeIf(String::isRealProfileDisplayName)
                    .orEmpty()
                val avatarUrl = profile.avatarUrl
                val handle = "@" + fullName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() || it == '_' }
                if (studentId.isNotBlank() && fullName.isNotBlank()) {
                    serverRepository?.updateMemberProfileForUser(
                        userId = studentId,
                        displayName = fullName,
                        handle = handle,
                        avatarUrl = avatarUrl,
                        avatarResName = avatarUrl ?: ""
                    )
                }
            }
        }

        viewModelScope.launch {
            _currentProfile.collectLatest { profile ->
                val studentId = profile?.studentId?.ifBlank { profile.universityEmail }.orEmpty().trim()
                val email = profile?.universityEmail.orEmpty().trim().lowercase()
                val activeUserId = studentId.ifBlank { email }
                if (serverRepository != null) {
                    runCatching {
                        serverRepository.syncRemoteServers()
                        if (studentId.isNotBlank() || email.isNotBlank()) {
                            serverRepository.syncRemoteMemberships(studentId, email)
                        }
                    }
                    if (activeUserId.isNotBlank()) {
                        launch {
                            runCatching {
                                serverRepository.syncRemoteFriends(activeUserId, email)
                            }
                        }
                        launch {
                            serverRepository.getFriendsForUser(activeUserId).collect { friends ->
                                _friendRelationships.value = friends.associateBy { it.friendUserId }
                            }
                        }
                    }
                    if (studentId.isNotBlank() || email.isNotBlank()) {
                        serverRepository.getJoinedServerIds(studentId, email).collect { ids ->
                            _joinedServerIds.value = ids.toSet()
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            _friendRelationships.collect { fMap ->
                if (_serverMembers.value.isNotEmpty()) {
                    _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                        members.map { mem ->
                            if (mem.isSelf) {
                                mem.copy(friendStatus = FriendRequestStatus.FRIENDS)
                            } else {
                                val rel = fMap[mem.userId] ?: fMap[mem.handle.removePrefix("@")]
                                val status = when (rel?.status) {
                                    com.campus.network.core.model.FriendStatus.FRIENDS -> FriendRequestStatus.FRIENDS
                                    com.campus.network.core.model.FriendStatus.PENDING_SENT -> FriendRequestStatus.REQUEST_SENT
                                    com.campus.network.core.model.FriendStatus.PENDING_RECEIVED -> FriendRequestStatus.REQUEST_RECEIVED
                                    else -> FriendRequestStatus.NONE
                                }
                                mem.copy(friendStatus = status)
                            }
                        }
                    }
                    _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let { sel ->
                        val rel = fMap[sel.userId] ?: fMap[sel.handle.removePrefix("@")]
                        val status = when {
                            sel.isSelf -> FriendRequestStatus.FRIENDS
                            rel?.status == com.campus.network.core.model.FriendStatus.FRIENDS -> FriendRequestStatus.FRIENDS
                            rel?.status == com.campus.network.core.model.FriendStatus.PENDING_SENT -> FriendRequestStatus.REQUEST_SENT
                            rel?.status == com.campus.network.core.model.FriendStatus.PENDING_RECEIVED -> FriendRequestStatus.REQUEST_RECEIVED
                            else -> FriendRequestStatus.NONE
                        }
                        sel.copy(friendStatus = status)
                    }
                }
            }
        }

        if (serverRepository != null) {
            viewModelScope.launch {
                runCatching {
                    serverRepository.syncRemoteServers()
                }
            }
            viewModelScope.launch {
                serverRepository.getServers().collect { dbServers ->
                    val mapped = dbServers.map { srv ->
                        val existing = _serversList.value.find { it.id == srv.id }
                        ServerItem(
                            id = srv.id,
                            name = srv.name,
                            memberCountText = "${srv.memberCount} ${if (srv.memberCount == 1) "Member" else "Members"}",
                            memberCount = srv.memberCount,
                            categoryText = srv.category,
                            isVerified = srv.isVerified,
                            iconResName = srv.avatarResName,
                            bannerResName = srv.bannerResName,
                            ownerId = srv.ownerId,
                            channels = existing?.channels.orEmpty().ifEmpty {
                                listOf(
                                    ServerChannelItem(id = "ch_${srv.id}_rules", name = "rules", channelType = "rules", categoryName = "WELCOME"),
                                    ServerChannelItem(id = "ch_${srv.id}_announcements", name = "announcements", channelType = "announcements", categoryName = "WELCOME"),
                                    ServerChannelItem(id = "ch_${srv.id}_general", name = "general", channelType = "text", categoryName = "TEXT CHANNELS")
                                )
                            }
                        )
                    }
                    _serversList.value = mapped
                    val profileId = _currentProfile.value?.studentId?.ifBlank { _currentProfile.value?.universityEmail } ?: ""
                    val userEmail = _currentProfile.value?.universityEmail.orEmpty().trim().lowercase()
                    val joinedOnes = mapped.filter {
                        _joinedServerIds.value.contains(it.id) ||
                        (profileId.isNotBlank() && it.ownerId.equals(profileId, ignoreCase = true)) ||
                        (userEmail.isNotBlank() && it.ownerId.equals(userEmail, ignoreCase = true))
                    }
                    if (_selectedServerId.value.isEmpty() && joinedOnes.isNotEmpty()) {
                        _selectedServerId.value = joinedOnes.first().id
                        _selectedChannelId.value = joinedOnes.first().channels.firstOrNull()?.id ?: ""
                    }
                }
            }
            viewModelScope.launch {
                combine(_selectedServerId, _previewServerId) { sel, prev -> prev ?: sel }
                    .collectLatest { serverId ->
                        if (serverId.isNotEmpty()) {
                            runCatching {
                                serverRepository.syncRemoteChannels(serverId)
                            }
                            serverRepository.getChannelsForServer(serverId).collect { dbChannels ->
                                if (dbChannels.isNotEmpty()) {
                                    val mappedChannels = dbChannels.map { ch ->
                                        ServerChannelItem(
                                            id = ch.id,
                                            name = ch.name,
                                            channelType = when (ch.channelType) {
                                                ServerChannelType.RULES -> "rules"
                                                ServerChannelType.ANNOUNCEMENT -> "announcements"
                                                ServerChannelType.VOICE -> "voice"
                                                else -> "text"
                                            },
                                            categoryName = ch.categoryName,
                                            isUnread = ch.unreadCount > 0,
                                            unreadBadgeCount = ch.unreadCount
                                        )
                                    }.distinctBy { it.id }

                                    _serversList.value = _serversList.value.map { srv ->
                                        if (srv.id == serverId) {
                                            val openId = _activeChatChannel.value?.id
                                            // Don't resurrect unread for the channel the student is viewing
                                            val channels = mappedChannels.map { ch ->
                                                if (ch.id == openId) {
                                                    ch.copy(isUnread = false, unreadBadgeCount = 0)
                                                } else ch
                                            }
                                            val unreadSum = channels.sumOf { it.unreadBadgeCount }
                                            srv.copy(
                                                channels = channels,
                                                badgeCount = unreadSum,
                                                hasUnread = unreadSum > 0 || channels.any { it.isUnread }
                                            )
                                        } else srv
                                    }
                                }
                            }
                        }
                    }
            }
            viewModelScope.launch {
                combine(_selectedServerId, _previewServerId) { sel, prev -> prev ?: sel }
                    .collectLatest { serverId ->
                        if (serverId.isNotEmpty()) {
                            runCatching {
                                serverRepository.syncRemoteRules(serverId)
                            }
                            serverRepository.getRulesForServer(serverId).collect { rules ->
                                if (rules.isNotEmpty()) {
                                    _currentServerRules.value = rules
                                }
                            }
                        }
                    }
            }
            viewModelScope.launch {
                combine(_selectedServerId, _previewServerId) { sel, prev -> prev ?: sel }
                    .collectLatest { serverId ->
                        if (serverId.isNotEmpty()) {
                            runCatching {
                                serverRepository.syncRemoteMembers(serverId)
                            }
                            serverRepository.getMembersForServer(serverId).collect { dbMembers ->
                                run {
                                    val currentAvatar = _currentProfile.value?.avatarUrl
                                    val currentStudentId = _currentProfile.value?.studentId.orEmpty().trim()
                                    val currentEmail = _currentProfile.value?.universityEmail.orEmpty().trim().lowercase()
                                    val known = studentProfileRepository.knownProfiles.value
                                    val mapped = dbMembers.map { mem ->
                                        val cleanUserId = mem.userId.trim()
                                        val isSelf = (currentStudentId.isNotBlank() && cleanUserId.equals(currentStudentId, ignoreCase = true)) ||
                                                (currentEmail.isNotBlank() && cleanUserId.equals(currentEmail, ignoreCase = true))
                                        val matchedProfile = known[cleanUserId] ?: (if (cleanUserId.contains("@")) known[cleanUserId.lowercase()] else null)
                                        val resolvedName = matchedProfile?.fullName
                                            ?.takeIf(String::isRealProfileDisplayName)
                                            .orEmpty()
                                        val resolvedHandle = resolvedName
                                            .takeIf(String::isNotBlank)
                                            ?.let { "@" + it.lowercase().replace(" ", "").filter { char -> char.isLetterOrDigit() || char == '_' } }
                                            .orEmpty()

                                        val rawAvatar = if (isSelf && !currentAvatar.isNullOrBlank()) {
                                            currentAvatar
                                        } else if (matchedProfile?.avatarUrl != null) {
                                            matchedProfile.avatarUrl
                                        } else {
                                            mem.avatarUrl ?: mem.avatarResName
                                        }
                                        val isUrl = rawAvatar?.startsWith("http") == true || rawAvatar?.startsWith("content:") == true
                                        val fMap = _friendRelationships.value
                                        val rel = fMap[mem.userId] ?: fMap[resolvedHandle.removePrefix("@")]
                                        val resolvedFriendStatus = when {
                                            isSelf -> FriendRequestStatus.FRIENDS
                                            rel?.status == com.campus.network.core.model.FriendStatus.FRIENDS -> FriendRequestStatus.FRIENDS
                                            rel?.status == com.campus.network.core.model.FriendStatus.PENDING_SENT -> FriendRequestStatus.REQUEST_SENT
                                            rel?.status == com.campus.network.core.model.FriendStatus.PENDING_RECEIVED -> FriendRequestStatus.REQUEST_RECEIVED
                                            else -> FriendRequestStatus.NONE
                                        }

                                        ServerMemberItem(
                                            id = "mem_${mem.userId}_$serverId",
                                            serverId = serverId,
                                            userId = mem.userId,
                                            name = resolvedName,
                                            handle = resolvedHandle,
                                            avatarResName = if (isUrl) null else rawAvatar,
                                            avatarUrl = if (isUrl) rawAvatar else null,
                                            serverNickname = resolvedName,
                                            serverTag = if (mem.role == com.campus.network.core.model.ServerMemberRole.OWNER) "OWNER" else "MEMBER",
                                            serverTagColorHex = mem.roleColorHex,
                                            role = ServerMemberRole(
                                                name = mem.roleTitle,
                                                roleType = when (mem.role) {
                                                    com.campus.network.core.model.ServerMemberRole.OWNER -> ServerRoleType.OWNER
                                                    com.campus.network.core.model.ServerMemberRole.ADMIN -> ServerRoleType.ADMIN
                                                    com.campus.network.core.model.ServerMemberRole.MODERATOR -> ServerRoleType.MODERATOR
                                                    else -> ServerRoleType.MEMBER
                                                },
                                                colorHex = mem.roleColorHex,
                                                isOwner = mem.role == com.campus.network.core.model.ServerMemberRole.OWNER
                                            ),
                                            isOnline = mem.isOnline,
                                            joinedDate = "Joined " + java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(mem.joinedAtEpochMs)),
                                            bio = mem.customStatus ?: "Campus peer & server member.",
                                            friendStatus = resolvedFriendStatus,
                                            isSelf = isSelf
                                        )
                                    }
                                    _serverMembers.value = _serverMembers.value + (serverId to mapped)
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun loadDefaultRules() {
        _currentServerRules.value = emptyList()
    }

    private fun seedDefaultServerMembers() {
        _perServerProfiles.value = emptyMap()
        _serverMembers.value = emptyMap()
    }

    val uiState: StateFlow<ChannelsUiState> = combine(
        combine(
            combine(_viewMode, _selectedServerId, _selectedChannelId) { vm, sId, cId -> Triple(vm, sId, cId) },
            combine(_isRailCollapsed, _serverSearchQuery) { rc, sq -> Pair(rc, sq) },
            _isServerProfileDrawerOpen,
            _isAddServerSelected,
            combine(_currentProfile, _previewServerId, _joinedServerIds, _isJoiningServer, _loadingPreviewServerName) { p, prev, joined, isJoining, loadingPrev ->
                ServerJoinSlice(p, prev, joined, isJoining, loadingPrev)
            }
        ) { t, railAndQuery, isProfileDrawerOpen, isAddServerSelected, joinSlice ->
            ServerStateSlice1(
                viewMode = t.first,
                selectedServerId = t.second,
                selectedChannelId = t.third,
                isRailCollapsed = railAndQuery.first,
                serverSearchQuery = railAndQuery.second,
                isServerProfileDrawerOpen = isProfileDrawerOpen,
                isAddServerSelected = isAddServerSelected,
                studentProfile = joinSlice.studentProfile,
                previewServerId = joinSlice.previewServerId,
                joinedServerIds = joinSlice.joinedServerIds,
                isJoiningServer = joinSlice.isJoiningServer,
                loadingPreviewServerName = joinSlice.loadingPreviewServerName
            )
        },
        combine(
            combine(
                _activeChatChannel,
                _isMediaDrawerOpen,
                _activeMediaTab,
                _channelMessages,
                _channelProfileTab
            ) { cChan, drawerOpen, mTab, cMsgs, cpTab ->
                Quintuple(cChan, drawerOpen, mTab, cMsgs, cpTab)
            },
            _typingByChannelId
        ) { quint, typingMap ->
            ServerStateSlice2(
                activeChatChannel = quint.first,
                isMediaDrawerOpen = quint.second,
                activeMediaTab = quint.third,
                channelMessages = quint.fourth,
                channelProfileTab = quint.fifth,
                typingByChannelId = typingMap
            )
        },
        combine(
            _selectedFaculty,
            _selectedDepartment,
            _isCoursesExpanded,
            _searchQuery
        ) { fac, dept, exp, q ->
            AcademicFilterSlice(fac, dept, exp, q)
        },
        combine(
            combine(
                _selectedCourseForDiscovery,
                _selectedStudentForProfile,
                _likedStudentIds,
                _feedbackMessage
            ) { cDisc, sProf, liked, fb ->
                PeerSlice(cDisc, sProf, liked, fb)
            },
            combine(
                _serverMembers,
                _perServerProfiles,
                _selectedMemberForProfile,
                _isEditServerProfileOpen,
                _isServerMembersSheetOpen
            ) { members, profiles, selMember, editOpen, membersSheetOpen ->
                ServerMemberSlice(members, profiles, selMember, editOpen, membersSheetOpen, _currentMemberFilterChannelId.value)
            }
        ) { peerSlice, memberSlice ->
            Pair(peerSlice, memberSlice)
        },
        combine(
            combine(
                _isCreateChannelSheetOpen,
                _isRulesSheetOpen,
                _isServerInviteSheetOpen,
                _isEditServerSettingsOpen,
                _targetChannelCategory
            ) { chOpen, rulesOpen, inviteOpen, settingsOpen, cat ->
                ServerManagementFlags(chOpen, rulesOpen, inviteOpen, settingsOpen, cat)
            },
            _currentServerRules,
            _currentServerInviteCode,
            _serversList
        ) { flags, rules, inviteCode, currentServers ->
            Pair(
                ServerManagementSlice(
                    flags.isCreateChannelSheetOpen,
                    flags.isRulesSheetOpen,
                    flags.isServerInviteSheetOpen,
                    flags.isEditServerSettingsOpen,
                    flags.targetChannelCategory,
                    rules,
                    inviteCode
                ),
                currentServers
            )
        }
    ) { slice1, slice2, academicSlice, peerAndMember, mgmtAndServers ->
        val peerSlice = peerAndMember.first
        val memberSlice = peerAndMember.second
        val mgmtSlice = mgmtAndServers.first
        val currentServers = mgmtAndServers.second
        val rawCourses = allEngineeringCourses
        val departments = listOf("All") + rawCourses.map { it.department }.distinct()

        val filteredCourses = rawCourses.filter { course ->
            val matchesDept = academicSlice.department == "All" || course.department == academicSlice.department
            val matchesQuery = academicSlice.query.isBlank() ||
                    course.title.contains(academicSlice.query, ignoreCase = true) ||
                    course.courseCode.contains(academicSlice.query, ignoreCase = true) ||
                    course.department.contains(academicSlice.query, ignoreCase = true)
            matchesDept && matchesQuery
        }

        val displayCourses = if (academicSlice.isExpanded || filteredCourses.size <= 3) {
            filteredCourses
        } else {
            filteredCourses.take(3)
        }

        val discoverMates = peerSlice.selectedCourse?.enrolledMates ?: sampleCourseMates

        val allServers = currentServers
        val profile = slice1.studentProfile?.copy(fullName = currentSupabaseProfileName().orEmpty())
        val currentUserId = profile?.studentId?.ifBlank { profile.universityEmail }.orEmpty().trim()
        val currentEmail = profile?.universityEmail.orEmpty().trim().lowercase()

        val typingMap = slice2.typingByChannelId
        val myStableId = profile?.stableUserId().orEmpty()
        val joinedServersList = allServers.filter { srv ->
            slice1.joinedServerIds.contains(srv.id) ||
            (currentUserId.isNotBlank() && srv.ownerId.equals(currentUserId, ignoreCase = true)) ||
            (currentEmail.isNotBlank() && srv.ownerId.equals(currentEmail, ignoreCase = true))
        }.map { srv ->
            val channelsWithTyping = srv.channels.map { ch ->
                val typers = typingMap[ch.id].orEmpty()
                    .filter { !it.userId.equals(myStableId, ignoreCase = true) }
                ch.copy(typingLabel = formatTypingLabel(typers))
            }
            val unreadSum = channelsWithTyping.sumOf {
                it.unreadBadgeCount.coerceAtLeast(if (it.isUnread) 1 else 0)
            }
            srv.copy(
                isPreview = false,
                channels = channelsWithTyping,
                badgeCount = unreadSum,
                hasUnread = unreadSum > 0
            )
        }

        val previewServerItem = slice1.previewServerId?.let { pId ->
            allServers.find { it.id == pId }?.copy(isPreview = true)
        }

        val currentServer = if (previewServerItem != null) {
            previewServerItem
        } else {
            joinedServersList.find { it.id == slice1.selectedServerId }
                ?: joinedServersList.firstOrNull()
        }

        val filteredChannels = currentServer?.channels?.filter { channel ->
            slice1.serverSearchQuery.isBlank() ||
                    channel.name.contains(slice1.serverSearchQuery, ignoreCase = true)
        }?.map { channel ->
            channel.copy(isSelected = (channel.id == slice1.selectedChannelId))
        } ?: emptyList()

        val enrichedServer = currentServer?.copy(channels = filteredChannels)

        // Resolve active chat channel and its messages
        val activeChannel = slice2.activeChatChannel ?: currentServer?.channels?.find { it.id == slice1.selectedChannelId }
        val currentMessages = activeChannel?.let { slice2.channelMessages[it.id] } ?: emptyList()
        val activeTypers = activeChannel?.id?.let { chId ->
            typingMap[chId].orEmpty().filter { !it.userId.equals(myStableId, ignoreCase = true) }
        }.orEmpty()

        val resolvedName = profile?.fullName?.takeIf(String::isRealProfileDisplayName).orEmpty()
        val resolvedAvatar = profile?.avatarUrl

        val allJoinedIds = slice1.joinedServerIds + joinedServersList.map { it.id }.toSet()

        ChannelsUiState(
            viewMode = slice1.viewMode,
            servers = joinedServersList,
            joinedServers = joinedServersList,
            discoverableServers = allServers,
            joinedServerIds = allJoinedIds,
            isJoiningServer = slice1.isJoiningServer,
            loadingPreviewServerName = slice1.loadingPreviewServerName,
            previewServer = previewServerItem,
            selectedServerId = currentServer?.id.orEmpty(),
            selectedServer = enrichedServer,
            selectedChannelId = slice1.selectedChannelId,
            isRailCollapsed = slice1.isRailCollapsed,
            activeUserName = resolvedName,
            activeUserAvatar = resolvedAvatar,
            studentProfile = profile,
            activeUserStatus = "Online",
            activeUserUnreadCount = joinedServersList.sumOf { srv ->
                srv.channels.sumOf { ch -> ch.unreadBadgeCount.coerceAtLeast(if (ch.isUnread) 1 else 0) }
            },
            otherChannelsUnreadCount = if (slice1.viewMode == ChannelViewMode.CHANNEL_CHAT) {
                val openId = activeChannel?.id
                joinedServersList.sumOf { srv ->
                    srv.channels
                        .filter { it.id != openId }
                        .sumOf { ch -> ch.unreadBadgeCount.coerceAtLeast(if (ch.isUnread) 1 else 0) }
                }
            } else {
                0
            },
            activeChatServer = currentServer,
            activeChatChannel = activeChannel,
            chatMessages = currentMessages,
            activeChannelTypingUsers = activeTypers,
            activeChannelTypingLabel = formatTypingLabel(activeTypers),
            isMediaDrawerOpen = slice2.isMediaDrawerOpen,
            activeMediaTab = slice2.activeMediaTab,
            activeChannelProfileTab = slice2.channelProfileTab,
            isServerProfileDrawerOpen = slice1.isServerProfileDrawerOpen,
            isAddServerSelected = slice1.isAddServerSelected,
            isCreateChannelSheetOpen = mgmtSlice.isCreateChannelSheetOpen,
            isRulesSheetOpen = mgmtSlice.isRulesSheetOpen,
            isServerInviteSheetOpen = mgmtSlice.isServerInviteSheetOpen,
            isEditServerSettingsOpen = mgmtSlice.isEditServerSettingsOpen,
            targetChannelCategory = mgmtSlice.targetChannelCategory,
            currentServerRules = mgmtSlice.currentServerRules,
            currentServerInviteCode = mgmtSlice.currentServerInviteCode,
            serverMembers = memberSlice.serverMembers,
            perServerProfiles = memberSlice.perServerProfiles,
            selectedMemberForProfile = memberSlice.selectedMember,
            isEditServerProfileOpen = memberSlice.isEditServerProfileOpen,
            isServerMembersSheetOpen = memberSlice.isServerMembersSheetOpen,
            currentMemberFilterChannelId = memberSlice.filterChannelId,
            selectedFaculty = academicSlice.faculty,
            selectedDepartment = academicSlice.department,
            departments = departments,
            courses = displayCourses,
            isCoursesExpanded = academicSlice.isExpanded,
            companies = companiesList,
            selectedCourseForDiscovery = peerSlice.selectedCourse,
            selectedStudentForProfile = peerSlice.selectedStudent,
            discoverMates = discoverMates,
            likedStudentIds = peerSlice.likedIds,
            searchQuery = academicSlice.query,
            feedbackMessage = peerSlice.feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChannelsUiState(
            servers = initialMasterServersList,
            selectedServer = initialMasterServersList.firstOrNull()
        )
    )

    fun clearSession() {
        _joinedServerIds.value = emptySet()
        _selectedServerId.value = ""
        _selectedChannelId.value = ""
        _previewServerId.value = null
        _serverMembers.value = emptyMap()
        _currentServerRules.value = emptyList()
        _activeChatChannel.value = null
        _viewMode.value = ChannelViewMode.CHANNELS_HUB
    }

    fun refreshServers() {
        val prof = _currentProfile.value ?: studentProfileRepository.studentProfile.value
        val studentId = prof?.studentId?.ifBlank { prof.universityEmail }.orEmpty().trim()
        val email = prof?.universityEmail.orEmpty().trim().lowercase()
        viewModelScope.launch {
            serverRepository?.let { repo ->
                runCatching {
                    repo.syncRemoteServers()
                    if (studentId.isNotBlank() || email.isNotBlank()) {
                        repo.syncRemoteMemberships(studentId, email)
                    }
                    val currentId = _previewServerId.value ?: _selectedServerId.value
                    if (currentId.isNotBlank()) {
                        repo.syncRemoteMembers(currentId)
                    }
                }
            }
        }
    }

    fun onEvent(event: ChannelsEvent) {
        when (event) {
            is ChannelsEvent.SelectServer -> {
                _previewServerId.value = null
                _isAddServerSelected.value = false
                _selectedServerId.value = event.serverId
                val server = _serversList.value.find { it.id == event.serverId }
                    ?: initialMasterServersList.find { it.id == event.serverId }
                server?.channels?.firstOrNull()?.let {
                    _selectedChannelId.value = it.id
                }
                // Hub-level typing poll so channel thread list shows "X is typing…"
                if (_viewMode.value != ChannelViewMode.CHANNEL_CHAT) {
                    startTypingPresencePoll(serverId = event.serverId, focusChannelId = null)
                }
            }
            is ChannelsEvent.SelectChannel -> {
                _selectedChannelId.value = event.channelId
            }
            is ChannelsEvent.OpenChannelChat -> {
                if (_previewServerId.value != null) {
                    _feedbackMessage.value = "Join this server to chat in #${event.channel.name}!"
                    return
                }
                _activeChatChannel.value = event.channel
                _selectedChannelId.value = event.channel.id
                _isMediaDrawerOpen.value = false
                _viewMode.value = ChannelViewMode.CHANNEL_CHAT
                // Clear unread for the channel the student just opened
                _serversList.value = _serversList.value.map { server ->
                    server.copy(
                        channels = server.channels.map { ch ->
                            if (ch.id == event.channel.id) {
                                ch.copy(isUnread = false, unreadBadgeCount = 0)
                            } else ch
                        },
                        badgeCount = server.channels
                            .filter { it.id != event.channel.id }
                            .sumOf { it.unreadBadgeCount.coerceAtLeast(if (it.isUnread) 1 else 0) },
                        hasUnread = server.channels.any { it.id != event.channel.id && (it.isUnread || it.unreadBadgeCount > 0) }
                    )
                }
                viewModelScope.launch {
                    serverRepository?.markChannelRead(event.channel.id)
                    // Hydrate from Room first so history survives logout / process death
                    hydrateChannelMessagesFromLocal(event.channel.id)
                }
                startChannelMessageStream(event.channel.id)
                startTypingPresencePoll(serverId = _selectedServerId.value, focusChannelId = event.channel.id)
                flushPendingChannelMessages()
            }
            is ChannelsEvent.CloseChannelChat -> {
                publishTyping(false)
                stopChannelMessageStream()
                _viewMode.value = ChannelViewMode.CHANNELS_HUB
                _isMediaDrawerOpen.value = false
                // Keep light typing poll for channel thread list indicators
                startTypingPresencePoll(serverId = _selectedServerId.value, focusChannelId = null)
            }
            is ChannelsEvent.OpenChannelProfile -> {
                _viewMode.value = ChannelViewMode.CHANNEL_PROFILE
            }
            is ChannelsEvent.CloseChannelProfile -> {
                _viewMode.value = ChannelViewMode.CHANNEL_CHAT
            }
            is ChannelsEvent.SelectChannelProfileTab -> {
                _channelProfileTab.value = event.tabName
            }
            is ChannelsEvent.ToggleServerProfileDrawer -> {
                _isServerProfileDrawerOpen.value = !_isServerProfileDrawerOpen.value
            }
            is ChannelsEvent.OpenServerProfileDrawer -> {
                _isServerProfileDrawerOpen.value = true
                val srvId = _previewServerId.value ?: _selectedServerId.value
                if (srvId.isNotBlank()) {
                    viewModelScope.launch {
                        serverRepository?.syncRemoteMembers(srvId)
                    }
                }
            }
            is ChannelsEvent.CloseServerProfileDrawer -> {
                _isServerProfileDrawerOpen.value = false
            }
            is ChannelsEvent.ToggleMediaDrawer -> {
                _isMediaDrawerOpen.value = !_isMediaDrawerOpen.value
            }
            is ChannelsEvent.SelectMediaTab -> {
                _activeMediaTab.value = event.tabName
            }
            is ChannelsEvent.SendChannelMessage -> {
                val activeChan = _activeChatChannel.value
                    ?: _serversList.value.find { it.id == _selectedServerId.value }?.channels?.find { it.id == _selectedChannelId.value }
                    ?: return
                val profile = _currentProfile.value
                val authorId = profile?.stableUserId().orEmpty()
                if (authorId.isBlank()) {
                    _feedbackMessage.value = "Sign in to send channel messages"
                    return
                }
                val author = currentSupabaseProfileName().orEmpty()
                if (author.isBlank()) {
                    _feedbackMessage.value = "A Supabase profile name is required to send channel messages"
                    return
                }
                val avatar = profile?.avatarUrl
                val now = System.currentTimeMillis()
                val msgId = "cmsg_${java.util.UUID.randomUUID()}"
                val newMessage = ChannelMessage(
                    id = msgId,
                    authorName = author,
                    authorId = authorId,
                    authorColorHex = 0xFF003B68,
                    authorAvatarRes = avatar,
                    timestamp = "Today at " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(now)),
                    text = event.text,
                    gifUrl = event.gifUrl,
                    imageUris = event.imageUris,
                    timestampEpochMs = now,
                    isMine = true
                )
                val currentList = _channelMessages.value[activeChan.id] ?: emptyList()
                _channelMessages.value = _channelMessages.value + (activeChan.id to (currentList + newMessage))
                seenRemoteChannelMessageIds.add(msgId)
                channelMessageWatermarks[activeChan.id] =
                    maxOf(channelMessageWatermarks[activeChan.id] ?: 0L, now)
                // Stop local typing once a message is sent
                publishTyping(false)
                val serverId = _selectedServerId.value
                viewModelScope.launch {
                    // Always persist locally first (offline cache). Remote sync is mandatory when online.
                    channelChatRepository.upsertLocalPending(
                        serverId = serverId,
                        channelId = activeChan.id,
                        senderId = authorId,
                        senderName = author,
                        senderAvatarUrl = avatar,
                        text = event.text,
                        gifUrl = event.gifUrl,
                        imageUrls = event.imageUris,
                        messageId = msgId,
                        timestampEpochMs = now,
                        isMine = true
                    )
                    val remote = channelChatRepository.sendMessage(
                        serverId = serverId,
                        channelId = activeChan.id,
                        senderId = authorId,
                        senderAvatarUrl = avatar,
                        text = event.text,
                        gifUrl = event.gifUrl,
                        imageUrls = event.imageUris,
                        messageId = msgId,
                        timestampEpochMs = now
                    )
                    if (remote.isSuccess) {
                        channelChatRepository.markDelivered(msgId)
                    } else {
                        _feedbackMessage.value =
                            "Saved offline — will sync when you're back online"
                    }
                }
            }
            is ChannelsEvent.SetChannelTyping -> {
                publishTyping(event.isTyping)
            }
            is ChannelsEvent.FlushPendingChannelMessages -> {
                flushPendingChannelMessages()
            }
            is ChannelsEvent.ToggleReaction -> {
                val activeChan = _activeChatChannel.value ?: return
                val currentList = _channelMessages.value[activeChan.id] ?: emptyList()
                val updated = currentList.map { msg ->
                    if (msg.id == event.messageId) {
                        msg.copy(reactionCount = if (msg.reactionCount > 0) msg.reactionCount - 1 else msg.reactionCount + 1)
                    } else msg
                }
                _channelMessages.value = _channelMessages.value + (activeChan.id to updated)
            }
            is ChannelsEvent.ToggleRail -> {
                _isRailCollapsed.value = !_isRailCollapsed.value
            }
            is ChannelsEvent.SearchChannels -> {
                _serverSearchQuery.value = event.query
            }
            is ChannelsEvent.AddServerClick -> {
                _previewServerId.value = null
                _isAddServerSelected.value = true
                _feedbackMessage.value = null
            }
            is ChannelsEvent.CreateServer -> {
                _isAddServerSelected.value = false
                val newId = "srv_custom_${System.currentTimeMillis()}"
                val cleanAvatar = if (event.avatarResName.isNotBlank()) event.avatarResName else "server_gradient"
                val defaultChannels = listOf(
                    ServerChannelItem(id = "ch_${newId}_rules", name = "rules", channelType = "rules", categoryName = "WELCOME", isUnread = false),
                    ServerChannelItem(id = "ch_${newId}_announcements", name = "announcements", channelType = "announcements", categoryName = "WELCOME", isUnread = false),
                    ServerChannelItem(id = "ch_${newId}_general", name = "general", channelType = "text", categoryName = "TEXT CHANNELS", isUnread = false)
                )
                val prof = _currentProfile.value ?: studentProfileRepository.studentProfile.value
                val author = currentSupabaseProfileName().orEmpty()
                val authorId = prof?.stableUserId().orEmpty()
                if (author.isBlank() || authorId.isBlank()) {
                    _feedbackMessage.value = "A Supabase profile is required to create a server"
                    return
                }
                val authorAvatar = prof?.avatarUrl
                val authorHandle = "@" + author.lowercase().replace(" ", "").filter { it.isLetterOrDigit() || it == '_' }
                val newServer = ServerItem(
                    id = newId,
                    name = event.name,
                    memberCountText = "1 Member",
                    memberCount = 1,
                    ownerId = authorId,
                    categoryText = event.category,
                    isVerified = false,
                    iconResName = cleanAvatar,
                    bannerResName = event.bannerResName,
                    badgeCount = 0,
                    hasUnread = false,
                    channels = defaultChannels
                )
                val newMember = ServerMemberItem(
                    id = "mem_${authorId}_$newId",
                    serverId = newId,
                    name = author,
                    handle = authorHandle,
                    avatarResName = if (authorAvatar?.startsWith("http") == true || authorAvatar?.startsWith("content:") == true) null else authorAvatar,
                    avatarUrl = if (authorAvatar?.startsWith("http") == true || authorAvatar?.startsWith("content:") == true) authorAvatar else null,
                    serverNickname = author,
                    serverTag = "OWNER",
                    serverTagColorHex = 0xFFEAB308,
                    role = ServerMemberRole("Server Owner", ServerRoleType.OWNER, 0xFFEAB308, isOwner = true),
                    isOnline = true,
                    joinedDate = "Joined Just Now",
                    bio = "Server Founder & Owner.",
                    friendStatus = FriendRequestStatus.NONE,
                    isSelf = true
                )

                _feedbackMessage.value = "Creating server ${event.name} on cloud..."

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val result = repo.createServer(
                            name = event.name,
                            category = event.category,
                            description = event.description,
                            avatarResName = cleanAvatar,
                            bannerResName = event.bannerResName,
                            customServerId = newId,
                            ownerId = authorId,
                            ownerAvatarUrl = authorAvatar
                        )
                        result.onSuccess {
                            _serversList.value = (_serversList.value + newServer).distinctBy { it.id }
                            _joinedServerIds.value = _joinedServerIds.value + newId
                            _selectedServerId.value = newId
                            _selectedChannelId.value = "ch_${newId}_general"
                            _previewServerId.value = null
                            _serverMembers.value = _serverMembers.value + (newId to listOf(newMember))
                            _perServerProfiles.value = _perServerProfiles.value + (newId to PerServerProfile(newId, author, "OWNER", 0xFFEAB308, "Server Owner"))
                            _feedbackMessage.value = "Created server ${event.name}!"
                            event.onResult?.invoke(Result.success(Unit))
                        }.onFailure { err ->
                            _feedbackMessage.value = "Failed to create server on cloud: ${err.localizedMessage ?: "Unknown error"}"
                            event.onResult?.invoke(Result.failure(err))
                        }
                    }
                }
            }
            is ChannelsEvent.OpenCreateChannelSheet -> {
                if (_previewServerId.value != null) {
                    _feedbackMessage.value = "Join this server to create channels!"
                    return
                }
                _targetChannelCategory.value = event.categoryName
                _isCreateChannelSheetOpen.value = true
            }
            is ChannelsEvent.CloseCreateChannelSheet -> {
                _isCreateChannelSheetOpen.value = false
            }
            is ChannelsEvent.CreateChannel -> {
                _isCreateChannelSheetOpen.value = false
                val cleanSlug = event.name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-").ifEmpty { "channel" }
                _feedbackMessage.value = "Creating #${cleanSlug} on cloud..."

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val cType = when (event.channelType.lowercase()) {
                            "announcements" -> ServerChannelType.ANNOUNCEMENT
                            "rules" -> ServerChannelType.RULES
                            "voice" -> ServerChannelType.VOICE
                            else -> ServerChannelType.TEXT
                        }
                        val result = repo.createChannel(
                            serverId = event.serverId,
                            name = event.name,
                            categoryName = event.categoryName,
                            channelType = cType,
                            topic = event.topic
                        )
                        result.onSuccess { createdChannel ->
                            val channelItem = ServerChannelItem(
                                id = createdChannel.id,
                                name = createdChannel.name,
                                channelType = createdChannel.channelType.name.lowercase(),
                                categoryName = createdChannel.categoryName,
                                isUnread = false
                            )
                            _serversList.value = _serversList.value.map { server ->
                                if (server.id == event.serverId) {
                                    server.copy(channels = (server.channels + channelItem).distinctBy { it.id })
                                } else server
                            }
                            _selectedChannelId.value = createdChannel.id
                            _feedbackMessage.value = "Created #${createdChannel.name}"
                        }.onFailure { err ->
                            _feedbackMessage.value = "Failed to create channel: ${err.localizedMessage ?: "Network error"}"
                        }
                    }
                }
            }
            is ChannelsEvent.OpenRulesSheet -> {
                _isRulesSheetOpen.value = true
            }
            is ChannelsEvent.CloseRulesSheet -> {
                _isRulesSheetOpen.value = false
            }
            is ChannelsEvent.CreateServerRule -> {
                val nextIdx = (_currentServerRules.value.maxOfOrNull { it.ruleIndex } ?: 0) + 1
                val newRule = ServerRule(
                    id = "rule_${event.serverId}_$nextIdx",
                    serverId = event.serverId,
                    ruleIndex = nextIdx,
                    title = event.title,
                    description = event.description
                )
                _currentServerRules.value = _currentServerRules.value + newRule
                _feedbackMessage.value = "Added rule: ${event.title}"

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val result = repo.createRule(event.serverId, event.title, event.description)
                        result.onFailure { err ->
                            _feedbackMessage.value = "Failed to sync rule to cloud: ${err.localizedMessage ?: "Network error"}"
                        }
                    }
                }
            }
            is ChannelsEvent.DeleteServerRule -> {
                _currentServerRules.value = _currentServerRules.value.filter { it.id != event.ruleId }
                _feedbackMessage.value = "Rule removed"

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val result = repo.deleteRule(event.ruleId)
                        result.onFailure { err ->
                            _feedbackMessage.value = "Failed to delete rule remotely: ${err.localizedMessage ?: "Network error"}"
                        }
                    }
                }
            }
            is ChannelsEvent.OpenServerInviteSheet -> {
                val currentServer = _serversList.value.find { it.id == _selectedServerId.value }
                if (currentServer != null && _currentServerInviteCode.value.isEmpty()) {
                    _currentServerInviteCode.value = "${currentServer.name.uppercase().replace(Regex("[^A-Z0-9]"), "-").take(10)}-2026"
                }
                _isServerInviteSheetOpen.value = true
            }
            is ChannelsEvent.CloseServerInviteSheet -> {
                _isServerInviteSheetOpen.value = false
            }
            is ChannelsEvent.JoinServerByInvite -> {
                _isAddServerSelected.value = false
                val prof = _currentProfile.value ?: studentProfileRepository.studentProfile.value
                val author = currentSupabaseProfileName().orEmpty()
                val authorAvatar = prof?.avatarUrl
                val authorId = prof?.stableUserId().orEmpty()
                if (author.isBlank() || authorId.isBlank()) {
                    _feedbackMessage.value = "A Supabase profile is required to join a server"
                    return
                }
                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val result = repo.joinServerByInvite(
                            inviteCode = event.inviteCode,
                            userId = authorId,
                            avatarUrl = authorAvatar,
                            avatarResName = authorAvatar ?: ""
                        )
                        result.onSuccess { srv ->
                            _joinedServerIds.value = _joinedServerIds.value + srv.id
                            _selectedServerId.value = srv.id
                            _previewServerId.value = null
                            _feedbackMessage.value = "Joined ${srv.name}!"
                            viewModelScope.launch {
                                repo.syncRemoteMembers(srv.id)
                                repo.syncRemoteServers()
                            }
                        }.onFailure { err ->
                            _feedbackMessage.value = err.message ?: "Failed to join server"
                        }
                    }
                }
            }
            is ChannelsEvent.PreviewCommunityServer -> {
                _isAddServerSelected.value = false
                val existing = _serversList.value.find { it.name.equals(event.serverName, ignoreCase = true) || it.id == event.serverName }
                if (existing != null) {
                    val currentUserId = _currentProfile.value?.studentId?.ifBlank { _currentProfile.value?.universityEmail } ?: ""
                    val isOwner = currentUserId.isNotBlank() && existing.ownerId == currentUserId
                    val isJoined = _joinedServerIds.value.contains(existing.id)

                    if (isOwner || isJoined) {
                        _previewServerId.value = null
                        _selectedServerId.value = existing.id
                        existing.channels.firstOrNull()?.let {
                            _selectedChannelId.value = it.id
                        }
                    } else {
                        viewModelScope.launch {
                            _loadingPreviewServerName.value = event.serverName
                            kotlinx.coroutines.delay(350)
                            _loadingPreviewServerName.value = null
                            _previewServerId.value = existing.id
                            existing.channels.firstOrNull()?.let {
                                _selectedChannelId.value = it.id
                            }
                        }
                    }
                }
            }
            is ChannelsEvent.ExitServerPreview -> {
                _previewServerId.value = null
                _isAddServerSelected.value = true
            }
            is ChannelsEvent.JoinCurrentPreviewServer -> {
                val previewId = _previewServerId.value ?: _selectedServerId.value
                if (previewId.isNotBlank()) {
                    val targetServer = _serversList.value.find { it.id == previewId }
                    val prof = _currentProfile.value ?: studentProfileRepository.studentProfile.value
                    val author = currentSupabaseProfileName().orEmpty()
                    val authorAvatar = prof?.avatarUrl
                    val authorId = prof?.stableUserId().orEmpty()
                    if (author.isBlank() || authorId.isBlank()) {
                        _feedbackMessage.value = "A Supabase profile is required to join a server"
                        return
                    }
                    val authorHandle = "@" + author.lowercase().replace(" ", "").filter { it.isLetterOrDigit() || it == '_' }

                    viewModelScope.launch {
                        _isJoiningServer.value = true

                        _joinedServerIds.value = _joinedServerIds.value + previewId
                        _selectedServerId.value = previewId
                        _previewServerId.value = null

                        val currentMembers = _serverMembers.value[previewId].orEmpty()
                        val newMember = ServerMemberItem(
                            id = "mem_${authorId}_$previewId",
                            serverId = previewId,
                            name = author,
                            handle = authorHandle,
                            avatarResName = if (authorAvatar?.startsWith("http") == true || authorAvatar?.startsWith("content:") == true) null else authorAvatar,
                            avatarUrl = if (authorAvatar?.startsWith("http") == true || authorAvatar?.startsWith("content:") == true) authorAvatar else null,
                            serverNickname = author,
                            serverTag = "MEMBER",
                            serverTagColorHex = 0xFF5C6470,
                            role = ServerMemberRole("Member", ServerRoleType.MEMBER, 0xFF5C6470, isOwner = false),
                            isOnline = true,
                            joinedDate = "Joined Just Now",
                            bio = "Campus peer & server member.",
                            friendStatus = FriendRequestStatus.NONE,
                            isSelf = true
                        )
                        _serverMembers.value = _serverMembers.value + (previewId to (currentMembers + newMember).distinctBy { it.id })

                        serverRepository?.joinServerDirectly(
                            serverId = previewId,
                            userId = authorId,
                            avatarUrl = authorAvatar,
                            avatarResName = authorAvatar ?: ""
                        )

                        runCatching {
                            serverRepository?.syncRemoteMembers(previewId)
                            serverRepository?.syncRemoteServers()
                        }

                        kotlinx.coroutines.delay(450)
                        _isJoiningServer.value = false
                        _feedbackMessage.value = "Joined ${targetServer?.name ?: "server"}!"
                    }
                }
            }
            is ChannelsEvent.SelectFaculty -> {
                _selectedFaculty.value = event.facultyName
                _selectedDepartment.value = "All"
                _isCoursesExpanded.value = false
            }
            is ChannelsEvent.SelectDepartment -> {
                _selectedDepartment.value = event.departmentName
            }
            is ChannelsEvent.ToggleCoursesExpanded -> {
                _isCoursesExpanded.value = !_isCoursesExpanded.value
            }
            is ChannelsEvent.Search -> _searchQuery.value = event.query
            is ChannelsEvent.SelectCourse -> {
                val dummyChannel = AcademicChannel(
                    id = event.course.id,
                    courseCode = event.course.courseCode,
                    courseTitle = event.course.title,
                    faculty = event.course.faculty,
                    department = event.course.department,
                    unreadCount = 0,
                    lastMessageSnippet = event.course.activeDiscussionSnippet,
                    lastMessageTime = "Just now",
                    activeStudentsCount = event.course.enrolledMatesCount,
                    isPinned = true
                )
                _selectedChannel.value = dummyChannel
                viewModelScope.launch {
                    repository.markChannelRead(event.course.id)
                }
            }
            is ChannelsEvent.OpenDiscovery -> {
                _selectedCourseForDiscovery.value = event.course
                _viewMode.value = ChannelViewMode.DISCOVER_CAROUSEL
            }
            is ChannelsEvent.OpenStudentProfile -> {
                _selectedStudentForProfile.value = event.student
                _viewMode.value = ChannelViewMode.STUDENT_PROFILE
            }
            is ChannelsEvent.BackToPreviousMode -> {
                if (_isServerProfileDrawerOpen.value) {
                    _isServerProfileDrawerOpen.value = false
                    return
                }
                when (_viewMode.value) {
                    ChannelViewMode.CHANNEL_PROFILE -> {
                        _viewMode.value = ChannelViewMode.CHANNEL_CHAT
                    }
                    ChannelViewMode.CHANNEL_CHAT -> {
                        publishTyping(false)
                        stopChannelMessageStream()
                        _viewMode.value = ChannelViewMode.CHANNELS_HUB
                        _isMediaDrawerOpen.value = false
                        startTypingPresencePoll(
                            serverId = _selectedServerId.value,
                            focusChannelId = null
                        )
                    }
                    ChannelViewMode.STUDENT_PROFILE -> {
                        _viewMode.value = ChannelViewMode.DISCOVER_CAROUSEL
                    }
                    ChannelViewMode.DISCOVER_CAROUSEL -> {
                        _viewMode.value = ChannelViewMode.CHANNELS_HUB
                        _selectedCourseForDiscovery.value = null
                    }
                    ChannelViewMode.CHANNELS_HUB -> Unit
                }
            }
            is ChannelsEvent.ToggleLikeStudent -> {
                val current = _likedStudentIds.value.toMutableSet()
                if (current.contains(event.studentId)) {
                    current.remove(event.studentId)
                    _feedbackMessage.value = "Removed from your study connections"
                } else {
                    current.add(event.studentId)
                    _feedbackMessage.value = "Study connection request sent!"
                }
                _likedStudentIds.value = current
            }
            is ChannelsEvent.ShowFeedback -> _feedbackMessage.value = event.message
            is ChannelsEvent.ClearFeedback -> _feedbackMessage.value = null
            is ChannelsEvent.ClearSelectedChannel -> _selectedChannel.value = null

            is ChannelsEvent.OpenMemberProfile -> {
                _selectedMemberForProfile.value = event.member
            }

            is ChannelsEvent.CloseMemberProfile -> {
                _selectedMemberForProfile.value = null
            }

            is ChannelsEvent.SendFriendRequest -> {
                val memberId = event.memberId
                val member = _selectedMemberForProfile.value?.takeIf { it.id == memberId }
                    ?: _serverMembers.value.values.flatten().find { it.id == memberId }
                _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                    members.map { mem ->
                        if (mem.id == memberId) {
                            mem.copy(friendStatus = FriendRequestStatus.REQUEST_SENT)
                        } else mem
                    }
                }
                _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let {
                    if (it.id == memberId) it.copy(friendStatus = FriendRequestStatus.REQUEST_SENT) else it
                }
                val me = _currentProfile.value
                val myId = me?.studentId?.takeIf { it.isNotBlank() }
                    ?: me?.universityEmail?.takeIf { it.isNotBlank() }
                    ?: return
                val peerUserId = extractMemberUserId(member, memberId, member?.serverId.orEmpty())
                if (peerUserId.isNotBlank()) {
                    viewModelScope.launch {
                        val result = runCatching {
                            (serverRepository ?: error("Friend services are unavailable"))
                                .sendFriendRequest(
                                    userId = myId,
                                    friendUserId = peerUserId,
                                    serverId = member?.serverId
                                )
                        }
                        _feedbackMessage.value = result.fold(
                            { "Friend request sent to $it!" },
                            { "Friend request could not be sent." }
                        )
                    }
                }
            }

            is ChannelsEvent.CancelFriendRequest -> {
                val memberId = event.memberId
                val member = _selectedMemberForProfile.value?.takeIf { it.id == memberId }
                    ?: _serverMembers.value.values.flatten().find { it.id == memberId }
                _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                    members.map { mem ->
                        if (mem.id == memberId) {
                            mem.copy(friendStatus = FriendRequestStatus.NONE)
                        } else mem
                    }
                }
                _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let {
                    if (it.id == memberId) it.copy(friendStatus = FriendRequestStatus.NONE) else it
                }
                _feedbackMessage.value = "Friend request cancelled."
                val me = _currentProfile.value
                val myId = me?.studentId?.takeIf { it.isNotBlank() }
                    ?: me?.universityEmail?.takeIf { it.isNotBlank() }
                val peerUserId = extractMemberUserId(member, memberId, member?.serverId.orEmpty())
                if (!myId.isNullOrBlank() && peerUserId.isNotBlank()) {
                    viewModelScope.launch {
                        serverRepository?.declineOrRemoveFriend(userId = myId, friendUserId = peerUserId)
                    }
                }
            }

            is ChannelsEvent.AcceptFriendRequest -> {
                val memberId = event.memberId
                val member = _selectedMemberForProfile.value?.takeIf { it.id == memberId }
                    ?: _serverMembers.value.values.flatten().find { it.id == memberId }
                _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                    members.map { mem ->
                        if (mem.id == memberId) {
                            mem.copy(friendStatus = FriendRequestStatus.FRIENDS)
                        } else mem
                    }
                }
                _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let {
                    if (it.id == memberId) it.copy(friendStatus = FriendRequestStatus.FRIENDS) else it
                }
                val me = _currentProfile.value
                val myId = me?.studentId?.takeIf { it.isNotBlank() }
                    ?: me?.universityEmail?.takeIf { it.isNotBlank() }
                val peerUserId = extractMemberUserId(member, memberId, member?.serverId.orEmpty())
                if (!myId.isNullOrBlank() && peerUserId.isNotBlank()) {
                    viewModelScope.launch {
                        val result = runCatching {
                            (serverRepository ?: error("Friend services are unavailable"))
                                .acceptFriendRequest(userId = myId, friendUserId = peerUserId)
                        }
                        _feedbackMessage.value = result.fold(
                            { "You are now friends with $it!" },
                            { "Friend request could not be accepted." }
                        )
                    }
                }
            }

            is ChannelsEvent.DeclineFriendRequest -> {
                val memberId = event.memberId
                val member = _selectedMemberForProfile.value?.takeIf { it.id == memberId }
                    ?: _serverMembers.value.values.flatten().find { it.id == memberId }
                _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                    members.map { mem ->
                        if (mem.id == memberId) {
                            mem.copy(friendStatus = FriendRequestStatus.NONE)
                        } else mem
                    }
                }
                _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let {
                    if (it.id == memberId) it.copy(friendStatus = FriendRequestStatus.NONE) else it
                }
                _feedbackMessage.value = "Friend request declined."
                val me = _currentProfile.value
                val myId = me?.studentId?.takeIf { it.isNotBlank() }
                    ?: me?.universityEmail?.takeIf { it.isNotBlank() }
                val peerUserId = extractMemberUserId(member, memberId, member?.serverId.orEmpty())
                if (!myId.isNullOrBlank() && peerUserId.isNotBlank()) {
                    viewModelScope.launch {
                        serverRepository?.declineOrRemoveFriend(userId = myId, friendUserId = peerUserId)
                    }
                }
            }

            is ChannelsEvent.OpenEditServerProfile -> {
                _isEditServerProfileOpen.value = true
            }

            is ChannelsEvent.CloseEditServerProfile -> {
                _isEditServerProfileOpen.value = false
            }

            is ChannelsEvent.SavePerServerProfile -> {
                val profile = PerServerProfile(
                    serverId = event.serverId,
                    nickname = event.nickname,
                    tag = event.tag,
                    tagColorHex = event.tagColorHex
                )
                _perServerProfiles.value = _perServerProfiles.value + (event.serverId to profile)

                _serverMembers.value = _serverMembers.value.mapValues { (sId, members) ->
                    if (sId == event.serverId) {
                        members.map { mem ->
                            val selfName = _currentProfile.value?.fullName?.ifBlank { "You" } ?: "You"
                            val selfFirst = _currentProfile.value?.firstName?.ifBlank { "You" } ?: "You"
                            if (mem.name.contains(selfFirst, ignoreCase = true) || mem.name.contains(selfName, ignoreCase = true)) {
                                mem.copy(
                                    serverNickname = event.nickname,
                                    serverTag = event.tag,
                                    serverTagColorHex = event.tagColorHex
                                )
                            } else mem
                        }
                    } else members
                }
                _feedbackMessage.value = "Updated server profile tag [${event.tag.uppercase()}]!"
            }

            is ChannelsEvent.OpenEditServerSettings -> {
                _isEditServerSettingsOpen.value = true
            }

            is ChannelsEvent.CloseEditServerSettings -> {
                _isEditServerSettingsOpen.value = false
            }

            is ChannelsEvent.UpdateServerSettings -> {
                _isEditServerSettingsOpen.value = false
                _serversList.value = _serversList.value.map { srv ->
                    if (srv.id == event.serverId) {
                        srv.copy(
                            name = event.name.trim().ifEmpty { srv.name },
                            categoryText = event.category.trim().ifEmpty { srv.categoryText },
                            iconResName = event.avatarResName.ifEmpty { srv.iconResName },
                            bannerResName = event.bannerResName ?: srv.bannerResName
                        )
                    } else srv
                }
                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val result = repo.updateServerProfile(
                            serverId = event.serverId,
                            name = event.name,
                            description = event.description,
                            category = event.category,
                            avatarResName = event.avatarResName,
                            bannerResName = event.bannerResName
                        )
                        result.onSuccess {
                            _feedbackMessage.value = "Server settings updated"
                        }.onFailure { err ->
                            _feedbackMessage.value = "Settings saved locally, but cloud update failed: ${err.localizedMessage ?: "Network error"}"
                        }
                    }
                }
            }

            is ChannelsEvent.UpdateMemberRole -> {
                val (roleTitle, roleColor, domainRole) = when (event.newRole) {
                    ServerRoleType.ADMIN -> Triple("Server Admin", 0xFF5865F2, com.campus.network.core.model.ServerMemberRole.ADMIN)
                    ServerRoleType.MODERATOR -> Triple("Moderator", 0xFF23A55A, com.campus.network.core.model.ServerMemberRole.MODERATOR)
                    ServerRoleType.CONTRIBUTOR -> Triple("Core Contributor", 0xFFF59E0B, com.campus.network.core.model.ServerMemberRole.MEMBER)
                    ServerRoleType.MEMBER -> Triple("Member", 0xFF5C6470, com.campus.network.core.model.ServerMemberRole.MEMBER)
                    ServerRoleType.OWNER -> Triple("Server Owner", 0xFFEAB308, com.campus.network.core.model.ServerMemberRole.OWNER)
                }
                val members = _serverMembers.value[event.serverId].orEmpty()
                val updatedMembers = members.map { mem ->
                    if (mem.id == event.memberId) {
                        mem.copy(
                            role = mem.role.copy(
                                name = roleTitle,
                                roleType = event.newRole,
                                colorHex = roleColor,
                                isOwner = (event.newRole == ServerRoleType.OWNER)
                            )
                        )
                    } else mem
                }
                _serverMembers.value = _serverMembers.value + (event.serverId to updatedMembers)
                if (_selectedMemberForProfile.value?.id == event.memberId) {
                    _selectedMemberForProfile.value = updatedMembers.find { it.id == event.memberId }
                }
                _feedbackMessage.value = "Role updated to $roleTitle"
                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        runCatching {
                            repo.updateMemberRole(
                                serverId = event.serverId,
                                userId = event.memberId,
                                role = domainRole,
                                roleTitle = roleTitle,
                                roleColorHex = roleColor
                            )
                        }
                    }
                }
            }

            is ChannelsEvent.OpenServerMembersSheet -> {
                _currentMemberFilterChannelId.value = event.channelId
                _isServerMembersSheetOpen.value = true
                if (event.serverId.isNotBlank()) {
                    viewModelScope.launch {
                        serverRepository?.syncRemoteMembers(event.serverId)
                    }
                }
            }

            is ChannelsEvent.CloseServerMembersSheet -> {
                _isServerMembersSheetOpen.value = false
                _currentMemberFilterChannelId.value = null
            }

            is ChannelsEvent.DeleteServer -> {
                val targetServerId = event.serverId
                _isServerProfileDrawerOpen.value = false
                _joinedServerIds.value = _joinedServerIds.value - targetServerId
                _serversList.value = _serversList.value.filterNot { it.id == targetServerId }
                val remaining = _serversList.value.filter { it.id in _joinedServerIds.value }
                if (_selectedServerId.value == targetServerId) {
                    val nextServer = remaining.firstOrNull()
                    _selectedServerId.value = nextServer?.id.orEmpty()
                    _selectedChannelId.value = nextServer?.channels?.firstOrNull()?.id.orEmpty()
                }
                _feedbackMessage.value = "Server deleted"
                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        runCatching {
                            repo.deleteServer(targetServerId)
                        }
                    }
                }
            }

            is ChannelsEvent.LeaveServer -> {
                val targetServerId = event.serverId
                _isServerProfileDrawerOpen.value = false
                _joinedServerIds.value = _joinedServerIds.value - targetServerId
                val remaining = _serversList.value.filter { it.id in _joinedServerIds.value }
                if (_selectedServerId.value == targetServerId) {
                    val nextServer = remaining.firstOrNull()
                    _selectedServerId.value = nextServer?.id.orEmpty()
                    _selectedChannelId.value = nextServer?.channels?.firstOrNull()?.id.orEmpty()
                }
                _feedbackMessage.value = "Left server"
                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val currentStudentId = _currentProfile.value?.studentId?.ifBlank { _currentProfile.value?.universityEmail } ?: ""
                        if (currentStudentId.isNotBlank()) {
                            runCatching {
                                repo.removeMember(targetServerId, currentStudentId)
                            }
                        }
                    }
                }
            }

            is ChannelsEvent.UpdateServerNotificationSetting -> {
                _feedbackMessage.value = "Notifications set to ${event.setting}"
            }
        }
    }

    /** Member UI ids are `mem_{userId}_{serverId}` — peel back to the stable user id. */
    private fun extractMemberUserId(member: ServerMemberItem?, memberId: String, serverId: String): String {
        if (!member?.userId.isNullOrBlank()) return member!!.userId
        val raw = memberId.trim()
        if (raw.isBlank()) return ""
        val withoutMem = raw.removePrefix("mem_")
        if (serverId.isNotBlank() && withoutMem.endsWith("_$serverId")) {
            return withoutMem.removeSuffix("_$serverId")
        }
        // Fallback: strip trailing _srv… segment when serverId unknown
        val lastSrv = withoutMem.lastIndexOf("_srv")
        if (lastSrv > 0) return withoutMem.substring(0, lastSrv)
        return withoutMem
    }

    private fun formatTypingLabel(typers: List<ChannelTypingUser>): String? {
        val namedTypers = typers.filter { it.displayName.isRealProfileDisplayName() }
        if (namedTypers.isEmpty()) return null
        return when (namedTypers.size) {
            1 -> "${namedTypers[0].displayName} is typing…"
            2 -> "${namedTypers[0].displayName} and ${namedTypers[1].displayName} are typing…"
            else -> "${namedTypers[0].displayName} and ${namedTypers.size - 1} others are typing…"
        }
    }

    private fun currentSupabaseProfileName(): String? {
        val profile = _currentProfile.value ?: studentProfileRepository.studentProfile.value ?: return null
        val userId = profile.stableUserId()
        val known = studentProfileRepository.knownProfiles.value
        val knownProfile = known[userId]
            ?: known[userId.lowercase()]
            ?: known.values.firstOrNull { it.stableUserId().equals(userId, ignoreCase = true) }
        return knownProfile?.fullName?.takeIf(String::isRealProfileDisplayName)
    }

    private fun publishTyping(isTyping: Boolean) {
        typingHeartbeatJob?.cancel()
        val channel = _activeChatChannel.value ?: return
        val serverId = _selectedServerId.value
        val profile = _currentProfile.value
        val userId = profile?.stableUserId().orEmpty()
        if (userId.isBlank() || channel.id.isBlank()) return
        val displayName = currentSupabaseProfileName().orEmpty()
        if (isTyping && displayName.isBlank()) return
        val avatar = profile?.avatarUrl
        if (!isTyping) {
            viewModelScope.launch {
                channelChatRepository.setTyping(
                    serverId = serverId,
                    channelId = channel.id,
                    userId = userId,
                    avatarUrl = avatar,
                    isTyping = false
                )
            }
            return
        }
        // Heartbeat every 2s while composer has text
        typingHeartbeatJob = viewModelScope.launch {
            while (isActive) {
                channelChatRepository.setTyping(
                    serverId = serverId,
                    channelId = channel.id,
                    userId = userId,
                    avatarUrl = avatar,
                    isTyping = true
                )
                delay(2_000L)
            }
        }
    }

    private fun startChannelMessageStream(channelId: String) {
        channelMessagePollJob?.cancel()
        channelMessagePollJob = viewModelScope.launch {
            // Initial history pull
            pullChannelMessages(channelId, fullHistory = true)
            while (isActive) {
                delay(2_500L)
                pullChannelMessages(channelId, fullHistory = false)
            }
        }
    }

    private fun stopChannelMessageStream() {
        channelMessagePollJob?.cancel()
        channelMessagePollJob = null
    }

    private suspend fun hydrateChannelMessagesFromLocal(channelId: String) {
        val me = _currentProfile.value?.stableUserId().orEmpty()
        val local = channelChatRepository.loadLocalMessages(channelId)
        if (local.isEmpty()) return
        val mapped = local.map { mapRemoteChannelMessage(it, me) }
        var maxTs = channelMessageWatermarks[channelId] ?: 0L
        local.forEach { msg ->
            seenRemoteChannelMessageIds.add(msg.id)
            maxTs = maxOf(maxTs, msg.timestampEpochMs)
        }
        channelMessageWatermarks[channelId] = maxTs
        val existing = _channelMessages.value[channelId].orEmpty().associateBy { it.id }.toMutableMap()
        mapped.forEach { existing[it.id] = it }
        _channelMessages.value =
            _channelMessages.value + (channelId to existing.values.sortedBy { it.timestampEpochMs })
    }

    private fun flushPendingChannelMessages() {
        viewModelScope.launch {
            val pending = channelChatRepository.getPendingMessages()
            if (pending.isEmpty()) return@launch
            for (msg in pending) {
                val remote = channelChatRepository.sendMessage(
                    serverId = msg.serverId,
                    channelId = msg.channelId,
                    senderId = msg.senderId,
                    senderAvatarUrl = msg.senderAvatarUrl,
                    text = msg.text,
                    gifUrl = msg.gifUrl,
                    imageUrls = msg.imageUrls,
                    messageId = msg.id,
                    timestampEpochMs = msg.timestampEpochMs
                )
                if (remote.isSuccess) {
                    channelChatRepository.markDelivered(msg.id)
                }
            }
        }
    }

    private suspend fun pullChannelMessages(channelId: String, fullHistory: Boolean) {
        val after = if (fullHistory) 0L else (channelMessageWatermarks[channelId] ?: 0L)
        val result = channelChatRepository.fetchMessages(channelId, afterEpochMs = after)
        val remote = result.getOrNull() ?: return
        if (remote.isEmpty()) return
        val me = _currentProfile.value?.stableUserId().orEmpty()
        // Durable local cache of whatever we pulled from remote
        channelChatRepository.cacheRemoteMessages(remote, me)
        val existing = _channelMessages.value[channelId].orEmpty().associateBy { it.id }.toMutableMap()
        var maxTs = channelMessageWatermarks[channelId] ?: 0L
        for (msg in remote) {
            maxTs = maxOf(maxTs, msg.timestampEpochMs)
            if (!seenRemoteChannelMessageIds.add(msg.id) && existing.containsKey(msg.id)) continue
            existing[msg.id] = mapRemoteChannelMessage(msg, me)
            // Bump unread when message arrives for a channel that isn't open
            val viewingThis = _viewMode.value == ChannelViewMode.CHANNEL_CHAT &&
                _activeChatChannel.value?.id == channelId
            if (!viewingThis && !msg.senderId.equals(me, ignoreCase = true)) {
                _serversList.value = _serversList.value.map { srv ->
                    srv.copy(
                        channels = srv.channels.map { ch ->
                            if (ch.id != channelId) ch
                            else ch.copy(
                                isUnread = true,
                                unreadBadgeCount = (ch.unreadBadgeCount + 1).coerceAtLeast(1)
                            )
                        }
                    )
                }
            }
        }
        channelMessageWatermarks[channelId] = maxTs
        val merged = existing.values.sortedBy { it.timestampEpochMs }
        _channelMessages.value = _channelMessages.value + (channelId to merged)
    }

    private fun mapRemoteChannelMessage(msg: RemoteChannelChatMessage, myUserId: String): ChannelMessage {
        val timeLabel = "Today at " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(msg.timestampEpochMs))
        val isMine = myUserId.isNotBlank() && msg.senderId.equals(myUserId, ignoreCase = true)
        return ChannelMessage(
            id = msg.id,
            authorName = msg.senderName,
            authorId = msg.senderId,
            authorAvatarRes = msg.senderAvatarUrl,
            authorColorHex = if (isMine) 0xFF003B68 else 0xFF1E1F22,
            timestamp = timeLabel,
            text = msg.text,
            gifUrl = msg.gifUrl,
            imageUris = msg.imageUrls,
            reactionCount = msg.reactionCount,
            timestampEpochMs = msg.timestampEpochMs,
            isMine = isMine
        )
    }

    private fun startTypingPresencePoll(serverId: String, focusChannelId: String?) {
        channelTypingPollJob?.cancel()
        if (serverId.isBlank() && focusChannelId.isNullOrBlank()) return
        channelTypingPollJob = viewModelScope.launch {
            while (isActive) {
                val me = _currentProfile.value?.stableUserId().orEmpty()
                val remote = when {
                    !focusChannelId.isNullOrBlank() ->
                        channelChatRepository.fetchTypingForChannel(focusChannelId).getOrNull().orEmpty()
                    serverId.isNotBlank() ->
                        channelChatRepository.fetchTypingForServer(serverId).getOrNull().orEmpty()
                    else -> emptyList()
                }
                val grouped = remote
                    .filter { !it.userId.equals(me, ignoreCase = true) }
                    .groupBy { it.channelId }
                    .mapValues { (_, users) ->
                        users.distinctBy { it.userId }.map {
                            ChannelTypingUser(
                                userId = it.userId,
                                displayName = it.displayName,
                                avatarUrl = it.avatarUrl
                            )
                        }
                    }
                _typingByChannelId.value = if (focusChannelId.isNullOrBlank()) {
                    grouped
                } else {
                    // Keep other channel typing labels from server poll when focused; merge focus
                    val base = _typingByChannelId.value.toMutableMap()
                    base.keys.filter { it != focusChannelId && it !in grouped }.forEach { base.remove(it) }
                    grouped.forEach { (k, v) -> base[k] = v }
                    if (focusChannelId !in grouped) base.remove(focusChannelId)
                    base
                }
                delay(1_500L)
            }
        }
    }
}

private data class ServerJoinSlice(
    val studentProfile: StudentProfile?,
    val previewServerId: String?,
    val joinedServerIds: Set<String>,
    val isJoiningServer: Boolean,
    val loadingPreviewServerName: String?
)

private data class ServerStateSlice1(
    val viewMode: ChannelViewMode,
    val selectedServerId: String,
    val selectedChannelId: String,
    val isRailCollapsed: Boolean,
    val serverSearchQuery: String,
    val isServerProfileDrawerOpen: Boolean = false,
    val isAddServerSelected: Boolean = false,
    val studentProfile: StudentProfile? = null,
    val previewServerId: String? = null,
    val joinedServerIds: Set<String> = emptySet(),
    val isJoiningServer: Boolean = false,
    val loadingPreviewServerName: String? = null
)

private data class ServerStateSlice2(
    val activeChatChannel: ServerChannelItem?,
    val isMediaDrawerOpen: Boolean,
    val activeMediaTab: String,
    val channelMessages: Map<String, List<ChannelMessage>>,
    val channelProfileTab: String = "Members",
    val typingByChannelId: Map<String, List<ChannelTypingUser>> = emptyMap()
)

/** Tiny tuple helper for nested combine without Pair nesting. */
private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

private data class AcademicFilterSlice(
    val faculty: String,
    val department: String,
    val isExpanded: Boolean,
    val query: String
)

private data class PeerSlice(
    val selectedCourse: CareerCourse?,
    val selectedStudent: CourseMate?,
    val likedIds: Set<String>,
    val feedback: String?
)

private data class ServerManagementFlags(
    val isCreateChannelSheetOpen: Boolean,
    val isRulesSheetOpen: Boolean,
    val isServerInviteSheetOpen: Boolean,
    val isEditServerSettingsOpen: Boolean,
    val targetChannelCategory: String
)

private data class ServerManagementSlice(
    val isCreateChannelSheetOpen: Boolean,
    val isRulesSheetOpen: Boolean,
    val isServerInviteSheetOpen: Boolean,
    val isEditServerSettingsOpen: Boolean,
    val targetChannelCategory: String,
    val currentServerRules: List<ServerRule>,
    val currentServerInviteCode: String
)

private data class ServerMemberSlice(
    val serverMembers: Map<String, List<ServerMemberItem>>,
    val perServerProfiles: Map<String, PerServerProfile>,
    val selectedMember: ServerMemberItem?,
    val isEditServerProfileOpen: Boolean,
    val isServerMembersSheetOpen: Boolean,
    val filterChannelId: String?
)
