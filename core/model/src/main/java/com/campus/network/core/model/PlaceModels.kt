package com.campus.network.core.model

/**
 * Domain model representing a discoverable place on the map
 * matching the Corner exploration design with Patriarch branding.
 */
data class CampusPlace(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val subtitle: String,
    val address: String,
    val neighborhood: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val savesCount: Int,
    val rankingTag: String,
    val priceTier: String,
    val hoursText: String,
    val isOpenNow: Boolean,
    val iconResName: String,
    val imagesResNames: List<String>,
    val savedByUsers: List<SavedByUser>,
    val stats: PlaceStats,
    val isSavedToTry: Boolean = false,
    val hasBeen: Boolean = false,
    val likedByFriend: String? = null
)

enum class PlaceCategory(val label: String, val iconResName: String) {
    TOP_PICKS("top picks", "cat_top_picks"),
    EAT("eat", "cat_eat"),
    CAFES("cafes", "cat_cafes"),
    BARS("bars", "cat_bars"),
    EVENTS("events", "cat_events"),
    GO_OUT("go out", "cat_go_out"),
    SHOPS("shops", "cat_shops")
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
