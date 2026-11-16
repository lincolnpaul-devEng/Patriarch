package com.campus.network.core.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** The only source of Supabase user credentials for PostgREST and Edge Functions. */
object SupabaseSessionStore {
    private const val LEGACY_PREFS = "socialpulse-auth-token"
    private const val SECURE_PREFS = "campus-auth-session"
    private const val SESSION_KEY = "session"

    @Volatile private var prefs: SharedPreferences? = null
    private val refreshMutex = Mutex()
    private val client = HttpClient(OkHttp)

    fun initialize(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val app = context.applicationContext
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encrypted = EncryptedSharedPreferences.create(
                app, SECURE_PREFS, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            // Migrate existing valid sessions before removing plaintext tokens.
            val legacy = app.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            if (!encrypted.contains(SESSION_KEY)) {
                val token = legacy.getString("access_token", null)
                if (!token.isNullOrBlank() && token.split('.').size == 3) {
                    encrypted.edit().putString(SESSION_KEY, JSONObject().apply {
                        put("user_id", legacy.getString("user_id", ""))
                        put("email", legacy.getString("email", ""))
                        put("full_name", legacy.getString("full_name", ""))
                        put("avatar_url", legacy.getString("avatar_url", ""))
                        put("access_token", token)
                        put("refresh_token", legacy.getString("refresh_token", ""))
                    }.toString()).commit()
                }
            }
            legacy.edit().remove("raw_session").remove("user_id").remove("email")
                .remove("full_name").remove("avatar_url").remove("access_token")
                .remove("refresh_token").commit()
            prefs = encrypted
        }
    }

    /** For unit test injection of in-memory preferences without Android crypto runtime. */
    fun initializeForTesting(testPrefs: SharedPreferences) {
        prefs = testPrefs
    }

    private fun decodeJwtPayload(token: String): JSONObject? = runCatching {
        val parts = token.split('.')
        if (parts.size != 3) return null
        val payload = parts[1].trim().replace('-', '+').replace('_', '/')
        val padded = when (payload.length % 4) {
            2 -> "$payload=="
            3 -> "$payload="
            else -> payload
        }
        val bytes = java.util.Base64.getDecoder().decode(padded)
        JSONObject(String(bytes, Charsets.UTF_8))
    }.getOrNull()

    fun session(): JSONObject? = prefs?.getString(SESSION_KEY, null)?.let { runCatching { JSONObject(it) }.getOrNull() }

    fun accessToken(): String? = session()?.optString("access_token")?.takeIf { it.split('.').size == 3 }

    suspend fun validAccessToken(supabaseUrl: String, anonKey: String): String = refreshMutex.withLock {
        val current = accessToken() ?: throw SecurityException("Sign in to access campus data")
        val expiresAt = decodeJwtPayload(current)?.optLong("exp", 0L) ?: 0L
        // Another writer (SDK auto-refresh job) may have rotated the token while we queued
        // on the mutex — re-check before doing anything destructive.
        if (expiresAt > System.currentTimeMillis() / 1000 + 60) return@withLock current

        // Prefer the SDK refresh path (single-flight + persists rotated tokens itself).
        val sdkRefreshed = runCatching {
            val refreshToken = session()?.optString("refresh_token")?.takeIf(String::isNotBlank)
                ?: throw SecurityException("Session expired. Sign in again")
            SupabaseSdkClientProvider.refreshSession(refreshToken)
        }.getOrNull()
        if (sdkRefreshed != null && sdkRefreshed.split('.').size == 3) {
            // saveSdkSession already persisted the rotated tokens via the SessionManager.
            return@withLock sdkRefreshed
        }

        // Manual refresh fallback (edge: SDK not initialized yet).
        val refreshToken = session()?.optString("refresh_token")?.takeIf(String::isNotBlank)
            ?: throw SecurityException("Session expired. Sign in again")
        val refreshResult = runCatching {
            val response = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
                header("apikey", anonKey)
                contentType(ContentType.Application.Json)
                setBody(JSONObject().put("refresh_token", refreshToken).toString())
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("refresh rejected (${response.status.value})")
            }
            JSONObject(response.bodyAsText())
        }
        val body = refreshResult.getOrNull()
        val nextToken = body?.optString("access_token")?.takeIf { it.split('.').size == 3 }
        if (nextToken == null) {
            // Only clear when the stored token is unchanged (nobody refreshed meanwhile):
            // a concurrent SDK refresh invalidates the old refresh token we tried — the
            // user is not signed out just because we raced.
            if (accessToken() == current) clear()
            throw SecurityException("Session expired. Sign in again")
        }
        val prior = session() ?: throw SecurityException("Session was cleared")
        prior.put("access_token", nextToken)
        prior.put("refresh_token", body.optString("refresh_token", refreshToken))
        prefs?.edit()?.putString(SESSION_KEY, prior.toString())?.commit()
        // Keep the SDK session in lock-step so PostgREST/Edge calls never send a stale JWT.
        SupabaseSdkClientProvider.syncSession(
            accessToken = nextToken,
            refreshToken = prior.optString("refresh_token").takeIf(String::isNotBlank)
        )
        nextToken
    }

    fun save(user: AuthenticatedSessionUser) {
        checkNotNull(prefs) { "SupabaseSessionStore must be initialized by the Application" }
            .edit().putString(SESSION_KEY, JSONObject().apply {
                put("user_id", user.userId)
                put("email", user.email)
                put("full_name", user.fullName)
                put("avatar_url", user.avatarUrl)
                put("access_token", user.accessToken)
                put("refresh_token", user.refreshToken)
            }.toString()).apply()
    }

    fun clear() {
        prefs?.edit()?.remove(SESSION_KEY)?.commit()
        // Drop the in-memory SDK session on a worker thread (no network calls).
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            SupabaseSdkClientProvider.clearSession()
        }
    }

    /**
     * Builds a Supabase SDK [UserSession] from the encrypted store so the official SDK can
     * auto-refresh it. Token-only legacy sessions are returned with an empty refresh token.
     */
    fun loadSdkSession(): UserSession? {
        val json = session() ?: return null
        val accessToken = json.optString("access_token").takeIf { it.split('.').size == 3 } ?: return null
        val refreshToken = json.optString("refresh_token").takeIf(String::isNotBlank).orEmpty()
        val expiresAtSecs = decodeJwtPayload(accessToken)?.optLong("exp", 0L) ?: 0L
        val expiresIn = (expiresAtSecs - System.currentTimeMillis() / 1000).coerceAtLeast(0L)
        return UserSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            tokenType = "bearer",
            user = null
        )
    }

    /** Persists an SDK-managed session (e.g. after an SDK auto-refresh) into the encrypted store. */
    fun saveSdkSession(session: UserSession) {
        val target = session() ?: JSONObject()
        if (session.accessToken.isNotBlank()) target.put("access_token", session.accessToken)
        if (session.refreshToken.isNotBlank()) target.put("refresh_token", session.refreshToken)
        checkNotNull(prefs) { "SupabaseSessionStore must be initialized by the Application" }
            .edit().putString(SESSION_KEY, target.toString()).apply()
    }

    /** A [SessionManager] that persists SDK sessions in the same encrypted store the app uses. */
    val sdkSessionManager: SessionManager = object : SessionManager {
        override suspend fun saveSession(session: UserSession) = saveSdkSession(session)
        override suspend fun loadSession(): UserSession? = loadSdkSession()
        override suspend fun deleteSession() {
            prefs?.edit()?.remove(SESSION_KEY)?.commit()
        }
    }
}
