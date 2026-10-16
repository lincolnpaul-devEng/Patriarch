package com.campus.network.feature.channels.model

/**
 * Domain model representing an academic course item
 * with enrolled course-mates and helper indicators.
 */
data class CareerCourse(
    val id: String,
    val title: String,
    val courseCode: String,
    val durationText: String = "",
    val viewersCountText: String = "",
    val thumbnailResName: String,
    val faculty: String,
    val department: String,
    val enrolledMatesCount: Int = 18,
    val enrolledMates: List<CourseMate> = emptyList(),
    val hasActiveHelper: Boolean = true,
    val activeDiscussionSnippet: String = "",
    val unreadDiscussionCount: Int = 0
)

data class CourseMate(
    val id: String,
    val name: String,
    val ageOrYear: String, // e.g. "22" or "27"
    val major: String, // e.g. "Electrical & Embedded Engineering"
    val faculty: String,
    val isVerified: Boolean = true,
    val isHelper: Boolean = false,
    val helperBadgeText: String? = null, // e.g. "Academic Workspace Helper"
    val studyGoal: String, // e.g. "Aiming for an A in midterm · Available evenings"
    val studyStyle: String, // e.g. "Discussion & Problem Sets"
    val availabilityText: String, // e.g. "Weekday Evenings"
    val sharedCourses: List<String>, // e.g. ["#eng-201", "#ee-310"]
    val photoResName: String,
    val bio: String = "",
    val karmaScore: Int = 450,
    val isLiked: Boolean = false
)

data class InDemandCompany(
    val id: String,
    val name: String,
    val jobPostsCountText: String, // e.g. "510 job posts"
    val demandMetricText: String, // e.g. "● 0.0%" or "● +14.2%"
    val isPositiveTrend: Boolean = false,
    val logoResName: String? = null,
    val initials: String = "DA"
)

enum class ChannelViewMode {
    CHANNELS_HUB,
    CHANNEL_CHAT,
    CHANNEL_PROFILE,
    DISCOVER_CAROUSEL,
    STUDENT_PROFILE
}
