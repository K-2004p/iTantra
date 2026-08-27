package com.itantra.app.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.domain.diagnostics.PerformanceMonitor
import com.itantra.app.domain.tts.AudioData
import com.itantra.app.domain.tts.OfflineSpeechSynthesizer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Concrete implementation of OfflineSpeechSynthesizer.
 * Synthesizes text to speech offline with language fallback and volume priority override.
 */
class OfflineSpeechSynthesizerImpl(
    private val context: Context
) : OfflineSpeechSynthesizer, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        }
    }

    override suspend fun synthesizeAndPlay(
        text: String,
        language: String,
        priority: PriorityLevel
    ) {
        PerformanceMonitor.recordTtsStart()

        if (priority == PriorityLevel.EMERGENCY) {
            // Maximum volume for emergency alerts
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            tts?.stop()
        }

        val locale = getLocaleForLanguage(language)
        tts?.language = locale

        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                PerformanceMonitor.recordPlaybackStart()
            }
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })

        tts?.speak(
            text,
            if (priority == PriorityLevel.EMERGENCY) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            params,
            "utterance_${System.currentTimeMillis()}"
        )
    }

    override suspend fun synthesize(text: String, language: String): AudioData {
        return AudioData(pcmSamples = ByteArray(0), sampleRate = 16000, durationMs = 1000L)
    }

    override suspend fun stop() {
        tts?.stop()
    }

    override fun isAvailable(language: String): Boolean {
        if (!isInitialized) return true
        val result = tts?.isLanguageAvailable(getLocaleForLanguage(language))
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun getLocaleForLanguage(language: String): Locale {
        return when (language.lowercase()) {
            "hi" -> Locale("hi", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "ta" -> Locale("ta", "IN")
            "te" -> Locale("te", "IN")
            "or" -> Locale("or", "IN")
            "bn" -> Locale("bn", "IN")
            else -> Locale.ENGLISH
        }
    }
}
