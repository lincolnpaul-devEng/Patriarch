package com.campus.network.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campus.network.core.model.FriendRelationship
import com.campus.network.core.model.FriendStatus
import com.campus.network.core.model.Server
import com.campus.network.core.model.ServerChannel
import com.campus.network.core.model.ServerChannelType
import com.campus.network.core.model.ServerInvite
import com.campus.network.core.model.ServerMember
import com.campus.network.core.model.ServerMemberRole
import com.campus.network.core.model.ServerRule

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val ownerId: String,
    val avatarResName: String,
    val bannerResName: String?,
    val isVerified: Boolean,
    val memberCount: Int,
    val inviteCode: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): Server = Server(
        id = id,
        name = name,
        description = description,
        category = category,
        ownerId = ownerId,
        avatarResName = avatarResName,
        bannerResName = bannerResName,
        isVerified = isVerified,
        memberCount = memberCount,
        inviteCode = inviteCode,
        createdAtEpochMs = createdAtEpochMs
    )

    companion object {
        fun fromDomain(server: Server): ServerEntity = ServerEntity(
            id = server.id,
            name = server.name,
            description = server.description,
            category = server.category,
            ownerId = server.ownerId,
            avatarResName = server.avatarResName,
            bannerResName = server.bannerResName,
            isVerified = server.isVerified,
            memberCount = server.memberCount,
            inviteCode = server.inviteCode,
            createdAtEpochMs = server.createdAtEpochMs
        )
    }
}

@Entity(tableName = "server_channels")
data class ServerChannelEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val name: String,
    val categoryName: String,
    val channelType: String,
    val topic: String,
    val unreadCount: Int,
    val isPinned: Boolean,
    val orderIndex: Int
) {
    fun toDomain(): ServerChannel = ServerChannel(
        id = id,
        serverId = serverId,
        name = name,
        categoryName = categoryName,
        channelType = runCatching { ServerChannelType.valueOf(channelType) }.getOrDefault(ServerChannelType.TEXT),
        topic = topic,
        unreadCount = unreadCount,
        isPinned = isPinned,
        orderIndex = orderIndex
    )

    companion object {
        fun fromDomain(channel: ServerChannel): ServerChannelEntity = ServerChannelEntity(
            id = channel.id,
            serverId = channel.serverId,
            name = channel.name,
            categoryName = channel.categoryName,
            channelType = channel.channelType.name,
            topic = channel.topic,
            unreadCount = channel.unreadCount,
            isPinned = channel.isPinned,
            orderIndex = channel.orderIndex
        )
    }
}

@Entity(tableName = "server_rules")
data class ServerRuleEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val ruleIndex: Int,
    val title: String,
    val description: String
) {
    fun toDomain(): ServerRule = ServerRule(
        id = id,
        serverId = serverId,
        ruleIndex = ruleIndex,
        title = title,
        description = description
    )

    companion object {
        fun fromDomain(rule: ServerRule): ServerRuleEntity = ServerRuleEntity(
            id = rule.id,
            serverId = rule.serverId,
            ruleIndex = rule.ruleIndex,
            title = rule.title,
            description = rule.description
        )
    }
}

@Entity(tableName = "server_members")
data class ServerMemberEntity(
    @PrimaryKey val id: String, // "$serverId:$userId"
    val userId: String,
    val serverId: String,
    val displayName: String,
    val handle: String,
    val role: String,
    val roleTitle: String,
    val roleColorHex: Long,
    val avatarUrl: String?,
    val avatarResName: String,
    val isOnline: Boolean,
    val customStatus: String?,
    val joinedAtEpochMs: Long,
    val badge: String?
) {
    fun toDomain(): ServerMember = ServerMember(
        userId = userId,
        serverId = serverId,
        displayName = displayName,
        handle = handle,
        role = runCatching { ServerMemberRole.valueOf(role) }.getOrDefault(ServerMemberRole.MEMBER),
        roleTitle = roleTitle,
        roleColorHex = roleColorHex,
        avatarUrl = avatarUrl,
        avatarResName = avatarResName,
        isOnline = isOnline,
        customStatus = customStatus,
        joinedAtEpochMs = joinedAtEpochMs,
        badge = badge
    )

    companion object {
        fun fromDomain(member: ServerMember): ServerMemberEntity = ServerMemberEntity(
            id = "${member.serverId}:${member.userId}",
            userId = member.userId,
            serverId = member.serverId,
            displayName = member.displayName,
            handle = member.handle,
            role = member.role.name,
            roleTitle = member.roleTitle,
            roleColorHex = member.roleColorHex,
            avatarUrl = member.avatarUrl,
            avatarResName = member.avatarResName ?: "",
            isOnline = member.isOnline,
            customStatus = member.customStatus,
            joinedAtEpochMs = member.joinedAtEpochMs,
            badge = member.badge
        )
    }
}

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String, // "$userId:$friendUserId"
    val userId: String,
    val friendUserId: String,
    val friendDisplayName: String,
    val friendHandle: String,
    val friendAvatarResName: String,
    val status: String,
    val lastInteractionEpochMs: Long,
    val dmChannelId: String?
) {
    fun toDomain(): FriendRelationship = FriendRelationship(
        id = id,
        userId = userId,
        friendUserId = friendUserId,
        friendDisplayName = friendDisplayName,
        friendHandle = friendHandle,
        friendAvatarResName = friendAvatarResName,
        status = runCatching { FriendStatus.valueOf(status) }.getOrDefault(FriendStatus.NONE),
        lastInteractionEpochMs = lastInteractionEpochMs,
        dmChannelId = dmChannelId
    )

    companion object {
        fun fromDomain(rel: FriendRelationship): FriendEntity = FriendEntity(
            id = rel.id.ifEmpty { "${rel.userId}:${rel.friendUserId}" },
            userId = rel.userId,
            friendUserId = rel.friendUserId,
            friendDisplayName = rel.friendDisplayName,
            friendHandle = rel.friendHandle,
            friendAvatarResName = rel.friendAvatarResName ?: "",
            status = rel.status.name,
            lastInteractionEpochMs = rel.lastInteractionEpochMs,
            dmChannelId = rel.dmChannelId
        )
    }
}

@Entity(tableName = "server_invites")
data class ServerInviteEntity(
    @PrimaryKey val code: String,
    val serverId: String,
    val serverName: String,
    val inviterId: String,
    val inviterName: String,
    val expiresAtEpochMs: Long,
    val maxUses: Int,
    val usesCount: Int,
    val isRevoked: Boolean
) {
    fun toDomain(): ServerInvite = ServerInvite(
        code = code,
        serverId = serverId,
        serverName = serverName,
        inviterId = inviterId,
        inviterName = inviterName,
        expiresAtEpochMs = expiresAtEpochMs,
        maxUses = maxUses,
        usesCount = usesCount,
        isRevoked = isRevoked
    )

    companion object {
        fun fromDomain(invite: ServerInvite): ServerInviteEntity = ServerInviteEntity(
            code = invite.code,
            serverId = invite.serverId,
            serverName = invite.serverName,
            inviterId = invite.inviterId,
            inviterName = invite.inviterName,
            expiresAtEpochMs = invite.expiresAtEpochMs,
            maxUses = invite.maxUses,
            usesCount = invite.usesCount,
            isRevoked = invite.isRevoked
        )
    }
}

/**
 * Local cache for server-channel chat.
 * Offline: store with [isOptimistic]=true / [isDelivered]=false.
 * Online: upsert remote then clear pending flags. Never treat remote sync as optional.
 */
@Entity(tableName = "server_channel_messages")
data class ServerChannelMessageEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val text: String = "",
    val gifUrl: String? = null,
    /** JSON array string of image URLs. */
    val imageUrlsJson: String = "[]",
    val reactionCount: Int = 0,
    val timestampEpochMs: Long,
    val isOptimistic: Boolean = false,
    val isDelivered: Boolean = false,
    val isMine: Boolean = false
)
