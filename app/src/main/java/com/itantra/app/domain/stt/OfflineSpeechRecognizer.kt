package com.itantra.app.domain.stt

/**
 * Abstraction for on-device offline speech recognition engine.
 * Supports streaming incremental STT and pause detection for Indian languages.
 */
interface OfflineSpeechRecognizer {
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
