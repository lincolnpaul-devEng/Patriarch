package com.campus.network.core.data

import com.campus.network.core.model.AudienceFilter
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.model.PlaceStats
import com.campus.network.core.model.SavedByUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class PlacesRepository {

    private val _places = MutableStateFlow<List<CampusPlace>>(initialCatalog())
    val places: Flow<List<CampusPlace>> = _places.asStateFlow()

    fun getPlacesByCategory(category: PlaceCategory?): Flow<List<CampusPlace>> {
        return _places.map { list ->
            if (category == null || category == PlaceCategory.TOP_PICKS) {
                list
            } else {
                list.filter { it.category == category }
            }
        }
    }

    fun toggleSavedToTry(placeId: String) {
        val current = _places.value
        _places.value = current.map { place ->
            if (place.id == placeId) {
                val newStatus = !place.isSavedToTry
                place.copy(
                    isSavedToTry = newStatus,
                    savesCount = if (newStatus) place.savesCount + 1 else place.savesCount - 1
                )
            } else place
        }
    }

    fun toggleBeen(placeId: String) {
        val current = _places.value
        _places.value = current.map { place ->
            if (place.id == placeId) {
                place.copy(hasBeen = !place.hasBeen)
            } else place
        }
    }

    private fun initialCatalog(): List<CampusPlace> {
        val users = listOf(
            SavedByUser("1", "eliza", "avatar_eliza", isLiked = true, isBookmarked = true),
            SavedByUser("2", "clin", "avatar_clin", isLiked = false, isBookmarked = true),
            SavedByUser("3", "junebog", "avatar_junebog", isLiked = true, isBookmarked = false),
            SavedByUser("4", "andrew", "avatar_andrew", isLiked = false, isBookmarked = true)
        )

        return listOf(
            CampusPlace(
                id = "la_cabra",
                name = "LA CABRA BAKERY",
                category = PlaceCategory.CAFES,
                subtitle = "popular scandi pastries",
                address = "152 2nd Ave",
                neighborhood = "East Village",
                description = "Scandinavian coffee shop serving the city's most talked-about cardamom buns, light roast pourovers, and rustic sourdough loaves in a minimalist space.",
                latitude = 40.7297,
                longitude = -73.9875,
                savesCount = 946,
                rankingTag = "POPULAR · #4 bakery",
                priceTier = "$",
                hoursText = "open 7am–6pm",
                isOpenNow = true,
                iconResName = "pin_bakery",
                imagesResNames = listOf("place_photo_1", "place_photo_2", "place_photo_3"),
                savedByUsers = users,
                stats = PlaceStats(saves = 553, stars = 106, likes = 278, dislikes = 9),
                isSavedToTry = true
            ),
            CampusPlace(
                id = "culture_espresso",
                name = "Culture Espresso",
                category = PlaceCategory.CAFES,
                subtitle = "trending cookie cafe",
                address = "72 W 38th St",
                neighborhood = "Midtown",
                description = "Renowned specialty espresso bar famous for its warm, freshly baked chocolate chip cookies and rotating single-origin roasts.",
                latitude = 40.7523,
                longitude = -73.9856,
                savesCount = 1420,
                rankingTag = "POPULAR · #1 cookie cafe",
                priceTier = "$",
                hoursText = "open 7am–7pm",
                isOpenNow = true,
                iconResName = "pin_cookie",
                imagesResNames = listOf("place_photo_2", "place_photo_1", "place_photo_3"),
                savedByUsers = users.take(3),
                stats = PlaceStats(saves = 820, stars = 412, likes = 630, dislikes = 4)
            ),
            CampusPlace(
                id = "cote_flatiron",
                name = "COTE Flatiron",
                category = PlaceCategory.EAT,
                subtitle = "trending korean steakhouse",
                address = "16 W 22nd St",
                neighborhood = "Flatiron",
                description = "Michelin-starred Korean steakhouse combining the fun of Korean BBQ with the hallmarks of a classic, high-end American chophouse.",
                latitude = 40.7410,
                longitude = -73.9902,
                savesCount = 2310,
                rankingTag = "POPULAR · #1 steakhouse",
                priceTier = "$$$",
                hoursText = "open 5pm–11pm",
                isOpenNow = true,
                iconResName = "pin_steak",
                imagesResNames = listOf("place_photo_3", "place_photo_2", "place_photo_1"),
                savedByUsers = users,
                stats = PlaceStats(saves = 1120, stars = 520, likes = 890, dislikes = 12)
            ),
            CampusPlace(
                id = "cote_550",
                name = "COTE 550",
                category = PlaceCategory.EAT,
                subtitle = "new eat",
                address = "550 Madison Ave",
                neighborhood = "Midtown East",
                description = "An ultra-modern high floor dining venue offering premier dry-aged prime beef cuts and an award-winning curated wine cellar.",
                latitude = 40.7612,
                longitude = -73.9723,
                savesCount = 680,
                rankingTag = "TRENDING",
                priceTier = "$$$$",
                hoursText = "open 5pm–10:30pm",
                isOpenNow = true,
                iconResName = "pin_plate",
                imagesResNames = listOf("place_photo_1", "place_photo_3", "place_photo_2"),
                savedByUsers = users.drop(1),
                stats = PlaceStats(saves = 340, stars = 145, likes = 290, dislikes = 2)
            ),
            CampusPlace(
                id = "lily_pond",
                name = "Lily Pond",
                category = PlaceCategory.CAFES,
                subtitle = "healthy cafe",
                address = "32 E 20th St",
                neighborhood = "Gramercy",
                description = "Sunlit neighborhood oasis featuring organic matcha lattes, grain bowls, and botanical tea blends.",
                latitude = 40.7388,
                longitude = -73.9882,
                savesCount = 790,
                rankingTag = "POPULAR · #3 healthy",
                priceTier = "$$",
                hoursText = "open 8am–5pm",
                isOpenNow = true,
                iconResName = "pin_eliza",
                imagesResNames = listOf("place_photo_2", "place_photo_1", "place_photo_3"),
                savedByUsers = users.take(2),
                stats = PlaceStats(saves = 410, stars = 95, likes = 310, dislikes = 5),
                likedByFriend = "eliza"
            ),
            CampusPlace(
                id = "la_pecora_bianca",
                name = "La Pecora Bianca SoHo",
                category = PlaceCategory.EAT,
                subtitle = "trending italian cafe & wine",
                address = "54 Prince St",
                neighborhood = "SoHo",
                description = "Bustling, light-filled Italian cafe offering housemade pastas, spritzes, and a vibrant all-day neighborhood atmosphere.",
                latitude = 40.7231,
                longitude = -73.9961,
                savesCount = 1150,
                rankingTag = "POPULAR · #2 italian",
                priceTier = "$$",
                hoursText = "open 8am–10pm",
                isOpenNow = true,
                iconResName = "pin_pasta",
                imagesResNames = listOf("place_photo_3", "place_photo_1", "place_photo_2"),
                savedByUsers = users.take(3),
                stats = PlaceStats(saves = 670, stars = 210, likes = 450, dislikes = 11)
            ),
            CampusPlace(
                id = "deluxe_green_bo",
                name = "Deluxe Green Bo",
                category = PlaceCategory.EAT,
                subtitle = "trending shanghai dumplings",
                address = "66 Bayard St",
                neighborhood = "Chinatown",
                description = "Beloved neighborhood staple famous for hot soup dumplings, pan-fried pork buns, and scallion pancakes.",
                latitude = 40.7153,
                longitude = -73.9982,
                savesCount = 1680,
                rankingTag = "POPULAR · #1 dumplings",
                priceTier = "$",
                hoursText = "open 11am–9:30pm",
                isOpenNow = true,
                iconResName = "pin_dumpling",
                imagesResNames = listOf("place_photo_1", "place_photo_2", "place_photo_3"),
                savedByUsers = users.take(2),
                stats = PlaceStats(saves = 910, stars = 380, likes = 740, dislikes = 7)
            ),
            CampusPlace(
                id = "antidote",
                name = "Antidote",
                category = PlaceCategory.BARS,
                subtitle = "trending szechuan cocktails",
                address = "66 S 2nd St",
                neighborhood = "Williamsburg",
                description = "Chic bi-level cocktail lounge and eatery pairing inventive spice-infused cocktails with contemporary Sichuan fare.",
                latitude = 40.7145,
                longitude = -73.9635,
                savesCount = 890,
                rankingTag = "POPULAR · #2 cocktail bar",
                priceTier = "$$$",
                hoursText = "open 5pm–12am",
                isOpenNow = true,
                iconResName = "pin_chilli",
                imagesResNames = listOf("place_photo_2", "place_photo_3", "place_photo_1"),
                savedByUsers = users.drop(2),
                stats = PlaceStats(saves = 530, stars = 180, likes = 390, dislikes = 3)
            ),
            CampusPlace(
                id = "patriarch_summit",
                name = "Patriarch Horizon Gala",
                category = PlaceCategory.EVENTS,
                subtitle = "flagship student showcase",
                address = "111 8th Ave",
                neighborhood = "Chelsea",
                description = "Annual Patriarch networking summit featuring keynote student startup demos, exclusive lounges, and live music.",
                latitude = 40.7418,
                longitude = -74.0048,
                savesCount = 3450,
                rankingTag = "FEATURED · #1 event",
                priceTier = "Free",
                hoursText = "open 6pm–11pm",
                isOpenNow = true,
                iconResName = "pin_event_stage",
                imagesResNames = listOf("place_photo_1", "place_photo_2", "place_photo_3"),
                savedByUsers = users,
                stats = PlaceStats(saves = 1890, stars = 650, likes = 1200, dislikes = 2)
            )
        )
    }
}
