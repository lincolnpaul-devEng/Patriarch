package com.campus.network.core.model

/**
 * Domain model representing a discoverable academic/campus place on the map
 * matching Patriarch campus network requirements with verified SafeTrade checkpoints.
 */
data class CampusPlace(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val subtitle: String,
    val address: String,
    val neighborhood: String,
    val campusRegion: String = "Nairobi",
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val savesCount: Int = 0,
    val rankingTag: String = "VERIFIED SPOT",
    val priceTier: String = "Free Access",
    val hoursText: String = "Open 24/7",
    val isOpenNow: Boolean = true,
    val securityLevel: String = "CCTV Monitored · 24/7 Guard",
    val isVerifiedSafeTrade: Boolean = false,
    val hasWifi: Boolean = true,
    val hasPowerOutlets: Boolean = true,
    val iconResName: String = "",
    val imageUrl: String? = null,
    val imagesResNames: List<String> = emptyList(),
    val savedByUsers: List<SavedByUser> = emptyList(),
    val stats: PlaceStats = PlaceStats(saves = 0, stars = 0, likes = 0, dislikes = 0),
    val isSavedToTry: Boolean = false,
    val hasBeen: Boolean = false,
    val likedByFriend: String? = null,
    val reviews: List<PlaceReview> = emptyList()
)

data class PlaceReview(
    val id: String,
    val placeId: String,
    val userId: String,
    val authorName: String,
    val comment: String,
    val createdAtEpochMs: Long
)

data class FriendLiveLocation(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val heading: Float? = null,
    val speedKmh: Float? = null,
    val stationarySinceEpochMs: Long = 0L,
    val privacyScope: LocationPrivacyScope = LocationPrivacyScope.VERIFIED_CAMPUS_PEERS,
    val expiresAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class CampusEventRecord(
    val id: String,
    val creatorId: String,
    val title: String,
    val hostName: String,
    val description: String,
    val locationName: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val startTimeIso: String,
    val startTimeEpochMs: Long,
    val category: String,
    val priceLabel: String?,
    val maxCapacity: Int,
    val attendeeCount: Int,
    val artworkKey: String,
    val bannerUrl: String? = null,
    val isUserRegistered: Boolean = false,
    val attendeeNames: List<String> = emptyList()
)

enum class PlaceCategory(val label: String) {
    TOP_PICKS("Top Picks"),
    STUDY_ZONES("Study Zones"),
    RENDEZVOUS("SafeTrade Spots"),
    ACADEMIC_LABS("Academic Labs"),
    GUILD_SERVICES("Campus Guild"),
    EVENTS("Events & Stages")
}

data class SavedByUser(
    val id: String,
    val username: String,
    val avatarResName: String,
    val isLiked: Boolean = true,
    val isBookmarked: Boolean = true
)

data class PlaceStats(
    val saves: Int,
    val stars: Int,
    val likes: Int,
    val dislikes: Int
)

enum class AudienceFilter(val label: String) {
    EVERYONE("everyone"),
    FRIENDS("friends"),
    FOLLOWING("following")
}
