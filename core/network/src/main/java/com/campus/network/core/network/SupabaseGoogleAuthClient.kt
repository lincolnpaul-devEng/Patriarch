package com.campus.network.core.network

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class AuthenticatedSessionUser(
    val userId: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?,
    val accessToken: String?,
    val refreshToken: String? = null,
    val requiresEmailConfirmation: Boolean = false
)

class SupabaseGoogleAuthClient(
    context: Context,
    private val supabaseClient: SupabaseSdkClientProvider = SupabaseSdkClientProvider(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        SupabaseSessionStore.initialize(context)
    }

    val googleWebClientId: String
        get() = BuildConfig.GOOGLE_CLIENT_ID_WEB

    val googleAndroidClientId: String
        get() = BuildConfig.GOOGLE_CLIENT_ID_ANDROID

    private val supabaseUrl: String = BuildConfig.SUPABASE_URL
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY

    private suspend fun invokeSupabaseRest(
        method: String,
        path: String,
        body: String? = null
    ): String = withContext(ioDispatcher) {
        val cleanPath = path.removePrefix("/")
        val url = URL("$supabaseUrl/$cleanPath")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doInput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("apikey", supabaseKey)
            setRequestProperty("Authorization", "Bearer $supabaseKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                outputStream.use { it.write(bytes) }
            }
        }
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.use { it.readBytes() }?.let { String(it, StandardCharsets.UTF_8) }.orEmpty()
        if (responseCode !in 200..299) {
            throw IllegalStateException(extractSupabaseError(responseBody).ifBlank { "HTTP $responseCode" })
        }
        responseBody
    }

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        fallbackEmail: String? = null,
        fallbackName: String? = null,
        fallbackAvatarUrl: String? = null
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val payload = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
            }.toString()
            val response = invokeSupabaseRest("POST", "auth/v1/token?grant_type=id_token", payload)
            val user = parseAuthResponse(response, fallbackEmail, fallbackName, fallbackAvatarUrl)
            saveSession(response, user)
            user
        }
    }

    suspend fun signInWithIdToken(
        idToken: String,
        fallbackEmail: String? = null,
        fallbackName: String? = null,
        fallbackAvatarUrl: String? = null
    ): Result<AuthenticatedSessionUser> = signInWithGoogleIdToken(
        idToken = idToken,
        fallbackEmail = fallbackEmail,
        fallbackName = fallbackName,
        fallbackAvatarUrl = fallbackAvatarUrl
    )

    suspend fun signInWithGoogleAccountProfile(
        email: String,
        fullName: String?,
        avatarUrl: String?,
        googleId: String?
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            // Fabricated local-only sessions are no longer supported: the caller must
            // complete a real Supabase Auth exchange (ID token / password / OTP).
            error("A real Supabase session is required; Google-account profile fallback was removed")
        }
    }

    suspend fun signInWithPassword(
        email: String,
        password: String
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }.toString()
            val response = invokeSupabaseRest("POST", "auth/v1/token?grant_type=password", payload)
            val user = parseAuthResponse(response, email.trim(), null, null)
            saveSession(response, user)
            user
        }
    }

    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName.trim())
                })
            }.toString()
            val response = invokeSupabaseRest("POST", "auth/v1/signup", payload)
            val user = parseAuthResponse(response, email.trim(), fullName.trim(), null)
            saveSession(response, user)
            user
        }
    }

    suspend fun sendOtp(email: String, fullName: String? = null): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val payload = JSONObject().apply {
                put("email", email.trim())
                if (!fullName.isNullOrBlank()) {
                    put("data", JSONObject().apply { put("full_name", fullName.trim()) })
                }
            }.toString()
            invokeSupabaseRest("POST", "auth/v1/otp", payload)
            true
        }
    }

    suspend fun verifyOtp(
        email: String,
        token: String,
        type: String = "signup"
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("token", token.trim())
                put("type", type)
            }.toString()
            val response = invokeSupabaseRest("POST", "auth/v1/verify", payload)
            val user = parseAuthResponse(response, email.trim(), null, null)
            saveSession(response, user)
            user
        }
    }

    suspend fun resendOtp(
        email: String,
        type: String = "signup",
        fullName: String? = null
    ): Result<Boolean> = sendOtp(email = email, fullName = fullName)

    fun getSavedSession(): AuthenticatedSessionUser? {
        val s = SupabaseSessionStore.session() ?: return null
        val token = s.optString("access_token")
        if (token.isNullOrBlank()) return null
        return AuthenticatedSessionUser(
            userId = s.optString("user_id"),
            email = s.optString("email"),
            fullName = s.optString("full_name").takeIf { it.isNotBlank() },
            avatarUrl = s.optString("avatar_url").takeIf { it.isNotBlank() },
            accessToken = token,
            refreshToken = s.optString("refresh_token").takeIf { it.isNotBlank() },
            requiresEmailConfirmation = false
        )
    }

    fun hasSeenIntroOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_INTRO, false)
    }

    fun setHasSeenIntroOnboarding(hasSeen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_INTRO, hasSeen).apply()
    }

    fun isSurveyCompleted(email: String): Boolean {
        val key = surveyCompletedKey(email.trim().lowercase())
        return prefs.getBoolean(key, false) || prefs.getBoolean(KEY_SURVEY_COMPLETED_GLOBAL, false)
    }

    fun setSurveyCompleted(email: String, completed: Boolean) {
        val key = surveyCompletedKey(email.trim().lowercase())
        prefs.edit()
            .putBoolean(key, completed)
            .putBoolean(KEY_SURVEY_COMPLETED_GLOBAL, completed)
            .apply()
    }

    fun signOut() {
        SupabaseSessionStore.clear()
        val hadSeenIntro = hasSeenIntroOnboarding()
        prefs.edit()
            .remove(KEY_RAW_SESSION)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_FULL_NAME)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        setHasSeenIntroOnboarding(hadSeenIntro)
    }

    private suspend fun saveSession(rawJson: String, user: AuthenticatedSessionUser) {
        SupabaseSessionStore.save(user)
        // Bring the SDK Auth plugin in lock-step so PostgREST attaches the fresh JWT.
        if (!user.accessToken.isNullOrBlank()) {
            runCatching {
                SupabaseSdkClientProvider.syncSession(
                    accessToken = user.accessToken,
                    refreshToken = user.refreshToken
                )
            }
        }
    }

    private fun parseAuthResponse(
        body: String,
        fallbackEmail: String?,
        fallbackName: String?,
        fallbackAvatarUrl: String?
    ): AuthenticatedSessionUser {
        val json = JSONObject(body)
        val accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val userObj = json.optJSONObject("user") ?: json

        val userId = userObj.optString("id").ifBlank { "user_${System.currentTimeMillis()}" }
        val email = userObj.optString("email").ifBlank { fallbackEmail ?: "" }
        val metadata = userObj.optJSONObject("user_metadata")
        val fullName = metadata?.optString("full_name")?.takeIf { it.isNotBlank() }
            ?: metadata?.optString("name")?.takeIf { it.isNotBlank() }
            ?: fallbackName
        val avatarUrl = metadata?.optString("avatar_url")?.takeIf { it.isNotBlank() }
            ?: metadata?.optString("picture")?.takeIf { it.isNotBlank() }
            ?: fallbackAvatarUrl

        val requiresConfirmation = accessToken == null && userObj.has("id")
        return AuthenticatedSessionUser(
            userId = userId,
            email = email,
            fullName = fullName,
            avatarUrl = avatarUrl,
            accessToken = accessToken,
            refreshToken = refreshToken,
            requiresEmailConfirmation = requiresConfirmation
        )
    }

    private fun extractSupabaseError(body: String): String {
        return runCatching {
            val json = JSONObject(body)
            json.optString("msg")
                .ifBlank { json.optString("error_description") }
                .ifBlank { json.optString("message") }
                .ifBlank { json.optString("error") }
                .ifBlank { body }
        }.getOrDefault(body)
    }

    companion object {
        private const val PREFS_NAME = "socialpulse-auth-token"
        private const val KEY_RAW_SESSION = "raw_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_HAS_SEEN_INTRO = "has_seen_intro_onboarding"
        private const val KEY_SURVEY_COMPLETED_GLOBAL = "has_completed_survey"

        private fun surveyCompletedKey(email: String) = "survey_completed_$email"

        fun getFriendlyErrorMessage(error: Throwable?): String {
            if (error == null) return "An unexpected error occurred. Please try again."
            val message = error.message ?: error.toString()

            if (message.contains("Invalid login credentials", ignoreCase = true)) {
                return "Incorrect email or password. Please try again."
            }
            if (message.contains("User already registered", ignoreCase = true)) {
                return "An account with this email already exists."
            }
            if (message.contains("Password should be at least", ignoreCase = true)) {
                return "Your password is too short. Please use at least 6 characters."
            }
            if (message.contains("Email not confirmed", ignoreCase = true)) {
                return "Please check your inbox and verify your email address."
            }
            if (message.contains("Unable to validate email address", ignoreCase = true) ||
                message.contains("invalid format", ignoreCase = true)
            ) {
                return "Please enter a valid email address."
            }
            if (message.contains("rate limit", ignoreCase = true) ||
                message.contains("Too many requests", ignoreCase = true)
            ) {
                return "Too many attempts. Please wait a moment before trying again."
            }
            if (message.contains("Network request failed", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("Failed to connect", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true)
            ) {
                return "Connection failed. Please check your internet and try again."
            }
            if (message.contains("SIGN_IN_CANCELLED", ignoreCase = true) ||
                message.contains("12501", ignoreCase = true)
            ) {
                return "Google sign-in was cancelled."
            }
            return message.ifBlank { "Something went wrong. Please try again later." }
        }
    }
}
