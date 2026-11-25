package com.campus.network.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseRealtimeClientTest {

    @Test
    fun buildSupabaseRealtimeWebSocketUrl_convertsSchemesAndTrimsSlashes() {
        assertEquals(
            "wss://xyz.supabase.co/realtime/v1/websocket?apikey=anon_key_1&vsn=1.0.0",
            buildSupabaseRealtimeWebSocketUrl("https://xyz.supabase.co/", "anon_key_1")
        )
        assertEquals(
            "wss://xyz.supabase.co/realtime/v1/websocket?apikey=anon_key_1&vsn=1.0.0",
            buildSupabaseRealtimeWebSocketUrl("Https://xyz.supabase.co/", "anon_key_1")
        )
        assertEquals(
            "ws://localhost:54321/realtime/v1/websocket?apikey=local_key&vsn=1.0.0",
            buildSupabaseRealtimeWebSocketUrl("http://localhost:54321///", "local_key")
        )
        assertEquals(
            "ws://localhost:54321/realtime/v1/websocket?apikey=local_key&vsn=1.0.0",
            buildSupabaseRealtimeWebSocketUrl("HTTP://localhost:54321/", "local_key")
        )
        assertEquals("", buildSupabaseRealtimeWebSocketUrl("   ", "anon_key_1"))
    }

    @Test
    fun buildPostgresChangesJoinFrame_containsTopicTableAndToken() {
        val frame = buildPostgresChangesJoinFrame(
            table = "campus_live_streams",
            accessToken = "jwt.token.value",
            ref = "42"
        )

        assertTrue(frame.contains("\"topic\":\"realtime:public:campus_live_streams\""))
        assertTrue(frame.contains("\"event\":\"phx_join\""))
        assertTrue(frame.contains("\"table\":\"campus_live_streams\""))
        assertTrue(frame.contains("\"access_token\":\"jwt.token.value\""))
        assertTrue(frame.contains("\"ref\":\"42\""))
    }

    @Test
    fun parseSupabaseRealtimeFrame_parsesInsertUpdateDeleteAndIgnoresControlFrames() {
        val insertFrame = """
            {
              "topic": "realtime:public:campus_live_streams",
              "event": "postgres_changes",
              "payload": {
                "data": {
                  "type": "INSERT",
                  "table": "campus_live_streams",
                  "record": {"id": "stream_1", "title": "Study Session", "is_live": true}
                }
              },
              "ref": null
            }
        """.trimIndent()
        val parsedInsert = parseSupabaseRealtimeFrame(insertFrame)
        assertNotNull(parsedInsert)
        assertEquals("campus_live_streams", parsedInsert?.table)
        assertEquals(RealtimeEventType.INSERT, parsedInsert?.eventType)
        assertTrue(parsedInsert?.recordJson?.contains("stream_1") == true)
        assertNull(parsedInsert?.oldRecordJson)

        val updateCommentsFrame = """
            {
              "topic": "realtime:public:campus_stream_comments",
              "event": "postgres_changes",
              "payload": {
                "data": {
                  "type": "UPDATE",
                  "table": "campus_stream_comments",
                  "record": {"id": "c_1", "comment_text": "Hello live"},
                  "old_record": {"id": "c_1", "comment_text": "Hello"}
                }
              }
            }
        """.trimIndent()
        val parsedUpdate = parseSupabaseRealtimeFrame(updateCommentsFrame)
        assertNotNull(parsedUpdate)
        assertEquals("campus_stream_comments", parsedUpdate?.table)
        assertEquals(RealtimeEventType.UPDATE, parsedUpdate?.eventType)
        assertTrue(parsedUpdate?.recordJson?.contains("Hello live") == true)
        assertTrue(parsedUpdate?.oldRecordJson?.contains("Hello") == true)

        val deletePresenceFrame = """
            {
              "topic": "realtime:public:campus_live_room_presence",
              "event": "postgres_changes",
              "payload": {
                "data": {
                  "type": "DELETE",
                  "table": "campus_live_room_presence",
                  "old_record": {"user_id": "STD-123"}
                }
              }
            }
        """.trimIndent()
        val parsedDelete = parseSupabaseRealtimeFrame(deletePresenceFrame)
        assertNotNull(parsedDelete)
        assertEquals("campus_live_room_presence", parsedDelete?.table)
        assertEquals(RealtimeEventType.DELETE, parsedDelete?.eventType)
        assertNull(parsedDelete?.recordJson)
        assertTrue(parsedDelete?.oldRecordJson?.contains("STD-123") == true)

        val replyFrame = """{"topic":"realtime:public:campus_live_streams","event":"phx_reply","payload":{"status":"ok"},"ref":"1"}"""
        assertNull(parseSupabaseRealtimeFrame(replyFrame))

        val heartbeatFrame = """{"topic":"phoenix","event":"phx_reply","payload":{"status":"ok"},"ref":"2"}"""
        assertNull(parseSupabaseRealtimeFrame(heartbeatFrame))
    }
}
