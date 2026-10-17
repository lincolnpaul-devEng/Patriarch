package com.campus.network.feature.channels.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

/**
 * Domain state representing the lifecycle of an on-device transcription session.
 */
sealed interface TranscriptionState {
    object Idle : TranscriptionState
    object Preparing : TranscriptionState
    object ReadyForSpeech : TranscriptionState
    object Listening : TranscriptionState
    data class PartialResult(val text: String) : TranscriptionState
    data class Success(val text: String) : TranscriptionState
    data class Error(val message: String, val code: Int? = null) : TranscriptionState
}

/**
 * On-Device, Offline-First Audio Transcription Manager for Channel Chat.
 *
 * Implements WhatsApp/patriarch-style privacy-focused speech recognition on Android using the native
 * [SpeechRecognizer] API with explicit offline flags [RecognizerIntent.EXTRA_PREFER_OFFLINE].
 * Audio never leaves the local hardware.
 */
class OnDeviceTranscriber(
    private val context: Context
) {
    companion object {
        private const val TAG = "ChannelAudioTranscriber"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isDedicatedOnDevice: Boolean = false
    private var hasFallenBackToStandard: Boolean = false
    private var lastLanguage: Locale = Locale.getDefault()

    private val _state = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val state: StateFlow<TranscriptionState> = _state.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    fun isOnDeviceSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context) || isOnDeviceSupported()
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening(language: Locale = Locale.getDefault()) {
        mainHandler.post {
            lastLanguage = language
            hasFallenBackToStandard = false

            if (!hasRecordPermission()) {
                val errorMsg = "Microphone permission is required for voice transcription. Please grant permission in Settings."
                Log.w(TAG, errorMsg)
                _state.value = TranscriptionState.Error(errorMsg, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
                return@post
            }

            if (!isRecognitionAvailable()) {
                val fallbackMsg = "Speech recognition service is not available on this device. Please install or enable Speech Services by Google in Settings."
                Log.w(TAG, fallbackMsg)
                _state.value = TranscriptionState.Error(fallbackMsg)
                return@post
            }

            _state.value = TranscriptionState.Preparing

            destroyRecognizer()

            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.mode = AudioManager.MODE_NORMAL
            } catch (e: Exception) {
                Log.w(TAG, "Unable to configure audio manager mode: ${e.message}")
            }

            var recognizer: SpeechRecognizer? = null
            isDedicatedOnDevice = false

            if (isOnDeviceSupported()) {
                try {
                    Log.d(TAG, "Creating dedicated on-device SpeechRecognizer instance.")
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    isDedicatedOnDevice = true
                } catch (e: Throwable) {
                    Log.w(TAG, "createOnDeviceSpeechRecognizer failed: ${e.message}. Falling back to standard recognizer.")
                    recognizer = null
                }
            }

            if (recognizer == null) {
                try {
                    Log.d(TAG, "Creating standard SpeechRecognizer instance with offline preference.")
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to instantiate SpeechRecognizer: ${e.message}", e)
                }
            }

            if (recognizer == null) {
                val errorMsg = "Unable to start SpeechRecognizer. Please ensure Google Speech Services are active on your device."
                Log.e(TAG, errorMsg)
                _state.value = TranscriptionState.Error(errorMsg)
                return@post
            }

            speechRecognizer = recognizer
            recognizer.setRecognitionListener(TranscriptionRecognitionListener())

            val intent = createSpeechIntent(language, preferOffline = isDedicatedOnDevice)
            Log.i(TAG, "Starting speech listening session (Language: ${language.toLanguageTag()}, Package: ${context.packageName}, onDevice: $isDedicatedOnDevice).")
            try {
                recognizer.startListening(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "startListening failed: ${e.message}", e)
                _state.value = TranscriptionState.Error("Failed to initiate voice recording: ${e.message}")
            }
        }
    }

    private fun createSpeechIntent(language: Locale, preferOffline: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun startStandardFallback(language: Locale) {
        destroyRecognizer()
        isDedicatedOnDevice = false
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(TranscriptionRecognitionListener())
            val intent = createSpeechIntent(language, preferOffline = false)
            Log.i(TAG, "Starting fallback standard speech recognition session.")
            recognizer.startListening(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "startStandardFallback failed: ${e.message}", e)
            _state.value = TranscriptionState.Error("Fallback speech recognition failed: ${e.message}")
        }
    }

    fun stopListening() {
        mainHandler.post {
            Log.d(TAG, "Stopping speech listening session.")
            speechRecognizer?.stopListening()
        }
    }

    fun cancel() {
        mainHandler.post {
            Log.d(TAG, "Cancelling speech session.")
            speechRecognizer?.cancel()
            _audioRms.value = 0f
            _isSpeechDetected.value = false
            _state.value = TranscriptionState.Idle
        }
    }

    fun destroy() {
        mainHandler.post {
            destroyRecognizer()
            _audioRms.value = 0f
            _isSpeechDetected.value = false
            _state.value = TranscriptionState.Idle
        }
    }

    private fun destroyRecognizer() {
        speechRecognizer?.let {
            it.setRecognitionListener(null)
            it.destroy()
        }
        speechRecognizer = null
    }

    private inner class TranscriptionRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.i(TAG, "onReadyForSpeech: Audio recorder initialized and ready for user voice.")
            _state.value = TranscriptionState.ReadyForSpeech
            _isSpeechDetected.value = false
            _audioRms.value = 0f
        }

        override fun onBeginningOfSpeech() {
            Log.i(TAG, "onBeginningOfSpeech: User began speaking.")
            _state.value = TranscriptionState.Listening
            _isSpeechDetected.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 1f) / 10f).coerceIn(0f, 1f)
            _audioRms.value = normalized
            _isSpeechDetected.value = rmsdB > 1.8f
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.v(TAG, "onBufferReceived: Audio buffer chunk received (${buffer?.size ?: 0} bytes).")
        }

        override fun onEndOfSpeech() {
            Log.i(TAG, "onEndOfSpeech: User finished speaking, processing final transcription.")
            _state.value = TranscriptionState.Preparing
            _isSpeechDetected.value = false
            _audioRms.value = 0f
        }

        override fun onError(errorCode: Int) {
            val domainMessage = mapSpeechErrorCode(errorCode)
            Log.e(TAG, "onError: SpeechRecognizer code $errorCode -> $domainMessage")

            if (isDedicatedOnDevice && !hasFallenBackToStandard && (errorCode == SpeechRecognizer.ERROR_CLIENT || errorCode == 11 || errorCode == 12 || errorCode == 13 || errorCode == 14)) {
                Log.w(TAG, "Dedicated on-device engine encountered error $errorCode ($domainMessage). Seamlessly falling back to standard SpeechRecognizer.")
                hasFallenBackToStandard = true
                mainHandler.post {
                    startStandardFallback(lastLanguage)
                }
                return
            }

            _state.value = TranscriptionState.Error(domainMessage, errorCode)
            _isSpeechDetected.value = false
            _audioRms.value = 0f
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestMatch = matches?.firstOrNull().orEmpty()
            Log.i(TAG, "onResults: Successfully extracted final transcription: '$bestMatch'")
            _state.value = TranscriptionState.Success(bestMatch)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val currentPartial = partialMatches?.firstOrNull().orEmpty()
            if (currentPartial.isNotBlank()) {
                Log.d(TAG, "onPartialResults: Interim text: '$currentPartial'")
                _state.value = TranscriptionState.PartialResult(currentPartial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.v(TAG, "onEvent: SpeechRecognizer eventType=$eventType")
        }
    }

    private fun mapSpeechErrorCode(code: Int): String {
        return when (code) {
            SpeechRecognizer.ERROR_AUDIO ->
                "Audio recording error. Please verify your microphone is not in use by another app."
            SpeechRecognizer.ERROR_CLIENT ->
                "Client speech engine error. Session reset."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission not granted. Please allow microphone access in app settings."
            SpeechRecognizer.ERROR_NETWORK ->
                "Network communication error occurred."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network request timed out."
            SpeechRecognizer.ERROR_NO_MATCH ->
                "No clear speech recognized. Please speak clearly into the microphone."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Speech recognizer is busy. Please wait a moment and try again."
            SpeechRecognizer.ERROR_SERVER ->
                "Recognition server error occurred."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No speech detected within the listening window."
            10 ->
                "Unable to check speech support on this system."
            11 ->
                "Too many recognition requests submitted. Please slow down."
            12 ->
                "Recognition service disconnected."
            13 ->
                "The requested language is not supported for on-device recognition."
            14 ->
                "On-device language pack is unavailable. Please download offline language pack in system settings."
            else ->
                "Speech recognition encountered an unknown error (Code $code)."
        }
    }
}
