package com.campus.network.core.network

import io.github.jan.supabase.auth.AuthChangeEvent
import io.github.jan.supabase.auth.Session
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Supabase Kotlin SDK client provider.
 *
 * Introduced alongside the existing custom Ktor [SupabaseClientProvider] for a
 * non-destructive, side-by-side migration. Existing Ktor-based repositories keep
 * working unchanged; newly migrated repositories use this provider.
 *
 * Credentials come from BuildConfig (injected from gitignored secrets.properties).
 * Privileged keys (STREAM_SECRET_KEY, CLOUDINARY_API_SECRET, etc.) remain excluded
 * from the APK and are only used server-side in Edge Functions.
 *
 * NOTE: Compilation requires the Kotlin Serialization Gradle plugin
 * (org.jetbrains.kotlin.plugin.serialization) to be available. The plugin alias
 * is declared in gradle/libs.versions.toml as kotlin-serialization.
 */
class SupabaseSdkClientProvider(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** The initialized Supabase client with Postgrest and Auth installed. */
    val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
        install(Postgrest)
        install(Auth) {
            autoRefreshToken = true
            detectSessionInUrl = false
        }
    }

    /** Convenience access to the underlying client. */
    val client = supabase

    /**
     * Signs in with a Google ID token via Supabase Auth GoTrue.
     * Replaces the manual Ktor POST to /auth/v1/token?grant_type=id_token.
     */
    suspend fun signInWithIdToken(
        idToken: String,
        provider: String = "google"
    ): io.github.jan.supabase.auth.AuthResponse<Session> = withContext(ioDispatcher) {
        supabase.auth.signInWithIdToken(
            idToken = idToken,
            provider = provider
        )
    }

    /**
     * Fetches the current authenticated user.
     */
    suspend fun getCurrentUser(): io.github.jan.supabase.auth.UserResponse = withContext(ioDispatcher) {
        supabase.auth.getUser()
    }

    /**
     * Signs out the current user.
     */
    suspend fun signOut() = withContext(ioDispatcher) {
        supabase.auth.signOut()
    }
}
