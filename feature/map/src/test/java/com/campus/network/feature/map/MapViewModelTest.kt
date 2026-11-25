package com.campus.network.feature.map

import com.campus.network.core.data.LiveLocationRepository
import com.campus.network.core.database.entity.FriendEntity
import com.campus.network.core.model.AudienceFilter
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.FriendLiveLocation
import com.campus.network.core.model.FriendStatus
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.model.StudentProfile
import com.campus.network.feature.map.events.computePresetStartTimeIso
import com.campus.network.feature.map.events.formatCoordinateHemisphere
import com.campus.network.feature.map.events.formatEventDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MapViewModelTest {

    @Test
    fun `resolveCanonicalProfile preserves own fullName when knownProfiles is empty`() {
        val current = StudentProfile(
            studentId = "STD-123",
            fullName = "Lincoln Paul",
            universityEmail = "lincoln@campus.edu"
        )

        val resolvedEmptyMap = MapViewModel.resolveCanonicalProfile(current, emptyMap())
        assertNotNull(resolvedEmptyMap)
        assertEquals("Lincoln Paul", resolvedEmptyMap!!.fullName)

        val canonicalOverride = mapOf(
            "STD-123" to current.copy(fullName = "Lincoln P. Odhiambo")
        )
        val resolvedWithCanonical = MapViewModel.resolveCanonicalProfile(current, canonicalOverride)
        assertEquals("Lincoln P. Odhiambo", resolvedWithCanonical!!.fullName)
    }

    @Test
    fun `buildAcceptedCampusFriends filters out non-friends and reflects real live online status`() {
        val now = 1_730_000_000_000L
        val entities = listOf(
            FriendEntity(
                id = "me:friend-live",
                userId = "me",
                friendUserId = "friend-live",
                friendDisplayName = "Imani Okafor",
                friendHandle = "@imani",
                friendAvatarResName = "",
                status = FriendStatus.FRIENDS.name,
                lastInteractionEpochMs = now,
                dmChannelId = "dm_1"
            ),
            FriendEntity(
                id = "me:friend-offline",
                userId = "me",
                friendUserId = "friend-offline",
                friendDisplayName = "Alex Otieno",
                friendHandle = "@alex",
                friendAvatarResName = "",
                status = FriendStatus.FRIENDS.name,
                lastInteractionEpochMs = now,
                dmChannelId = "dm_2"
            ),
            FriendEntity(
                id = "me:pending",
                userId = "me",
                friendUserId = "pending",
                friendDisplayName = "Pending Peer",
                friendHandle = "@pending",
                friendAvatarResName = "",
                status = FriendStatus.PENDING_SENT.name,
                lastInteractionEpochMs = now,
                dmChannelId = null
            )
        )

        val liveLocations = listOf(
            FriendLiveLocation(
                userId = "friend-live",
                displayName = "Imani Okafor",
                latitude = -1.3094,
                longitude = 36.8122,
                updatedAtEpochMs = now - 10_000L,
                expiresAtEpochMs = now + 600_000L
            ),
            FriendLiveLocation(
                userId = "friend-offline",
                displayName = "Alex Otieno",
                latitude = -1.3100,
                longitude = 36.8130,
                updatedAtEpochMs = now - (LiveLocationRepository.STALE_LOCATION_MS + 5_000L),
                expiresAtEpochMs = now + 600_000L
            )
        )

        val result = MapViewModel.buildAcceptedCampusFriends(
            acceptedEntities = entities,
            knownProfiles = emptyMap(),
            liveLocations = liveLocations,
            nowMs = now
        )

        assertEquals(2, result.size)
        val imani = result.first { it.id == "friend-live" }
        val alex = result.first { it.id == "friend-offline" }
        assertTrue(imani.isOnline)
        assertFalse(alex.isOnline)
    }

    @Test
    fun `formatCoordinateHemisphere formats southern-western and northern-eastern hemispheres`() {
        assertEquals("1.3094° S, 36.8122° E", formatCoordinateHemisphere(-1.3094, 36.8122))
        assertEquals("40.7128° N, 74.0060° W", formatCoordinateHemisphere(40.7128, -74.0060))
    }

    @Test
    fun `computePresetStartTimeIso produces valid ISO-8601 instant`() {
        val iso = computePresetStartTimeIso("Tomorrow, 2:00 PM")
        val parsed = Instant.parse(iso)
        assertTrue(parsed.toEpochMilli() > System.currentTimeMillis())
        assertTrue(formatEventDateTime(iso).startsWith("Tomorrow"))
    }

    @Test
    fun `filterPlaces applies AudienceFilter FRIENDS correctly`() {
        val place1 = CampusPlace(
            id = "p1",
            name = "Library",
            category = PlaceCategory.STUDY_ZONES,
            subtitle = "Quiet",
            address = "Block A",
            neighborhood = "Central",
            campusRegion = "Nairobi",
            description = "Study",
            latitude = -1.309,
            longitude = 36.812
        )
        val place2 = place1.copy(id = "p2", name = "Labs", category = PlaceCategory.ACADEMIC_LABS)

        val meets = listOf(
            FlashMeet(
                id = "m1",
                title = "Friend Meet",
                description = "Study",
                category = MeetCategory.STUDY_JAM,
                latitude = -1.309,
                longitude = 36.812,
                locationName = "Lib",
                creatorName = "Imani",
                creatorUserId = "friend-1",
                createdAtEpochMs = 1L,
                expiresAtEpochMs = Long.MAX_VALUE
            ),
            FlashMeet(
                id = "m2",
                title = "Stranger Meet",
                description = "Study",
                category = MeetCategory.STUDY_JAM,
                latitude = -1.309,
                longitude = 36.812,
                locationName = "Quad",
                creatorName = "Stranger",
                creatorUserId = "stranger-9",
                createdAtEpochMs = 1L,
                expiresAtEpochMs = Long.MAX_VALUE
            )
        )

        val filters = MapViewModel.FilterState(
            selectedCategory = PlaceCategory.TOP_PICKS,
            audienceFilter = AudienceFilter.FRIENDS,
            selectedRegion = "All Hubs",
            isOpenNowOnly = false,
            searchQuery = "",
            selectedMeetCategory = null
        )

        val friendsFilterPlaces = MapViewModel.filterPlaces(
            allPlaces = listOf(place1, place2),
            filters = filters,
            userLoc = null,
            savedIds = emptySet(),
            beenIds = emptySet(),
            friendSavedIds = setOf("p1")
        )
        assertEquals(1, friendsFilterPlaces.size)
        assertEquals("p1", friendsFilterPlaces[0].id)

        val friendOnlyMeets = MapViewModel.filterMeets(
            allMeets = meets,
            filters = filters,
            stableCurrentId = "me",
            friendIdSet = setOf("friend-1"),
            joinedMeetIds = emptySet()
        )
        assertEquals(1, friendOnlyMeets.size)
        assertEquals("m1", friendOnlyMeets[0].id)

        // Ensure AudienceFilter.FRIENDS enum label stays intact
        assertEquals("friends", AudienceFilter.FRIENDS.label)
    }

    @Test
    fun `formatZenlyOrbitalDuration formats hours and minutes matching Pinterest video`() {
        val now = 1_730_000_000_000L
        val oneHrThreeMinsAgo = now - (63L * 60_000L)
        assertEquals(
            "1 HR, 3 MIN",
            com.campus.network.feature.map.ui.mapbox.formatZenlyOrbitalDuration(
                stationarySinceEpochMs = oneHrThreeMinsAgo,
                updatedAtEpochMs = now,
                expiresAtEpochMs = now + 600_000L,
                nowEpochMs = now
            )
        )

        val twoHrFiftyFourMinsAgo = now - (174L * 60_000L)
        assertEquals(
            "2 HR, 54 MIN",
            com.campus.network.feature.map.ui.mapbox.formatZenlyOrbitalDuration(
                stationarySinceEpochMs = twoHrFiftyFourMinsAgo,
                updatedAtEpochMs = now,
                expiresAtEpochMs = now + 600_000L,
                nowEpochMs = now
            )
        )

        val fourteenMinsAgo = now - (14L * 60_000L)
        assertEquals(
            "14 MIN",
            com.campus.network.feature.map.ui.mapbox.formatZenlyOrbitalDuration(
                stationarySinceEpochMs = fourteenMinsAgo,
                updatedAtEpochMs = now,
                expiresAtEpochMs = now + 600_000L,
                nowEpochMs = now
            )
        )
    }
}
