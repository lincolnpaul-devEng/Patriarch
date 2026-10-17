package com.campus.network.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance Supabase Client Gateway using Ktor engine.
 * Credentials injected securely from UniHub project configuration.
 */
class SupabaseClientProvider(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val client = HttpClient(OkHttp)

    val baseUrl: String
        get() = supabaseUrl

    /**
     * Executes an authenticated PostgREST query on Dispatchers.IO.
     */
    suspend fun queryTable(
        tableName: String,
        selectQuery: String = "*",
        userJwt: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/$tableName?select=$selectQuery") {
                header("apikey", anonKey)
                header("Authorization", "Bearer ${userJwt ?: anonKey}")
                header("Accept", "application/json")
            }
            response.bodyAsText()
        }
    }

    /**
     * Inserts records into a Supabase table.
     */
    suspend fun insertRecord(
        tableName: String,
        jsonPayload: String,
        userJwt: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val response: HttpResponse = client.post("$supabaseUrl/rest/v1/$tableName") {
                header("apikey", anonKey)
                header("Authorization", "Bearer ${userJwt ?: anonKey}")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            response.bodyAsText()
        }
    }

    /**
     * Calls the Supabase Edge Function to fetch a signed Stream JWT token.
     */
    suspend fun fetchStreamToken(
        userId: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val payload = org.json.JSONObject().apply {
                put("userId", userId)
                if (!displayName.isNullOrBlank()) put("name", displayName)
                if (!avatarUrl.isNullOrBlank()) put("image", avatarUrl)
            }
            val response: HttpResponse = client.post("$supabaseUrl/functions/v1/stream-token") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            val json = org.json.JSONObject(body)
            if (json.has("token")) {
                json.getString("token")
            } else {
                throw IllegalStateException("Edge function error: ${json.optString("error", body)}")
            }
        }
    }
}
