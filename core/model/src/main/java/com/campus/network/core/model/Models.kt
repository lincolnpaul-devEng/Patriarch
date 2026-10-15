package com.campus.network.core.model

/**
 * Tab 1 Domain: Dynamic Flash-Meetup with active countdown & campus coordinates.
 */
data class FlashMeet(
    val id: String,
    val title: String,
    val description: String,
    val category: MeetCategory,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val creatorName: String,
    val creatorAvatar: String? = null,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val attendeeCount: Int = 1,
    val maxAttendees: Int = 8
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAtEpochMs

    val remainingSeconds: Long
        get() = ((expiresAtEpochMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
}

enum class MeetCategory {
    STUDY_JAM,
    FOOD_RUN,
    PROJECT_HACK,
    SPORTS_PICKUP,
    COMMUTE_POOL
}

/**
 * Tab 2 Domain: Cascading nested academic channels for faculty, department, and course codes.
 */
data class AcademicChannel(
    val id: String,
    val courseCode: String, // e.g., "#cs-201"
    val courseTitle: String,
    val faculty: String,
    val department: String,
    val unreadCount: Int = 0,
    val lastMessageSnippet: String = "",
    val lastMessageTime: String = "",
    val activeStudentsCount: Int = 0,
    val isPinned: Boolean = false
)

data class FacultyDepartmentGroup(
    val facultyName: String,
    val departments: List<DepartmentAccordion>
)

data class DepartmentAccordion(
    val departmentName: String,
    val courses: List<AcademicChannel>
)

/**
 * Tab 3 Domain: Inline peer transaction settlement payload for Stream Chat slot factory.
 */
data class PeerTransactionPayload(
    val transactionId: String,
    val senderName: String,
    val recipientName: String,
    val amount: Double,
    val currency: String = "KES",
    val purpose: String,
    val status: SettlementStatus,
    val timestampMs: Long,
    val referenceCode: String
)

enum class SettlementStatus {
    PENDING_ESCROW,
    ESCROW_LOCKED,
    SETTLED,
    CANCELLED
}

/**
 * Tab 4 Domain: Campus Micro-Commerce (Storefront Item Grid & Real-Time Micro-Gig Board).
 */
data class MarketItem(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String = "KES",
    val category: String,
    val condition: String,
    val sellerName: String,
    val sellerRating: Float,
    val isCampusVerifiedSeller: Boolean,
    val campusHallLocation: String,
    val imageUrl: String? = null,
    val timestampMs: Long
)

data class MicroGig(
    val id: String,
    val title: String,
    val description: String,
    val payoutAmount: Double,
    val currency: String = "KES",
    val estimatedDuration: String,
    val posterName: String,
    val posterTrustScore: Int,
    val tag: String,
    val urgencyLevel: GigUrgency,
    val spotsAvailable: Int = 1,
    val timestampMs: Long
)

enum class GigUrgency {
    NORMAL,
    HIGH,
    FLASH_URGENT
}

/**
 * Tab 5 Domain: Gamified Persona (Student identity, verified skill tags, private Quest Deck).
 */
data class StudentProfile(
    val studentId: String,
    val fullName: String,
    val universityEmail: String,
    val universityName: String,
    val degreeProgram: String,
    val academicYear: String,
    val consistencyStreakDays: Int,
    val behavioralTrustScore: Int, // 0 - 100
    val verifiedSkillBadges: List<VerifiedSkillBadge>,
    val privateQuestDeck: List<ConsistencyQuest>
)

data class VerifiedSkillBadge(
    val id: String,
    val name: String,
    val facultyVerifier: String,
    val verifiedDate: String,
    val endorsementCount: Int,
    val level: BadgeLevel
)

enum class BadgeLevel {
    BRONZE,
    SILVER,
    GOLD,
    CAMPUS_MASTER
}

data class ConsistencyQuest(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val currentProgress: Int,
    val targetProgress: Int,
    val isCompleted: Boolean,
    val category: QuestCategory
) {
    val progressPercentage: Float
        get() = if (targetProgress > 0) (currentProgress.toFloat() / targetProgress.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class QuestCategory {
    ACADEMIC_PEER_HELP,
    COMMERCE_RELIABILITY,
    COMMUNITY_ENGAGEMENT,
    FOCUS_STREAK
}
