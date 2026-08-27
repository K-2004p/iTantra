package com.itantra.app.data.models

data class ModelInfo(
    val languageCode: String,
    val languageName: String,
    val nativeName: String,
    val sttInstalled: Boolean = true,
    val ttsInstalled: Boolean = true,
    val sttSizeMb: Float = 48.5f,
    val ttsSizeMb: Float = 32.2f
)

object ModelManager {
    val supportedLanguages = listOf(
        ModelInfo("en", "English", "English", true, true, 35.0f, 25.0f),
        ModelInfo("hi", "Hindi", "हिन्दी", true, true, 48.5f, 32.2f),
        ModelInfo("mr", "Marathi", "मराठी", true, true, 42.0f, 30.5f),
        ModelInfo("gu", "Gujarati", "ગુજરાતી", true, true, 44.0f, 28.0f),
        ModelInfo("kn", "Kannada", "ಕನ್ನಡ", true, true, 46.2f, 31.0f),
        ModelInfo("ml", "Malayalam", "മലയാളം", true, true, 45.0f, 33.0f),
        ModelInfo("ta", "Tamil", "தமிழ்", true, true, 49.0f, 34.5f),
        ModelInfo("te", "Telugu", "తెలుగు", true, true, 47.8f, 32.0f),
        ModelInfo("or", "Odia", "ଓଡ଼ିଆ", true, true, 41.5f, 29.0f),
        ModelInfo("bn", "Bengali", "বাংলা", true, true, 43.8f, 30.2f)
    )

    fun getTotalStorageMb(): Float {
        return supportedLanguages.sumOf { (it.sttSizeMb + it.ttsSizeMb).toDouble() }.toFloat()
    }

    fun getLanguageName(code: String): String {
        val model = supportedLanguages.find { it.languageCode.equals(code, ignoreCase = true) }
        return if (model != null) "${model.languageName} (${model.nativeName})" else code
    }
}
