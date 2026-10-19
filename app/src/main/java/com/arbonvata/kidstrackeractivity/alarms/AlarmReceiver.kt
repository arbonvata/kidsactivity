package com.arbonvata.kidstrackeractivity.alarms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.arbonvata.kidstrackeractivity.MainActivity
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Handles the two alarm kinds: ACTION_ALARM posts the reminder notification,
 * ACTION_RESYNC re-runs the alarm sync (fired by the keeper alarm at the
 * edge of the computed 7-day horizon, so reminders keep themselves alive
 * without an app launch or reboot).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_RESYNC -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        GlobalContext.get().get<AlarmSyncCoordinator>().resyncOnce()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_ALARM -> postReminder(context, intent)
        }
    }

    /**
     * The notification text is formatted at fire time from the database, so
     * the locale current right now applies (the schedule-time copy used to
     * be English-only) and kid/activity renames made after scheduling show
     * up correctly.
     */
    private fun postReminder(
        context: Context,
        intent: Intent,
    ) {
        val activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, 0L)
        val kidId = intent.getLongExtra(EXTRA_KID_ID, 0L)
        val isSchool = intent.getBooleanExtra(EXTRA_IS_SCHOOL, false)
        val offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Single-row lookups; fast enough for onReceive's main thread.
        val store = GlobalContext.get().get<BoxStore>()
        val activity = store.boxFor(Activity::class.java).get(activityId)
        val kid = store.boxFor(Kid::class.java).get(kidId)
        // The reactive sync cancels deleted rows' alarms; if one slips
        // through there is nothing left to remind about.
        if (activity == null || kid == null) return

        val text =
            if (isSchool) {
                context.getString(R.string.notification_alarm_school_text_format, kid.name, activity.name, offsetMinutes)
            } else {
                context.getString(R.string.notification_alarm_activity_text_format, kid.name, activity.name, offsetMinutes)
            }
        NotificationChannels.ensure(context)
        val contentIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                // Tapping the notification opens the activity it belongs to;
                // MainActivity hands this to navController.handleDeepLink.
                Intent(
                    Intent.ACTION_VIEW,
                    "kidstracker://edit_activity/$activityId".toUri(),
                    context,
                    MainActivity::class.java,
                ),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(context, NotificationChannels.ACTIVITY_ALARMS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(
                    context.getString(
                        if (isSchool) R.string.notification_alarm_school_title else R.string.notification_alarm_activity_title,
                    ),
                ).setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    companion object {
        const val ACTION_ALARM = "com.arbonvata.kidstrackeractivity.action.ALARM"
        const val ACTION_RESYNC = "com.arbonvata.kidstrackeractivity.action.RESYNC"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_KID_ID = "extra_kid_id"
        const val EXTRA_IS_SCHOOL = "extra_is_school"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    }
}
