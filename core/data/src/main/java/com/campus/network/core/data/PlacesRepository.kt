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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    /**
     * Returns places sorted by proximity to the user's actual GPS location.
     */
    fun getPlacesSortedByProximity(userLat: Double, userLng: Double): Flow<List<CampusPlace>> {
        return _places.map { list ->
            list.sortedBy { place ->
                haversineDistanceKm(userLat, userLng, place.latitude, place.longitude)
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

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the Earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun initialCatalog(): List<CampusPlace> {
        val verifiedPeers = listOf(
            SavedByUser("1", "sarah.k", "avatar_eliza", isLiked = true, isBookmarked = true),
            SavedByUser("2", "kevin.c", "avatar_clin", isLiked = true, isBookmarked = true),
            SavedByUser("3", "brian.m", "avatar_andrew", isLiked = true, isBookmarked = false),
            SavedByUser("4", "lincoln.k", "avatar_me", isLiked = true, isBookmarked = true)
        )

        return listOf(
            // --- NAIROBI & MAIN CAMPUS SPOTS ---
            CampusPlace(
                id = "strat_quad_b",
                name = "Strathmore Quad B & Hall 4",
                category = PlaceCategory.RENDEZVOUS,
                subtitle = "Primary Campus SafeTrade Checkpoint",
                address = "Ole Sangale Rd, Madaraka",
                neighborhood = "Residential Quad",
                campusRegion = "Nairobi",
                description = "Dedicated physical rendezvous hub equipped with 24/7 CCTV surveillance, campus security post, and well-lit student benches for peer handovers.",
                latitude = -1.3094,
                longitude = 36.8122,
                savesCount = 1420,
                rankingTag = "TOP SAFETRADE SPOT · CCTV 24/7",
                priceTier = "Free Access",
                hoursText = "Open 24 Hours",
                isOpenNow = true,
                securityLevel = "CCTV Monitored · 24/7 Guard Post",
                isVerifiedSafeTrade = true,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_1", "place_photo_2", "place_photo_3"),
                savedByUsers = verifiedPeers,
                stats = PlaceStats(saves = 1420, stars = 380, likes = 890, dislikes = 2),
                isSavedToTry = true
            ),
            CampusPlace(
                id = "main_library",
                name = "University Main Library (Level 3)",
                category = PlaceCategory.STUDY_ZONES,
                subtitle = "Silent Research & Focus Hall",
                address = "Central Campus Spine, 3rd Floor",
                neighborhood = "Academic Wing",
                campusRegion = "Nairobi",
                description = "Strictly silent study floor with individual carrels, high-speed university Wi-Fi, dual power outlets at each desk, and reference course material shelves.",
                latitude = -1.3087,
                longitude = 36.8129,
                savesCount = 2850,
                rankingTag = "#1 STUDY SPOT · SILENT ZONE",
                priceTier = "Free Access",
                hoursText = "Open 7:00 AM – 10:00 PM",
                isOpenNow = true,
                securityLevel = "Library Reception & Turnstile Access",
                isVerifiedSafeTrade = false,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_2", "place_photo_1", "place_photo_3"),
                savedByUsers = verifiedPeers.take(3),
                stats = PlaceStats(saves = 2850, stars = 740, likes = 1420, dislikes = 5),
                likedByFriend = "sarah.k"
            ),
            CampusPlace(
                id = "guild_hub",
                name = "Student Guild Hub (Table 4)",
                category = PlaceCategory.GUILD_SERVICES,
                subtitle = "Guild Arbitration & Trade Table",
                address = "Student Guild Pavilion, Ground Floor",
                neighborhood = "Student Center",
                campusRegion = "Nairobi",
                description = "Official Student Guild meeting ground with designated SafeTrade arbitration station, community message boards, and campus lost-and-found registry.",
                latitude = -1.3082,
                longitude = 36.8118,
                savesCount = 980,
                rankingTag = "GUILD ENDORSED SPOT",
                priceTier = "Free Access",
                hoursText = "Open 8:00 AM – 8:00 PM",
                isOpenNow = true,
                securityLevel = "Guild Executives & CCTV",
                isVerifiedSafeTrade = true,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_3", "place_photo_1", "place_photo_2"),
                savedByUsers = verifiedPeers.take(2),
                stats = PlaceStats(saves = 980, stars = 210, likes = 540, dislikes = 4)
            ),
            CampusPlace(
                id = "turing_lab",
                name = "Turing Computing & AI Lab",
                category = PlaceCategory.ACADEMIC_LABS,
                subtitle = "Phase 2 High-Performance Lab",
                address = "Technology Complex, Block B Room 204",
                neighborhood = "Science & Tech",
                campusRegion = "Nairobi",
                description = "High-performance workstation lab outfitted with dual monitors, Linux/CUDA development environments, and collaborative whiteboard clusters for hackathons.",
                latitude = -1.3101,
                longitude = 36.8135,
                savesCount = 1750,
                rankingTag = "TOP TECH LAB · 24/7 ACCESS",
                priceTier = "Student ID Required",
                hoursText = "Open 24/7 with Card Tap",
                isOpenNow = true,
                securityLevel = "Biometric / RFID Card Tap",
                isVerifiedSafeTrade = false,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_1", "place_photo_3", "place_photo_2"),
                savedByUsers = verifiedPeers,
                stats = PlaceStats(saves = 1750, stars = 490, likes = 920, dislikes = 3)
            ),
            CampusPlace(
                id = "smart_lockers",
                name = "Library Smart Lockers",
                category = PlaceCategory.RENDEZVOUS,
                subtitle = "24/7 Automated SafePay Handover",
                address = "Library Entrance Foyer, Ground Floor",
                neighborhood = "Campus Spine",
                campusRegion = "Nairobi",
                description = "Automated digital parcel lockers for contactless peer-to-peer textbook and gear handovers. Release code generates automatically upon SafePay Vault confirmation.",
                latitude = -1.3089,
                longitude = 36.8127,
                savesCount = 1120,
                rankingTag = "CONTACTLESS SAFETRADE",
                priceTier = "Free for Students",
                hoursText = "Open 24 Hours",
                isOpenNow = true,
                securityLevel = "CCTV Monitored · PIN / QR Lock",
                isVerifiedSafeTrade = true,
                hasWifi = true,
                hasPowerOutlets = false,
                imagesResNames = listOf("place_photo_2", "place_photo_1", "place_photo_3"),
                savedByUsers = verifiedPeers.drop(1),
                stats = PlaceStats(saves = 1120, stars = 310, likes = 680, dislikes = 1)
            ),
            CampusPlace(
                id = "auditorium_hub",
                name = "Auditorium & Innovation Hub",
                category = PlaceCategory.EVENTS,
                subtitle = "Main Campus Event Stage",
                address = "Patriarch Innovation Center, Ground Floor",
                neighborhood = "Central Plaza",
                campusRegion = "Nairobi",
                description = "500-seat amphitheater and networking galleria hosting campus tech summits, guest speaker series, GDSC demo days, and university guild debates.",
                latitude = -1.3078,
                longitude = 36.8121,
                savesCount = 3200,
                rankingTag = "MAIN EVENT VENUE",
                priceTier = "Free Entry",
                hoursText = "Open During Scheduled Events",
                isOpenNow = true,
                securityLevel = "Event Security & Ushers",
                isVerifiedSafeTrade = false,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_1", "place_photo_2", "place_photo_3"),
                savedByUsers = verifiedPeers,
                stats = PlaceStats(saves = 3200, stars = 860, likes = 1600, dislikes = 3)
            ),
            CampusPlace(
                id = "science_labs",
                name = "Phase 1 Science & Robotics Labs",
                category = PlaceCategory.ACADEMIC_LABS,
                subtitle = "Engineering & Hardware Exchange",
                address = "Science Block, South Entrance",
                neighborhood = "South Quad",
                campusRegion = "Nairobi",
                description = "Engineering design labs, 3D printing studio, and verified rendezvous checkpoint for science lab coats, tools, and calculators.",
                latitude = -1.3105,
                longitude = 36.8115,
                savesCount = 890,
                rankingTag = "HARDWARE & LAB GEAR",
                priceTier = "Student ID Required",
                hoursText = "Open 8:00 AM – 9:00 PM",
                isOpenNow = true,
                securityLevel = "Faculty Lab Technician on Duty",
                isVerifiedSafeTrade = true,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_3", "place_photo_2", "place_photo_1"),
                savedByUsers = verifiedPeers.take(2),
                stats = PlaceStats(saves = 890, stars = 230, likes = 480, dislikes = 2)
            ),
            CampusPlace(
                id = "sports_complex",
                name = "Campus Sports Arena & Track",
                category = PlaceCategory.GUILD_SERVICES,
                subtitle = "Recreation & Athletics Pavilion",
                address = "East Sports Fields, Main Pavilion",
                neighborhood = "Sports Fields",
                campusRegion = "Nairobi",
                description = "Multi-purpose sports pavilion with indoor basketball courts, outdoor running track, fitness gym, and open student bleachers for evening meetups.",
                latitude = -1.3072,
                longitude = 36.8142,
                savesCount = 1350,
                rankingTag = "SPORTS & WELLNESS",
                priceTier = "Free for Students",
                hoursText = "Open 6:00 AM – 9:00 PM",
                isOpenNow = true,
                securityLevel = "Athletics Staff & Campus Guard",
                isVerifiedSafeTrade = false,
                hasWifi = true,
                hasPowerOutlets = false,
                imagesResNames = listOf("place_photo_2", "place_photo_3", "place_photo_1"),
                savedByUsers = verifiedPeers.drop(2),
                stats = PlaceStats(saves = 1350, stars = 320, likes = 710, dislikes = 6)
            ),

            // --- MOMBASA REGIONAL CAMPUS SPOTS ---
            CampusPlace(
                id = "mombasa_library",
                name = "Coastal Campus Library & Study Hall",
                category = PlaceCategory.STUDY_ZONES,
                subtitle = "Sea-Breeze Quiet Focus Pavilion",
                address = "Nyali Campus Complex, Ocean Wing",
                neighborhood = "Nyali",
                campusRegion = "Mombasa",
                description = "Ocean-facing campus study hall with natural ventilation, high-speed fiber internet, and quiet study carrels for coastal university students.",
                latitude = -4.0435,
                longitude = 39.6682,
                savesCount = 740,
                rankingTag = "TOP COASTAL STUDY SPOT",
                priceTier = "Free Access",
                hoursText = "Open 8:00 AM – 9:00 PM",
                isOpenNow = true,
                securityLevel = "Campus Guard & Reception",
                isVerifiedSafeTrade = false,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_1", "place_photo_2", "place_photo_3"),
                savedByUsers = verifiedPeers.take(2),
                stats = PlaceStats(saves = 740, stars = 180, likes = 390, dislikes = 1)
            ),
            CampusPlace(
                id = "mombasa_rendezvous",
                name = "Mombasa Campus Guild Pavilion",
                category = PlaceCategory.RENDEZVOUS,
                subtitle = "Coastal SafeTrade Rendezvous",
                address = "Mombasa CBD Campus, 1st Floor Foyer",
                neighborhood = "Mombasa Island",
                campusRegion = "Mombasa",
                description = "Designated physical exchange spot with CCTV coverage, peer trade desk, and secure student lockers for textbooks and electronics.",
                latitude = -4.0620,
                longitude = 39.6705,
                savesCount = 610,
                rankingTag = "COASTAL SAFETRADE HUB",
                priceTier = "Free Access",
                hoursText = "Open 8:00 AM – 7:00 PM",
                isOpenNow = true,
                securityLevel = "CCTV Monitored · 24/7 Guard",
                isVerifiedSafeTrade = true,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_2", "place_photo_3", "place_photo_1"),
                savedByUsers = verifiedPeers.take(3),
                stats = PlaceStats(saves = 610, stars = 145, likes = 310, dislikes = 2)
            ),

            // --- KISUMU REGIONAL CAMPUS SPOTS ---
            CampusPlace(
                id = "kisumu_study_hall",
                name = "Lake Basin Campus Study Center",
                category = PlaceCategory.STUDY_ZONES,
                subtitle = "Victoria Focus & Research Hall",
                address = "Oginga Odinga Rd, Academic Wing",
                neighborhood = "Kisumu Central",
                campusRegion = "Kisumu",
                description = "Lakeside regional campus study center with high-speed academic network, modern workstations, and group discussion chambers.",
                latitude = -0.0917,
                longitude = 34.7680,
                savesCount = 680,
                rankingTag = "TOP WESTERN STUDY SPOT",
                priceTier = "Free Access",
                hoursText = "Open 7:30 AM – 9:00 PM",
                isOpenNow = true,
                securityLevel = "Campus Security Post",
                isVerifiedSafeTrade = false,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_1", "place_photo_3", "place_photo_2"),
                savedByUsers = verifiedPeers.take(2),
                stats = PlaceStats(saves = 680, stars = 160, likes = 340, dislikes = 1)
            ),
            CampusPlace(
                id = "kisumu_rendezvous",
                name = "Victoria Student Guild Pavilion",
                category = PlaceCategory.RENDEZVOUS,
                subtitle = "Lakeside SafeTrade Point",
                address = "Kisumu Campus Grounds, Main Quad",
                neighborhood = "Milimani",
                campusRegion = "Kisumu",
                description = "Official rendezvous point for safe student gear handovers in the Western Kenya university corridor with direct campus security oversight.",
                latitude = -0.0965,
                longitude = 34.7620,
                savesCount = 520,
                rankingTag = "VERIFIED LAKE REGION HUB",
                priceTier = "Free Access",
                hoursText = "Open 8:00 AM – 7:30 PM",
                isOpenNow = true,
                securityLevel = "CCTV Monitored · Guard Post",
                isVerifiedSafeTrade = true,
                hasWifi = true,
                hasPowerOutlets = true,
                imagesResNames = listOf("place_photo_3", "place_photo_1", "place_photo_2"),
                savedByUsers = verifiedPeers,
                stats = PlaceStats(saves = 520, stars = 120, likes = 260, dislikes = 0)
            )
        )
    }
}
