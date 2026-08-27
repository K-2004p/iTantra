package com.itantra.app.domain.emergency

import com.itantra.app.domain.communication.TextPacket

/**
 * Manages priority message queuing and emergency alert overrides.
 */
class EmergencyAlertManager {
    private var activeEmergencyPacket: TextPacket? = null

    fun isEmergencyActive(): Boolean = activeEmergencyPacket != null

    fun getActiveEmergency(): TextPacket? = activeEmergencyPacket

    fun triggerEmergency(packet: TextPacket) {
        activeEmergencyPacket = packet
    }

    fun dismissEmergency() {
        activeEmergencyPacket = null
    }
}
