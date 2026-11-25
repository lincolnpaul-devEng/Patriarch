package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.CampusEventEntity
import com.campus.network.core.model.CampusEventRecord
import com.campus.network.core.model.SyncStatus
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.RealtimeEventType
import com.campus.network.core.network.SupabaseRealtimeChange
import com.campus.network.core.network.SupabaseSdkClientProvider
import com.campus.network.core.network.SupabaseSessionStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Serializable
data class CampusEventRow(
    val id: String = "",
    val creator_id: String? = null,
    val title: String = "",
    val host_name: String = "",
    val description: String = "",
    val location_name: String = "",
    val full_address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val start_time: String = "",
    val category: String = "Campus",
    val price_label: String? = "Free Admission",
    val max_capacity: Int = 100,
    val attendee_count: Int = 0,
    val artwork_key: String = "event_thumb_founders",
    val banner_url: String? = null
)

@Serializable
data class CampusEventRsvpRow(
    val event_id: String = "",
    val user_id: String = "",
    val display_name: String = "",
    val created_at_epoch_ms: Long = 0L
)

@Serializable
private data class ToggleEventRsvpParams(
    val p_event_id: String
)

private val eventsRealtimeJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/**
 * Online-first repository for Campus Events and RSVPs backed by Room (`campus_events`)
 * and Supabase (`campus_events`, `campus_event_rsvps`, `toggle_campus_event_rsvp` RPC).
 */
class CampusEventsRepository(
    private val database: CampusDatabase? = null,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val eventDao = database?.campusEventDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    private val _memoryEvents = MutableStateFlow<List<CampusEventRecord>>(emptyList())
    private val _registeredEventIds = MutableStateFlow<Set<String>>(emptySet())
    val registeredEventIds: StateFlow<Set<String>> = _registeredEventIds.asStateFlow()

    private val _attendeePreviews = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    fun observeEvents(): Flow<List<CampusEventRecord>> {
        val baseFlow: Flow<List<CampusEventRecord>> = if (eventDao != null) {
            combine(eventDao.getAllEvents(), flowOf(Unit)) { entities, _ ->
                entities.map { it.toDomain() }
            }
        } else {
            _memoryEvents
        }

        return combine(
            baseFlow,
            _registeredEventIds,
            _attendeePreviews
        ) { events, registeredIds, previews ->
            events.map { ev ->
                ev.copy(
                    isUserRegistered = ev.id in registeredIds,
                    attendeeNames = previews[ev.id].orEmpty()
                )
            }
        }.flowOn(ioDispatcher)
    }

    suspend fun createEvent(
        id: String = "evt_${UUID.randomUUID()}",
        creatorUserId: String,
        title: String,
        hostName: String,
        description: String,
        locationName: String,
        fullAddress: String,
        latitude: Double,
        longitude: Double,
        startTimeIso: String,
        category: String,
        priceLabel: String?,
        maxCapacity: Int,
        artworkKey: String,
        bannerUrl: String? = null
    ): Result<CampusEventRecord> = withContext(ioDispatcher) {
        val resolvedCreatorId = creatorUserId.trim().ifBlank {
            SupabaseSessionStore.session()?.optString("user_id").orEmpty().trim()
        }
        if (resolvedCreatorId.isBlank()) {
            return@withContext Result.failure(SecurityException("Sign in to create a campus event"))
        }
        val creatorProfileName = runCatching {
            profileNameResolver.resolve(listOf(resolvedCreatorId))[resolvedCreatorId.lowercase()]
        }.getOrNull().orEmpty()
        val resolvedHost = hostName.trim().ifBlank { creatorProfileName }
        val startEpochMs = parseIsoToEpochMs(startTimeIso)
        val syncId = UUID.randomUUID().toString()

        val row = CampusEventRow(
            id = id,
            creator_id = resolvedCreatorId,
            title = title.trim(),
            host_name = resolvedHost,
            description = description.trim(),
            location_name = locationName.trim(),
            full_address = fullAddress.trim(),
            latitude = latitude,
            longitude = longitude,
            start_time = startTimeIso,
            category = category,
            price_label = priceLabel,
            max_capacity = maxCapacity.coerceAtLeast(1),
            attendee_count = 1,
            artwork_key = artworkKey.ifBlank { "event_thumb_founders" },
            banner_url = bannerUrl
        )

        val remoteResult = runCatching {
            supabase.from("campus_events").upsert(row)
            // Record creator as initial attendee so toggle_campus_event_rsvp count stays consistent.
            runCatching {
                supabase.from("campus_event_rsvps").upsert(
                    CampusEventRsvpRow(
                        event_id = id,
                        user_id = resolvedCreatorId,
                        display_name = creatorProfileName,
                        created_at_epoch_ms = System.currentTimeMillis()
                    ),
                    onConflict = "event_id,user_id"
                )
            }
        }

        if (remoteResult.isFailure) {
            return@withContext Result.failure(
                remoteResult.exceptionOrNull() ?: IllegalStateException("Failed to publish event")
            )
        }

        val entity = CampusEventEntity(
            id = row.id,
            creatorId = resolvedCreatorId,
            title = row.title,
            hostName = resolvedHost,
            description = row.description,
            locationName = row.location_name,
            fullAddress = row.full_address,
            latitude = row.latitude,
            longitude = row.longitude,
            startTimeIso = row.start_time,
            startTimeEpochMs = startEpochMs,
            category = row.category,
            priceLabel = row.price_label,
            maxCapacity = row.max_capacity,
            attendeeCount = 1,
            artworkKey = row.artwork_key,
            bannerUrl = row.banner_url,
            syncStatus = SyncStatus.SYNCED.name,
            syncId = syncId
        )
        eventDao?.insertEvent(entity)
        _registeredEventIds.update { it + id }
        if (creatorProfileName.isRealProfileDisplayName()) {
            _attendeePreviews.update { map ->
                map + (id to (listOf(creatorProfileName) + map[id].orEmpty()).distinct().take(3))
            }
        }
        val domain = entity.toDomain().copy(
            isUserRegistered = true,
            attendeeNames = _attendeePreviews.value[id].orEmpty()
        )
        if (eventDao == null) {
            _memoryEvents.update { list -> (listOf(domain) + list.filterNot { it.id == id }) }
        }
        Result.success(domain)
    }

    suspend fun syncRemoteEvents(
        currentUserId: String? = null
    ): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<CampusEventRow> = supabase.from("campus_events")
                .select(columns = Columns.ALL) {
                    order("start_time", order = Order.ASCENDING)
                    limit(100)
                }
                .decodeList()

            val creatorIds = rows.mapNotNull { it.creator_id?.takeIf(String::isNotBlank) }
            val resolvedCreators = if (creatorIds.isNotEmpty()) {
                runCatching { profileNameResolver.resolve(creatorIds) }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            val entities = rows.map { row ->
                val cId = row.creator_id.orEmpty()
                val creatorName = resolvedCreators[cId.trim().lowercase()].orEmpty()
                val displayHost = row.host_name.takeIf { it.isNotBlank() } ?: creatorName
                CampusEventEntity(
                    id = row.id,
                    creatorId = cId,
                    title = row.title,
                    hostName = displayHost,
                    description = row.description,
                    locationName = row.location_name,
                    fullAddress = row.full_address.ifBlank { row.location_name },
                    latitude = row.latitude,
                    longitude = row.longitude,
                    startTimeIso = row.start_time,
                    startTimeEpochMs = parseIsoToEpochMs(row.start_time),
                    category = row.category,
                    priceLabel = row.price_label,
                    maxCapacity = row.max_capacity.coerceAtLeast(1),
                    attendeeCount = row.attendee_count.coerceAtLeast(0),
                    artworkKey = row.artwork_key.ifBlank { "event_thumb_founders" },
                    bannerUrl = row.banner_url,
                    syncStatus = SyncStatus.SYNCED.name,
                    syncId = null
                )
            }

            if (eventDao != null) {
                if (entities.isNotEmpty()) {
                    eventDao.insertEvents(entities)
                    eventDao.deleteGhostEvents(entities.map { it.id })
                } else {
                    eventDao.clearSyncedEvents()
                }
            } else {
                _memoryEvents.value = entities.map { it.toDomain() }
            }

            syncRsvps(currentUserId)
            System.currentTimeMillis()
        }
    }

    suspend fun syncRsvps(currentUserId: String? = null): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val rsvps: List<CampusEventRsvpRow> = supabase.from("campus_event_rsvps")
                .select(columns = Columns.ALL) {
                    order("created_at_epoch_ms", order = Order.DESCENDING)
                    limit(500)
                }
                .decodeList()

            val resolverMap = if (rsvps.isNotEmpty()) {
                runCatching { profileNameResolver.resolve(rsvps.map { it.user_id }) }
                    .getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            if (!currentUserId.isNullOrBlank()) {
                _registeredEventIds.value = rsvps
                    .filter { it.user_id.equals(currentUserId.trim(), ignoreCase = true) }
                    .map { it.event_id }
                    .toSet()
            }

            val previewsByEvent = rsvps.groupBy { it.event_id }
                .mapValues { (_, list) ->
                    list.mapNotNull { row ->
                        resolverMap[row.user_id.trim().lowercase()]?.takeIf(String::isRealProfileDisplayName)
                            ?: row.display_name.takeIf(String::isRealProfileDisplayName)
                    }.distinct().take(3)
                }
            _attendeePreviews.value = previewsByEvent
        }
    }

    suspend fun toggleRsvp(
        eventId: String,
        currentUserName: String? = null
    ): Result<Pair<Boolean, Int>> = withContext(ioDispatcher) {
        runCatching {
            val obj = supabase.postgrest.rpc(
                "toggle_campus_event_rsvp",
                ToggleEventRsvpParams(p_event_id = eventId)
            ).decodeAs<JsonObject>()

            val isRegistered = (obj["is_registered"] as? JsonPrimitive)?.booleanOrNull ?: false
            val attendeeCount = (obj["attendee_count"] as? JsonPrimitive)?.intOrNull ?: 0

            _registeredEventIds.update { current ->
                if (isRegistered) current + eventId else current - eventId
            }
            eventDao?.updateAttendeeCount(eventId, attendeeCount)
            if (!currentUserName.isNullOrBlank() && currentUserName.isRealProfileDisplayName()) {
                _attendeePreviews.update { map ->
                    val existing = map[eventId].orEmpty()
                    val next = if (isRegistered) {
                        (listOf(currentUserName) + existing).distinct().take(3)
                    } else {
                        existing.filterNot { it.equals(currentUserName, ignoreCase = true) }
                    }
                    map + (eventId to next)
                }
            }
            if (eventDao == null) {
                _memoryEvents.update { list ->
                    list.map {
                        if (it.id == eventId) it.copy(attendeeCount = attendeeCount, isUserRegistered = isRegistered) else it
                    }
                }
            }
            isRegistered to attendeeCount
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            supabase.from("campus_events").delete {
                filter { eq("id", eventId) }
            }
            eventDao?.deleteEventById(eventId)
            _registeredEventIds.update { it - eventId }
            _attendeePreviews.update { it - eventId }
            if (eventDao == null) {
                _memoryEvents.update { list -> list.filterNot { it.id == eventId } }
            }
            Unit
        }
    }

    suspend fun applyRealtimeChange(
        change: SupabaseRealtimeChange,
        currentUserId: String? = null
    ): Unit = withContext(ioDispatcher) {
        when (change.table.lowercase()) {
            "campus_events" -> handleEventChange(change)
            "campus_event_rsvps" -> {
                syncRsvps(currentUserId)
                Unit
            }
            else -> Unit
        }
    }

    private suspend fun handleEventChange(change: SupabaseRealtimeChange) {
        if (change.eventType == RealtimeEventType.DELETE) {
            val deletedId = extractJsonField(change.oldRecordJson, "id") ?: return
            eventDao?.deleteEventById(deletedId)
            if (eventDao == null) {
                _memoryEvents.update { list -> list.filterNot { it.id == deletedId } }
            }
            return
        }
        val jsonStr = change.recordJson ?: return
        val row = runCatching {
            eventsRealtimeJson.decodeFromString<CampusEventRow>(jsonStr)
        }.getOrNull() ?: return
        if (row.id.isBlank()) return

        val creatorId = row.creator_id.orEmpty()
        val resolvedName = if (row.host_name.isBlank() && creatorId.isNotBlank()) {
            runCatching { profileNameResolver.resolve(listOf(creatorId))[creatorId.lowercase()] }.getOrNull().orEmpty()
        } else {
            row.host_name
        }
        val entity = CampusEventEntity(
            id = row.id,
            creatorId = creatorId,
            title = row.title,
            hostName = resolvedName,
            description = row.description,
            locationName = row.location_name,
            fullAddress = row.full_address.ifBlank { row.location_name },
            latitude = row.latitude,
            longitude = row.longitude,
            startTimeIso = row.start_time,
            startTimeEpochMs = parseIsoToEpochMs(row.start_time),
            category = row.category,
            priceLabel = row.price_label,
            maxCapacity = row.max_capacity.coerceAtLeast(1),
            attendeeCount = row.attendee_count.coerceAtLeast(0),
            artworkKey = row.artwork_key.ifBlank { "event_thumb_founders" },
            bannerUrl = row.banner_url
        )
        eventDao?.insertEvent(entity)
        if (eventDao == null) {
            val domain = entity.toDomain()
            _memoryEvents.update { list -> list.filterNot { it.id == row.id } + domain }
        }
    }

    private fun extractJsonField(jsonStr: String?, key: String): String? {
        if (jsonStr.isNullOrBlank()) return null
        val obj = runCatching {
            eventsRealtimeJson.parseToJsonElement(jsonStr) as? JsonObject
        }.getOrNull() ?: return null
        return (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun CampusEventEntity.toDomain() = CampusEventRecord(
        id = id,
        creatorId = creatorId,
        title = title,
        hostName = hostName,
        description = description,
        locationName = locationName,
        fullAddress = fullAddress,
        latitude = latitude,
        longitude = longitude,
        startTimeIso = startTimeIso,
        startTimeEpochMs = startTimeEpochMs,
        category = category,
        priceLabel = priceLabel,
        maxCapacity = maxCapacity,
        attendeeCount = attendeeCount,
        artworkKey = artworkKey,
        bannerUrl = bannerUrl
    )

    companion object {
        fun parseIsoToEpochMs(iso: String?): Long {
            if (iso.isNullOrBlank()) return System.currentTimeMillis()
            return runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
                ?: System.currentTimeMillis()
        }
    }
}
