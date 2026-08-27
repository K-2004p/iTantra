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

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isListening = false

    override suspend fun startServer(port: Int): Unit = withContext(Dispatchers.IO) {
        try {
            disconnect()
            _connectionStatus.value = ConnectionStatus.CONNECTING
            serverSocket = ServerSocket(port)
            isListening = true

            // Wait for client connection on background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val socket = serverSocket?.accept() ?: return@launch
                    activeSocket = socket
                    outputStream = DataOutputStream(socket.getOutputStream())
                    inputStream = DataInputStream(socket.getInputStream())
                    _connectionStatus.value = ConnectionStatus.CONNECTED

                    listenForPackets(inputStream!!)
                } catch (e: Exception) {
                    if (isListening) {
                        _connectionStatus.value = ConnectionStatus.ERROR
                    }
                }
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    override suspend fun connectToServer(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        try {
            disconnect()
            _connectionStatus.value = ConnectionStatus.CONNECTING
            val socket = Socket(host, port)
            activeSocket = socket
            outputStream = DataOutputStream(socket.getOutputStream())
            inputStream = DataInputStream(socket.getInputStream())
            _connectionStatus.value = ConnectionStatus.CONNECTED

            CoroutineScope(Dispatchers.IO).launch {
                listenForPackets(inputStream!!)
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    override suspend fun send(packet: TextPacket): Boolean = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext false
        return@withContext try {
            val encodedFrame = ProtocolEncoder.encode(packet)
            stream.writeInt(encodedFrame.size)
            stream.write(encodedFrame)
            stream.flush()
            true
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        }
    }

    private suspend fun listenForPackets(stream: DataInputStream) {
        try {
            while (activeSocket?.isConnected == true && !activeSocket!!.isClosed) {
                val frameLength = stream.readInt()
                if (frameLength <= 0 || frameLength > 1024 * 1024) break

                val frameBuffer = ByteArray(frameLength)
                stream.readFully(frameBuffer)

                val packet = ProtocolEncoder.decode(frameBuffer)
                _receivedPackets.emit(packet)
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        isListening = false
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
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is InetAddress) {
                            val ip = addr.hostAddress ?: ""
                            if (ip.indexOf(':') < 0) return ip
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
