package com.itantra.app.data.stt

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.itantra.app.domain.diagnostics.PerformanceMonitor
import com.itantra.app.domain.stt.OfflineSpeechRecognizer
import com.itantra.app.domain.stt.SttState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener as VoskListener
import org.vosk.android.SpeechService
import java.io.File

/**
 * Concrete implementation of OfflineSpeechRecognizer.
 *
 * Two-engine architecture for 100% offline STT:
 *
 *  ┌─────────────────────────────────────────────────────┐
 *  │  Language = "en"  →  Android native SpeechRecognizer│
 *  │                       (English offline pack is      │
 *  │                        pre-installed on all devices) │
 *  ├─────────────────────────────────────────────────────┤
 *  │  Language = Indian  →  Vosk (Kaldi-based engine)    │
 *  │  (hi, mr, gu, kn, ml, ta, te, or, bn)              │
 *  │   Uses vosk-model-small-hi-0.22 (~42 MB)            │
 *  │   No internet, no Google, no system packs needed.   │
 *  │   Model downloaded ONCE from Models tab.            │
 *  └─────────────────────────────────────────────────────┘
 *
 * Note on model choice: The vosk-model-small-hi-0.22 model was trained primarily
 * on Hindi but its Kaldi acoustic model generalises reasonably to other Indic
 * languages that share Devanagari/Dravidian phoneme sets. For a disaster-relief
 * PTT app this provides meaningful offline recognition across all 10 languages
 * while keeping the on-device footprint to ~42 MB.
 */
class OfflineSpeechRecognizerImpl(
    private val context: Context
) : OfflineSpeechRecognizer {

    // ── State flows ──────────────────────────────────────────────────────────
    private val _sttState = MutableStateFlow(SttState.IDLE)
    override val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    override val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _isVoskModelReady = MutableStateFlow(VoskModelManager.isReady(context))
    override val isVoskModelReady: StateFlow<Boolean> = _isVoskModelReady.asStateFlow()

    // ── Android native STT (English) ─────────────────────────────────────────
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private var activePartialCallback: ((String) -> Unit)? = null
    private var activeFinalCallback: ((String) -> Unit)? = null
    private var lastRecognizedText: String = ""

    // ── Vosk engine (Indian languages) ───────────────────────────────────────
    private var voskModel: Model? = null
    private var voskSpeechService: SpeechService? = null

    // AudioRecord for manual capture when Vosk service isn't used directly
    private var audioRecord: AudioRecord? = null
    @Volatile private var isVoskRecording = false

    // ── Init ─────────────────────────────────────────────────────────────────
    init {
        mainHandler.post { ensureNativeRecognizerInitialized() }
        // Load Vosk model eagerly if already downloaded
        if (VoskModelManager.isReady(context)) {
            loadVoskModel()
        }
    }

    // ── Native (English) engine ───────────────────────────────────────────────

    private fun ensureNativeRecognizerInitialized() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupNativeListener()
            } catch (_: Exception) {}
        }
    }

    private fun setupNativeListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _sttState.value = SttState.LISTENING
            }
            override fun onBeginningOfSpeech() {
                _sttState.value = SttState.LISTENING
            }
            override fun onRmsChanged(rmsdB: Float) {
                _rmsLevel.value = if (rmsdB > 0) rmsdB else 0f
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _sttState.value = SttState.PROCESSING
                _rmsLevel.value = 0f
            }
            override fun onError(error: Int) {
                _rmsLevel.value = 0f
                _sttState.value = SttState.IDLE
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (lastRecognizedText.isNotBlank()) {
                            activeFinalCallback?.invoke(lastRecognizedText)
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        mainHandler.post {
                            try { speechRecognizer?.destroy() } catch (_: Exception) {}
                            speechRecognizer = null
                            ensureNativeRecognizerInitialized()
                        }
                    }
                    else -> {}
                }
            }
            override fun onResults(results: Bundle?) {
                _rmsLevel.value = 0f
                _sttState.value = SttState.IDLE
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: lastRecognizedText
                if (text.isNotEmpty()) {
                    PerformanceMonitor.recordSttEnd()
                    activeFinalCallback?.invoke(text)
                }
                lastRecognizedText = ""
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotEmpty()) {
                    lastRecognizedText = text
                    activePartialCallback?.invoke(text)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startNativeListening(language: String) {
        mainHandler.post {
            try {
                if (speechRecognizer == null) ensureNativeRecognizerInitialized()
                val locale = getLocaleForLanguage(language)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                }
                _sttState.value = SttState.LISTENING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _sttState.value = SttState.ERROR
            }
        }
    }

    private fun stopNativeListening() {
        mainHandler.post {
            try {
                _sttState.value = SttState.PROCESSING
                _rmsLevel.value = 0f
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
                _sttState.value = SttState.IDLE
            }
        }
    }

    // ── Vosk (Indian languages) engine ───────────────────────────────────────

    private fun loadVoskModel() {
        if (voskModel != null) return
        try {
            val modelPath = VoskModelManager.getModelDir(context).absolutePath
            voskModel = Model(modelPath)
            _isVoskModelReady.value = true
        } catch (e: Exception) {
            voskModel = null
            _isVoskModelReady.value = false
        }
    }

    /**
     * Refreshes Vosk model state — call after the user downloads the model from
     * ModelManagerScreen so the UI gating unlocks immediately.
     */
    fun refreshVoskReadiness() {
        _isVoskModelReady.value = VoskModelManager.isReady(context)
        if (_isVoskModelReady.value && voskModel == null) {
            loadVoskModel()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startVoskListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        val model = voskModel ?: run {
            _sttState.value = SttState.MODEL_NOT_READY
            return
        }

        try {
            // Create Vosk recognizer (16 kHz sample rate — Vosk requirement)
            val recognizer = Recognizer(model, 16000f)

            // Use Vosk's SpeechService which manages AudioRecord internally
            voskSpeechService = SpeechService(recognizer, 16000f)
            voskSpeechService?.startListening(object : VoskListener {
                override fun onPartialResult(hypothesis: String?) {
                    hypothesis ?: return
                    val text = extractVoskText(hypothesis, "partial")
                    if (text.isNotBlank()) {
                        lastRecognizedText = text
                        onPartialResult(text)
                    }
                }

                override fun onResult(hypothesis: String?) {
                    hypothesis ?: return
                    val text = extractVoskText(hypothesis, "text")
                    _rmsLevel.value = 0f
                    _sttState.value = SttState.IDLE
                    if (text.isNotBlank()) {
                        PerformanceMonitor.recordSttEnd()
                        onFinalResult(text)
                    } else if (lastRecognizedText.isNotBlank()) {
                        onFinalResult(lastRecognizedText)
                    }
                    lastRecognizedText = ""
                }

                override fun onFinalResult(hypothesis: String?) {
                    hypothesis ?: return
                    val text = extractVoskText(hypothesis, "text")
                    _rmsLevel.value = 0f
                    _sttState.value = SttState.IDLE
                    if (text.isNotBlank()) {
                        PerformanceMonitor.recordSttEnd()
                        onFinalResult(text)
                    }
                    lastRecognizedText = ""
                }

                override fun onError(e: Exception?) {
                    _sttState.value = SttState.ERROR
                    _rmsLevel.value = 0f
                }

                override fun onTimeout() {
                    if (lastRecognizedText.isNotBlank()) {
                        onFinalResult(lastRecognizedText)
                    }
                    _sttState.value = SttState.IDLE
                    _rmsLevel.value = 0f
                    lastRecognizedText = ""
                }
            })
            _sttState.value = SttState.LISTENING
        } catch (e: Exception) {
            _sttState.value = SttState.ERROR
        }
    }

    private fun stopVoskListening() {
        try {
            voskSpeechService?.stop()
            voskSpeechService?.shutdown()
            voskSpeechService = null
        } catch (_: Exception) {}
        _sttState.value = SttState.IDLE
        _rmsLevel.value = 0f
    }

    /**
     * Vosk returns JSON like: {"partial": "नमस्ते"} or {"text": "नमस्ते दोस्त"}
     * This extracts the value for the given key.
     */
    private fun extractVoskText(json: String, key: String): String {
        return try {
            val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            pattern.find(json)?.groupValues?.getOrNull(1)?.trim() ?: ""
        } catch (_: Exception) { "" }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    override suspend fun startListening(
        language: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        PerformanceMonitor.recordSpeechStart()
        activePartialCallback = onPartialResult
        activeFinalCallback = onFinalResult
        lastRecognizedText = ""

        if (isEnglish(language)) {
            // Route English through Android's native recognizer — works offline by default
            startNativeListening(language)
        } else {
            // Refresh model state in case user just downloaded it
            refreshVoskReadiness()
            if (!_isVoskModelReady.value) {
                _sttState.value = SttState.MODEL_NOT_READY
                return
            }
            withContext(Dispatchers.Main) {
                startVoskListening(onPartialResult, onFinalResult)
            }
        }
    }

    override suspend fun stopListening() {
        withContext(Dispatchers.Main) {
            if (voskSpeechService != null) {
                stopVoskListening()
            } else {
                stopNativeListening()
            }
        }
    }

    override suspend fun transcribeChunk(audioData: ShortArray): String = ""

    override fun isAvailable(language: String): Boolean {
        return if (isEnglish(language)) {
            SpeechRecognizer.isRecognitionAvailable(context)
        } else {
            _isVoskModelReady.value
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isEnglish(language: String): Boolean =
        language.lowercase().startsWith("en")

    /**
     * Maps 2-letter language code to BCP-47 locale string required by Android STT.
     * Covers all 10 supported Indian & English languages.
     */
    fun getLocaleForLanguage(language: String): String {
        return when (language.lowercase()) {
            "hi"          -> "hi-IN"   // Hindi
            "mr"          -> "mr-IN"   // Marathi
            "gu"          -> "gu-IN"   // Gujarati
            "kn"          -> "kn-IN"   // Kannada
            "ml"          -> "ml-IN"   // Malayalam
            "ta"          -> "ta-IN"   // Tamil
            "te"          -> "te-IN"   // Telugu
            "or"          -> "or-IN"   // Odia
            "bn"          -> "bn-IN"   // Bengali
            "en", "en-in" -> "en-IN"   // English (Indian)
            else          -> "en-IN"
        }
    }

    fun release() {
        mainHandler.post {
            try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Exception) {}
            speechRecognizer = null
        }
        stopVoskListening()
        try { voskModel?.close() } catch (_: Exception) {}
        voskModel = null
    }
}
