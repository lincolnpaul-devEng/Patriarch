package com.campus.network.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Supabase Kotlin SDK client provider.
 *
 * Provides the official [SupabaseClient] configured with [Postgrest].
 * Credentials come from BuildConfig (injected from secrets.properties).
 *
 * NOTE: The Supabase Kotlin SDK version (2.0.0) is pinned to be compatible with
 * the project's Kotlin 1.9.23.
 */
class SupabaseSdkClientProvider(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** The initialized Supabase client with Postgrest installed. */
    val supabase: SupabaseClient = createSupabaseClient(supabaseUrl, supabaseKey) {
        install(Postgrest)
    }

    /** Convenience access to the underlying client. */
    val client: SupabaseClient = supabase

    /**
     * Calls the Supabase Edge Function to fetch a signed Stream JWT token.
     */
    suspend fun fetchStreamToken(
        userId: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val jwt = SupabaseSessionStore.validAccessToken(supabaseUrl = supabaseUrl, anonKey = supabaseKey)
            val url = URL("${supabaseUrl.trimEnd('/')}/functions/v1/stream-token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", supabaseKey)
            conn.setRequestProperty("Authorization", "Bearer $jwt")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("userId", userId)
                if (!displayName.isNullOrBlank()) put("name", displayName)
                if (!avatarUrl.isNullOrBlank()) put("image", avatarUrl)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                throw IllegalStateException("Stream token edge function failed ($statusCode): $responseText")
            }

            val json = JSONObject(responseText)
            if (json.has("token")) {
                json.getString("token")
            } else {
                throw IllegalStateException("Edge function error: ${json.optString("error", responseText)}")
            }
        }
    }

    companion object {
        private val instance by lazy { SupabaseSdkClientProvider() }

        /** Shared global singleton SupabaseClient. */
        val defaultClient: SupabaseClient
            get() = instance.supabase

        /** Shared helper for Edge Function stream tokens. */
        suspend fun fetchStreamToken(
            userId: String,
            displayName: String? = null,
            avatarUrl: String? = null
        ): Result<String> = instance.fetchStreamToken(userId, displayName, avatarUrl)
    }
}
