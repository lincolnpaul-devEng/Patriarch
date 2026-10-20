package com.campus.network.core.data

import com.campus.network.core.model.CallMediaKind
import com.campus.network.core.model.CallSession
import com.campus.network.core.model.CallSessionStatus
import com.campus.network.core.model.OutgoingCallInvite
import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID

/**
 * Supabase-backed call invite signaling.
 * Works across two devices when both can reach PostgREST (poll or future realtime).
 */
class CallSignalingRepository(
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun createOutgoingInvite(invite: OutgoingCallInvite): Result<CallSession> =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val session = CallSession(
                id = "call_${UUID.randomUUID()}",
                channelId = invite.channelId,
                callerId = invite.callerId,
                callerName = invite.callerName,
                calleeId = invite.calleeId,
                calleeName = invite.calleeName,
                mediaKind = invite.mediaKind,
                status = CallSessionStatus.RINGING,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            val payload = sessionToJson(session).toString()
            val result = supabaseClient.upsertRecord(
                "campus_call_sessions",
                payload,
                onConflict = "id"
            )
            if (result.isSuccess) Result.success(session)
            else Result.failure(result.exceptionOrNull() ?: Exception("Failed to create call session"))
        }

    suspend fun updateStatus(sessionId: String, status: CallSessionStatus): Result<Unit> =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val payload = JSONObject().apply {
                put("status", status.name)
                put("updated_at_epoch_ms", now)
            }.toString()
            val encoded = URLEncoder.encode(sessionId, Charsets.UTF_8.name())
            val result = supabaseClient.updateRecord(
                "campus_call_sessions?id=eq.$encoded",
                payload
            )
            if (result.isSuccess) Result.success(Unit)
            else Result.failure(result.exceptionOrNull() ?: Exception("Failed to update call status"))
        }

    suspend fun getSession(sessionId: String): CallSession? = withContext(ioDispatcher) {
        val encoded = URLEncoder.encode(sessionId, Charsets.UTF_8.name())
        val raw = supabaseClient.queryTable("campus_call_sessions?id=eq.$encoded&limit=1")
            .getOrNull() ?: return@withContext null
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return@withContext null
        if (array.length() == 0) return@withContext null
        parseSession(array.getJSONObject(0))
    }

    /** Incoming RINGING invites for this user (newest first). */
    suspend fun fetchIncomingRinging(calleeUserId: String): List<CallSession> =
        withContext(ioDispatcher) {
            if (calleeUserId.isBlank()) return@withContext emptyList()
            val encoded = URLEncoder.encode(calleeUserId, Charsets.UTF_8.name())
            val raw = supabaseClient.queryTable(
                "campus_call_sessions?callee_id=eq.$encoded&status=eq.RINGING&order=created_at_epoch_ms.desc&limit=5"
            ).getOrNull() ?: return@withContext emptyList()
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return@withContext emptyList()
            buildList {
                for (i in 0 until array.length()) {
                    parseSession(array.getJSONObject(i))?.let { add(it) }
                }
            }
        }

    suspend fun registerPushToken(userId: String, fcmToken: String): Result<Unit> =
        withContext(ioDispatcher) {
            if (userId.isBlank() || fcmToken.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("userId/token required"))
            }
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("fcm_token", fcmToken)
                put("updated_at_epoch_ms", System.currentTimeMillis())
            }.toString()
            val result = supabaseClient.upsertRecord(
                "campus_device_push_tokens",
                payload,
                onConflict = "user_id"
            )
            if (result.isSuccess) Result.success(Unit)
            else Result.failure(result.exceptionOrNull() ?: Exception("Failed to register push token"))
        }

    private fun sessionToJson(session: CallSession): JSONObject = JSONObject().apply {
        put("id", session.id)
        put("channel_id", session.channelId)
        put("caller_id", session.callerId)
        put("caller_name", session.callerName)
        put("callee_id", session.calleeId)
        put("callee_name", session.calleeName)
        put("media_kind", session.mediaKind.name)
        put("status", session.status.name)
        put("created_at_epoch_ms", session.createdAtEpochMs)
        put("updated_at_epoch_ms", session.updatedAtEpochMs)
    }

    private fun parseSession(obj: JSONObject): CallSession? {
        val id = obj.optString("id").ifBlank { return null }
        val media = runCatching {
            CallMediaKind.valueOf(obj.optString("media_kind", "AUDIO").uppercase())
        }.getOrDefault(CallMediaKind.AUDIO)
        val status = runCatching {
            CallSessionStatus.valueOf(obj.optString("status", "RINGING").uppercase())
        }.getOrDefault(CallSessionStatus.RINGING)
        return CallSession(
            id = id,
            channelId = obj.optString("channel_id"),
            callerId = obj.optString("caller_id"),
            callerName = obj.optString("caller_name"),
            calleeId = obj.optString("callee_id"),
            calleeName = obj.optString("callee_name"),
            mediaKind = media,
            status = status,
            createdAtEpochMs = obj.optLong("created_at_epoch_ms", System.currentTimeMillis()),
            updatedAtEpochMs = obj.optLong("updated_at_epoch_ms", System.currentTimeMillis())
        )
    }
}
