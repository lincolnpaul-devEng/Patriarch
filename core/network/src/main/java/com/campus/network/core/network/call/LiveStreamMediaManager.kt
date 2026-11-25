package com.campus.network.core.network.call

import android.content.Context
import android.util.Log
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CameraDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class LiveMediaRole {
    NONE,
    BROADCASTER,
    VIEWER
}

enum class LiveMediaConnectionState {
    IDLE,
    CONNECTING,
    LIVE,
    FAILED
}

data class LiveMediaState(
    val streamId: String? = null,
    val role: LiveMediaRole = LiveMediaRole.NONE,
    val connectionState: LiveMediaConnectionState = LiveMediaConnectionState.IDLE,
    val activeCall: Call? = null,
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isRecording: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Coordinates live broadcast publishing and viewer playback over Stream Video WebRTC SFU.
 */
class LiveStreamMediaManager(
    private val appContext: Context,
    private val apiKey: String,
    private val tokenFetcher: suspend (userId: String) -> Result<String>,
    private val displayNameResolver: suspend (userId: String) -> String = { it }
) {
    val isConfigured: Boolean = apiKey.isNotBlank()

    private val mutex = Mutex()
    private val _state = MutableStateFlow(LiveMediaState())
    val state: StateFlow<LiveMediaState> = _state.asStateFlow()

    suspend fun startBroadcast(
        streamId: String,
        localUserId: String,
        displayName: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
        useFrontCamera: Boolean
    ): Result<Call> = mutex.withLock {
        if (!isConfigured) {
            val error = IllegalStateException("STREAM_API_KEY is not configured")
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.BROADCASTER,
                connectionState = LiveMediaConnectionState.FAILED,
                errorMessage = error.message
            )
            return@withLock Result.failure(error)
        }
        if (localUserId.isBlank()) {
            val error = IllegalArgumentException("Sign in before broadcasting")
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.BROADCASTER,
                connectionState = LiveMediaConnectionState.FAILED,
                errorMessage = error.message
            )
            return@withLock Result.failure(error)
        }

        val current = _state.value
        val existingCall = current.activeCall
        if (current.streamId == streamId &&
            current.role == LiveMediaRole.BROADCASTER &&
            current.connectionState == LiveMediaConnectionState.LIVE &&
            existingCall != null
        ) {
            applyCameraDirection(existingCall, useFrontCamera)
            existingCall.camera.setEnabled(cameraEnabled)
            existingCall.microphone.setEnabled(micEnabled)
            existingCall.speaker.setEnabled(true)
            _state.update {
                it.copy(
                    isMicEnabled = micEnabled,
                    isCameraEnabled = cameraEnabled,
                    isFrontCamera = useFrontCamera,
                    errorMessage = null
                )
            }
            return@withLock Result.success(existingCall)
        }

        leaveActiveCallLocked()
        _state.value = LiveMediaState(
            streamId = streamId,
            role = LiveMediaRole.BROADCASTER,
            connectionState = LiveMediaConnectionState.CONNECTING,
            isMicEnabled = micEnabled,
            isCameraEnabled = cameraEnabled,
            isFrontCamera = useFrontCamera
        )

        return@withLock runCatching {
            val client = StreamVideoClientHost.ensureClient(
                appContext = appContext,
                apiKey = apiKey,
                userId = localUserId,
                tokenFetcher = tokenFetcher,
                displayNameResolver = { id ->
                    displayName.ifBlank { displayNameResolver(id) }
                }
            )
            val callId = StreamVideoCallMediaProvider.sanitizeCallId(streamId)
            val call = client.call(CALL_TYPE, callId)
            val joinResult = call.join(create = true)
            if (joinResult.isFailure) {
                throw joinResult.errorOrNull()?.let { Exception(it.message) }
                    ?: IllegalStateException("Unable to join live broadcast room")
            }
            applyCameraDirection(call, useFrontCamera)
            call.camera.setEnabled(cameraEnabled)
            call.microphone.setEnabled(micEnabled)
            call.speaker.setEnabled(true)
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.BROADCASTER,
                connectionState = LiveMediaConnectionState.LIVE,
                activeCall = call,
                isMicEnabled = micEnabled,
                isCameraEnabled = cameraEnabled,
                isFrontCamera = useFrontCamera
            )
            Log.i(TAG, "Started live broadcast callId=$callId")
            call
        }.onFailure { err ->
            Log.e(TAG, "Failed to start broadcast for $streamId", err)
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.BROADCASTER,
                connectionState = LiveMediaConnectionState.FAILED,
                errorMessage = err.message ?: "Live broadcast connection failed"
            )
        }
    }

    suspend fun joinAsViewer(
        streamId: String,
        localUserId: String,
        displayName: String
    ): Result<Call> = mutex.withLock {
        if (!isConfigured) {
            val error = IllegalStateException("STREAM_API_KEY is not configured")
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.VIEWER,
                connectionState = LiveMediaConnectionState.FAILED,
                errorMessage = error.message
            )
            return@withLock Result.failure(error)
        }
        if (localUserId.isBlank()) {
            val error = IllegalArgumentException("Sign in to watch live streams")
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.VIEWER,
                connectionState = LiveMediaConnectionState.FAILED,
                errorMessage = error.message
            )
            return@withLock Result.failure(error)
        }

        val current = _state.value
        val existingCall = current.activeCall
        if (current.streamId == streamId &&
            current.connectionState == LiveMediaConnectionState.LIVE &&
            existingCall != null
        ) {
            return@withLock Result.success(existingCall)
        }

        leaveActiveCallLocked()
        _state.value = LiveMediaState(
            streamId = streamId,
            role = LiveMediaRole.VIEWER,
            connectionState = LiveMediaConnectionState.CONNECTING
        )

        return@withLock runCatching {
            val client = StreamVideoClientHost.ensureClient(
                appContext = appContext,
                apiKey = apiKey,
                userId = localUserId,
                tokenFetcher = tokenFetcher,
                displayNameResolver = { id ->
                    displayName.ifBlank { displayNameResolver(id) }
                }
            )
            val callId = StreamVideoCallMediaProvider.sanitizeCallId(streamId)
            val call = client.call(CALL_TYPE, callId)
            val joinResult = call.join(create = true)
            if (joinResult.isFailure) {
                throw joinResult.errorOrNull()?.let { Exception(it.message) }
                    ?: IllegalStateException("Unable to connect to live stream")
            }
            call.camera.setEnabled(false)
            call.microphone.setEnabled(false)
            call.speaker.setEnabled(true)
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.VIEWER,
                connectionState = LiveMediaConnectionState.LIVE,
                activeCall = call,
                isMicEnabled = false,
                isCameraEnabled = false
            )
            Log.i(TAG, "Joined live stream as viewer callId=$callId")
            call
        }.onFailure { err ->
            Log.e(TAG, "Failed to join stream $streamId as viewer", err)
            _state.value = LiveMediaState(
                streamId = streamId,
                role = LiveMediaRole.VIEWER,
                connectionState = LiveMediaConnectionState.FAILED,
                errorMessage = err.message ?: "Unable to connect to live video"
            )
        }
    }

    suspend fun setMicEnabled(enabled: Boolean) = mutex.withLock {
        _state.value.activeCall?.microphone?.setEnabled(enabled)
        _state.update { it.copy(isMicEnabled = enabled) }
    }

    suspend fun setCameraEnabled(enabled: Boolean) = mutex.withLock {
        _state.value.activeCall?.camera?.setEnabled(enabled)
        _state.update { it.copy(isCameraEnabled = enabled) }
    }

    suspend fun flipCamera() = mutex.withLock {
        val call = _state.value.activeCall ?: return@withLock
        val nextFront = !_state.value.isFrontCamera
        call.camera.flip()
        _state.update { it.copy(isFrontCamera = nextFront) }
    }

    suspend fun toggleRecording(): Result<Boolean> = mutex.withLock {
        val call = _state.value.activeCall
            ?: return@withLock Result.failure(IllegalStateException("Start a live broadcast to record"))
        val currentlyRecording = _state.value.isRecording
        return@withLock runCatching {
            if (currentlyRecording) {
                val res = call.stopRecording()
                if (res.isFailure) {
                    throw Exception(res.errorOrNull()?.message ?: "Unable to stop recording")
                }
                _state.update { it.copy(isRecording = false) }
                false
            } else {
                val res = call.startRecording()
                if (res.isFailure) {
                    throw Exception(res.errorOrNull()?.message ?: "Recording requires Stream call recording permission")
                }
                _state.update { it.copy(isRecording = true) }
                true
            }
        }
    }

    suspend fun leaveStream(streamId: String? = null) = mutex.withLock {
        val currentId = _state.value.streamId
        if (streamId != null && currentId != null && currentId != streamId) {
            return@withLock
        }
        leaveActiveCallLocked()
        _state.value = LiveMediaState()
    }

    suspend fun endBroadcast(streamId: String) = mutex.withLock {
        val current = _state.value
        val call = current.activeCall
        if (call != null && (current.streamId == null || current.streamId == streamId)) {
            runCatching { call.end() }
            runCatching { call.leave() }
            _state.value = LiveMediaState()
        }
    }

    private fun applyCameraDirection(call: Call, useFrontCamera: Boolean) {
        val target = if (useFrontCamera) CameraDirection.Front else CameraDirection.Back
        runCatching { call.camera.setDirection(target) }
    }

    private fun leaveActiveCallLocked() {
        val call = _state.value.activeCall ?: return
        runCatching { call.camera.setEnabled(false) }
        runCatching { call.microphone.setEnabled(false) }
        runCatching { call.leave() }
    }

    private companion object {
        const val TAG = "LiveStreamMedia"
        const val CALL_TYPE = "default"
    }
}

object LiveStreamMediaRegistry {
    @Volatile
    private var manager: LiveStreamMediaManager? = null

    fun install(mediaManager: LiveStreamMediaManager?) {
        manager = mediaManager
    }

    fun resolve(): LiveStreamMediaManager? = manager
}
