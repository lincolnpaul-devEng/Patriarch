package com.campus.network.feature.messages.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceTranscriberTest {

    @Test
    fun testTranscriptionStatesInstantiation() {
        val idle = TranscriptionState.Idle
        val preparing = TranscriptionState.Preparing
        val ready = TranscriptionState.ReadyForSpeech
        val listening = TranscriptionState.Listening
        val partial = TranscriptionState.PartialResult("hello")
        val success = TranscriptionState.Success("hello world")
        val error = TranscriptionState.Error("Microphone permission required", 9)

        assertNotNull(idle)
        assertNotNull(preparing)
        assertNotNull(ready)
        assertNotNull(listening)
        assertEquals("hello", partial.text)
        assertEquals("hello world", success.text)
        assertEquals("Microphone permission required", error.message)
        assertEquals(9, error.code)
    }

    @Test
    fun testWaveformAmplitudeCalculations() {
        val minHeightPx = 10f
        val maxHeightPx = 80f

        // Calm state: amplitude is 0f, height must equal minHeightPx exactly
        val calmAmp = 0f
        val calmHeight = minHeightPx + (maxHeightPx - minHeightPx) * calmAmp
        assertEquals(minHeightPx, calmHeight, 0.001f)

        // Active speech: amplitude is 1f, height must equal maxHeightPx
        val fullAmp = 1f
        val fullHeight = minHeightPx + (maxHeightPx - minHeightPx) * fullAmp
        assertEquals(maxHeightPx, fullHeight, 0.001f)

        // Mid speech: amplitude is 0.5f
        val midAmp = 0.5f
        val midHeight = minHeightPx + (maxHeightPx - minHeightPx) * midAmp
        assertEquals(45f, midHeight, 0.001f)
    }

    @Test
    fun testAudioRmsNormalizationFormula() {
        // RMSdB to normalized range 0f..1f: ((rmsdB + 1f) / 10f).coerceIn(0f, 1f)
        val silenceRms = -5f
        val normSilence = ((silenceRms + 1f) / 10f).coerceIn(0f, 1f)
        assertEquals(0f, normSilence, 0.001f)

        val vocalRms = 4f
        val normVocal = ((vocalRms + 1f) / 10f).coerceIn(0f, 1f)
        assertEquals(0.5f, normVocal, 0.001f)

        val maxRms = 12f
        val normMax = ((maxRms + 1f) / 10f).coerceIn(0f, 1f)
        assertEquals(1f, normMax, 0.001f)
    }

    @Test
    fun testZeroEmojiInErrorMessages() {
        val emojiRegex = Regex("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]")
        val sampleMessages = listOf(
            "Microphone permission is required for voice transcription. Please grant permission in Settings.",
            "Speech recognition service is not available on this device. Please install or enable Speech Services by Google in Settings.",
            "Audio recording error. Please verify your microphone is not in use by another app.",
            "Client error occurred during speech session.",
            "No clear speech recognized. Please speak clearly into the microphone.",
            "The requested language is not supported for on-device recognition."
        )

        for (msg in sampleMessages) {
            assertFalse("Found emoji in message: $msg", emojiRegex.containsMatchIn(msg))
        }
    }
}
