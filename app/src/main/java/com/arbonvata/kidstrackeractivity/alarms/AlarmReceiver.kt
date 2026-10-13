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

/** Posts the reminder notification when an exact alarm fires. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
        val activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, 0L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    companion object {
        const val ACTION_ALARM = "com.arbonvata.kidstrackeractivity.action.ALARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
    }
}
