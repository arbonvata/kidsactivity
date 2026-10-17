package com.arbonvata.kidstrackeractivity.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

data class SettingsState(
    val schoolAlarmMinutes: Int = 10,
    val activityAlarmMinutes: Int = 15,
    // Pessimistic until the real system state is read, so the rows never
    // claim "Granted" before the first refresh.
    val exactAlarmGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
) {
    val remindersBlocked: Boolean get() = !exactAlarmGranted || !notificationsGranted
}

/**
 * Stateful entry point of the Settings screen: exposes the persisted alarm
 * offsets via [SettingsViewModel] (dragging updates local state; the value is
 * written on release), reads the real permission state from the system,
 * launches the matching request flows, and refreshes the statuses on every
 * resume (e.g. after returning from system settings).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val schoolMinutes by viewModel.schoolAlarmMinutes.collectAsState()
    val activityMinutes by viewModel.activityAlarmMinutes.collectAsState()
    var state by remember { mutableStateOf(SettingsState()) }

    // Adopt persisted offsets when they change outside a drag (e.g. on first
    // load); keyed on the flow value, so an in-progress drag is not fought.
    LaunchedEffect(schoolMinutes) { state = state.copy(schoolAlarmMinutes = schoolMinutes) }
    LaunchedEffect(activityMinutes) { state = state.copy(activityAlarmMinutes = activityMinutes) }

    fun refreshPermissions() {
        val permissions = context.checkReminderPermissions()
        state =
            state.copy(
                exactAlarmGranted = permissions.exactAlarmGranted,
                notificationsGranted = permissions.notificationsGranted,
                batteryOptimizationDisabled = permissions.batteryOptimizationDisabled,
            )
    }

    // After the system stops showing the permission dialog (repeated or
    // permanent denial) a plain request would silently do nothing, so "Grant"
    // routes to the app's notification settings instead from then on.
    var notificationsPermanentlyDenied by remember { mutableStateOf(false) }

    val requestNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val activity = context.findActivity()
            notificationsPermanentlyDenied =
                !granted &&
                activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            refreshPermissions()
        }
    val requestExactAlarm =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissions()
        }
    val requestIgnoreBatteryOpt =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissions()
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsContent(
        state = state,
        onSchoolAlarmChange = { state = state.copy(schoolAlarmMinutes = it) },
        onSchoolAlarmChangeFinished = { viewModel.onSchoolAlarmChange(state.schoolAlarmMinutes) },
        onActivityAlarmChange = { state = state.copy(activityAlarmMinutes = it) },
        onActivityAlarmChangeFinished = { viewModel.onActivityAlarmChange(state.activityAlarmMinutes) },
        onRequestExactAlarm = {
            requestExactAlarm.launch(exactAlarmRequestIntent(context))
        },
        onRequestNotifications = {
            when {
                notificationsPermanentlyDenied ->
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                // Below Android 13 notifications need no runtime permission.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDisableBatteryOpt = {
            requestIgnoreBatteryOpt.launch(batteryOptimizationRequestIntent(context))
        },
        onBackupClick = onBackupClick,
        onRestoreClick = onRestoreClick,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    onSchoolAlarmChange: (Int) -> Unit,
    onSchoolAlarmChangeFinished: () -> Unit,
    onActivityAlarmChange: (Int) -> Unit,
    onActivityAlarmChangeFinished: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Permissions Section
            SettingsSection(title = stringResource(R.string.settings_section_permissions)) {
                if (state.remindersBlocked) {
                    Text(
                        text = stringResource(R.string.settings_permissions_blocked_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                PermissionRow(
                    icon = Icons.Default.Alarm,
                    title = stringResource(R.string.settings_exact_alarms_title),
                    subtitle = stringResource(R.string.settings_exact_alarms_subtitle),
                    isGranted = state.exactAlarmGranted,
                    onClick = onRequestExactAlarm,
                )
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = stringResource(R.string.settings_notifications_subtitle),
                    isGranted = state.notificationsGranted,
                    onClick = onRequestNotifications,
                )
                PermissionRow(
                    icon = Icons.Default.BatteryFull,
                    title = stringResource(R.string.settings_battery_optimization_title),
                    subtitle = stringResource(R.string.settings_battery_optimization_subtitle),
                    isGranted = state.batteryOptimizationDisabled,
                    onClick = onDisableBatteryOpt,
                )
            }

            // Alarm Timing Section
            SettingsSection(title = stringResource(R.string.settings_section_alarm_timing)) {
                SettingsSlider(
                    title = stringResource(R.string.settings_school_end_reminder_title),
                    value = state.schoolAlarmMinutes,
                    valueRange = 5..30,
                    suffix = stringResource(R.string.settings_suffix_min_after),
                    onValueChange = onSchoolAlarmChange,
                    onValueChangeFinished = onSchoolAlarmChangeFinished,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSlider(
                    title = stringResource(R.string.settings_activity_reminder_title),
                    value = state.activityAlarmMinutes,
                    valueRange = 5..60,
                    suffix = stringResource(R.string.settings_suffix_min_before),
                    onValueChange = onActivityAlarmChange,
                    onValueChangeFinished = onActivityAlarmChangeFinished,
                )
            }

            // Data Section
            SettingsSection(title = stringResource(R.string.settings_section_data)) {
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = stringResource(R.string.settings_backup_data_title),
                    subtitle = stringResource(R.string.settings_backup_data_subtitle),
                    onClick = onBackupClick,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.settings_restore_data_title),
                    subtitle = stringResource(R.string.settings_restore_data_subtitle),
                    onClick = onRestoreClick,
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isGranted) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.settings_permission_granted),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        } else {
            TextButton(onClick = onClick) { Text(stringResource(R.string.settings_permission_grant)) }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Int,
    valueRange: IntRange,
    suffix: String,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_slider_value, value, suffix),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first) / 5 - 1,
        )
    }
}

@Preview(showBackground = true, name = "Permissions — one still pending")
@Composable
fun SettingsContentPreview() {
    MaterialTheme {
        SettingsContent(
            state =
                SettingsState(
                    schoolAlarmMinutes = 10,
                    activityAlarmMinutes = 15,
                    exactAlarmGranted = true,
                    notificationsGranted = false,
                    batteryOptimizationDisabled = true,
                ),
            onSchoolAlarmChange = {},
            onSchoolAlarmChangeFinished = {},
            onActivityAlarmChange = {},
            onActivityAlarmChangeFinished = {},
            onRequestExactAlarm = {},
            onRequestNotifications = {},
            onDisableBatteryOpt = {},
            onBackupClick = {},
            onRestoreClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Permissions — all granted")
@Composable
fun SettingsContentGrantedPreview() {
    MaterialTheme {
        SettingsContent(
            state =
                SettingsState(
                    schoolAlarmMinutes = 20,
                    activityAlarmMinutes = 30,
                    exactAlarmGranted = true,
                    notificationsGranted = true,
                    batteryOptimizationDisabled = true,
                ),
            onSchoolAlarmChange = {},
            onSchoolAlarmChangeFinished = {},
            onActivityAlarmChange = {},
            onActivityAlarmChangeFinished = {},
            onRequestExactAlarm = {},
            onRequestNotifications = {},
            onDisableBatteryOpt = {},
            onBackupClick = {},
            onRestoreClick = {},
            onBackClick = {},
        )
    }
}
