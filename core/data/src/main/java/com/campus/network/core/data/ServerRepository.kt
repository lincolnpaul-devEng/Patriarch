package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.FriendEntity
import com.campus.network.core.database.entity.ServerChannelEntity
import com.campus.network.core.database.entity.ServerEntity
import com.campus.network.core.database.entity.ServerInviteEntity
import com.campus.network.core.database.entity.ServerMemberEntity
import com.campus.network.core.database.entity.ServerRuleEntity
import com.campus.network.core.model.FriendRelationship
import com.campus.network.core.model.FriendStatus
import com.campus.network.core.model.Server
import com.campus.network.core.model.ServerChannel
import com.campus.network.core.model.ServerChannelType
import com.campus.network.core.model.ServerInvite
import com.campus.network.core.model.ServerMember
import com.campus.network.core.model.ServerMemberRole
import com.campus.network.core.model.ServerRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository orchestrating Campus Servers, Nested Server Channels, Rule Sets,
 * Role Identification, Invite Tokens, and the Peer Social Graph (Friends & DM routing).
 */
class ServerRepository(
    private val database: CampusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val serverDao = database.serverDao()
    private val channelDao = database.serverChannelDao()
    private val ruleDao = database.serverRuleDao()
    private val memberDao = database.serverMemberDao()
    private val friendDao = database.friendDao()
    private val inviteDao = database.serverInviteDao()

    // ==========================================
    // 1. SERVER LIFECYCLE & DISCOVERY
    // ==========================================

    fun getServers(): Flow<List<Server>> {
        return serverDao.getAllServers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getServerById(serverId: String): Flow<Server?> {
        return serverDao.getServerById(serverId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    suspend fun createServer(
        name: String,
        category: String,
        description: String = "",
        ownerId: String = "user_lincoln",
        ownerDisplayName: String = "Lincoln Paul",
        ownerHandle: String = "@lincoln_lead",
        avatarResName: String = "server_codesistency",
        bannerResName: String? = null
    ): Server = withContext(ioDispatcher) {
        val serverId = "srv_${UUID.randomUUID().toString().take(8)}"
        val cleanName = name.trim().ifEmpty { "New Server" }
        val slug = cleanName.uppercase().replace(Regex("[^A-Z0-9]"), "-").take(12)
        val inviteCode = "$slug-${UUID.randomUUID().toString().take(6).uppercase()}"
        val now = System.currentTimeMillis()

        val server = Server(
            id = serverId,
            name = cleanName,
            description = description,
            category = category.ifEmpty { "Community" },
            ownerId = ownerId,
            avatarResName = avatarResName,
            bannerResName = bannerResName,
            isVerified = false,
            memberCount = 1,
            inviteCode = inviteCode,
            createdAtEpochMs = now
        )

        // 1. Save Server
        serverDao.insertServer(ServerEntity.fromDomain(server))

        // 2. Add Owner Member
        val ownerMember = ServerMember(
            userId = ownerId,
            serverId = serverId,
            displayName = ownerDisplayName,
            handle = ownerHandle,
            role = ServerMemberRole.OWNER,
            roleTitle = "Server Owner",
            roleColorHex = 0xFF003B68,
            avatarUrl = null,
            avatarResName = "avatar_lincoln",
            isOnline = true,
            customStatus = "Server Founder",
            joinedAtEpochMs = now,
            badge = "Owner"
        )
        memberDao.insertMember(ServerMemberEntity.fromDomain(ownerMember))

        // 3. Create Default Welcome Channels & General Chat
        val defaultChannels = listOf(
            ServerChannel(
                id = "ch_${serverId}_rules",
                serverId = serverId,
                name = "rules",
                categoryName = "WELCOME",
                channelType = ServerChannelType.RULES,
                topic = "Official server rules and safety policies.",
                unreadCount = 0,
                isPinned = true,
                orderIndex = 1
            ),
            ServerChannel(
                id = "ch_${serverId}_announcements",
                serverId = serverId,
                name = "announcements",
                categoryName = "WELCOME",
                channelType = ServerChannelType.ANNOUNCEMENT,
                topic = "Updates, events, and broadcast announcements.",
                unreadCount = 0,
                isPinned = true,
                orderIndex = 2
            ),
            ServerChannel(
                id = "ch_${serverId}_general",
                serverId = serverId,
                name = "general",
                categoryName = "TEXT CHANNELS",
                channelType = ServerChannelType.TEXT,
                topic = "General discussion and hangout lounge.",
                unreadCount = 0,
                isPinned = false,
                orderIndex = 3
            )
        )
        channelDao.insertChannels(defaultChannels.map { ServerChannelEntity.fromDomain(it) })

        // 4. Create Default Starter Rules
        val defaultRules = listOf(
            ServerRule(
                id = "rule_${serverId}_1",
                serverId = serverId,
                ruleIndex = 1,
                title = "Be Respectful",
                description = "Maintain civility and respect toward all campus peers and community members."
            ),
            ServerRule(
                id = "rule_${serverId}_2",
                serverId = serverId,
                ruleIndex = 2,
                title = "No Spam or Unsolicited Promotion",
                description = "Keep chat channels relevant to the server's topics and avoid spamming links."
            ),
            ServerRule(
                id = "rule_${serverId}_3",
                serverId = serverId,
                ruleIndex = 3,
                title = "Follow Moderation Directions",
                description = "Direct any concerns to server admins or moderators."
            )
        )
        ruleDao.insertRules(defaultRules.map { ServerRuleEntity.fromDomain(it) })

        // 5. Create Default Permanent Invite Link
        val invite = ServerInvite(
            code = inviteCode,
            serverId = serverId,
            serverName = cleanName,
            inviterId = ownerId,
            inviterName = ownerDisplayName,
            expiresAtEpochMs = Long.MAX_VALUE,
            maxUses = 0,
            usesCount = 1,
            isRevoked = false
        )
        inviteDao.insertInvite(ServerInviteEntity.fromDomain(invite))

        server
    }

    suspend fun updateServerProfile(
        serverId: String,
        name: String,
        description: String,
        category: String,
        avatarResName: String
    ) = withContext(ioDispatcher) {
        val existing = serverDao.getServerByIdSync(serverId) ?: return@withContext
        val updated = existing.copy(
            name = name.trim().ifEmpty { existing.name },
            description = description.trim(),
            category = category.trim().ifEmpty { existing.category },
            avatarResName = avatarResName.ifEmpty { existing.avatarResName }
        )
        serverDao.insertServer(updated)
    }

    suspend fun deleteServer(serverId: String) = withContext(ioDispatcher) {
        serverDao.deleteServer(serverId)
    }

    // ==========================================
    // 2. CHANNELS MANAGEMENT
    // ==========================================

    fun getChannelsForServer(serverId: String): Flow<List<ServerChannel>> {
        return channelDao.getChannelsForServer(serverId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getChannelById(channelId: String): Flow<ServerChannel?> {
        return channelDao.getChannelById(channelId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    suspend fun createChannel(
        serverId: String,
        name: String,
        categoryName: String = "TEXT CHANNELS",
        channelType: ServerChannelType = ServerChannelType.TEXT,
        topic: String = ""
    ): ServerChannel = withContext(ioDispatcher) {
        val cleanName = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-")
        val channelId = "ch_${serverId}_${cleanName.take(16)}_${UUID.randomUUID().toString().take(4)}"
        val existingChannels = channelDao.getChannelsForServer(serverId).firstOrNull().orEmpty()
        val nextOrder = (existingChannels.maxOfOrNull { it.orderIndex } ?: 0) + 1

        val channel = ServerChannel(
            id = channelId,
            serverId = serverId,
            name = cleanName,
            categoryName = categoryName.trim().ifEmpty { "TEXT CHANNELS" },
            channelType = channelType,
            topic = topic.trim(),
            unreadCount = 0,
            isPinned = false,
            orderIndex = nextOrder
        )

        channelDao.insertChannel(ServerChannelEntity.fromDomain(channel))
        channel
    }

    suspend fun deleteChannel(channelId: String) = withContext(ioDispatcher) {
        channelDao.deleteChannel(channelId)
    }

    suspend fun markChannelRead(channelId: String) = withContext(ioDispatcher) {
        channelDao.markChannelRead(channelId)
    }

    // ==========================================
    // 3. SERVER RULES MANAGEMENT
    // ==========================================

    fun getRulesForServer(serverId: String): Flow<List<ServerRule>> {
        return ruleDao.getRulesForServer(serverId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    suspend fun createRule(
        serverId: String,
        title: String,
        description: String
    ): ServerRule = withContext(ioDispatcher) {
        val existingRules = ruleDao.getRulesForServer(serverId).firstOrNull().orEmpty()
        val nextIndex = (existingRules.maxOfOrNull { it.ruleIndex } ?: 0) + 1
        val ruleId = "rule_${serverId}_$nextIndex"

        val rule = ServerRule(
            id = ruleId,
            serverId = serverId,
            ruleIndex = nextIndex,
            title = title.trim(),
            description = description.trim()
        )

        ruleDao.insertRule(ServerRuleEntity.fromDomain(rule))
        rule
    }

    suspend fun deleteRule(ruleId: String) = withContext(ioDispatcher) {
        ruleDao.deleteRule(ruleId)
    }

    // ==========================================
    // 4. MEMBERS & ROLE IDENTIFICATION
    // ==========================================

    fun getMembersForServer(serverId: String): Flow<List<ServerMember>> {
        return memberDao.getMembersForServer(serverId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getMember(serverId: String, userId: String): Flow<ServerMember?> {
        return memberDao.getMember(serverId, userId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    suspend fun updateMemberRole(
        serverId: String,
        userId: String,
        role: ServerMemberRole,
        roleTitle: String,
        roleColorHex: Long
    ) = withContext(ioDispatcher) {
        memberDao.updateMemberRole(serverId, userId, role.name, roleTitle, roleColorHex)
    }

    suspend fun removeMember(serverId: String, userId: String) = withContext(ioDispatcher) {
        memberDao.removeMember(serverId, userId)
    }

    // ==========================================
    // 5. INVITATIONS & JOINING
    // ==========================================

    fun getInviteByCode(code: String): Flow<ServerInvite?> {
        return inviteDao.getInviteByCode(code.trim().uppercase())
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    fun getInvitesForServer(serverId: String): Flow<List<ServerInvite>> {
        return inviteDao.getInvitesForServer(serverId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    suspend fun createInvite(
        serverId: String,
        inviterId: String = "user_lincoln",
        inviterName: String = "Lincoln Paul",
        maxUses: Int = 0
    ): ServerInvite = withContext(ioDispatcher) {
        val server = serverDao.getServerByIdSync(serverId) ?: throw IllegalArgumentException("Server not found")
        val slug = server.name.uppercase().replace(Regex("[^A-Z0-9]"), "-").take(10)
        val code = "$slug-${UUID.randomUUID().toString().take(6).uppercase()}"

        val invite = ServerInvite(
            code = code,
            serverId = serverId,
            serverName = server.name,
            inviterId = inviterId,
            inviterName = inviterName,
            expiresAtEpochMs = Long.MAX_VALUE,
            maxUses = maxUses,
            usesCount = 0,
            isRevoked = false
        )

        inviteDao.insertInvite(ServerInviteEntity.fromDomain(invite))
        invite
    }

    suspend fun joinServerByInvite(
        inviteCode: String,
        userId: String = "user_lincoln",
        displayName: String = "Lincoln Paul",
        handle: String = "@lincoln_lead",
        avatarResName: String = "avatar_lincoln"
    ): Result<Server> = withContext(ioDispatcher) {
        val code = inviteCode.trim().uppercase()
        val inviteEntity = inviteDao.getInviteByCodeSync(code)
            ?: return@withContext Result.failure(IllegalArgumentException("Invite code not found"))

        if (inviteEntity.isRevoked) {
            return@withContext Result.failure(IllegalStateException("This invite link has been revoked"))
        }

        if (inviteEntity.expiresAtEpochMs < System.currentTimeMillis()) {
            return@withContext Result.failure(IllegalStateException("This invite link has expired"))
        }

        if (inviteEntity.maxUses > 0 && inviteEntity.usesCount >= inviteEntity.maxUses) {
            return@withContext Result.failure(IllegalStateException("This invite link has reached its maximum uses"))
        }

        val server = serverDao.getServerByIdSync(inviteEntity.serverId)?.toDomain()
            ?: return@withContext Result.failure(IllegalArgumentException("Target server no longer exists"))

        // Check if member already in server
        val existingMember = memberDao.getMemberSync(server.id, userId)
        if (existingMember == null) {
            val newMember = ServerMember(
                userId = userId,
                serverId = server.id,
                displayName = displayName,
                handle = handle,
                role = ServerMemberRole.MEMBER,
                roleTitle = "Member",
                roleColorHex = 0xFF5C6470,
                avatarUrl = null,
                avatarResName = avatarResName,
                isOnline = true,
                customStatus = null,
                joinedAtEpochMs = System.currentTimeMillis(),
                badge = null
            )
            memberDao.insertMember(ServerMemberEntity.fromDomain(newMember))
            serverDao.incrementMemberCount(server.id)
            inviteDao.incrementInviteUsage(code)
        }

        Result.success(server)
    }

    // ==========================================
    // 6. SOCIAL GRAPH & FRIEND REQUESTS TO DIRECT CHAT
    // ==========================================

    fun getFriendsForUser(userId: String = "user_lincoln"): Flow<List<FriendRelationship>> {
        return friendDao.getFriendsForUser(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getFriendRelationship(userId: String = "user_lincoln", friendUserId: String): Flow<FriendRelationship?> {
        return friendDao.getFriendRelationship(userId, friendUserId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    suspend fun sendFriendRequest(
        userId: String = "user_lincoln",
        friendUserId: String,
        friendDisplayName: String,
        friendHandle: String,
        friendAvatarResName: String = "avatar_lincoln"
    ) = withContext(ioDispatcher) {
        val id = "$userId:$friendUserId"
        val existing = friendDao.getFriendRelationshipSync(userId, friendUserId)
        val now = System.currentTimeMillis()

        if (existing == null) {
            val relationship = FriendRelationship(
                id = id,
                userId = userId,
                friendUserId = friendUserId,
                friendDisplayName = friendDisplayName,
                friendHandle = friendHandle,
                friendAvatarResName = friendAvatarResName,
                status = FriendStatus.PENDING_SENT,
                lastInteractionEpochMs = now,
                dmChannelId = null
            )
            friendDao.insertFriend(FriendEntity.fromDomain(relationship))
        } else {
            friendDao.updateFriendStatus(id, FriendStatus.PENDING_SENT.name, now)
        }
    }

    suspend fun acceptFriendRequest(
        userId: String = "user_lincoln",
        friendUserId: String
    ) = withContext(ioDispatcher) {
        val id = "$userId:$friendUserId"
        val now = System.currentTimeMillis()
        val dmId = "dm_${userId}_${friendUserId}"

        val existing = friendDao.getFriendRelationshipSync(userId, friendUserId)
        if (existing != null) {
            friendDao.updateFriendStatus(id, FriendStatus.FRIENDS.name, now)
            friendDao.updateDmChannelId(id, dmId)
        }
    }

    suspend fun declineOrRemoveFriend(
        userId: String = "user_lincoln",
        friendUserId: String
    ) = withContext(ioDispatcher) {
        val id = "$userId:$friendUserId"
        friendDao.deleteFriendRelationship(id)
    }

    suspend fun getOrCreateDmChannelId(
        userId: String = "user_lincoln",
        friendUserId: String
    ): String = withContext(ioDispatcher) {
        val id = "$userId:$friendUserId"
        val existing = friendDao.getFriendRelationshipSync(userId, friendUserId)
        val existingDmId = existing?.dmChannelId
        if (existingDmId != null) {
            return@withContext existingDmId
        }

        val newDmId = "dm_${minOf(userId, friendUserId)}_${maxOf(userId, friendUserId)}"
        if (existing != null) {
            friendDao.updateDmChannelId(id, newDmId)
        }
        newDmId
    }
}
