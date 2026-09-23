package com.itantra.app.domain.translation

/**
 * Domain interface for offline multilingual text translation between 10 supported languages.
 */
interface OfflineTranslator {
    /**
     * Translates input text from source language to target language offline.
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String

    /**
     * Checks if a language pair is supported offline.
     */
    fun isLanguagePairSupported(sourceLanguage: String, targetLanguage: String): Boolean
}
