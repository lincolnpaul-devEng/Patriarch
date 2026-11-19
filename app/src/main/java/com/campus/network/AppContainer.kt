package com.campus.network

import android.content.Context
import com.campus.network.core.data.AcademicChannelsRepository
import com.campus.network.core.data.CampusHubRepository
import com.campus.network.core.data.CampusStatsRepository
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.data.FlashMeetRepository
import com.campus.network.core.data.LiveStreamRepository
import com.campus.network.core.data.ServerChannelChatRepository
import com.campus.network.core.data.ServerRepository
import com.campus.network.core.data.RendezvousRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient

/**
 * Dependency container providing app-scoped repositories and services.
 */
interface AppContainer {
    val database: CampusDatabase
    val supabaseClient: SupabaseClient
    val studentProfileRepository: StudentProfileRepository
    val serverRepository: ServerRepository
    val hubRepository: CampusHubRepository
    val rendezvousRepository: RendezvousRepository
    val campusStatsRepository: CampusStatsRepository
    val academicChannelsRepository: AcademicChannelsRepository
    val serverChannelChatRepository: ServerChannelChatRepository
    val flashMeetRepository: FlashMeetRepository
    val liveStreamRepository: LiveStreamRepository
    val deltaSyncManager: DeltaSyncManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: CampusDatabase by lazy {
        CampusDatabase.getInstance(context)
    }

    override val supabaseClient: SupabaseClient
        get() = SupabaseSdkClientProvider.defaultClient

    override val studentProfileRepository: StudentProfileRepository by lazy {
        StudentProfileRepository(
            database = database,
            sdkClient = supabaseClient
        )
    }

    override val serverRepository: ServerRepository by lazy {
        ServerRepository(
            database = database,
            supabase = supabaseClient
        )
    }

    override val campusStatsRepository: CampusStatsRepository by lazy {
        CampusStatsRepository()
    }

    override val hubRepository: CampusHubRepository by lazy {
        CampusHubRepository(
            context = context,
            database = database,
            supabase = supabaseClient
        )
    }

    override val rendezvousRepository: RendezvousRepository by lazy {
        RendezvousRepository()
    }

    override val academicChannelsRepository: AcademicChannelsRepository by lazy {
        AcademicChannelsRepository(database)
    }

    override val serverChannelChatRepository: ServerChannelChatRepository by lazy {
        ServerChannelChatRepository(
            supabase = supabaseClient,
            messageDao = database.serverChannelMessageDao()
        )
    }

    override val flashMeetRepository: FlashMeetRepository by lazy {
        FlashMeetRepository(
            database = database,
            supabase = supabaseClient
        )
    }

    override val liveStreamRepository: LiveStreamRepository by lazy {
        LiveStreamRepository(
            database = database,
            supabase = supabaseClient
        )
    }

    override val deltaSyncManager: DeltaSyncManager by lazy {
        DeltaSyncManager.get(context)
    }
}
