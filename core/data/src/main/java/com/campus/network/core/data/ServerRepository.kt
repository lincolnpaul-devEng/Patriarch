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
    private val supabaseClient: com.campus.network.core.network.SupabaseClientProvider = com.campus.network.core.network.SupabaseClientProvider(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val serverDao = database.serverDao()
    private val channelDao = database.serverChannelDao()
    private val ruleDao = database.serverRuleDao()
    private val memberDao = database.serverMemberDao()
    private val friendDao = database.friendDao()
    private val inviteDao = database.serverInviteDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabaseClient)

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
            val rpcPayload = org.json.JSONObject().apply {
                put("p_server_id", serverId)
                put("p_name", cleanName)
                put("p_category", category.ifEmpty { "Community" })
                put("p_description", description)
                put("p_owner_id", cleanOwnerId)
                put("p_owner_display_name", ownerDisplayName)
                put("p_owner_handle", ownerHandle)
                if (!ownerAvatarUrl.isNullOrBlank()) put("p_owner_avatar_url", ownerAvatarUrl)
                put("p_avatar_res_name", avatarResName)
                if (!bannerResName.isNullOrBlank()) put("p_banner_res_name", bannerResName)
                put("p_invite_code", inviteCode)
            }.toString()
            supabaseClient.callRpc("create_server_atomic", rpcPayload).getOrThrow()

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

            val payload = org.json.JSONObject().apply {
                put("name", updated.name)
                put("description", updated.description)
                put("category", updated.category)
                put("avatar_res_name", updated.avatarResName)
                if (updated.bannerResName != null) put("banner_res_name", updated.bannerResName)
            }.toString()
            supabaseClient.updateRecord("servers?id=eq.$serverId", payload).getOrThrow()

            serverDao.insertServer(updated)
        }
    }

    suspend fun deleteServer(serverId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabaseClient.deleteRecord("servers?id=eq.$serverId").getOrThrow()
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

            val payload = org.json.JSONObject().apply {
                put("id", channelId)
                put("server_id", serverId)
                put("name", cleanName)
                put("category_name", channel.categoryName)
                put("channel_type", channelType.name.lowercase())
                put("topic", topic.trim())
                put("is_unread", false)
                put("order_index", nextOrder)
                put("created_at_epoch_ms", System.currentTimeMillis())
            }.toString()
            supabaseClient.upsertRecord("server_channels", payload, onConflict = "id").getOrThrow()

            channelDao.insertChannel(ServerChannelEntity.fromDomain(channel))
            channel
        }
    }

    suspend fun deleteChannel(channelId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabaseClient.deleteRecord("server_channels?id=eq.$channelId").getOrThrow()
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

            val payload = org.json.JSONObject().apply {
                put("id", ruleId)
                put("server_id", serverId)
                put("rule_index", nextIndex)
                put("title", rule.title)
                put("description", rule.description)
            }.toString()
            supabaseClient.upsertRecord("server_rules", payload, onConflict = "id").getOrThrow()

            ruleDao.insertRule(ServerRuleEntity.fromDomain(rule))
            rule
        }
    }

    suspend fun deleteRule(ruleId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabaseClient.deleteRecord("server_rules?id=eq.$ruleId").getOrThrow()
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
    ) = withContext(ioDispatcher) {
        memberDao.updateMemberRole(serverId, userId, role.name, roleTitle, roleColorHex)
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
            val payload = org.json.JSONObject().apply {
                put("id", "${server.id}:${userId}")
                put("server_id", server.id)
                put("user_id", userId)
                put("display_name", displayName)
                put("handle", handle)
                put("role", "MEMBER")
                put("role_title", "Member")
                put("role_color_hex", 0xFF5C6470)
                if (avatarUrl != null) put("avatar_url", avatarUrl)
                put("avatar_res_name", avatarResName)
                put("is_online", true)
                put("joined_at_epoch_ms", System.currentTimeMillis())
            }.toString()
            val res = supabaseClient.upsertRecord("server_members", payload, onConflict = "id", ignoreDuplicates = true)
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
            val payload = org.json.JSONObject().apply {
                put("id", "${serverId}:${userId}")
                put("server_id", serverId)
                put("user_id", userId)
                put("display_name", displayName)
                put("handle", handle.ifBlank { "@student" })
                put("role", "MEMBER")
                put("role_title", "Member")
                put("role_color_hex", 0xFF5C6470)
                if (avatarUrl != null) put("avatar_url", avatarUrl)
                put("avatar_res_name", avatarResName)
                put("is_online", true)
                put("joined_at_epoch_ms", System.currentTimeMillis())
            }.toString()
            val res = supabaseClient.upsertRecord("server_members", payload, onConflict = "id", ignoreDuplicates = true)
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

    suspend fun syncRemoteServers() = withContext(ioDispatcher) {
        val result = supabaseClient.queryTable("servers?select=*")
        result.onSuccess { jsonStr ->
            runCatching {
                val array = org.json.JSONArray(jsonStr)
                val servers = mutableListOf<ServerEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val srv = ServerEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        category = obj.optString("category", "Community"),
                        ownerId = obj.optString("owner_id", ""),
                        avatarResName = obj.optString("avatar_res_name", "server_codesistency"),
                        bannerResName = obj.optString("banner_res_name").takeIf { it.isNotBlank() },
                        isVerified = obj.optBoolean("is_verified", false),
                        memberCount = obj.optInt("member_count", 1),
                        inviteCode = obj.optString("invite_code", ""),
                        createdAtEpochMs = obj.optLong("created_at_epoch_ms", System.currentTimeMillis())
                    )
                    servers.add(srv)
                }
                if (servers.isNotEmpty()) {
                    serverDao.insertServers(servers)
                }
            }
        }
    }

    suspend fun syncRemoteChannels(serverId: String) = withContext(ioDispatcher) {
        if (serverId.isBlank()) return@withContext
        val result = supabaseClient.queryTable("server_channels?server_id=eq.$serverId&order=order_index.asc")
        result.onSuccess { jsonStr ->
            runCatching {
                val array = org.json.JSONArray(jsonStr)
                val channels = mutableListOf<ServerChannelEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cType = when (obj.optString("channel_type", "text").lowercase()) {
                        "rules" -> ServerChannelType.RULES
                        "announcements" -> ServerChannelType.ANNOUNCEMENT
                        "voice" -> ServerChannelType.VOICE
                        else -> ServerChannelType.TEXT
                    }
                    val ch = ServerChannelEntity(
                        id = obj.getString("id"),
                        serverId = obj.getString("server_id"),
                        name = obj.getString("name"),
                        categoryName = obj.optString("category_name", "TEXT CHANNELS"),
                        channelType = cType.name,
                        topic = obj.optString("topic", ""),
                        unreadCount = if (obj.optBoolean("is_unread", false)) 1 else 0,
                        isPinned = false,
                        orderIndex = obj.optInt("order_index", 0)
                    )
                    channels.add(ch)
                }
                if (channels.isNotEmpty()) {
                    channelDao.insertChannels(channels)
                }
            }
        }
    }

    suspend fun syncRemoteRules(serverId: String) = withContext(ioDispatcher) {
        if (serverId.isBlank()) return@withContext
        val result = supabaseClient.queryTable("server_rules?server_id=eq.$serverId&order=rule_index.asc")
        result.onSuccess { jsonStr ->
            runCatching {
                val array = org.json.JSONArray(jsonStr)
                val rules = mutableListOf<ServerRuleEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val r = ServerRuleEntity(
                        id = obj.getString("id"),
                        serverId = obj.getString("server_id"),
                        ruleIndex = obj.getInt("rule_index"),
                        title = obj.getString("title"),
                        description = obj.optString("description", "")
                    )
                    rules.add(r)
                }
                if (rules.isNotEmpty()) {
                    ruleDao.insertRules(rules)
                }
            }
        }
    }

    suspend fun removeMember(serverId: String, userId: String) = withContext(ioDispatcher) {
        memberDao.removeMember(serverId, userId)
        supabaseClient?.let { client ->
            runCatching {
                val memberId = "${serverId}:${userId}"
                client.deleteRecord("server_members?id=eq.$memberId")
            }
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

    suspend fun syncRemoteMembers(serverId: String) = withContext(ioDispatcher) {
        val client = supabaseClient ?: return@withContext
        val cleanServerId = serverId.trim()
        if (cleanServerId.isBlank()) return@withContext
        val result = client.queryTable("server_members?server_id=eq.$cleanServerId")
        result.onSuccess { jsonStr ->
            runCatching {
                val array = org.json.JSONArray(jsonStr)
                val profileNames = profileNameResolver.resolve((0 until array.length()).map {
                    array.getJSONObject(it).optString("user_id")
                })
                val members = mutableListOf<ServerMemberEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val srvId = obj.optString("server_id")
                    val uId = obj.optString("user_id")
                    if (srvId.isNotBlank() && uId.isNotBlank()) {
                        val member = ServerMember(
                            userId = uId,
                            serverId = srvId,
                            displayName = profileNames[uId.lowercase()].orEmpty(),
                            handle = obj.optString("handle", "@student"),
                            role = runCatching { ServerMemberRole.valueOf(obj.optString("role", "MEMBER")) }.getOrDefault(ServerMemberRole.MEMBER),
                            roleTitle = obj.optString("role_title", "Member"),
                            roleColorHex = obj.optLong("role_color_hex", 0xFF5C6470),
                            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                            avatarResName = obj.optString("avatar_res_name", ""),
                            isOnline = obj.optBoolean("is_online", true),
                            customStatus = obj.optString("custom_status").takeIf { it.isNotBlank() },
                            joinedAtEpochMs = obj.optLong("joined_at_epoch_ms", System.currentTimeMillis()),
                            badge = obj.optString("badge").takeIf { it.isNotBlank() }
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
    }

    suspend fun syncRemoteMemberships(userId: String, email: String = "") = withContext(ioDispatcher) {
        val client = supabaseClient ?: return@withContext
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim()
        val filter = when {
            cleanUserId.isNotBlank() && cleanEmail.isNotBlank() -> "or=(user_id.eq.$cleanUserId,user_id.eq.$cleanEmail)"
            cleanUserId.isNotBlank() -> "user_id=eq.$cleanUserId"
            cleanEmail.isNotBlank() -> "user_id=eq.$cleanEmail"
            else -> return@withContext
        }
        val result = client.queryTable("server_members?$filter")
        result.onSuccess { jsonStr ->
            runCatching {
                val array = org.json.JSONArray(jsonStr)
                val profileNames = profileNameResolver.resolve((0 until array.length()).map {
                    array.getJSONObject(it).optString("user_id")
                })
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val srvId = obj.optString("server_id")
                    val uId = obj.optString("user_id")
                    if (srvId.isNotBlank() && uId.isNotBlank()) {
                        val member = ServerMember(
                            userId = uId,
                            serverId = srvId,
                            displayName = profileNames[uId.lowercase()].orEmpty(),
                            handle = obj.optString("handle", "@student"),
                            role = runCatching { ServerMemberRole.valueOf(obj.optString("role", "MEMBER")) }.getOrDefault(ServerMemberRole.MEMBER),
                            roleTitle = obj.optString("role_title", "Member"),
                            roleColorHex = obj.optLong("role_color_hex", 0xFF5C6470),
                            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                            avatarResName = obj.optString("avatar_res_name", ""),
                            isOnline = obj.optBoolean("is_online", true),
                            customStatus = obj.optString("custom_status").takeIf { it.isNotBlank() },
                            joinedAtEpochMs = obj.optLong("joined_at_epoch_ms", System.currentTimeMillis()),
                            badge = obj.optString("badge").takeIf { it.isNotBlank() }
                        )
                        memberDao.insertMember(ServerMemberEntity.fromDomain(member))
                    }
                }
            }
        }
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

    suspend fun syncRemoteFriends(userId: String, email: String = ""): List<FriendRelationship> = withContext(ioDispatcher) {
        val client = supabaseClient ?: return@withContext emptyList()
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim()
        if (cleanUserId.isBlank() && cleanEmail.isBlank()) return@withContext emptyList()

        val filter = when {
            cleanUserId.isNotBlank() && cleanEmail.isNotBlank() && cleanUserId != cleanEmail ->
                "user_id=in.($cleanUserId,$cleanEmail)"
            cleanUserId.isNotBlank() ->
                "user_id=eq.$cleanUserId"
            else ->
                "user_id=eq.$cleanEmail"
        }
        val result = client.queryTable("campus_friends?$filter&order=last_interaction_epoch_ms.desc")
        val mappedFriends = mutableListOf<FriendRelationship>()

        result.onSuccess { jsonStr ->
            runCatching {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val uId = obj.optString("user_id")
                    val fId = obj.optString("friend_user_id")
                    if (id.isNotBlank() && uId.isNotBlank() && fId.isNotBlank()) {
                        val rel = FriendRelationship(
                            id = id,
                            userId = uId,
                            friendUserId = fId,
                            friendDisplayName = "",
                            friendHandle = obj.optString("friend_handle", ""),
                            friendAvatarResName = obj.optString("friend_avatar_res_name", ""),
                            status = runCatching { FriendStatus.valueOf(obj.optString("status", FriendStatus.NONE.name)) }.getOrDefault(FriendStatus.NONE),
                            lastInteractionEpochMs = obj.optLong("last_interaction_epoch_ms", System.currentTimeMillis()),
                            dmChannelId = obj.optString("dm_channel_id").takeIf { it.isNotBlank() }
                        )
                        mappedFriends.add(rel)
                    }
                }
            }.onFailure { e ->
                android.util.Log.e("ServerRepository", "Error parsing remote friends: ${e.message}")
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

        // Mirror both sides to Supabase so the recipient device can see PENDING_RECEIVED
        val client = supabaseClient
        if (client != null) {
            val outbound = org.json.JSONObject().apply {
                put("id", id)
                put("user_id", cleanSender)
                put("friend_user_id", cleanFriend)
                put("friend_display_name", friendDisplayName)
                put("friend_handle", "")
                put("friend_avatar_res_name", "")
                put("status", FriendStatus.PENDING_SENT.name)
                put("last_interaction_epoch_ms", now)
            }.toString()
            val inbound = org.json.JSONObject().apply {
                put("id", reverseId)
                put("user_id", cleanFriend)
                put("friend_user_id", cleanSender)
                put("friend_display_name", senderDisplayName)
                put("friend_handle", "")
                put("friend_avatar_res_name", "")
                put("status", FriendStatus.PENDING_RECEIVED.name)
                put("last_interaction_epoch_ms", now)
            }.toString()

            val resOut = client.upsertRecord("campus_friends", outbound, onConflict = "id")
            val resIn = client.upsertRecord("campus_friends", inbound, onConflict = "id")
            if (resOut.isFailure || resIn.isFailure) {
                android.util.Log.e("ServerRepository", "campus_friends upsert failed: out=${resOut.exceptionOrNull()?.message}, in=${resIn.exceptionOrNull()?.message}")
            }

            CampusInboxNotifier(client).notifyFriendRequest(
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

        val client = supabaseClient
        if (client != null) {
            runCatching {
                val reversePatch = org.json.JSONObject().apply {
                    put("status", FriendStatus.FRIENDS.name)
                    put("dm_channel_id", dmId)
                    put("last_interaction_epoch_ms", now)
                    put("friend_display_name", userDisplayName)
                }.toString()
                val forwardPatch = org.json.JSONObject().apply {
                    put("status", FriendStatus.FRIENDS.name)
                    put("dm_channel_id", dmId)
                    put("last_interaction_epoch_ms", now)
                    put("friend_display_name", friendDisplayName)
                }.toString()
                client.updateRecord("campus_friends?id=eq.$id", forwardPatch)
                client.updateRecord("campus_friends?id=eq.$reverseId", reversePatch)
                CampusInboxNotifier(client).notifyFriendRequestAccepted(
                    recipientUserId = cleanFriend,
                    actorUserId = cleanUser
                )
            }
        }
        friendDisplayName
    }

    suspend fun declineOrRemoveFriend(
        userId: String,
        friendUserId: String
    ) = withContext(ioDispatcher) {
        val cleanUser = userId.trim()
        val cleanFriend = friendUserId.trim()
        if (cleanUser.isBlank() || cleanFriend.isBlank()) return@withContext

        val id = "$cleanUser:$cleanFriend"
        val reverseId = "$cleanFriend:$cleanUser"
        friendDao.deleteFriendRelationship(id)
        friendDao.deleteFriendRelationship(reverseId)
        val client = supabaseClient
        if (client != null) {
            runCatching {
                client.deleteRecord("campus_friends?id=eq.$id")
                client.deleteRecord("campus_friends?id=eq.$reverseId")
            }
        }
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
