package com.itantra.app.domain.stt

import kotlinx.coroutines.flow.StateFlow

enum class SttState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR,
    /**
     * Emitted when the user tries to speak a non-English language but the Vosk
     * offline model has not been downloaded yet. The UI should show a prompt
     * directing the user to the Models tab.
     */
    MODEL_NOT_READY
}

/**
 * Abstraction for on-device offline speech recognition engine.
 * Supports streaming incremental STT and pause detection for Indian languages.
 *
 * Architecture:
 *  - English      → Android native SpeechRecognizer (offline by default)
 *  - Indian langs → Vosk (Kaldi-based) after one-time model download (~95 MB)
 */
interface OfflineSpeechRecognizer {
    val sttState: StateFlow<SttState>
    val rmsLevel: StateFlow<Float>

    /**
     * True once the Vosk model directory is present and valid in filesDir.
     * The UI should observe this to show/hide the "download model" prompt.
     */
    val isVoskModelReady: StateFlow<Boolean>

    /**
     * Start continuous microphone listening and streaming STT.
     * @param language ISO language code (e.g. "hi", "mr", "gu", "kn", "ml", "ta", "te", "or", "bn", "en")
     * @param onPartialResult Callback for real-time partial sentence updates as user speaks
     * @param onFinalResult Callback when a completed sentence is recognized after a pause
     */
    suspend fun startListening(
        language: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    )

    /**
     * Stop microphone recording and finalize recognition.
     */
    suspend fun stopListening()

    /**
     * Process raw PCM 16-bit audio chunk for custom ONNX / LiteRT model evaluation.
     */
    suspend fun transcribeChunk(audioData: ShortArray): String

    /**
     * Check if offline STT model is available on-device for specified language.
     */
    fun isAvailable(language: String): Boolean
}
