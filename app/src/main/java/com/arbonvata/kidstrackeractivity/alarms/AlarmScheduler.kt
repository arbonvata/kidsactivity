package com.arbonvata.kidstrackeractivity.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.ZoneId

/** Syncs the desired set of upcoming alarms into AlarmManager. */
interface AlarmScheduler {
    /** Cancels alarms no longer wanted, then (re)schedules [alarms]. */
    fun syncAlarms(alarms: List<ScheduledAlarm>)

    fun cancelAll()
}

class AlarmManagerAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {
    private var scheduledRequestCodes: Set<Int> = emptySet()

    override fun syncAlarms(alarms: List<ScheduledAlarm>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarms not granted; skipping ${alarms.size} alarms")
            return
        }

        val wantedRequestCodes = alarms.map { it.requestCode }.toSet()
        (scheduledRequestCodes - wantedRequestCodes).forEach { requestCode ->
            alarmManager.cancel(pendingIntent(requestCode))
        }
        alarms.forEach { alarm ->
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAt
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                pendingIntent(alarm.requestCode, alarm),
            )
        }
        scheduledRequestCodes = wantedRequestCodes
    }

    override fun cancelAll() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        scheduledRequestCodes.forEach { requestCode -> alarmManager.cancel(pendingIntent(requestCode)) }
        scheduledRequestCodes = emptySet()
    }

    private fun pendingIntent(
        requestCode: Int,
        alarm: ScheduledAlarm? = null,
    ): PendingIntent {
        val intent =
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_ALARM)
                .putExtra(AlarmReceiver.EXTRA_REQUEST_CODE, requestCode)
                .putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, alarm?.activityId ?: 0L)
                .putExtra(AlarmReceiver.EXTRA_TITLE, alarm?.title)
                .putExtra(AlarmReceiver.EXTRA_TEXT, alarm?.text)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
