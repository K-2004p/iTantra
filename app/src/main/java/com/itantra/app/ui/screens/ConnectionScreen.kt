package com.itantra.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import com.itantra.app.data.networking.SelectedTransportMode
import com.itantra.app.data.networking.SocketTextTransport
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.ui.theme.*

@Composable
fun ConnectionScreen(
    connectionStatus: ConnectionStatus,
    selectedTransportMode: SelectedTransportMode,
    pairedBluetoothDevices: List<Pair<String, String>>, // name to MAC address
    discoveredBluetoothDevices: List<Pair<String, String>> = emptyList(),
    isBtDiscovering: Boolean = false,
    localBtName: String = "Android Device",
    lastErrorMessage: String?,
    isServerRunning: Boolean,
    serverPort: Int,
    onSelectTransportMode: (SelectedTransportMode) -> Unit,
    onStartServer: (Int) -> Unit,
    onConnectToServer: (String, Int) -> Unit,
    onConnectBluetooth: (String) -> Unit,
    onStartBtDiscovery: () -> Unit = {},
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    var hostIpInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("8888") }
    var localIps by remember { mutableStateOf(SocketTextTransport.getAllLocalIpAddresses()) }
    val primaryIp = remember(localIps) { SocketTextTransport.getLocalIpAddress() }

    // Pre-fill default host IP if empty
    LaunchedEffect(Unit) {
        if (hostIpInput.isEmpty()) {
            hostIpInput = if (primaryIp != "127.0.0.1" && primaryIp != SocketTextTransport.DEFAULT_HOTSPOT_IP) {
                primaryIp
            } else {
                SocketTextTransport.DEFAULT_HOTSPOT_IP
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
        // ── Top Header Card ────────────────────────────────────────────────────
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OFFLINE FIELD LINK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Direct P2P Transceiver Link",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                        Badge(containerColor = SuccessGreenSubtle, contentColor = SuccessGreen) {
                            Text("100% OFFLINE", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Zero infrastructure required: Wi-Fi Hotspot or Bluetooth RFCOMM",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // ── Transport Mode Selector ────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TechBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SELECT TRANSMISSION MEDIUM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Wi-Fi Mode Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTransportMode == SelectedTransportMode.WIFI)
                                    NeonCyanSubtle else TechDarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (selectedTransportMode == SelectedTransportMode.WIFI) 2.dp else 1.dp,
                                    color = if (selectedTransportMode == SelectedTransportMode.WIFI) NeonCyan else TechBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectTransportMode(SelectedTransportMode.WIFI) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = "Wi-Fi",
                                    tint = if (selectedTransportMode == SelectedTransportMode.WIFI) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Wi-Fi / Hotspot", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (selectedTransportMode == SelectedTransportMode.WIFI) NeonCyan else TextSecondary
                                )
                                Text("High-Speed TCP", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        // Bluetooth Mode Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTransportMode == SelectedTransportMode.BLUETOOTH)
                                    NeonCyanSubtle else TechDarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (selectedTransportMode == SelectedTransportMode.BLUETOOTH) 2.dp else 1.dp,
                                    color = if (selectedTransportMode == SelectedTransportMode.BLUETOOTH) NeonCyan else TechBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectTransportMode(SelectedTransportMode.BLUETOOTH) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = "Bluetooth",
                                    tint = if (selectedTransportMode == SelectedTransportMode.BLUETOOTH) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Bluetooth P2P", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (selectedTransportMode == SelectedTransportMode.BLUETOOTH) NeonCyan else TextSecondary
                                )
                                Text("RFCOMM SPP", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // ── Link Status & Info Card ────────────────────────────────────────────
        item {
            val statusColor = when (connectionStatus) {
                ConnectionStatus.CONNECTED    -> SuccessGreen
                ConnectionStatus.CONNECTING  -> AmberAlert
                ConnectionStatus.DISCONNECTED -> TextMuted
                ConnectionStatus.ERROR        -> EmergencyRed
            }
            val statusText = when {
                connectionStatus == ConnectionStatus.CONNECTED -> "CONNECTED (Channel Ready)"
                connectionStatus == ConnectionStatus.CONNECTING && isServerRunning -> "SERVER ACTIVE (Listening for peer...)"
                connectionStatus == ConnectionStatus.CONNECTING -> "CONNECTING TO PEER..."
                connectionStatus == ConnectionStatus.ERROR -> "CONNECTION FAILED"
                else -> "DISCONNECTED (Offline)"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (connectionStatus == ConnectionStatus.ERROR) EmergencyRed else TechBorder,
                        RoundedCornerShape(14.dp)
                    )
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LINK STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    // Error banner
                    if (!lastErrorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmergencyRedSubtle),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EmergencyRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(lastErrorMessage, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }

                    if (selectedTransportMode == SelectedTransportMode.WIFI) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = TechBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("This Device Local IP:", fontSize = 11.sp, color = TextSecondary)
                                Text(primaryIp, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                            Row {
                                IconButton(onClick = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clip?.setPrimaryClip(ClipData.newPlainText("IP", primaryIp))
                                    Toast.makeText(context, "IP Copied: $primaryIp", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    localIps = SocketTextTransport.getAllLocalIpAddresses()
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh IPs", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Presets
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SuggestionChip(
                                onClick = { hostIpInput = SocketTextTransport.DEFAULT_HOTSPOT_IP },
                                label = { Text("Use Hotspot IP (192.168.43.1)", fontSize = 10.sp) }
                            )
                            if (primaryIp != "127.0.0.1" && primaryIp != SocketTextTransport.DEFAULT_HOTSPOT_IP) {
                                SuggestionChip(
                                    onClick = { hostIpInput = primaryIp },
                                    label = { Text("Use This IP ($primaryIp)", fontSize = 10.sp) }
                                )
                            }
                        }
                    } else {
                        // Bluetooth info
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = TechBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("This Phone's Bluetooth Identity:", fontSize = 11.sp, color = TextSecondary)
                                Text(localBtName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }
                    }
                }
            }
        }

        // ── Wi-Fi Connection Steps ─────────────────────────────────────────────
        if (selectedTransportMode == SelectedTransportMode.WIFI) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TechBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Step 1: Receiver
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = NeonCyan, contentColor = TechDarkBackground) {
                                Text("STEP 1: RECEIVER PHONE", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Turn ON Hotspot in Android Settings, then start server to receive transmissions.",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onStartServer(portInput.toIntOrNull() ?: 8888) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServerRunning) SuccessGreen else NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = TechDarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServerRunning) "✓ SERVER ACTIVE (PORT ${serverPort})" else "START SERVER (RECEIVER ROLE)",
                                fontWeight = FontWeight.Bold,
                                color = TechDarkBackground,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = TechBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Step 2: Sender
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = AmberAlert, contentColor = TechDarkBackground) {
                                Text("STEP 2: SENDER PHONE", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Connect to Receiver's Hotspot Wi-Fi in Android Settings, enter their IP, and tap Connect.",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = hostIpInput,
                            onValueChange = { hostIpInput = it },
                            label = { Text("Receiver Phone IP Address") },
                            placeholder = { Text("e.g. 192.168.43.1") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = TechBorder,
                                focusedLabelColor = NeonCyan,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onConnectToServer(hostIpInput, portInput.toIntOrNull() ?: 8888) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAlert),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("CONNECT TO RECEIVER (SENDER ROLE)", fontWeight = FontWeight.Bold, color = TechDarkBackground, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── Bluetooth Connection Controls ──────────────────────────────────────
        if (selectedTransportMode == SelectedTransportMode.BLUETOOTH) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TechCardSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TechBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("BLUETOOTH RFCOMM P2P CHANNEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Receiver Listen button
                        Button(
                            onClick = { onStartServer(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isServerRunning) SuccessGreen else NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = TechDarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServerRunning) "✓ LISTENING FOR BT CONNECTIONS" else "LISTEN FOR BT (RECEIVER ROLE)",
                                fontWeight = FontWeight.Bold,
                                color = TechDarkBackground,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = TechBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Scan nearby button & paired devices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SELECT PEER DEVICE (SENDER)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            }
                            OutlinedButton(
                                onClick = onStartBtDiscovery,
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBtDiscovering) "Scanning..." else "Scan", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val allDevices = (pairedBluetoothDevices + discoveredBluetoothDevices).distinctBy { it.second }

                        if (allDevices.isEmpty()) {
                            Text(
                                "No nearby devices detected.\n1. Enable Bluetooth on both phones.\n2. Receiver taps 'LISTEN FOR BT' above.\n3. Sender taps 'Scan' above or pairs in Android Settings.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                allDevices.forEach { (name, address) ->
                                    val isPaired = pairedBluetoothDevices.any { it.second == address }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(TechDarkBackground, RoundedCornerShape(10.dp))
                                            .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
                                            .clickable { onConnectBluetooth(address) }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                if (isPaired) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Badge(containerColor = TechCardSurfaceElevated, contentColor = NeonCyan) {
                                                        Text("PAIRED", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(address, fontSize = 10.sp, color = TextMuted)
                                        }
                                        Icon(
                                            Icons.Default.BluetoothConnected,
                                            contentDescription = "Connect",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Disconnect Button ──────────────────────────────────────────────────
        if (connectionStatus == ConnectionStatus.CONNECTED || isServerRunning) {
            item {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("DISCONNECT / STOP SERVER", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}
