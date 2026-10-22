package com.campus.network.core.data

import com.campus.network.core.model.CallMediaKind
import com.campus.network.core.model.CallSession
import com.campus.network.core.model.CallSessionStatus
import com.campus.network.core.model.OutgoingCallInvite
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CallSessionRow(
    val id: String = "",
    val channel_id: String = "",
    val caller_id: String = "",
    val caller_name: String = "",
    val callee_id: String = "",
    val callee_name: String = "",
    val media_kind: String = "AUDIO",
    val status: String = "RINGING",
    val created_at_epoch_ms: Long = 0L,
    val updated_at_epoch_ms: Long = 0L
)

@Serializable
data class PushTokenRow(
    val user_id: String,
    val fcm_token: String,
    val updated_at_epoch_ms: Long
)

/**
 * Supabase-backed call invite signaling.
 * Works across two devices when both can reach PostgREST.
 */
class CallSignalingRepository(
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    suspend fun createOutgoingInvite(invite: OutgoingCallInvite): Result<CallSession> =
        withContext(ioDispatcher) {
            val profileNames = try {
                profileNameResolver.resolve(listOf(invite.callerId, invite.calleeId))
            } catch (error: Exception) {
                return@withContext Result.failure(error)
            }
            val callerName = profileNames[invite.callerId.trim().lowercase()]
                ?.takeIf(String::isRealProfileDisplayName)
            val calleeName = profileNames[invite.calleeId.trim().lowercase()]
                ?.takeIf(String::isRealProfileDisplayName)
            if (callerName == null || calleeName == null) {
                return@withContext Result.failure(
                    IllegalStateException("Supabase profile names are required to start a call")
                )
            }
            val now = System.currentTimeMillis()
            val session = CallSession(
                id = "call_${UUID.randomUUID()}",
                channelId = invite.channelId,
                callerId = invite.callerId,
                callerName = callerName,
                calleeId = invite.calleeId,
                calleeName = calleeName,
                mediaKind = invite.mediaKind,
                status = CallSessionStatus.RINGING,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            runCatching {
                supabase.from("campus_call_sessions").upsert(
                    CallSessionRow(
                        id = session.id,
                        channel_id = session.channelId,
                        caller_id = session.callerId,
                        caller_name = session.callerName,
                        callee_id = session.calleeId,
                        callee_name = session.calleeName,
                        media_kind = session.mediaKind.name,
                        status = session.status.name,
                        created_at_epoch_ms = session.createdAtEpochMs,
                        updated_at_epoch_ms = session.updatedAtEpochMs
                    )
                )
                session
            }
        }

    suspend fun updateStatus(sessionId: String, status: CallSessionStatus): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val now = System.currentTimeMillis()
                supabase.from("campus_call_sessions").update(
                    mapOf(
                        "status" to status.name,
                        "updated_at_epoch_ms" to now
                    )
                ) {
                    filter {
                        eq("id", sessionId)
                    }
                }
                Unit
            }
        }

    suspend fun getSession(sessionId: String): CallSession? = withContext(ioDispatcher) {
        val rows: List<CallSessionRow> = runCatching {
            supabase.from("campus_call_sessions")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", sessionId)
                    }
                    limit(1)
                }
                .decodeList<CallSessionRow>()
        }.getOrNull() ?: return@withContext null

        val row = rows.firstOrNull() ?: return@withContext null
        resolveSessionNames(listOf(row.toDomain())).firstOrNull()
    }

    /** Incoming RINGING invites for this user (newest first). */
    suspend fun fetchIncomingRinging(calleeUserId: String): List<CallSession> =
        withContext(ioDispatcher) {
            if (calleeUserId.isBlank()) return@withContext emptyList()
            val rows: List<CallSessionRow> = runCatching {
                supabase.from("campus_call_sessions")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("callee_id", calleeUserId)
                            eq("status", "RINGING")
                        }
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                        limit(5)
                    }
                    .decodeList<CallSessionRow>()
            }.getOrDefault(emptyList())

            val sessions = rows.map { it.toDomain() }
            resolveSessionNames(sessions)
        }

    suspend fun registerPushToken(userId: String, fcmToken: String): Result<Unit> =
        withContext(ioDispatcher) {
            if (userId.isBlank() || fcmToken.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("userId/token required"))
            }
            runCatching {
                supabase.from("campus_device_push_tokens").upsert(
                    PushTokenRow(
                        user_id = userId,
                        fcm_token = fcmToken,
                        updated_at_epoch_ms = System.currentTimeMillis()
                    )
                )
                Unit
            }
        }

    private suspend fun resolveSessionNames(sessions: List<CallSession>): List<CallSession> {
        if (sessions.isEmpty()) return emptyList()
        val names = profileNameResolver.resolve(
            sessions.flatMap { listOf(it.callerId, it.calleeId) }
        )
        return sessions.map { session ->
            session.copy(
                callerName = names[session.callerId.lowercase()].orEmpty(),
                calleeName = names[session.calleeId.lowercase()].orEmpty()
            )
        }
    }
}

private fun CallSessionRow.toDomain(): CallSession = CallSession(
    id = id,
    channelId = channel_id,
    callerId = caller_id,
    callerName = caller_name,
    calleeId = callee_id,
    calleeName = callee_name,
    mediaKind = runCatching { CallMediaKind.valueOf(media_kind.uppercase()) }.getOrDefault(CallMediaKind.AUDIO),
    status = runCatching { CallSessionStatus.valueOf(status.uppercase()) }.getOrDefault(CallSessionStatus.RINGING),
    createdAtEpochMs = created_at_epoch_ms,
    updatedAtEpochMs = updated_at_epoch_ms
)
