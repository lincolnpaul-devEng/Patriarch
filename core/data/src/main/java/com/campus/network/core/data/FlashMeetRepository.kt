package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for Tab 1: Map (Flash-Meet and Campus Events Interface).
 * Enforces online-first remote persistence with Room local caching.
 */
class FlashMeetRepository(
    private val database: CampusDatabase,
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = database.flashMeetDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabaseClient)

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
        durationMinutes: Int
    ): FlashMeet = withContext(ioDispatcher) {
        val resolvedCreatorId = creatorUserId.trim()
        require(resolvedCreatorId.isNotBlank()) { "creatorUserId is required to publish a flash meet" }
        val resolvedCreatorName = profileNameResolver.resolve(listOf(resolvedCreatorId))[resolvedCreatorId.lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to publish a flash meet")
        val now = System.currentTimeMillis()
        val expiry = now + (durationMinutes * 60 * 1000L)
        val entity = FlashMeetEntity(
            id = "meet-${now}",
            title = title,
            description = description,
            categoryName = category.name,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            creatorName = resolvedCreatorName,
            creatorAvatar = null,
            createdAtEpochMs = now,
            expiresAtEpochMs = expiry,
            attendeeCount = 1,
            maxAttendees = 6
        )

        val remotePayload = JSONObject().apply {
            put("id", entity.id)
            put("title", entity.title)
            put("description", entity.description)
            put("category_name", entity.categoryName)
            put("latitude", entity.latitude)
            put("longitude", entity.longitude)
            put("location_name", entity.locationName)
            put("creator_name", entity.creatorName)
            put("creator_id", resolvedCreatorId)
            put("created_at_epoch_ms", entity.createdAtEpochMs)
            put("expires_at_epoch_ms", entity.expiresAtEpochMs)
            put("attendee_count", entity.attendeeCount)
            put("max_attendees", entity.maxAttendees)
        }.toString()

        val remoteResult = supabaseClient.upsertRecord("campus_flash_meets", remotePayload, onConflict = "id")
        if (remoteResult.isFailure) {
            throw remoteResult.exceptionOrNull() ?: Exception("Failed to publish flash meet to cloud")
        }

        dao.insertMeets(listOf(entity))
        dao.purgeExpired(now)

        return@withContext entity.toDomain()
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
        val payload = JSONObject().apply {
            put("id", id)
            put("creator_id", creatorId)
            put("title", title)
            put("description", description)
            put("location_name", locationName)
            put("latitude", latitude)
            put("longitude", longitude)
            put("start_time", startTimeIso)
            put("category", category)
            put("attendee_count", attendeeCount)
        }.toString()
        supabaseClient.upsertRecord("campus_events", payload, onConflict = "id").map { }
    }

    /**
     * Synchronizes live flash meets from Supabase to local Room database.
     */
    suspend fun syncRemoteFlashMeets(): Result<Unit> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val queryResult = supabaseClient.queryTable(
            "campus_flash_meets?expires_at_epoch_ms=gt.$now&order=created_at_epoch_ms.desc&limit=30"
        )
        if (queryResult.isFailure) {
            return@withContext Result.failure(queryResult.exceptionOrNull() ?: Exception("Failed to fetch flash meets"))
        }
        val raw = queryResult.getOrNull() ?: "[]"
        val array = JSONArray(raw)
        val creatorNames = try {
            profileNameResolver.resolve((0 until array.length()).map { array.getJSONObject(it).optString("creator_id") })
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        }
        val entities = mutableListOf<FlashMeetEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            entities.add(
                FlashMeetEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    categoryName = obj.optString("category_name", MeetCategory.STUDY_JAM.name),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    locationName = obj.optString("location_name", ""),
                    creatorName = creatorNames[obj.optString("creator_id").lowercase()].orEmpty(),
                    creatorAvatar = obj.optString("creator_avatar", "").ifBlank { null },
                    createdAtEpochMs = obj.optLong("created_at_epoch_ms", now),
                    expiresAtEpochMs = obj.optLong("expires_at_epoch_ms", now + 3600000L),
                    attendeeCount = obj.optInt("attendee_count", 1),
                    maxAttendees = obj.optInt("max_attendees", 6)
                )
            )
        }
        if (entities.isNotEmpty()) {
            dao.insertMeets(entities)
        }
        Result.success(Unit)
    }
}
