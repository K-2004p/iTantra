package com.itantra.app.data.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.itantra.app.domain.translation.OfflineTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Concrete implementation of OfflineTranslator providing 10-language offline translation.
 * Combines high-speed instant dictionary rule-engine for tactical/emergency commands
 * with Google ML Kit on-device neural translation for arbitrary spoken text.
 */
class OfflineTranslatorImpl : OfflineTranslator {

    private val supportedLangs = setOf("en", "hi", "mr", "gu", "kn", "ml", "ta", "te", "or", "bn")
    private val translatorCache = ConcurrentHashMap<String, Translator>()

    // Comprehensive offline dictionary & vocabulary delegated to PhraseBook
    private val translationDictionary = PhraseBook.phrases
    private val vocabularyWords = PhraseBook.vocabulary

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        val src = sourceLanguage.lowercase()
        val tgt = targetLanguage.lowercase()

        if (src == tgt || text.isBlank()) {
            return@withContext text
        }

        val cleanText = text.trim()
        val lowerText = cleanText.lowercase()

        // 1. Exact & Substring phrase match across rich emergency dictionary
        for ((_, langMap) in translationDictionary) {
            val srcValue = langMap[src]?.lowercase()
            if (srcValue != null && (lowerText == srcValue || lowerText == srcValue.trim())) {
                val targetText = langMap[tgt]
                if (!targetText.isNullOrEmpty()) {
                    return@withContext targetText
                }
            }
        }

        // 1b. Check if phrase is contained in text
        for ((_, langMap) in translationDictionary) {
            val srcValue = langMap[src]?.lowercase()
            if (srcValue != null && srcValue.length > 3 && lowerText.contains(srcValue)) {
                val targetText = langMap[tgt]
                if (!targetText.isNullOrEmpty()) {
                    return@withContext targetText
                }
            }
        }

        // 2. Token / word replacement for combined emergency words
        for ((_, vocabMap) in vocabularyWords) {
            val srcWord = vocabMap[src]?.lowercase()
            if (srcWord != null && (lowerText == srcWord || lowerText == srcWord.trim())) {
                val targetWord = vocabMap[tgt]
                if (!targetWord.isNullOrEmpty()) {
                    return@withContext targetWord
                }
            }
        }

        // 3. ML Kit On-Device Neural Translation (when models are pre-cached)
        val mlKitSource = mapToMlKitLanguage(src)
        val mlKitTarget = mapToMlKitLanguage(tgt)

        if (mlKitSource != null && mlKitTarget != null) {
            try {
                val cacheKey = "${mlKitSource}_to_${mlKitTarget}"
                val client = translatorCache.getOrPut(cacheKey) {
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(mlKitSource)
                        .setTargetLanguage(mlKitTarget)
                        .build()
                    Translation.getClient(options)
                }

                // Check if model is downloaded (do not hang if offline)
                val isDownloaded = isModelDownloaded(src) && isModelDownloaded(tgt)
                if (isDownloaded) {
                    val result = suspendCancellableCoroutine<String?> { cont ->
                        client.translate(cleanText)
                            .addOnSuccessListener { cont.resume(it) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                    if (!result.isNullOrEmpty()) {
                        return@withContext result
                    }
                } else {
                    // Try fast on-device translation
                    val result = suspendCancellableCoroutine<String?> { cont ->
                        client.translate(cleanText)
                            .addOnSuccessListener { cont.resume(it) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                    if (!result.isNullOrEmpty()) {
                        return@withContext result
                    }
                }
            } catch (_: Exception) {
                // Fall through to dictionary token matching
            }
        }

        // 4. Token-by-token replacement fallback
        val words = cleanText.split("\\s+".toRegex())
        val translatedTokens = words.map { word ->
            val wLower = word.lowercase()
            var matched: String? = null
            for ((_, vocabMap) in vocabularyWords) {
                val vSrc = vocabMap[src]?.lowercase()
                if (vSrc != null && wLower.contains(vSrc)) {
                    matched = vocabMap[tgt]
                    break
                }
            }
            matched ?: word
        }
        val tokenTranslation = translatedTokens.joinToString(" ")
        if (tokenTranslation != cleanText) {
            return@withContext tokenTranslation
        }

        // 5. Clean output
        val tgtName = getLangName(tgt)
        return@withContext cleanText
    }

    override fun isLanguagePairSupported(sourceLanguage: String, targetLanguage: String): Boolean {
        return supportedLangs.contains(sourceLanguage.lowercase()) && supportedLangs.contains(targetLanguage.lowercase())
    }

    private fun mapToMlKitLanguage(code: String): String? {
        return when (code.lowercase()) {
            "en" -> TranslateLanguage.ENGLISH
            "hi" -> TranslateLanguage.HINDI
            "mr" -> TranslateLanguage.MARATHI
            "gu" -> TranslateLanguage.GUJARATI
            "kn" -> TranslateLanguage.KANNADA
            "ta" -> TranslateLanguage.TAMIL
            "te" -> TranslateLanguage.TELUGU
            "bn" -> TranslateLanguage.BENGALI
            "ur" -> TranslateLanguage.URDU
            else -> TranslateLanguage.fromLanguageTag(code)
        }
    }

    suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        val mlLang = mapToMlKitLanguage(languageCode) ?: return@withContext false
        val model = TranslateRemoteModel.Builder(mlLang).build()
        return@withContext suspendCancellableCoroutine { cont ->
            RemoteModelManager.getInstance().isModelDownloaded(model)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(false) }
        }
    }

    suspend fun downloadModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        val mlLang = mapToMlKitLanguage(languageCode) ?: return@withContext false
        val model = TranslateRemoteModel.Builder(mlLang).build()
        val conditions = DownloadConditions.Builder().build()
        return@withContext suspendCancellableCoroutine { cont ->
            RemoteModelManager.getInstance().download(model, conditions)
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { cont.resume(false) }
        }
    }

    private fun getLangName(code: String): String {
        return when (code.lowercase()) {
            "en" -> "English"
            "hi" -> "Hindi"
            "mr" -> "Marathi"
            "gu" -> "Gujarati"
            "kn" -> "Kannada"
            "ml" -> "Malayalam"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            "or" -> "Odia"
            "bn" -> "Bengali"
            else -> code.uppercase()
        }
    }
}
