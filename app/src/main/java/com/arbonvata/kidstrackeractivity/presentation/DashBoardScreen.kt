package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.TodayViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Stateful entry point of the Today tab: owns the [TodayViewModel] and
 * delegates rendering to the stateless [DashboardScreen].
 */
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = koinViewModel(),
    onAddActivityClick: () -> Unit,
    onViewWeekClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onActivityClick: (Activity) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            DashboardScreen(
                kids = uiState.kids,
                todayOccurrences = uiState.todayOccurrences,
                onAddActivityClick = onAddActivityClick,
                onViewWeekClick = onViewWeekClick,
                onSettingsClick = onSettingsClick,
                onActivityClick = onActivityClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    kids: List<Kid>,
    todayOccurrences: List<ActivityOccurence>,
    onAddActivityClick: () -> Unit,
    onViewWeekClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onActivityClick: (Activity) -> Unit,
) {
    val now = LocalTime.now()
    val nextActivity =
        todayOccurrences
            .filter { it.startTime.isAfter(now) }
            .minByOrNull { it.startTime }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivityClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Date Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${todayOccurrences.size} activities today",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Next Activity Highlight
            if (nextActivity != null) {
                val minutesUntil = Duration.between(now, nextActivity.startTime).toMinutes()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Up next: ${nextActivity.activity.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "For ${nextActivity.kidName} • in $minutesUntil min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            // Today's Activities
            Text(
                "Today's Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (todayOccurrences.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                ) {
                    Text(
                        "🎉 No activities today!",
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                todayOccurrences.forEach { occurrence ->
                    val alarmsDisabled = !occurrence.activity.isEnabled
                    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    OutlinedCard(
                        onClick = { onActivityClick(occurrence.activity) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                occurrence.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (alarmsDisabled) dimmed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(60.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    occurrence.activity.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (alarmsDisabled) dimmed else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    occurrence.kidName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (alarmsDisabled) {
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = "Alarms disabled",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            // Quick Action Button
            OutlinedButton(
                onClick = onViewWeekClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Full Week")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Today — mixed enabled and muted rows")
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        val today = LocalDate.now()
        // Hand-built so both row styles are always visible, regardless of
        // the real weekday.
        val todayOccurrences =
            listOf(
                ActivityOccurence(DebugTestData.mockActivities[0], "Leo", today, LocalTime.of(17, 0), LocalTime.of(18, 30)),
                ActivityOccurence(DebugTestData.mockActivities[2], "Leo", today, LocalTime.of(15, 0), LocalTime.of(16, 0)),
            )
        DashboardScreen(
            kids = DebugTestData.mockKids,
            todayOccurrences = todayOccurrences,
            onAddActivityClick = {},
            onViewWeekClick = {},
            onSettingsClick = {},
            onActivityClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Today — no activities")
@Composable
fun DashboardScreenEmptyPreview() {
    MaterialTheme {
        DashboardScreen(
            kids = DebugTestData.mockKids,
            todayOccurrences = emptyList(),
            onAddActivityClick = {},
            onViewWeekClick = {},
            onSettingsClick = {},
            onActivityClick = {},
        )
    }
}
