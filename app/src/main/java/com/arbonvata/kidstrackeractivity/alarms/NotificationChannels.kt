package com.arbonvata.kidstrackeractivity.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.arbonvata.kidstrackeractivity.R

object NotificationChannels {
    const val ACTIVITY_ALARMS = "activity_reminder_alarms"

    // Channels are immutable once created: v1 left the sound implicit, v2
    // used the system default NOTIFICATION sound (which is often set to
    // "None" on MIUI). v3 sounds on the alarm stream instead.
    private const val LEGACY_SILENT_CHANNEL = "activity_alarms"
    private const val LEGACY_NOTIFICATION_CHANNEL = "activity_reminders"

    /** Idempotent; safe to call at app start and from the receiver. */
    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LEGACY_SILENT_CHANNEL)
        manager.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL)
        if (manager.getNotificationChannel(ACTIVITY_ALARMS) != null) return

        val channel =
            NotificationChannel(
                ACTIVITY_ALARMS,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                // The alarm stream keeps reminders audible even when the
                // system default notification sound is "None" (common on
                // MIUI) or the notification stream is muted.
                val soundUri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (soundUri != null) {
                    setSound(
                        soundUri,
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
        manager.createNotificationChannel(channel)
    }
}
