package com.itantra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onDismissEmergency: () -> Unit,
    onReplayTts: (TextPacket) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(16.dp)
    ) {
        // Emergency Alert Takeover Banner
        if (activeEmergency != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, EmergencyRed, RoundedCornerShape(12.dp))
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Emergency", tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EMERGENCY BROADCAST RECEIVED",
                            fontWeight = FontWeight.Bold,
                            color = EmergencyRed,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${activeEmergency.payload}\"",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "From: ${activeEmergency.senderName} • Language: ${ModelManager.getLanguageName(activeEmergency.language)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismissEmergency,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("DISMISS ALERT", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Live Audio Playback Banner
        if (isPlayingTts) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Playing", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔊 Playing Speech via On-Device TTS...",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Text(
            text = "RECEIVED PACKET FEED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (receivedPackets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for incoming messages...", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(receivedPackets) { packet ->
                    PacketItem(packet = packet, onReplay = { onReplayTts(packet) })
                }
            }
        }
    }
}

@Composable
fun PacketItem(packet: TextPacket, onReplay: () -> Unit) {
    val borderColor = when (packet.priority) {
        PriorityLevel.EMERGENCY -> EmergencyRed
        PriorityLevel.IMPORTANT -> AmberAlert
        else -> TechBorder
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TechCardSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${packet.senderName} (${ModelManager.getLanguageName(packet.language)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = packet.priority.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (packet.priority) {
                        PriorityLevel.EMERGENCY -> EmergencyRed
                        PriorityLevel.IMPORTANT -> AmberAlert
                        else -> SuccessGreen
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"${packet.payload}\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRC32: 0x${packet.checksum.toString(16).uppercase()} • Seq #${packet.sequenceNumber}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                IconButton(onClick = onReplay, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Replay", tint = NeonCyan)
                }
            }
        }
    }
}
