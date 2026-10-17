package com.campus.network.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campus.network.core.database.dao.AcademicChannelDao
import com.campus.network.core.database.dao.ChatMessageDao
import com.campus.network.core.database.dao.FlashMeetDao
import com.campus.network.core.database.dao.MarketItemDao
import com.campus.network.core.database.dao.MicroGigDao
import com.campus.network.core.database.dao.LiveStreamDao
import com.campus.network.core.database.dao.StreamerProfileDao
import com.campus.network.core.database.dao.StreamCommentDao
import com.campus.network.core.database.dao.ServerDao
import com.campus.network.core.database.dao.ServerChannelDao
import com.campus.network.core.database.dao.ServerRuleDao
import com.campus.network.core.database.dao.ServerMemberDao
import com.campus.network.core.database.dao.FriendDao
import com.campus.network.core.database.dao.ServerInviteDao
import com.campus.network.core.database.entity.AcademicChannelEntity
import com.campus.network.core.database.entity.ChatMessageEntity
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamerProfileEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.database.entity.ServerEntity
import com.campus.network.core.database.entity.ServerChannelEntity
import com.campus.network.core.database.entity.ServerRuleEntity
import com.campus.network.core.database.entity.ServerMemberEntity
import com.campus.network.core.database.entity.FriendEntity
import com.campus.network.core.database.entity.ServerInviteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FlashMeetEntity::class,
        AcademicChannelEntity::class,
        ChatMessageEntity::class,
        MarketItemEntity::class,
        MicroGigEntity::class,
        LiveStreamEntity::class,
        StreamerProfileEntity::class,
        StreamCommentEntity::class,
        ServerEntity::class,
        ServerChannelEntity::class,
        ServerRuleEntity::class,
        ServerMemberEntity::class,
        FriendEntity::class,
        ServerInviteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun flashMeetDao(): FlashMeetDao
    abstract fun academicChannelDao(): AcademicChannelDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun marketItemDao(): MarketItemDao
    abstract fun microGigDao(): MicroGigDao
    abstract fun liveStreamDao(): LiveStreamDao
    abstract fun streamerProfileDao(): StreamerProfileDao
    abstract fun streamCommentDao(): StreamCommentDao
    abstract fun serverDao(): ServerDao
    abstract fun serverChannelDao(): ServerChannelDao
    abstract fun serverRuleDao(): ServerRuleDao
    abstract fun serverMemberDao(): ServerMemberDao
    abstract fun friendDao(): FriendDao
    abstract fun serverInviteDao(): ServerInviteDao

    companion object {
        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getInstance(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campus_network.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate seed data asynchronously on Dispatchers.IO to ensure zero frame drops on first open
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialCache(getInstance(context))
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private suspend fun populateInitialCache(database: CampusDatabase) {
            val now = System.currentTimeMillis()

            // 1. Initial Flash Meets on Campus
            val sampleMeets = listOf(
                FlashMeetEntity(
                    id = "meet-1",
                    title = "Algorithmic Complexity Jam",
                    description = "Tackling Dynamic Programming sets before tomorrow's midterm. Bring your notebooks!",
                    categoryName = "STUDY_JAM",
                    latitude = -1.3090,
                    longitude = 36.8120,
                    locationName = "Turing Hall 3rd Floor Lounge",
                    creatorName = "Brian Ochieng",
                    creatorAvatar = null,
                    createdAtEpochMs = now - (15 * 60 * 1000),
                    expiresAtEpochMs = now + (45 * 60 * 1000),
                    attendeeCount = 5,
                    maxAttendees = 8
                ),
                FlashMeetEntity(
                    id = "meet-2",
                    title = "Solar Car Telemetry Sync",
                    description = "Debugging CAN-bus serial packet loss on prototype board #3.",
                    categoryName = "PROJECT_HACK",
                    latitude = -1.3105,
                    longitude = 36.8142,
                    locationName = "MakerSpace Lab A",
                    creatorName = "Wanjiku N.",
                    creatorAvatar = null,
                    createdAtEpochMs = now - (5 * 60 * 1000),
                    expiresAtEpochMs = now + (75 * 60 * 1000),
                    attendeeCount = 3,
                    maxAttendees = 6
                ),
                FlashMeetEntity(
                    id = "meet-3",
                    title = "Quick Coffee & Chapati Run",
                    description = "Heading down to Student Center cafeteria before the 11 AM lecture.",
                    categoryName = "FOOD_RUN",
                    latitude = -1.3082,
                    longitude = 36.8111,
                    locationName = "Student Center Atrium",
                    creatorName = "Kelvin Mutua",
                    creatorAvatar = null,
                    createdAtEpochMs = now - (2 * 60 * 1000),
                    expiresAtEpochMs = now + (20 * 60 * 1000),
                    attendeeCount = 2,
                    maxAttendees = 4
                )
            )
            database.flashMeetDao().insertMeets(sampleMeets)

            // 2. Initial Academic Channels
            val sampleChannels = listOf(
                AcademicChannelEntity(
                    id = "ch-cs201",
                    courseCode = "#cs-201",
                    courseTitle = "Data Structures & Algorithms",
                    faculty = "School of Computing & Informatics",
                    department = "Computer Science",
                    unreadCount = 4,
                    lastMessageSnippet = "Prof. Mwangi pushed the red-black tree test cases to git.",
                    lastMessageTime = "10:14 AM",
                    activeStudentsCount = 184,
                    isPinned = true
                ),
                AcademicChannelEntity(
                    id = "ch-cs305",
                    courseCode = "#cs-305",
                    courseTitle = "Operating Systems & Concurrency",
                    faculty = "School of Computing & Informatics",
                    department = "Computer Science",
                    unreadCount = 12,
                    lastMessageSnippet = "Can someone check my semaphore deadlock trace?",
                    lastMessageTime = "09:42 AM",
                    activeStudentsCount = 142,
                    isPinned = true
                ),
                AcademicChannelEntity(
                    id = "ch-se202",
                    courseCode = "#se-202",
                    courseTitle = "Software Architecture & Design Patterns",
                    faculty = "School of Computing & Informatics",
                    department = "Software Engineering",
                    unreadCount = 0,
                    lastMessageSnippet = "UDF and Clean Architecture slide deck uploaded.",
                    lastMessageTime = "Yesterday",
                    activeStudentsCount = 98,
                    isPinned = false
                ),
                AcademicChannelEntity(
                    id = "ch-ee310",
                    courseCode = "#ee-310",
                    courseTitle = "Embedded Microcontrollers",
                    faculty = "Faculty of Engineering & Technology",
                    department = "Electrical Engineering",
                    unreadCount = 2,
                    lastMessageSnippet = "Oscilloscope lab slots available between 2 PM and 5 PM.",
                    lastMessageTime = "08:30 AM",
                    activeStudentsCount = 76,
                    isPinned = false
                )
            )
            database.academicChannelDao().insertChannels(sampleChannels)

            // 3. Initial Market Items
            val sampleItems = listOf(
                MarketItemEntity(
                    id = "item-1",
                    title = "Engineering Drafting Compass & Geometry Set",
                    price = 450.0,
                    currency = "KES",
                    category = "Hardware",
                    condition = "Complete 8-Piece Set",
                    sellerName = "Student Mart Hub",
                    sellerRating = 4.9f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Hostel Tuck Shop",
                    imageUrl = null,
                    timestampMs = now - (15 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-2",
                    title = "Stewart Calculus Early Transcendentals (8th Ed)",
                    price = 1450.0,
                    currency = "KES",
                    category = "Textbooks",
                    condition = "Hardcover (Like New)",
                    sellerName = "Brian Omondi",
                    sellerRating = 4.8f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Library Quad",
                    imageUrl = null,
                    timestampMs = now - (30 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-3",
                    title = "Casio FX-991EX ClassWiz Calculator",
                    price = 2800.0,
                    currency = "KES",
                    category = "Electronics",
                    condition = "Original Solar + Battery",
                    sellerName = "Faith Chebet",
                    sellerRating = 4.9f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Hall 4 Hostels",
                    imageUrl = null,
                    timestampMs = now - (45 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-4",
                    title = "Logitech Pebble Wireless Bluetooth Mouse",
                    price = 1800.0,
                    currency = "KES",
                    category = "Electronics",
                    condition = "Mint Condition (Silent Click)",
                    sellerName = "Campus Tech Hub",
                    sellerRating = 5.0f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Student Center",
                    imageUrl = null,
                    timestampMs = now - (60 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-5",
                    title = "Anker 65W GaN USB-C Fast Charger",
                    price = 2400.0,
                    currency = "KES",
                    category = "Electronics",
                    condition = "2x USB-C + 1x USB-A",
                    sellerName = "Denis Kiprono",
                    sellerRating = 4.7f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Engineering Block",
                    imageUrl = null,
                    timestampMs = now - (90 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-6",
                    title = "Pure White Cotton Lab Coat + Safety Goggles",
                    price = 950.0,
                    currency = "KES",
                    category = "Lab & Gear",
                    condition = "Size L (Freshly Laundered)",
                    sellerName = "Wanjiku N.",
                    sellerRating = 4.9f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Chemistry Annex",
                    imageUrl = null,
                    timestampMs = now - (120 * 60 * 1000)
                )
            )
            database.marketItemDao().insertItems(sampleItems)

            // 4. Initial Micro Gigs
            val sampleGigs = listOf(
                MicroGigEntity(
                    id = "gig-1",
                    title = "LaTeX Formatting for B.Sc Final Year Proposal",
                    description = "Need someone familiar with IEEE bibtex template to format my 18-page thesis proposal draft cleanly.",
                    payoutAmount = 1500.0,
                    currency = "KES",
                    estimatedDuration = "2 hours",
                    posterName = "Sarah Nduta",
                    posterTrustScore = 98,
                    tag = "Academic Aid",
                    urgencyName = "FLASH_URGENT",
                    spotsAvailable = 1,
                    timestampMs = now - (30 * 60 * 1000)
                ),
                MicroGigEntity(
                    id = "gig-2",
                    title = "Urgent Delivery: Soldering Iron & Flux to MakerSpace",
                    description = "Left my Weller kit in Hostel Block B Room 214. Bring it to Lab A entrance.",
                    payoutAmount = 350.0,
                    currency = "KES",
                    estimatedDuration = "20 mins",
                    posterName = "Evans Kariuki",
                    posterTrustScore = 94,
                    tag = "Campus Errand",
                    urgencyName = "FLASH_URGENT",
                    spotsAvailable = 1,
                    timestampMs = now - (10 * 60 * 1000)
                ),
                MicroGigEntity(
                    id = "gig-3",
                    title = "Physics I Calculus Problem Set Tutor",
                    description = "Looking for a 3rd/4th year student to walk through 4 rotational inertia problems tonight at the Library.",
                    payoutAmount = 1000.0,
                    currency = "KES",
                    estimatedDuration = "1 hour",
                    posterName = "Joy Achieng",
                    posterTrustScore = 92,
                    tag = "Tutoring",
                    urgencyName = "HIGH",
                    spotsAvailable = 2,
                    timestampMs = now - (90 * 60 * 1000)
                )
            )
            database.microGigDao().insertGigs(sampleGigs)

            // 6. Initial Campus Live Streams
            val sampleStreams = listOf(
                LiveStreamEntity(
                    id = "founders",
                    broadcasterId = "student-sara",
                    broadcasterName = "Sara.HM",
                    broadcasterHandle = "sara.hm",
                    broadcasterAvatarRes = 0,
                    title = "Celebrating Student Founders Demo Day",
                    description = "Live pitch: Building peer solar charging stations across student hostels. Ask questions in comments!",
                    category = "Spotlight",
                    audience = "CAMPUS",
                    isLive = true,
                    viewerCount = 1250,
                    peakViewers = 1420,
                    likeCount = 342,
                    startedAtEpochMs = now - (35 * 60 * 1000)
                ),
                LiveStreamEntity(
                    id = "guild_debate",
                    broadcasterId = "guild-uon",
                    broadcasterName = "UoN.Guild",
                    broadcasterHandle = "uon.guild",
                    broadcasterAvatarRes = 0,
                    title = "Guild Presidential Debate 2026",
                    description = "Live from Taifa Hall Auditorium. Real-time candidate cross-examinations and student policy polling.",
                    category = "Campus Debate",
                    audience = "CAMPUS",
                    isLive = true,
                    viewerCount = 1840,
                    peakViewers = 2100,
                    likeCount = 580,
                    startedAtEpochMs = now - (50 * 60 * 1000)
                ),
                LiveStreamEntity(
                    id = "gdsc_hackathon",
                    broadcasterId = "gdsc-strath",
                    broadcasterName = "GDSC.Build",
                    broadcasterHandle = "gdsc.build",
                    broadcasterAvatarRes = 0,
                    title = "GDSC AI Hackathon: Final Pitches",
                    description = "Top 6 finalist student teams showcase live on-device Android AI models. Voting is now live!",
                    category = "Tech Sprint",
                    audience = "CAMPUS",
                    isLive = true,
                    viewerCount = 890,
                    peakViewers = 950,
                    likeCount = 210,
                    startedAtEpochMs = now - (20 * 60 * 1000)
                ),
                LiveStreamEntity(
                    id = "safetrade_quad",
                    broadcasterId = "safetrade-hub",
                    broadcasterName = "SafeTrade.Hub",
                    broadcasterHandle = "safetrade.hub",
                    broadcasterAvatarRes = 0,
                    title = "SafeTrade Quad Rendezvous & Market",
                    description = "Streaming live from the Student Guild Quad canopy. Hardware checkups, textbook drop-offs, and safe escrow payouts.",
                    category = "Spotlight",
                    audience = "CAMPUS",
                    isLive = true,
                    viewerCount = 620,
                    peakViewers = 710,
                    likeCount = 145,
                    startedAtEpochMs = now - (10 * 60 * 1000)
                )
            )
            database.liveStreamDao().insertStreams(sampleStreams)

            // 7. Initial Campus Servers
            val sampleServers = listOf(
                ServerEntity(
                    id = "srv_codesistency",
                    name = "Codesistency",
                    description = "Official campus open source engineering community & collaborative builders space.",
                    category = "Engineering & Open Source",
                    ownerId = "user_lincoln",
                    avatarResName = "server_codesistency",
                    bannerResName = "channel_banner_preview",
                    isVerified = true,
                    memberCount = 48,
                    inviteCode = "CODESISTENCY-2026",
                    createdAtEpochMs = now - (60L * 86400000L)
                ),
                ServerEntity(
                    id = "srv_gdsc_strath",
                    name = "GDSC Strathmore",
                    description = "Google Developer Student Club. Building on-device AI, cloud architectures and Android apps.",
                    category = "Developer Student Clubs",
                    ownerId = "user_robloach",
                    avatarResName = "server_avatar1",
                    bannerResName = null,
                    isVerified = true,
                    memberCount = 120,
                    inviteCode = "GDSC-STRATH-AI",
                    createdAtEpochMs = now - (30L * 86400000L)
                ),
                ServerEntity(
                    id = "srv_engineering_hub",
                    name = "Engineering Hub",
                    description = "Cross-faculty academic channel, lab coordination, and peer-to-peer tutoring.",
                    category = "Academic & Faculty",
                    ownerId = "user_lincoln",
                    avatarResName = "server_avatar3",
                    bannerResName = null,
                    isVerified = false,
                    memberCount = 85,
                    inviteCode = "ENG-UON-2026",
                    createdAtEpochMs = now - (15L * 86400000L)
                )
            )
            database.serverDao().insertServers(sampleServers)

            // 8. Initial Server Channels
            val sampleServerChannels = listOf(
                ServerChannelEntity(
                    id = "ch_codesistency_rules",
                    serverId = "srv_codesistency",
                    name = "rules",
                    categoryName = "WELCOME",
                    channelType = "RULES",
                    topic = "Community guidelines, code of conduct and server policies.",
                    unreadCount = 0,
                    isPinned = true,
                    orderIndex = 1
                ),
                ServerChannelEntity(
                    id = "ch_codesistency_announcements",
                    serverId = "srv_codesistency",
                    name = "announcements",
                    categoryName = "WELCOME",
                    channelType = "ANNOUNCEMENT",
                    topic = "Official server updates, workshop schedules, and hackathon alerts.",
                    unreadCount = 1,
                    isPinned = true,
                    orderIndex = 2
                ),
                ServerChannelEntity(
                    id = "ch_mern_github",
                    serverId = "srv_codesistency",
                    name = "mern-stack-github",
                    categoryName = "DEVELOPMENT",
                    channelType = "TEXT",
                    topic = "Full-stack Node.js, Express, React, and Git PR peer review sessions.",
                    unreadCount = 0,
                    isPinned = false,
                    orderIndex = 3
                ),
                ServerChannelEntity(
                    id = "ch_android_dev",
                    serverId = "srv_codesistency",
                    name = "android-dev-room",
                    categoryName = "DEVELOPMENT",
                    channelType = "TEXT",
                    topic = "Modern Android development with Jetpack Compose, Kotlin Coroutines and KMP.",
                    unreadCount = 3,
                    isPinned = false,
                    orderIndex = 4
                ),
                ServerChannelEntity(
                    id = "ch_general",
                    serverId = "srv_codesistency",
                    name = "general-chat",
                    categoryName = "COMMUNITY",
                    channelType = "TEXT",
                    topic = "Open lounge for campus builders, coffee breaks, and casual discussions.",
                    unreadCount = 0,
                    isPinned = false,
                    orderIndex = 5
                ),
                ServerChannelEntity(
                    id = "ch_gdsc_rules",
                    serverId = "srv_gdsc_strath",
                    name = "rules",
                    categoryName = "INFO",
                    channelType = "RULES",
                    topic = "GDSC Strathmore community code of conduct and workshop safety.",
                    unreadCount = 0,
                    isPinned = true,
                    orderIndex = 1
                ),
                ServerChannelEntity(
                    id = "ch_gdsc_announcements",
                    serverId = "srv_gdsc_strath",
                    name = "announcements",
                    categoryName = "INFO",
                    channelType = "ANNOUNCEMENT",
                    topic = "Google Cloud Study Jams & Flutter Festival announcements.",
                    unreadCount = 2,
                    isPinned = true,
                    orderIndex = 2
                ),
                ServerChannelEntity(
                    id = "ch_gdsc_ai_lab",
                    serverId = "srv_gdsc_strath",
                    name = "ai-ml-sprint",
                    categoryName = "TECH LABS",
                    channelType = "TEXT",
                    topic = "On-device Gemini Nano inference, LiteRT models, and model quantization.",
                    unreadCount = 0,
                    isPinned = false,
                    orderIndex = 3
                )
            )
            database.serverChannelDao().insertChannels(sampleServerChannels)

            // 9. Initial Server Rules
            val sampleRules = listOf(
                ServerRuleEntity(
                    id = "rule_codesistency_1",
                    serverId = "srv_codesistency",
                    ruleIndex = 1,
                    title = "Respect Everyone",
                    description = "Treat all campus engineers and contributors with respect. Harassment, hateful conduct, or derogatory remarks will not be tolerated."
                ),
                ServerRuleEntity(
                    id = "rule_codesistency_2",
                    serverId = "srv_codesistency",
                    ruleIndex = 2,
                    title = "Keep Content Relevant",
                    description = "Post topics in their designated channels. Code discussions belong in #mern-stack-github or #android-dev-room."
                ),
                ServerRuleEntity(
                    id = "rule_codesistency_3",
                    serverId = "srv_codesistency",
                    ruleIndex = 3,
                    title = "No Unsolicited Advertising / Spam",
                    description = "Commercial promotions, paid assignments, or referral link dumps without moderator consent will lead to timeouts."
                ),
                ServerRuleEntity(
                    id = "rule_codesistency_4",
                    serverId = "srv_codesistency",
                    ruleIndex = 4,
                    title = "Honour Academic & Code Integrity",
                    description = "Do not post exam questions or unauthorized assignment leaks. Focus on conceptual understanding and open collaboration."
                ),
                ServerRuleEntity(
                    id = "rule_codesistency_5",
                    serverId = "srv_codesistency",
                    ruleIndex = 5,
                    title = "Follow Moderation Guidance",
                    description = "Community moderators are here to keep discussions productive. Direct issues to server admins or moderators via DM."
                ),
                ServerRuleEntity(
                    id = "rule_gdsc_1",
                    serverId = "srv_gdsc_strath",
                    ruleIndex = 1,
                    title = "Be Welcoming & Inclusive",
                    description = "GDSC is an inclusive community welcoming developers of all skill levels from beginners to senior engineers."
                ),
                ServerRuleEntity(
                    id = "rule_gdsc_2",
                    serverId = "srv_gdsc_strath",
                    ruleIndex = 2,
                    title = "Hands-on Learning",
                    description = "Encourage peer learning and practical implementations over passive consumption."
                )
            )
            database.serverRuleDao().insertRules(sampleRules)

            // 10. Initial Server Members
            val sampleMembers = listOf(
                ServerMemberEntity(
                    id = "srv_codesistency:user_lincoln",
                    userId = "user_lincoln",
                    serverId = "srv_codesistency",
                    displayName = "Lincoln Paul (You)",
                    handle = "@lincoln_lead",
                    role = "OWNER",
                    roleTitle = "Campus Engineer",
                    roleColorHex = 0xFF003B68,
                    avatarUrl = null,
                    avatarResName = "avatar_lincoln",
                    isOnline = true,
                    customStatus = "Cooking new campus features",
                    joinedAtEpochMs = now - (60L * 86400000L),
                    badge = "Owner"
                ),
                ServerMemberEntity(
                    id = "srv_codesistency:user_robloach",
                    userId = "user_robloach",
                    serverId = "srv_codesistency",
                    displayName = "RobLoach",
                    handle = "@robloach",
                    role = "MODERATOR",
                    roleTitle = "Mod / Audio Lead",
                    roleColorHex = 0xFF23A55A,
                    avatarUrl = null,
                    avatarResName = "server_avatar2",
                    isOnline = true,
                    customStatus = "Tuning spatial audio engines",
                    joinedAtEpochMs = now - (50L * 86400000L),
                    badge = "Mod"
                ),
                ServerMemberEntity(
                    id = "srv_codesistency:user_ath",
                    userId = "user_ath",
                    serverId = "srv_codesistency",
                    displayName = "Ath",
                    handle = "@ath_sound",
                    role = "MEMBER",
                    roleTitle = "Soundtrack Contributor",
                    roleColorHex = 0xFF84CC16,
                    avatarUrl = null,
                    avatarResName = "avatar_ath",
                    isOnline = true,
                    customStatus = "Sound engineering & beats",
                    joinedAtEpochMs = now - (40L * 86400000L),
                    badge = "TDS"
                ),
                ServerMemberEntity(
                    id = "srv_codesistency:user_deleted",
                    userId = "user_deleted",
                    serverId = "srv_codesistency",
                    displayName = "Deleted User",
                    handle = "@deleted_user",
                    role = "MEMBER",
                    roleTitle = "Member",
                    roleColorHex = 0xFF5C6470,
                    avatarUrl = null,
                    avatarResName = "avatar_deleted_user",
                    isOnline = false,
                    customStatus = null,
                    joinedAtEpochMs = now - (30L * 86400000L),
                    badge = null
                ),
                ServerMemberEntity(
                    id = "srv_codesistency:user_reallybasic",
                    userId = "user_reallybasic",
                    serverId = "srv_codesistency",
                    displayName = "ReallyBasicName the ...",
                    handle = "@basic_name",
                    role = "MEMBER",
                    roleTitle = "Audio Engineer",
                    roleColorHex = 0xFF0284C7,
                    avatarUrl = null,
                    avatarResName = "server_dragon",
                    isOnline = false,
                    customStatus = "Studying DSP algorithms",
                    joinedAtEpochMs = now - (20L * 86400000L),
                    badge = null
                )
            )
            database.serverMemberDao().insertMembers(sampleMembers)

            // 11. Initial Friends Graph (for user_lincoln)
            val sampleFriends = listOf(
                FriendEntity(
                    id = "user_lincoln:user_robloach",
                    userId = "user_lincoln",
                    friendUserId = "user_robloach",
                    friendDisplayName = "RobLoach",
                    friendHandle = "@robloach",
                    friendAvatarResName = "server_avatar2",
                    status = "FRIENDS",
                    lastInteractionEpochMs = now - 3600000L,
                    dmChannelId = "dm_robloach"
                ),
                FriendEntity(
                    id = "user_lincoln:user_ath",
                    userId = "user_lincoln",
                    friendUserId = "user_ath",
                    friendDisplayName = "Ath",
                    friendHandle = "@ath_sound",
                    friendAvatarResName = "avatar_ath",
                    status = "FRIENDS",
                    lastInteractionEpochMs = now - 7200000L,
                    dmChannelId = "dm_ath"
                ),
                FriendEntity(
                    id = "user_lincoln:user_reallybasic",
                    userId = "user_lincoln",
                    friendUserId = "user_reallybasic",
                    friendDisplayName = "ReallyBasicName the ...",
                    friendHandle = "@basic_name",
                    friendAvatarResName = "server_dragon",
                    status = "PENDING_RECEIVED",
                    lastInteractionEpochMs = now - 86400000L,
                    dmChannelId = null
                )
            )
            database.friendDao().insertFriends(sampleFriends)

            // 12. Initial Server Invites
            val sampleInvites = listOf(
                ServerInviteEntity(
                    code = "CODESISTENCY-2026",
                    serverId = "srv_codesistency",
                    serverName = "Codesistency",
                    inviterId = "user_lincoln",
                    inviterName = "Lincoln Paul",
                    expiresAtEpochMs = Long.MAX_VALUE,
                    maxUses = 0,
                    usesCount = 48,
                    isRevoked = false
                ),
                ServerInviteEntity(
                    code = "GDSC-STRATH-AI",
                    serverId = "srv_gdsc_strath",
                    serverName = "GDSC Strathmore",
                    inviterId = "user_robloach",
                    inviterName = "RobLoach",
                    expiresAtEpochMs = Long.MAX_VALUE,
                    maxUses = 0,
                    usesCount = 120,
                    isRevoked = false
                ),
                ServerInviteEntity(
                    code = "ENG-UON-2026",
                    serverId = "srv_engineering_hub",
                    serverName = "Engineering Hub",
                    inviterId = "user_lincoln",
                    inviterName = "Lincoln Paul",
                    expiresAtEpochMs = Long.MAX_VALUE,
                    maxUses = 0,
                    usesCount = 85,
                    isRevoked = false
                )
            )
            val inviteDao = database.serverInviteDao()
            sampleInvites.forEach { inviteDao.insertInvite(it) }

        }
    }
}
