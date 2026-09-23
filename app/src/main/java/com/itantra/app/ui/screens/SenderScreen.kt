package com.itantra.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.data.models.ModelManager
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.ui.theme.*

@Composable
fun SenderScreen(
    inputLanguage: String,
    outputLanguage: String,
    connectionStatus: ConnectionStatus,
    onTransmitText: (String, PriorityLevel, String) -> Unit,
    onStartStt: () -> Unit,
    onStopStt: () -> Unit,
    onPlayTts: (String, String) -> Unit,
    onTranslate: (String, String, String, (String) -> Unit) -> Unit,
    recognizedText: String,
    partialText: String,
    isVoskModelReady: Boolean = true,
    onGoToModels: () -> Unit = {}
) {
    var selectedPriority by remember { mutableStateOf(PriorityLevel.NORMAL) }
    var isPressed by remember { mutableStateOf(false) }

    // Editable text & translation
    var messageInput by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var selectedTranslateLang by remember { mutableStateOf(outputLanguage) }
    var showTranslateDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(outputLanguage) {
        if (selectedTranslateLang.isEmpty() || selectedTranslateLang == inputLanguage) {
            selectedTranslateLang = outputLanguage
        }
    }

    // Interactive button scale & pulse animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.14f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val scrollState = rememberScrollState()

    // Sync recognizedText into messageInput when STT produces results
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            messageInput = recognizedText
        }
    }

    // Quick tactical phrases
    val quickPhrases = listOf(
        Triple("🚨 Help Needed", "Need help immediately", PriorityLevel.EMERGENCY),
        Triple("🩺 Medical Aid", "Need medical assistance", PriorityLevel.EMERGENCY),
        Triple("🚑 Ambulance", "Ambulance needed immediately", PriorityLevel.EMERGENCY),
        Triple("🔥 Fire Alert", "Fire outbreak emergency", PriorityLevel.EMERGENCY),
        Triple("💧 Need Water", "Water supply required", PriorityLevel.IMPORTANT),
        Triple("🍱 Need Food", "Food required", PriorityLevel.IMPORTANT),
        Triple("⚠️ Danger Ahead", "Danger ahead, do not proceed", PriorityLevel.IMPORTANT),
        Triple("🌊 Flood Warning", "Flood water rising alert", PriorityLevel.EMERGENCY),
        Triple("🛑 Evacuate Now", "Evacuate area immediately", PriorityLevel.EMERGENCY),
        Triple("✅ All Clear Safe", "All clear standard operational status", PriorityLevel.NORMAL),
        Triple("📍 Where Are You?", "Where are you located?", PriorityLevel.NORMAL)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Tactical Link & Language Status Bar ────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> SuccessGreenSubtle
                    ConnectionStatus.CONNECTING -> AmberAlertSubtle
                    else -> TechCardSurface
                }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> SuccessGreen.copy(alpha = 0.6f)
                        ConnectionStatus.CONNECTING -> AmberAlert.copy(alpha = 0.6f)
                        else -> TechBorder
                    },
                    RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> SuccessGreen
                                    ConnectionStatus.CONNECTING -> AmberAlert
                                    else -> EmergencyRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> "LINK ACTIVE"
                            ConnectionStatus.CONNECTING -> "CONNECTING..."
                            else -> "OFFLINE / STANDALONE"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> SuccessGreen
                            ConnectionStatus.CONNECTING -> AmberAlert
                            else -> TextSecondary
                        },
                        letterSpacing = 0.5.sp
                    )
                }

                // Dual Language Route Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = TechCardSurfaceElevated,
                        contentColor = AmberAlert
                    ) {
                        Text(
                            text = ModelManager.getLanguageName(inputLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("➔", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(
                        containerColor = TechCardSurfaceElevated,
                        contentColor = NeonCyan
                    ) {
                        Text(
                            text = ModelManager.getLanguageName(outputLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Quick Tactical Emergency Phrases ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK TACTICAL PHRASES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Tap to load",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPhrases) { (chipLabel, fullText, priority) ->
                val chipAccent = when (priority) {
                    PriorityLevel.EMERGENCY -> EmergencyRed
                    PriorityLevel.IMPORTANT -> AmberAlert
                    PriorityLevel.NORMAL -> NeonCyan
                }
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (priority) {
                            PriorityLevel.EMERGENCY -> EmergencyRedSubtle
                            PriorityLevel.IMPORTANT -> AmberAlertSubtle
                            PriorityLevel.NORMAL -> TechCardSurface
                        }
                    ),
                    modifier = Modifier
                        .border(1.dp, chipAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    messageInput = fullText
                                    selectedPriority = priority
                                }
                            )
                        }
                ) {
                    Text(
                        text = chipLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Transmission Priority Control ──────────────────────────────────────
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
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIORITY LEVEL:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityLevel.entries.forEach { priority ->
                        val isSelected = selectedPriority == priority
                        val color = when (priority) {
                            PriorityLevel.NORMAL    -> NeonCyan
                            PriorityLevel.IMPORTANT -> AmberAlert
                            PriorityLevel.EMERGENCY -> EmergencyRed
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color else TechCardSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) color else TechBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { selectedPriority = priority })
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = priority.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TechDarkBackground else color
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Futuristic PTT Hold-To-Talk Button ─────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            // Animated Pulse Wave Rings when active
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .size(146.dp * pulseGlow)
                        .clip(CircleShape)
                        .border(2.dp, NeonCyan.copy(alpha = 0.35f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(134.dp * pulseGlow)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.55f), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(146.dp)
                        .clip(CircleShape)
                        .border(1.dp, TechBorder, CircleShape)
                )
            }

            // Core PTT Button
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (isPressed) {
                            Brush.radialGradient(listOf(NeonCyan, NeonCyanDark))
                        } else {
                            Brush.radialGradient(listOf(TechCardSurfaceElevated, TechDarkBackground))
                        }
                    )
                    .border(
                        width = if (isPressed) 3.dp else 2.dp,
                        color = if (isPressed) Color.White else NeonCyan,
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                onStartStt()
                                tryAwaitRelease()
                                isPressed = false
                                onStopStt()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPressed) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (isPressed) TechDarkBackground else NeonCyan,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPressed) "LISTENING..." else "HOLD TO TALK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isPressed) TechDarkBackground else TextPrimary
                    )
                    Text(
                        text = if (isPressed) "RELEASE TO FINISH" else ModelManager.getLanguageName(inputLanguage),
                        fontSize = 9.sp,
                        color = if (isPressed) TechDarkBackground.copy(alpha = 0.8f) else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Offline STT Setup Notice (if applicable) ───────────────────────────
        val isIndianLanguage = !inputLanguage.lowercase().startsWith("en")
        if (isIndianLanguage && !isVoskModelReady) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AmberAlertSubtle),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberAlert.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Offline Speech Engine Pending",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberAlert
                        )
                        Text(
                            "Download the offline STT package once in the Models tab for 100% offline speech recognition.",
                            fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onGoToModels,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAlert),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Setup in Models Tab →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TechDarkBackground)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Terminal / Message Card ────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TRANSMISSION PAYLOAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPressed) {
                            Badge(containerColor = AmberAlertSubtle, contentColor = AmberAlert) {
                                Text("STREAMING...", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                            }
                        } else {
                            Badge(containerColor = TechCardSurfaceElevated, contentColor = TextMuted) {
                                Text("${messageInput.length} chars", fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Editable text box
                OutlinedTextField(
                    value = if (isPressed && partialText.isNotEmpty()) partialText else messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = {
                        Text(
                            "Hold the mic or type message in ${ModelManager.getLanguageName(inputLanguage)}...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        if (messageInput.isNotEmpty() && !isPressed) {
                            IconButton(onClick = {
                                messageInput = ""
                                translatedText = ""
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TechBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // ── Integrated Translation Section ─────────────────────────────
                if (messageInput.isNotBlank() && !isPressed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = TechBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translate to:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Box {
                            OutlinedButton(
                                onClick = { showTranslateDropdown = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedTranslateLang.isEmpty()) ModelManager.getLanguageName(outputLanguage)
                                    else ModelManager.getLanguageName(selectedTranslateLang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(
                                expanded = showTranslateDropdown,
                                onDismissRequest = { showTranslateDropdown = false }
                            ) {
                                ModelManager.supportedLanguages
                                    .filter { it.languageCode != inputLanguage }
                                    .forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text("${lang.nativeName} (${lang.languageName})") },
                                            onClick = {
                                                selectedTranslateLang = lang.languageCode
                                                showTranslateDropdown = false
                                                onTranslate(
                                                    messageInput,
                                                    inputLanguage,
                                                    lang.languageCode
                                                ) { result -> translatedText = result }
                                            }
                                        )
                                    }
                            }
                        }
                    }

                    // Translated Preview Box
                    if (translatedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonCyanSubtle),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "[${selectedTranslateLang.uppercase()}] TRANSLATION (OFFLINE)",
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(translatedText, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onPlayTts(translatedText, selectedTranslateLang) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hear Audio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { onTransmitText(translatedText, selectedPriority, selectedTranslateLang) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAlert),
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = TechDarkBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Translated", fontSize = 11.sp, color = TechDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Controls: Play & Transmit Original
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPlayTts(messageInput, inputLanguage) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PLAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                val textToSend = messageInput.trim()
                                if (textToSend.isNotEmpty()) {
                                    val target = if (selectedTranslateLang.isNotEmpty()) selectedTranslateLang else outputLanguage
                                    onTransmitText(textToSend, selectedPriority, target)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (selectedPriority) {
                                    PriorityLevel.EMERGENCY -> EmergencyRed
                                    PriorityLevel.IMPORTANT -> AmberAlert
                                    PriorityLevel.NORMAL -> NeonCyan
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(2f).height(46.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = TechDarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRANSMIT NOW",
                                color = TechDarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                } else {
                    // Default Transmit state
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val textToSend = messageInput.trim().ifEmpty { "Help" }
                            val target = if (selectedTranslateLang.isNotEmpty()) selectedTranslateLang else outputLanguage
                            onTransmitText(textToSend, selectedPriority, target)
                        },
                        enabled = messageInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = TechDarkBackground, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRANSMIT", color = TechDarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
