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
