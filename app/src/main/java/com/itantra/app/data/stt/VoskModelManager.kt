package com.itantra.app.data.stt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manages download and lifecycle of the Vosk offline speech model.
 *
 * Model: vosk-model-small-hi-0.22 (Hindi small, ~42 MB compressed)
 * Coverage: Hindi (hi) — the only Indian language with an official Vosk small model.
 *           All other Indian languages (mr, gu, kn, ml, ta, te, or, bn) also route through
 *           this engine using the Indian-English acoustic base, which handles mixed Indic
 *           speech better than the Android native STT without internet.
 *
 * The model is stored at: <filesDir>/vosk-model/
 * A sentinel file <filesDir>/vosk-model/.ready is created after successful extraction.
 */
object VoskModelManager {

    private const val TAG = "VoskModelManager"

    // Vosk Hindi small model — Apache 2.0, alphacephei CDN
    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip"
    private const val MODEL_DIR_NAME = "vosk-model"
    private const val SENTINEL_FILE = ".ready"
    private const val MODEL_ZIP_TMP = "vosk_model_tmp.zip"

    // Approximate compressed size in bytes (shown in UI progress)
    const val MODEL_SIZE_MB = 42

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    /** null = idle, 0..1 = downloading, negative = error */
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    fun getModelDir(context: Context): File =
        File(context.filesDir, MODEL_DIR_NAME)

    fun isReady(context: Context): Boolean =
        File(getModelDir(context), SENTINEL_FILE).exists()

    /**
     * Downloads and extracts the Vosk model.
     * Emits progress via [downloadProgress] (0.0–1.0).
     * Returns true on success, false on failure.
     */
    suspend fun downloadModel(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadProgress.value = 0f
            val tmpZip = File(context.filesDir, MODEL_ZIP_TMP)
            val modelDir = getModelDir(context)

            // --- Download ---
            val url = URL(MODEL_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connect()
            val totalBytes = conn.contentLength.toLong()
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tmpZip).use { output ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        downloadedBytes += n
                        if (totalBytes > 0) {
                            _downloadProgress.value = downloadedBytes.toFloat() / totalBytes * 0.8f
                        }
                    }
                }
            }

            // --- Extract ---
            modelDir.mkdirs()
            _downloadProgress.value = 0.82f
            ZipInputStream(tmpZip.inputStream()).use { zip ->
                var entry = zip.nextEntry
                var totalEntries = 0
                while (entry != null) {
                    totalEntries++
                    val relativePath = if (entry.name.contains('/')) entry.name.substringAfter('/') else entry.name
                    if (relativePath.isNotBlank()) {
                        val outFile = File(modelDir, relativePath)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out ->
                                zip.copyTo(out)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // --- Mark as ready ---
            File(modelDir, SENTINEL_FILE).createNewFile()
            tmpZip.delete()

            _downloadProgress.value = 1f
            Log.i(TAG, "Vosk model extracted to ${modelDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Vosk model download failed: ${e.message}", e)
            _downloadProgress.value = -1f
            false
        } finally {
            // Reset after short delay — caller handles the final state display
        }
    }

    fun deleteModel(context: Context) {
        getModelDir(context).deleteRecursively()
        _downloadProgress.value = null
    }
}
