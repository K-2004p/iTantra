package com.itantra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.data.networking.SocketTextTransport
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.ui.theme.*

@Composable
fun ConnectionScreen(
    connectionStatus: ConnectionStatus,
    onStartServer: (Int) -> Unit,
    onConnectToServer: (String, Int) -> Unit,
    onDisconnect: () -> Unit
) {
    var hostIpInput by remember { mutableStateOf("192.168.1.5") }
    var portInput by remember { mutableStateOf("8888") }
    val localIp = remember { SocketTextTransport.getLocalIpAddress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
            .padding(16.dp)
    ) {
        // High-Tech Offline Indicator Header
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "iTantra Neural Transceiver",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = NeonCyan
                    )
                    Badge(
                        containerColor = SuccessGreen,
                        contentColor = TechDarkBackground
                    ) {
                        Text("● OFFLINE AI", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiOff, contentDescription = "No Internet Required", tint = AmberAlert)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Internet: NOT REQUIRED (Zero Cloud / Local P2P Only)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LOCAL LINK STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connection Status:", fontSize = 14.sp, color = TextPrimary)
                    Text(
                        text = connectionStatus.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> SuccessGreen
                            ConnectionStatus.CONNECTING -> AmberAlert
                            ConnectionStatus.DISCONNECTED -> TextMuted
                            ConnectionStatus.ERROR -> EmergencyRed
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("This Device IP:", fontSize = 14.sp, color = TextPrimary)
                    Text(localIp, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ESTABLISH LOCAL P2P LINK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Host Server (Receiver Role)
                Button(
                    onClick = { onStartServer(portInput.toIntOrNull() ?: 8888) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("START SERVER (RECEIVER ROLE)", fontWeight = FontWeight.Bold, color = TechDarkBackground)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = TechBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Connect to Host (Sender Role)
                OutlinedTextField(
                    value = hostIpInput,
                    onValueChange = { hostIpInput = it },
                    label = { Text("Receiver Device IP Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TechBorder,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onConnectToServer(hostIpInput, portInput.toIntOrNull() ?: 8888) },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAlert),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONNECT TO RECEIVER (SENDER ROLE)", fontWeight = FontWeight.Bold, color = TechDarkBackground)
                }

                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DISCONNECT", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
