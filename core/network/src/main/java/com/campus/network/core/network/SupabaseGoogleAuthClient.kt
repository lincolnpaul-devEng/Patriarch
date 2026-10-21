package com.campus.network.core.network

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import org.json.JSONObject

data class AuthenticatedSessionUser(
    val userId: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val requiresEmailConfirmation: Boolean = false
)

/**
 * Supabase + Google OAuth Authentication Client replicating the SocialPulse mobile auth architecture:
 * - Native Google Sign-In ID Token exchange (`POST /auth/v1/token?grant_type=id_token` with `provider = "google"`)
 * - Email & Password Sign-In (`POST /auth/v1/token?grant_type=password`)
 * - Email & Password Sign-Up (`POST /auth/v1/signup` with `data.full_name`)
 * - Persistent session storage under key `socialpulse-auth-token`
 * - Friendly error mapping (`getFriendlyErrorMessage`)
 */
class SupabaseGoogleAuthClient(
    context: Context,
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val client = HttpClient(OkHttp)
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    init { SupabaseSessionStore.initialize(context) }

    val googleWebClientId: String
        get() = BuildConfig.GOOGLE_CLIENT_ID_WEB

    val googleAndroidClientId: String
        get() = BuildConfig.GOOGLE_CLIENT_ID_ANDROID

    /**
     * Exchanges a Google ID Token with Supabase Auth (`supabase.auth.signInWithIdToken({ provider: 'google', token })`).
     */
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
            }
            val response: HttpResponse = client.post("$supabaseUrl/auth/v1/token?grant_type=id_token") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val sessionUser = parseAuthResponse(
                    body = body,
                    fallbackEmail = fallbackEmail,
                    fallbackName = fallbackName,
                    fallbackAvatarUrl = fallbackAvatarUrl
                )
                saveSession(body, sessionUser)
                sessionUser
            } else {
                throw IllegalStateException(extractSupabaseError(body))
            }
        }
    }

    // Google sign-in requires an ID token accepted by Supabase Auth. A Google profile alone
    // is not an authenticated Supabase session.

    /**
     * Signs in a user with email & password (`supabase.auth.signInWithPassword({ email, password })`).
     */
    suspend fun signInWithPassword(
        email: String,
        password: String
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }
            val response: HttpResponse = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw IllegalStateException(extractSupabaseError(body))
            }
            val sessionUser = parseAuthResponse(
                body = body,
                fallbackEmail = cleanEmail,
                fallbackName = null,
                fallbackAvatarUrl = null
            )
            saveSession(body, sessionUser)
            sessionUser
        }
    }

    /**
     * Registers a new user with email, password, and full_name metadata (`supabase.auth.signUp`).
     */
    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            val cleanName = fullName.trim()
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", cleanName)
                })
            }
            val response: HttpResponse = client.post("$supabaseUrl/auth/v1/signup") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw IllegalStateException(extractSupabaseError(body))
            }
            val responseJson = JSONObject(body)
            val userJson = responseJson.optJSONObject("user") ?: responseJson
            val identities = userJson.optJSONArray("identities")
            if (identities != null && identities.length() == 0) {
                throw IllegalStateException("User already registered")
            }
            val sessionUser = parseAuthResponse(
                body = body,
                fallbackEmail = cleanEmail,
                fallbackName = cleanName,
                fallbackAvatarUrl = null
            )
            if (!sessionUser.accessToken.isNullOrBlank()) {
                saveSession(body, sessionUser)
            }
            sessionUser
        }
    }

    /**
     * Dispatches a real 6-digit verification code using Resend via Supabase Edge Function `send-verification-otp`.
     */
    suspend fun sendOtp(
        email: String,
        fullName: String? = null
    ): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            val payload = JSONObject().apply {
                put("action", "send")
                put("email", cleanEmail)
                if (!fullName.isNullOrBlank()) {
                    put("fullName", fullName.trim())
                }
            }
            val response: HttpResponse = client.post("$supabaseUrl/functions/v1/send-verification-otp") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw IllegalStateException(extractSupabaseError(body))
            }
            true
        }
    }

    /**
     * Verifies a 6-digit OTP code against the deployed Supabase Edge Function (and Supabase Auth),
     * marks user as verified in database, and builds authenticated session.
     */
    suspend fun verifyOtp(
        email: String,
        token: String,
        type: String = "signup"
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            val cleanToken = token.trim()

            // 1. Verify 6-digit OTP code against the deployed Supabase Edge Function with Resend & DB lookup
            val edgePayload = JSONObject().apply {
                put("action", "verify")
                put("email", cleanEmail)
                put("code", cleanToken)
            }
            val edgeResponse: HttpResponse = client.post("$supabaseUrl/functions/v1/send-verification-otp") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(edgePayload.toString())
            }
            val edgeBody = edgeResponse.bodyAsText()
            if (!edgeResponse.status.isSuccess()) {
                // Reject invalid or expired OTP code immediately
                throw IllegalStateException(extractSupabaseError(edgeBody))
            }

            // 2. Also attempt Supabase Auth /auth/v1/verify if available
            val authPayload = JSONObject().apply {
                put("type", type)
                put("email", cleanEmail)
                put("token", cleanToken)
            }
            val authResponse: HttpResponse = runCatching {
                client.post("$supabaseUrl/auth/v1/verify") {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                    contentType(ContentType.Application.Json)
                    setBody(authPayload.toString())
                }
            }.getOrNull() ?: edgeResponse

            val authBody = runCatching { authResponse.bodyAsText() }.getOrDefault("")

            check(authResponse.status.isSuccess() && authBody.isNotBlank()) {
                "Code verified, but Supabase Auth did not issue a session. Request a new sign-in code."
            }
            val sessionUser = parseAuthResponse(authBody, cleanEmail, null, null)
            check(!sessionUser.accessToken.isNullOrBlank() && !sessionUser.refreshToken.isNullOrBlank()) {
                "Supabase Auth did not issue a valid session"
            }
            saveSession(authBody, sessionUser)
            sessionUser
        }
    }

    /**
     * Resends an OTP verification code via the deployed send-verification-otp Edge Function.
     */
    suspend fun resendOtp(
        email: String,
        type: String = "signup",
        fullName: String? = null
    ): Result<Boolean> = withContext(ioDispatcher) {
        sendOtp(email = email, fullName = fullName)
    }

    /**
     * Checks if a student profile is already registered in `campusprofile` table by email.
     */
    suspend fun isProfileRegistered(email: String): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank()) return@runCatching false
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")
            val response: HttpResponse = client.get("$supabaseUrl/rest/v1/campusprofile?university_email=ilike.$encodedEmail&select=id,university_email") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                header("Accept", "application/json")
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText().trim()
                val jsonArray = org.json.JSONArray(body)
                jsonArray.length() > 0
            } else {
                throw IllegalStateException(extractSupabaseError(response.bodyAsText()))
            }
        }
    }

    /**
     * Registers a student profile in the `campusprofile` table so all Google and email
     * signups are tracked in the exact same table.
     */
    suspend fun registerCampusProfile(
        userId: String? = null,
        email: String,
        fullName: String,
        avatarUrl: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val session = getSavedSession() ?: error("Sign in before registering a profile")
            check(session.email.equals(email, ignoreCase = true)) { "Profile email must match the session" }
            check(userId == null || session.userId == userId) { "Profile identity must match the session" }
            SupabaseClientProvider(supabaseUrl, anonKey)
                .callRpc("ensure_campusprofile_for_auth_user", JSONObject().apply {
                    put("p_full_name", fullName)
                    put("p_avatar_url", avatarUrl)
                }.toString()).getOrThrow()
        }
    }

    fun getSavedSession(): AuthenticatedSessionUser? {
        val stored = SupabaseSessionStore.session() ?: return null
        val email = stored.optString(KEY_EMAIL).takeIf { it.isNotBlank() } ?: return null
        return AuthenticatedSessionUser(
            userId = stored.optString(KEY_USER_ID),
            email = email,
            fullName = stored.optString(KEY_FULL_NAME).takeIf { it.isNotBlank() },
            avatarUrl = stored.optString(KEY_AVATAR_URL).takeIf { it.isNotBlank() },
            accessToken = SupabaseSessionStore.accessToken(),
            refreshToken = stored.optString(KEY_REFRESH_TOKEN).takeIf { it.isNotBlank() }
        )
    }

    fun isSurveyCompleted(email: String? = null): Boolean {
        val targetEmail = email?.trim()?.lowercase() ?: getSavedSession()?.email?.trim()?.lowercase()
        if (targetEmail.isNullOrBlank()) return prefs.getBoolean(KEY_SURVEY_COMPLETED_GLOBAL, false)
        return prefs.getBoolean("survey_completed_$targetEmail", false) ||
            prefs.getBoolean(KEY_SURVEY_COMPLETED_GLOBAL, false)
    }

    fun setSurveyCompleted(email: String? = null, completed: Boolean = true) {
        val targetEmail = email?.trim()?.lowercase() ?: getSavedSession()?.email?.trim()?.lowercase()
        prefs.edit().apply {
            putBoolean(KEY_SURVEY_COMPLETED_GLOBAL, completed)
            if (!targetEmail.isNullOrBlank()) {
                putBoolean("survey_completed_$targetEmail", completed)
            }
            apply()
        }
    }

    fun hasSeenIntroOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_INTRO, false)
    }

    fun setHasSeenIntroOnboarding(seen: Boolean = true) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_INTRO, seen).apply()
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

    private fun saveSession(rawJson: String, user: AuthenticatedSessionUser) {
        SupabaseSessionStore.save(user)
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
        private const val KEY_SURVEY_COMPLETED_GLOBAL = "has_completed_survey"
        private const val KEY_HAS_SEEN_INTRO = "has_seen_intro_onboarding"

        /**
         * Direct port of SocialPulse's `utils/errorMessages.ts` `getFriendlyErrorMessage`.
         */
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

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
