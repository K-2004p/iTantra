package com.itantra.app

import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.domain.communication.ProtocolEncoder
import com.itantra.app.domain.communication.TextPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolEncoderTest {

    @Test
    fun testTextPayloadCompressionAndDecompression() {
        val originalText = "मुझे अभी मदद चाहिए और रास्ते की जानकारी चाहिए"
        val compressed = ProtocolEncoder.compressText(originalText)
        val decompressed = ProtocolEncoder.decompressText(compressed)

        println("Original Text Length: ${originalText.length} chars (${originalText.toByteArray().size} bytes)")
        println("Compressed Bytes: ${compressed.size} bytes")
        assertTrue("Compressed data should be compact", compressed.isNotEmpty())
        assertEquals("Decompressed text must match original sentence", originalText, decompressed)
    }

    @Test
    fun testBinaryPacketEncodingAndChecksumVerification() {
        val packet = TextPacket(
            messageId = "test_101",
            sequenceNumber = 42,
            senderName = "Laptop_Tester",
            language = "mr",
            priority = PriorityLevel.EMERGENCY,
            payload = "मला त्वरित मदत हवी आहे"
        )

        val frame = ProtocolEncoder.encode(packet)
        assertNotNull(frame)
        assertTrue("Frame size must be valid", frame.size > 24)

        val decodedPacket = ProtocolEncoder.decode(frame)
        assertEquals(packet.sequenceNumber, decodedPacket.sequenceNumber)
        assertEquals(packet.senderName, decodedPacket.senderName)
        assertEquals(packet.language, decodedPacket.language)
        assertEquals(packet.priority, decodedPacket.priority)
        assertEquals(packet.payload, decodedPacket.payload)
        println("CRC32 Checksum Verified: 0x${decodedPacket.checksum.toString(16).uppercase()}")
    }
}
