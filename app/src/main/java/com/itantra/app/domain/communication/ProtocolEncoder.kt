package com.itantra.app.domain.communication

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Encodes TextPacket objects into compact binary frames and decodes them on the receiver.
 * Uses zlib compression and CRC32 checksum for low bitrate radio access efficiency.
 */
object ProtocolEncoder {
    private const val HEADER_MAGIC: Short = 0x4954 // "IT" (iTantra magic header)

    /**
     * Compress string payload using zlib Deflater.
     */
    fun compressText(text: String): ByteArray {
        val input = text.toByteArray(StandardCharsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(input)
        deflater.finish()

        val outputStream = ByteArrayOutputStream(input.size)
        val buffer = ByteArray(256)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()
        return outputStream.toByteArray()
    }

    /**
     * Decompress string payload using zlib Inflater.
     */
    fun decompressText(compressedData: ByteArray): String {
        val inflater = Inflater()
        inflater.setInput(compressedData)

        val outputStream = ByteArrayOutputStream(compressedData.size * 2)
        val buffer = ByteArray(256)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            outputStream.write(buffer, 0, count)
        }
        inflater.end()
        return outputStream.toString(StandardCharsets.UTF_8.name())
    }

    /**
     * Calculate CRC32 checksum for packet header & payload bytes.
     */
    fun calculateChecksum(data: ByteArray): Long {
        val crc = CRC32()
        crc.update(data)
        return crc.value
    }

    /**
     * Encode TextPacket into binary Byte array frame.
     * Frame layout:
     * [2 bytes MAGIC] [4 bytes SEQ] [1 byte PRIORITY] [1 byte MSG_TYPE] [8 bytes TIMESTAMP]
     * [1 byte LANG_LEN] [LANG_BYTES] [1 byte SENDER_LEN] [SENDER_BYTES] [4 bytes PAYLOAD_LEN] [COMPRESSED_PAYLOAD] [4 bytes CHECKSUM]
     */
    fun encode(packet: TextPacket): ByteArray {
        val compressedPayload = compressText(packet.payload)
        val langBytes = packet.language.toByteArray(StandardCharsets.UTF_8)
        val senderBytes = packet.senderName.toByteArray(StandardCharsets.UTF_8)

        val bodySize = 2 + 4 + 1 + 1 + 8 + 1 + langBytes.size + 1 + senderBytes.size + 4 + compressedPayload.size
        val buffer = ByteBuffer.allocate(bodySize)

        buffer.putShort(HEADER_MAGIC)
        buffer.putInt(packet.sequenceNumber)
        buffer.put(packet.priority.value)
        buffer.put(packet.messageType.value)
        buffer.putLong(packet.timestamp)
        
        buffer.put(langBytes.size.toByte())
        buffer.put(langBytes)

        buffer.put(senderBytes.size.toByte())
        buffer.put(senderBytes)

        buffer.putInt(compressedPayload.size)
        buffer.put(compressedPayload)

        val bodyData = buffer.array()
        val checksum = calculateChecksum(bodyData)

        val finalFrame = ByteBuffer.allocate(bodySize + 4)
        finalFrame.put(bodyData)
        finalFrame.putInt(checksum.toInt())

        return finalFrame.array()
    }

    /**
     * Decode binary Byte array frame into TextPacket.
     * Validates CRC32 checksum.
     */
    fun decode(frame: ByteArray): TextPacket {
        require(frame.size >= 24) { "Frame size too small: ${frame.size} bytes" }

        val bodySize = frame.size - 4
        val bodyData = frame.copyOfRange(0, bodySize)
        
        val expectedChecksum = calculateChecksum(bodyData)
        val buffer = ByteBuffer.wrap(frame)
        buffer.position(bodySize)
        val actualChecksum = buffer.int.toLong() and 0xFFFFFFFFL

        require(expectedChecksum == actualChecksum) {
            "CRC32 Checksum mismatch! Expected $expectedChecksum but got $actualChecksum"
        }

        val readBuffer = ByteBuffer.wrap(bodyData)
        val magic = readBuffer.short
        require(magic == HEADER_MAGIC) { "Invalid magic header: $magic" }

        val seq = readBuffer.int
        val priority = PriorityLevel.fromByte(readBuffer.get())
        val msgType = MessageType.fromByte(readBuffer.get())
        val timestamp = readBuffer.long

        val langLen = readBuffer.get().toInt() and 0xFF
        val langBytes = ByteArray(langLen)
        readBuffer.get(langBytes)
        val language = String(langBytes, StandardCharsets.UTF_8)

        val senderLen = readBuffer.get().toInt() and 0xFF
        val senderBytes = ByteArray(senderLen)
        readBuffer.get(senderBytes)
        val senderName = String(senderBytes, StandardCharsets.UTF_8)

        val payloadLen = readBuffer.int
        val compressedPayload = ByteArray(payloadLen)
        readBuffer.get(compressedPayload)
        val payload = decompressText(compressedPayload)

        return TextPacket(
            messageId = "pkt_${seq}_${timestamp}",
            sequenceNumber = seq,
            senderName = senderName,
            language = language,
            priority = priority,
            messageType = msgType,
            timestamp = timestamp,
            payload = payload,
            checksum = actualChecksum
        )
    }
}
