package com.campus.network.core.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    fun session(): JSONObject? = prefs?.getString(SESSION_KEY, null)?.let { runCatching { JSONObject(it) }.getOrNull() }

    fun accessToken(): String? = session()?.optString("access_token")?.takeIf { it.split('.').size == 3 }

    suspend fun validAccessToken(supabaseUrl: String, anonKey: String): String = refreshMutex.withLock {
        val current = accessToken() ?: throw SecurityException("Sign in to access campus data")
        val expiresAt = runCatching {
            val payload = current.split('.')[1]
            JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
                .getLong("exp")
        }.getOrDefault(0L)
        if (expiresAt > System.currentTimeMillis() / 1000 + 60) return@withLock current

        val refreshToken = session()?.optString("refresh_token")?.takeIf(String::isNotBlank)
            ?: throw SecurityException("Session expired. Sign in again")
        val response = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("refresh_token", refreshToken).toString())
        }
        if (!response.status.isSuccess()) {
            clear()
            throw SecurityException("Session expired. Sign in again")
        }
        val body = JSONObject(response.bodyAsText())
        val nextToken = body.optString("access_token").takeIf { it.split('.').size == 3 }
            ?: throw SecurityException("Supabase did not return an access token")
        val prior = session() ?: throw SecurityException("Session was cleared")
        prior.put("access_token", nextToken)
        prior.put("refresh_token", body.optString("refresh_token", refreshToken))
        prefs?.edit()?.putString(SESSION_KEY, prior.toString())?.commit()
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

    fun clear() { prefs?.edit()?.remove(SESSION_KEY)?.commit() }
}
