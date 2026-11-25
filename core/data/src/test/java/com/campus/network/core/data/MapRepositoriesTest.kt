package com.campus.network.core.data

import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.network.RealtimeEventType
import com.campus.network.core.network.SupabaseRealtimeChange
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapRepositoriesTest {

    @Serializable
    private data class MeetAttendanceRpcParams(val p_meet_id: String)

    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    private fun buildMockClient(responder: (tableOrPath: String) -> String): SupabaseClient {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            respond(
                content = responder(path),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }
        return createSupabaseClient(
            supabaseUrl = "https://test.supabase.co",
            supabaseKey = "test-anon-key"
        ) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    encodeDefaults = true
                }
            )
            httpEngine = engine
            install(Postgrest)
        }
    }

    @Test
    fun `LiveLocationRepository filters expired, ghost mode, and self locations and handles realtime transitions`() = runBlocking {
        val now = System.currentTimeMillis()
        val future = now + 600_000L
        val past = now - 60_000L
        val staleUpdated = now - (LiveLocationRepository.STALE_LOCATION_MS + 30_000L)

        val liveLocationsJson = """
            [
              {
                "user_id": "STD-SELF",
                "display_name": "Self Student",
                "latitude": -1.3090,
                "longitude": 36.8120,
                "privacy_scope": "VERIFIED_CAMPUS_PEERS",
                "is_sharing": true,
                "share_token": "tok_self_1234567890",
                "expires_at_epoch_ms": $future,
                "updated_at_epoch_ms": $now
              },
              {
                "user_id": "STD-FRIEND-1",
                "display_name": "Imani Okafor",
                "latitude": -1.3094,
                "longitude": 36.8122,
                "privacy_scope": "VERIFIED_CAMPUS_PEERS",
                "is_sharing": true,
                "expires_at_epoch_ms": $future,
                "updated_at_epoch_ms": $now
              },
              {
                "user_id": "STD-EXPIRED",
                "display_name": "Alex Otieno",
                "latitude": -1.3100,
                "longitude": 36.8130,
                "privacy_scope": "TIMED_SESSION_30M",
                "is_sharing": true,
                "expires_at_epoch_ms": $past,
                "updated_at_epoch_ms": $now
              },
              {
                "user_id": "STD-STALE",
                "display_name": "Tessa Wanjiku",
                "latitude": -1.3080,
                "longitude": 36.8110,
                "privacy_scope": "VERIFIED_CAMPUS_PEERS",
                "is_sharing": true,
                "expires_at_epoch_ms": $future,
                "updated_at_epoch_ms": $staleUpdated
              },
              {
                "user_id": "STD-GHOST",
                "display_name": "CJ Kamau",
                "latitude": -1.3075,
                "longitude": 36.8105,
                "privacy_scope": "GHOST_MODE_HIDDEN",
                "is_sharing": false,
                "expires_at_epoch_ms": $future,
                "updated_at_epoch_ms": $now
              }
            ]
        """.trimIndent()

        val client = buildMockClient { path ->
            if (path.contains("campus_live_locations")) liveLocationsJson else "[]"
        }
        val repo = LiveLocationRepository(supabase = client)

        val visible = repo.syncVisibleFriendLocations(currentUserId = "STD-SELF").getOrThrow()
        assertEquals(1, visible.size)
        assertEquals("STD-FRIEND-1", visible[0].userId)
        assertEquals("Imani Okafor", visible[0].displayName)
        assertTrue(repo.isSharing.value)
        assertEquals("tok_self_1234567890", repo.activeShareToken.value)

        // Realtime update switches STD-FRIEND-1 into Ghost Mode -> immediately removed
        repo.applyRealtimeChange(
            SupabaseRealtimeChange(
                table = "campus_live_locations",
                eventType = RealtimeEventType.UPDATE,
                recordJson = """
                    {
                      "user_id": "STD-FRIEND-1",
                      "display_name": "Imani Okafor",
                      "latitude": -1.3094,
                      "longitude": 36.8122,
                      "privacy_scope": "GHOST_MODE_HIDDEN",
                      "is_sharing": false,
                      "expires_at_epoch_ms": 0,
                      "updated_at_epoch_ms": $now
                    }
                """.trimIndent()
            ),
            currentUserId = "STD-SELF"
        )
        assertTrue(repo.friendLocations.value.isEmpty())

        // Realtime insert brings another friend live, then delete removes them
        repo.applyRealtimeChange(
            SupabaseRealtimeChange(
                table = "campus_live_locations",
                eventType = RealtimeEventType.INSERT,
                recordJson = """
                    {
                      "user_id": "STD-FRIEND-2",
                      "display_name": "Erica Njoroge",
                      "latitude": -1.3088,
                      "longitude": 36.8125,
                      "privacy_scope": "TIMED_SESSION_30M",
                      "is_sharing": true,
                      "expires_at_epoch_ms": $future,
                      "updated_at_epoch_ms": $now
                    }
                """.trimIndent()
            ),
            currentUserId = "STD-SELF"
        )
        assertEquals(1, repo.friendLocations.value.size)
        assertEquals(LocationPrivacyScope.TIMED_SESSION_30M, repo.friendLocations.value[0].privacyScope)

        repo.applyRealtimeChange(
            SupabaseRealtimeChange(
                table = "campus_live_locations",
                eventType = RealtimeEventType.DELETE,
                oldRecordJson = """{"user_id":"STD-FRIEND-2"}"""
            ),
            currentUserId = "STD-SELF"
        )
        assertTrue(repo.friendLocations.value.isEmpty())
    }

    @Test
    fun `LiveLocationRepository shouldPublishUpdate throttles frequent micro-movements`() {
        val repo = LiveLocationRepository()
        // Before any publish, always true
        assertTrue(repo.shouldPublishUpdate(-1.3094, 36.8122, nowMs = 1_000L))
        // Haversine 10+ meters helper verification
        val nearMeters = LiveLocationRepository.haversineMeters(-1.3094, 36.8122, -1.30941, 36.81221)
        assertTrue(nearMeters < LiveLocationRepository.MIN_PUBLISH_DISTANCE_METERS)
        val farMeters = LiveLocationRepository.haversineMeters(-1.3094, 36.8122, -1.3097, 36.8125)
        assertTrue(farMeters >= LiveLocationRepository.MIN_PUBLISH_DISTANCE_METERS)
    }

    @Test
    fun `CampusEventsRepository syncs remote events and toggles RSVP via RPC`() = runBlocking {
        val eventsJson = """
            [
              {
                "id": "evt_101",
                "creator_id": "STD-HOST",
                "title": "Robotics Sprint Night",
                "host_name": "Imani Okafor",
                "description": "Autonomous rover calibration.",
                "location_name": "Innovation Hub Auditorium",
                "full_address": "Ground Floor, Innovation Complex",
                "latitude": -1.3078,
                "longitude": 36.8121,
                "start_time": "2026-11-22T18:00:00Z",
                "category": "Tech & AI",
                "price_label": "Free Admission",
                "max_capacity": 50,
                "attendee_count": 4,
                "artwork_key": "event_thumb_founders"
              }
            ]
        """.trimIndent()

        val rsvpsJson = """
            [
              {
                "event_id": "evt_101",
                "user_id": "STD-FRIEND-1",
                "display_name": "Alex Otieno",
                "created_at_epoch_ms": 1730000000000
              }
            ]
        """.trimIndent()

        val client = buildMockClient { path ->
            when {
                path.contains("toggle_campus_event_rsvp") -> """{"is_registered": true, "attendee_count": 5}"""
                path.contains("campus_event_rsvps") -> rsvpsJson
                path.contains("campus_events") -> eventsJson
                else -> "[]"
            }
        }

        val repo = CampusEventsRepository(database = null, supabase = client)
        repo.syncRemoteEvents(currentUserId = "STD-SELF").getOrThrow()

        val synced = repo.observeEvents().first()
        assertEquals(1, synced.size)
        assertEquals("Robotics Sprint Night", synced[0].title)
        assertEquals(50, synced[0].maxCapacity)
        assertFalse(synced[0].isUserRegistered)
        assertEquals(listOf("Alex Otieno"), synced[0].attendeeNames)

        val (registered, count) = repo.toggleRsvp("evt_101", currentUserName = "Lincoln Paul").getOrThrow()
        assertTrue(registered)
        assertEquals(5, count)

        val updated = repo.observeEvents().first()
        assertTrue(updated[0].isUserRegistered)
        assertEquals(5, updated[0].attendeeCount)
        assertTrue(updated[0].attendeeNames.contains("Lincoln Paul"))
    }

    @Test
    fun `PlacesRepository syncs places, bookmarks, and reviews from Supabase`() = runBlocking {
        val placesJson = """
            [
              {
                "id": "place_verified_101",
                "name": "Engineering Pavilion Spot",
                "subtitle": "Verified Locker Point",
                "address": "Engineering Wing",
                "category": "RENDEZVOUS",
                "campus_region": "Main Campus",
                "latitude": -1.3094,
                "longitude": 36.8122,
                "rating": 4.9,
                "review_count": 12,
                "is_open_now": true,
                "opening_hours": "Open 24/7",
                "is_verified_safe_spot": true,
                "features": ["wifi", "power", "cctv"],
                "neighborhood": "Engineering Quad",
                "security_level": "CCTV Monitored"
              }
            ]
        """.trimIndent()

        val bookmarksJson = """
            [
              {
                "place_id": "place_verified_101",
                "user_id": "STD-SELF",
                "status": "SAVED",
                "created_at_epoch_ms": 1730000000000
              },
              {
                "place_id": "place_verified_101",
                "user_id": "STD-FRIEND",
                "status": "BEEN",
                "created_at_epoch_ms": 1730000010000
              }
            ]
        """.trimIndent()

        val reviewsJson = """
            [
              {
                "id": "rev_1",
                "place_id": "place_verified_101",
                "user_id": "STD-FRIEND",
                "author_name": "Tessa Wanjiku",
                "comment": "Bright lighting for evening trades.",
                "created_at_epoch_ms": 1730000020000
              }
            ]
        """.trimIndent()

        val client = buildMockClient { path ->
            when {
                path.contains("campus_place_bookmarks") -> bookmarksJson
                path.contains("campus_place_reviews") -> reviewsJson
                path.contains("campus_places") -> placesJson
                else -> "[]"
            }
        }

        val repo = PlacesRepository(database = null, supabase = client)
        repo.syncPlaces(currentUserId = "STD-SELF", friendIds = setOf("STD-FRIEND")).getOrThrow()

        val loaded = repo.places.first()
        assertEquals(1, loaded.size)
        val place = loaded[0]
        assertEquals(PlaceCategory.RENDEZVOUS, place.category)
        assertTrue(place.isVerifiedSafeTrade)
        assertTrue(place.hasWifi)
        assertTrue(place.hasPowerOutlets)
        assertTrue(place.isSavedToTry)
        assertFalse(place.hasBeen)
        assertEquals(1, place.reviews.size)
        assertEquals("Tessa Wanjiku", place.reviews[0].authorName)
        assertTrue(repo.friendSavedPlaceIds.value.contains("place_verified_101"))
    }

    @Test
    fun `FlashMeetRow and toggle_flash_meet_attendance RPC decode properly`() = runBlocking {
        val meetJson = """
            [
              {
                "id": "meet-1",
                "title": "Calculus Sprint",
                "description": "Past papers review",
                "category_name": "STUDY_JAM",
                "latitude": -1.3094,
                "longitude": 36.8122,
                "location_name": "Main Library",
                "creator_name": "Imani Okafor",
                "creator_id": "STD-HOST",
                "created_at_epoch_ms": 1730000000000,
                "expires_at_epoch_ms": 1730003600000,
                "attendee_count": 3,
                "max_attendees": 6
              }
            ]
        """.trimIndent()

        val client = buildMockClient { path ->
            when {
                path.contains("toggle_flash_meet_attendance") -> """{"is_joined": true, "attendee_count": 4}"""
                else -> meetJson
            }
        }

        val meets: List<FlashMeetRow> = client.from("campus_flash_meets").select(Columns.ALL).decodeList()
        assertEquals(1, meets.size)
        assertEquals("STD-HOST", meets[0].creator_id)
        assertEquals(3, meets[0].attendee_count)

        val rpcObj = client.postgrest
            .rpc("toggle_flash_meet_attendance", MeetAttendanceRpcParams("meet-1"))
            .decodeAs<JsonObject>()
        assertEquals(true, (rpcObj["is_joined"] as? JsonPrimitive)?.booleanOrNull)
        assertEquals(4, (rpcObj["attendee_count"] as? JsonPrimitive)?.intOrNull)
    }
}
