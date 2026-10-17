package com.arbonvata.kidstrackeractivity.presentation

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Snapshot of the three permissions reminders depend on. Battery optimization
 * is advisory only — without it alarms can be delayed under Doze but still
 * fire — so [remindersBlocked] deliberately ignores it: only the Settings rows
 * surface it.
 */
data class ReminderPermissions(
    val exactAlarmGranted: Boolean,
    val notificationsGranted: Boolean,
    val batteryOptimizationDisabled: Boolean,
) {
    /** True when reminders cannot fire at all: exact alarms or notifications missing. */
    val remindersBlocked: Boolean get() = !exactAlarmGranted || !notificationsGranted
}

/** Reads the live system state of every permission reminders depend on. */
fun Context.checkReminderPermissions(): ReminderPermissions =
    ReminderPermissions(
        exactAlarmGranted = getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        notificationsGranted = areNotificationsGranted(),
        batteryOptimizationDisabled =
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
    )

/** Below Android 13 notifications need no runtime permission. */
fun Context.areNotificationsGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Permission state that refreshes on every ON_RESUME — e.g. after returning
 * from a system settings screen where a permission was flipped. An observer
 * registered while the lifecycle is already RESUMED is brought up to the
 * current state, so the initial read is live as well.
 */
@Composable
fun rememberReminderPermissions(): ReminderPermissions {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(context.checkReminderPermissions()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) permissions = context.checkReminderPermissions()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return permissions
}

/** The hosting [android.app.Activity], found through any ContextWrapper chain. */
fun Context.findActivity(): android.app.Activity? =
    when (this) {
        is android.app.Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/** Opens the system screen holding this app's exact-alarm toggle. */
fun exactAlarmRequestIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())

/** Opens the system dialog asking to exempt this app from battery optimizations. */
fun batteryOptimizationRequestIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())

class ReminderPermissionRequests(
    /** Starts the guided chain for every permission reminders still lack. */
    val requestAllMissing: () -> Unit,
)

/**
 * A guided grant chain covering all three permission steps: notifications
 * (the only one with an in-app dialog), then the exact-alarm screen and the
 * battery-optimization dialog — special-access permissions have no in-app
 * dialog, so those steps are brief system surfaces focused on this app that
 * the user confirms and returns from. Each step skips anything already
 * granted, and each callback checks the live system state before launching
 * the next one; [rememberReminderPermissions] picks the final state up on
 * the ON_RESUME that follows.
 */
@Composable
fun rememberReminderPermissionRequests(): ReminderPermissionRequests {
    val context = LocalContext.current

    val requestBattery =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val requestExactAlarm =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val permissions = context.checkReminderPermissions()
            if (!permissions.batteryOptimizationDisabled) {
                requestBattery.launch(batteryOptimizationRequestIntent(context))
            }
        }
    val requestNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            val permissions = context.checkReminderPermissions()
            when {
                !permissions.exactAlarmGranted ->
                    requestExactAlarm.launch(exactAlarmRequestIntent(context))

                !permissions.batteryOptimizationDisabled ->
                    requestBattery.launch(batteryOptimizationRequestIntent(context))
            }
        }

    return ReminderPermissionRequests(
        requestAllMissing = {
            val permissions = context.checkReminderPermissions()
            when {
                !permissions.notificationsGranted ->
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

                !permissions.exactAlarmGranted ->
                    requestExactAlarm.launch(exactAlarmRequestIntent(context))

                // A required permission is missing by definition of being
                // started at all; battery optimization closes the chain.
                else -> requestBattery.launch(batteryOptimizationRequestIntent(context))
            }
        },
    )
}
