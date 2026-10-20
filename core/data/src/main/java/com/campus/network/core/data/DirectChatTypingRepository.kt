package com.campus.network.core.data

import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class RemoteDmTypingUser(
    val threadId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val updatedAtEpochMs: Long
)

/**
 * Ephemeral typing presence for direct-message threads via Supabase REST.
 * Clients treat rows older than ~4.5s as stale.
 */
class DirectChatTypingRepository(
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
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
        val encodedThread = URLEncoder.encode(threadId, Charsets.UTF_8.name())
        val encodedUser = URLEncoder.encode(userId, Charsets.UTF_8.name())
        if (!isTyping) {
            return@withContext supabaseClient.deleteRecord(
                "campus_dm_typing?thread_id=eq.$encodedThread&user_id=eq.$encodedUser"
            ).map { }
        }
        val payload = JSONObject().apply {
            put("thread_id", threadId)
            put("user_id", userId)
            put("display_name", displayName.ifBlank { "Student" })
            if (!avatarUrl.isNullOrBlank()) put("avatar_url", avatarUrl)
            put("updated_at_epoch_ms", System.currentTimeMillis())
        }.toString()
        supabaseClient.upsertRecord(
            "campus_dm_typing",
            payload,
            onConflict = "thread_id,user_id"
        ).map { }
    }

    suspend fun fetchTypingForThread(
        threadId: String,
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L
    ): Result<List<RemoteDmTypingUser>> = withContext(ioDispatcher) {
        val encoded = URLEncoder.encode(threadId, Charsets.UTF_8.name())
        supabaseClient.queryTable(
            "campus_dm_typing?thread_id=eq.$encoded" +
                "&updated_at_epoch_ms=gt.$fresherThanEpochMs&order=updated_at_epoch_ms.desc&limit=10"
        ).mapCatching { raw -> parseTyping(raw) }
    }

    suspend fun fetchRecentTyping(
        fresherThanEpochMs: Long = System.currentTimeMillis() - 4_500L,
        limit: Int = 40
    ): Result<List<RemoteDmTypingUser>> = withContext(ioDispatcher) {
        supabaseClient.queryTable(
            "campus_dm_typing?updated_at_epoch_ms=gt.$fresherThanEpochMs" +
                "&order=updated_at_epoch_ms.desc&limit=$limit"
        ).mapCatching { raw -> parseTyping(raw) }
    }

    private fun parseTyping(raw: String): List<RemoteDmTypingUser> {
        val array = JSONArray(raw)
        val out = mutableListOf<RemoteDmTypingUser>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            out.add(
                RemoteDmTypingUser(
                    threadId = obj.optString("thread_id", ""),
                    userId = obj.optString("user_id", ""),
                    displayName = obj.optString("display_name", "Student"),
                    avatarUrl = obj.optString("avatar_url").ifBlank { null },
                    updatedAtEpochMs = obj.optLong("updated_at_epoch_ms", 0L)
                )
            )
        }
        return out
    }
}
