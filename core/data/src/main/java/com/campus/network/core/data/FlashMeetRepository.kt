package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.model.SyncStatus
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.RealtimeEventType
import com.campus.network.core.network.SupabaseRealtimeChange
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

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

@Serializable
data class FlashMeetAttendeeRow(
    val meet_id: String = "",
    val user_id: String = "",
    val joined_at_epoch_ms: Long = 0L
)

@Serializable
private data class ToggleFlashMeetAttendanceParams(
    val p_meet_id: String
)

@Serializable
private data class EndFlashMeetParams(
    val p_meet_id: String
)

private val flashMeetRealtimeJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/**
 * Repository for Tab 1: Map (Flash-Meet and Campus Events Interface).
 * Enforces online-first remote persistence with Room local caching and Supabase Realtime.
 */
class FlashMeetRepository(
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = database.flashMeetDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    private val _joinedMeetIds = MutableStateFlow<Set<String>>(emptySet())
    val joinedMeetIds: StateFlow<Set<String>> = _joinedMeetIds.asStateFlow()

    /**
     * Emits live non-expired meets continuously on the IO thread, re-evaluating expiry every 15s.
     */
    fun getActiveFlashMeets(): Flow<List<FlashMeet>> {
        val expiryTickerFlow = flow {
            emit(System.currentTimeMillis())
            while (currentCoroutineContext().isActive) {
                delay(EXPIRY_TICK_MS)
                val now = System.currentTimeMillis()
                runCatching { dao.purgeExpired(now) }
                emit(now)
            }
        }
        return combine(dao.getLiveMeets(0L), expiryTickerFlow) { entities, now ->
            entities
                .filter { it.expiresAtEpochMs > now }
                .map { it.toDomain() }
        }.flowOn(ioDispatcher)
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
            syncStatus = SyncStatus.PENDING.name,
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
            SyncStatus.SYNCED
        } else {
            SyncStatus.FAILED
        }
        val finalEntity = pendingEntity.copy(syncStatus = syncStatus.name)
        dao.insertMeets(listOf(finalEntity))
        dao.purgeExpired(now)

        return@withContext finalEntity.toDomain()
    }

    suspend fun toggleAttendance(meetId: String): Result<Pair<Boolean, Int>> = withContext(ioDispatcher) {
        runCatching {
            val resultObj = supabase.postgrest.rpc(
                "toggle_flash_meet_attendance",
                ToggleFlashMeetAttendanceParams(p_meet_id = meetId)
            ).decodeAs<JsonObject>()

            val isJoined = (resultObj["is_joined"] as? JsonPrimitive)?.booleanOrNull ?: false
            val attendeeCount = (resultObj["attendee_count"] as? JsonPrimitive)?.intOrNull ?: 1

            _joinedMeetIds.update { current ->
                if (isJoined) current + meetId else current - meetId
            }
            dao.updateAttendeeCount(meetId, attendeeCount)
            isJoined to attendeeCount
        }
    }

    suspend fun syncJoinedMeetIds(currentUserId: String?): Result<Set<String>> = withContext(ioDispatcher) {
        val cleanId = currentUserId?.trim().orEmpty()
        if (cleanId.isBlank()) {
            return@withContext Result.success(emptySet())
        }
        runCatching {
            val rows: List<FlashMeetAttendeeRow> = supabase.from("campus_flash_meet_attendees")
                .select(columns = Columns.ALL) {
                    filter { eq("user_id", cleanId) }
                    limit(100)
                }
                .decodeList()
            val joined = rows.map { it.meet_id }.toSet()
            _joinedMeetIds.value = joined
            joined
        }
    }

    suspend fun endFlashMeet(meetId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabase.postgrest.rpc(
                "end_flash_meet",
                EndFlashMeetParams(p_meet_id = meetId)
            )
            dao.deleteMeetById(meetId)
            _joinedMeetIds.update { it - meetId }
            Unit
        }
    }

    suspend fun applyRealtimeChange(
        change: SupabaseRealtimeChange,
        currentUserId: String? = null
    ): Unit = withContext(ioDispatcher) {
        when (change.table.lowercase()) {
            "campus_flash_meets" -> handleFlashMeetChange(change)
            "campus_flash_meet_attendees" -> {
                syncJoinedMeetIds(currentUserId)
                Unit
            }
            else -> Unit
        }
    }

    private suspend fun handleFlashMeetChange(change: SupabaseRealtimeChange) {
        if (change.eventType == RealtimeEventType.DELETE) {
            val deletedId = extractJsonField(change.oldRecordJson, "id") ?: return
            dao.deleteMeetById(deletedId)
            return
        }
        val jsonStr = change.recordJson ?: return
        val row = runCatching {
            flashMeetRealtimeJson.decodeFromString<FlashMeetRow>(jsonStr)
        }.getOrNull() ?: return
        if (row.id.isBlank()) return

        val now = System.currentTimeMillis()
        if (row.expires_at_epoch_ms <= now) {
            dao.deleteMeetById(row.id)
            return
        }

        val resolvedName = runCatching {
            profileNameResolver.resolve(listOf(row.creator_id))[row.creator_id.lowercase()]
        }.getOrNull().orEmpty().ifBlank { row.creator_name }

        dao.insertMeets(
            listOf(
                FlashMeetEntity(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    categoryName = row.category_name.ifBlank { MeetCategory.STUDY_JAM.name },
                    latitude = row.latitude,
                    longitude = row.longitude,
                    locationName = row.location_name,
                    creatorUserId = row.creator_id,
                    creatorName = resolvedName,
                    creatorAvatar = row.creator_avatar,
                    createdAtEpochMs = row.created_at_epoch_ms,
                    expiresAtEpochMs = row.expires_at_epoch_ms,
                    attendeeCount = row.attendee_count,
                    maxAttendees = row.max_attendees
                )
            )
        )
    }

    private fun extractJsonField(jsonStr: String?, key: String): String? {
        if (jsonStr.isNullOrBlank()) return null
        val obj = runCatching {
            flashMeetRealtimeJson.parseToJsonElement(jsonStr) as? JsonObject
        }.getOrNull() ?: return null
        return (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
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
            } catch (_: Exception) {
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
            dao.purgeExpired(now)
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

    companion object {
        private const val EXPIRY_TICK_MS = 15_000L
    }
}
