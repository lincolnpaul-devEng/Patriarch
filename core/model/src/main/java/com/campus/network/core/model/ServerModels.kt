package com.campus.network.core.model

/**
 * Domain representation of channel types supported within a Campus Server.
 */
enum class ServerChannelType {
    TEXT,
    ANNOUNCEMENT,
    RULES,
    VOICE,
    FORUM
}

/**
 * Domain member role hierarchy within a Campus Server.
 */
enum class ServerMemberRole {
    OWNER,
    ADMIN,
    MODERATOR,
    MEMBER,
    GUEST
}

/**
 * Peer-to-peer friend relationship status for social graph & DM routing.
 */
enum class FriendStatus {
    FRIENDS,
    PENDING_SENT,
    PENDING_RECEIVED,
    BLOCKED,
    NONE
}

/**
 * Domain model representing a Campus Server (guild/community).
 */
data class Server(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "Community",
    val ownerId: String = "user_lincoln",
    val avatarResName: String = "server_codesistency",
    val bannerResName: String? = null,
    val isVerified: Boolean = false,
    val memberCount: Int = 1,
    val inviteCode: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Domain model representing a channel inside a Server.
 */
data class ServerChannel(
    val id: String,
    val serverId: String,
    val name: String,
    val categoryName: String = "Text Channels",
    val channelType: ServerChannelType = ServerChannelType.TEXT,
    val topic: String = "",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val orderIndex: Int = 0
)

/**
 * Formal rules established by the server owner/moderators.
 */
data class ServerRule(
    val id: String,
    val serverId: String,
    val ruleIndex: Int,
    val title: String,
    val description: String
)

/**
 * Server member with role, permissions, and identity badges.
 */
data class ServerMember(
    val userId: String,
    val serverId: String,
    val displayName: String,
    val handle: String,
    val role: ServerMemberRole = ServerMemberRole.MEMBER,
    val roleTitle: String = "Member",
    val roleColorHex: Long = 0xFF5C6470,
    val avatarUrl: String? = null,
    val avatarResName: String = "avatar_lincoln",
    val isOnline: Boolean = false,
    val customStatus: String? = null,
    val joinedAtEpochMs: Long = System.currentTimeMillis(),
    val badge: String? = null
)

/**
 * Friend graph connection between two campus members.
 */
data class FriendRelationship(
    val id: String,
    val userId: String,
    val friendUserId: String,
    val friendDisplayName: String,
    val friendHandle: String,
    val friendAvatarResName: String = "avatar_lincoln",
    val status: FriendStatus = FriendStatus.NONE,
    val lastInteractionEpochMs: Long = System.currentTimeMillis(),
    val dmChannelId: String? = null
)

/**
 * Invitation link token for joining a Server.
 */
data class ServerInvite(
    val code: String,
    val serverId: String,
    val serverName: String,
    val inviterId: String,
    val inviterName: String,
    val expiresAtEpochMs: Long = Long.MAX_VALUE,
    val maxUses: Int = 0,
    val usesCount: Int = 0,
    val isRevoked: Boolean = false
)
