package com.campus.network.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.AcademicChannelsRepository
import com.campus.network.core.data.ServerRepository
import com.campus.network.core.model.AcademicChannel
import com.campus.network.core.model.ServerChannelType
import com.campus.network.core.model.ServerRule
import com.campus.network.feature.channels.model.CareerCourse
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.channels.model.CourseMate
import com.campus.network.feature.channels.model.FriendRequestStatus
import com.campus.network.feature.channels.model.InDemandCompany
import com.campus.network.feature.channels.model.PerServerProfile
import com.campus.network.feature.channels.model.ServerMemberItem
import com.campus.network.feature.channels.model.ServerMemberRole
import com.campus.network.feature.channels.model.ServerRoleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val unreadBadgeCount: Int = 0
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
    val dateDividerText: String? = null
)

data class ServerItem(
    val id: String,
    val name: String,
    val memberCountText: String = "",
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
    val channels: List<ServerChannelItem> = emptyList()
)

data class ChannelsUiState(
    val viewMode: ChannelViewMode = ChannelViewMode.CHANNELS_HUB,
    val servers: List<ServerItem> = emptyList(),
    val selectedServerId: String = "srv_codesistency",
    val selectedServer: ServerItem? = null,
    val selectedChannelId: String = "ch_mern_github",
    val isRailCollapsed: Boolean = false,
    val activeUserName: String = "Lincoln Paul",
    val activeUserStatus: String = "Online",
    val activeUserUnreadCount: Int = 2,
    val activeChatServer: ServerItem? = null,
    val activeChatChannel: ServerChannelItem? = null,
    val chatMessages: List<ChannelMessage> = emptyList(),
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
        val bannerResName: String? = null
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
    data object OpenEditServerProfile : ChannelsEvent
    data object CloseEditServerProfile : ChannelsEvent
    data class SavePerServerProfile(
        val serverId: String,
        val nickname: String,
        val tag: String,
        val tagColorHex: Long
    ) : ChannelsEvent
    data class OpenServerMembersSheet(val serverId: String, val channelId: String? = null) : ChannelsEvent
    data object CloseServerMembersSheet : ChannelsEvent
}

class ChannelsViewModel(
    private val repository: AcademicChannelsRepository,
    private val serverRepository: ServerRepository? = null
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ChannelViewMode.CHANNELS_HUB)
    private val _selectedServerId = MutableStateFlow("srv_codesistency")
    private val _selectedChannelId = MutableStateFlow("ch_mern_github")
    private val _isRailCollapsed = MutableStateFlow(false)
    private val _serverSearchQuery = MutableStateFlow("")
    private val _isServerProfileDrawerOpen = MutableStateFlow(false)
    private val _isAddServerSelected = MutableStateFlow(false)
    private val _activeChatChannel = MutableStateFlow<ServerChannelItem?>(null)
    private val _isMediaDrawerOpen = MutableStateFlow(false)
    private val _activeMediaTab = MutableStateFlow("Photos")
    private val _channelProfileTab = MutableStateFlow("Members")
    private val _channelMessages = MutableStateFlow<Map<String, List<ChannelMessage>>>(emptyMap())
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
    private val _isServerMembersSheetOpen = MutableStateFlow(false)
    private val _currentMemberFilterChannelId = MutableStateFlow<String?>(null)

    // Master Servers & Channels Catalog (Matching patriarch Layout & Campus Community)
    private val codesistencyChannels = listOf(
        ServerChannelItem(id = "ch_onlyfans_c", name = "onlyfans-c", isUnread = false, badgeText = "@ NEW"),
        ServerChannelItem(id = "ch_redis_chat", name = "redis-chat-app", isUnread = false, hasBoltIcon = true),
        ServerChannelItem(id = "ch_nextjs", name = "nextjs-course", isUnread = true),
        ServerChannelItem(id = "ch_react_native_recipe", name = "react-native-recipe-app", isUnread = true),
        ServerChannelItem(id = "ch_leetcode", name = "leetcode-clone", isUnread = true),
        ServerChannelItem(id = "ch_threads", name = "threads-clone", isUnread = false),
        ServerChannelItem(id = "ch_mern_github", name = "mern-github-app", isUnread = true, isSelected = true),
        ServerChannelItem(id = "ch_instagram", name = "instagram-clone", isUnread = false),
        ServerChannelItem(id = "ch_mern_chat", name = "mern-chat-app", isUnread = false),
        ServerChannelItem(id = "ch_whatsapp", name = "whatsapp-clone", isUnread = false),
        ServerChannelItem(id = "ch_graphql", name = "graphql-crash-course", isUnread = false),
        ServerChannelItem(id = "ch_twitter", name = "twitter-clone", isUnread = false),
        ServerChannelItem(id = "ch_pern_chat", name = "pern-chat-app", isUnread = false),
        ServerChannelItem(id = "ch_go_react", name = "go-react-tutorial", isUnread = false),
        ServerChannelItem(id = "ch_react_python", name = "react-python-tutorial", isUnread = true),
        ServerChannelItem(id = "ch_study_app", name = "study-app-react-native", isUnread = false)
    )

    private val initialMasterServersList = listOf(
        ServerItem(
            id = "srv_owl",
            name = "Unciv",
            memberCountText = "13,812 Members",
            categoryText = "Community",
            isVerified = true,
            iconResName = "server_owl",
            badgeCount = 50,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_unciv_role", name = "role-subscriptions", isUnread = false),
                ServerChannelItem(id = "ch_unciv_spam", name = "spam-ban", isUnread = false),
                ServerChannelItem(id = "ch_unciv_announcements", name = "announcements", isUnread = false),
                ServerChannelItem(id = "ch_unciv_github", name = "github", isUnread = true),
                ServerChannelItem(id = "ch_unciv_devs", name = "developers", isUnread = false),
                ServerChannelItem(id = "ch_unciv_polls", name = "polls", isUnread = false),
                ServerChannelItem(id = "ch_unciv_pollchat", name = "poll-chat", isUnread = true, isSelected = true),
                ServerChannelItem(id = "ch_unciv_bugs", name = "bugs", isUnread = false),
                ServerChannelItem(id = "ch_unciv_bugs_disc", name = "bugs-discussion", isUnread = true),
                ServerChannelItem(id = "ch_unciv_suggestions", name = "suggestions", isUnread = false),
                ServerChannelItem(id = "ch_unciv_art", name = "game-art", isUnread = false),
                ServerChannelItem(id = "ch_unciv_music", name = "game-music", isUnread = false),
                ServerChannelItem(id = "ch_unciv_ios", name = "ios-development", isUnread = false),
                ServerChannelItem(id = "ch_unciv_data", name = "data-gathering", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_basi",
            name = "BASI",
            memberCountText = "63,075 Members",
            categoryText = "Community",
            isVerified = true,
            badgeType = "booster",
            iconResName = "server_dragon",
            badgeCount = 0,
            hasUnread = true,
            bannerResName = "server_banner_basi",
            isPreview = false,
            boostGoalText = "37 Boosts",
            hasServerGuide = false,
            channels = listOf(
                ServerChannelItem(
                    id = "ch_basi_announcements",
                    name = "announcements",
                    channelType = "announcements",
                    categoryName = "Announcements",
                    isUnread = true,
                    unreadBadgeCount = 17
                ),
                ServerChannelItem(
                    id = "ch_basi_rules",
                    name = "rules",
                    channelType = "rules",
                    categoryName = "Announcements",
                    isUnread = false
                ),
                ServerChannelItem(
                    id = "ch_basi_donations",
                    name = "donation-contributions",
                    channelType = "text",
                    categoryName = "Home",
                    isUnread = false
                ),
                ServerChannelItem(
                    id = "ch_basi_general",
                    name = "general-chat",
                    channelType = "text",
                    categoryName = "General",
                    isUnread = false
                ),
                ServerChannelItem(
                    id = "ch_basi_intros",
                    name = "introductions",
                    channelType = "text",
                    categoryName = "General",
                    isUnread = false
                ),
                ServerChannelItem(
                    id = "ch_basi_jailbreak",
                    name = "jailbreaking",
                    channelType = "text",
                    categoryName = "General",
                    isUnread = false
                )
            )
        ),
        ServerItem(
            id = "srv_honeycomb",
            name = "Campus Hub & Guilds",
            memberCountText = "8,420 Members",
            categoryText = "Campus Life",
            isVerified = true,
            iconResName = "server_home_cluster",
            badgeCount = 0,
            hasUnread = false,
            channels = listOf(
                ServerChannelItem(id = "ch_hub_general", name = "campus-general", isUnread = true),
                ServerChannelItem(id = "ch_hub_clubs", name = "student-clubs", isUnread = false),
                ServerChannelItem(id = "ch_hub_events", name = "weekend-events", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_codesistency",
            name = "Codesistency",
            memberCountText = "6,203 Members",
            categoryText = "Community",
            isVerified = true,
            iconResName = "server_codesistency",
            badgeCount = 139,
            hasUnread = false,
            channels = codesistencyChannels
        ),
        ServerItem(
            id = "srv_avatar2",
            name = "Nairobi Tech Guild",
            memberCountText = "3,410 Members",
            categoryText = "Technology",
            isVerified = true,
            iconResName = "server_avatar2",
            badgeCount = 40,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_ntg_general", name = "general-chat", isUnread = true),
                ServerChannelItem(id = "ch_ntg_web", name = "web-dev-kenya", isUnread = false),
                ServerChannelItem(id = "ch_ntg_mobile", name = "android-kotlin-squad", isUnread = true),
                ServerChannelItem(id = "ch_ntg_ai", name = "ai-builders", isUnread = false),
                ServerChannelItem(id = "ch_ntg_jobs", name = "campus-gigs", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_w_red",
            name = "Campus Founders Hub",
            memberCountText = "1,890 Members",
            categoryText = "Entrepreneurship",
            isVerified = false,
            iconResName = "server_w_red",
            badgeCount = 0,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_fh_pitch", name = "pitch-deck-critique", isUnread = true),
                ServerChannelItem(id = "ch_fh_cofounders", name = "find-a-cofounder", isUnread = false),
                ServerChannelItem(id = "ch_fh_mvp", name = "mvp-showcase", isUnread = false),
                ServerChannelItem(id = "ch_fh_funding", name = "grant-opportunities", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_gradient",
            name = "AI & HCI Labs",
            memberCountText = "4,120 Members",
            categoryText = "Research",
            isVerified = false,
            iconResName = "server_gradient",
            badgeCount = 0,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_hci_general", name = "lab-announcements", isUnread = true),
                ServerChannelItem(id = "ch_hci_papers", name = "arxiv-weekly-reads", isUnread = false),
                ServerChannelItem(id = "ch_hci_ux", name = "interaction-telemetry", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_ai_brain",
            name = "Deep Learning Nairobi",
            memberCountText = "2,560 Members",
            categoryText = "AI Engineering",
            isVerified = false,
            iconResName = "server_ai_brain",
            badgeCount = 41,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_dl_pytorch", name = "pytorch-sprints", isUnread = true),
                ServerChannelItem(id = "ch_dl_models", name = "open-source-llms", isUnread = false),
                ServerChannelItem(id = "ch_dl_hardware", name = "gpu-clusters", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_logo2",
            name = "Open Source Collective",
            memberCountText = "5,320 Members",
            categoryText = "Software",
            isVerified = true,
            iconResName = "server_logo2",
            badgeCount = 33,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_osc_contribute", name = "first-timers-welcome", isUnread = true),
                ServerChannelItem(id = "ch_osc_bounties", name = "paid-bounties", isUnread = false),
                ServerChannelItem(id = "ch_osc_reviews", name = "code-reviews", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_dragon",
            name = "Gaming & Graphics Guild",
            memberCountText = "980 Members",
            categoryText = "Gaming",
            isVerified = false,
            iconResName = "server_dragon",
            badgeCount = 17,
            hasUnread = false,
            channels = listOf(
                ServerChannelItem(id = "ch_gg_unreal", name = "unreal-engine-5", isUnread = false),
                ServerChannelItem(id = "ch_gg_shaders", name = "glsl-shaders", isUnread = false),
                ServerChannelItem(id = "ch_gg_jams", name = "game-jam-lobby", isUnread = true)
            )
        ),
        ServerItem(
            id = "srv_curly",
            name = "Full Stack Academy",
            memberCountText = "7,810 Members",
            categoryText = "Education",
            isVerified = true,
            iconResName = "server_curly",
            badgeCount = 16,
            hasUnread = false,
            channels = listOf(
                ServerChannelItem(id = "ch_fsa_frontend", name = "react-nextjs-hub", isUnread = false),
                ServerChannelItem(id = "ch_fsa_backend", name = "go-microservices", isUnread = false),
                ServerChannelItem(id = "ch_fsa_devops", name = "docker-k8s", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_eureka",
            name = "Eureka Labs",
            memberCountText = "12,450 Members",
            categoryText = "AI Education",
            isVerified = true,
            iconResName = "server_eureka",
            badgeCount = 0,
            hasUnread = false,
            channels = listOf(
                ServerChannelItem(id = "ch_elk_lectures", name = "ai-curriculum", isUnread = false),
                ServerChannelItem(id = "ch_elk_projects", name = "course-projects", isUnread = false)
            )
        ),
        ServerItem(
            id = "srv_nvidia",
            name = "NVIDIA Campus Devs",
            memberCountText = "15,800 Members",
            categoryText = "Hardware & CUDA",
            isVerified = true,
            iconResName = "server_nvidia",
            badgeCount = 0,
            hasUnread = true,
            channels = listOf(
                ServerChannelItem(id = "ch_nvd_cuda", name = "cuda-c-sprints", isUnread = true),
                ServerChannelItem(id = "ch_nvd_jetson", name = "jetson-robotics", isUnread = false),
                ServerChannelItem(id = "ch_nvd_grants", name = "academic-grants", isUnread = false)
            )
        )
    )

    private val _serversList = MutableStateFlow(initialMasterServersList)

    // Master Course-Mates Catalog
    private val sampleCourseMates = listOf(
        CourseMate(
            id = "mate_rk",
            name = "Rk",
            ageOrYear = "27",
            major = "Electrical & Embedded Engineering",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = true,
            helperBadgeText = "Academic Workspace Helper \u00B7 Top 5% in Circuits",
            studyGoal = "Building prototype PCB schematics \u00B7 Seeking study buddy for weekly problem sets",
            studyStyle = "Hardware Lab & Discussion",
            availabilityText = "Weekday Evenings & Weekends",
            sharedCourses = listOf("#eng-201", "#ee-310"),
            photoResName = "student_rk",
            bio = "Senior engineering student passionate about low-power IoT devices, firmware architecture, and mentoring first-year peers.",
            karmaScore = 840
        ),
        CourseMate(
            id = "mate_eliza",
            name = "Eliza",
            ageOrYear = "21",
            major = "HCI & Product Design",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = true,
            helperBadgeText = "Academic Workspace Helper",
            studyGoal = "Need study buddy for usability testing heuristics and design telemetry",
            studyStyle = "Visual Flowcharts & Interactive Reviews",
            availabilityText = "Afternoons & Evenings",
            sharedCourses = listOf("#eng-201", "#eng-202"),
            photoResName = "student_eliza",
            bio = "Product designer and researcher. I love breaking down complex user telemetry data into intuitive systems.",
            karmaScore = 620
        ),
        CourseMate(
            id = "mate_clin",
            name = "Clin",
            ageOrYear = "22",
            major = "Autonomous Robotics & Mechatronics",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = false,
            helperBadgeText = null,
            studyGoal = "Exam prep on ROS2 kinematics & path planning algorithms",
            studyStyle = "Code & Simulation Sprints",
            availabilityText = "Night Owl Sprints",
            sharedCourses = listOf("#eng-201", "#me-340"),
            photoResName = "student_clin",
            bio = "Maker and roboticist. Working on autonomous campus delivery drones.",
            karmaScore = 310
        ),
        CourseMate(
            id = "mate_junebog",
            name = "June",
            ageOrYear = "23",
            major = "Distributed Systems & Telemetry",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = true,
            helperBadgeText = "Lead Campus Peer Mentor",
            studyGoal = "Benchmarking telemetry pipelines and concurrency locks",
            studyStyle = "Theory & Whiteboard Sessions",
            availabilityText = "Mornings & Weekends",
            sharedCourses = listOf("#eng-201", "#se-305"),
            photoResName = "student_june",
            bio = "Passionate about high-throughput distributed message queues and helping classmates debug concurrency deadlocks.",
            karmaScore = 920
        ),
        CourseMate(
            id = "mate_andrew",
            name = "Andrew",
            ageOrYear = "22",
            major = "Mechanical & Aerospace Systems",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = false,
            helperBadgeText = null,
            studyGoal = "Finite element analysis review for midterm prep",
            studyStyle = "Practice Exams & Problem Solving",
            availabilityText = "Tuesday & Thursday Evenings",
            sharedCourses = listOf("#eng-201", "#me-210"),
            photoResName = "student_andrew",
            bio = "Aerospace enthusiast, CAD designer, and always down for coffee and physics derivations.",
            karmaScore = 280
        )
    )

    private val allEngineeringCourses = listOf(
        CareerCourse(
            id = "ux_design_2",
            title = "UX Design: 2 Analyzing User Data",
            courseCode = "#eng-201",
            thumbnailResName = "course_thumb_1",
            faculty = "Faculty of Engineering & Technology",
            department = "HCI & Design Systems",
            enrolledMatesCount = 18,
            enrolledMates = sampleCourseMates,
            hasActiveHelper = true,
            activeDiscussionSnippet = "Session on usability telemetry metrics uploaded."
        ),
        CareerCourse(
            id = "ux_foundations",
            title = "UX Foundations: Making the Case for Usability Testing",
            courseCode = "#eng-202",
            thumbnailResName = "course_thumb_2",
            faculty = "Faculty of Engineering & Technology",
            department = "HCI & Design Systems",
            enrolledMatesCount = 12,
            enrolledMates = sampleCourseMates.take(3),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Sample stakeholder presentation templates shared."
        ),
        CareerCourse(
            id = "interaction_design_flow",
            title = "Interaction Design: Flow",
            courseCode = "#eng-203",
            thumbnailResName = "course_thumb_3",
            faculty = "Faculty of Engineering & Technology",
            department = "HCI & Design Systems",
            enrolledMatesCount = 15,
            enrolledMates = sampleCourseMates.drop(1),
            hasActiveHelper = true,
            activeDiscussionSnippet = "State machine flow diagrams for mobile apps."
        ),
        CareerCourse(
            id = "embedded_microcontrollers",
            title = "Embedded Microcontrollers & IoT Architecture",
            courseCode = "#ee-310",
            thumbnailResName = "course_thumb_4",
            faculty = "Faculty of Engineering & Technology",
            department = "Electrical & Embedded",
            enrolledMatesCount = 24,
            enrolledMates = listOf(sampleCourseMates[0], sampleCourseMates[3]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Oscilloscope lab slots available between 2 PM and 5 PM."
        ),
        CareerCourse(
            id = "robotics_kinematics",
            title = "Autonomous Robotics & Kinematics Control",
            courseCode = "#me-340",
            thumbnailResName = "course_thumb_5",
            faculty = "Faculty of Engineering & Technology",
            department = "Mechatronics & Robotics",
            enrolledMatesCount = 16,
            enrolledMates = listOf(sampleCourseMates[2], sampleCourseMates[4]),
            hasActiveHelper = false,
            activeDiscussionSnippet = "ROS2 navigation stack configuration notes."
        ),
        CareerCourse(
            id = "distributed_cloud_telemetry",
            title = "Distributed Systems & Cloud Telemetry",
            courseCode = "#se-305",
            thumbnailResName = "course_thumb_6",
            faculty = "Faculty of Engineering & Technology",
            department = "Software & Systems",
            enrolledMatesCount = 20,
            enrolledMates = listOf(sampleCourseMates[3], sampleCourseMates[0]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Telemetry pipeline benchmark comparison charts."
        ),
        CareerCourse(
            id = "signal_processing",
            title = "Signal Processing & Fourier Analytics",
            courseCode = "#ee-220",
            thumbnailResName = "course_thumb_1",
            faculty = "Faculty of Engineering & Technology",
            department = "Electrical & Embedded",
            enrolledMatesCount = 11,
            enrolledMates = listOf(sampleCourseMates[0], sampleCourseMates[1]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Fast Fourier Transform implementation in C++."
        ),
        CareerCourse(
            id = "renewable_energy_grids",
            title = "Renewable Energy Grids & Power Electronics",
            courseCode = "#ee-410",
            thumbnailResName = "course_thumb_2",
            faculty = "Faculty of Engineering & Technology",
            department = "Electrical & Embedded",
            enrolledMatesCount = 9,
            enrolledMates = listOf(sampleCourseMates[0]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Inverter phase sync simulation files uploaded."
        ),
        CareerCourse(
            id = "fluid_dynamics_fea",
            title = "Fluid Dynamics & Finite Element Analysis",
            courseCode = "#me-210",
            thumbnailResName = "course_thumb_3",
            faculty = "Faculty of Engineering & Technology",
            department = "Mechanical",
            enrolledMatesCount = 14,
            enrolledMates = listOf(sampleCourseMates[4], sampleCourseMates[2]),
            hasActiveHelper = false,
            activeDiscussionSnippet = "Boundary condition setup guide for ANSYS Fluent."
        )
    )

    private val companiesList = listOf(
        InDemandCompany(
            id = "data_annotation",
            name = "DataAnnotation",
            jobPostsCountText = "510 job posts",
            demandMetricText = "\u25CF 0.0%",
            isPositiveTrend = false,
            logoResName = "company_da",
            initials = "DA"
        ),
        InDemandCompany(
            id = "google_eng",
            name = "Google Engineering",
            jobPostsCountText = "342 job posts",
            demandMetricText = "\u25CF +14.2%",
            isPositiveTrend = true,
            initials = "G"
        ),
        InDemandCompany(
            id = "tesla_energy",
            name = "Tesla Energy & Robotics",
            jobPostsCountText = "189 job posts",
            demandMetricText = "\u25CF +8.5%",
            isPositiveTrend = true,
            initials = "T"
        ),
        InDemandCompany(
            id = "safaricom_labs",
            name = "Safaricom Tech Labs",
            jobPostsCountText = "126 job posts",
            demandMetricText = "\u25CF +5.1%",
            isPositiveTrend = true,
            initials = "S"
        )
    )

    init {
        // Seed default chat messages matching the reference screenshot and community conversations
        val uncivMusicMessages = listOf(
            ChannelMessage(
                id = "msg_1",
                authorName = "RobLoach",
                authorColorHex = 0xFF23A55A,
                authorAvatarRes = "server_avatar2",
                timestamp = "03/11/2026 3:48 PM",
                text = "https://github.com/Kurwizimi/Civ-VII-Music-Pack",
                linkUrl = "https://github.com/Kurwizimi/Civ-VII-Music-Pack",
                embed = ChannelMessageEmbed(
                    provider = "GitHub",
                    title = "GitHub - Kurwizimi/Civ-VII-Music-Pack: This mod adds the Civ themes...",
                    description = "This mod adds the Civ themes from Civ VII and puts them into UnCiv. Recommended to play with the 'Civ VII Nations' mod. - Kurwizimi/Civ-VII-Music-Pack",
                    subCardTitle = "Kurwizimi/Civ-VII-Music-Pack",
                    subCardDescription = "This mod adds the Civ themes from Civ VII and puts them into UnCiv. Recommended to play with the 'Civ...",
                    contributorsCount = 2,
                    issuesCount = 0,
                    starsCount = 2,
                    forksCount = 1
                ),
                reactionCount = 1
            ),
            ChannelMessage(
                id = "msg_2",
                authorName = "Deleted User",
                authorColorHex = 0xFF1E1F22,
                authorAvatarRes = "avatar_deleted_user",
                timestamp = "03/11/2026 8:09 PM",
                text = "I really like Civ V music pack."
            ),
            ChannelMessage(
                id = "msg_div_1",
                authorName = "",
                timestamp = "",
                text = "",
                isDateDivider = true,
                dateDividerText = "April 30, 2026"
            ),
            ChannelMessage(
                id = "msg_3",
                authorName = "Ath",
                authorColorHex = 0xFF84CC16,
                authorBadge = "TDS",
                authorAvatarRes = "avatar_ath",
                timestamp = "04/30/2026 4:30 PM",
                text = "The less i know the better"
            ),
            ChannelMessage(
                id = "msg_4",
                authorName = "Deleted User",
                authorColorHex = 0xFF1E1F22,
                authorAvatarRes = "avatar_deleted_user",
                timestamp = "04/30/2026 5:55 PM",
                text = "How so?"
            ),
            ChannelMessage(
                id = "msg_5",
                authorName = "ReallyBasicName the ...",
                authorColorHex = 0xFF0284C7,
                authorAvatarRes = "server_dragon",
                timestamp = "04/30/2026 6:00 PM",
                replyTo = "Deleted User How so?",
                text = "It's a song by Tame Impala."
            ),
            ChannelMessage(
                id = "msg_6",
                authorName = "Ath",
                authorColorHex = 0xFF84CC16,
                authorBadge = "TDS",
                authorAvatarRes = "avatar_ath",
                timestamp = "04/30/2026 6:33 PM",
                text = "Let it happen\n\nMan in the mirror\n\nDracula\n\nBeat it\n\nShiawasenomonosashi"
            )
        )

        _channelMessages.value = mapOf(
            "ch_unciv_music" to uncivMusicMessages,
            "ch_unciv_role" to listOf(
                ChannelMessage(
                    id = "msg_role_1",
                    authorName = "Admin",
                    authorColorHex = 0xFF23A55A,
                    authorAvatarRes = "server_owl",
                    timestamp = "Yesterday at 2:00 PM",
                    text = "Welcome to Unciv community! Assign your mod and developer roles here."
                )
            ),
            "ch_unciv_pollchat" to listOf(
                ChannelMessage(
                    id = "msg_poll_1",
                    authorName = "Civ Modder",
                    authorColorHex = 0xFF0284C7,
                    authorAvatarRes = "server_avatar2",
                    timestamp = "Today at 9:00 AM",
                    text = "Poll results for sound effects pack are now open!"
                )
            ),
            "ch_redis_chat" to listOf(
                ChannelMessage(
                    id = "msg_rc_1",
                    authorName = "Lincoln Paul",
                    authorColorHex = 0xFF003B68,
                    authorAvatarRes = "avatar_lincoln",
                    timestamp = "Today at 10:15 AM",
                    text = "Redis Pub/Sub real-time socket stream integrated with campus cluster."
                ),
                ChannelMessage(
                    id = "msg_rc_2",
                    authorName = "Codesistency Admin",
                    authorColorHex = 0xFF23A55A,
                    authorAvatarRes = "server_codesistency",
                    timestamp = "Today at 10:18 AM",
                    text = "Telemetry benchmark shows sub-10ms response latencies across nodes."
                )
            ),
            "ch_mern_github" to listOf(
                ChannelMessage(
                    id = "msg_mg_1",
                    authorName = "Lincoln Paul",
                    authorColorHex = 0xFF003B68,
                    authorAvatarRes = "avatar_lincoln",
                    timestamp = "Today at 11:00 AM",
                    text = "Reviewing the pull request for GraphQL cursor pagination."
                )
            )
        )
        loadDefaultRules()
        seedDefaultServerMembers()
        if (serverRepository != null) {
            viewModelScope.launch {
                serverRepository.getServers().collect { dbServers ->
                    if (dbServers.isNotEmpty()) {
                        val mapped = dbServers.map { srv ->
                            val existing = _serversList.value.find { it.id == srv.id }
                            ServerItem(
                                id = srv.id,
                                name = srv.name,
                                memberCountText = "${srv.memberCount} Members",
                                categoryText = srv.category,
                                isVerified = srv.isVerified,
                                iconResName = srv.avatarResName,
                                bannerResName = srv.bannerResName,
                                channels = existing?.channels.orEmpty().ifEmpty {
                                    listOf(
                                        ServerChannelItem(id = "ch_${srv.id}_rules", name = "rules", channelType = "rules", categoryName = "WELCOME"),
                                        ServerChannelItem(id = "ch_${srv.id}_announcements", name = "announcements", channelType = "announcements", categoryName = "WELCOME"),
                                        ServerChannelItem(id = "ch_${srv.id}_general", name = "general", channelType = "text", categoryName = "TEXT CHANNELS")
                                    )
                                }
                            )
                        }
                        val existingIds = mapped.map { it.id }.toSet()
                        val remaining = _serversList.value.filter { it.id !in existingIds }
                        _serversList.value = (mapped + remaining).distinctBy { it.id }
                    }
                }
            }
            viewModelScope.launch {
                _selectedServerId.collect { serverId ->
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
                                    srv.copy(channels = mappedChannels)
                                } else srv
                            }
                        }
                    }
                }
            }
            viewModelScope.launch {
                _selectedServerId.collect { serverId ->
                    serverRepository.getRulesForServer(serverId).collect { rules ->
                        if (rules.isNotEmpty()) {
                            _currentServerRules.value = rules
                        }
                    }
                }
            }
        }
    }

    private fun loadDefaultRules() {
        _currentServerRules.value = listOf(
            ServerRule(
                id = "rule_default_1",
                serverId = _selectedServerId.value,
                ruleIndex = 1,
                title = "Respect Everyone",
                description = "Treat all campus engineers and contributors with respect. Harassment, hateful conduct, or derogatory remarks will not be tolerated."
            ),
            ServerRule(
                id = "rule_default_2",
                serverId = _selectedServerId.value,
                ruleIndex = 2,
                title = "Keep Content Relevant",
                description = "Post topics in designated channels. Keep academic questions in study rooms and announcements in broadcast feeds."
            ),
            ServerRule(
                id = "rule_default_3",
                serverId = _selectedServerId.value,
                ruleIndex = 3,
                title = "No Unsolicited Advertising / Spam",
                description = "Commercial solicitations, DM spam, or referral link dumps without moderator consent will lead to immediate timeouts."
            ),
            ServerRule(
                id = "rule_default_4",
                serverId = _selectedServerId.value,
                ruleIndex = 4,
                title = "Academic & Code Integrity",
                description = "Do not share unauthorized exam solutions or malicious exploits. Focus on peer learning."
            ),
            ServerRule(
                id = "rule_default_5",
                serverId = _selectedServerId.value,
                ruleIndex = 5,
                title = "Follow Moderation Guidance",
                description = "Direct issues to server admins or moderators via DM."
            )
        )
    }

    private fun seedDefaultServerMembers() {
        val initialProfiles = mapOf(
            "srv_codesistency" to PerServerProfile(
                serverId = "srv_codesistency",
                nickname = "Lincoln Paul",
                tag = "DEV",
                tagColorHex = 0xFF5865F2,
                role = "Server Owner"
            ),
            "srv_owl" to PerServerProfile(
                serverId = "srv_owl",
                nickname = "Lincoln (Modder)",
                tag = "MOD",
                tagColorHex = 0xFF23A55A,
                role = "Mod Contributor"
            )
        )
        _perServerProfiles.value = initialProfiles

        val codesistencyMembers = listOf(
            ServerMemberItem(
                id = "mem_lincoln",
                serverId = "srv_codesistency",
                name = "Lincoln Paul",
                handle = "@lincolnpaul",
                avatarResName = "avatar_lincoln",
                serverNickname = "Lincoln Paul",
                serverTag = "DEV",
                serverTagColorHex = 0xFF5865F2,
                role = ServerMemberRole("Server Owner", ServerRoleType.OWNER, 0xFFEAB308, isOwner = true),
                isOnline = true,
                statusText = "Building Campus Network",
                joinedDate = "Joined Sep 2026",
                bio = "Lead Developer & Campus Patriarch Ambassador. Building next-gen campus workspace.",
                friendStatus = FriendRequestStatus.FRIENDS
            ),
            ServerMemberItem(
                id = "mem_robloach",
                serverId = "srv_codesistency",
                name = "RobLoach",
                handle = "@robloach",
                avatarResName = "server_avatar2",
                serverTag = "MOD",
                serverTagColorHex = 0xFF23A55A,
                role = ServerMemberRole("Audio Lead & Moderator", ServerRoleType.MODERATOR, 0xFF23A55A),
                isOnline = true,
                statusText = "Composing soundtrack",
                joinedDate = "Joined Oct 2026",
                bio = "Game soundtrack sound designer & modular synth enthusiast.",
                friendStatus = FriendRequestStatus.NONE,
                channels = listOf("ch_unciv_music", "game-music", "poll-chat", "redis-chat-app")
            ),
            ServerMemberItem(
                id = "mem_ath",
                serverId = "srv_codesistency",
                name = "Ath",
                handle = "@ath_sound",
                avatarResName = "avatar_ath",
                serverTag = "TDS",
                serverTagColorHex = 0xFFA855F7,
                role = ServerMemberRole("Soundtrack Contributor", ServerRoleType.CONTRIBUTOR, 0xFFA855F7),
                isOnline = true,
                statusText = "Working on 8-bit beats",
                joinedDate = "Joined Oct 2026",
                bio = "Electronic beats & orchestral compositions for campus indie games.",
                friendStatus = FriendRequestStatus.NONE,
                channels = listOf("ch_unciv_music", "game-music", "poll-chat")
            ),
            ServerMemberItem(
                id = "mem_sarah",
                serverId = "srv_codesistency",
                name = "Sarah Nduta",
                handle = "@sarah_n",
                avatarResName = "avatar_ath",
                serverTag = "CAMP",
                serverTagColorHex = 0xFF5865F2,
                role = ServerMemberRole("Campus Peer", ServerRoleType.MEMBER, 0xFF5865F2),
                isOnline = true,
                joinedDate = "Joined Oct 2026",
                bio = "Research Assistant & Peer Escrow Mentor.",
                friendStatus = FriendRequestStatus.REQUEST_SENT
            ),
            ServerMemberItem(
                id = "mem_faith",
                serverId = "srv_codesistency",
                name = "Faith Wangari",
                handle = "@faith_w",
                avatarResName = "server_eureka",
                serverTag = "TECH",
                serverTagColorHex = 0xFFEC4899,
                role = ServerMemberRole("CS Sophomore", ServerRoleType.MEMBER, 0xFFEC4899),
                isOnline = true,
                joinedDate = "Joined Oct 2026",
                bio = "Distributed systems student & active lab presenter.",
                friendStatus = FriendRequestStatus.NONE
            ),
            ServerMemberItem(
                id = "mem_brian",
                serverId = "srv_codesistency",
                name = "Brian Ochieng",
                handle = "@brian_och",
                avatarResName = "server_dragon",
                serverTag = "ENG",
                serverTagColorHex = 0xFF0284C7,
                role = ServerMemberRole("Mechanical Peer", ServerRoleType.MEMBER, 0xFF0284C7),
                isOnline = false,
                joinedDate = "Joined Oct 2026",
                bio = "Hardware enthusiast & calculator swap coordinator.",
                friendStatus = FriendRequestStatus.NONE
            ),
            ServerMemberItem(
                id = "mem_jane",
                serverId = "srv_codesistency",
                name = "Jane Doe",
                handle = "@janedoe",
                avatarResName = "server_curly",
                serverTag = "STUDY",
                serverTagColorHex = 0xFF5C6470,
                role = ServerMemberRole("Student Peer", ServerRoleType.MEMBER, 0xFF5C6470),
                isOnline = false,
                joinedDate = "Joined Oct 2026",
                bio = "Study group organizer.",
                friendStatus = FriendRequestStatus.NONE
            ),
            ServerMemberItem(
                id = "mem_deleted",
                serverId = "srv_codesistency",
                name = "Deleted User",
                handle = "@deleted_user",
                avatarResName = "avatar_deleted_user",
                role = ServerMemberRole("Former Member", ServerRoleType.MEMBER, 0xFF80848E),
                isOnline = false,
                joinedDate = "Joined Aug 2026",
                bio = "",
                friendStatus = FriendRequestStatus.NONE
            )
        )

        val owlMembers = codesistencyMembers.map {
            it.copy(serverId = "srv_owl", id = "${it.id}_owl")
        }

        _serverMembers.value = mapOf(
            "srv_codesistency" to codesistencyMembers,
            "srv_owl" to owlMembers,
            "srv_basi" to codesistencyMembers.map { it.copy(serverId = "srv_basi", id = "${it.id}_basi") }
        )
    }

    val uiState: StateFlow<ChannelsUiState> = combine(
        combine(
            combine(_viewMode, _selectedServerId, _selectedChannelId) { vm, sId, cId -> Triple(vm, sId, cId) },
            _isRailCollapsed,
            _serverSearchQuery,
            _isServerProfileDrawerOpen,
            _isAddServerSelected
        ) { t, railCollapsed, sQuery, isProfileDrawerOpen, isAddServerSelected ->
            ServerStateSlice1(t.first, t.second, t.third, railCollapsed, sQuery, isProfileDrawerOpen, isAddServerSelected)
        },
        combine(
            _activeChatChannel,
            _isMediaDrawerOpen,
            _activeMediaTab,
            _channelMessages,
            _channelProfileTab
        ) { cChan, drawerOpen, mTab, cMsgs, cpTab ->
            ServerStateSlice2(cChan, drawerOpen, mTab, cMsgs, cpTab)
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
                _targetChannelCategory
            ) { chOpen, rulesOpen, inviteOpen, cat ->
                ServerManagementFlags(chOpen, rulesOpen, inviteOpen, cat)
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

        // Resolve current server & channels
        val currentServer = currentServers.find { it.id == slice1.selectedServerId }
            ?: currentServers.firstOrNull() ?: initialMasterServersList.first()

        val filteredChannels = currentServer.channels.filter { channel ->
            slice1.serverSearchQuery.isBlank() ||
                    channel.name.contains(slice1.serverSearchQuery, ignoreCase = true)
        }.map { channel ->
            channel.copy(isSelected = (channel.id == slice1.selectedChannelId))
        }

        val enrichedServer = currentServer.copy(channels = filteredChannels)

        // Resolve active chat channel and its messages
        val activeChannel = slice2.activeChatChannel ?: currentServer.channels.find { it.id == slice1.selectedChannelId }
        val currentMessages = activeChannel?.let { slice2.channelMessages[it.id] } ?: emptyList()

        ChannelsUiState(
            viewMode = slice1.viewMode,
            servers = currentServers,
            selectedServerId = slice1.selectedServerId,
            selectedServer = enrichedServer,
            selectedChannelId = slice1.selectedChannelId,
            isRailCollapsed = slice1.isRailCollapsed,
            activeUserName = "Lincoln Paul",
            activeUserStatus = "Online",
            activeUserUnreadCount = 2,
            activeChatServer = currentServer,
            activeChatChannel = activeChannel,
            chatMessages = currentMessages,
            isMediaDrawerOpen = slice2.isMediaDrawerOpen,
            activeMediaTab = slice2.activeMediaTab,
            activeChannelProfileTab = slice2.channelProfileTab,
            isServerProfileDrawerOpen = slice1.isServerProfileDrawerOpen,
            isAddServerSelected = slice1.isAddServerSelected,
            isCreateChannelSheetOpen = mgmtSlice.isCreateChannelSheetOpen,
            isRulesSheetOpen = mgmtSlice.isRulesSheetOpen,
            isServerInviteSheetOpen = mgmtSlice.isServerInviteSheetOpen,
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
            selectedServer = initialMasterServersList.first()
        )
    )

    fun onEvent(event: ChannelsEvent) {
        when (event) {
            is ChannelsEvent.SelectServer -> {
                _isAddServerSelected.value = false
                _selectedServerId.value = event.serverId
                val server = _serversList.value.find { it.id == event.serverId }
                    ?: initialMasterServersList.find { it.id == event.serverId }
                server?.channels?.firstOrNull()?.let {
                    _selectedChannelId.value = it.id
                }
            }
            is ChannelsEvent.SelectChannel -> {
                _selectedChannelId.value = event.channelId
            }
            is ChannelsEvent.OpenChannelChat -> {
                _activeChatChannel.value = event.channel
                _selectedChannelId.value = event.channel.id
                _isMediaDrawerOpen.value = false
                _viewMode.value = ChannelViewMode.CHANNEL_CHAT
            }
            is ChannelsEvent.CloseChannelChat -> {
                _viewMode.value = ChannelViewMode.CHANNELS_HUB
                _isMediaDrawerOpen.value = false
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
                val currentList = _channelMessages.value[activeChan.id] ?: emptyList()
                val newMessage = ChannelMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    authorName = "Lincoln Paul",
                    authorColorHex = 0xFF003B68,
                    authorAvatarRes = "avatar_lincoln",
                    timestamp = "Today at " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                    text = event.text,
                    gifUrl = event.gifUrl,
                    imageUris = event.imageUris
                )
                _channelMessages.value = _channelMessages.value + (activeChan.id to (currentList + newMessage))
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
                val newServer = ServerItem(
                    id = newId,
                    name = event.name,
                    memberCountText = "1 Member",
                    categoryText = event.category,
                    isVerified = false,
                    iconResName = cleanAvatar,
                    bannerResName = event.bannerResName,
                    badgeCount = 0,
                    hasUnread = false,
                    channels = defaultChannels
                )
                _serversList.value = (_serversList.value + newServer).distinctBy { it.id }
                _selectedServerId.value = newId
                _selectedChannelId.value = "ch_${newId}_general"
                _feedbackMessage.value = "Created server: ${event.name}"
                val newMember = ServerMemberItem(
                    id = "mem_lincoln_$newId",
                    serverId = newId,
                    name = "Lincoln Paul",
                    handle = "@lincolnpaul",
                    avatarResName = "avatar_lincoln",
                    serverNickname = "Lincoln Paul",
                    serverTag = "OWNER",
                    serverTagColorHex = 0xFFEAB308,
                    role = ServerMemberRole("Server Owner", ServerRoleType.OWNER, 0xFFEAB308, isOwner = true),
                    isOnline = true,
                    joinedDate = "Joined Just Now",
                    bio = "Server Founder & Owner.",
                    friendStatus = FriendRequestStatus.FRIENDS
                )
                _serverMembers.value = _serverMembers.value + (newId to listOf(newMember))
                _perServerProfiles.value = _perServerProfiles.value + (newId to PerServerProfile(newId, "Lincoln Paul", "OWNER", 0xFFEAB308, "Server Owner"))

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        runCatching {
                            repo.createServer(
                                name = event.name,
                                category = event.category,
                                description = event.description,
                                avatarResName = cleanAvatar,
                                bannerResName = event.bannerResName,
                                customServerId = newId
                            )
                        }
                    }
                }
            }
            is ChannelsEvent.OpenCreateChannelSheet -> {
                _targetChannelCategory.value = event.categoryName
                _isCreateChannelSheetOpen.value = true
            }
            is ChannelsEvent.CloseCreateChannelSheet -> {
                _isCreateChannelSheetOpen.value = false
            }
            is ChannelsEvent.CreateChannel -> {
                _isCreateChannelSheetOpen.value = false
                val cleanSlug = event.name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-").ifEmpty { "channel" }
                val uniqueSuffix = System.currentTimeMillis().toString().takeLast(4)
                val channelId = "ch_${event.serverId}_${cleanSlug.take(14)}_$uniqueSuffix"
                val newChannelItem = ServerChannelItem(
                    id = channelId,
                    name = cleanSlug,
                    channelType = event.channelType,
                    categoryName = event.categoryName,
                    isUnread = false
                )
                _serversList.value = _serversList.value.map { server ->
                    if (server.id == event.serverId) {
                        server.copy(channels = (server.channels + newChannelItem).distinctBy { it.id })
                    } else server
                }
                _selectedChannelId.value = channelId
                _feedbackMessage.value = "Created #$cleanSlug"

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        runCatching {
                            val cType = when (event.channelType.lowercase()) {
                                "announcements" -> ServerChannelType.ANNOUNCEMENT
                                "rules" -> ServerChannelType.RULES
                                "voice" -> ServerChannelType.VOICE
                                else -> ServerChannelType.TEXT
                            }
                            repo.createChannel(
                                serverId = event.serverId,
                                name = event.name,
                                categoryName = event.categoryName,
                                channelType = cType,
                                topic = event.topic
                            )
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
                        runCatching {
                            repo.createRule(event.serverId, event.title, event.description)
                        }
                    }
                }
            }
            is ChannelsEvent.DeleteServerRule -> {
                _currentServerRules.value = _currentServerRules.value.filter { it.id != event.ruleId }
                _feedbackMessage.value = "Rule removed"

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        runCatching {
                            repo.deleteRule(event.ruleId)
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
                _feedbackMessage.value = "Joined server via invite link!"

                serverRepository?.let { repo ->
                    viewModelScope.launch {
                        val result = repo.joinServerByInvite(event.inviteCode)
                        result.onSuccess { srv ->
                            _selectedServerId.value = srv.id
                            _feedbackMessage.value = "Joined ${srv.name}!"
                        }.onFailure { err ->
                            _feedbackMessage.value = err.message ?: "Failed to join server"
                        }
                    }
                }
            }
            is ChannelsEvent.PreviewCommunityServer -> {
                _isAddServerSelected.value = false
                val existing = _serversList.value.find { it.name.equals(event.serverName, ignoreCase = true) }
                if (existing != null) {
                    _selectedServerId.value = existing.id
                    existing.channels.firstOrNull()?.let {
                        _selectedChannelId.value = it.id
                    }
                } else if (event.serverName.equals("Clash of Clans", ignoreCase = true)) {
                    val previewCoc = ServerItem(
                        id = "srv_clash_preview",
                        name = "Clash of Clans",
                        memberCountText = "458,484 Members",
                        categoryText = "Community",
                        isVerified = true,
                        badgeType = "verified",
                        iconResName = "server_dragon",
                        badgeCount = 0,
                        hasUnread = false,
                        bannerResName = "server_banner_clash",
                        isPreview = true,
                        hasServerGuide = true,
                        channels = listOf(
                            ServerChannelItem(
                                id = "ch_coc_rules",
                                name = "server-rules",
                                channelType = "rules",
                                categoryName = "Info",
                                isSelected = true
                            ),
                            ServerChannelItem(
                                id = "ch_coc_info",
                                name = "server-info",
                                channelType = "text",
                                categoryName = "Info"
                            ),
                            ServerChannelItem(
                                id = "ch_coc_faq",
                                name = "server-support-faq",
                                channelType = "text",
                                categoryName = "Info"
                            ),
                            ServerChannelItem(
                                id = "ch_coc_roles",
                                name = "self-roles",
                                channelType = "text",
                                categoryName = "Info"
                            ),
                            ServerChannelItem(
                                id = "ch_coc_boost",
                                name = "boosting-info",
                                channelType = "text",
                                categoryName = "Info"
                            ),
                            ServerChannelItem(
                                id = "ch_coc_announcements",
                                name = "announcements",
                                channelType = "announcements",
                                categoryName = "Announcements"
                            )
                        )
                    )
                    _serversList.value = _serversList.value + previewCoc
                    _selectedServerId.value = previewCoc.id
                    _selectedChannelId.value = "ch_coc_rules"
                } else {
                    val newId = "srv_comm_${System.currentTimeMillis()}"
                    val iconRes = when (event.serverName) {
                        "Heartopia" -> "server_curly"
                        "Campus Tech & AI Guild" -> "server_ai_brain"
                        else -> "server_gradient"
                    }
                    val communityServer = ServerItem(
                        id = newId,
                        name = event.serverName,
                        memberCountText = "Community Hub",
                        categoryText = "Gaming & Community",
                        isVerified = true,
                        badgeType = "verified",
                        iconResName = iconRes,
                        bannerResName = if (event.serverName == "Heartopia") "server_banner_clash" else null,
                        isPreview = true,
                        hasServerGuide = true,
                        channels = listOf(
                            ServerChannelItem(
                                id = "ch_${newId}_rules",
                                name = "server-rules",
                                channelType = "rules",
                                categoryName = "Info",
                                isSelected = true
                            ),
                            ServerChannelItem(
                                id = "ch_${newId}_announcements",
                                name = "announcements",
                                channelType = "announcements",
                                categoryName = "Announcements"
                            ),
                            ServerChannelItem(
                                id = "ch_${newId}_general",
                                name = "general-chat",
                                channelType = "text",
                                categoryName = "General"
                            )
                        )
                    )
                    _serversList.value = _serversList.value + communityServer
                    _selectedServerId.value = newId
                    _selectedChannelId.value = "ch_${newId}_rules"
                }
            }
            is ChannelsEvent.ExitServerPreview -> {
                _isAddServerSelected.value = true
            }
            is ChannelsEvent.JoinCurrentPreviewServer -> {
                val currentId = _selectedServerId.value
                _serversList.value = _serversList.value.map { srv ->
                    if (srv.id == currentId) {
                        srv.copy(isPreview = false)
                    } else {
                        srv
                    }
                }
                _feedbackMessage.value = "Joined server!"
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
                        _viewMode.value = ChannelViewMode.CHANNELS_HUB
                        _isMediaDrawerOpen.value = false
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
                var targetName = "Member"
                _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                    members.map { mem ->
                        if (mem.id == memberId) {
                            targetName = mem.serverNickname ?: mem.name
                            mem.copy(friendStatus = FriendRequestStatus.REQUEST_SENT)
                        } else mem
                    }
                }
                _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let {
                    if (it.id == memberId) it.copy(friendStatus = FriendRequestStatus.REQUEST_SENT) else it
                }
                _feedbackMessage.value = "Friend request sent to $targetName!"
            }

            is ChannelsEvent.CancelFriendRequest -> {
                val memberId = event.memberId
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
            }

            is ChannelsEvent.AcceptFriendRequest -> {
                val memberId = event.memberId
                var targetName = "Member"
                _serverMembers.value = _serverMembers.value.mapValues { (_, members) ->
                    members.map { mem ->
                        if (mem.id == memberId) {
                            targetName = mem.serverNickname ?: mem.name
                            mem.copy(friendStatus = FriendRequestStatus.FRIENDS)
                        } else mem
                    }
                }
                _selectedMemberForProfile.value = _selectedMemberForProfile.value?.let {
                    if (it.id == memberId) it.copy(friendStatus = FriendRequestStatus.FRIENDS) else it
                }
                _feedbackMessage.value = "You are now friends with $targetName!"
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
                            if (mem.name.contains("Lincoln", ignoreCase = true)) {
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

            is ChannelsEvent.OpenServerMembersSheet -> {
                _currentMemberFilterChannelId.value = event.channelId
                _isServerMembersSheetOpen.value = true
            }

            is ChannelsEvent.CloseServerMembersSheet -> {
                _isServerMembersSheetOpen.value = false
                _currentMemberFilterChannelId.value = null
            }
        }
    }
}

private data class ServerStateSlice1(
    val viewMode: ChannelViewMode,
    val selectedServerId: String,
    val selectedChannelId: String,
    val isRailCollapsed: Boolean,
    val serverSearchQuery: String,
    val isServerProfileDrawerOpen: Boolean = false,
    val isAddServerSelected: Boolean = false
)

private data class ServerStateSlice2(
    val activeChatChannel: ServerChannelItem?,
    val isMediaDrawerOpen: Boolean,
    val activeMediaTab: String,
    val channelMessages: Map<String, List<ChannelMessage>>,
    val channelProfileTab: String = "Members"
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
    val targetChannelCategory: String
)

private data class ServerManagementSlice(
    val isCreateChannelSheetOpen: Boolean,
    val isRulesSheetOpen: Boolean,
    val isServerInviteSheetOpen: Boolean,
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
