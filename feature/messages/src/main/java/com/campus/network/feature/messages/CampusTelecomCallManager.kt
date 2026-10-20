package com.campus.network.feature.messages

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.launch

class CampusTelecomCallManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val callsManager = CallsManager(context.applicationContext)
    private var activeCallJob: Job? = null
    private var activeCallScope: CallControlScope? = null
    private var telecomRegistered: Boolean = false

    private val _currentEndpoint = MutableStateFlow<CallEndpointCompat?>(null)
    val currentEndpoint: StateFlow<CallEndpointCompat?> = _currentEndpoint.asStateFlow()

    private val _availableEndpoints = MutableStateFlow<List<CallEndpointCompat>>(emptyList())
    val availableEndpoints: StateFlow<List<CallEndpointCompat>> = _availableEndpoints.asStateFlow()

    private val _isHeldByCarrier = MutableStateFlow(false)
    val isHeldByCarrier: StateFlow<Boolean> = _isHeldByCarrier.asStateFlow()

    init {
        try {
            callsManager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
            telecomRegistered = true
            Log.i(TAG, "Registered with Telecom (CAPABILITY_BASELINE)")
        } catch (t: Throwable) {
            telecomRegistered = false
            Log.e(TAG, "Telecom registration failed — calls continue without system UI", t)
        }
    }

    fun startOutgoingCall(
        peerName: String,
        peerHandle: String,
        isVideo: Boolean,
        onDisconnectBySystem: () -> Unit
    ) {
        endCall()
        _isHeldByCarrier.value = false
        if (!telecomRegistered) {
            Log.w(TAG, "Skipping addCall — Telecom not registered")
            return
        }

        val attributes = CallAttributesCompat(
            displayName = peerName,
            address = Uri.parse("campus:$peerHandle"),
            direction = CallAttributesCompat.DIRECTION_OUTGOING,
            callType = if (isVideo) CallAttributesCompat.CALL_TYPE_VIDEO_CALL else CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            callCapabilities = CallAttributesCompat.SUPPORTS_SET_INACTIVE
        )

        activeCallJob = scope.launch(Dispatchers.Default) {
            try {
                callsManager.addCall(
                    attributes,
                    onAnswer = { _ -> },
                    onDisconnect = { _ ->
                        _isHeldByCarrier.value = false
                        onDisconnectBySystem()
                    },
                    onSetActive = {
                        _isHeldByCarrier.value = false
                    },
                    onSetInactive = {
                        _isHeldByCarrier.value = true
                    }
                ) {
                    activeCallScope = this
                    launch {
                        currentCallEndpoint.collect { endpoint ->
                            _currentEndpoint.value = endpoint
                        }
                    }
                    launch {
                        this@CampusTelecomCallManager.availableEndpoints.collect { endpoints ->
                            _availableEndpoints.value = endpoints
                        }
                    }
                }
                Log.i(TAG, "Outgoing Telecom call added for $peerName")
            } catch (t: Throwable) {
                Log.e(TAG, "addCall failed for $peerName", t)
            }
        }
    }

    fun setCallConnected() {
        scope.launch {
            try {
                activeCallScope?.setActive()
            } catch (t: Throwable) {
                Log.e(TAG, "setActive failed", t)
            }
        }
    }

    fun setSpeakerOn(isSpeakerOn: Boolean) {
        scope.launch {
            try {
                val callScope = activeCallScope ?: return@launch
                val endpoints = _availableEndpoints.value
                val targetType = if (isSpeakerOn) {
                    CallEndpointCompat.TYPE_SPEAKER
                } else {
                    CallEndpointCompat.TYPE_EARPIECE
                }
                val targetEndpoint = endpoints.find { it.type == targetType }
                    ?: endpoints.find { it.type == CallEndpointCompat.TYPE_STREAMING }
                if (targetEndpoint != null) {
                    callScope.requestEndpointChange(targetEndpoint)
                } else {
                    Log.w(TAG, "No endpoint for speaker=$isSpeakerOn (available=${endpoints.size})")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "setSpeakerOn failed", t)
            }
        }
    }

    fun endCall() {
        activeCallJob?.cancel()
        activeCallJob = null
        val callScope = activeCallScope
        activeCallScope = null
        _isHeldByCarrier.value = false
        if (callScope != null) {
            scope.launch {
                try {
                    callScope.disconnect(DisconnectCause(DisconnectCause.LOCAL))
                } catch (t: Throwable) {
                    Log.e(TAG, "disconnect failed", t)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CampusTelecomCall"
    }
}
