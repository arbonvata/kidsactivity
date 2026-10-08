package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class SettingsState(
    val schoolAlarmMinutes: Int = 10,
    val activityAlarmMinutes: Int = 15,
    val exactAlarmGranted: Boolean = true,
    val notificationsGranted: Boolean = true,
    val batteryOptimizationDisabled: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onSchoolAlarmChange: (Int) -> Unit,
    onActivityAlarmChange: (Int) -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Permissions Section
            SettingsSection(title = "Permissions") {
                PermissionRow(
                    icon = Icons.Default.Alarm,
                    title = "Exact Alarms",
                    subtitle = "Required for precise timing",
                    isGranted = state.exactAlarmGranted,
                    onClick = onRequestExactAlarm
                )
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Show reminders",
                    isGranted = state.notificationsGranted,
                    onClick = onRequestNotifications
                )
                PermissionRow(
                    icon = Icons.Default.BatteryFull,
                    title = "Battery Optimization",
                    subtitle = "Disable to prevent delays",
                    isGranted = state.batteryOptimizationDisabled,
                    onClick = onDisableBatteryOpt
                )
            }

            // Alarm Timing Section
            SettingsSection(title = "Alarm Timing") {
                SettingsSlider(
                    title = "School end reminder",
                    value = state.schoolAlarmMinutes,
                    valueRange = 5..30,
                    suffix = "min after",
                    onValueChange = onSchoolAlarmChange
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSlider(
                    title = "Activity reminder",
                    value = state.activityAlarmMinutes,
                    valueRange = 5..60,
                    suffix = "min before",
                    onValueChange = onActivityAlarmChange
                )
            }

            // Data Section
            SettingsSection(title = "Data") {
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "Backup Data",
                    subtitle = "Export to file",
                    onClick = onBackupClick
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Restore Data",
                    subtitle = "Import from backup",
                    onClick = onRestoreClick
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
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
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("$value $suffix", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first) / 5 - 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            state = SettingsState(
                schoolAlarmMinutes = 10,
                activityAlarmMinutes = 15,
                exactAlarmGranted = true,
                notificationsGranted = true,
                batteryOptimizationDisabled = false
            ),
            onSchoolAlarmChange = {},
            onActivityAlarmChange = {},
            onRequestExactAlarm = {},
            onRequestNotifications = {},
            onDisableBatteryOpt = {},
            onBackupClick = {},
            onRestoreClick = {},
            onBackClick = {}
        )
    }
}