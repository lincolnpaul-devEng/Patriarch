package com.campus.network.feature.channels.model

/**
 * Server role hierarchy classification.
 */
enum class ServerRoleType {
    OWNER,
    ADMIN,
    MODERATOR,
    CONTRIBUTOR,
    MEMBER
}

/**
 * Server member role definition with color and hierarchy indicators.
 */
data class ServerMemberRole(
    val name: String,
    val roleType: ServerRoleType,
    val colorHex: Long,
    val isOwner: Boolean = false
)

/**
 * Mutual peer connection and friend request states.
 */
enum class FriendRequestStatus {
    NONE,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    FRIENDS
}

/**
 * Comprehensive server member representation with per-server profile tags,
 * server roles, online presence, and friend connection status.
 */
data class ServerMemberItem(
    val id: String,
    val serverId: String,
    val name: String,
    val handle: String,
    val avatarResName: String? = null,
    val avatarUrl: String? = null,
    val serverNickname: String? = null,
    val serverTag: String? = null, // e.g. "DEV", "TDS", "CAMP", "LEAD"
    val serverTagColorHex: Long = 0xFF5865F2,
    val role: ServerMemberRole,
    val isOnline: Boolean = true,
    val statusText: String? = null,
    val joinedDate: String = "Joined Oct 2026",
    val bio: String = "Campus developer & active community peer.",
    val friendStatus: FriendRequestStatus = FriendRequestStatus.NONE,
    val channels: List<String> = emptyList(),
    val isSelf: Boolean = false
)

/**
 * Per-server profile customizations for the active user.
 */
data class PerServerProfile(
    val serverId: String,
    val nickname: String,
    val tag: String,
    val tagColorHex: Long = 0xFF5865F2,
    val role: String = "Server Owner"
)
