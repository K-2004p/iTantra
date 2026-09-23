package com.itantra.app.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.itantra.app.data.networking.HybridTransportManager
import com.itantra.app.data.networking.NetworkKeepAliveService
import com.itantra.app.data.networking.SelectedTransportMode
import com.itantra.app.data.stt.OfflineSpeechRecognizerImpl
import com.itantra.app.data.translation.OfflineTranslatorImpl
import com.itantra.app.data.tts.OfflineSpeechSynthesizerImpl
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.domain.communication.TextPacket
import com.itantra.app.domain.diagnostics.PerformanceMonitor
import com.itantra.app.domain.emergency.EmergencyAlertManager
import com.itantra.app.ui.screens.*
import com.itantra.app.domain.emergency.AppRole
import com.itantra.app.domain.emergency.AppRoleManager
import com.itantra.app.domain.emergency.LoudAlertManager
import com.itantra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sttEngine: OfflineSpeechRecognizerImpl
    private lateinit var ttsEngine: OfflineSpeechSynthesizerImpl
    private lateinit var loudAlertManager: LoudAlertManager
    private lateinit var roleManager: AppRoleManager
    private val textTransport = HybridTransportManager()
    private val emergencyManager = EmergencyAlertManager()
    private val translator = OfflineTranslatorImpl()

    private var sequenceCounter = 1

    // BroadcastReceiver for Bluetooth device discovery
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        val name = try {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                device.name ?: "Unknown Device"
                            } else "Bluetooth Device"
                        } catch (e: Exception) {
                            "Bluetooth Device"
                        }
                        textTransport.bluetoothTransport.onDeviceDiscovered(name, device.address)
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordAudioGranted) {
            Toast.makeText(this, "Microphone permission required for STT", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sttEngine = OfflineSpeechRecognizerImpl(this)
        ttsEngine = OfflineSpeechSynthesizerImpl(this)
        loudAlertManager = LoudAlertManager(this)
        roleManager = AppRoleManager(this)

        // Register BT discovery receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)

        checkAndRequestPermissions()

        try {
            NetworkKeepAliveService.start(this)
        } catch (_: Exception) {}

        setContent {
            ITantraTheme {
                MainApp()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.VIBRATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ Bluetooth permissions
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainApp() {
        var appRole by remember { mutableStateOf(roleManager.getAppRole()) }
        var showRoleMenu by remember { mutableStateOf(false) }

        var selectedTab by remember {
            mutableIntStateOf(
                when (appRole) {
                    AppRole.RECEIVER -> 1
                    else -> 0
                }
            )
        }
        // Independent Input (I Speak) and Output (I Hear / Target) Languages
        var inputLanguage by remember { mutableStateOf("hi") }
        var outputLanguage by remember { mutableStateOf("mr") }

        // Sender STT state
        var recognizedText by remember { mutableStateOf("") }
        var partialText by remember { mutableStateOf("") }

        // Vosk offline model readiness (gates Indian language STT mic button)
        val isVoskModelReady by sttEngine.isVoskModelReady.collectAsState()

        // Receiver STT state (bidirectional reply)
        var isReceiverListening by remember { mutableStateOf(false) }
        var receiverRecognizedText by remember { mutableStateOf("") }

        var isPlayingTts by remember { mutableStateOf(false) }

        val connectionStatus by textTransport.observeConnectionStatus().collectAsState()
        val selectedTransportMode by textTransport.selectedMode.collectAsState()
        val lastErrorMessage by textTransport.lastErrorMessage.collectAsState()
        val isWifiServerRunning by textTransport.isWifiServerRunning.collectAsState()
        val isBtServerRunning by textTransport.isBtServerRunning.collectAsState()
        val wifiServerPort by textTransport.wifiServerPort.collectAsState()

        val isServerRunning = if (selectedTransportMode == SelectedTransportMode.WIFI) isWifiServerRunning else isBtServerRunning

        val receivedPackets = remember { mutableStateListOf<TextPacket>() }
        var activeEmergency by remember { mutableStateOf<TextPacket?>(null) }

        // Bluetooth discovery and paired lists
        val pairedBtDevices = remember { textTransport.bluetoothTransport.getPairedDevices() }
        val discoveredBtDevices by textTransport.bluetoothTransport.discoveredDevices.collectAsState()
        val isBtDiscovering by textTransport.bluetoothTransport.isDiscovering.collectAsState()
        val localBtName = remember { textTransport.bluetoothTransport.getLocalDeviceName() }

        // Auto-start server if in Receiver or Transceiver (dual) role
        LaunchedEffect(appRole) {
            if (appRole == AppRole.RECEIVER || appRole == AppRole.TRANSCEIVER) {
                lifecycleScope.launch {
                    textTransport.startServer(8888)
                }
            }
        }

        // Ensure packet collection is never interrupted when changing languages
        val currentTargetLanguage by rememberUpdatedState(outputLanguage)

        // Observe incoming packets non-stop
        LaunchedEffect(Unit) {
            textTransport.observeReceivedPackets().collect { packet ->
                receivedPackets.add(0, packet)
                if (packet.priority == PriorityLevel.EMERGENCY) {
                    emergencyManager.triggerEmergency(packet)
                    activeEmergency = packet
                }

                // 1. Play High-Decibel Loud Attention Chime, Haptic Vibration & Notification
                loudAlertManager.playLoudIncomingAlert(packet)

                // 2. Offline Translate message into Target Language if source != target
                // Target is packet's intended targetLanguage (if non-empty) or this phone's selected outputLanguage
                val targetLang = if (packet.targetLanguage.isNotEmpty()) packet.targetLanguage else currentTargetLanguage
                
                val textToSpeak = if (!packet.language.equals(targetLang, ignoreCase = true)) {
                    try {
                        translator.translate(packet.payload, packet.language, targetLang)
                    } catch (_: Exception) {
                        packet.payload
                    }
                } else {
                    packet.payload
                }

                // 3. Auto TTS playback in target language
                isPlayingTts = true
                ttsEngine.synthesizeAndPlay(textToSpeak, targetLang, packet.priority)
                isPlayingTts = false
            }
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "iTantra Transceiver",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        "10 Indian Languages • Direct P2P Mesh",
                                        fontSize = 10.sp,
                                        color = NeonCyan.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        },
                        actions = {
                            // Phone Role Selector Button
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                FilterChip(
                                    selected = true,
                                    onClick = { showRoleMenu = true },
                                    shape = RoundedCornerShape(20.dp),
                                    label = {
                                        Text(
                                            text = when (appRole) {
                                                AppRole.SENDER -> "📱 SENDER"
                                                AppRole.RECEIVER -> "🔊 RECEIVER"
                                                AppRole.TRANSCEIVER -> "🔄 DUAL MODE"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (appRole) {
                                            AppRole.SENDER -> AmberAlert
                                            AppRole.RECEIVER -> NeonCyan
                                            AppRole.TRANSCEIVER -> SuccessGreen
                                        },
                                        selectedLabelColor = TechDarkBackground
                                    )
                                )
                                DropdownMenu(
                                    expanded = showRoleMenu,
                                    onDismissRequest = { showRoleMenu = false }
                                ) {
                                    AppRole.entries.forEach { role ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(role.title, fontWeight = FontWeight.Bold)
                                                    Text(role.description, fontSize = 10.sp, color = TextMuted)
                                                }
                                            },
                                            onClick = {
                                                appRole = role
                                                roleManager.setAppRole(role)
                                                showRoleMenu = false
                                                if (role == AppRole.RECEIVER || role == AppRole.TRANSCEIVER) {
                                                    if (role == AppRole.RECEIVER) selectedTab = 1
                                                    lifecycleScope.launch { textTransport.startServer(8888) }
                                                } else if (role == AppRole.SENDER) {
                                                    selectedTab = 0
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = TechDarkBackground)
                    )
                    HorizontalDivider(color = TechBorder, thickness = 1.dp)
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = TechBorder, thickness = 1.dp)
                    NavigationBar(
                        containerColor = TechCardSurface,
                        tonalElevation = 0.dp
                    ) {
                        val tabs = listOf(
                            NavTab("Sender",   Icons.Default.Mic),
                            NavTab("Receiver", Icons.Default.Hearing),
                            NavTab("Connect",  Icons.Default.Wifi),
                            NavTab("Language", Icons.Default.Language),
                            NavTab("Models",   Icons.Default.SdStorage),
                            NavTab("Metrics",  Icons.Default.Speed)
                        )
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(20.dp)) },
                                label = { Text(tab.title, fontSize = 10.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TechDarkBackground,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    // ─── SENDER ───────────────────────────────────────────────────────────────
                    0 -> SenderScreen(
                        inputLanguage = inputLanguage,
                        outputLanguage = outputLanguage,
                        connectionStatus = connectionStatus,
                        recognizedText = recognizedText,
                        partialText = partialText,
                        onTransmitText = { text, priority, targetLang ->
                            val packet = TextPacket(
                                messageId = "msg_${System.currentTimeMillis()}",
                                sequenceNumber = sequenceCounter++,
                                senderName = Build.MODEL ?: "Device A",
                                language = inputLanguage,
                                targetLanguage = targetLang,
                                priority = priority,
                                payload = text
                            )
                            lifecycleScope.launch {
                                val success = textTransport.send(packet)
                                val mode = if (selectedTransportMode == SelectedTransportMode.BLUETOOTH) "Bluetooth" else "Wi-Fi"
                                if (success) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Transmitted via $mode (${text.length} chars)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val err = textTransport.lastErrorMessage.value ?: "No active peer connection"
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Transmission failed: $err. Check Connect tab.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onStartStt = {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                checkAndRequestPermissions()
                                Toast.makeText(this@MainActivity, "Microphone permission required. Please allow access.", Toast.LENGTH_SHORT).show()
                            } else if (!inputLanguage.lowercase().startsWith("en") && !isVoskModelReady) {
                                Toast.makeText(this@MainActivity, "Please download the offline STT model in the Models tab first.", Toast.LENGTH_SHORT).show()
                            } else {
                                recognizedText = ""
                                partialText = ""
                                lifecycleScope.launch {
                                    sttEngine.startListening(
                                        language = inputLanguage,
                                        onPartialResult = { partialText = it },
                                        onFinalResult = { text ->
                                            recognizedText = text
                                            partialText = ""
                                        }
                                    )
                                }
                            }
                        },
                        onStopStt = {
                            lifecycleScope.launch { sttEngine.stopListening() }
                        },
                        onPlayTts = { text, lang ->
                            lifecycleScope.launch {
                                ttsEngine.synthesizeAndPlay(text, lang, PriorityLevel.NORMAL)
                            }
                        },
                        onTranslate = { text, srcLang, tgtLang, callback ->
                            lifecycleScope.launch {
                                val translated = translator.translate(text, srcLang, tgtLang)
                                callback(translated)
                            }
                        },
                        isVoskModelReady = isVoskModelReady,
                        onGoToModels = { selectedTab = 4 }
                    )

                    // ─── RECEIVER ─────────────────────────────────────────────────────────────
                    1 -> ReceiverScreen(
                        receivedPackets = receivedPackets,
                        activeEmergency = activeEmergency,
                        isPlayingTts = isPlayingTts,
                        inputLanguage = inputLanguage,
                        outputLanguage = outputLanguage,
                        isReceiverListening = isReceiverListening,
                        receiverRecognizedText = receiverRecognizedText,
                        onDismissEmergency = {
                            emergencyManager.dismissEmergency()
                            activeEmergency = null
                        },
                        onReplayTts = { packet ->
                            lifecycleScope.launch {
                                val targetLang = if (packet.targetLanguage.isNotEmpty()) packet.targetLanguage else outputLanguage
                                val textToSpeak = if (!packet.language.equals(targetLang, ignoreCase = true)) {
                                    translator.translate(packet.payload, packet.language, targetLang)
                                } else {
                                    packet.payload
                                }
                                ttsEngine.synthesizeAndPlay(textToSpeak, targetLang, packet.priority)
                            }
                        },
                        onTranslateAndPlay = { text, srcLang, tgtLang, callback ->
                            lifecycleScope.launch {
                                val translated = translator.translate(text, srcLang, tgtLang)
                                callback(translated)
                                isPlayingTts = true
                                ttsEngine.synthesizeAndPlay(translated, tgtLang, PriorityLevel.NORMAL)
                                isPlayingTts = false
                            }
                        },
                        onStartReceiverStt = {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                checkAndRequestPermissions()
                                Toast.makeText(this@MainActivity, "Microphone permission required for reply.", Toast.LENGTH_SHORT).show()
                            } else if (!inputLanguage.lowercase().startsWith("en") && !isVoskModelReady) {
                                Toast.makeText(this@MainActivity, "Please download the offline STT model in the Models tab first.", Toast.LENGTH_SHORT).show()
                            } else {
                                isReceiverListening = true
                                receiverRecognizedText = ""
                                lifecycleScope.launch {
                                    sttEngine.startListening(
                                        language = inputLanguage,
                                        onPartialResult = { receiverRecognizedText = it },
                                        onFinalResult = { text ->
                                            receiverRecognizedText = text
                                            isReceiverListening = false
                                            // Auto-transmit receiver reply back
                                            val replyPacket = TextPacket(
                                                messageId = "reply_${System.currentTimeMillis()}",
                                                sequenceNumber = sequenceCounter++,
                                                senderName = "${Build.MODEL ?: "Device"} [Reply]",
                                                language = inputLanguage,
                                                targetLanguage = outputLanguage,
                                                priority = PriorityLevel.NORMAL,
                                                payload = text
                                            )
                                            lifecycleScope.launch { textTransport.send(replyPacket) }
                                        }
                                    )
                                }
                            }
                        },
                        onStopReceiverStt = {
                            isReceiverListening = false
                            lifecycleScope.launch { sttEngine.stopListening() }
                        },
                        onSendReplyText = { text, priority, targetLang ->
                            val replyPacket = TextPacket(
                                messageId = "reply_${System.currentTimeMillis()}",
                                sequenceNumber = sequenceCounter++,
                                senderName = "${Build.MODEL ?: "Device"} [Reply]",
                                language = inputLanguage,
                                targetLanguage = targetLang,
                                priority = priority,
                                payload = text
                            )
                            lifecycleScope.launch { textTransport.send(replyPacket) }
                        },
                        onReplayLoudAlert = { priority ->
                            lifecycleScope.launch {
                                loudAlertManager.playLoudIncomingAlert(priority)
                            }
                        }
                    )

                    // ─── CONNECT ──────────────────────────────────────────────────────────────
                    2 -> ConnectionScreen(
                        connectionStatus = connectionStatus,
                        selectedTransportMode = selectedTransportMode,
                        pairedBluetoothDevices = pairedBtDevices,
                        discoveredBluetoothDevices = discoveredBtDevices,
                        isBtDiscovering = isBtDiscovering,
                        localBtName = localBtName,
                        lastErrorMessage = lastErrorMessage,
                        isServerRunning = isServerRunning,
                        serverPort = wifiServerPort,
                        onSelectTransportMode = { mode -> textTransport.setTransportMode(mode) },
                        onStartServer = { port ->
                            lifecycleScope.launch { textTransport.startServer(port) }
                        },
                        onConnectToServer = { host, port ->
                            lifecycleScope.launch { textTransport.connectToServer(host, port) }
                        },
                        onConnectBluetooth = { macAddress ->
                            textTransport.setTransportMode(SelectedTransportMode.BLUETOOTH)
                            lifecycleScope.launch {
                                textTransport.connectToServer(macAddress, 0)
                            }
                        },
                        onStartBtDiscovery = {
                            textTransport.bluetoothTransport.startDiscovery()
                        },
                        onDisconnect = {
                            lifecycleScope.launch { textTransport.disconnect() }
                        }
                    )

                    // ─── LANGUAGE ─────────────────────────────────────────────────────────────
                    3 -> LanguageScreen(
                        inputLanguage = inputLanguage,
                        outputLanguage = outputLanguage,
                        onInputLanguageSelected = { inputLanguage = it },
                        onOutputLanguageSelected = { outputLanguage = it }
                    )

                    // ─── MODELS ──────────────────────────────────────────────────────────────
                    4 -> ModelManagerScreen(
                        translator = translator,
                        sttEngine = sttEngine
                    )

                    // ─── METRICS ─────────────────────────────────────────────────────────────
                    5 -> DiagnosticsScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            NetworkKeepAliveService.stop(this)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
        ttsEngine.release()
    }

    private data class NavTab(val title: String, val icon: ImageVector)
}
