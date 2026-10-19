package com.arbonvata.kidstrackeractivity.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** Syncs the desired set of upcoming alarms into AlarmManager. */
interface AlarmScheduler {
    /**
     * Cancels alarms no longer wanted, then (re)schedules [alarms] and a
     * rolling "keeper" resync alarm (see
     * [AlarmManagerAlarmScheduler]). Suspends because the set of live
     * request codes is persisted via DataStore, which has no synchronous
     * API.
     */
    suspend fun syncAlarms(alarms: List<ScheduledAlarm>)

    suspend fun cancelAll()
}

class AlarmManagerAlarmScheduler(
    private val context: Context,
    private val codesStore: ScheduledAlarmCodesStore,
) : AlarmScheduler {
    /** Serializes syncs: the app-start collector and the boot/keeper resync can overlap. */
    private val mutex = Mutex()

    /** Codes known to be live in AlarmManager; null until loaded from disk. */
    private var scheduledRequestCodes: Set<Int>? = null

    override suspend fun syncAlarms(alarms: List<ScheduledAlarm>) =
        mutex.withLock {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                // No new exact alarms can be set, but the queued ones must
                // not keep firing stale data (e.g. for activities deleted
                // while the permission was missing) — cancel everything and
                // keep only the keeper, so reminders recover by themselves
                // once the permission is granted again.
                val queued = rememberScheduledRequestCodes()
                Log.w(TAG, "Exact alarms not granted; cancelling ${queued.size} queued alarms")
                cancelQueuedActivityAlarms(alarmManager)
                scheduleKeeperAlarm(alarmManager)
                return@withLock
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
            scheduleKeeperAlarm(alarmManager)
        }

    override suspend fun cancelAll() =
        mutex.withLock {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            cancelQueuedActivityAlarms(alarmManager)
            alarmManager.cancel(keeperPendingIntent())
        }

    /**
     * The computed alarms only cover the coordinator's horizon (7 days), so
     * without intervention they would silently stop once a week passes
     * without an app launch or reboot. One inexact keeper alarm fires just
     * before the horizon expires and re-runs the sync (see AlarmReceiver's
     * ACTION_RESYNC), which schedules the next window and the keeper again —
     * reminders keep themselves alive. Inexact on purpose: no extra
     * permission needed, and the horizon's remaining day absorbs doze delay.
     */
    private fun scheduleKeeperAlarm(alarmManager: AlarmManager) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + KEEPER_INTERVAL_MS,
            keeperPendingIntent(),
        )
    }

    private suspend fun cancelQueuedActivityAlarms(alarmManager: AlarmManager) {
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
                .putExtra(AlarmReceiver.EXTRA_KID_ID, alarm?.kidId ?: 0L)
                .putExtra(AlarmReceiver.EXTRA_IS_SCHOOL, alarm?.isSchoolActivity ?: false)
                .putExtra(AlarmReceiver.EXTRA_OFFSET_MINUTES, alarm?.offsetMinutes ?: 0)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun keeperPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            KEEPER_REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_RESYNC),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val TAG = "AlarmScheduler"

        /** One day before the 7-day horizon of computed alarms expires. */
        const val KEEPER_INTERVAL_MS = 6L * 24 * 60 * 60 * 1000

        /** Activity alarm codes are always >= 10_000 (activityId >= 1), so 0 is free. */
        const val KEEPER_REQUEST_CODE = 0
    }
}
