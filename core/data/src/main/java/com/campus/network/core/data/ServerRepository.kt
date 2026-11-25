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
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ServerRow(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Community",
    val owner_id: String = "",
    val avatar_res_name: String = "server_codesistency",
    val banner_res_name: String? = null,
    val is_verified: Boolean = false,
    val member_count: Int = 1,
    val invite_code: String = "",
    val created_at_epoch_ms: Long = 0L,
    val updated_at: String? = null
)

@Serializable
data class ServerChannelRow(
    val id: String = "",
    val server_id: String = "",
    val name: String = "",
    val category_name: String = "TEXT CHANNELS",
    val channel_type: String = "text",
    val topic: String = "",
    val is_unread: Boolean = false,
    val order_index: Int = 0,
    val created_at_epoch_ms: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class ServerRuleRow(
    val id: String = "",
    val server_id: String = "",
    val rule_index: Int = 0,
    val title: String = "",
    val description: String = "",
    val updated_at: Long = 0L
)

@Serializable
data class ServerMemberRow(
    val id: String = "",
    val server_id: String = "",
    val user_id: String = "",
    val display_name: String = "",
    val handle: String = "@student",
    val role: String = "MEMBER",
    val role_title: String = "Member",
    val role_color_hex: Long = 0xFF5C6470,
    val avatar_url: String? = null,
    val avatar_res_name: String = "",
    val is_online: Boolean = true,
    val custom_status: String? = null,
    val joined_at_epoch_ms: Long = 0L,
    val badge: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class ServerMemberWriteRow(
    val id: String,
    val server_id: String,
    val user_id: String,
    val display_name: String,
    val handle: String = "@student",
    val role: String = "MEMBER",
    val role_title: String = "Member",
    val role_color_hex: Long = 0xFF5C6470,
    val avatar_url: String? = null,
    val avatar_res_name: String = "",
    val is_online: Boolean = true,
    val joined_at_epoch_ms: Long = System.currentTimeMillis(),
    val custom_status: String? = null,
    val badge: String? = null
)

@Serializable
data class FriendRow(
    val id: String = "",
    val user_id: String = "",
    val friend_user_id: String = "",
    val friend_display_name: String = "",
    val friend_handle: String = "",
    val friend_avatar_res_name: String = "",
    val status: String = "NONE",
    val last_interaction_epoch_ms: Long = 0L,
    val dm_channel_id: String? = null
)

@Serializable
data class CreateServerAtomicParams(
    val p_server_id: String,
    val p_name: String,
    val p_category: String,
    val p_description: String,
    val p_owner_id: String,
    val p_owner_display_name: String,
    val p_owner_handle: String,
    val p_owner_avatar_url: String? = null,
    val p_avatar_res_name: String,
    val p_banner_res_name: String? = null,
    val p_invite_code: String
)

/**
 * Repository orchestrating Campus Servers, Nested Server Channels, Rule Sets,
 * Role Identification, Invite Tokens, and the Peer Social Graph (Friends & DM routing).
 */
class ServerRepository(
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val serverDao = database.serverDao()
    private val channelDao = database.serverChannelDao()
    private val ruleDao = database.serverRuleDao()
    private val memberDao = database.serverMemberDao()
    private val friendDao = database.friendDao()
    private val inviteDao = database.serverInviteDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    // ==========================================
    // 1. SERVER LIFECYCLE & DISCOVERY
    // ==========================================

    fun getServers(): Flow<List<Server>> {
        return serverDao.getAllServers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getJoinedServers(userId: String): Flow<List<Server>> {
        if (userId.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return serverDao.getJoinedServersForUser(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getJoinedServerIds(userId: String, email: String = ""): Flow<List<String>> {
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim()
        if (cleanUserId.isBlank() && cleanEmail.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return memberDao.getJoinedServerIdsForUser(cleanUserId, cleanEmail)
            .flowOn(ioDispatcher)
    }

    suspend fun clearLocalCache() = withContext(ioDispatcher) {
        runCatching {
            memberDao.clearAllMembers()
            serverDao.clearAllServers()
        }
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
        ownerId: String = "",
        ownerAvatarUrl: String? = null,
        avatarResName: String = "server_codesistency",
        bannerResName: String? = null,
        customServerId: String? = null
    ): Result<Server> = withContext(ioDispatcher) {
        runCatching {
            require(ownerId.isNotBlank()) { "ownerId is required to create a server" }
            val cleanOwnerId = ownerId.trim()
            val ownerDisplayName = profileNameResolver.resolve(listOf(cleanOwnerId))[cleanOwnerId.lowercase()]
                ?.takeIf(String::isRealProfileDisplayName)
                ?: throw IllegalStateException("A Supabase profile name is required to create a server")
            val ownerHandle = "@" + ownerDisplayName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() || it == '_' }
            val serverId = customServerId ?: "srv_${UUID.randomUUID().toString().take(8)}"
            val cleanName = name.trim().ifEmpty { "New Server" }
            val slug = cleanName.uppercase().replace(Regex("[^A-Z0-9]"), "-").take(12)
            val inviteCode = "$slug-${UUID.randomUUID().toString().take(6).uppercase()}"
            val now = System.currentTimeMillis()

            val server = Server(
                id = serverId,
                name = cleanName,
                description = description,
                category = category.ifEmpty { "Community" },
                ownerId = cleanOwnerId,
                avatarResName = avatarResName,
                bannerResName = bannerResName,
                isVerified = false,
                memberCount = 1,
                inviteCode = inviteCode,
                createdAtEpochMs = now
            )

            // 1. Execute atomic server creation in ONE remote Supabase RPC call
            supabase.postgrest.rpc(
                "create_server_atomic",
                CreateServerAtomicParams(
                    p_server_id = serverId,
                    p_name = cleanName,
                    p_category = category.ifEmpty { "Community" },
                    p_description = description,
                    p_owner_id = cleanOwnerId,
                    p_owner_display_name = ownerDisplayName,
                    p_owner_handle = ownerHandle,
                    p_owner_avatar_url = ownerAvatarUrl,
                    p_avatar_res_name = avatarResName,
                    p_banner_res_name = bannerResName,
                    p_invite_code = inviteCode
                )
            )

            // 2. Cache confirmed server, owner member, channels, rules and invite in Room SQLite
            serverDao.insertServer(ServerEntity.fromDomain(server))
            val ownerMember = ServerMember(
                userId = cleanOwnerId,
                serverId = serverId,
                displayName = ownerDisplayName,
                handle = ownerHandle,
                role = ServerMemberRole.OWNER,
                roleTitle = "Server Owner",
                roleColorHex = 0xFF003B68,
                avatarUrl = ownerAvatarUrl,
                avatarResName = ownerAvatarUrl ?: "",
                isOnline = true,
                customStatus = "Server Founder",
                joinedAtEpochMs = now,
                badge = "Owner"
            )
            memberDao.insertMember(ServerMemberEntity.fromDomain(ownerMember))

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

            val invite = ServerInvite(
                code = inviteCode,
                serverId = serverId,
                serverName = cleanName,
                inviterId = cleanOwnerId,
                inviterName = ownerDisplayName,
                expiresAtEpochMs = Long.MAX_VALUE,
                maxUses = 0,
                usesCount = 1,
                isRevoked = false
            )
            inviteDao.insertInvite(ServerInviteEntity.fromDomain(invite))

            server
        }
    }

    suspend fun updateServerProfile(
        serverId: String,
        name: String,
        description: String,
        category: String,
        avatarResName: String,
        bannerResName: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = serverDao.getServerByIdSync(serverId)
                ?: throw IllegalStateException("Server not found.")
            val updated = existing.copy(
                name = name.trim().ifEmpty { existing.name },
                description = description.trim(),
                category = category.trim().ifEmpty { existing.category },
                avatarResName = avatarResName.ifEmpty { existing.avatarResName },
                bannerResName = bannerResName ?: existing.bannerResName
            )

            supabase.from("servers").update(
                mapOf(
                    "name" to updated.name,
                    "description" to updated.description,
                    "category" to updated.category,
                    "avatar_res_name" to updated.avatarResName,
                    "banner_res_name" to updated.bannerResName
                )
            ) {
                filter { eq("id", serverId) }
            }

            serverDao.insertServer(updated)
        }
    }

    suspend fun deleteServer(serverId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabase.from("servers").delete {
                filter { eq("id", serverId) }
            }
            serverDao.deleteServer(serverId)
        }
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
    ): Result<ServerChannel> = withContext(ioDispatcher) {
        runCatching {
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

            supabase.from("server_channels").upsert(
                ServerChannelRow(
                    id = channelId,
                    server_id = serverId,
                    name = cleanName,
                    category_name = channel.categoryName,
                    channel_type = channelType.name.lowercase(),
                    topic = topic.trim(),
                    is_unread = false,
                    order_index = nextOrder,
                    created_at_epoch_ms = System.currentTimeMillis()
                )
            )

            channelDao.insertChannel(ServerChannelEntity.fromDomain(channel))
            channel
        }
    }

    suspend fun deleteChannel(channelId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabase.from("server_channels").delete {
                filter { eq("id", channelId) }
            }
            channelDao.deleteChannel(channelId)
        }
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
    ): Result<ServerRule> = withContext(ioDispatcher) {
        runCatching {
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

            supabase.from("server_rules").upsert(
                ServerRuleRow(
                    id = ruleId,
                    server_id = serverId,
                    rule_index = nextIndex,
                    title = rule.title,
                    description = rule.description
                )
            )

            ruleDao.insertRule(ServerRuleEntity.fromDomain(rule))
            rule
        }
    }

    suspend fun deleteRule(ruleId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabase.from("server_rules").delete {
                filter { eq("id", ruleId) }
            }
            ruleDao.deleteRule(ruleId)
        }
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
    ): Result<Unit> = withContext(ioDispatcher) {
        val cleanMemberId = userId.trim().removePrefix("mem_").removeSuffix("_$serverId")
        val remoteId = "${serverId}:${cleanMemberId}"
        val remoteResult = runCatching {
            supabase.from("server_members").update(
                mapOf(
                    "role" to role.name,
                    "role_title" to roleTitle,
                    "role_color_hex" to roleColorHex
                )
            ) {
                filter { eq("id", remoteId) }
            }
        }
        if (remoteResult.isSuccess) {
            memberDao.updateMemberRole(serverId, cleanMemberId, role.name, roleTitle, roleColorHex)
            Result.success(Unit)
        } else {
            Result.failure(
                remoteResult.exceptionOrNull() ?: IllegalStateException("Could not update member role on the remote database.")
            )
        }
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
        inviterId: String = "",
        maxUses: Int = 0
    ): ServerInvite = withContext(ioDispatcher) {
        require(inviterId.isNotBlank()) { "inviterId is required to create an invite" }
        val inviterName = profileNameResolver.resolve(listOf(inviterId))[inviterId.lowercase()]
            ?.takeIf(String::isRealProfileDisplayName)
            ?: throw IllegalStateException("A Supabase profile name is required to create an invite")
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
        userId: String = "",
        avatarUrl: String? = null,
        avatarResName: String = ""
    ): Result<Server> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("userId is required to join a server"))
        }
        val displayName = try {
            profileNameResolver.resolve(listOf(userId))[userId.trim().lowercase()]
                ?.takeIf(String::isRealProfileDisplayName)
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        } ?: return@withContext Result.failure(
            IllegalStateException("A Supabase profile name is required to join a server")
        )
        val handle = "@" + displayName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() || it == '_' }
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
        val existingMember = if (userId.isNotBlank()) memberDao.getMemberSync(server.id, userId) else null
        if (userId.isNotBlank()) {
            val memberRow = ServerMemberWriteRow(
                id = "${server.id}:${userId}",
                server_id = server.id,
                user_id = userId,
                display_name = displayName,
                handle = handle,
                role = "MEMBER",
                role_title = "Member",
                role_color_hex = 0xFF5C6470,
                avatar_url = avatarUrl,
                avatar_res_name = avatarResName,
                is_online = true,
                joined_at_epoch_ms = System.currentTimeMillis()
            )
            val res = runCatching {
                supabase.from("server_members").upsert(memberRow, onConflict = "id")
            }
            if (res.isFailure) {
                return@withContext Result.failure(res.exceptionOrNull() ?: IllegalStateException("Failed to join server on cloud"))
            }

            val newMember = ServerMember(
                userId = userId,
                serverId = server.id,
                displayName = displayName,
                handle = handle,
                role = ServerMemberRole.MEMBER,
                roleTitle = "Member",
                roleColorHex = 0xFF5C6470,
                avatarUrl = avatarUrl,
                avatarResName = avatarResName,
                isOnline = true,
                customStatus = null,
                joinedAtEpochMs = System.currentTimeMillis(),
                badge = null
            )
            memberDao.insertMember(ServerMemberEntity.fromDomain(newMember))
            if (existingMember == null) {
                serverDao.incrementMemberCount(server.id)
                inviteDao.incrementInviteUsage(code)
            }
        }

        Result.success(server)
    }

    suspend fun joinServerDirectly(
        serverId: String,
        userId: String = "",
        avatarUrl: String? = null,
        avatarResName: String = ""
    ): Result<Server> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("userId is required to join a server"))
        }
        val displayName = try {
            profileNameResolver.resolve(listOf(userId))[userId.trim().lowercase()]
                ?.takeIf(String::isRealProfileDisplayName)
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        } ?: return@withContext Result.failure(
            IllegalStateException("A Supabase profile name is required to join a server")
        )
        val handle = "@" + displayName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() || it == '_' }
        val serverEntity = serverDao.getServerByIdSync(serverId)
            ?: return@withContext Result.failure(IllegalArgumentException("Server not found"))

        val existingMember = if (userId.isNotBlank()) memberDao.getMemberSync(serverId, userId) else null
        if (userId.isNotBlank()) {
            val memberRow = ServerMemberWriteRow(
                id = "${serverId}:${userId}",
                server_id = serverId,
                user_id = userId,
                display_name = displayName,
                handle = handle.ifBlank { "@student" },
                role = "MEMBER",
                role_title = "Member",
                role_color_hex = 0xFF5C6470,
                avatar_url = avatarUrl,
                avatar_res_name = avatarResName,
                is_online = true,
                joined_at_epoch_ms = System.currentTimeMillis()
            )
            val res = runCatching {
                supabase.from("server_members").upsert(memberRow, onConflict = "id")
            }
            if (res.isFailure) {
                return@withContext Result.failure(res.exceptionOrNull() ?: IllegalStateException("Failed to join server on cloud"))
            }

            val newMember = ServerMember(
                userId = userId,
                serverId = serverId,
                displayName = displayName,
                handle = handle.ifBlank { "@student" },
                role = ServerMemberRole.MEMBER,
                roleTitle = "Member",
                roleColorHex = 0xFF5C6470,
                avatarUrl = avatarUrl,
                avatarResName = avatarResName,
                isOnline = true,
                customStatus = null,
                joinedAtEpochMs = System.currentTimeMillis(),
                badge = null
            )
            memberDao.insertMember(ServerMemberEntity.fromDomain(newMember))
            if (existingMember == null) {
                serverDao.incrementMemberCount(serverId)
            }
        }
        Result.success(serverEntity.toDomain())
    }

    suspend fun syncRemoteServers(sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val cursor = if (sinceMs > 0L) {
                java.time.Instant.ofEpochMilli(sinceMs).toString()
            } else null
            val remoteList = supabase.from("servers").select {
                filter {
                    if (cursor != null) {
                        gt("updated_at", cursor)
                    }
                }
                order("updated_at", order = Order.ASCENDING)
                limit(100)
            }.decodeList<ServerRow>()

            var newestTimestamp = sinceMs
            val servers = mutableListOf<ServerEntity>()
            for (obj in remoteList) {
                val updatedAt = runCatching {
                    obj.updated_at?.let { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() } ?: 0L
                }.getOrDefault(0L)
                newestTimestamp = maxOf(newestTimestamp, updatedAt)
                servers.add(
                    ServerEntity(
                        id = obj.id,
                        name = obj.name,
                        description = obj.description,
                        category = obj.category,
                        ownerId = obj.owner_id,
                        avatarResName = obj.avatar_res_name,
                        bannerResName = obj.banner_res_name?.takeIf { it.isNotBlank() },
                        isVerified = obj.is_verified,
                        memberCount = obj.member_count,
                        inviteCode = obj.invite_code,
                        createdAtEpochMs = if (obj.created_at_epoch_ms > 0L) obj.created_at_epoch_ms else System.currentTimeMillis()
                    )
                )
            }
            if (servers.isNotEmpty()) serverDao.insertServers(servers)
            if (sinceMs == 0L) {
                val remoteIds = servers.map { it.id }
                if (remoteIds.isNotEmpty()) {
                    serverDao.deleteGhostServers(remoteIds)
                } else {
                    serverDao.clearSyncedServers()
                }
            }
            newestTimestamp
        }
    }

    suspend fun syncRemoteChannels(serverId: String, sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        if (serverId.isBlank()) return@withContext Result.success(sinceMs)
        val result = runCatching {
            supabase.from("server_channels").select {
                filter {
                    eq("server_id", serverId)
                    if (sinceMs > 0L) {
                        gt("updated_at", sinceMs)
                    }
                }
                order("order_index", order = Order.ASCENDING)
            }.decodeList<ServerChannelRow>()
        }
        var newWatermark = sinceMs
        result.onSuccess { list ->
            val channels = list.map { obj ->
                if (obj.updated_at > newWatermark) newWatermark = obj.updated_at
                val cType = when (obj.channel_type.lowercase()) {
                    "rules" -> ServerChannelType.RULES
                    "announcements" -> ServerChannelType.ANNOUNCEMENT
                    "voice" -> ServerChannelType.VOICE
                    else -> ServerChannelType.TEXT
                }
                ServerChannelEntity(
                    id = obj.id,
                    serverId = obj.server_id,
                    name = obj.name,
                    categoryName = obj.category_name,
                    channelType = cType.name,
                    topic = obj.topic,
                    unreadCount = if (obj.is_unread) 1 else 0,
                    isPinned = false,
                    orderIndex = obj.order_index
                )
            }
            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
            }
        }
        Result.success(if (newWatermark > sinceMs) newWatermark else System.currentTimeMillis())
    }

    suspend fun syncRemoteRules(serverId: String, sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        if (serverId.isBlank()) return@withContext Result.success(sinceMs)
        val result = runCatching {
            supabase.from("server_rules").select {
                filter {
                    eq("server_id", serverId)
                    if (sinceMs > 0L) {
                        gt("updated_at", sinceMs)
                    }
                }
                order("rule_index", order = Order.ASCENDING)
            }.decodeList<ServerRuleRow>()
        }
        var newWatermark = sinceMs
        result.onSuccess { list ->
            val rules = list.map { obj ->
                if (obj.updated_at > newWatermark) newWatermark = obj.updated_at
                ServerRuleEntity(
                    id = obj.id,
                    serverId = obj.server_id,
                    ruleIndex = obj.rule_index,
                    title = obj.title,
                    description = obj.description
                )
            }
            if (rules.isNotEmpty()) {
                ruleDao.insertRules(rules)
            }
        }
        Result.success(if (newWatermark > sinceMs) newWatermark else System.currentTimeMillis())
    }

    suspend fun removeMember(serverId: String, userId: String): Result<Unit> = withContext(ioDispatcher) {
        val memberId = "${serverId}:${userId.trim()}"
        val remoteResult = runCatching {
            supabase.from("server_members").delete {
                filter { eq("id", memberId) }
            }
        }
        if (remoteResult.isSuccess) {
            memberDao.removeMember(serverId, userId)
            Result.success(Unit)
        } else {
            Result.failure(
                remoteResult.exceptionOrNull() ?: IllegalStateException("Could not leave server on the remote database.")
            )
        }
    }

    suspend fun savePerServerProfile(
        serverId: String,
        userId: String,
        nickname: String,
        tag: String,
        tagColorHex: Long,
        roleTitle: String = "Member"
    ): Result<Unit> = withContext(ioDispatcher) {
        val cleanServerId = serverId.trim()
        val cleanUserId = userId.trim()
        if (cleanServerId.isBlank() || cleanUserId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Sign in to update your server profile."))
        }
        runCatching {
            supabase.from("per_server_profiles").upsert(
                mapOf(
                    "id" to "${cleanServerId}:${cleanUserId}",
                    "server_id" to cleanServerId,
                    "user_id" to cleanUserId,
                    "nickname" to nickname.trim(),
                    "tag" to tag.trim(),
                    "tag_color_hex" to tagColorHex,
                    "role_title" to roleTitle
                ),
                onConflict = "id"
            )
            Unit
        }
    }

    suspend fun updateMemberProfileForUser(
        userId: String,
        displayName: String,
        handle: String,
        avatarUrl: String?,
        avatarResName: String = ""
    ) = withContext(ioDispatcher) {
        if (userId.isNotBlank()) {
            memberDao.updateMemberProfileForUser(userId, displayName, handle, avatarUrl, avatarResName)
        }
    }

    suspend fun updateServerOwnerProfile(
        serverId: String,
        displayName: String,
        handle: String,
        avatarUrl: String?,
        avatarResName: String = ""
    ) = withContext(ioDispatcher) {
        if (serverId.isNotBlank() && displayName.isNotBlank()) {
            memberDao.updateServerOwnerProfile(serverId, displayName, handle, avatarUrl, avatarResName)
        }
    }

    suspend fun syncRemoteMembers(serverId: String, sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        val cleanServerId = serverId.trim()
        if (cleanServerId.isBlank()) return@withContext Result.success(sinceMs)
        val cursor = if (sinceMs > 0L) {
            java.time.Instant.ofEpochMilli(sinceMs).toString()
        } else null
        val result = runCatching {
            supabase.from("server_members").select {
                filter {
                    eq("server_id", cleanServerId)
                    if (cursor != null) {
                        gt("updated_at", cursor)
                    }
                }
            }.decodeList<ServerMemberRow>()
        }
        var newWatermark = sinceMs
        result.onSuccess { list ->
            runCatching {
                val profileNames = profileNameResolver.resolve(list.map { it.user_id })
                val members = mutableListOf<ServerMemberEntity>()
                for (obj in list) {
                    val updatedAt = runCatching {
                        obj.updated_at?.let { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() } ?: 0L
                    }.getOrDefault(0L)
                    if (updatedAt > newWatermark) newWatermark = updatedAt
                    val srvId = obj.server_id
                    val uId = obj.user_id
                    if (srvId.isNotBlank() && uId.isNotBlank()) {
                        val member = ServerMember(
                            userId = uId,
                            serverId = srvId,
                            displayName = profileNames[uId.lowercase()].orEmpty(),
                            handle = obj.handle,
                            role = runCatching { ServerMemberRole.valueOf(obj.role) }.getOrDefault(ServerMemberRole.MEMBER),
                            roleTitle = obj.role_title,
                            roleColorHex = obj.role_color_hex,
                            avatarUrl = obj.avatar_url?.takeIf { it.isNotBlank() },
                            avatarResName = obj.avatar_res_name,
                            isOnline = obj.is_online,
                            customStatus = obj.custom_status?.takeIf { it.isNotBlank() },
                            joinedAtEpochMs = if (obj.joined_at_epoch_ms > 0L) obj.joined_at_epoch_ms else System.currentTimeMillis(),
                            badge = obj.badge?.takeIf { it.isNotBlank() }
                        )
                        members.add(ServerMemberEntity.fromDomain(member))
                    }
                }
                if (members.isNotEmpty()) {
                    memberDao.insertMembers(members)
                    serverDao.updateMemberCount(cleanServerId, members.size)
                }
            }
        }
        Result.success(if (newWatermark > sinceMs) newWatermark else System.currentTimeMillis())
    }

    suspend fun syncRemoteMemberships(userId: String, email: String = "", sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim()
        val targetIds = listOfNotNull(cleanUserId.takeIf { it.isNotBlank() }, cleanEmail.takeIf { it.isNotBlank() }).distinct()
        if (targetIds.isEmpty()) return@withContext Result.success(sinceMs)
        val cursor = if (sinceMs > 0L) {
            java.time.Instant.ofEpochMilli(sinceMs).toString()
        } else null

        val result = runCatching {
            supabase.from("server_members").select {
                filter {
                    isIn("user_id", targetIds)
                    if (cursor != null) {
                        gt("updated_at", cursor)
                    }
                }
            }.decodeList<ServerMemberRow>()
        }
        var newWatermark = sinceMs
        result.onSuccess { list ->
            runCatching {
                val profileNames = profileNameResolver.resolve(list.map { it.user_id })
                for (obj in list) {
                    val updatedAt = runCatching {
                        obj.updated_at?.let { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() } ?: 0L
                    }.getOrDefault(0L)
                    if (updatedAt > newWatermark) newWatermark = updatedAt
                    val srvId = obj.server_id
                    val uId = obj.user_id
                    if (srvId.isNotBlank() && uId.isNotBlank()) {
                        val member = ServerMember(
                            userId = uId,
                            serverId = srvId,
                            displayName = profileNames[uId.lowercase()].orEmpty(),
                            handle = obj.handle,
                            role = runCatching { ServerMemberRole.valueOf(obj.role) }.getOrDefault(ServerMemberRole.MEMBER),
                            roleTitle = obj.role_title,
                            roleColorHex = obj.role_color_hex,
                            avatarUrl = obj.avatar_url?.takeIf { it.isNotBlank() },
                            avatarResName = obj.avatar_res_name,
                            isOnline = obj.is_online,
                            customStatus = obj.custom_status?.takeIf { it.isNotBlank() },
                            joinedAtEpochMs = if (obj.joined_at_epoch_ms > 0L) obj.joined_at_epoch_ms else System.currentTimeMillis(),
                            badge = obj.badge?.takeIf { it.isNotBlank() }
                        )
                        memberDao.insertMember(ServerMemberEntity.fromDomain(member))
                    }
                }
                if (sinceMs == 0L) {
                    val validServerIds = list.map { it.server_id }.filter { it.isNotBlank() }.distinct()
                    if (validServerIds.isNotEmpty()) {
                        memberDao.deleteGhostMemberships(cleanUserId, cleanEmail, validServerIds)
                    } else {
                        memberDao.clearSyncedMembershipsForUser(cleanUserId, cleanEmail)
                    }
                }
            }
        }
        Result.success(if (newWatermark > sinceMs) newWatermark else System.currentTimeMillis())
    }

    // ==========================================
    // 6. SOCIAL GRAPH & FRIEND REQUESTS TO DIRECT CHAT
    // ==========================================

    fun getFriendsForUser(userId: String, email: String = ""): Flow<List<FriendRelationship>> {
        val cleanUser = userId.trim()
        val cleanEmail = email.trim()
        if (cleanUser.isBlank() && cleanEmail.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return friendDao.getFriendsForUser(cleanUser, cleanEmail)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getPendingReceivedFriendRequestsCount(userId: String, email: String = ""): Flow<Int> {
        val cleanUser = userId.trim()
        val cleanEmail = email.trim()
        if (cleanUser.isBlank() && cleanEmail.isBlank()) return kotlinx.coroutines.flow.flowOf(0)
        return friendDao.getPendingReceivedFriendRequestsCount(cleanUser, cleanEmail)
            .flowOn(ioDispatcher)
    }

    fun getPendingReceivedFriendRequests(userId: String, email: String = ""): Flow<List<FriendRelationship>> {
        val cleanUser = userId.trim()
        val cleanEmail = email.trim()
        if (cleanUser.isBlank() && cleanEmail.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return friendDao.getPendingReceivedFriendRequests(cleanUser, cleanEmail)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getFriendRelationship(userId: String, friendUserId: String, email: String = ""): Flow<FriendRelationship?> {
        val cleanUser = userId.trim()
        val cleanFriend = friendUserId.trim()
        val cleanEmail = email.trim()
        if (cleanUser.isBlank() || cleanFriend.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return friendDao.getFriendRelationship(cleanUser, cleanFriend, cleanEmail)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    suspend fun syncRemoteFriends(userId: String, email: String = "", sinceMs: Long = 0L): List<FriendRelationship> = withContext(ioDispatcher) {
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim()
        val users = listOfNotNull(cleanUserId.takeIf { it.isNotBlank() }, cleanEmail.takeIf { it.isNotBlank() }).distinct()
        if (users.isEmpty()) return@withContext emptyList()

        val result = runCatching {
            supabase.from("campus_friends").select {
                filter {
                    isIn("user_id", users)
                    if (sinceMs > 0L) {
                        gt("last_interaction_epoch_ms", sinceMs)
                    }
                }
                order("last_interaction_epoch_ms", order = Order.DESCENDING)
            }.decodeList<FriendRow>()
        }
        val mappedFriends = mutableListOf<FriendRelationship>()
        result.onSuccess { list ->
            for (obj in list) {
                if (obj.id.isNotBlank() && obj.user_id.isNotBlank() && obj.friend_user_id.isNotBlank()) {
                    val rel = FriendRelationship(
                        id = obj.id,
                        userId = obj.user_id,
                        friendUserId = obj.friend_user_id,
                        friendDisplayName = "",
                        friendHandle = obj.friend_handle,
                        friendAvatarResName = obj.friend_avatar_res_name,
                        status = runCatching { FriendStatus.valueOf(obj.status) }.getOrDefault(FriendStatus.NONE),
                        lastInteractionEpochMs = if (obj.last_interaction_epoch_ms > 0L) obj.last_interaction_epoch_ms else System.currentTimeMillis(),
                        dmChannelId = obj.dm_channel_id?.takeIf { it.isNotBlank() }
                    )
                    mappedFriends.add(rel)
                }
            }
        }.onFailure { e ->
            android.util.Log.e("ServerRepository", "Failed querying campus_friends: ${e.message}")
        }
        val profileNames = runCatching {
            profileNameResolver.resolve(mappedFriends.map { it.friendUserId })
        }.onFailure { e ->
            android.util.Log.e("ServerRepository", "Failed resolving friend profile names: ${e.message}")
        }.getOrDefault(emptyMap())
        val resolvedFriends = mappedFriends.map { relationship ->
            relationship.copy(
                friendDisplayName = profileNames[relationship.friendUserId.lowercase()].orEmpty()
            )
        }
        if (resolvedFriends.isNotEmpty()) {
            friendDao.insertFriends(resolvedFriends.map(FriendEntity::fromDomain))
        }
        if (sinceMs == 0L) {
            val remoteIds = resolvedFriends.map { it.id }
            if (remoteIds.isNotEmpty()) {
                friendDao.deleteGhostFriends(cleanUserId, cleanEmail, remoteIds)
            } else {
                friendDao.clearSyncedFriends(cleanUserId, cleanEmail)
            }
        }
        android.util.Log.d("ServerRepository", "syncRemoteFriends completed: loaded ${resolvedFriends.size} friend relationships")
        resolvedFriends
    }

    suspend fun sendFriendRequest(
        userId: String,
        friendUserId: String,
        serverId: String? = null
    ): String = withContext(ioDispatcher) {
        val cleanSender = userId.trim()
        val cleanFriend = friendUserId.trim()
        if (cleanSender.isBlank() || cleanFriend.isBlank() || cleanSender.equals(cleanFriend, ignoreCase = true)) {
            throw IllegalArgumentException("Friend request requires two distinct user IDs")
        }
        val profileNames = profileNameResolver.resolve(listOf(cleanSender, cleanFriend))
        val senderDisplayName = profileNames[cleanSender.lowercase()]
            ?: throw IllegalStateException("Sender profile has no real full_name in Supabase")
        val friendDisplayName = profileNames[cleanFriend.lowercase()]
            ?: throw IllegalStateException("Friend profile has no real full_name in Supabase")

        val id = "$cleanSender:$cleanFriend"
        val reverseId = "$cleanFriend:$cleanSender"
        val existing = friendDao.getFriendRelationshipSync(cleanSender, cleanFriend)
        val now = System.currentTimeMillis()

        // Mirror both sides to Supabase first before confirming locally
        val outbound = FriendRow(
            id = id,
            user_id = cleanSender,
            friend_user_id = cleanFriend,
            friend_display_name = friendDisplayName,
            friend_handle = "",
            friend_avatar_res_name = "",
            status = FriendStatus.PENDING_SENT.name,
            last_interaction_epoch_ms = now
        )
        val inbound = FriendRow(
            id = reverseId,
            user_id = cleanFriend,
            friend_user_id = cleanSender,
            friend_display_name = senderDisplayName,
            friend_handle = "",
            friend_avatar_res_name = "",
            status = FriendStatus.PENDING_RECEIVED.name,
            last_interaction_epoch_ms = now
        )

        val resOut = runCatching { supabase.from("campus_friends").upsert(outbound) }
        val resIn = runCatching { supabase.from("campus_friends").upsert(inbound) }
        if (resOut.isFailure || resIn.isFailure) {
            val err = resOut.exceptionOrNull() ?: resIn.exceptionOrNull()
            android.util.Log.e("ServerRepository", "campus_friends upsert failed: ${err?.message}")
            throw err ?: IllegalStateException("Could not send friend request to the remote database.")
        }

        if (existing == null) {
            val relationship = FriendRelationship(
                id = id,
                userId = cleanSender,
                friendUserId = cleanFriend,
                friendDisplayName = friendDisplayName,
                friendHandle = "",
                friendAvatarResName = "",
                status = FriendStatus.PENDING_SENT,
                lastInteractionEpochMs = now,
                dmChannelId = null
            )
            friendDao.insertFriend(FriendEntity.fromDomain(relationship))
        } else {
            friendDao.insertFriend(
                existing.copy(
                    friendDisplayName = friendDisplayName,
                    status = FriendStatus.PENDING_SENT.name,
                    lastInteractionEpochMs = now
                )
            )
        }

        runCatching {
            CampusInboxNotifier(supabase).notifyFriendRequest(
                recipientUserId = cleanFriend,
                senderUserId = cleanSender,
                serverId = serverId
            )
        }
        friendDisplayName
    }

    suspend fun acceptFriendRequest(
        userId: String,
        friendUserId: String
    ): String = withContext(ioDispatcher) {
        val cleanUser = userId.trim()
        val cleanFriend = friendUserId.trim()
        if (cleanUser.isBlank() || cleanFriend.isBlank()) {
            throw IllegalArgumentException("Accepting a friend request requires two user IDs")
        }
        val profileNames = profileNameResolver.resolve(listOf(cleanUser, cleanFriend))
        val userDisplayName = profileNames[cleanUser.lowercase()]
            ?: throw IllegalStateException("User profile has no real full_name in Supabase")
        val friendDisplayName = profileNames[cleanFriend.lowercase()]
            ?: throw IllegalStateException("Friend profile has no real full_name in Supabase")

        val id = "$cleanUser:$cleanFriend"
        val reverseId = "$cleanFriend:$cleanUser"
        val now = System.currentTimeMillis()
        val dmId = "dm_${minOf(cleanUser, cleanFriend)}_${maxOf(cleanUser, cleanFriend)}"

        val remoteResult = runCatching {
            supabase.from("campus_friends").update(
                mapOf(
                    "status" to FriendStatus.FRIENDS.name,
                    "dm_channel_id" to dmId,
                    "last_interaction_epoch_ms" to now,
                    "friend_display_name" to friendDisplayName
                )
            ) {
                filter { eq("id", id) }
            }
            supabase.from("campus_friends").update(
                mapOf(
                    "status" to FriendStatus.FRIENDS.name,
                    "dm_channel_id" to dmId,
                    "last_interaction_epoch_ms" to now,
                    "friend_display_name" to userDisplayName
                )
            ) {
                filter { eq("id", reverseId) }
            }
        }
        if (remoteResult.isFailure) {
            throw remoteResult.exceptionOrNull()
                ?: IllegalStateException("Could not confirm friendship on the remote database.")
        }

        val existing = friendDao.getFriendRelationshipSync(cleanUser, cleanFriend)
        if (existing != null) {
            friendDao.insertFriend(
                existing.copy(
                    friendDisplayName = friendDisplayName,
                    status = FriendStatus.FRIENDS.name,
                    lastInteractionEpochMs = now,
                    dmChannelId = dmId
                )
            )
        } else {
            friendDao.insertFriend(
                FriendEntity(
                    id = id,
                    userId = cleanUser,
                    friendUserId = cleanFriend,
                    friendDisplayName = friendDisplayName,
                    friendHandle = "",
                    friendAvatarResName = "",
                    status = FriendStatus.FRIENDS.name,
                    lastInteractionEpochMs = now,
                    dmChannelId = dmId
                )
            )
        }

        runCatching {
            CampusInboxNotifier(supabase).notifyFriendRequestAccepted(
                recipientUserId = cleanFriend,
                actorUserId = cleanUser
            )
        }
        friendDisplayName
    }

    suspend fun declineOrRemoveFriend(
        userId: String,
        friendUserId: String
    ): Result<Unit> = withContext(ioDispatcher) {
        val cleanUser = userId.trim()
        val cleanFriend = friendUserId.trim()
        if (cleanUser.isBlank() || cleanFriend.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid user IDs"))
        }

        val id = "$cleanUser:$cleanFriend"
        val reverseId = "$cleanFriend:$cleanUser"
        val remoteResult = runCatching {
            supabase.from("campus_friends").delete {
                filter { eq("id", id) }
            }
            supabase.from("campus_friends").delete {
                filter { eq("id", reverseId) }
            }
        }
        if (remoteResult.isFailure) {
            return@withContext Result.failure(
                remoteResult.exceptionOrNull()
                    ?: IllegalStateException("Could not remove friend request on the remote database.")
            )
        }
        friendDao.deleteFriendRelationship(id)
        friendDao.deleteFriendRelationship(reverseId)
        Result.success(Unit)
    }

    suspend fun getOrCreateDmChannelId(
        userId: String,
        friendUserId: String
    ): String = withContext(ioDispatcher) {
        val cleanUser = userId.trim()
        val cleanFriend = friendUserId.trim()
        if (cleanUser.isBlank() || cleanFriend.isBlank()) return@withContext ""

        val id = "$cleanUser:$cleanFriend"
        val existing = friendDao.getFriendRelationshipSync(cleanUser, cleanFriend)
        val existingDmId = existing?.dmChannelId
        if (existingDmId != null) {
            return@withContext existingDmId
        }

        val newDmId = "dm_${minOf(cleanUser, cleanFriend)}_${maxOf(cleanUser, cleanFriend)}"
        if (existing != null) {
            friendDao.updateDmChannelId(id, newDmId)
        }
        newDmId
    }
}
