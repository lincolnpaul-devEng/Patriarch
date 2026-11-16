package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class FlashMeetRow(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category_name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val location_name: String = "",
    val creator_name: String = "",
    val creator_id: String = "",
    val creator_avatar: String? = null,
    val created_at_epoch_ms: Long = 0L,
    val expires_at_epoch_ms: Long = 0L,
    val attendee_count: Int = 1,
    val max_attendees: Int = 6
)

@Serializable
data class EventRow(
    val id: String = "",
    val creator_id: String = "",
    val title: String = "",
    val description: String = "",
    val location_name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val start_time: String = "",
    val category: String = "",
    val attendee_count: Int = 0
)

/**
 * Repository for Tab 1: Map (Flash-Meet and Campus Events Interface).
 * Enforces online-first remote persistence with Room local caching.
 */
class FlashMeetRepository(
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = database.flashMeetDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    /**
     * Emits live meets continuously on the IO thread.
     */
    fun getActiveFlashMeets(): Flow<List<FlashMeet>> {
        return dao.getLiveMeets(System.currentTimeMillis())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Creates a flash meet remotely and then caches in Room.
     */
    suspend fun createFlashMeet(
        title: String,
        description: String,
        category: MeetCategory,
        latitude: Double,
        longitude: Double,
        locationName: String,
        creatorUserId: String,
        durationMinutes: Int,
        creatorDisplayName: String? = null
    ): FlashMeet = withContext(ioDispatcher) {
        val resolvedCreatorId = creatorUserId.trim()
        require(resolvedCreatorId.isNotBlank()) { "creatorUserId is required to publish a flash meet" }
        val resolvedCreatorName = creatorDisplayName?.takeIf(String::isRealProfileDisplayName)
            ?: profileNameResolver.resolve(listOf(resolvedCreatorId))[resolvedCreatorId.lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to publish a flash meet")
        val now = System.currentTimeMillis()
        val expiry = now + (durationMinutes * 60 * 1000L)
        val syncId = java.util.UUID.randomUUID().toString()
        val meetId = "meet-$syncId"

        val pendingEntity = FlashMeetEntity(
            id = meetId,
            title = title,
            description = description,
            categoryName = category.name,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            creatorName = resolvedCreatorName,
            creatorUserId = resolvedCreatorId,
            creatorAvatar = null,
            createdAtEpochMs = now,
            expiresAtEpochMs = expiry,
            attendeeCount = 1,
            maxAttendees = 6,
            syncStatus = com.campus.network.core.model.SyncStatus.PENDING.name,
            syncId = syncId
        )
        dao.insertMeets(listOf(pendingEntity))
        dao.purgeExpired(now)

        val remoteResult = runCatching {
            supabase.from("campus_flash_meets").upsert(
                FlashMeetRow(
                    id = meetId,
                    title = title,
                    description = description,
                    category_name = category.name,
                    latitude = latitude,
                    longitude = longitude,
                    location_name = locationName,
                    creator_name = resolvedCreatorName,
                    creator_id = resolvedCreatorId,
                    created_at_epoch_ms = now,
                    expires_at_epoch_ms = expiry,
                    attendee_count = 1,
                    max_attendees = 6
                )
            )
        }

        val syncStatus = if (remoteResult.isSuccess) {
            com.campus.network.core.model.SyncStatus.SYNCED
        } else {
            com.campus.network.core.model.SyncStatus.FAILED
        }
        val finalEntity = pendingEntity.copy(syncStatus = syncStatus.name)
        dao.insertMeets(listOf(finalEntity))
        dao.purgeExpired(now)

        return@withContext finalEntity.toDomain()
    }

    /**
     * Creates a campus event directly in remote Supabase table.
     */
    suspend fun createCampusEvent(
        id: String,
        title: String,
        description: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        startTimeIso: String,
        category: String,
        attendeeCount: Int
    ): Result<Unit> = withContext(ioDispatcher) {
        val creatorId = com.campus.network.core.network.SupabaseSessionStore.session()
            ?.optString("user_id")?.takeIf(String::isNotBlank)
            ?: return@withContext Result.failure(SecurityException("Sign in to create a campus event"))
        runCatching {
            supabase.from("campus_events").upsert(
                EventRow(
                    id = id,
                    creator_id = creatorId,
                    title = title,
                    description = description,
                    location_name = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    start_time = startTimeIso,
                    category = category,
                    attendee_count = attendeeCount
                )
            )
            Unit
        }
    }

    /**
     * Synchronizes live flash meets from Supabase to local Room database.
     */
    suspend fun syncRemoteFlashMeets(sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val now = System.currentTimeMillis()
            val rows: List<FlashMeetRow> = supabase.from("campus_flash_meets")
                .select(columns = Columns.ALL) {
                    filter {
                        gt("expires_at_epoch_ms", now)
                        if (sinceMs > 0L) {
                            gt("created_at_epoch_ms", sinceMs)
                        }
                    }
                    order("created_at_epoch_ms", order = Order.ASCENDING)
                    limit(100)
                }
                .decodeList<FlashMeetRow>()

            var newWatermark = sinceMs
            val creatorNames = try {
                profileNameResolver.resolve(rows.map { it.creator_id })
            } catch (error: Exception) {
                emptyMap<String, String>()
            }

            val entities = rows.map { row ->
                val cam = row.created_at_epoch_ms
                if (cam > newWatermark) newWatermark = cam
                FlashMeetEntity(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    categoryName = row.category_name.ifBlank { MeetCategory.STUDY_JAM.name },
                    latitude = row.latitude,
                    longitude = row.longitude,
                    locationName = row.location_name,
                    creatorUserId = row.creator_id,
                    creatorName = creatorNames[row.creator_id.lowercase()].orEmpty().ifBlank { row.creator_name },
                    creatorAvatar = row.creator_avatar,
                    createdAtEpochMs = cam,
                    expiresAtEpochMs = row.expires_at_epoch_ms,
                    attendeeCount = row.attendee_count,
                    maxAttendees = row.max_attendees
                )
            }
            if (entities.isNotEmpty()) {
                dao.insertMeets(entities)
            }
            if (sinceMs == 0L) {
                val remoteIds = entities.map { it.id }
                if (remoteIds.isNotEmpty()) {
                    dao.deleteGhostMeets(remoteIds)
                } else {
                    dao.clearSyncedMeets()
                }
            }
            newWatermark
        }
    }
}
