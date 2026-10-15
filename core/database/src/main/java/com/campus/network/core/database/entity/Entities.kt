package com.campus.network.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory

@Entity(tableName = "flash_meets")
data class FlashMeetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val creatorName: String,
    val creatorAvatar: String?,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val attendeeCount: Int,
    val maxAttendees: Int
) {
    fun toDomain(): FlashMeet = FlashMeet(
        id = id,
        title = title,
        description = description,
        category = runCatching { MeetCategory.valueOf(categoryName) }.getOrDefault(MeetCategory.STUDY_JAM),
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        creatorName = creatorName,
        creatorAvatar = creatorAvatar,
        createdAtEpochMs = createdAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
        attendeeCount = attendeeCount,
        maxAttendees = maxAttendees
    )
}

@Entity(tableName = "academic_channels")
data class AcademicChannelEntity(
    @PrimaryKey val id: String,
    val courseCode: String,
    val courseTitle: String,
    val faculty: String,
    val department: String,
    val unreadCount: Int,
    val lastMessageSnippet: String,
    val lastMessageTime: String,
    val activeStudentsCount: Int,
    val isPinned: Boolean
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampEpochMs: Long,
    val isOptimistic: Boolean,
    val settlementPayloadJson: String? = null
)

@Entity(tableName = "market_items")
data class MarketItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Double,
    val currency: String,
    val category: String,
    val condition: String,
    val sellerName: String,
    val sellerRating: Float,
    val isCampusVerifiedSeller: Boolean,
    val campusHallLocation: String,
    val imageUrl: String?,
    val timestampMs: Long
)

@Entity(tableName = "micro_gigs")
data class MicroGigEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val payoutAmount: Double,
    val currency: String,
    val estimatedDuration: String,
    val posterName: String,
    val posterTrustScore: Int,
    val tag: String,
    val urgencyName: String,
    val spotsAvailable: Int,
    val timestampMs: Long
)
