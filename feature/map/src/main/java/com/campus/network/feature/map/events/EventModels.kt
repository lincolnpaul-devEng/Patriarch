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

val sampleCampusEvents = listOf(
    CampusEvent(
        id = "evt_hackathon",
        title = "GDSC Campus Hackathon 2026: AI & Distributed Networks",
        hostName = "Google Developer Student Club & Tech Guild",
        hostAvatarRes = R.drawable.avatar_andrew,
        artworkRes = R.drawable.event_thumb_founders,
        dateTimeFormatted = "Today, 6:00 PM EAT",
        locationName = "Innovation Hub Auditorium",
        fullAddress = "Innovation Complex, Ground Floor, Strathmore",
        statusBadge = EventBadgeType.GOING,
        priceLabel = "Free Admission",
        isUserRegistered = true,
        latitude = -1.3078,
        longitude = 36.8121,
        guestCount = 186,
        description = "48-hour student build marathon focusing on decentralized campus services, automated peer verification, and agentic workflows. Mentors from Safaricom and Google on site.",
        dateSection = "Your Events"
    ),
    CampusEvent(
        id = "evt_guild_debate",
        title = "Student Guild Presidential Debate & Campus Townhall",
        hostName = "Campus Electoral Commission & Student Guild",
        hostAvatarRes = R.drawable.avatar_junebog,
        artworkRes = R.drawable.event_thumb_paws,
        dateTimeFormatted = "Tomorrow, 2:00 PM EAT",
        locationName = "Student Guild Hub (Table 4)",
        fullAddress = "Central Campus Spine & Guild Pavilion",
        statusBadge = EventBadgeType.WAITLISTED,
        priceLabel = null,
        latitude = -1.3082,
        longitude = 36.8118,
        guestCount = 320,
        description = "Live presidential debate featuring the 2026 Student Guild candidates addressing campus commerce security, bursary access, and academic peer support.",
        dateSection = "Your Events"
    ),
    CampusEvent(
        id = "evt_safetrade_expo",
        title = "SafeTrade Tech & Hardware Swap Meet",
        hostName = "Campus SafeTrade Society",
        hostAvatarRes = R.drawable.avatar_clin,
        artworkRes = R.drawable.event_thumb_eclipse,
        dateTimeFormatted = "Friday, 11:00 AM EAT",
        locationName = "Phase 1 Science & Robotics Labs Entrance",
        fullAddress = "South Quad Science Complex, Nairobi",
        statusBadge = EventBadgeType.INVITED,
        priceLabel = "Free Swap",
        latitude = -1.3105,
        longitude = 36.8115,
        guestCount = 95,
        description = "Physical hardware verification day. Bring scientific calculators, microcontrollers, lab coats, and engineering textbooks for verified peer trade and instant SafePay handovers.",
        dateSection = "Your Events"
    ),
    CampusEvent(
        id = "evt_study_night",
        title = "Finals Week 24/7 Study All-Nighter & Free Coffee",
        hostName = "University Library Board",
        hostAvatarRes = R.drawable.avatar_eliza,
        artworkRes = R.drawable.event_thumb_afterhours,
        dateTimeFormatted = "Everyday this week, 8:00 PM - 6:00 AM EAT",
        locationName = "University Main Library (Level 3)",
        fullAddress = "Level 3 Silent Zone, Central Library",
        statusBadge = null,
        priceLabel = "Free Access",
        latitude = -1.3087,
        longitude = 36.8129,
        guestCount = 450,
        description = "Extended 24/7 hours across all library floors. Complimentary hot coffee and snacks provided at the foyer. Group discussion pods open for reservation.",
        dateSection = "Picked for You"
    ),
    CampusEvent(
        id = "evt_coastal_summit",
        title = "Coastal & Lake Region Student Leadership Summit",
        hostName = "All-Campus Inter-University Guild",
        hostAvatarRes = R.drawable.avatar_me,
        artworkRes = R.drawable.event_hero_trees,
        dateTimeFormatted = "Saturday, 9:00 AM EAT",
        locationName = "Coastal Campus Pavilion",
        fullAddress = "Nyali Campus Complex, Ocean Wing, Mombasa",
        statusBadge = null,
        priceLabel = "Free Entry",
        isFeatured = true,
        featuredLabel = "Featured Campus Event",
        latitude = -4.0435,
        longitude = 39.6682,
        guestCount = 210,
        description = "Annual inter-campus symposium bringing together student delegates from Nairobi, Mombasa, Kisumu, and Eldoret to discuss student welfare and peer entrepreneurship.",
        isUserRegistered = false,
        dateSection = "Picked for You"
    )
)
