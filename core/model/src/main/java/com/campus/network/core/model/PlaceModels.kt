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
    val imagesResNames: List<String> = emptyList(),
    val savedByUsers: List<SavedByUser> = emptyList(),
    val stats: PlaceStats = PlaceStats(saves = 0, stars = 0, likes = 0, dislikes = 0),
    val isSavedToTry: Boolean = false,
    val hasBeen: Boolean = false,
    val likedByFriend: String? = null
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
