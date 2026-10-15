package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for Tab 1: Map (Flash-Meet Interface).
 * Enforces local-first optimistic rendering: draws instantly from Room DB before remote fetch.
 */
class FlashMeetRepository(
    private val database: CampusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = database.flashMeetDao()

    /**
     * Emits live meets continuously on the IO thread.
     */
    fun getActiveFlashMeets(): Flow<List<FlashMeet>> {
        return dao.getLiveMeets(System.currentTimeMillis())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Creates an optimistic flash meet locally and then queues background sync.
     */
    suspend fun createFlashMeet(
        title: String,
        description: String,
        category: MeetCategory,
        latitude: Double,
        longitude: Double,
        locationName: String,
        creatorName: String,
        durationMinutes: Int
    ): FlashMeet = withContext(ioDispatcher) {
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
            creatorName = creatorName,
            creatorAvatar = null,
            createdAtEpochMs = now,
            expiresAtEpochMs = expiry,
            attendeeCount = 1,
            maxAttendees = 6
        )

        // Optimistic local draw
        dao.insertMeets(listOf(entity))
        dao.purgeExpired(now)

        return@withContext entity.toDomain()
    }
}
