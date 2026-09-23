package com.itantra.app.data.networking

import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.domain.communication.TextPacket
import com.itantra.app.domain.communication.TextTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class SelectedTransportMode {
    WIFI,
    BLUETOOTH
}

/**
 * Unified hybrid transport layer seamlessly combining Wi-Fi (Sockets / Wi-Fi Direct)
 * and Bluetooth RFCOMM transports with instant mode selection.
 */
class HybridTransportManager : TextTransport {

    val wifiTransport = SocketTextTransport()
    val bluetoothTransport = BluetoothTextTransport()

    private val _selectedMode = MutableStateFlow(SelectedTransportMode.WIFI)
    val selectedMode: StateFlow<SelectedTransportMode> = _selectedMode.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun observeConnectionStatus(): StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<TextPacket>(extraBufferCapacity = 64)
    override fun observeReceivedPackets(): Flow<TextPacket> = _receivedPackets.asSharedFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    val wifiServerPort: StateFlow<Int> = wifiTransport.serverPort
    val isWifiServerRunning: StateFlow<Boolean> = wifiTransport.isServerRunning
    val isBtServerRunning: StateFlow<Boolean> = bluetoothTransport.isServerRunning

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Observe Wi-Fi status & packets
        scope.launch {
            wifiTransport.observeConnectionStatus().collectLatest { status ->
                updateUnifiedStatus()
            }
        }
        scope.launch {
            wifiTransport.observeReceivedPackets().collect { packet ->
                _receivedPackets.emit(packet)
            }
        }
        scope.launch {
            wifiTransport.lastErrorMessage.collectLatest { error ->
                if (_selectedMode.value == SelectedTransportMode.WIFI && error != null) {
                    _lastErrorMessage.value = error
                }
            }
        }

        // Observe Bluetooth status & packets
        scope.launch {
            bluetoothTransport.observeConnectionStatus().collectLatest { status ->
                updateUnifiedStatus()
            }
        }
        scope.launch {
            bluetoothTransport.observeReceivedPackets().collect { packet ->
                _receivedPackets.emit(packet)
            }
        }
        scope.launch {
            bluetoothTransport.lastErrorMessage.collectLatest { error ->
                if (_selectedMode.value == SelectedTransportMode.BLUETOOTH && error != null) {
                    _lastErrorMessage.value = error
                }
            }
        }
    }

    private fun updateUnifiedStatus() {
        val wifiStatus = wifiTransport.observeConnectionStatus().value
        val btStatus = bluetoothTransport.observeConnectionStatus().value

        _connectionStatus.value = when {
            wifiStatus == ConnectionStatus.CONNECTED || btStatus == ConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
            wifiStatus == ConnectionStatus.CONNECTING || btStatus == ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
            wifiStatus == ConnectionStatus.ERROR && btStatus == ConnectionStatus.ERROR -> ConnectionStatus.ERROR
            else -> ConnectionStatus.DISCONNECTED
        }
    }

    fun setTransportMode(mode: SelectedTransportMode) {
        _selectedMode.value = mode
        val activeStatus = when (mode) {
            SelectedTransportMode.WIFI -> wifiTransport.observeConnectionStatus().value
            SelectedTransportMode.BLUETOOTH -> bluetoothTransport.observeConnectionStatus().value
        }
        if (activeStatus == ConnectionStatus.CONNECTED) {
            _connectionStatus.value = ConnectionStatus.CONNECTED
        } else {
            updateUnifiedStatus()
        }
        _lastErrorMessage.value = when (mode) {
            SelectedTransportMode.WIFI -> wifiTransport.lastErrorMessage.value
            SelectedTransportMode.BLUETOOTH -> bluetoothTransport.lastErrorMessage.value
        }
    }

    /**
     * Start servers on active mode, and optionally both for dual transceiver
     */
    override suspend fun startServer(port: Int) {
        scope.launch {
            try {
                wifiTransport.startServer(port)
            } catch (_: Exception) {}
        }
        scope.launch {
            try {
                bluetoothTransport.startServer(0)
            } catch (_: Exception) {}
        }
    }

    override suspend fun connectToServer(host: String, port: Int) {
        when (_selectedMode.value) {
            SelectedTransportMode.WIFI -> wifiTransport.connectToServer(host, port)
            SelectedTransportMode.BLUETOOTH -> bluetoothTransport.connectToServer(host, port)
        }
    }

    override suspend fun send(packet: TextPacket): Boolean {
        // Try active selected mode first
        val primaryResult = when (_selectedMode.value) {
            SelectedTransportMode.WIFI -> wifiTransport.send(packet)
            SelectedTransportMode.BLUETOOTH -> bluetoothTransport.send(packet)
        }
        if (primaryResult) return true

        // Fallback to alternate connected medium if primary failed
        return when (_selectedMode.value) {
            SelectedTransportMode.WIFI -> {
                if (bluetoothTransport.observeConnectionStatus().value == ConnectionStatus.CONNECTED) {
                    bluetoothTransport.send(packet)
                } else false
            }
            SelectedTransportMode.BLUETOOTH -> {
                if (wifiTransport.observeConnectionStatus().value == ConnectionStatus.CONNECTED) {
                    wifiTransport.send(packet)
                } else false
            }
        }
    }

    override suspend fun disconnect() {
        wifiTransport.disconnect()
        bluetoothTransport.disconnect()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _lastErrorMessage.value = null
    }
}

