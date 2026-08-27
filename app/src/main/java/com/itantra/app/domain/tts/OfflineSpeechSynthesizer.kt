package com.itantra.app.domain.tts

import com.itantra.app.domain.communication.PriorityLevel

/**
 * Audio data payload containing PCM / WAV audio buffer synthesized on-device.
 */
data class AudioData(
    val pcmSamples: ByteArray,
    val sampleRate: Int = 16000,
    val durationMs: Long = 0L
)

/**
 * Abstraction for on-device offline speech synthesizer (TTS).
 * Synthesizes and plays back recognized text using on-device models.
 */
interface OfflineSpeechSynthesizer {
    /**
     * Synthesize text into speech and immediately play through device speaker.
     * @param text Sentence to speak
     * @param language ISO language code
     * @param priority Priority level (EMERGENCY overrides ongoing speech and plays at max volume)
     */
    suspend fun synthesizeAndPlay(
        text: String,
        language: String,
        priority: PriorityLevel = PriorityLevel.NORMAL
    )

    /**
     * Synthesize text into raw PCM AudioData without immediate playback.
     */
    suspend fun synthesize(
        text: String,
        language: String
    ): AudioData

    /**
     * Stop active audio playback.
     */
    suspend fun stop()

    /**
     * Check if offline TTS voice is available for specified language.
     */
    fun isAvailable(language: String): Boolean
}
