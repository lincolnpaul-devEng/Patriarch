package com.campus.network.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemoryCodeVerifierCache
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration test suite verifying that remote database writes across
 * all core entities (servers, members, channels, rules, invites, profiles, marketplace, chat)
 * commit directly to remote Supabase without skipping or silent local-only fallbacks.
 *
 * Uses the official Supabase Kotlin SDK with Postgrest and Auth plugins.
 */
class RemoteDatabaseWritesIntegrationTest {

    private val supabaseUrl = System.getenv("SUPABASE_TEST_URL").orEmpty()
    private val anonKey = System.getenv("SUPABASE_TEST_ANON_KEY").orEmpty()
    private val userJwt = System.getenv("SUPABASE_TEST_USER_JWT").orEmpty()

    private lateinit var client: SupabaseClient
    private val testServerId = "srv_test_integration_${System.currentTimeMillis()}"
    private val testUserId = "STD-TEST-${System.currentTimeMillis().toString().takeLast(6)}"
    private val testEmail = "test_student_${System.currentTimeMillis()}@campus.ac.ke"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Serializable
    data class CreateServerAtomicParams(
        val p_server_id: String,
        val p_name: String,
        val p_category: String,
        val p_description: String = "",
        val p_owner_id: String,
        val p_owner_display_name: String = "Test Founder",
        val p_owner_handle: String = "@testfounder",
        val p_avatar_res_name: String = "server_gradient",
        val p_banner_res_name: String = "banner_gradient_cyber"
    )

    @Serializable
    data class ServerChannelWriteRow(
        val id: String,
        val server_id: String,
        val name: String,
        val category_name: String,
        val channel_type: String,
        val topic: String,
        val is_unread: Boolean = false,
        val order_index: Int = 0,
        val created_at_epoch_ms: Long
    )

    @Serializable
    data class ServerRuleWriteRow(
        val id: String,
        val server_id: String,
        val rule_index: Int,
        val title: String,
        val description: String
    )

    @Serializable
    data class ServerMemberWriteRow(
        val id: String,
        val server_id: String,
        val user_id: String,
        val display_name: String,
        val handle: String,
        val role: String,
        val role_title: String,
        val role_color_hex: Long,
        val is_online: Boolean,
        val joined_at_epoch_ms: Long
    )

    @Serializable
    data class MarketItemWriteRow(
        val id: String,
        val title: String,
        val description: String,
        val price: Double,
        val currency: String,
        val category: String,
        val seller_name: String,
        val seller_id: String,
        val timestamp_ms: Long
    )

    @Serializable
    data class ChatMessageWriteRow(
        val id: String,
        val channel_id: String,
        val sender_id: String,
        val sender_name: String,
        val text: String,
        val timestamp_epoch_ms: Long,
        val is_delivered: Boolean,
        val is_read: Boolean
    )

    @Serializable
    data class MicroGigWriteRow(
        val id: String,
        val title: String,
        val description: String,
        val payout_amount: Double,
        val currency: String,
        val estimated_duration: String,
        val poster_name: String,
        val poster_trust_score: Int,
        val tag: String,
        val urgency_name: String,
        val spots_available: Int,
        val timestamp_ms: Long
    )

    @Serializable
    data class EventWriteRow(
        val id: String,
        val title: String,
        val description: String,
        val location_name: String,
        val latitude: Double,
        val longitude: Double,
        val start_time: String,
        val category: String,
        val attendee_count: Int
    )

    @Serializable
    data class LiveStreamWriteRow(
        val id: String,
        val broadcaster_id: String,
        val broadcaster_name: String,
        val broadcaster_handle: String,
        val title: String,
        val description: String,
        val category: String,
        val audience: String,
        val is_live: Boolean,
        val viewer_count: Int,
        val peak_viewers: Int,
        val like_count: Int,
        val started_at_epoch_ms: Long
    )

    @Serializable
    data class StreamCommentWriteRow(
        val id: String,
        val stream_id: String,
        val sender_id: String,
        val sender_name: String,
        val comment_text: String,
        val timestamp_epoch_ms: Long
    )

    @Serializable
    data class FlashMeetWriteRow(
        val id: String,
        val title: String,
        val description: String,
        val category_name: String,
        val latitude: Double,
        val longitude: Double,
        val location_name: String,
        val creator_name: String,
        val created_at_epoch_ms: Long,
        val expires_at_epoch_ms: Long,
        val attendee_count: Int,
        val max_attendees: Int
    )

    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    @Before
    fun setUp() {
        assumeTrue(
            "Live writes require an explicitly opted-in disposable test project",
            System.getenv("RUN_LIVE_SUPABASE_TESTS") == "1" &&
                supabaseUrl.isNotBlank() && anonKey.isNotBlank() && userJwt.isNotBlank()
        )
        client = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = anonKey
        ) {
            defaultSerializer = KotlinXSerializer(json)
            install(Auth) {
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
                enableLifecycleCallbacks = false
            }
            install(Postgrest)
        }
        runBlocking {
            client.auth.importAuthToken(userJwt)
        }
    }

    @After
    fun tearDown() {
        if (!::client.isInitialized) return
        runBlocking {
            runCatching { client.from("servers").delete { filter { eq("id", testServerId) } } }
            runCatching { client.from("server_members").delete { filter { eq("server_id", testServerId) } } }
            runCatching { client.from("server_channels").delete { filter { eq("server_id", testServerId) } } }
            runCatching { client.from("server_rules").delete { filter { eq("server_id", testServerId) } } }
            runCatching { client.from("server_invites").delete { filter { eq("server_id", testServerId) } } }
            runCatching { client.from("campusprofile").delete { filter { eq("university_email", testEmail) } } }
            runCatching { client.from("campusprofile").delete { filter { eq("student_id", testUserId) } } }
        }
    }

    private suspend inline fun <reified T : Any> callRpc(name: String, parameters: T): Result<String> = runCatching {
        client.postgrest.rpc(name, parameters).data
    }

    private suspend fun queryTable(tableName: String, request: PostgrestRequestBuilder.() -> Unit = {}): JsonArray {
        val raw = client.from(tableName).select(request = request).data
        return json.parseToJsonElement(raw).jsonArray
    }

    @Test
    fun testAtomicServerCreationAndVerification() {
        runBlocking {
        val rpcPayload = CreateServerAtomicParams(
            p_server_id = testServerId,
            p_name = "Automated Test Campus",
            p_category = "Engineering",
            p_description = "Integration test verification server",
            p_owner_id = testUserId
        )

        val rpcResult = callRpc("create_server_atomic", rpcPayload)
        assertTrue("Atomic server creation RPC failed: ${rpcResult.exceptionOrNull()?.message}", rpcResult.isSuccess)

        val serverBundle = json.parseToJsonElement(rpcResult.getOrThrow()).jsonObject
        assertEquals(testServerId, serverBundle["id"]?.jsonPrimitive?.content)
        assertEquals("Automated Test Campus", serverBundle["name"]?.jsonPrimitive?.content)

        val srvArray = queryTable("servers") { filter { eq("id", testServerId) } }
        assertEquals(1, srvArray.size)
        assertEquals("Automated Test Campus", srvArray[0].jsonObject["name"]?.jsonPrimitive?.content)

        val memArray = queryTable("server_members") { filter { eq("server_id", testServerId) } }
        assertTrue("Owner member not found in remote database", memArray.isNotEmpty())
        assertEquals("OWNER", memArray[0].jsonObject["role"]?.jsonPrimitive?.content)

        val chArray = queryTable("server_channels") { filter { eq("server_id", testServerId) } }
        assertEquals("Expected 3 default channels (rules, announcements, general)", 3, chArray.size)

        val rulesArray = queryTable("server_rules") { filter { eq("server_id", testServerId) } }
        assertEquals("Expected 3 default server rules", 3, rulesArray.size)

        val invArray = queryTable("server_invites") { filter { eq("server_id", testServerId) } }
        assertEquals(1, invArray.size)
        }
    }
    @Test
    fun testServerUpdateRemoteWrite() {
        runBlocking {
        val rpcPayload = CreateServerAtomicParams(
            p_server_id = testServerId,
            p_name = "Server Before Update",
            p_category = "Community",
            p_owner_id = testUserId
        )
        callRpc("create_server_atomic", rpcPayload).getOrThrow()

        @Serializable
        data class ServerUpdate(val name: String, val description: String)
        client.from("servers").update(ServerUpdate("Server After Update", "Updated remote description")) {
            filter { eq("id", testServerId) }
        }

        val array = queryTable("servers") { filter { eq("id", testServerId) } }
        assertEquals("Server After Update", array[0].jsonObject["name"]?.jsonPrimitive?.content)
        assertEquals("Updated remote description", array[0].jsonObject["description"]?.jsonPrimitive?.content)
        }
    }
    @Test
    fun testChannelCrudRemoteWrites() {
        runBlocking {
        val rpcPayload = CreateServerAtomicParams(
            p_server_id = testServerId,
            p_name = "Channel Test Server",
            p_category = "Tech",
            p_owner_id = testUserId
        )
        callRpc("create_server_atomic", rpcPayload).getOrThrow()

        val customChannelId = "ch_${testServerId}_lab_discussions"

        val channelPayload = ServerChannelWriteRow(
            id = customChannelId,
            server_id = testServerId,
            name = "lab-discussions",
            category_name = "LABS",
            channel_type = "text",
            topic = "Hardware lab notes",
            created_at_epoch_ms = System.currentTimeMillis()
        )

        client.from("server_channels").upsert(channelPayload, onConflict = "id")

        val selectArray = queryTable("server_channels") { filter { eq("id", customChannelId) } }
        assertEquals(1, selectArray.size)
        assertEquals("lab-discussions", selectArray[0].jsonObject["name"]?.jsonPrimitive?.content)

        client.from("server_channels").delete { filter { eq("id", customChannelId) } }

        val afterArray = queryTable("server_channels") { filter { eq("id", customChannelId) } }
        assertEquals(0, afterArray.size)
        }
    }
    @Test
    fun testServerRulesCrudRemoteWrites() {
        runBlocking {
        val rpcPayload = CreateServerAtomicParams(
            p_server_id = testServerId,
            p_name = "Rules Test Server",
            p_category = "Tech",
            p_owner_id = testUserId
        )
        callRpc("create_server_atomic", rpcPayload).getOrThrow()

        val customRuleId = "rule_${testServerId}_4"

        val rulePayload = ServerRuleWriteRow(
            id = customRuleId,
            server_id = testServerId,
            rule_index = 4,
            title = "Post in English or Swahili",
            description = "Maintain community clarity across languages."
        )

        client.from("server_rules").upsert(rulePayload, onConflict = "id")

        val selectArray = queryTable("server_rules") { filter { eq("id", customRuleId) } }
        assertEquals(1, selectArray.size)
        assertEquals("Post in English or Swahili", selectArray[0].jsonObject["title"]?.jsonPrimitive?.content)

        client.from("server_rules").delete { filter { eq("id", customRuleId) } }

        val afterArray = queryTable("server_rules") { filter { eq("id", customRuleId) } }
        assertEquals(0, afterArray.size)
        }
    }
    @Test
    fun testServerMemberRoleRemoteWrites() {
        runBlocking {
        val rpcPayload = CreateServerAtomicParams(
            p_server_id = testServerId,
            p_name = "Member Test Server",
            p_category = "Study Jam",
            p_owner_id = testUserId
        )
        callRpc("create_server_atomic", rpcPayload).getOrThrow()

        val peerUserId = "STD-PEER-${System.currentTimeMillis().toString().takeLast(6)}"
        val peerMemberId = "${testServerId}:${peerUserId}"

        val memberPayload = ServerMemberWriteRow(
            id = peerMemberId,
            server_id = testServerId,
            user_id = peerUserId,
            display_name = "Peer Contributor",
            handle = "@peercontrib",
            role = "MEMBER",
            role_title = "Member",
            role_color_hex = -10722192L,
            is_online = true,
            joined_at_epoch_ms = System.currentTimeMillis()
        )

        client.from("server_members").upsert(memberPayload, onConflict = "id")

        @Serializable
        data class RoleUpdate(val role: String, val role_title: String, val role_color_hex: Long)
        client.from("server_members").update(RoleUpdate("MODERATOR", "Server Moderator", 2336090L)) {
            filter { eq("id", peerMemberId) }
        }

        val verifyArray = queryTable("server_members") { filter { eq("id", peerMemberId) } }
        assertEquals(1, verifyArray.size)
        assertEquals("MODERATOR", verifyArray[0].jsonObject["role"]?.jsonPrimitive?.content)

        client.from("server_members").delete { filter { eq("id", peerMemberId) } }
        }
    }
    @Test
    fun testCampusProfileRemoteWriteAndSync() {
        runBlocking {
        @Serializable
        data class ProfileWrite(
            val full_name: String,
            val university_email: String,
            val university_name: String,
            val degree_program: String,
            val academic_year: String,
            val student_id: String,
            val bio: String,
            val avatar_url: String
        )
        val profilePayload = ProfileWrite(
            full_name = "Integration Test Student",
            university_email = testEmail,
            university_name = "University of Nairobi",
            degree_program = "Electrical and Information Engineering",
            academic_year = "Year 3",
            student_id = testUserId,
            bio = "Testing remote profile persistence",
            avatar_url = "https://res.cloudinary.com/dpupj8sr0/image/upload/v1792448134/sample.jpg"
        )

        client.from("campusprofile").upsert(profilePayload, onConflict = "university_email")

        val profileArray = queryTable("campusprofile") { filter { eq("university_email", testEmail) } }
        assertTrue("Student profile was not found in remote Supabase", profileArray.isNotEmpty())
        assertEquals("Integration Test Student", profileArray[0].jsonObject["full_name"]?.jsonPrimitive?.content)
        assertEquals("Electrical and Information Engineering", profileArray[0].jsonObject["degree_program"]?.jsonPrimitive?.content)

        @Serializable
        data class BioUpdate(val bio: String)
        client.from("campusprofile").update(BioUpdate("Updated bio on cloud")) {
            filter { eq("university_email", testEmail) }
        }

        val updatedArray = queryTable("campusprofile") { filter { eq("university_email", testEmail) } }
        assertEquals("Updated bio on cloud", updatedArray[0].jsonObject["bio"]?.jsonPrimitive?.content)

        client.from("campusprofile").delete { filter { eq("university_email", testEmail) } }
        }
    }
    @Test
    fun testMarketplaceItemRemoteWrites() {
        runBlocking {
        val itemId = "item_test_${System.currentTimeMillis()}"

        val itemPayload = MarketItemWriteRow(
            id = itemId,
            title = "Engineering Textbooks Bundle",
            description = "Circuit analysis 8th edition and DSP guide.",
            price = 1500.0,
            currency = "KES",
            category = "Books",
            seller_name = "Test Student",
            seller_id = "test_seller_${System.currentTimeMillis()}",
            timestamp_ms = System.currentTimeMillis()
        )

        client.from("campus_market_items").upsert(itemPayload, onConflict = "id")

        val array = queryTable("campus_market_items") { filter { eq("id", itemId) } }
        assertEquals(1, array.size)
        assertEquals("Engineering Textbooks Bundle", array[0].jsonObject["title"]?.jsonPrimitive?.content)

        client.from("campus_market_items").delete { filter { eq("id", itemId) } }
        }
    }
    @Test
    fun testChatMessageRemoteWrites() {
        runBlocking {
        val msgId = "msg_test_${System.currentTimeMillis()}"

        val msgPayload = ChatMessageWriteRow(
            id = msgId,
            channel_id = "ch_direct_test_stream",
            sender_id = testUserId,
            sender_name = "Test Student",
            text = "Direct chat remote persistence verification",
            timestamp_epoch_ms = System.currentTimeMillis(),
            is_delivered = true,
            is_read = false
        )

        client.from("campus_chat_messages").upsert(msgPayload, onConflict = "id")

        val array = queryTable("campus_chat_messages") { filter { eq("id", msgId) } }
        assertEquals(1, array.size)
        assertEquals("Direct chat remote persistence verification", array[0].jsonObject["text"]?.jsonPrimitive?.content)

        client.from("campus_chat_messages").delete { filter { eq("id", msgId) } }
        }
    }
    @Test
    fun testMicroGigCreationRemoteWrites() {
        runBlocking {
        val gigId = "gig_test_${System.currentTimeMillis()}"

        val gigPayload = MicroGigWriteRow(
            id = gigId,
            title = "Circuit Simulation in Proteus",
            description = "Need help debugging an operational amplifier simulation before Friday.",
            payout_amount = 800.0,
            currency = "KES",
            estimated_duration = "2 hours",
            poster_name = "Test Student",
            poster_trust_score = 95,
            tag = "Electronics",
            urgency_name = "URGENT",
            spots_available = 1,
            timestamp_ms = System.currentTimeMillis()
        )

        client.from("campus_micro_gigs").upsert(gigPayload, onConflict = "id")

        val array = queryTable("campus_micro_gigs") { filter { eq("id", gigId) } }
        assertEquals(1, array.size)
        assertEquals("Circuit Simulation in Proteus", array[0].jsonObject["title"]?.jsonPrimitive?.content)
        assertEquals(800.0, array[0].jsonObject["payout_amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0, 0.001)

        @Serializable
        data class SpotsUpdate(val spots_available: Int)
        client.from("campus_micro_gigs").update(SpotsUpdate(0)) { filter { eq("id", gigId) } }

        client.from("campus_micro_gigs").delete { filter { eq("id", gigId) } }
        }
    }
    @Test
    fun testEventCreationRemoteWrites() {
        runBlocking {
        val eventId = "evt_test_${System.currentTimeMillis()}"

        val eventPayload = EventWriteRow(
            id = eventId,
            title = "Campus Developers Summit",
            description = "Annual gathering for campus mobile and cloud engineers.",
            location_name = "Central Lecture Hall A",
            latitude = -1.2800,
            longitude = 36.8150,
            start_time = "2026-10-25T14:00:00Z",
            category = "TECH",
            attendee_count = 25
        )

        client.from("campus_events").upsert(eventPayload, onConflict = "id")

        val array = queryTable("campus_events") { filter { eq("id", eventId) } }
        assertEquals(1, array.size)
        assertEquals("Campus Developers Summit", array[0].jsonObject["title"]?.jsonPrimitive?.content)
        assertEquals("Central Lecture Hall A", array[0].jsonObject["location_name"]?.jsonPrimitive?.content)

        @Serializable
        data class AttendeeUpdate(val attendee_count: Int)
        client.from("campus_events").update(AttendeeUpdate(26)) { filter { eq("id", eventId) } }

        client.from("campus_events").delete { filter { eq("id", eventId) } }
        }
    }
    @Test
    fun testLiveStreamCreationAndEndRemoteWrites() {
        runBlocking {
        val streamId = "stream_test_${System.currentTimeMillis()}"

        val streamPayload = LiveStreamWriteRow(
            id = streamId,
            broadcaster_id = testUserId,
            broadcaster_name = "Student Developer",
            broadcaster_handle = "@stdev",
            title = "Building Kotlin Compose Multiplatform Apps",
            description = "Live coding session explaining state hoisting.",
            category = "Tech",
            audience = "CAMPUS",
            is_live = true,
            viewer_count = 1,
            peak_viewers = 1,
            like_count = 0,
            started_at_epoch_ms = System.currentTimeMillis()
        )

        client.from("campus_live_streams").upsert(streamPayload, onConflict = "id")

        val array = queryTable("campus_live_streams") { filter { eq("id", streamId) } }
        assertEquals(1, array.size)
        assertEquals("Building Kotlin Compose Multiplatform Apps", array[0].jsonObject["title"]?.jsonPrimitive?.content)
        assertEquals(true, array[0].jsonObject["is_live"]?.jsonPrimitive?.booleanOrNull)

        @Serializable
        data class EndStreamUpdate(
            val is_live: Boolean,
            val duration_seconds: Int,
            val peak_viewers: Int,
            val like_count: Int,
            val ended_at_epoch_ms: Long
        )
        client.from("campus_live_streams").update(
            EndStreamUpdate(
                is_live = false,
                duration_seconds = 3600,
                peak_viewers = 45,
                like_count = 120,
                ended_at_epoch_ms = System.currentTimeMillis()
            )
        ) { filter { eq("id", streamId) } }

        val verifyArray = queryTable("campus_live_streams") { filter { eq("id", streamId) } }
        val verifyObj = verifyArray[0].jsonObject
        assertEquals(false, verifyObj["is_live"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(45, verifyObj["peak_viewers"]?.jsonPrimitive?.intOrNull)
        assertEquals(120, verifyObj["like_count"]?.jsonPrimitive?.intOrNull)

        client.from("campus_live_streams").delete { filter { eq("id", streamId) } }
        }
    }
    @Test
    fun testStreamCommentRemoteWrites() {
        runBlocking {
        val commentId = "c_test_${System.currentTimeMillis()}"
        val streamId = "stream_test_cmnt_${System.currentTimeMillis()}"

        val commentPayload = StreamCommentWriteRow(
            id = commentId,
            stream_id = streamId,
            sender_id = testUserId,
            sender_name = "Student Reviewer",
            comment_text = "Great architecture overview!",
            timestamp_epoch_ms = System.currentTimeMillis()
        )

        client.from("campus_stream_comments").upsert(commentPayload, onConflict = "id")

        val array = queryTable("campus_stream_comments") { filter { eq("id", commentId) } }
        assertEquals(1, array.size)
        assertEquals("Great architecture overview!", array[0].jsonObject["comment_text"]?.jsonPrimitive?.content)

        client.from("campus_stream_comments").delete { filter { eq("id", commentId) } }
        }
    }
    @Test
    fun testFlashMeetRemoteWrites() {
        runBlocking {
        val meetId = "meet_test_${System.currentTimeMillis()}"

        val meetPayload = FlashMeetWriteRow(
            id = meetId,
            title = "Algorithms Discussion Group",
            description = "Preparing for dynamic programming exam.",
            category_name = "STUDY_JAM",
            latitude = -1.3090,
            longitude = 36.8120,
            location_name = "Science Library 2nd Floor",
            creator_name = "Test Student",
            created_at_epoch_ms = System.currentTimeMillis(),
            expires_at_epoch_ms = System.currentTimeMillis() + 3600000L,
            attendee_count = 2,
            max_attendees = 6
        )

        client.from("campus_flash_meets").upsert(meetPayload, onConflict = "id")

        val array = queryTable("campus_flash_meets") { filter { eq("id", meetId) } }
        assertEquals(1, array.size)
        assertEquals("Algorithms Discussion Group", array[0].jsonObject["title"]?.jsonPrimitive?.content)

        client.from("campus_flash_meets").delete { filter { eq("id", meetId) } }
        }
    }
}