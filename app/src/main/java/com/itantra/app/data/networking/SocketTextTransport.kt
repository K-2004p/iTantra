package com.itantra.app.data.networking

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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Concrete implementation of TextTransport using offline TCP Sockets over Local Wi-Fi / Hotspot / Wi-Fi Direct.
 */
class SocketTextTransport : TextTransport {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun observeConnectionStatus(): StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<TextPacket>(extraBufferCapacity = 64)
    override fun observeReceivedPackets(): Flow<TextPacket> = _receivedPackets.asSharedFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _serverPort = MutableStateFlow(8888)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isListening = false

    override suspend fun startServer(port: Int): Unit = withContext(Dispatchers.IO) {
        try {
            disconnect()
            _serverPort.value = port
            _lastErrorMessage.value = null
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress(port))
            serverSocket = ss
            isListening = true
            _isServerRunning.value = true

            // Continuous listening loop to accept clients (and re-accept if client reconnects)
            CoroutineScope(Dispatchers.IO).launch {
                while (isListening && serverSocket?.isClosed == false) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        socket.tcpNoDelay = true
                        socket.keepAlive = true
                        
                        // Close previous active socket if any
                        activeSocket?.close()
                        activeSocket = socket
                        outputStream = DataOutputStream(socket.getOutputStream())
                        inputStream = DataInputStream(socket.getInputStream())
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        _lastErrorMessage.value = null

                        listenForPackets(inputStream!!)
                    } catch (e: Exception) {
                        if (isListening && serverSocket?.isClosed == false) {
                            _lastErrorMessage.value = "Server listen notice: ${e.localizedMessage}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _lastErrorMessage.value = "Failed to start server on port $port: ${e.localizedMessage}"
            _connectionStatus.value = ConnectionStatus.ERROR
            _isServerRunning.value = false
        }
    }

    override suspend fun connectToServer(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        try {
            disconnect()
            _lastErrorMessage.value = null
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            val cleanHost = host.trim()
            if (cleanHost.isEmpty()) {
                _lastErrorMessage.value = "Host IP cannot be empty"
                _connectionStatus.value = ConnectionStatus.ERROR
                return@withContext
            }

            val socket = Socket()
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.connect(InetSocketAddress(cleanHost, port), 5000) // 5s timeout

            activeSocket = socket
            outputStream = DataOutputStream(socket.getOutputStream())
            inputStream = DataInputStream(socket.getInputStream())
            _connectionStatus.value = ConnectionStatus.CONNECTED
            _lastErrorMessage.value = null

            CoroutineScope(Dispatchers.IO).launch {
                listenForPackets(inputStream!!)
            }
        } catch (e: Exception) {
            _lastErrorMessage.value = "Cannot connect to $host:$port (${e.javaClass.simpleName}: ${e.localizedMessage ?: "Check IP address & ensure Receiver Server is Started"})"
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    override suspend fun send(packet: TextPacket): Boolean = withContext(Dispatchers.IO) {
        val stream = outputStream
        if (stream == null || activeSocket?.isConnected != true || activeSocket?.isClosed == true) {
            _lastErrorMessage.value = "No active connection. Please connect first."
            return@withContext false
        }
        return@withContext try {
            val encodedFrame = ProtocolEncoder.encode(packet)
            stream.writeInt(encodedFrame.size)
            stream.write(encodedFrame)
            stream.flush()
            true
        } catch (e: Exception) {
            _lastErrorMessage.value = "Send failed: ${e.localizedMessage}"
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        }
    }

    private suspend fun listenForPackets(stream: DataInputStream) {
        try {
            while (activeSocket?.isConnected == true && activeSocket?.isClosed == false) {
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
            _lastErrorMessage.value = "Connection closed: ${e.localizedMessage}"
        } finally {
            if (!_isServerRunning.value) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } else {
                // If this is a server, keep status as CONNECTING (waiting for next connection)
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

    companion object {
        const val DEFAULT_HOTSPOT_IP = "192.168.43.1"
        const val DEFAULT_SERVER_PORT = 8888

        /**
         * Discovers local IP address, prioritizing Hotspot (ap*, softap*, swlan*, wlan*) and Wi-Fi interfaces.
         */
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return DEFAULT_HOTSPOT_IP
                val interfaceList = interfaces.toList()

                // Priority 1: Hotspot & SoftAP interfaces (e.g. ap0, softap0, swlan0, rndis0, tether)
                val hotspotIps = interfaceList
                    .filter { it.name.startsWith("ap", ignoreCase = true) || 
                              it.name.startsWith("softap", ignoreCase = true) ||
                              it.name.startsWith("swlan", ignoreCase = true) ||
                              it.name.startsWith("tether", ignoreCase = true) ||
                              it.name.startsWith("rndis", ignoreCase = true) ||
                              it.name.contains("p2p", ignoreCase = true) }
                    .flatMap { it.inetAddresses.toList() }
                    .filter { !it.isLoopbackAddress && it is InetAddress && !it.hostAddress.orEmpty().contains(':') }
                if (hotspotIps.isNotEmpty()) return hotspotIps.first().hostAddress ?: DEFAULT_HOTSPOT_IP

                // Priority 2: Wi-Fi / Ethernet interfaces (wlan0, wlan1, eth0)
                val wifiIps = interfaceList
                    .filter { it.name.startsWith("wlan", ignoreCase = true) || 
                              it.name.startsWith("eth", ignoreCase = true) }
                    .flatMap { it.inetAddresses.toList() }
                    .filter { !it.isLoopbackAddress && it is InetAddress && !it.hostAddress.orEmpty().contains(':') }
                if (wifiIps.isNotEmpty()) return wifiIps.first().hostAddress ?: DEFAULT_HOTSPOT_IP

                // Priority 3: Any non-loopback IPv4
                val anyIps = interfaceList
                    .flatMap { it.inetAddresses.toList() }
                    .filter { !it.isLoopbackAddress && it is InetAddress && !it.hostAddress.orEmpty().contains(':') }
                if (anyIps.isNotEmpty()) return anyIps.first().hostAddress ?: DEFAULT_HOTSPOT_IP

            } catch (_: Exception) {}
            return DEFAULT_HOTSPOT_IP
        }

        /**
         * Returns all detected IPv4 addresses with their interface names (e.g. wlan0, ap0).
         */
        fun getAllLocalIpAddresses(): List<Pair<String, String>> {
            val result = mutableListOf<Pair<String, String>>()
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
                for (intf in interfaces) {
                    for (addr in intf.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is InetAddress) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotEmpty() && !host.contains(':')) {
                                val label = when {
                                    intf.name.startsWith("ap", ignoreCase = true) || intf.name.startsWith("softap", ignoreCase = true) -> "Hotspot (${intf.name})"
                                    intf.name.startsWith("wlan", ignoreCase = true) -> "Wi-Fi (${intf.name})"
                                    else -> intf.displayName ?: intf.name
                                }
                                result.add(label to host)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return result
        }
    }
}

