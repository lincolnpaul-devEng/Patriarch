package com.campus.network.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemoryCodeVerifierCache
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Supabase Kotlin SDK client provider.
 *
 * Provides the official [SupabaseClient] configured with [Auth] and [Postgrest].
 * Credentials come from BuildConfig (injected from secrets.properties).
 *
 * Session handling is unified behind [SupabaseSessionStore]:
 *  - The SDK Auth plugin is installed with a [SessionManager][Auth.Config.sessionManager]
 *    backed by the encrypted store, so SDK auto-refresh persists rotated tokens.
 *  - Custom sign-in flows (Google ID token / OTP / password) persist the session via
 *    [SupabaseSessionStore.save] and then call [syncSession] to bring the SDK live.
 *  - On process start the last stored session is imported asynchronously (never blocking
 *    the main thread), with SDK auto-refresh keeping the JWT valid for PostgREST calls.
 *
 * NOTE: The Supabase Kotlin SDK version (2.0.0) is pinned to be compatible with
 * the project's Kotlin 1.9.23.
 */
class SupabaseSdkClientProvider(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** The initialized Supabase client with Auth and Postgrest installed. */
    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    val supabase: SupabaseClient = createSupabaseClient(supabaseUrl, supabaseKey) {
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        })
        // The 2.0.0 Postgrest plugin does not throw on non-2xx write responses; make
        // /rest/v1 failures loud so callers (and delta sync) can see real rejections.
        httpConfig {
            install(
                io.ktor.client.plugins.api.createClientPlugin("RestV1ThrowOnHttpError") {
                    onResponse { response ->
                        val isRestEndpoint = response.request.url.toString().contains("/rest/v1/")
                        if (isRestEndpoint && !response.status.isSuccess()) {
                            val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
                            throw PostgrestHttpException(response.status.value, bodyText)
                        }
                    }
                }
            )
        }
        install(Auth) {
            sessionManager = SupabaseSessionStore.sdkSessionManager
            codeVerifierCache = MemoryCodeVerifierCache()
            // Keep default alwaysAutoRefresh/autoSaveToStorage so rotated tokens land back
            // in the encrypted store automatically. Lifecycle callbacks stay off because the
            // app manages foreground refresh through its own connectivity/sync workers.
            enableLifecycleCallbacks = false
        }
        install(Postgrest)
    }.also { client ->
        // Import the persisted session on a worker thread (never blocks first access).
        CoroutineScope(ioDispatcher + SupervisorJob()).launch {
            val session = runCatching { SupabaseSessionStore.loadSdkSession() }.getOrNull() ?: return@launch
            val canAutoRefresh = session.refreshToken.isNotBlank()
            runCatching {
                client.auth.importSession(session, autoRefresh = canAutoRefresh)
            }.onFailure {
                // Refreshing an expired session can fail while offline — fall back to a
                // token-only import so at least the same JWT as before is attached.
                if (canAutoRefresh) {
                    runCatching { client.auth.importSession(session, autoRefresh = false) }
                }
            }
        }
    }

    /** Convenience access to the underlying client. */
    val client: SupabaseClient = supabase

    private val edgeFunctionHttpClient = HttpClient(OkHttp)

    /**
     * Brings the SDK session in lock-step with the encrypted store after a custom
     * sign-in or manual refresh. Token-only sessions are imported without auto refresh.
     */
    suspend fun syncSession(accessToken: String, refreshToken: String?) = withContext(ioDispatcher) {
        if (accessToken.isBlank() || accessToken.split('.').size != 3) return@withContext
        runCatching {
            supabase.auth.importSession(
                SupabaseSessionStore.loadSdkSession()
                    ?: buildSessionFrom(accessToken, refreshToken),
                autoRefresh = !refreshToken.isNullOrBlank()
            )
        }
    }

    /** Drops the in-memory SDK session (no network). The store owns persistence. */
    suspend fun clearSession() = withContext(ioDispatcher) {
        runCatching { supabase.auth.clearSession() }
    }

    /**
     * Refreshes the current session through the SDK (single-flight internally) and
     * persists the rotated tokens via the installed [SessionManager].
     * @return the new access token, or null when refresh failed.
     */
    suspend fun refreshSession(refreshToken: String?): String? = withContext(ioDispatcher) {
        if (refreshToken.isNullOrBlank()) return@withContext null
        runCatching {
            supabase.auth.refreshSession(refreshToken).accessToken
        }.getOrNull()
    }

    private fun buildSessionFrom(accessToken: String, refreshToken: String?) =
        io.github.jan.supabase.gotrue.user.UserSession(
            accessToken = accessToken,
            refreshToken = refreshToken.orEmpty(),
            expiresIn = 0L,
            tokenType = "bearer",
            user = null
        )

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
            val payload = JSONObject().apply {
                put("userId", userId)
                if (!displayName.isNullOrBlank()) put("name", displayName)
                if (!avatarUrl.isNullOrBlank()) put("image", avatarUrl)
            }
            val response = edgeFunctionHttpClient.post("${supabaseUrl.trimEnd('/')}/functions/v1/stream-token") {
                header("apikey", supabaseKey)
                header("Authorization", "Bearer $jwt")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val responseText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Stream token edge function failed (${response.status.value}): $responseText")
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

        /** Brings the SDK session in lock-step with the encrypted store. */
        suspend fun syncSession(accessToken: String, refreshToken: String?) =
            instance.syncSession(accessToken, refreshToken)

        /** Drops the in-memory SDK session without network calls. */
        suspend fun clearSession() = instance.clearSession()

        /** SDK-driven session refresh; returns the rotated access token or null. */
        suspend fun refreshSession(refreshToken: String?): String? =
            instance.refreshSession(refreshToken)

        /** Shared helper for Edge Function stream tokens. */
        suspend fun fetchStreamToken(
            userId: String,
            displayName: String? = null,
            avatarUrl: String? = null
        ): Result<String> = instance.fetchStreamToken(userId, displayName, avatarUrl)
    }
}
