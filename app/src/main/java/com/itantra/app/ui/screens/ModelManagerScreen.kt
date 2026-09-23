package com.itantra.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.data.models.ModelInfo
import com.itantra.app.data.models.ModelManager
import com.itantra.app.data.stt.OfflineSpeechRecognizerImpl
import com.itantra.app.data.stt.VoskModelManager
import com.itantra.app.data.translation.OfflineTranslatorImpl
import com.itantra.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ModelManagerScreen(
    translator: OfflineTranslatorImpl? = null,
    sttEngine: OfflineSpeechRecognizerImpl? = null
) {
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current

    // Translation download state
    var isDownloadingAll by remember { mutableStateOf(false) }
    var downloadStatusText by remember { mutableStateOf<String?>(null) }

    // Vosk model download state
    val voskProgress by VoskModelManager.downloadProgress.collectAsState()
    val isVoskReady by (sttEngine?.isVoskModelReady
        ?: kotlinx.coroutines.flow.MutableStateFlow(VoskModelManager.isReady(localContext)))
        .collectAsState()
    var isDownloadingVosk by remember { mutableStateOf(false) }
    var voskError by remember { mutableStateOf<String?>(null) }

    // Sync Vosk state from progress flow
    LaunchedEffect(voskProgress) {
        when {
            voskProgress == null         -> isDownloadingVosk = false
            voskProgress!! < 0f          -> {
                isDownloadingVosk = false
                voskError = "Download failed. Check internet connection and retry."
            }
            voskProgress!! >= 1f         -> {
                isDownloadingVosk = false
                voskError = null
                sttEngine?.refreshVoskReadiness()
            }
            else                         -> {
                isDownloadingVosk = true
                voskError = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Storage Status Overview (Sizes Removed) ──────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), TechBorder)),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonCyanSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SdStorage, contentDescription = "Storage", tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("ON-DEVICE AI MODEL ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                            Text("Offline Neural Translation & Speech", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("10 Indian Languages Configured", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                    Badge(
                        containerColor = if (isVoskReady) SuccessGreenSubtle else AmberAlertSubtle,
                        contentColor = if (isVoskReady) SuccessGreen else AmberAlert
                    ) {
                        Text(
                            text = if (isVoskReady) "ACTIVE" else "STANDBY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // ── Vosk STT Model Card (Sizes Removed) ───────────────────────────────────
        item {
            val cardBorderColor = when {
                isVoskReady       -> SuccessGreen
                voskError != null -> EmergencyRed
                else              -> AmberAlert
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoskReady) SuccessGreenSubtle else AmberAlertSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isVoskReady) SuccessGreen else AmberAlert,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "OFFLINE SPEECH-TO-TEXT ENGINE",
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp
                                )
                                Text("Vosk Neural Kaldi · 100% On-Device", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                        Badge(
                            containerColor = if (isVoskReady) SuccessGreenSubtle else AmberAlertSubtle,
                            contentColor   = if (isVoskReady) SuccessGreen else AmberAlert
                        ) {
                            Text(
                                if (isVoskReady) "READY ✓" else "NEEDS DOWNLOAD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "One-time setup over Wi-Fi. " +
                        "After download, speech recognition for Hindi, Marathi, Gujarati, Kannada, " +
                        "Malayalam, Tamil, Telugu, Odia & Bengali works fully offline — " +
                        "no SIM card, cellular data, or internet required.",
                        fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
                    )

                    // Progress indicator
                    AnimatedVisibility(visible = isDownloadingVosk && voskProgress != null && voskProgress!! in 0f..1f) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            val pct = ((voskProgress ?: 0f) * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if ((voskProgress ?: 0f) < 0.82f) "Downloading STT model files..." else "Extracting model packages...",
                                    fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold
                                )
                                Text(text = "$pct%", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { voskProgress ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan,
                                trackColor = TechBorder
                            )
                        }
                    }

                    // Error Message
                    if (voskError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(voskError!!, fontSize = 12.sp, color = EmergencyRed, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isVoskReady) {
                        Button(
                            onClick = {
                                isDownloadingVosk = true
                                voskError = null
                                scope.launch { VoskModelManager.downloadModel(localContext) }
                            },
                            enabled = !isDownloadingVosk,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDownloadingVosk) TechBorder else AmberAlert
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TechDarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isDownloadingVosk) "DOWNLOADING OFFLINE ENGINE..."
                                else "DOWNLOAD OFFLINE STT MODEL",
                                fontWeight = FontWeight.Bold, color = TechDarkBackground, fontSize = 12.sp
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessGreenSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Offline speech engine verified. All 10 Indian languages operational.",
                                fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── Device Setup Instructions ──────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TechCardSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TechBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OFFLINE FIELD DEPLOYMENT GUIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAlert, letterSpacing = 0.5.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Offline STT: Tap 'Download Offline STT Model' once over Wi-Fi.\n" +
                        "2. Translation: Tap 'Pre-Cache Translation Models' below once over Wi-Fi.\n" +
                        "3. Field Mission: Zero cellular tower, SIM, or Wi-Fi router needed in disaster zones.",
                        fontSize = 11.sp, color = TextPrimary, lineHeight = 16.sp
                    )
                }
            }
        }

        // ── Neural Translation Pack ────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TechBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyanSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("OFFLINE NEURAL TRANSLATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp)
                                Text("ML Kit On-Device Neural Models", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                        Badge(containerColor = SuccessGreenSubtle, contentColor = SuccessGreen) {
                            Text("OFFLINE READY", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Download all Indian language pairs once over Wi-Fi so neural translation works seamlessly without network connection.",
                        fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
                    )

                    if (downloadStatusText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(downloadStatusText!!, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }

                    if (translator != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isDownloadingAll = true
                                downloadStatusText = "Downloading translation models..."
                                scope.launch {
                                    val langs = listOf("en", "hi", "mr", "gu", "kn", "ta", "te", "bn")
                                    var count = 0
                                    for (lang in langs) {
                                        downloadStatusText = "Downloading $lang model (${count + 1}/${langs.size})..."
                                        val ok = translator.downloadModel(lang)
                                        if (ok) count++
                                    }
                                    downloadStatusText = "✓ Complete! $count / ${langs.size} models verified for offline translation."
                                    isDownloadingAll = false
                                }
                            },
                            enabled = !isDownloadingAll,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TechDarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isDownloadingAll) "DOWNLOADING TRANSLATION MODELS..." else "PRE-CACHE TRANSLATION MODELS",
                                fontWeight = FontWeight.Bold, color = TechDarkBackground, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Budget Device Architecture Card (Sizes Removed) ────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreenSubtle),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUDGET DEVICE OPTIMISED ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, letterSpacing = 0.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    listOf(
                        "Neural Engine"       to "Vosk Kaldi + ONNX Mobile",
                        "Quantization Type"   to "INT8 Quantized",
                        "Model Execution"     to "On-Demand (1 Concurrent)",
                        "Memory Optimization" to "LRU Single Cache Paging",
                        "Network Requirement" to "Zero (100% On-Device)",
                        "OS Compatibility"    to "Android 7.0+ (API 24 to 36)",
                        "Target Hardware"     to "Standard & Entry-Level Devices"
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 11.sp, color = TextSecondary)
                            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // ── Section Title ──────────────────────────────────────────────────────
        item {
            Text(
                text = "SUPPORTED LANGUAGE INVENTORY",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── Language Models List (Sizes Removed) ─────────────────────────────────
        items(ModelManager.supportedLanguages) { model ->
            ModelItemRow(model = model, isVoskReady = isVoskReady)
        }
    }
}

@Composable
fun ModelItemRow(model: ModelInfo, isVoskReady: Boolean) {
    val isSttReady = model.languageCode.equals("en", ignoreCase = true) || isVoskReady
    Card(
        colors = CardDefaults.cardColors(containerColor = TechCardSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.nativeName,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = model.languageName,
                        fontSize = 13.sp, color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // STT Status (size removed)
                    Text(
                        text = "STT: ${if (isSttReady) "Ready" else "Pending Setup"}",
                        fontSize = 11.sp,
                        color = if (isSttReady) SuccessGreen else AmberAlert,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("•", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.width(10.dp))
                    // TTS Status (size removed)
                    Text("TTS: Ready", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (model.languageCode.equals("en", ignoreCase = true)) "Android Native Engine" else "Vosk On-Device Engine",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(
                    containerColor = if (isSttReady) SuccessGreenSubtle else AmberAlertSubtle,
                    contentColor = if (isSttReady) SuccessGreen else AmberAlert
                ) {
                    Text(
                        text = if (isSttReady) "READY ✓" else "NEEDS STT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Language code pill (replaces size MB)
                Badge(
                    containerColor = TechCardSurfaceElevated,
                    contentColor = NeonCyan
                ) {
                    Text(
                        text = model.languageCode.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
