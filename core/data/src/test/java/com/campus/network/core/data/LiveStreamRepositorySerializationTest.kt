package com.campus.network.core.data

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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStreamRepositorySerializationTest {

    @Serializable
    private data class LikesRpcTestParams(
        val p_stream_id: String,
        val p_delta: Int = 1
    )

    @Serializable
    private data class ViewersRpcTestParams(
        val p_stream_id: String,
        val p_delta: Int
    )

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
    fun `LiveStreamRow and sparse fields deserialize cleanly from Supabase JSON`() = runBlocking {
        val json = """
            [
              {
                "id": "stream_123",
                "broadcaster_id": "STD-111",
                "broadcaster_name": "Imani Okafor",
                "broadcaster_handle": "imani",
                "title": "Exam Prep Q&A",
                "is_live": true,
                "stream_playback_url": "stream://default/stream_123",
                "viewer_count": 14,
                "peak_viewers": 19,
                "like_count": 42,
                "started_at_epoch_ms": 1730000000000
              },
              {
                "id": "stream_sparse",
                "broadcaster_id": "STD-222",
                "broadcaster_name": "Alex Otieno",
                "title": "Quick Room Check",
                "is_live": false
              }
            ]
        """.trimIndent()

        val client = buildMockClient(json)
        val rows: List<LiveStreamRow> = client
            .from("campus_live_streams")
            .select(columns = Columns.ALL)
            .decodeList()

        assertEquals(2, rows.size)
        assertEquals("stream_123", rows[0].id)
        assertEquals("stream://default/stream_123", rows[0].stream_playback_url)
        assertEquals(14, rows[0].viewer_count)
        assertEquals(42, rows[0].like_count)
        assertTrue(rows[0].is_live)

        assertEquals("stream_sparse", rows[1].id)
        assertFalse(rows[1].is_live)
        assertNull(rows[1].stream_playback_url)
        assertEquals(0, rows[1].like_count)
    }

    @Test
    fun `StreamCommentRow and LiveRoomPresenceRow deserialize with full and sparse fields`() = runBlocking {
        val commentsJson = """
            [
              {
                "id": "comment-1",
                "stream_id": "stream_123",
                "sender_id": "STD-111",
                "sender_name": "Imani Okafor",
                "comment_text": "Audio sounds super clear!",
                "timestamp_epoch_ms": 1730000050000
              }
            ]
        """.trimIndent()
        val commentRows: List<StreamCommentRow> = buildMockClient(commentsJson)
            .from("campus_stream_comments")
            .select(columns = Columns.ALL)
            .decodeList()

        assertEquals(1, commentRows.size)
        assertEquals("Audio sounds super clear!", commentRows[0].comment_text)
        assertNull(commentRows[0].sender_avatar_url)

        val presenceJson = """
            [
              {
                "user_id": "STD-999",
                "display_name": "Tessa Wanjiku",
                "is_hand_raised": true,
                "is_muted": false,
                "last_seen_epoch_ms": 1730000090000
              }
            ]
        """.trimIndent()
        val presenceRows: List<LiveRoomPresenceRow> = buildMockClient(presenceJson)
            .from("campus_live_room_presence")
            .select(columns = Columns.ALL)
            .decodeList()

        assertEquals(1, presenceRows.size)
        assertEquals("STD-999", presenceRows[0].user_id)
        assertEquals("Tessa Wanjiku", presenceRows[0].display_name)
        assertTrue(presenceRows[0].is_hand_raised)
        assertFalse(presenceRows[0].is_muted)
        assertTrue(presenceRows[0].is_camera_on)
    }

    @Test
    fun `increment_live_stream_likes and adjust_live_stream_viewers RPCs decode scalar integers`() = runBlocking {
        val likeCount = buildMockClient("43")
            .postgrest
            .rpc("increment_live_stream_likes", LikesRpcTestParams(p_stream_id = "stream_123", p_delta = 1))
            .decodeAs<Int>()
        assertEquals(43, likeCount)

        val viewerCount = buildMockClient("15")
            .postgrest
            .rpc("adjust_live_stream_viewers", ViewersRpcTestParams(p_stream_id = "stream_123", p_delta = 1))
            .decodeAs<Int>()
        assertEquals(15, viewerCount)
    }
}
