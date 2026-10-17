package com.campus.network.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campus.network.core.database.entity.FriendEntity
import com.campus.network.core.database.entity.ServerChannelEntity
import com.campus.network.core.database.entity.ServerEntity
import com.campus.network.core.database.entity.ServerInviteEntity
import com.campus.network.core.database.entity.ServerMemberEntity
import com.campus.network.core.database.entity.ServerRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY createdAtEpochMs ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :serverId LIMIT 1")
    fun getServerById(serverId: String): Flow<ServerEntity?>

    @Query("SELECT * FROM servers WHERE id = :serverId LIMIT 1")
    suspend fun getServerByIdSync(serverId: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Query("UPDATE servers SET inviteCode = :inviteCode WHERE id = :serverId")
    suspend fun updateInviteCode(serverId: String, inviteCode: String)

    @Query("UPDATE servers SET memberCount = memberCount + 1 WHERE id = :serverId")
    suspend fun incrementMemberCount(serverId: String)

    @Query("DELETE FROM servers WHERE id = :serverId")
    suspend fun deleteServer(serverId: String)
}

@Dao
interface ServerChannelDao {
    @Query("SELECT * FROM server_channels WHERE serverId = :serverId ORDER BY orderIndex ASC, name ASC")
    fun getChannelsForServer(serverId: String): Flow<List<ServerChannelEntity>>

    @Query("SELECT * FROM server_channels WHERE id = :channelId LIMIT 1")
    fun getChannelById(channelId: String): Flow<ServerChannelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ServerChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ServerChannelEntity>)

    @Query("DELETE FROM server_channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Query("UPDATE server_channels SET unreadCount = 0 WHERE id = :channelId")
    suspend fun markChannelRead(channelId: String)
}

@Dao
interface ServerRuleDao {
    @Query("SELECT * FROM server_rules WHERE serverId = :serverId ORDER BY ruleIndex ASC")
    fun getRulesForServer(serverId: String): Flow<List<ServerRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ServerRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ServerRuleEntity>)

    @Query("DELETE FROM server_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: String)
}

@Dao
interface ServerMemberDao {
    @Query("SELECT * FROM server_members WHERE serverId = :serverId ORDER BY isOnline DESC, role ASC, displayName ASC")
    fun getMembersForServer(serverId: String): Flow<List<ServerMemberEntity>>

    @Query("SELECT * FROM server_members WHERE serverId = :serverId AND userId = :userId LIMIT 1")
    fun getMember(serverId: String, userId: String): Flow<ServerMemberEntity?>

    @Query("SELECT * FROM server_members WHERE serverId = :serverId AND userId = :userId LIMIT 1")
    suspend fun getMemberSync(serverId: String, userId: String): ServerMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ServerMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ServerMemberEntity>)

    @Query("UPDATE server_members SET role = :role, roleTitle = :roleTitle, roleColorHex = :roleColorHex WHERE serverId = :serverId AND userId = :userId")
    suspend fun updateMemberRole(serverId: String, userId: String, role: String, roleTitle: String, roleColorHex: Long)

    @Query("DELETE FROM server_members WHERE serverId = :serverId AND userId = :userId")
    suspend fun removeMember(serverId: String, userId: String)
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends WHERE userId = :userId ORDER BY lastInteractionEpochMs DESC")
    fun getFriendsForUser(userId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE userId = :userId AND friendUserId = :friendUserId LIMIT 1")
    fun getFriendRelationship(userId: String, friendUserId: String): Flow<FriendEntity?>

    @Query("SELECT * FROM friends WHERE userId = :userId AND friendUserId = :friendUserId LIMIT 1")
    suspend fun getFriendRelationshipSync(userId: String, friendUserId: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>)

    @Query("UPDATE friends SET status = :status, lastInteractionEpochMs = :lastInteractionMs WHERE id = :id")
    suspend fun updateFriendStatus(id: String, status: String, lastInteractionMs: Long)

    @Query("UPDATE friends SET dmChannelId = :dmChannelId WHERE id = :id")
    suspend fun updateDmChannelId(id: String, dmChannelId: String)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriendRelationship(id: String)
}

@Dao
interface ServerInviteDao {
    @Query("SELECT * FROM server_invites WHERE code = :code LIMIT 1")
    fun getInviteByCode(code: String): Flow<ServerInviteEntity?>

    @Query("SELECT * FROM server_invites WHERE code = :code LIMIT 1")
    suspend fun getInviteByCodeSync(code: String): ServerInviteEntity?

    @Query("SELECT * FROM server_invites WHERE serverId = :serverId ORDER BY expiresAtEpochMs ASC")
    fun getInvitesForServer(serverId: String): Flow<List<ServerInviteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(invite: ServerInviteEntity)

    @Query("UPDATE server_invites SET usesCount = usesCount + 1 WHERE code = :code")
    suspend fun incrementInviteUsage(code: String)

    @Query("UPDATE server_invites SET isRevoked = 1 WHERE code = :code")
    suspend fun revokeInvite(code: String)
}
