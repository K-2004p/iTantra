package com.itantra.app.domain.communication

enum class PriorityLevel(val value: Byte) {
    NORMAL(0),
    IMPORTANT(1),
    EMERGENCY(2);

    companion object {
        fun fromByte(b: Byte): PriorityLevel = entries.find { it.value == b } ?: NORMAL
    }
}

enum class MessageType(val value: Byte) {
    TEXT_MESSAGE(0),
    ACK(1),
    PING(2),
    EMERGENCY_ALERT(3);

    companion object {
        fun fromByte(b: Byte): MessageType = entries.find { it.value == b } ?: TEXT_MESSAGE
    }
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class ConnectionType {
    LOCAL_WIFI,
    WIFI_DIRECT,
    BLUETOOTH
}

/**
 * Compact binary-encodable text packet structure optimized for low bitrate links.
 */
data class TextPacket(
    val messageId: String,
    val sequenceNumber: Int,
    val senderName: String,
    val language: String, // Source language (e.g. "mr")
    val targetLanguage: String = "", // Intended destination language (e.g. "hi")
    val priority: PriorityLevel = PriorityLevel.NORMAL,
    val messageType: MessageType = MessageType.TEXT_MESSAGE,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: String,
    val checksum: Long = 0L
)
