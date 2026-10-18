package com.arbonvata.kidstrackeractivity.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.ZoneId

/** Syncs the desired set of upcoming alarms into AlarmManager. */
interface AlarmScheduler {
    /**
     * Cancels alarms no longer wanted, then (re)schedules [alarms]. Suspends
     * because the set of live request codes is persisted via DataStore, which
     * has no synchronous API.
     */
    suspend fun syncAlarms(alarms: List<ScheduledAlarm>)

    suspend fun cancelAll()
}

class AlarmManagerAlarmScheduler(
    private val context: Context,
    private val codesStore: ScheduledAlarmCodesStore,
) : AlarmScheduler {
    /** Codes known to be live in AlarmManager; null until loaded from disk. */
    private var scheduledRequestCodes: Set<Int>? = null

    override suspend fun syncAlarms(alarms: List<ScheduledAlarm>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarms not granted; skipping ${alarms.size} alarms")
            return
        }

        val previouslyScheduled = rememberScheduledRequestCodes()
        val wantedRequestCodes = alarms.map { it.requestCode }.toSet()
        (previouslyScheduled - wantedRequestCodes).forEach { requestCode ->
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
        codesStore.saveRequestCodes(wantedRequestCodes)
    }

    override suspend fun cancelAll() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        rememberScheduledRequestCodes().forEach { requestCode -> alarmManager.cancel(pendingIntent(requestCode)) }
        scheduledRequestCodes = emptySet()
        codesStore.saveRequestCodes(emptySet())
    }

    /**
     * Caches the persisted code set after the first read this process: the
     * diff baseline must survive process death, because AlarmManager keeps
     * the alarms themselves.
     */
    private suspend fun rememberScheduledRequestCodes(): Set<Int> =
        scheduledRequestCodes ?: codesStore.loadRequestCodes().also { scheduledRequestCodes = it }

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
