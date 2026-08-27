package com.itantra.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.ui.theme.*

@Composable
fun SenderScreen(
    currentLanguage: String,
    connectionStatus: ConnectionStatus,
    onTransmitText: (String, PriorityLevel) -> Unit,
    onStartStt: () -> Unit,
    onStopStt: () -> Unit,
    recognizedText: String,
    partialText: String
) {
    var isPttMode by remember { mutableStateOf(true) }
    var selectedPriority by remember { mutableStateOf(PriorityLevel.NORMAL) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(targetValue = if (isPressed) 1.15f else 1.0f, label = "buttonScale")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Selector Card
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
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMUNICATION MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Row {
                    FilterChip(
                        selected = isPttMode,
                        onClick = { isPttMode = true },
                        label = { Text("Push-to-Talk") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = TechDarkBackground
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isPttMode,
                        onClick = { isPttMode = false },
                        label = { Text("Conversation") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = TechDarkBackground
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Priority Level Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "MESSAGE PRIORITY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    PriorityLevel.entries.forEach { priority ->
                        val color = when (priority) {
                            PriorityLevel.NORMAL -> NeonCyan
                            PriorityLevel.IMPORTANT -> AmberAlert
                            PriorityLevel.EMERGENCY -> EmergencyRed
                        }
                        val isSelected = selectedPriority == priority

                        OutlinedButton(
                            onClick = { selectedPriority = priority },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = color
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(color, color))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(priority.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Large PTT Button
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isPressed) {
                        Brush.radialGradient(listOf(NeonCyan, NeonCyanDark))
                    } else {
                        Brush.radialGradient(listOf(TechCardSurface, TechDarkBackground))
                    }
                )
                .border(
                    width = 4.dp,
                    color = if (isPressed) NeonCyan else TechBorder,
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
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = if (isPressed) TechDarkBackground else NeonCyan,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPressed) "LISTENING..." else "HOLD TO TALK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPressed) TechDarkBackground else TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Recognized Text Display
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECOGNIZED SENTENCE (${currentLanguage.uppercase()})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = if (isPressed) "● STREAMING STT" else "READY",
                        fontSize = 11.sp,
                        color = if (isPressed) AmberAlert else SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayText = when {
                    isPressed && partialText.isNotEmpty() -> partialText
                    recognizedText.isNotEmpty() -> recognizedText
                    else -> "Press and hold the button above to speak..."
                }

                Text(
                    text = displayText,
                    fontSize = 16.sp,
                    color = if (displayText.startsWith("Press")) TextMuted else TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (recognizedText.isNotEmpty() && !isPressed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onTransmitText(recognizedText, selectedPriority) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = TechDarkBackground)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TRANSMIT", color = TechDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
