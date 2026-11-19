package com.campus.network.core.data

import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Real home-screen statistics: global counts of campus events, posts and guilds.
 * Backed by the `campus_home_stats` aggregate RPC (no row data is exposed).
 */
class CampusStatsRepository(
    private val clientProvider: () -> io.github.jan.supabase.SupabaseClient = {
        SupabaseSdkClientProvider.defaultClient
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    data class CampusStats(
        val eventsCount: Int = 0,
        val postsCount: Int = 0,
        val guildsCount: Int = 0
    ) {
        companion object {
            val Empty = CampusStats()
        }
    }

    @Serializable
    private data class CampusStatsRow(
        @SerialName("events_count") val eventsCount: Long = 0L,
        @SerialName("posts_count") val postsCount: Long = 0L,
        @SerialName("guilds_count") val guildsCount: Long = 0L
    )

    suspend fun fetchStats(): Result<CampusStats> = withContext(ioDispatcher) {
        runCatching {
            val row = clientProvider().postgrest.rpc("campus_home_stats").decodeSingle<CampusStatsRow>()
            CampusStats(
                eventsCount = row.eventsCount.coerceAtLeast(0L).toInt(),
                postsCount = row.postsCount.coerceAtLeast(0L).toInt(),
                guildsCount = row.guildsCount.coerceAtLeast(0L).toInt()
            )
        }
    }
}
