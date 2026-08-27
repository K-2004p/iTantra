package com.itantra.app.domain.communication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport abstraction for sending and receiving compressed text packets over offline P2P radio/wireless links.
 */
interface TextTransport {
    /**
     * Start local server (Receiver Mode) listening for incoming connections.
     */
    suspend fun startServer(port: Int = 8888)

    /**
     * Connect to local server (Sender Mode).
     */
    suspend fun connectToServer(host: String, port: Int = 8888)

    /**
     * Transmit text packet.
     * @return true if successfully transmitted and acknowledged
     */
    suspend fun send(packet: TextPacket): Boolean

    /**
     * Stream of received text packets.
     */
    fun observeReceivedPackets(): Flow<TextPacket>

    /**
     * Current connection state.
     */
    fun observeConnectionStatus(): StateFlow<ConnectionStatus>

    /**
     * Disconnect and release socket/wireless resources.
     */
    suspend fun disconnect()
}
