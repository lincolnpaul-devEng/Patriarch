package com.campus.network.core.data

import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.cleanDisplayName
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemoryCodeVerifierCache
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests that verify Supabase SDK PostgREST integration at the data layer.
 *
 * Uses Ktor [MockEngine] to intercept HTTP calls issued by the Supabase SDK
 * and return canned JSON payloads that mirror real PostgREST responses.
 * No live network connection is required.
 *
 * Coverage:
 *  1. campusprofile SELECT → CampusProfileRow deserialization
 *  2. campus_market_items SELECT → MarketItemRow deserialization
 *  3. Profile name canonicalisation via cleanDisplayName
 *  4. findKnownProfile multi-key resolution
 *  5. SupabaseProfileNameResolver resolve + resolveWithAvatars
 */
class SupabaseSdkRepositoryTest {

    // ──────────────────────────────────────────────────────────────────────
    // Canned JSON fixtures matching real PostgREST response shapes
    // ──────────────────────────────────────────────────────────────────────

    private val campusProfilesJson = """
        [
          {
            "id": "c43b0556-0341-4fdb-994c-d046ea824a9f",
            "user_id": "f7991a02-d3a5-441f-b735-a08fc5376cd3",
            "student_id": "STD-3880914068",
            "full_name": "Lincoln Paul",
            "university_email": "paullincoln428@gmail.com",
            "university_name": "Kenyatta University",
            "avatar_url": "https://res.cloudinary.com/dpupj8sr0/image/upload/v1792448134/vjbugqvjgwu3vxrqgbjv.jpg",
            "degree_program": "Computer Science",
            "academic_year": "Year 3",
            "bio": "Android developer",
            "campus_residence": "Nyayo Hall",
            "phone_number": "+254700000000",
            "preferred_rendezvous_point": "Library Entrance",
            "notifications_enabled": true,
            "escrow_alerts_enabled": true,
            "quest_reminders_enabled": true,
            "is_trust_score_public": false,
            "is_online_status_visible": true,
            "activity_ledger_visibility": "FRIENDS",
            "location_privacy_scope": "CAMPUS_WIDE",
            "behavioral_trust_score": 85,
            "campus_merit_points": 250,
            "created_at": "2026-10-15T10:00:00Z",
            "updated_at": "2026-10-23T08:00:00Z"
          },
          {
            "id": "94632fc2-ff10-4368-9455-4455da02d9c5",
            "user_id": "6c189025-a1b6-4fd3-b3fa-0fc8a67438da",
            "student_id": "94632fc2-ff10-4368-9455-4455da02d9c5",
            "full_name": "Lincoln Paul",
            "university_email": "maujihtech@gmail.com",
            "university_name": "Kenyatta University",
            "avatar_url": "https://lh3.googleusercontent.com/a/ACg8ocIMy3FJ3ADQ1SebkQgFFDN8u0i405cWCn23CJ71OVYNoQVHeeo=s96-c",
            "degree_program": null,
            "academic_year": null,
            "bio": null,
            "campus_residence": null,
            "phone_number": null,
            "preferred_rendezvous_point": null,
            "notifications_enabled": true,
            "escrow_alerts_enabled": true,
            "quest_reminders_enabled": true,
            "is_trust_score_public": false,
            "is_online_status_visible": false,
            "activity_ledger_visibility": "FRIENDS",
            "location_privacy_scope": "CAMPUS_WIDE",
            "behavioral_trust_score": 50,
            "campus_merit_points": 0,
            "created_at": "2026-10-14T09:00:00Z",
            "updated_at": null
          }
        ]
    """.trimIndent()

    private val marketItemsJson = """
        [
          {
            "id": "item_c812f47d-cddb-4d28-9d77-b87dd48f76bc",
            "title": "Capture",
            "price": 2000.0,
            "currency": "KES",
            "category": "Electronics",
            "condition": "Good",
            "seller_name": "Lincoln Paul",
            "seller_id": "c43b0556-0341-4fdb-994c-d046ea824a9f",
            "seller_rating": 5.0,
            "is_campus_verified_seller": true,
            "campus_hall_location": "Afya Center",
            "image_url": null,
            "timestamp_ms": 1729500000000,
            "description": "Camera capture device",
            "pickup_spot_name": "Afya Center",
            "pickup_security_level": "CCTV Monitored Safe Zone",
            "pickup_latitude": -1.3090,
            "pickup_longitude": 36.8120,
            "pickup_timing": "Immediate (within 1 hr)",
            "payout_method": "Campus SafePay Escrow",
            "requires_qr_release_code": true
          }
        ]
    """.trimIndent()

    // ──────────────────────────────────────────────────────────────────────
    // Helper: build a SupabaseClient backed by MockEngine
    // ──────────────────────────────────────────────────────────────────────

    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    private fun buildMockClient(jsonBody: String): SupabaseClient {
        val engine = MockEngine { _ ->
            respond(
                content = jsonBody,
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
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                encodeDefaults = true
            })
            httpEngine = engine
            install(Postgrest)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. CampusProfileRow deserialization from PostgREST-style JSON
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `campusprofile SELECT deserializes to CampusProfileRow list`() = runBlocking {
        val client = buildMockClient(campusProfilesJson)

        val rows: List<CampusProfileRow> = client
            .from("campusprofile")
            .select(columns = Columns.ALL)
            .decodeList()

        assertEquals(2, rows.size)

        val first = rows[0]
        assertEquals("c43b0556-0341-4fdb-994c-d046ea824a9f", first.id)
        assertEquals("f7991a02-d3a5-441f-b735-a08fc5376cd3", first.user_id)
        assertEquals("STD-3880914068", first.student_id)
        assertEquals("Lincoln Paul", first.full_name)
        assertEquals("paullincoln428@gmail.com", first.university_email)
        assertEquals("Kenyatta University", first.university_name)
        assertNotNull(first.avatar_url)
        assertTrue(first.avatar_url!!.startsWith("https://"))
        assertEquals(85, first.behavioral_trust_score)
        assertEquals(250, first.campus_merit_points)
        assertTrue(first.notifications_enabled)
        assertTrue(first.is_online_status_visible)

        val second = rows[1]
        assertEquals("94632fc2-ff10-4368-9455-4455da02d9c5", second.id)
        assertEquals("maujihtech@gmail.com", second.university_email)
        // nullable fields should deserialize to null without exceptions
        assertEquals(null, second.degree_program)
        assertEquals(null, second.bio)
        assertEquals(null, second.campus_residence)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. MarketItemRow deserialization from PostgREST-style JSON
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `market items SELECT deserializes to MarketItemRow list`() = runBlocking {
        val client = buildMockClient(marketItemsJson)

        val rows: List<MarketItemRow> = client
            .from("campus_market_items")
            .select(columns = Columns.ALL)
            .decodeList()

        assertEquals(1, rows.size)

        val item = rows[0]
        assertEquals("item_c812f47d-cddb-4d28-9d77-b87dd48f76bc", item.id)
        assertEquals("Capture", item.title)
        assertEquals(2000.0, item.price, 0.01)
        assertEquals("KES", item.currency)
        assertEquals("Electronics", item.category)
        assertEquals("Good", item.condition)
        assertEquals("Lincoln Paul", item.seller_name)
        assertEquals("c43b0556-0341-4fdb-994c-d046ea824a9f", item.seller_id)
        assertEquals(5.0, item.seller_rating, 0.01)
        assertTrue(item.is_campus_verified_seller)
        assertEquals("Afya Center", item.campus_hall_location)
        assertEquals(null, item.image_url)
        assertTrue(item.requires_qr_release_code)
        assertEquals("Afya Center", item.pickup_spot_name)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. CampusProfileRow → StudentProfile domain mapping
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `CampusProfileRow maps to StudentProfile with canonical fields`() = runBlocking {
        val client = buildMockClient(campusProfilesJson)

        val rows: List<CampusProfileRow> = client
            .from("campusprofile")
            .select()
            .decodeList()

        val row = rows[0]
        val cleanName = cleanDisplayName(row.full_name)
        assertTrue(cleanName.isRealProfileDisplayName())

        val profile = StudentProfile(
            studentId = row.student_id?.trim().orEmpty().ifBlank { row.id.orEmpty() },
            fullName = cleanName,
            avatarUrl = row.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") },
            universityEmail = row.university_email?.trim().orEmpty(),
            universityName = row.university_name.orEmpty(),
            campusResidence = row.campus_residence.orEmpty(),
            degreeProgram = row.degree_program.orEmpty(),
            academicYear = row.academic_year.orEmpty(),
            bio = row.bio.orEmpty(),
            phoneNumber = row.phone_number.orEmpty(),
            behavioralTrustScore = row.behavioral_trust_score,
            campusMeritPoints = row.campus_merit_points
        )

        assertEquals("STD-3880914068", profile.studentId)
        assertEquals("Lincoln Paul", profile.fullName)
        assertEquals("paullincoln428@gmail.com", profile.universityEmail)
        assertNotNull(profile.avatarUrl)
        assertEquals("STD-3880914068", profile.stableUserId())
        assertEquals(85, profile.behavioralTrustScore)
        assertEquals(250, profile.campusMeritPoints)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. cleanDisplayName strips badge suffixes + underscore slugs
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `cleanDisplayName strips all known badge suffixes`() {
        assertEquals("Lincoln Paul", cleanDisplayName("Lincoln Paul (Verified Student)"))
        assertEquals("Lincoln Paul", cleanDisplayName("Lincoln Paul (Verified Student Seller)"))
        assertEquals("Lincoln Paul", cleanDisplayName("Lincoln Paul (Verified)"))
        assertEquals("Lincoln Paul", cleanDisplayName("Lincoln Paul (Seller)"))
        assertEquals("Lincoln Paul", cleanDisplayName("Lincoln Paul (Member)"))
    }

    @Test
    fun `cleanDisplayName converts underscore slugs to title case`() {
        assertEquals("Lincoln Paul", cleanDisplayName("lincoln_paul"))
        assertEquals("Lincoln Paul", cleanDisplayName("lincoln_paul__verified_student_"))
        assertEquals("Lincoln Paul", cleanDisplayName("lincoln_paul__verified_student_seller_"))
    }

    @Test
    fun `cleanDisplayName preserves already-clean names`() {
        assertEquals("Lincoln Paul", cleanDisplayName("Lincoln Paul"))
        assertEquals("Erick Ochieng", cleanDisplayName("Erick Ochieng"))
    }

    @Test
    fun `cleanDisplayName handles null and blank`() {
        assertEquals("", cleanDisplayName(null))
        assertEquals("", cleanDisplayName(""))
        assertEquals("", cleanDisplayName("   "))
    }

    @Test
    fun `isRealProfileDisplayName rejects generic placeholders`() {
        assertFalse("Campus Peer".isRealProfileDisplayName())
        assertFalse("Unknown".isRealProfileDisplayName())
        assertFalse("Student".isRealProfileDisplayName())
        assertFalse("Guest".isRealProfileDisplayName())
        assertFalse("".isRealProfileDisplayName())
    }

    @Test
    fun `isRealProfileDisplayName accepts legitimate names`() {
        assertTrue("Lincoln Paul".isRealProfileDisplayName())
        assertTrue("Erick Ochieng".isRealProfileDisplayName())
        assertTrue("Isack Kibet".isRealProfileDisplayName())
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. SupabaseProfileNameResolver: resolve + resolveWithAvatars
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `SupabaseProfileNameResolver resolve returns name map keyed by lowercase id`() = runBlocking {
        val client = buildMockClient(campusProfilesJson)
        val resolver = SupabaseProfileNameResolver(client)

        val result = resolver.resolve(
            listOf("c43b0556-0341-4fdb-994c-d046ea824a9f")
        )

        // The resolver should index at least the requested UUID (lowercase)
        assertTrue(result.isNotEmpty())
        // Values should be clean display names, not raw DB strings
        result.values.forEach { name ->
            assertTrue("Name '$name' should pass isRealProfileDisplayName", name.isRealProfileDisplayName())
        }
    }

    @Test
    fun `SupabaseProfileNameResolver resolveWithAvatars returns avatar URLs`() = runBlocking {
        val client = buildMockClient(campusProfilesJson)
        val resolver = SupabaseProfileNameResolver(client)

        val result = resolver.resolveWithAvatars(
            listOf("c43b0556-0341-4fdb-994c-d046ea824a9f")
        )

        assertTrue(result.isNotEmpty())
        val anyHasAvatar = result.values.any { it.avatarUrl != null }
        assertTrue("At least one profile should have an avatar URL", anyHasAvatar)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. PostgREST SELECT with filter and ordering compiles correctly
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `PostgREST select with filter and order compiles against SDK`() = runBlocking {
        val client = buildMockClient(marketItemsJson)

        // Mirrors the exact query pattern used by CampusHubRepository.syncRemoteMarketItems
        val rows: List<MarketItemRow> = client
            .from("campus_market_items")
            .select {
                filter {
                    gt("timestamp_ms", 0L)
                }
                order("timestamp_ms", order = Order.ASCENDING)
                limit(100)
            }
            .decodeList()

        assertEquals(1, rows.size)
        assertEquals("Capture", rows[0].title)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 7. Nullable-field robustness: JSON with all nulls still deserializes
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `CampusProfileRow deserializes gracefully with all nullable fields as null`() = runBlocking {
        val minimalJson = """
            [{
                "id": "abc-123",
                "full_name": "Test User",
                "notifications_enabled": true,
                "escrow_alerts_enabled": true,
                "quest_reminders_enabled": true,
                "is_trust_score_public": false,
                "is_online_status_visible": false,
                "activity_ledger_visibility": "FRIENDS",
                "location_privacy_scope": "CAMPUS_WIDE",
                "behavioral_trust_score": 50,
                "campus_merit_points": 0
            }]
        """.trimIndent()

        val client = buildMockClient(minimalJson)
        val rows: List<CampusProfileRow> = client
            .from("campusprofile")
            .select()
            .decodeList()

        assertEquals(1, rows.size)
        val row = rows[0]
        assertEquals("abc-123", row.id)
        assertEquals("Test User", row.full_name)
        assertEquals(null, row.user_id)
        assertEquals(null, row.student_id)
        assertEquals(null, row.university_email)
        assertEquals(null, row.avatar_url)
        assertEquals(null, row.degree_program)
        assertEquals(null, row.bio)
        assertEquals(null, row.campus_residence)
        assertEquals(null, row.phone_number)
        assertEquals(50, row.behavioral_trust_score)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 8. MarketItemRow with missing optional fields
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `MarketItemRow deserializes with default values for missing fields`() = runBlocking {
        val sparseJson = """
            [{
                "id": "item-sparse-001",
                "title": "Old Textbook",
                "price": 500.0,
                "seller_name": "Jane Doe",
                "seller_id": "user-xyz",
                "seller_rating": 4.5,
                "is_campus_verified_seller": false,
                "timestamp_ms": 1729500000000
            }]
        """.trimIndent()

        val client = buildMockClient(sparseJson)
        val rows: List<MarketItemRow> = client
            .from("campus_market_items")
            .select()
            .decodeList()

        assertEquals(1, rows.size)
        val row = rows[0]
        assertEquals("item-sparse-001", row.id)
        assertEquals("Old Textbook", row.title)
        assertEquals(500.0, row.price, 0.01)
        assertEquals("Jane Doe", row.seller_name)
        assertEquals("user-xyz", row.seller_id)
        assertFalse(row.is_campus_verified_seller)
        // Defaults should be applied for missing fields
        assertEquals("KES", row.currency)
        assertEquals("General", row.category)
        assertEquals("Good", row.condition)
        assertEquals("Main Campus", row.campus_hall_location)
        assertTrue(row.requires_qr_release_code)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 9. stableUserId prefers studentId, falls back to email
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `stableUserId returns studentId when available`() {
        val profile = StudentProfile(
            studentId = "STD-3880914068",
            universityEmail = "paullincoln428@gmail.com"
        )
        assertEquals("STD-3880914068", profile.stableUserId())
    }

    @Test
    fun `stableUserId falls back to email when studentId is blank`() {
        val profile = StudentProfile(
            studentId = "",
            universityEmail = "paullincoln428@gmail.com"
        )
        assertEquals("paullincoln428@gmail.com", profile.stableUserId())
    }

    // ══════════════════════════════════════════════════════════════════════
    // 10. Authorization header forwarding test
    // ══════════════════════════════════════════════════════════════════════

    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    @Test
    fun `SupabaseClient sends Authorization header when configured in httpConfig`() = runBlocking {
        var recordedAuthHeader: String? = null
        var recordedApiKeyHeader: String? = null
        val engine = MockEngine { request ->
            recordedAuthHeader = request.headers[HttpHeaders.Authorization]
            recordedApiKeyHeader = request.headers["apikey"]
            respond(
                content = campusProfilesJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = createSupabaseClient(
            supabaseUrl = "https://test.supabase.co",
            supabaseKey = "test-anon-key"
        ) {
            httpEngine = engine
            install(Auth) {
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
                enableLifecycleCallbacks = false
            }
            install(Postgrest)
        }
        client.auth.importAuthToken("custom-jwt-token-xyz")
        client.from("campusprofile").select().decodeList<CampusProfileRow>()
        println("RECORDED AUTH: $recordedAuthHeader")
        println("RECORDED APIKEY: $recordedApiKeyHeader")
        assertEquals("Bearer custom-jwt-token-xyz", recordedAuthHeader)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 11. CampusProfileWriteRow serialization excludes server-managed fields
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `CampusProfileWriteRow serializes without id or server-managed privilege fields`() {
        val row = CampusProfileWriteRow(
            user_id = "user-uuid-123",
            full_name = "Lincoln Paul",
            university_name = "Kenyatta University",
            degree_program = "Computer Science"
        )
        val serialized = Json.encodeToString(CampusProfileWriteRow.serializer(), row)

        assertTrue(serialized.contains(""""user_id":"user-uuid-123""""))
        assertTrue(serialized.contains(""""full_name":"Lincoln Paul""""))
        assertFalse(serialized.contains(""""student_id""""))
        assertFalse(serialized.contains(""""university_email""""))
        assertFalse(serialized.contains(""""behavioral_trust_score""""))
        assertFalse(serialized.contains(""""campus_merit_points""""))
        assertFalse(serialized.contains(""""wallet_balance""""))
        assertFalse(serialized.contains(""""vault_balance""""))
    }

    // ══════════════════════════════════════════════════════════════════════
    // 12. Transient vs permanent sync failure classification
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `PostgREST 5xx and 429 are transient while 4xx errors are permanent`() {
        val serverError = com.campus.network.core.network.PostgrestHttpException(500, "Database timeout")
        val rateLimit = com.campus.network.core.network.PostgrestHttpException(429, "Too many requests")
        val badRequest = com.campus.network.core.network.PostgrestHttpException(400, "Invalid column")
        val forbidden = com.campus.network.core.network.PostgrestHttpException(403, "RLS violation")

        assertTrue(serverError.isTransient)
        assertTrue(rateLimit.isTransient)
        assertFalse(badRequest.isTransient)
        assertFalse(forbidden.isTransient)
    }
}
