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
    val latitude: Double = 34.1458,
    val longitude: Double = -118.1404,
    val guestCount: Int = 48,
    val dateSection: String = "Your Events"
)

val sampleCampusEvents = listOf(
    CampusEvent(
        id = "evt_founders",
        title = "Extraordinary Founders Dinner (hosted by Andrew Yeung)",
        hostName = "Andrew's Yeung's Tech Events",
        hostAvatarRes = R.drawable.avatar_andrew,
        artworkRes = R.drawable.event_thumb_founders,
        dateTimeFormatted = "Today, 6:00 PM GMT-7",
        locationName = "Showplace Square",
        fullAddress = "100 Hooper St, San Francisco, CA 94107",
        statusBadge = EventBadgeType.WAITLISTED,
        priceLabel = null,
        description = "An intimate, curated gathering of high-growth founders, angel investors, and innovators. Join us for a private dinner discussion on scaling infrastructure and campus venture building.",
        dateSection = "Your Events"
    ),
    CampusEvent(
        id = "evt_paws",
        title = "Paws, People & Purpose",
        hostName = "Dogs Only Social Club and Big Dog...",
        hostAvatarRes = R.drawable.avatar_junebog,
        artworkRes = R.drawable.event_thumb_paws,
        dateTimeFormatted = "18 Jul, 2:00 PM GMT-7",
        locationName = "GoodPeople",
        fullAddress = "11459 Santa Monica Blvd, Los Angeles, CA 90025",
        statusBadge = EventBadgeType.GOING,
        isUserRegistered = true,
        priceLabel = null,
        description = "A warm community gathering connecting animal welfare advocates, rescue teams, and dog lovers across campus. Complimentary artisan treats and adoption drives.",
        dateSection = "Your Events"
    ),
    CampusEvent(
        id = "evt_eclipse",
        title = "Solar Eclipse Viewing Party",
        hostName = "Alex Smith",
        hostAvatarRes = R.drawable.avatar_clin,
        artworkRes = R.drawable.event_thumb_eclipse,
        dateTimeFormatted = "22 Jul, 1:30 PM GMT-7",
        locationName = "1226 University Dr",
        fullAddress = "1226 University Dr, Menlo Park, CA 94025",
        statusBadge = EventBadgeType.INVITED,
        priceLabel = null,
        description = "Join the Astronomy and Physics Circle for a panoramic open-air eclipse observation. Certified solar optical glasses and high-magnification telescopes provided.",
        dateSection = "Your Events"
    ),
    CampusEvent(
        id = "evt_afterhours",
        title = "after hours: a happy hour for la's creatives and founders",
        hostName = "Creative Coffee Club",
        hostAvatarRes = R.drawable.avatar_eliza,
        artworkRes = R.drawable.event_thumb_afterhours,
        dateTimeFormatted = "5:30 PM GMT-7",
        locationName = "Tu Madre - West Hollywood",
        fullAddress = "1111 N Hayworth Ave, West Hollywood, CA 90046",
        statusBadge = null,
        priceLabel = "US$10",
        description = "Unwind after classes with campus designers, developers, and product creators. Enjoy craft beverages and open conversations on design systems and creative ventures.",
        dateSection = "Tomorrow / Wednesday"
    ),
    CampusEvent(
        id = "evt_trees",
        title = "An Evening for the Trees with Julia Butterfly Hill",
        hostName = "Julia Butterfly Hill",
        hostAvatarRes = R.drawable.avatar_me,
        artworkRes = R.drawable.event_hero_trees,
        dateTimeFormatted = "Wed, 8 Jul, 6:30 PM - 8:30 PM GMT-7",
        locationName = "Pasadena Presbyterian Church",
        fullAddress = "585 E Colorado Blvd, Pasadena, CA 91101, USA",
        statusBadge = null,
        priceLabel = "US$5",
        isFeatured = true,
        featuredLabel = "Featured in Los Angeles",
        description = "Join legendary activist Julia Butterfly Hill for an inspiring evening discussing conscious environmental leadership, ancient redwood preservation, and everyday sustainable action. Features an acoustic musical reflection and open Q&A.",
        isUserRegistered = false,
        latitude = 34.1458,
        longitude = -118.1404,
        guestCount = 142,
        dateSection = "Picked for You"
    )
)
