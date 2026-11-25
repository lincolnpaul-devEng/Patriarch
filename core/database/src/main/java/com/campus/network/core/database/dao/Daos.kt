package com.campus.network.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.campus.network.core.database.entity.AcademicChannelEntity
import com.campus.network.core.database.entity.CampusEventEntity
import com.campus.network.core.database.entity.CampusPlaceEntity
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

    @Query("UPDATE flash_meets SET syncStatus = :status WHERE id = :meetId AND syncId = :syncId")
    suspend fun updateSyncStatus(meetId: String, syncId: String?, status: String)

    @Query("SELECT * FROM flash_meets WHERE syncStatus = 'PENDING' ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingMeets(): List<FlashMeetEntity>

    @Query("SELECT * FROM flash_meets WHERE syncStatus = 'FAILED' ORDER BY createdAtEpochMs ASC")
    suspend fun getFailedMeets(): List<FlashMeetEntity>

    @Query("DELETE FROM flash_meets WHERE id NOT IN (:validIds) AND syncStatus != 'PENDING'")
    suspend fun deleteGhostMeets(validIds: List<String>)

    @Query("DELETE FROM flash_meets WHERE syncStatus != 'PENDING'")
    suspend fun clearSyncedMeets()

    @Query("UPDATE flash_meets SET attendeeCount = :count WHERE id = :meetId")
    suspend fun updateAttendeeCount(meetId: String, count: Int)

    @Query("DELETE FROM flash_meets WHERE id = :meetId")
    suspend fun deleteMeetById(meetId: String)
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

    @Query("SELECT * FROM chat_messages ORDER BY timestampEpochMs DESC")
    fun observeAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT DISTINCT channelId FROM chat_messages")
    suspend fun getDistinctChannelIds(): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM chat_messages
        WHERE channelId = :channelId AND isMine = 0 AND isRead = 0
        """
    )
    suspend fun countUnreadForChannel(channelId: String): Int

    @Query(
        """
        UPDATE chat_messages SET isRead = 1
        WHERE channelId = :channelId AND isMine = 0 AND isRead = 0
        """
    )
    suspend fun markChannelMessagesRead(channelId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimisticMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isOptimistic = 0 WHERE id = :messageId")
    suspend fun markMessageSynced(messageId: String)

    @Query("UPDATE chat_messages SET isOptimistic = 0, isDelivered = 1 WHERE id = :messageId")
    suspend fun markMessageDelivered(messageId: String)

    @Query("SELECT * FROM chat_messages WHERE isOptimistic = 1 OR isDelivered = 0 ORDER BY timestampEpochMs ASC")
    suspend fun getPendingMessages(): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chat_messages WHERE channelId = :channelId")
    suspend fun clearChannelMessages(channelId: String)

    @Query("UPDATE chat_messages SET syncStatus = :status, syncId = :syncId WHERE id = :messageId")
    suspend fun updateChatMessageSyncStatus(messageId: String, status: String, syncId: String?)

    @Query("SELECT * FROM chat_messages WHERE syncStatus = 'FAILED' ORDER BY timestampEpochMs ASC")
    suspend fun getFailedChatMessages(): List<ChatMessageEntity>

}

@Dao
interface MarketItemDao {
    @Query("SELECT * FROM market_items ORDER BY timestampMs DESC")
    fun getAllMarketItems(): Flow<List<MarketItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MarketItemEntity>)

    @Query("UPDATE market_items SET syncStatus = :status WHERE id = :itemId AND syncId = :syncId")
    suspend fun updateSyncStatus(itemId: String, syncId: String?, status: String)

    @Query("SELECT * FROM market_items WHERE syncStatus = 'PENDING' ORDER BY timestampMs ASC")
    suspend fun getPendingItems(): List<MarketItemEntity>

    @Query("SELECT * FROM market_items WHERE syncStatus = 'FAILED' ORDER BY timestampMs ASC")
    suspend fun getFailedItems(): List<MarketItemEntity>

    @Query("DELETE FROM market_items WHERE id NOT IN (:validIds) AND syncStatus != 'PENDING'")
    suspend fun deleteGhostItems(validIds: List<String>)

    @Query("DELETE FROM market_items WHERE syncStatus != 'PENDING'")
    suspend fun clearSyncedItems()

    @Query("SELECT * FROM market_items WHERE sellerUserId = :sellerId AND syncStatus != 'PENDING' ORDER BY timestampMs DESC")
    suspend fun getItemsBySeller(sellerId: String): List<MarketItemEntity>

    @Query("DELETE FROM market_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("UPDATE market_items SET status = :status WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: String, status: String)

    @Query("SELECT * FROM market_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): MarketItemEntity?
}

@Dao
interface MicroGigDao {
    @Query("SELECT * FROM micro_gigs ORDER BY timestampMs DESC")
    fun getAllGigs(): Flow<List<MicroGigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGigs(gigs: List<MicroGigEntity>)

    @Query("UPDATE micro_gigs SET syncStatus = :status WHERE id = :gigId AND syncId = :syncId")
    suspend fun updateSyncStatus(gigId: String, syncId: String?, status: String)

    @Query("SELECT * FROM micro_gigs WHERE syncStatus = 'PENDING' ORDER BY timestampMs ASC")
    suspend fun getPendingGigs(): List<MicroGigEntity>

    @Query("SELECT * FROM micro_gigs WHERE syncStatus = 'FAILED' ORDER BY timestampMs ASC")
    suspend fun getFailedGigs(): List<MicroGigEntity>

    @Query("DELETE FROM micro_gigs WHERE id NOT IN (:validIds) AND syncStatus != 'PENDING'")
    suspend fun deleteGhostGigs(validIds: List<String>)

    @Query("DELETE FROM micro_gigs WHERE syncStatus != 'PENDING'")
    suspend fun clearSyncedGigs()
}
@Dao
interface LiveStreamDao {
    @Query("SELECT * FROM live_streams ORDER BY isLive DESC, startedAtEpochMs DESC")
    fun getAllStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE isLive = 1 ORDER BY startedAtEpochMs DESC")
    fun getActiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE id = :streamId LIMIT 1")
    fun getStreamById(streamId: String): Flow<LiveStreamEntity?>

    @Query("SELECT * FROM live_streams WHERE id = :streamId LIMIT 1")
    suspend fun getStreamByIdSync(streamId: String): LiveStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: LiveStreamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<LiveStreamEntity>)

    @Query("UPDATE live_streams SET syncStatus = :status WHERE id = :streamId AND syncId = :syncId")
    suspend fun updateSyncStatus(streamId: String, syncId: String?, status: String)

    @Query("SELECT * FROM live_streams WHERE syncStatus = 'PENDING' ORDER BY startedAtEpochMs ASC")
    suspend fun getPendingStreams(): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE syncStatus = 'FAILED' ORDER BY startedAtEpochMs ASC")
    suspend fun getFailedStreams(): List<LiveStreamEntity>

    @Query("DELETE FROM live_streams WHERE id NOT IN (:validIds) AND syncStatus != 'PENDING'")
    suspend fun deleteGhostStreams(validIds: List<String>)

    @Query("DELETE FROM live_streams WHERE syncStatus != 'PENDING'")
    suspend fun clearSyncedStreams()

    @Query("UPDATE live_streams SET isLive = 0, endedAtEpochMs = :endedAt, durationSeconds = :duration, peakViewers = :peak, likeCount = :likes WHERE id = :streamId")
    suspend fun endStream(streamId: String, endedAt: Long, duration: Int, peak: Int, likes: Int)

    @Query("UPDATE live_streams SET viewerCount = :viewers, peakViewers = MAX(peakViewers, :viewers) WHERE id = :streamId")
    suspend fun updateViewerCount(streamId: String, viewers: Int)

    @Query("UPDATE live_streams SET likeCount = likeCount + 1 WHERE id = :streamId")
    suspend fun incrementLikes(streamId: String)

    @Query("UPDATE live_streams SET likeCount = :likes WHERE id = :streamId")
    suspend fun updateLikeCount(streamId: String, likes: Int)

    @Query("DELETE FROM live_streams WHERE id = :streamId")
    suspend fun deleteStreamById(streamId: String)
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

    @Query("UPDATE stream_comments SET syncStatus = :status WHERE id = :commentId AND syncId = :syncId")
    suspend fun updateSyncStatus(commentId: String, syncId: String?, status: String)

    @Query("SELECT * FROM stream_comments WHERE syncStatus = 'PENDING' ORDER BY timestampEpochMs ASC")
    suspend fun getPendingComments(): List<StreamCommentEntity>

    @Query("SELECT * FROM stream_comments WHERE syncStatus = 'FAILED' ORDER BY timestampEpochMs ASC")
    suspend fun getFailedComments(): List<StreamCommentEntity>

    @Query("DELETE FROM stream_comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: String)
}

@Dao
interface CampusPlaceDao {
    @Query("SELECT * FROM campus_places ORDER BY name ASC")
    fun getAllPlaces(): Flow<List<CampusPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<CampusPlaceEntity>)

    @Query("DELETE FROM campus_places WHERE id NOT IN (:validIds)")
    suspend fun deleteGhostPlaces(validIds: List<String>)

    @Query("DELETE FROM campus_places")
    suspend fun clearAllPlaces()
}

@Dao
interface CampusEventDao {
    @Query("SELECT * FROM campus_events ORDER BY startTimeEpochMs ASC")
    fun getAllEvents(): Flow<List<CampusEventEntity>>

    @Query("SELECT * FROM campus_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventByIdSync(eventId: String): CampusEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CampusEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CampusEventEntity>)

    @Query("UPDATE campus_events SET attendeeCount = :count WHERE id = :eventId")
    suspend fun updateAttendeeCount(eventId: String, count: Int)

    @Query("UPDATE campus_events SET syncStatus = :status WHERE id = :eventId AND syncId = :syncId")
    suspend fun updateSyncStatus(eventId: String, syncId: String?, status: String)

    @Query("SELECT * FROM campus_events WHERE syncStatus = 'PENDING' ORDER BY startTimeEpochMs ASC")
    suspend fun getPendingEvents(): List<CampusEventEntity>

    @Query("SELECT * FROM campus_events WHERE syncStatus = 'FAILED' ORDER BY startTimeEpochMs ASC")
    suspend fun getFailedEvents(): List<CampusEventEntity>

    @Query("DELETE FROM campus_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("DELETE FROM campus_events WHERE id NOT IN (:validIds) AND syncStatus != 'PENDING'")
    suspend fun deleteGhostEvents(validIds: List<String>)

    @Query("DELETE FROM campus_events WHERE syncStatus != 'PENDING'")
    suspend fun clearSyncedEvents()
}
