package com.itantra.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Hearing
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
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.domain.communication.TextPacket
import com.itantra.app.ui.theme.*

@Composable
fun ReceiverScreen(
    receivedPackets: List<TextPacket>,
    activeEmergency: TextPacket?,
    isPlayingTts: Boolean,
    inputLanguage: String,
    outputLanguage: String,
    isReceiverListening: Boolean,
    receiverRecognizedText: String = "",
    onDismissEmergency: () -> Unit,
    onReplayTts: (TextPacket) -> Unit,
    onTranslateAndPlay: (String, String, String, (String) -> Unit) -> Unit,
    onStartReceiverStt: () -> Unit,
    onStopReceiverStt: () -> Unit,
    onSendReplyText: ((String, PriorityLevel, String) -> Unit)? = null,
    onReplayLoudAlert: ((PriorityLevel) -> Unit)? = null
) {
    var globalTranslateTo by remember { mutableStateOf(outputLanguage) }
    var showGlobalTranslateDropdown by remember { mutableStateOf(false) }
    var replyTextInput by remember { mutableStateOf("") }
    var isReplyPttPressed by remember { mutableStateOf(false) }

    LaunchedEffect(outputLanguage) {
        globalTranslateTo = outputLanguage
    }

    val pttScale by animateFloatAsState(
        targetValue = if (isReplyPttPressed || isReceiverListening) 1.12f else 1.0f,
        label = "pttScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "emergencyTransition")
    val emergencyPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergencyPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // ── Emergency Broadcast Alert Card ──────────────────────────────────────
        if (activeEmergency != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmergencyRedSubtle),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, EmergencyRed.copy(alpha = emergencyPulse), RoundedCornerShape(14.dp))
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(EmergencyRed.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Emergency", tint = EmergencyRed, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("EMERGENCY BROADCAST ALERT", fontWeight = FontWeight.Bold, color = EmergencyRed, fontSize = 12.sp, letterSpacing = 0.8.sp)
                                Text("High-decibel chime triggered", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                        Badge(containerColor = EmergencyRed, contentColor = Color.White) {
                            Text("CRITICAL", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${activeEmergency.payload}\"",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "From: ${activeEmergency.senderName} • ${ModelManager.getLanguageName(activeEmergency.language)}",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        Button(
                            onClick = onDismissEmergency,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("DISMISS ALERT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── Speech Synthesizer Status Bar ──────────────────────────────────────
        AnimatedVisibility(visible = isPlayingTts) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonCyanSubtle),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔊 Synthesizing speech in ${ModelManager.getLanguageName(outputLanguage)}...",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ── Receive & Hear In Configuration Bar ────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(NeonCyanSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("AUTO-TRANSLATE INCOMING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                        Text("Receive & Hear In Audio:", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { showGlobalTranslateDropdown = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (globalTranslateTo.isEmpty()) "Original"
                            else ModelManager.getLanguageName(globalTranslateTo),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(
                        expanded = showGlobalTranslateDropdown,
                        onDismissRequest = { showGlobalTranslateDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Original Audio (No Translation)") },
                            onClick = {
                                globalTranslateTo = ""
                                showGlobalTranslateDropdown = false
                            }
                        )
                        ModelManager.supportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.nativeName} · ${lang.languageName}") },
                                onClick = {
                                    globalTranslateTo = lang.languageCode
                                    showGlobalTranslateDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Transmission Messages Feed ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (receivedPackets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(TechCardSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hearing, contentDescription = null, tint = NeonCyanSubtle.copy(alpha = 0.8f), modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Listening for incoming transmissions...",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Transmissions from peers via Wi-Fi Hotspot or Bluetooth will appear here",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(receivedPackets) { packet ->
                        PacketItem(
                            packet = packet,
                            globalTranslateTo = globalTranslateTo,
                            onReplay = { onReplayTts(packet) },
                            onTranslateAndPlay = onTranslateAndPlay
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── High-Tech Bidirectional Reply Dock ─────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isReplyPttPressed || isReceiverListening) NeonCyan else TechBorder,
                    RoundedCornerShape(14.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REPLY BACK TO TRANSMITTER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 0.5.sp)
                    }
                    Text(
                        text = if (isReceiverListening || isReplyPttPressed) "● LISTENING..." else "Speak in ${ModelManager.getLanguageName(inputLanguage)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceiverListening || isReplyPttPressed) AmberAlert else TextMuted
                    )
                }

                if (receiverRecognizedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🗣 \"$receiverRecognizedText\"", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hold to Reply Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .scale(pttScale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isReplyPttPressed || isReceiverListening) {
                                    Brush.horizontalGradient(listOf(AmberAlert, NeonCyan))
                                } else {
                                    Brush.horizontalGradient(listOf(TechCardSurfaceElevated, TechDarkBackground))
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isReplyPttPressed || isReceiverListening) NeonCyan else AmberAlert,
                                RoundedCornerShape(10.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isReplyPttPressed = true
                                        onStartReceiverStt()
                                        tryAwaitRelease()
                                        isReplyPttPressed = false
                                        onStopReceiverStt()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Reply Mic",
                                tint = if (isReplyPttPressed || isReceiverListening) TechDarkBackground else AmberAlert,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isReplyPttPressed || isReceiverListening) "RELEASE TO SEND" else "HOLD TO REPLY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isReplyPttPressed || isReceiverListening) TechDarkBackground else TextPrimary
                            )
                        }
                    }

                    // Optional Type-to-reply input
                    if (onSendReplyText != null) {
                        OutlinedTextField(
                            value = replyTextInput,
                            onValueChange = { replyTextInput = it },
                            placeholder = { Text("Or type...", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = TechBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan
                            )
                        )
                        IconButton(
                            onClick = {
                                if (replyTextInput.isNotBlank()) {
                                    val target = receivedPackets.firstOrNull()?.language ?: outputLanguage
                                    onSendReplyText(replyTextInput.trim(), PriorityLevel.NORMAL, target)
                                    replyTextInput = ""
                                }
                            },
                            enabled = replyTextInput.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (replyTextInput.isNotBlank()) NeonCyan else TechCardSurfaceElevated, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (replyTextInput.isNotBlank()) TechDarkBackground else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PacketItem(
    packet: TextPacket,
    globalTranslateTo: String,
    onReplay: () -> Unit,
    onTranslateAndPlay: (String, String, String, (String) -> Unit) -> Unit
) {
    var translatedText by remember(packet.messageId, globalTranslateTo) { mutableStateOf<String?>(null) }
    var showPerMsgDropdown by remember { mutableStateOf(false) }
    var perMsgTargetLang by remember { mutableStateOf(globalTranslateTo) }

    LaunchedEffect(globalTranslateTo) {
        perMsgTargetLang = globalTranslateTo
        if (globalTranslateTo.isNotEmpty() && globalTranslateTo != packet.language) {
            onTranslateAndPlay(packet.payload, packet.language, globalTranslateTo) { res ->
                translatedText = res
            }
        } else {
            translatedText = null
        }
    }

    val borderColor = when (packet.priority) {
        PriorityLevel.EMERGENCY -> EmergencyRed
        PriorityLevel.IMPORTANT -> AmberAlert
        else -> TechBorder
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TechCardSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (packet.priority != PriorityLevel.NORMAL) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Sender, Language Pill, Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (packet.priority) {
                                    PriorityLevel.EMERGENCY -> EmergencyRed
                                    PriorityLevel.IMPORTANT -> AmberAlert
                                    else -> NeonCyan
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = packet.senderName,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = TechDarkBackground, contentColor = TextSecondary) {
                        Text(
                            "${packet.language.uppercase()}${if (packet.targetLanguage.isNotEmpty()) "➔${packet.targetLanguage.uppercase()}" else ""}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Badge(
                    containerColor = when (packet.priority) {
                        PriorityLevel.EMERGENCY -> EmergencyRedSubtle
                        PriorityLevel.IMPORTANT -> AmberAlertSubtle
                        else -> SuccessGreenSubtle
                    },
                    contentColor = when (packet.priority) {
                        PriorityLevel.EMERGENCY -> EmergencyRed
                        PriorityLevel.IMPORTANT -> AmberAlert
                        else -> SuccessGreen
                    }
                ) {
                    Text(
                        text = packet.priority.name,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Verbatim Original Message Text
            Text(
                text = "\"${packet.payload}\"",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, lineHeight = 20.sp
            )

            // Translated text card if available
            if (!translatedText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonCyanSubtle),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "[${perMsgTargetLang.uppercase()}] TRANSLATION (${ModelManager.getLanguageName(perMsgTargetLang)})",
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(translatedText!!, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seq #${packet.sequenceNumber} • CRC32: 0x${packet.checksum.toString(16).uppercase()} ✓",
                    fontSize = 10.sp, color = TextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Translate to another language dropdown
                    Box {
                        IconButton(
                            onClick = { showPerMsgDropdown = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = "Translate", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showPerMsgDropdown,
                            onDismissRequest = { showPerMsgDropdown = false }
                        ) {
                            ModelManager.supportedLanguages
                                .filter { it.languageCode != packet.language }
                                .forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text("${lang.nativeName} · ${lang.languageName}") },
                                        onClick = {
                                            perMsgTargetLang = lang.languageCode
                                            showPerMsgDropdown = false
                                            onTranslateAndPlay(packet.payload, packet.language, lang.languageCode) { res ->
                                                translatedText = res
                                            }
                                        }
                                    )
                                }
                        }
                    }

                    // Replay audio button
                    IconButton(
                        onClick = onReplay,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
