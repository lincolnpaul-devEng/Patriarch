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
    val isOptimistic: Boolean = false,
    val settlementPayloadJson: String? = null,
    val imageUri: String? = null,
    val imageUrl: String? = null,
    val gifUrl: String? = null,
    val replyToText: String? = null,
    val isMine: Boolean = false,
    val isDelivered: Boolean = true,
    val isRead: Boolean = true
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
    val timestampMs: Long,
    val description: String = "",
    val pickupSpotName: String = "Quad Library Ground Floor Lockers",
    val pickupSecurityLevel: String = "CCTV Monitored Safe Zone",
    val pickupLatitude: Double = -1.3090,
    val pickupLongitude: Double = 36.8120,
    val pickupTiming: String = "Immediate (within 1 hr)",
    val payoutMethod: String = "Campus SafePay Escrow",
    val requiresQrReleaseCode: Boolean = true,
    val qrReleaseCode: String = "CP-849201"
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
    val timestampMs: Long,
    val bannerUrl: String? = null
)
@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val id: String,
    val broadcasterId: String,
    val broadcasterName: String,
    val broadcasterHandle: String,
    val broadcasterAvatarRes: Int = 0,
    val broadcasterAvatarUrl: String? = null,
    val title: String,
    val description: String,
    val category: String,
    val audience: String = "CAMPUS",
    val isLive: Boolean = true,
    val streamPlaybackUrl: String? = null,
    val backdropImageRes: Int = 0,
    val backdropImageUrl: String? = null,
    val viewerCount: Int = 0,
    val peakViewers: Int = 0,
    val likeCount: Int = 0,
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val endedAtEpochMs: Long? = null,
    val durationSeconds: Int = 0,
    val serverId: String? = null
)

@Entity(tableName = "streamer_profiles")
data class StreamerProfileEntity(
    @PrimaryKey val studentId: String,
    val location: String,
    val joinedDate: String,
    val hoursLive: Int,
    val followersCount: String,
    val bestDay: String,
    val streamsPerWeek: Int,
    val topTopicsCsv: String,
    val weeklyActivityCsv: String
)

@Entity(tableName = "stream_comments")
data class StreamCommentEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val senderAvatarRes: Int = 0,
    val commentText: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

