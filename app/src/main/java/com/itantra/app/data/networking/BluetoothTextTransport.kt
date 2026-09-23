package com.itantra.app.data.networking

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.itantra.app.domain.communication.ConnectionStatus
import com.itantra.app.domain.communication.ProtocolEncoder
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/**
 * Concrete implementation of TextTransport using Bluetooth RFCOMM SPP (Serial Port Profile).
 * Enables 100% offline peer-to-peer communication between devices over Bluetooth without Wi-Fi router.
 */
class BluetoothTextTransport : TextTransport {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun observeConnectionStatus(): StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<TextPacket>(extraBufferCapacity = 64)
    override fun observeReceivedPackets(): Flow<TextPacket> = _receivedPackets.asSharedFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val discoveredDevices: StateFlow<List<Pair<String, String>>> = _discoveredDevices.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isListening = false

    companion object {
        // App-specific dedicated 128-bit RFCOMM SPP UUID for iTantra
        val ITANTRA_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        // Standard SPP UUID as fallback
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME = "iTantra_BT_Transceiver"
    }

    @SuppressLint("MissingPermission")
    fun getLocalDeviceName(): String {
        return try {
            bluetoothAdapter?.name ?: "Android Device"
        } catch (_: Exception) {
            "Android Device"
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocalDeviceAddress(): String {
        return try {
            bluetoothAdapter?.address ?: "Available"
        } catch (_: Exception) {
            "Available"
        }
    }

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    override suspend fun startServer(port: Int): Unit = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            _lastErrorMessage.value = "Bluetooth is not supported on this device"
            _connectionStatus.value = ConnectionStatus.ERROR
            return@withContext
        }
        if (!bluetoothAdapter.isEnabled) {
            _lastErrorMessage.value = "Bluetooth is turned OFF. Please turn ON Bluetooth in Android Settings."
            _connectionStatus.value = ConnectionStatus.ERROR
            return@withContext
        }

        try {
            disconnect()
            _lastErrorMessage.value = null
            _connectionStatus.value = ConnectionStatus.CONNECTING

            // Multi-tier server socket listening:
            // Tier 1: Insecure RFCOMM with iTantra custom UUID (no PIN prompt, fast P2P)
            // Tier 2: Secure RFCOMM with iTantra custom UUID
            // Tier 3: Insecure RFCOMM with standard SPP UUID
            // Tier 4: Secure RFCOMM with standard SPP UUID
            serverSocket = try {
                bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, ITANTRA_UUID)
            } catch (e1: Exception) {
                try {
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, ITANTRA_UUID)
                } catch (e2: Exception) {
                    try {
                        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
                    } catch (e3: Exception) {
                        bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
                    }
                }
            }

            isListening = true
            _isServerRunning.value = true

            CoroutineScope(Dispatchers.IO).launch {
                while (isListening && serverSocket != null) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        try {
                            activeSocket?.close()
                        } catch (_: Exception) {}
                        
                        activeSocket = socket
                        outputStream = DataOutputStream(socket.outputStream)
                        inputStream = DataInputStream(socket.inputStream)
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        _lastErrorMessage.value = null

                        listenForPackets(inputStream!!)
                    } catch (e: Exception) {
                        if (isListening && serverSocket != null) {
                            _lastErrorMessage.value = "Bluetooth listen notice: ${e.localizedMessage}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _lastErrorMessage.value = "Bluetooth Server failed to start: ${e.localizedMessage} (Ensure Bluetooth is ON and permissions granted)"
            _connectionStatus.value = ConnectionStatus.ERROR
            _isServerRunning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectToServer(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            _lastErrorMessage.value = "Bluetooth is not supported on this device"
            _connectionStatus.value = ConnectionStatus.ERROR
            return@withContext
        }
        if (!bluetoothAdapter.isEnabled) {
            _lastErrorMessage.value = "Bluetooth is turned OFF. Please turn ON Bluetooth."
            _connectionStatus.value = ConnectionStatus.ERROR
            return@withContext
        }

        try {
            disconnect()
            _lastErrorMessage.value = null
            _connectionStatus.value = ConnectionStatus.CONNECTING

            val cleanHost = host.trim()
            if (cleanHost.isEmpty()) {
                _lastErrorMessage.value = "Target Bluetooth MAC address cannot be empty"
                _connectionStatus.value = ConnectionStatus.ERROR
                return@withContext
            }

            // Interpret `host` parameter as Bluetooth MAC Address (e.g. "AA:BB:CC:DD:EE:FF")
            val targetDevice: BluetoothDevice = try {
                bluetoothAdapter.getRemoteDevice(cleanHost)
            } catch (e: Exception) {
                _lastErrorMessage.value = "Invalid Bluetooth device address: $cleanHost"
                _connectionStatus.value = ConnectionStatus.ERROR
                return@withContext
            }

            // Cancel discovery before connecting for high-speed socket handshake
            try {
                bluetoothAdapter.cancelDiscovery()
                _isDiscovering.value = false
            } catch (_: Exception) {}

            // Comprehensive Multi-Tier RFCOMM Socket Connection Fallback:
            var connectedSocket: BluetoothSocket? = null
            var lastConnectException: Exception? = null

            // Tier 1: Insecure RFCOMM to iTantra dedicated UUID
            try {
                val sock = targetDevice.createInsecureRfcommSocketToServiceRecord(ITANTRA_UUID)
                sock.connect()
                connectedSocket = sock
            } catch (e: Exception) {
                lastConnectException = e
            }

            // Tier 2: Secure RFCOMM to iTantra dedicated UUID
            if (connectedSocket == null) {
                try {
                    val sock = targetDevice.createRfcommSocketToServiceRecord(ITANTRA_UUID)
                    sock.connect()
                    connectedSocket = sock
                } catch (e: Exception) {
                    lastConnectException = e
                }
            }

            // Tier 3: Insecure RFCOMM to Standard SPP UUID
            if (connectedSocket == null) {
                try {
                    val sock = targetDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    sock.connect()
                    connectedSocket = sock
                } catch (e: Exception) {
                    lastConnectException = e
                }
            }

            // Tier 4: Secure RFCOMM to Standard SPP UUID
            if (connectedSocket == null) {
                try {
                    val sock = targetDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                    sock.connect()
                    connectedSocket = sock
                } catch (e: Exception) {
                    lastConnectException = e
                }
            }

            // Tier 5: Reflection fallback for channel 1 (works across stubborn Android devices)
            if (connectedSocket == null) {
                try {
                    val method = targetDevice.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                    val sock = method.invoke(targetDevice, 1) as BluetoothSocket
                    sock.connect()
                    connectedSocket = sock
                } catch (e: Exception) {
                    try {
                        val method = targetDevice.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        val sock = method.invoke(targetDevice, 1) as BluetoothSocket
                        sock.connect()
                        connectedSocket = sock
                    } catch (e2: Exception) {
                        lastConnectException = e2
                    }
                }
            }

            if (connectedSocket == null) {
                throw lastConnectException ?: Exception("Could not establish RFCOMM channel with $cleanHost")
            }

            activeSocket = connectedSocket
            outputStream = DataOutputStream(connectedSocket.outputStream)
            inputStream = DataInputStream(connectedSocket.inputStream)
            _connectionStatus.value = ConnectionStatus.CONNECTED
            _lastErrorMessage.value = null

            CoroutineScope(Dispatchers.IO).launch {
                listenForPackets(inputStream!!)
            }
        } catch (e: Exception) {
            _lastErrorMessage.value = "Bluetooth connect failed: ${e.localizedMessage ?: "Device unreachable"}. Ensure peer has iTantra open with Bluetooth ON."
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    override suspend fun send(packet: TextPacket): Boolean = withContext(Dispatchers.IO) {
        val stream = outputStream
        if (stream == null || activeSocket?.isConnected != true) {
            _lastErrorMessage.value = "No active Bluetooth connection. Please connect first."
            return@withContext false
        }
        return@withContext try {
            val encodedFrame = ProtocolEncoder.encode(packet)
            stream.writeInt(encodedFrame.size)
            stream.write(encodedFrame)
            stream.flush()
            true
        } catch (e: Exception) {
            _lastErrorMessage.value = "Bluetooth send failed: ${e.localizedMessage}"
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        }
    }

    private suspend fun listenForPackets(stream: DataInputStream) {
        try {
            while (activeSocket?.isConnected == true) {
                val frameLength = try {
                    stream.readInt()
                } catch (e: Exception) {
                    break
                }
                if (frameLength <= 0 || frameLength > 1024 * 1024) break

                val frameBuffer = ByteArray(frameLength)
                stream.readFully(frameBuffer)

                val packet = ProtocolEncoder.decode(frameBuffer)
                _receivedPackets.emit(packet)
            }
        } catch (e: Exception) {
            _lastErrorMessage.value = "Bluetooth stream ended: ${e.localizedMessage}"
        } finally {
            if (!_isServerRunning.value) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } else {
                _connectionStatus.value = ConnectionStatus.CONNECTING
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        isListening = false
        _isServerRunning.value = false
        try {
            inputStream?.close()
            outputStream?.close()
            activeSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
        serverSocket = null
        outputStream = null
        inputStream = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<Pair<String, String>> {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return emptyList()
        return try {
            bluetoothAdapter.bondedDevices.map { device ->
                (device.name ?: "Unknown Device") to device.address
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return false
        _discoveredDevices.value = emptyList()
        _isDiscovering.value = true
        return try {
            bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
        } catch (_: Exception) {
            _isDiscovering.value = false
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery() {
        _isDiscovering.value = false
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (_: Exception) {}
    }

    fun onDeviceDiscovered(name: String?, address: String) {
        val currentList = _discoveredDevices.value.toMutableList()
        val displayName = if (name.isNullOrBlank()) "Nearby Device ($address)" else name
        if (currentList.none { it.second.equals(address, ignoreCase = true) }) {
            currentList.add(displayName to address)
            _discoveredDevices.value = currentList
        }
    }
}

