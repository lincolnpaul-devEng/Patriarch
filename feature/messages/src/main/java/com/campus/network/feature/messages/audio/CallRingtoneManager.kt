package com.campus.network.feature.messages.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Call ringback / incoming ringtone using ToneGenerator with proper audio routing.
 *
 * Outgoing ringback uses [AudioManager.STREAM_RING] (audible without an active Telecom
 * voice session). Incoming uses STREAM_RING + vibration.
 *
 * Always requests audio focus and sets [AudioManager.MODE_IN_COMMUNICATION] so tones
 * are not silently dropped on modern devices.
 */
class CallRingtoneManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var toneGenerator: ToneGenerator? = null
    private var toneLoopJob: Job? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var heldAudioFocus: Boolean = false

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Outgoing ringback while waiting for peer to accept. */
    @Synchronized
    fun startOutgoingRingback() {
        stop()
        if (!prepareAudioSession(forIncoming = false)) {
            Log.w(TAG, "Outgoing ringback: audio session prepare failed")
        }

        toneLoopJob = scope.launch(Dispatchers.Default) {
            try {
                // STREAM_RING is audible without an active Telecom voice call route.
                toneGenerator = ToneGenerator(AudioManager.STREAM_RING, 90)
                Log.i(TAG, "Outgoing ringback started")
                while (isActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1200)
                    delay(3200L)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Outgoing ringback failed", t)
            } finally {
                releaseToneGenerator()
            }
        }
    }

    /** Incoming ringtone + vibration while callee decides. */
    @Synchronized
    fun startIncomingRingtone() {
        stop()
        if (!prepareAudioSession(forIncoming = true)) {
            Log.w(TAG, "Incoming ringtone: audio session prepare failed")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationPattern = longArrayOf(0, 1000, 1000)
                vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Incoming vibration failed", t)
        }

        toneLoopJob = scope.launch(Dispatchers.Default) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_RING, ToneGenerator.MAX_VOLUME)
                Log.i(TAG, "Incoming ringtone started")
                while (isActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400)
                    delay(3000L)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Incoming ringtone failed", t)
            } finally {
                releaseToneGenerator()
            }
        }
    }

    @Synchronized
    fun stop() {
        toneLoopJob?.cancel()
        toneLoopJob = null
        releaseToneGenerator()
        try {
            vibrator?.cancel()
        } catch (t: Throwable) {
            Log.w(TAG, "Vibrator cancel failed", t)
        }
        releaseAudioSession()
    }

    private fun prepareAudioSession(forIncoming: Boolean): Boolean {
        return try {
            previousAudioMode = audioManager.mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            // Prefer loudspeaker for ring alert / ringback so users hear it.
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true

            val attrs = AudioAttributes.Builder()
                .setUsage(
                    if (forIncoming) AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                    else AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING
                )
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { /* no-op */ }
                    .build()
                audioFocusRequest = focusReq
                val result = audioManager.requestAudioFocus(focusReq)
                heldAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (!heldAudioFocus) {
                    Log.w(TAG, "Audio focus not granted (result=$result)")
                }
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                heldAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "prepareAudioSession failed", t)
            false
        }
    }

    private fun releaseAudioSession() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else if (heldAudioFocus) {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "abandonAudioFocus failed", t)
        } finally {
            audioFocusRequest = null
            heldAudioFocus = false
        }
        try {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = previousAudioMode
        } catch (t: Throwable) {
            Log.w(TAG, "restore audio mode failed", t)
        }
    }

    private fun releaseToneGenerator() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "ToneGenerator release failed", t)
        } finally {
            toneGenerator = null
        }
    }

    companion object {
        private const val TAG = "CallRingtoneManager"
    }
}
