package com.campus.network.core.data

import com.campus.network.core.network.SupabaseRealtimeClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * App-scoped coordinator maintaining live Supabase Realtime subscriptions for
 * friend live locations, flash meets, campus events, and campus places, with adaptive reconcile.
 */
class MapRealtimeCoordinator(
    private val liveLocationRepository: LiveLocationRepository,
    private val flashMeetRepository: FlashMeetRepository,
    private val campusEventsRepository: CampusEventsRepository,
    private val placesRepository: PlacesRepository,
    private val realtimeClient: SupabaseRealtimeClient = SupabaseRealtimeClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val started = AtomicBoolean(false)
    private val userIdProviderRef = AtomicReference<() -> String?> { null }
    private val friendIdsProviderRef = AtomicReference<() -> Set<String>> { emptySet() }

    private var collectorsJob: Job? = null
    private var reconcileJob: Job? = null

    val isRealtimeConnected: StateFlow<Boolean> = realtimeClient.isConnected

    fun start(
        currentUserIdProvider: () -> String?,
        friendIdsProvider: () -> Set<String> = { emptySet() }
    ) {
        userIdProviderRef.set(currentUserIdProvider)
        friendIdsProviderRef.set(friendIdsProvider)

        if (started.compareAndSet(false, true)) {
            startCollectorsLocked()
        }
        realtimeClient.start(SUBSCRIBED_TABLES)
        startReconcileLoopIfNeeded()
    }

    fun resumeIfStarted() {
        if (!started.get()) return
        realtimeClient.start(SUBSCRIBED_TABLES)
        startReconcileLoopIfNeeded()
    }

    fun pause() {
        reconcileJob?.cancel()
        reconcileJob = null
        realtimeClient.stop()
    }

    fun stop() {
        started.set(false)
        reconcileJob?.cancel()
        reconcileJob = null
        collectorsJob?.cancel()
        collectorsJob = null
        realtimeClient.stop()
    }

    suspend fun refreshAll() {
        val currentUserId = userIdProviderRef.get().invoke()
        val friendIds = friendIdsProviderRef.get().invoke()
        runCatching { liveLocationRepository.syncVisibleFriendLocations(currentUserId) }
        runCatching { flashMeetRepository.syncRemoteFlashMeets(0L) }
        runCatching { flashMeetRepository.syncJoinedMeetIds(currentUserId) }
        runCatching { campusEventsRepository.syncRemoteEvents(currentUserId) }
        runCatching { placesRepository.syncPlaces(currentUserId, friendIds) }
    }

    private fun startCollectorsLocked() {
        collectorsJob?.cancel()
        collectorsJob = scope.launch {
            launch {
                realtimeClient.changes.collect { change ->
                    val currentUserId = userIdProviderRef.get().invoke()
                    val friendIds = friendIdsProviderRef.get().invoke()
                    runCatching {
                        when (change.table.lowercase()) {
                            "campus_live_locations" ->
                                liveLocationRepository.applyRealtimeChange(change, currentUserId)
                            "campus_flash_meets",
                            "campus_flash_meet_attendees" ->
                                flashMeetRepository.applyRealtimeChange(change, currentUserId)
                            "campus_events",
                            "campus_event_rsvps" ->
                                campusEventsRepository.applyRealtimeChange(change, currentUserId)
                            "campus_places",
                            "campus_place_bookmarks",
                            "campus_place_reviews" ->
                                placesRepository.applyRealtimeChange(change, currentUserId, friendIds)
                            else -> Unit
                        }
                    }.onFailure {
                        if (it is CancellationException) throw it
                    }
                }
            }

            launch {
                var wasConnected = false
                realtimeClient.isConnected.collect { connected ->
                    if (connected && !wasConnected) {
                        refreshAll()
                    }
                    wasConnected = connected
                }
            }
        }
    }

    private fun startReconcileLoopIfNeeded() {
        if (reconcileJob?.isActive == true) return
        reconcileJob = scope.launch {
            refreshAll()

            while (isActive) {
                val intervalMs = if (realtimeClient.isConnected.value) {
                    CONNECTED_RECONCILE_MS
                } else {
                    FALLBACK_POLL_MS
                }
                delay(intervalMs)
                liveLocationRepository.pruneStaleLocations()
                refreshAll()
            }
        }
    }

    private companion object {
        val SUBSCRIBED_TABLES = listOf(
            "campus_live_locations",
            "campus_flash_meets",
            "campus_flash_meet_attendees",
            "campus_events",
            "campus_event_rsvps",
            "campus_places",
            "campus_place_bookmarks",
            "campus_place_reviews"
        )
        const val FALLBACK_POLL_MS = 12_000L
        const val CONNECTED_RECONCILE_MS = 45_000L
    }
}
