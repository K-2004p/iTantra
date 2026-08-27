package com.itantra.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.itantra.app.data.networking.SocketTextTransport
import com.itantra.app.data.stt.OfflineSpeechRecognizerImpl
import com.itantra.app.data.tts.OfflineSpeechSynthesizerImpl
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.domain.communication.TextPacket
import com.itantra.app.domain.diagnostics.PerformanceMonitor
import com.itantra.app.domain.emergency.EmergencyAlertManager
import com.itantra.app.ui.screens.*
import com.itantra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sttEngine: OfflineSpeechRecognizerImpl
    private lateinit var ttsEngine: OfflineSpeechSynthesizerImpl
    private val textTransport = SocketTextTransport()
    private val emergencyManager = EmergencyAlertManager()

    private var sequenceCounter = 1

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

        checkAndRequestPermissions()

        setContent {
            ITantraTheme {
                MainApp()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
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
        var selectedTab by remember { mutableIntStateOf(0) }
        var currentLanguage by remember { mutableStateOf("hi") }
        var recognizedText by remember { mutableStateOf("") }
        var partialText by remember { mutableStateOf("") }
        var isPlayingTts by remember { mutableStateOf(false) }

        val connectionStatus by textTransport.observeConnectionStatus().collectAsState()
        val receivedPackets = remember { mutableStateListOf<TextPacket>() }
        var activeEmergency by remember { mutableStateOf<TextPacket?>(null) }

        // Observe incoming packets over transport
        LaunchedEffect(Unit) {
            textTransport.observeReceivedPackets().collectLatest { packet ->
                receivedPackets.add(0, packet)
                if (packet.priority == PriorityLevel.EMERGENCY) {
                    emergencyManager.triggerEmergency(packet)
                    activeEmergency = packet
                }

                // Auto playback received text using offline TTS
                isPlayingTts = true
                ttsEngine.synthesizeAndPlay(packet.payload, packet.language, packet.priority)
                isPlayingTts = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("iTantra Transceiver", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NeonCyan)
                            Text("SIH 2026 • PS ID: 26173 • Offline P2P Link", fontSize = 10.sp, color = TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TechDarkBackground)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = TechCardSurface) {
                    val tabs = listOf(
                        NavTab("Sender", Icons.Default.Mic),
                        NavTab("Receiver", Icons.Default.Hearing),
                        NavTab("Connect", Icons.Default.Wifi),
                        NavTab("Language", Icons.Default.Language),
                        NavTab("Models", Icons.Default.SdStorage),
                        NavTab("Metrics", Icons.Default.Speed)
                    )
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TechDarkBackground,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
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
                    0 -> SenderScreen(
                        currentLanguage = currentLanguage,
                        connectionStatus = connectionStatus,
                        onTransmitText = { text, priority ->
                            val packet = TextPacket(
                                messageId = "msg_${System.currentTimeMillis()}",
                                sequenceNumber = sequenceCounter++,
                                senderName = Build.MODEL ?: "Device A",
                                language = currentLanguage,
                                priority = priority,
                                payload = text
                            )
                            lifecycleScope.launch {
                                val success = textTransport.send(packet)
                                if (success) {
                                    Toast.makeText(this@MainActivity, "Transmitted (${packet.payload.length} chars)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Transmission failed: No connection", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onStartStt = {
                            recognizedText = ""
                            partialText = ""
                            lifecycleScope.launch {
                                sttEngine.startListening(
                                    language = currentLanguage,
                                    onPartialResult = { partialText = it },
                                    onFinalResult = { text ->
                                        recognizedText = text
                                        partialText = ""
                                    }
                                )
                            }
                        },
                        onStopStt = {
                            lifecycleScope.launch {
                                sttEngine.stopListening()
                            }
                        },
                        recognizedText = recognizedText,
                        partialText = partialText
                    )

                    1 -> ReceiverScreen(
                        receivedPackets = receivedPackets,
                        activeEmergency = activeEmergency,
                        isPlayingTts = isPlayingTts,
                        onDismissEmergency = {
                            emergencyManager.dismissEmergency()
                            activeEmergency = null
                        },
                        onReplayTts = { packet ->
                            lifecycleScope.launch {
                                ttsEngine.synthesizeAndPlay(packet.payload, packet.language, packet.priority)
                            }
                        }
                    )

                    2 -> ConnectionScreen(
                        connectionStatus = connectionStatus,
                        onStartServer = { port ->
                            lifecycleScope.launch {
                                textTransport.startServer(port)
                            }
                        },
                        onConnectToServer = { host, port ->
                            lifecycleScope.launch {
                                textTransport.connectToServer(host, port)
                            }
                        },
                        onDisconnect = {
                            lifecycleScope.launch {
                                textTransport.disconnect()
                            }
                        }
                    )

                    3 -> LanguageScreen(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = { currentLanguage = it }
                    )

                    4 -> ModelManagerScreen()

                    5 -> DiagnosticsScreen()
                }
            }
        }
    }

    private data class NavTab(val title: String, val icon: ImageVector)
}
