package com.campus.network.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.campus.network.core.database.entity.AcademicChannelEntity
import com.campus.network.core.database.entity.ChatMessageEntity
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamerProfileEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashMeetDao {
    @Query("SELECT * FROM flash_meets WHERE expiresAtEpochMs > :currentTime ORDER BY createdAtEpochMs DESC")
    fun getLiveMeets(currentTime: Long): Flow<List<FlashMeetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeets(meets: List<FlashMeetEntity>)

    @Query("DELETE FROM flash_meets WHERE expiresAtEpochMs <= :currentTime")
    suspend fun purgeExpired(currentTime: Long)
}

@Dao
interface AcademicChannelDao {
    @Query("SELECT * FROM academic_channels ORDER BY faculty, department, courseCode ASC")
    fun getAllChannels(): Flow<List<AcademicChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<AcademicChannelEntity>)

    @Query("UPDATE academic_channels SET unreadCount = 0 WHERE id = :channelId")
    suspend fun markChannelRead(channelId: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY timestampEpochMs ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimisticMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isOptimistic = 0 WHERE id = :messageId")
    suspend fun markMessageSynced(messageId: String)
}

@Dao
interface MarketItemDao {
    @Query("SELECT * FROM market_items ORDER BY timestampMs DESC")
    fun getAllMarketItems(): Flow<List<MarketItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MarketItemEntity>)
}

@Dao
interface MicroGigDao {
    @Query("SELECT * FROM micro_gigs ORDER BY timestampMs DESC")
    fun getAllGigs(): Flow<List<MicroGigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGigs(gigs: List<MicroGigEntity>)
}
@Dao
interface LiveStreamDao {
    @Query("SELECT * FROM live_streams ORDER BY isLive DESC, startedAtEpochMs DESC")
    fun getAllStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE isLive = 1 ORDER BY startedAtEpochMs DESC")
    fun getActiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE id = :streamId LIMIT 1")
    fun getStreamById(streamId: String): Flow<LiveStreamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: LiveStreamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<LiveStreamEntity>)

    @Query("UPDATE live_streams SET isLive = 0, endedAtEpochMs = :endedAt, durationSeconds = :duration, peakViewers = :peak, likeCount = :likes WHERE id = :streamId")
    suspend fun endStream(streamId: String, endedAt: Long, duration: Int, peak: Int, likes: Int)

    @Query("UPDATE live_streams SET viewerCount = :viewers, peakViewers = MAX(peakViewers, :viewers) WHERE id = :streamId")
    suspend fun updateViewerCount(streamId: String, viewers: Int)

    @Query("UPDATE live_streams SET likeCount = likeCount + 1 WHERE id = :streamId")
    suspend fun incrementLikes(streamId: String)
}

@Dao
interface StreamerProfileDao {
    @Query("SELECT * FROM streamer_profiles WHERE studentId = :studentId LIMIT 1")
    fun getProfile(studentId: String): Flow<StreamerProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StreamerProfileEntity)
}

@Dao
interface StreamCommentDao {
    @Query("SELECT * FROM stream_comments WHERE streamId = :streamId ORDER BY timestampEpochMs ASC")
    fun getCommentsForStream(streamId: String): Flow<List<StreamCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: StreamCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<StreamCommentEntity>)
}

