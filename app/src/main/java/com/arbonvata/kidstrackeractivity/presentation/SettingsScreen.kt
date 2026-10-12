package com.arbonvata.kidstrackeractivity.presentation

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

data class SettingsState(
    val schoolAlarmMinutes: Int = 10,
    val activityAlarmMinutes: Int = 15,
    val exactAlarmGranted: Boolean = true,
    val notificationsGranted: Boolean = true,
    val batteryOptimizationDisabled: Boolean = false,
)

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
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        state =
            state.copy(
                exactAlarmGranted = alarmManager.canScheduleExactAlarms(),
                notificationsGranted = areNotificationsGranted(context),
                batteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName),
            )
    }

    val requestNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
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
            requestExactAlarm.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
            )
        },
        onRequestNotifications = {
            // Below Android 13 notifications need no runtime permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDisableBatteryOpt = {
            requestIgnoreBatteryOpt.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri()),
            )
        },
        onBackupClick = onBackupClick,
        onRestoreClick = onRestoreClick,
        onBackClick = onBackClick,
    )
}

private fun areNotificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Permissions Section
            SettingsSection(title = "Permissions") {
                PermissionRow(
                    icon = Icons.Default.Alarm,
                    title = "Exact Alarms",
                    subtitle = "Required for precise timing",
                    isGranted = state.exactAlarmGranted,
                    onClick = onRequestExactAlarm,
                )
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Show reminders",
                    isGranted = state.notificationsGranted,
                    onClick = onRequestNotifications,
                )
                PermissionRow(
                    icon = Icons.Default.BatteryFull,
                    title = "Battery Optimization",
                    subtitle = "Disable to prevent delays",
                    isGranted = state.batteryOptimizationDisabled,
                    onClick = onDisableBatteryOpt,
                )
            }

            // Alarm Timing Section
            SettingsSection(title = "Alarm Timing") {
                SettingsSlider(
                    title = "School end reminder",
                    value = state.schoolAlarmMinutes,
                    valueRange = 5..30,
                    suffix = "min after",
                    onValueChange = onSchoolAlarmChange,
                    onValueChangeFinished = onSchoolAlarmChangeFinished,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSlider(
                    title = "Activity reminder",
                    value = state.activityAlarmMinutes,
                    valueRange = 5..60,
                    suffix = "min before",
                    onValueChange = onActivityAlarmChange,
                    onValueChangeFinished = onActivityAlarmChangeFinished,
                )
            }

            // Data Section
            SettingsSection(title = "Data") {
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "Backup Data",
                    subtitle = "Export to file",
                    onClick = onBackupClick,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Restore Data",
                    subtitle = "Import from backup",
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
            Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.tertiary)
        } else {
            TextButton(onClick = onClick) { Text("Grant") }
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
            Text("$value $suffix", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
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

@Preview(showBackground = true)
@Composable
fun SettingsContentPreview() {
    MaterialTheme {
        SettingsContent(
            state =
                SettingsState(
                    schoolAlarmMinutes = 10,
                    activityAlarmMinutes = 15,
                    exactAlarmGranted = true,
                    notificationsGranted = true,
                    batteryOptimizationDisabled = false,
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
