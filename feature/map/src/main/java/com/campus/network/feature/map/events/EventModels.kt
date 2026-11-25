package com.campus.network.feature.map.events

import androidx.compose.ui.graphics.Color
import com.campus.network.core.ui.theme.BrandGoldSoft
import com.campus.network.core.ui.theme.BrandGoldText
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.BrandGreenSoft
import com.campus.network.feature.map.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

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
    val creatorId: String = "",
    val title: String,
    val hostName: String,
    val hostAvatarRes: Int = 0,
    val artworkRes: Int = R.drawable.event_thumb_founders,
    val artworkKey: String = artworkKeyForRes(artworkRes),
    val bannerUrl: String? = null,
    val dateTimeFormatted: String,
    val startTimeIso: String = "",
    val startTimeEpochMs: Long = 0L,
    val locationName: String,
    val fullAddress: String,
    val statusBadge: EventBadgeType? = null,
    val priceLabel: String? = null,
    val maxCapacity: Int = 100,
    val isFeatured: Boolean = false,
    val featuredLabel: String? = null,
    val description: String = "",
    val isUserRegistered: Boolean = false,
    val category: String = "Campus",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val guestCount: Int = 1,
    val attendeeNames: List<String> = emptyList(),
    val dateSection: String = "Your Events"
)

val sampleCampusEvents = emptyList<CampusEvent>()

fun artworkResForKey(key: String?): Int {
    return when (key?.trim()) {
        "event_thumb_eclipse" -> R.drawable.event_thumb_eclipse
        "event_thumb_paws" -> R.drawable.event_thumb_paws
        "event_thumb_afterhours" -> R.drawable.event_thumb_afterhours
        "event_hero_trees" -> R.drawable.event_hero_trees
        else -> R.drawable.event_thumb_founders
    }
}

fun artworkKeyForRes(resId: Int): String {
    return when (resId) {
        R.drawable.event_thumb_eclipse -> "event_thumb_eclipse"
        R.drawable.event_thumb_paws -> "event_thumb_paws"
        R.drawable.event_thumb_afterhours -> "event_thumb_afterhours"
        R.drawable.event_hero_trees -> "event_hero_trees"
        else -> "event_thumb_founders"
    }
}

fun computePresetStartTimeIso(preset: String, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val now = ZonedDateTime.now(zoneId)
    val today = now.toLocalDate()
    val target: ZonedDateTime = when (preset) {
        "Today, 6:00 PM" -> {
            val sixPm = today.atTime(18, 0).atZone(zoneId)
            if (now.isBefore(sixPm)) sixPm else now.plusHours(1)
        }
        "Tomorrow, 2:00 PM" -> today.plusDays(1).atTime(14, 0).atZone(zoneId)
        "Friday, 11:00 AM" -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            .atTime(11, 0)
            .atZone(zoneId)
            .let { if (it.isBefore(now)) it.plusWeeks(1) else it }
        "This Weekend" -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            .atTime(10, 0)
            .atZone(zoneId)
            .let { if (it.isBefore(now)) it.plusWeeks(1) else it }
        else -> now.plusHours(2)
    }
    return target.toInstant().toString()
}

fun formatEventDateTime(
    startTimeIso: String,
    epochMs: Long = 0L,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val instant = runCatching { Instant.parse(startTimeIso) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(startTimeIso).toInstant() }.getOrNull()
        ?: if (epochMs > 0L) Instant.ofEpochMilli(epochMs) else return startTimeIso.ifBlank { "Upcoming" }

    val zdt = instant.atZone(zoneId)
    val today = LocalDate.now(zoneId)
    val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    return when (zdt.toLocalDate()) {
        today -> "Today, ${zdt.format(timeFmt)}"
        today.plusDays(1) -> "Tomorrow, ${zdt.format(timeFmt)}"
        else -> "${zdt.format(dateFmt)} · ${zdt.format(timeFmt)}"
    }
}

fun formatCoordinateHemisphere(latitude: Double, longitude: Double): String {
    val latDir = if (latitude >= 0) "N" else "S"
    val lngDir = if (longitude >= 0) "E" else "W"
    return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(latitude), latDir, abs(longitude), lngDir)
}
