package com.campus.network.core.data

import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

data class RemoteDmTypingUser(
    val threadId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val updatedAtEpochMs: Long
)

@Serializable
data class DmTypingRow(
    val thread_id: String,
    val user_id: String,
    val display_name: String,
    val avatar_url: String?,
    val updated_at_epoch_ms: Long
)

class DirectChatTypingRepository(
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun setTyping(
        threadId: String,
        userId: String,
        displayName: String,
        avatarUrl: String?,
        isTyping: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        if (threadId.isBlank() || userId.isBlank()) {
            return@withContext Result.success(Unit)
        }
        runCatching {
            if (!isTyping) {
                supabase.from("campus_dm_typing").delete {
                    filter {
                        eq("thread_id", threadId)
                        eq("user_id", userId)
                    }
                }
            } else {
                supabase.from("campus_dm_typing").upsert(
                    DmTypingRow(
                        thread_id = threadId,
                        user_id = userId,
                        display_name = displayName,
                        avatar_url = avatarUrl,
                        updated_at_epoch_ms = System.currentTimeMillis()
                    )
                )
            }
            Unit
        }
    }

    suspend fun fetchTypingForThread(
        threadId: String,
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L
    ): Result<List<RemoteDmTypingUser>> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<DmTypingRow> = supabase.from("campus_dm_typing")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("thread_id", threadId)
                        gt("updated_at_epoch_ms", fresherThanEpochMs)
                    }
                    order("updated_at_epoch_ms", order = Order.DESCENDING)
                    limit(10)
                }
                .decodeList()
            rows.map { row ->
                RemoteDmTypingUser(
                    threadId = row.thread_id,
                    userId = row.user_id,
                    displayName = row.display_name,
                    avatarUrl = row.avatar_url,
                    updatedAtEpochMs = row.updated_at_epoch_ms
                )
            }
        }
    }

    suspend fun fetchRecentTyping(
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L,
        limit: Int = 40
    ): Result<List<RemoteDmTypingUser>> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<DmTypingRow> = supabase.from("campus_dm_typing")
                .select(columns = Columns.ALL) {
                    filter {
                        gt("updated_at_epoch_ms", fresherThanEpochMs)
                    }
                    order("updated_at_epoch_ms", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList()
            rows.map { row ->
                RemoteDmTypingUser(
                    threadId = row.thread_id,
                    userId = row.user_id,
                    displayName = row.display_name,
                    avatarUrl = row.avatar_url,
                    updatedAtEpochMs = row.updated_at_epoch_ms
                )
            }
        }
    }
}
