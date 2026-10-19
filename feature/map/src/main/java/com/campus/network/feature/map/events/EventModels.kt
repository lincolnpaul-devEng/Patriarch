package com.campus.network.feature.map.events

import androidx.compose.ui.graphics.Color
import com.campus.network.core.ui.theme.BrandGoldSoft
import com.campus.network.core.ui.theme.BrandGoldText
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.BrandGreenSoft
import com.campus.network.feature.map.R

enum class MapNavigationMode {
    MAP,
    EVENTS_LIST,
    EVENT_DETAIL
}

enum class EventBadgeType(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    WAITLISTED(
        label = "Waitlisted",
        backgroundColor = BrandGoldSoft,
        textColor = BrandGoldText
    ),
    GOING(
        label = "Going",
        backgroundColor = BrandGreenSoft,
        textColor = BrandGreen
    ),
    INVITED(
        label = "Invited",
        backgroundColor = Color(0xFFEDE9FE),
        textColor = Color(0xFF7C3AED)
    )
}

data class CampusEvent(
    val id: String,
    val title: String,
    val hostName: String,
    val hostAvatarRes: Int,
    val artworkRes: Int,
    val dateTimeFormatted: String,
    val locationName: String,
    val fullAddress: String,
    val statusBadge: EventBadgeType? = null,
    val priceLabel: String? = null,
    val isFeatured: Boolean = false,
    val featuredLabel: String? = null,
    val description: String = "",
    val isUserRegistered: Boolean = false,
    val category: String = "Campus",
    val latitude: Double = -1.3078,
    val longitude: Double = 36.8121,
    val guestCount: Int = 48,
    val dateSection: String = "Your Events"
)

val sampleCampusEvents = emptyList<CampusEvent>()

