package com.campus.network.core.network

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration test suite verifying that remote database writes across
 * all core entities (servers, members, channels, rules, invites, profiles, marketplace, chat)
 * commit directly to remote Supabase without skipping or silent local-only fallbacks.
 */
class RemoteDatabaseWritesIntegrationTest {

    private val supabaseUrl = System.getenv("SUPABASE_TEST_URL").orEmpty()
    private val anonKey = System.getenv("SUPABASE_TEST_ANON_KEY").orEmpty()
    private val userJwt = System.getenv("SUPABASE_TEST_USER_JWT").orEmpty()

    private lateinit var client: SupabaseClientProvider
    private val testServerId = "srv_test_integration_${System.currentTimeMillis()}"
    private val testUserId = "STD-TEST-${System.currentTimeMillis().toString().takeLast(6)}"
    private val testEmail = "test_student_${System.currentTimeMillis()}@campus.ac.ke"

    @Before
    fun setUp() {
        assumeTrue("Live writes require an explicitly opted-in disposable test project",
            System.getenv("RUN_LIVE_SUPABASE_TESTS") == "1" &&
                supabaseUrl.isNotBlank() && anonKey.isNotBlank() && userJwt.isNotBlank())
        client = SupabaseClientProvider(supabaseUrl = supabaseUrl, anonKey = anonKey,
            userJwtProvider = { userJwt })
    }

    @After
    fun tearDown() {
        if (!::client.isInitialized) return
        runBlocking {
            // Clean up test server and cascading entries
            client.deleteRecord("servers?id=eq.$testServerId")
            client.deleteRecord("server_members?server_id=eq.$testServerId")
            client.deleteRecord("server_channels?server_id=eq.$testServerId")
            client.deleteRecord("server_rules?server_id=eq.$testServerId")
            client.deleteRecord("server_invites?server_id=eq.$testServerId")
            client.deleteRecord("campusprofile?email=eq.$testEmail")
            client.deleteRecord("campusprofile?student_id=eq.$testUserId")
        }
    }

    @Test
    fun testAtomicServerCreationAndVerification() {
        runBlocking {
            // 1. One-send atomic server creation via RPC
            val rpcPayload = JSONObject().apply {
                put("p_server_id", testServerId)
                put("p_name", "Automated Test Campus")
                put("p_category", "Engineering")
                put("p_description", "Integration test verification server")
                put("p_owner_id", testUserId)
                put("p_owner_display_name", "Test Founder")
                put("p_owner_handle", "@testfounder")
                put("p_avatar_res_name", "server_gradient")
                put("p_banner_res_name", "banner_gradient_cyber")
            }.toString()

            val rpcResult = client.callRpc("create_server_atomic", rpcPayload)
            assertTrue("Atomic server creation RPC failed: ${rpcResult.exceptionOrNull()?.message}", rpcResult.isSuccess)

            val serverBundle = JSONObject(rpcResult.getOrThrow())
            assertEquals(testServerId, serverBundle.getString("id"))
            assertEquals("Automated Test Campus", serverBundle.getString("name"))

            // 2. Verify server persisted in remote Supabase table
            val srvQuery = client.queryTable("servers?id=eq.$testServerId")
            assertTrue(srvQuery.isSuccess)
            val srvArray = JSONArray(srvQuery.getOrThrow())
            assertEquals(1, srvArray.length())
            assertEquals("Automated Test Campus", srvArray.getJSONObject(0).getString("name"))

            // 3. Verify owner member persisted
            val memQuery = client.queryTable("server_members?server_id=eq.$testServerId")
            assertTrue(memQuery.isSuccess)
            val memArray = JSONArray(memQuery.getOrThrow())
            assertTrue("Owner member not found in remote database", memArray.length() >= 1)
            assertEquals("OWNER", memArray.getJSONObject(0).getString("role"))

            // 4. Verify default channels persisted
            val chQuery = client.queryTable("server_channels?server_id=eq.$testServerId")
            assertTrue(chQuery.isSuccess)
            val chArray = JSONArray(chQuery.getOrThrow())
            assertEquals("Expected 3 default channels (rules, announcements, general)", 3, chArray.length())

            // 5. Verify default rules persisted
            val rulesQuery = client.queryTable("server_rules?server_id=eq.$testServerId")
            assertTrue(rulesQuery.isSuccess)
            val rulesArray = JSONArray(rulesQuery.getOrThrow())
            assertEquals("Expected 3 default server rules", 3, rulesArray.length())

            // 6. Verify invite token persisted
            val invQuery = client.queryTable("server_invites?server_id=eq.$testServerId")
            assertTrue(invQuery.isSuccess)
            val invArray = JSONArray(invQuery.getOrThrow())
            assertEquals(1, invArray.length())
        }
    }

    @Test
    fun testServerUpdateRemoteWrite() {
        runBlocking {
            // Create server first
            val rpcPayload = JSONObject().apply {
                put("p_server_id", testServerId)
                put("p_name", "Server Before Update")
                put("p_category", "Community")
                put("p_owner_id", testUserId)
            }.toString()
            client.callRpc("create_server_atomic", rpcPayload).getOrThrow()

            // Execute remote UPDATE
            val updatePayload = JSONObject().apply {
                put("name", "Server After Update")
                put("description", "Updated remote description")
            }.toString()
            val updateResult = client.updateRecord("servers?id=eq.$testServerId", updatePayload)
            assertTrue("Remote update failed: ${updateResult.exceptionOrNull()?.message}", updateResult.isSuccess)

            // Verify update in remote Supabase
            val verifyQuery = client.queryTable("servers?id=eq.$testServerId")
            val array = JSONArray(verifyQuery.getOrThrow())
            assertEquals("Server After Update", array.getJSONObject(0).getString("name"))
            assertEquals("Updated remote description", array.getJSONObject(0).getString("description"))
        }
    }

    @Test
    fun testChannelCrudRemoteWrites() {
        runBlocking {
            // Create base server
            val rpcPayload = JSONObject().apply {
                put("p_server_id", testServerId)
                put("p_name", "Channel Test Server")
                put("p_category", "Tech")
                put("p_owner_id", testUserId)
            }.toString()
            client.callRpc("create_server_atomic", rpcPayload).getOrThrow()

            val customChannelId = "ch_${testServerId}_lab_discussions"

            // 1. Remote INSERT channel
            val channelPayload = JSONObject().apply {
                put("id", customChannelId)
                put("server_id", testServerId)
                put("name", "lab-discussions")
                put("category_name", "LABS")
                put("channel_type", "text")
                put("topic", "Hardware lab notes")
                put("is_unread", false)
                put("order_index", 4)
                put("created_at_epoch_ms", System.currentTimeMillis())
            }.toString()

            val insertResult = client.upsertRecord("server_channels", channelPayload, onConflict = "id")
            assertTrue("Channel remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("server_channels?id=eq.$customChannelId")
            val selectArray = JSONArray(selectResult.getOrThrow())
            assertEquals(1, selectArray.length())
            assertEquals("lab-discussions", selectArray.getJSONObject(0).getString("name"))

            // 3. Remote DELETE channel
            val deleteResult = client.deleteRecord("server_channels?id=eq.$customChannelId")
            assertTrue("Channel remote delete failed: ${deleteResult.exceptionOrNull()?.message}", deleteResult.isSuccess)

            // 4. Verify channel is gone from remote database
            val afterDelete = client.queryTable("server_channels?id=eq.$customChannelId")
            val afterArray = JSONArray(afterDelete.getOrThrow())
            assertEquals(0, afterArray.length())
        }
    }

    @Test
    fun testServerRulesCrudRemoteWrites() {
        runBlocking {
            // Create base server
            val rpcPayload = JSONObject().apply {
                put("p_server_id", testServerId)
                put("p_name", "Rules Test Server")
                put("p_category", "Tech")
                put("p_owner_id", testUserId)
            }.toString()
            client.callRpc("create_server_atomic", rpcPayload).getOrThrow()

            val customRuleId = "rule_${testServerId}_4"

            // 1. Remote INSERT rule
            val rulePayload = JSONObject().apply {
                put("id", customRuleId)
                put("server_id", testServerId)
                put("rule_index", 4)
                put("title", "Post in English or Swahili")
                put("description", "Maintain community clarity across languages.")
            }.toString()

            val insertResult = client.upsertRecord("server_rules", rulePayload, onConflict = "id")
            assertTrue("Rule remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("server_rules?id=eq.$customRuleId")
            val selectArray = JSONArray(selectResult.getOrThrow())
            assertEquals(1, selectArray.length())
            assertEquals("Post in English or Swahili", selectArray.getJSONObject(0).getString("title"))

            // 3. Remote DELETE rule
            val deleteResult = client.deleteRecord("server_rules?id=eq.$customRuleId")
            assertTrue("Rule remote delete failed: ${deleteResult.exceptionOrNull()?.message}", deleteResult.isSuccess)

            // 4. Verify deletion
            val afterDelete = client.queryTable("server_rules?id=eq.$customRuleId")
            val afterArray = JSONArray(afterDelete.getOrThrow())
            assertEquals(0, afterArray.length())
        }
    }

    @Test
    fun testServerMemberRoleRemoteWrites() {
        runBlocking {
            // Create base server
            val rpcPayload = JSONObject().apply {
                put("p_server_id", testServerId)
                put("p_name", "Member Test Server")
                put("p_category", "Study Jam")
                put("p_owner_id", testUserId)
            }.toString()
            client.callRpc("create_server_atomic", rpcPayload).getOrThrow()

            val peerUserId = "STD-PEER-${System.currentTimeMillis().toString().takeLast(6)}"
            val peerMemberId = "${testServerId}:${peerUserId}"

            // 1. Remote INSERT peer member
            val memberPayload = JSONObject().apply {
                put("id", peerMemberId)
                put("server_id", testServerId)
                put("user_id", peerUserId)
                put("display_name", "Peer Contributor")
                put("handle", "@peercontrib")
                put("role", "MEMBER")
                put("role_title", "Member")
                put("role_color_hex", 0xFF5C6470)
                put("is_online", true)
                put("joined_at_epoch_ms", System.currentTimeMillis())
            }.toString()

            val joinResult = client.upsertRecord("server_members", memberPayload, onConflict = "id")
            assertTrue("Member join remote write failed: ${joinResult.exceptionOrNull()?.message}", joinResult.isSuccess)

            // 2. Remote UPDATE role to MODERATOR
            val updatePayload = JSONObject().apply {
                put("role", "MODERATOR")
                put("role_title", "Server Moderator")
                put("role_color_hex", 0xFF23A55A)
            }.toString()

            val roleUpdateResult = client.updateRecord("server_members?id=eq.$peerMemberId", updatePayload)
            assertTrue("Role update remote write failed: ${roleUpdateResult.exceptionOrNull()?.message}", roleUpdateResult.isSuccess)

            // 3. Verify updated role in Supabase
            val verifyResult = client.queryTable("server_members?id=eq.$peerMemberId")
            val verifyArray = JSONArray(verifyResult.getOrThrow())
            assertEquals(1, verifyArray.length())
            assertEquals("MODERATOR", verifyArray.getJSONObject(0).getString("role"))

            // 4. Remote DELETE member (leave server)
            val leaveResult = client.deleteRecord("server_members?id=eq.$peerMemberId")
            assertTrue("Member leave remote write failed: ${leaveResult.exceptionOrNull()?.message}", leaveResult.isSuccess)
        }
    }

    @Test
    fun testCampusProfileRemoteWriteAndSync() {
        runBlocking {
            // 1. Remote INSERT / UPSERT student profile
            val profilePayload = JSONObject().apply {
                put("full_name", "Integration Test Student")
                put("university_email", testEmail)
                put("university_name", "University of Nairobi")
                put("degree_program", "Electrical and Information Engineering")
                put("academic_year", "Year 3")
                put("student_id", testUserId)
                put("bio", "Testing remote profile persistence")
                put("avatar_url", "https://res.cloudinary.com/dpupj8sr0/image/upload/v1792448134/sample.jpg")
            }.toString()

            val writeResult = client.upsertRecord("campusprofile", profilePayload, onConflict = "university_email")
            assertTrue("Campus profile remote write failed: ${writeResult.exceptionOrNull()?.message}", writeResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campusprofile?university_email=eq.$testEmail")
            assertTrue(selectResult.isSuccess)
            val profileArray = JSONArray(selectResult.getOrThrow())
            assertTrue("Student profile was not found in remote Supabase", profileArray.length() >= 1)
            assertEquals("Integration Test Student", profileArray.getJSONObject(0).getString("full_name"))
            assertEquals("Electrical and Information Engineering", profileArray.getJSONObject(0).getString("degree_program"))

            // 3. Remote UPDATE bio
            val updatePayload = JSONObject().apply {
                put("bio", "Updated bio on cloud")
            }.toString()
            val updateRes = client.updateRecord("campusprofile?university_email=eq.$testEmail", updatePayload)
            assertTrue("Campus profile update failed: ${updateRes.exceptionOrNull()?.message}", updateRes.isSuccess)

            // 4. Verify updated bio
            val verifyRes = client.queryTable("campusprofile?university_email=eq.$testEmail")
            val updatedArray = JSONArray(verifyRes.getOrThrow())
            assertEquals("Updated bio on cloud", updatedArray.getJSONObject(0).getString("bio"))

            // 5. Clean up
            client.deleteRecord("campusprofile?university_email=eq.$testEmail")
        }
    }

    @Test
    fun testMarketplaceItemRemoteWrites() {
        runBlocking {
            val itemId = "item_test_${System.currentTimeMillis()}"

            // 1. Remote INSERT market item
            val itemPayload = JSONObject().apply {
                put("id", itemId)
                put("title", "Engineering Textbooks Bundle")
                put("description", "Circuit analysis 8th edition and DSP guide.")
                put("price", 1500.0)
                put("currency", "KES")
                put("category", "Books")
                put("seller_name", "Test Student")
                put("seller_id", "test_seller_${System.currentTimeMillis()}")
                put("timestamp_ms", System.currentTimeMillis())
            }.toString()

            val insertResult = client.upsertRecord("campus_market_items", itemPayload, onConflict = "id")
            assertTrue("Market item remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_market_items?id=eq.$itemId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Engineering Textbooks Bundle", array.getJSONObject(0).getString("title"))

            // 3. Clean up
            val deleteResult = client.deleteRecord("campus_market_items?id=eq.$itemId")
            assertTrue(deleteResult.isSuccess)
        }
    }

    @Test
    fun testChatMessageRemoteWrites() {
        runBlocking {
            val msgId = "msg_test_${System.currentTimeMillis()}"

            // 1. Remote INSERT chat message
            val msgPayload = JSONObject().apply {
                put("id", msgId)
                put("channel_id", "ch_direct_test_stream")
                put("sender_id", testUserId)
                put("sender_name", "Test Student")
                put("text", "Direct chat remote persistence verification")
                put("timestamp_epoch_ms", System.currentTimeMillis())
                put("is_delivered", true)
                put("is_read", false)
            }.toString()

            val insertResult = client.upsertRecord("campus_chat_messages", msgPayload, onConflict = "id")
            assertTrue("Chat message remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_chat_messages?id=eq.$msgId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Direct chat remote persistence verification", array.getJSONObject(0).getString("text"))

            // 3. Clean up
            val deleteResult = client.deleteRecord("campus_chat_messages?id=eq.$msgId")
            assertTrue(deleteResult.isSuccess)
        }
    }

    @Test
    fun testMicroGigCreationRemoteWrites() {
        runBlocking {
            val gigId = "gig_test_${System.currentTimeMillis()}"

            // 1. Remote INSERT micro gig
            val gigPayload = JSONObject().apply {
                put("id", gigId)
                put("title", "Circuit Simulation in Proteus")
                put("description", "Need help debugging an operational amplifier simulation before Friday.")
                put("payout_amount", 800.0)
                put("currency", "KES")
                put("estimated_duration", "2 hours")
                put("poster_name", "Test Student")
                put("poster_trust_score", 95)
                put("tag", "Electronics")
                put("urgency_name", "URGENT")
                put("spots_available", 1)
                put("timestamp_ms", System.currentTimeMillis())
            }.toString()

            val insertResult = client.upsertRecord("campus_micro_gigs", gigPayload, onConflict = "id")
            assertTrue("Micro gig remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_micro_gigs?id=eq.$gigId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Circuit Simulation in Proteus", array.getJSONObject(0).getString("title"))
            assertEquals(800.0, array.getJSONObject(0).getDouble("payout_amount"), 0.001)

            // 3. Remote UPDATE verification
            val updatePayload = JSONObject().apply {
                put("spots_available", 0)
            }.toString()
            val updateResult = client.updateRecord("campus_micro_gigs?id=eq.$gigId", updatePayload)
            assertTrue("Micro gig remote update failed: ${updateResult.exceptionOrNull()?.message}", updateResult.isSuccess)

            // 4. Clean up
            val deleteResult = client.deleteRecord("campus_micro_gigs?id=eq.$gigId")
            assertTrue(deleteResult.isSuccess)
        }
    }

    @Test
    fun testEventCreationRemoteWrites() {
        runBlocking {
            val eventId = "evt_test_${System.currentTimeMillis()}"

            // 1. Remote INSERT campus event
            val eventPayload = JSONObject().apply {
                put("id", eventId)
                put("title", "Campus Developers Summit")
                put("description", "Annual gathering for campus mobile and cloud engineers.")
                put("location_name", "Central Lecture Hall A")
                put("latitude", -1.2800)
                put("longitude", 36.8150)
                put("start_time", "2026-10-25T14:00:00Z")
                put("category", "TECH")
                put("attendee_count", 25)
            }.toString()

            val insertResult = client.upsertRecord("campus_events", eventPayload, onConflict = "id")
            assertTrue("Campus event remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_events?id=eq.$eventId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Campus Developers Summit", array.getJSONObject(0).getString("title"))
            assertEquals("Central Lecture Hall A", array.getJSONObject(0).getString("location_name"))

            // 3. Remote UPDATE verification
            val updatePayload = JSONObject().apply {
                put("attendee_count", 26)
            }.toString()
            val updateResult = client.updateRecord("campus_events?id=eq.$eventId", updatePayload)
            assertTrue("Campus event update failed: ${updateResult.exceptionOrNull()?.message}", updateResult.isSuccess)

            // 4. Clean up
            val deleteResult = client.deleteRecord("campus_events?id=eq.$eventId")
            assertTrue(deleteResult.isSuccess)
        }
    }

    @Test
    fun testLiveStreamCreationAndEndRemoteWrites() {
        runBlocking {
            val streamId = "stream_test_${System.currentTimeMillis()}"

            // 1. Remote INSERT live stream
            val streamPayload = JSONObject().apply {
                put("id", streamId)
                put("broadcaster_id", testUserId)
                put("broadcaster_name", "Student Developer")
                put("broadcaster_handle", "@stdev")
                put("title", "Building Kotlin Compose Multiplatform Apps")
                put("description", "Live coding session explaining state hoisting.")
                put("category", "Tech")
                put("audience", "CAMPUS")
                put("is_live", true)
                put("viewer_count", 1)
                put("peak_viewers", 1)
                put("like_count", 0)
                put("started_at_epoch_ms", System.currentTimeMillis())
            }.toString()

            val insertResult = client.upsertRecord("campus_live_streams", streamPayload, onConflict = "id")
            assertTrue("Live stream remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_live_streams?id=eq.$streamId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Building Kotlin Compose Multiplatform Apps", array.getJSONObject(0).getString("title"))
            assertTrue(array.getJSONObject(0).getBoolean("is_live"))

            // 3. Remote UPDATE stream when stream ends
            val endPayload = JSONObject().apply {
                put("is_live", false)
                put("duration_seconds", 3600)
                put("peak_viewers", 45)
                put("like_count", 120)
                put("ended_at_epoch_ms", System.currentTimeMillis())
            }.toString()
            val endResult = client.updateRecord("campus_live_streams?id=eq.$streamId", endPayload)
            assertTrue("Live stream end update failed: ${endResult.exceptionOrNull()?.message}", endResult.isSuccess)

            // Verify end state in remote Supabase
            val verifyResult = client.queryTable("campus_live_streams?id=eq.$streamId")
            val verifyObj = JSONArray(verifyResult.getOrThrow()).getJSONObject(0)
            assertEquals(false, verifyObj.getBoolean("is_live"))
            assertEquals(45, verifyObj.getInt("peak_viewers"))
            assertEquals(120, verifyObj.getInt("like_count"))

            // 4. Clean up
            val deleteResult = client.deleteRecord("campus_live_streams?id=eq.$streamId")
            assertTrue(deleteResult.isSuccess)
        }
    }

    @Test
    fun testStreamCommentRemoteWrites() {
        runBlocking {
            val commentId = "c_test_${System.currentTimeMillis()}"
            val streamId = "stream_test_cmnt_${System.currentTimeMillis()}"

            // 1. Remote INSERT comment
            val commentPayload = JSONObject().apply {
                put("id", commentId)
                put("stream_id", streamId)
                put("sender_id", testUserId)
                put("sender_name", "Student Reviewer")
                put("comment_text", "Great architecture overview!")
                put("timestamp_epoch_ms", System.currentTimeMillis())
            }.toString()

            val insertResult = client.upsertRecord("campus_stream_comments", commentPayload, onConflict = "id")
            assertTrue("Stream comment remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_stream_comments?id=eq.$commentId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Great architecture overview!", array.getJSONObject(0).getString("comment_text"))

            // 3. Clean up
            val deleteResult = client.deleteRecord("campus_stream_comments?id=eq.$commentId")
            assertTrue(deleteResult.isSuccess)
        }
    }

    @Test
    fun testFlashMeetRemoteWrites() {
        runBlocking {
            val meetId = "meet_test_${System.currentTimeMillis()}"

            // 1. Remote INSERT flash meet
            val meetPayload = JSONObject().apply {
                put("id", meetId)
                put("title", "Algorithms Discussion Group")
                put("description", "Preparing for dynamic programming exam.")
                put("category_name", "STUDY_JAM")
                put("latitude", -1.3090)
                put("longitude", 36.8120)
                put("location_name", "Science Library 2nd Floor")
                put("creator_name", "Test Student")
                put("created_at_epoch_ms", System.currentTimeMillis())
                put("expires_at_epoch_ms", System.currentTimeMillis() + 3600000L)
                put("attendee_count", 2)
                put("max_attendees", 6)
            }.toString()

            val insertResult = client.upsertRecord("campus_flash_meets", meetPayload, onConflict = "id")
            assertTrue("Flash meet remote write failed: ${insertResult.exceptionOrNull()?.message}", insertResult.isSuccess)

            // 2. Remote SELECT verification
            val selectResult = client.queryTable("campus_flash_meets?id=eq.$meetId")
            val array = JSONArray(selectResult.getOrThrow())
            assertEquals(1, array.length())
            assertEquals("Algorithms Discussion Group", array.getJSONObject(0).getString("title"))

            // 3. Clean up
            val deleteResult = client.deleteRecord("campus_flash_meets?id=eq.$meetId")
            assertTrue(deleteResult.isSuccess)
        }
    }
}
