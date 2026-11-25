package com.campus.network.core.data

import com.campus.network.core.model.LiveStream
import com.campus.network.core.network.SupabaseRealtimeClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * App-scoped coordinator that maintains live Supabase Realtime subscriptions for livestreams,
 * stream comments, and Live Room presence, with adaptive fallback polling while offline/reconnecting.
 */
class LiveStreamRealtimeCoordinator(
    private val liveStreamRepository: LiveStreamRepository,
    private val realtimeClient: SupabaseRealtimeClient = SupabaseRealtimeClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val started = AtomicBoolean(false)
    private val userIdProviderRef = AtomicReference<() -> String?> { null }
    private val alertCallbackRef = AtomicReference<(LiveStream) -> Unit> {}

    private var collectorsJob: Job? = null
    private var reconcileJob: Job? = null

    val isRealtimeConnected: StateFlow<Boolean> = realtimeClient.isConnected

    private val _incomingLiveStreams = MutableSharedFlow<LiveStream>(extraBufferCapacity = 16)
    val incomingLiveStreams: SharedFlow<LiveStream> = _incomingLiveStreams.asSharedFlow()

    fun start(
        currentUserIdProvider: () -> String?,
        onOtherUserWentLive: (LiveStream) -> Unit = {}
    ) {
        userIdProviderRef.set(currentUserIdProvider)
        alertCallbackRef.set(onOtherUserWentLive)

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

    fun reconnectIfActive() {
        if (started.get()) {
            realtimeClient.reconnect()
        }
    }

    fun stop() {
        started.set(false)
        reconcileJob?.cancel()
        reconcileJob = null
        collectorsJob?.cancel()
        collectorsJob = null
        realtimeClient.stop()
    }

    suspend fun refreshNow(activeStreamId: String? = null) {
        runCatching { liveStreamRepository.syncRemoteLiveStreams(0L) }
        runCatching { liveStreamRepository.syncRoomPresence() }
        if (!activeStreamId.isNullOrBlank()) {
            runCatching { liveStreamRepository.syncRemoteCommentsForStream(activeStreamId) }
        }
    }

    private fun startCollectorsLocked() {
        collectorsJob?.cancel()
        collectorsJob = scope.launch {
            launch {
                realtimeClient.changes.collect { change ->
                    runCatching {
                        liveStreamRepository.applyRealtimeChange(
                            change = change,
                            currentUserId = userIdProviderRef.get().invoke()
                        )
                    }.onFailure {
                        if (it is CancellationException) throw it
                    }
                }
            }

            launch {
                var wasConnected = false
                realtimeClient.isConnected.collect { connected ->
                    if (connected && !wasConnected) {
                        runCatching { liveStreamRepository.syncRemoteLiveStreams(0L) }
                        runCatching { liveStreamRepository.syncRoomPresence() }
                    }
                    wasConnected = connected
                }
            }

            launch {
                liveStreamRepository.newLiveStreamAlerts.collect { stream ->
                    val currentId = userIdProviderRef.get().invoke()
                    if (!currentId.isNullOrBlank() &&
                        stream.broadcasterId.equals(currentId, ignoreCase = true)
                    ) {
                        return@collect
                    }
                    _incomingLiveStreams.emit(stream)
                    runCatching { alertCallbackRef.get().invoke(stream) }
                }
            }
        }
    }

    private fun startReconcileLoopIfNeeded() {
        if (reconcileJob?.isActive == true) return
        reconcileJob = scope.launch {
            runCatching { liveStreamRepository.syncRemoteLiveStreams(0L) }
            runCatching { liveStreamRepository.syncRoomPresence() }

            while (isActive) {
                val intervalMs = if (realtimeClient.isConnected.value) {
                    CONNECTED_RECONCILE_MS
                } else {
                    FALLBACK_POLL_MS
                }
                delay(intervalMs)
                runCatching { liveStreamRepository.syncRemoteLiveStreams(0L) }
                runCatching { liveStreamRepository.syncRoomPresence() }
            }
        }
    }

    private companion object {
        val SUBSCRIBED_TABLES = listOf(
            "campus_live_streams",
            "campus_stream_comments",
            "campus_live_room_presence"
        )
        const val FALLBACK_POLL_MS = 8_000L
        const val CONNECTED_RECONCILE_MS = 30_000L
    }
}
