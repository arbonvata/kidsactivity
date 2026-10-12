package com.arbonvata.kidstrackeractivity.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val ACTIVITY_ALARMS = "activity_alarms"

    /** Idempotent; safe to call at app start and from the receiver. */
    fun ensure(context: Context) {
        val channel =
            NotificationChannel(
                ACTIVITY_ALARMS,
                "Activity reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Reminders before activities start and after school ends" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
