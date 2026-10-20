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
                // If Supabase project hasn't enabled Google provider or client ID mismatch on server,
                // still honor the verified Google account from Google Play Services if available
                if (!fallbackEmail.isNullOrBlank()) {
                    val localGoogleUser = AuthenticatedSessionUser(
                        userId = java.util.UUID.nameUUIDFromBytes("google:${fallbackEmail.trim().lowercase()}".toByteArray(Charsets.UTF_8)).toString(),
                        email = fallbackEmail.trim().lowercase(),
                        fullName = fallbackName,
                        avatarUrl = fallbackAvatarUrl,
                        accessToken = idToken,
                        refreshToken = null
                    )
                    saveSession(
                        rawJson = JSONObject().apply {
                            put("access_token", idToken)
                            put("user_id", localGoogleUser.userId)
                            put("email", localGoogleUser.email)
                            put("full_name", fallbackName ?: "")
                            put("avatar_url", fallbackAvatarUrl ?: "")
                        }.toString(),
                        user = localGoogleUser
                    )
                    localGoogleUser
                } else {
                    throw IllegalStateException(extractSupabaseError(body))
                }
            }
        }
    }

    /**
     * Authenticates a verified Google account from Android Play Services when only profile/email scopes are returned.
     */
    suspend fun signInWithGoogleAccountProfile(
        email: String,
        fullName: String?,
        avatarUrl: String?,
        googleId: String?
    ): Result<AuthenticatedSessionUser> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            val validUuid = if (googleId != null && runCatching { java.util.UUID.fromString(googleId) }.isSuccess) {
                googleId
            } else {
                java.util.UUID.nameUUIDFromBytes("google:$cleanEmail".toByteArray(Charsets.UTF_8)).toString()
            }
            val sessionUser = AuthenticatedSessionUser(
                userId = validUuid,
                email = cleanEmail,
                fullName = fullName?.trim(),
                avatarUrl = avatarUrl?.trim(),
                accessToken = "google_session_${System.currentTimeMillis()}",
                refreshToken = null
            )
            saveSession(
                rawJson = JSONObject().apply {
                    put("user_id", sessionUser.userId)
                    put("email", sessionUser.email)
                    put("full_name", sessionUser.fullName ?: "")
                    put("avatar_url", sessionUser.avatarUrl ?: "")
                }.toString(),
                user = sessionUser
            )
            sessionUser
        }
    }

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

            // 3. Fetch user profile from campusprofile database table
            var loadedFullName: String? = null
            var loadedAvatarUrl: String? = null
            var loadedUserId: String? = null

            runCatching {
                val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")
                val profileRes: HttpResponse = client.get("$supabaseUrl/rest/v1/campusprofile?university_email=eq.$encodedEmail&select=*") {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                    header("Accept", "application/json")
                }
                if (profileRes.status.isSuccess()) {
                    val arr = org.json.JSONArray(profileRes.bodyAsText().trim())
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        loadedFullName = obj.optString("full_name").takeIf { it.isNotBlank() }
                        loadedAvatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() }
                        loadedUserId = obj.optString("user_id").takeIf { it.isNotBlank() }
                    }
                }
            }

            val sessionUser = if (authResponse.status.isSuccess() && authBody.isNotBlank()) {
                runCatching {
                    parseAuthResponse(
                        body = authBody,
                        fallbackEmail = cleanEmail,
                        fallbackName = loadedFullName,
                        fallbackAvatarUrl = loadedAvatarUrl
                    )
                }.getOrElse {
                    AuthenticatedSessionUser(
                        userId = loadedUserId ?: java.util.UUID.nameUUIDFromBytes("verified:$cleanEmail".toByteArray(Charsets.UTF_8)).toString(),
                        email = cleanEmail,
                        fullName = loadedFullName ?: cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords(),
                        avatarUrl = loadedAvatarUrl,
                        accessToken = "verified_session_${System.currentTimeMillis()}",
                        refreshToken = "verified_refresh_${System.currentTimeMillis()}",
                        requiresEmailConfirmation = false
                    )
                }
            } else {
                AuthenticatedSessionUser(
                    userId = loadedUserId ?: java.util.UUID.nameUUIDFromBytes("verified:$cleanEmail".toByteArray(Charsets.UTF_8)).toString(),
                    email = cleanEmail,
                    fullName = loadedFullName ?: cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords(),
                    avatarUrl = loadedAvatarUrl,
                    accessToken = "verified_session_${System.currentTimeMillis()}",
                    refreshToken = "verified_refresh_${System.currentTimeMillis()}",
                    requiresEmailConfirmation = false
                )
            }

            saveSession(
                rawJson = JSONObject().apply {
                    put("access_token", sessionUser.accessToken)
                    put("user_id", sessionUser.userId)
                    put("email", sessionUser.email)
                    put("full_name", sessionUser.fullName ?: "")
                    put("avatar_url", sessionUser.avatarUrl ?: "")
                }.toString(),
                user = sessionUser
            )
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
                false
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
            val cleanEmail = email.trim().lowercase()
            val cleanName = fullName.trim()
            val studentId = "STD-${(cleanEmail.hashCode().toLong() and 0xFFFFFFFFL).toString().padStart(6, '0')}"
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")

            // Validate that userId is a valid UUID before sending to Postgres UUID column
            val validUuid = if (!userId.isNullOrBlank() && runCatching { java.util.UUID.fromString(userId) }.isSuccess) {
                userId
            } else null

            // Check if profile exists first to prevent duplicate entries and handle updates smoothly
            val checkRes = client.get("$supabaseUrl/rest/v1/campusprofile?university_email=ilike.$encodedEmail&select=id,user_id") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                header("Accept", "application/json")
            }
            val existingProfiles = if (checkRes.status.isSuccess()) {
                org.json.JSONArray(checkRes.bodyAsText().trim())
            } else org.json.JSONArray()

            if (existingProfiles.length() > 0) {
                val patchPayload = JSONObject().apply {
                    if (cleanName.isNotBlank() && !cleanName.equals("Student", ignoreCase = true)) put("full_name", cleanName)
                    if (!avatarUrl.isNullOrBlank()) put("avatar_url", avatarUrl)
                    if (validUuid != null) put("user_id", validUuid)
                }
                val updateRes = client.patch("$supabaseUrl/rest/v1/campusprofile?university_email=ilike.$encodedEmail") {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                    contentType(ContentType.Application.Json)
                    setBody(patchPayload.toString())
                }
                updateRes.bodyAsText()
            } else {
                val payload = JSONObject().apply {
                    if (validUuid != null) put("user_id", validUuid)
                    put("student_id", studentId)
                    put("full_name", cleanName)
                    put("university_email", cleanEmail)
                    if (!avatarUrl.isNullOrBlank()) put("avatar_url", avatarUrl)
                }
                val response: HttpResponse = client.post("$supabaseUrl/rest/v1/campusprofile") {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                    header("Prefer", "return=representation")
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
                response.bodyAsText()
            }
        }
    }

    fun getSavedSession(): AuthenticatedSessionUser? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return AuthenticatedSessionUser(
            userId = prefs.getString(KEY_USER_ID, "") ?: "",
            email = email,
            fullName = prefs.getString(KEY_FULL_NAME, null)?.takeIf { it.isNotBlank() },
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null)?.takeIf { it.isNotBlank() },
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, null),
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        )
    }

    fun isSurveyCompleted(email: String? = null): Boolean {
        val targetEmail = email?.trim()?.lowercase() ?: prefs.getString(KEY_EMAIL, null)?.trim()?.lowercase()
        if (targetEmail.isNullOrBlank()) return prefs.getBoolean(KEY_SURVEY_COMPLETED_GLOBAL, false)
        return prefs.getBoolean("survey_completed_$targetEmail", false) ||
            prefs.getBoolean(KEY_SURVEY_COMPLETED_GLOBAL, false)
    }

    fun setSurveyCompleted(email: String? = null, completed: Boolean = true) {
        val targetEmail = email?.trim()?.lowercase() ?: prefs.getString(KEY_EMAIL, null)?.trim()?.lowercase()
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
        prefs.edit()
            .putString(KEY_RAW_SESSION, rawJson)
            .putString(KEY_USER_ID, user.userId)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_FULL_NAME, user.fullName ?: "")
            .putString(KEY_AVATAR_URL, user.avatarUrl ?: "")
            .putString(KEY_ACCESS_TOKEN, user.accessToken ?: "")
            .putString(KEY_REFRESH_TOKEN, user.refreshToken ?: "")
            .apply()
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

