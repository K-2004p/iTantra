package com.itantra.app.domain.emergency

import android.content.Context
import android.content.SharedPreferences
import com.itantra.app.BuildConfig

enum class AppRole(val title: String, val description: String) {
    SENDER("SENDER PHONE", "Push-to-Talk STT, Translation, Transmit to Receiver"),
    RECEIVER("RECEIVER PHONE", "Auto-Listen Server, Loud Notification Alerts, Auto-TTS"),
    TRANSCEIVER("FULL TRANSCEIVER", "Bidirectional Dual-Role Mode")
}

class AppRoleManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("itanta_role_prefs", Context.MODE_PRIVATE)

    fun getAppRole(): AppRole {
        val defaultRole = try {
            BuildConfig.DEFAULT_ROLE
        } catch (_: Throwable) {
            AppRole.TRANSCEIVER.name
        }
        val name = prefs.getString("selected_app_role", defaultRole) ?: defaultRole
        return try {
            AppRole.valueOf(name)
        } catch (_: Exception) {
            AppRole.TRANSCEIVER
        }
    }

    fun setAppRole(role: AppRole) {
        prefs.edit().putString("selected_app_role", role.name).apply()
    }
}
