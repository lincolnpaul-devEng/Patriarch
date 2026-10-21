package com.campus.network.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val userJwtProvider: suspend () -> String = {
        SupabaseSessionStore.validAccessToken(supabaseUrl, anonKey)
    }
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
            val jwt = requireUserJwt(userJwt)
            val querySeparator = if ('?' in tableName) '&' else '?'
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/$tableName${querySeparator}select=$selectQuery") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
                header("Accept", "application/json")
            }
            response.bodyOrThrow()
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
            val jwt = requireUserJwt(userJwt)
            val response: HttpResponse = client.post("$supabaseUrl/rest/v1/$tableName") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            response.bodyOrThrow()
        }
    }

    /**
     * Updates records in a Supabase table using PATCH with filter query.
     */
    suspend fun updateRecord(
        tableAndQuery: String,
        jsonPayload: String,
        userJwt: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val jwt = requireUserJwt(userJwt)
            val response: HttpResponse = client.patch("$supabaseUrl/rest/v1/$tableAndQuery") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            response.bodyOrThrow()
        }
    }

    /**
     * Upserts records into a Supabase table with configurable conflict resolution.
     */
    suspend fun upsertRecord(
        tableName: String,
        jsonPayload: String,
        onConflict: String? = null,
        ignoreDuplicates: Boolean = false,
        userJwt: String? = null,
        returnMinimal: Boolean = false
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val jwt = requireUserJwt(userJwt)
            val endpoint = if (onConflict.isNullOrBlank()) {
                "$supabaseUrl/rest/v1/$tableName"
            } else {
                "$supabaseUrl/rest/v1/$tableName?on_conflict=$onConflict"
            }
            val resolution = if (ignoreDuplicates) "resolution=ignore-duplicates" else "resolution=merge-duplicates"
            val response: HttpResponse = client.post(endpoint) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
                header("Prefer", "$resolution,return=${if (returnMinimal) "minimal" else "representation"}")
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            response.bodyOrThrow()
        }
    }

    /**
     * Calls a PostgreSQL RPC stored procedure in Supabase.
     */
    suspend fun callRpc(
        functionName: String,
        jsonPayload: String = "{}",
        userJwt: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val jwt = requireUserJwt(userJwt)
            val response: HttpResponse = client.post("$supabaseUrl/rest/v1/rpc/$functionName") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
                header("Content-Type", "application/json")
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            response.bodyOrThrow()
        }
    }

    /**
     * Deletes records from a Supabase table using DELETE with filter query.
     */
    suspend fun deleteRecord(
        tableAndQuery: String,
        userJwt: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val jwt = requireUserJwt(userJwt)
            val response: HttpResponse = client.delete("$supabaseUrl/rest/v1/$tableAndQuery") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
            }
            response.bodyOrThrow()
        }
    }

    private suspend fun HttpResponse.bodyOrThrow(): String {
        val body = bodyAsText()
        check(status.isSuccess()) {
            "Supabase request failed (HTTP ${status.value}): ${body.take(500)}"
        }
        return body
    }

    /** Calls a JWT-protected Edge Function; no provider API secret is held on-device. */
    suspend fun invokeFunction(name: String, jsonPayload: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val jwt = requireUserJwt(null)
            val response = client.post("$supabaseUrl/functions/v1/$name") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            response.bodyOrThrow()
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
            val jwt = requireUserJwt(null)
            val payload = org.json.JSONObject().apply {
                put("userId", userId)
                if (!displayName.isNullOrBlank()) put("name", displayName)
                if (!avatarUrl.isNullOrBlank()) put("image", avatarUrl)
            }
            val response: HttpResponse = client.post("$supabaseUrl/functions/v1/stream-token") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $jwt")
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

    /** Never silently downgrade an authenticated operation to the public anon role. */
    private suspend fun requireUserJwt(explicitJwt: String?): String =
        (explicitJwt ?: userJwtProvider())
            .takeIf { it.isNotBlank() && it != anonKey }
            ?: throw SecurityException("Sign in to access campus data")
}
