package com.campus.network.core.network

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
 *
 * NOTE: The Supabase Kotlin SDK version (2.0.0) is pinned to be compatible with
 * the project's Kotlin 1.9.23. Newer versions (3.x+) require Kotlin 2.x.
 */
class SupabaseSdkClientProvider(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** The initialized Supabase client with Postgrest installed. */
    val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
        install(Postgrest)
    }

    /** Convenience access to the underlying client. */
    val client = supabase
}
