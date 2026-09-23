package com.itantra.app.domain.emergency

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.itantra.app.domain.communication.PriorityLevel
import com.itantra.app.domain.communication.TextPacket
import com.itantra.app.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages high-decibel audible chimes, haptic vibration alerts, and Android system notifications
 * on the Receiver phone whenever incoming transmissions are detected across Wi-Fi or Bluetooth.
 * Designed for emergency responders & field operators in noisy disaster zones.
 */
class LoudAlertManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var activeToneGenerator: ToneGenerator? = null

    companion object {
        const val CHANNEL_ID_EMERGENCY = "itantra_emergency_channel"
        const val CHANNEL_ID_MESSAGES = "itantra_messages_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Emergency Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority audible alerts for disaster and emergency messages"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 150, 400, 150, 700)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Incoming Transmissions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audible notifications for incoming P2P text and voice messages"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager?.createNotificationChannel(emergencyChannel)
            notificationManager?.createNotificationChannel(messageChannel)
        }
    }

    suspend fun playLoudIncomingAlert(packet: TextPacket) = withContext(Dispatchers.IO) {
        try {
            // 1. Post High-Priority System Heads-Up Notification
            postSystemNotification(packet)

            // 2. Trigger High-Decibel Haptic Vibration
            triggerVibration(packet.priority)

            // 3. Play Loud Audible Chime / Tone
            playAudibleAlert(packet.priority)
        } catch (_: Exception) {}
    }

    suspend fun playLoudIncomingAlert(priority: PriorityLevel) = withContext(Dispatchers.IO) {
        try {
            triggerVibration(priority)
            playAudibleAlert(priority)
        } catch (_: Exception) {}
    }

    fun postSystemNotification(packet: TextPacket) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = if (packet.priority == PriorityLevel.EMERGENCY) CHANNEL_ID_EMERGENCY else CHANNEL_ID_MESSAGES
            val title = when (packet.priority) {
                PriorityLevel.EMERGENCY -> "🚨 EMERGENCY ALERT from ${packet.senderName}"
                PriorityLevel.IMPORTANT -> "⚠️ IMPORTANT MESSAGE from ${packet.senderName}"
                PriorityLevel.NORMAL    -> "📩 Incoming Message from ${packet.senderName}"
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(packet.payload)
                .setStyle(NotificationCompat.BigTextStyle().bigText(packet.payload))
                .setPriority(
                    if (packet.priority == PriorityLevel.EMERGENCY) NotificationCompat.PRIORITY_MAX
                    else NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    if (packet.priority == PriorityLevel.EMERGENCY) NotificationCompat.CATEGORY_ALARM
                    else NotificationCompat.CATEGORY_MESSAGE
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            NotificationManagerCompat.from(context).notify(packet.sequenceNumber, builder.build())
        } catch (_: Exception) {}
    }

    private fun triggerVibration(priority: PriorityLevel) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val pattern = when (priority) {
            PriorityLevel.EMERGENCY -> longArrayOf(0, 400, 100, 400, 100, 700)
            PriorityLevel.IMPORTANT -> longArrayOf(0, 300, 100, 300)
            PriorityLevel.NORMAL    -> longArrayOf(0, 250, 80, 250)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = when (priority) {
                PriorityLevel.EMERGENCY -> intArrayOf(0, 255, 0, 255, 0, 255)
                PriorityLevel.IMPORTANT -> intArrayOf(0, 230, 0, 230)
                PriorityLevel.NORMAL    -> intArrayOf(0, 200, 0, 200)
            }
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun playAudibleAlert(priority: PriorityLevel) {
        try {
            when (priority) {
                PriorityLevel.EMERGENCY -> {
                    // Maximum volume boost for Emergency stream
                    val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 15
                    audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

                    try {
                        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(context, alertUri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ringtone.audioAttributes = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        }
                        ringtone.play()
                    } catch (_: Exception) {
                        activeToneGenerator?.release()
                        activeToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                        activeToneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                    }
                }
                PriorityLevel.IMPORTANT -> {
                    // Ensure notification stream is audible
                    val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 5
                    val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 15
                    if (curVol < maxVol / 2) {
                        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (maxVol * 0.8).toInt(), 0)
                    }

                    try {
                        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(context, alertUri)
                        ringtone.play()
                    } catch (_: Exception) {
                        activeToneGenerator?.release()
                        activeToneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                        activeToneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 500)
                    }
                }
                PriorityLevel.NORMAL -> {
                    // Crisp loud chime
                    try {
                        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(context, alertUri)
                        ringtone.play()
                    } catch (_: Exception) {
                        activeToneGenerator?.release()
                        activeToneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
                        activeToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
