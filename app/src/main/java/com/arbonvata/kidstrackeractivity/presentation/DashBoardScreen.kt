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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
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
    onAddKidClick: () -> Unit,
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

        // First-run state: every activity belongs to a kid, so until one is
        // added the dashboard is replaced by an add-kid prompt.
        uiState.kids.isEmpty() -> {
            WelcomeNoKidsContent(onAddKidClick = onAddKidClick)
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

@Composable
fun WelcomeNoKidsContent(onAddKidClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.welcome_add_first_kid_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAddKidClick) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.welcome_add_kid))
            }
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
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.dashboard_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivityClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dashboard_add_activity))
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
                        text = pluralStringResource(R.plurals.today_activities_count, todayOccurrences.size, todayOccurrences.size),
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
                                stringResource(R.string.dashboard_up_next_format, nextActivity.activity.name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.dashboard_up_next_details_format, nextActivity.kidName, minutesUntil),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            // Today's Activities
            Text(
                stringResource(R.string.dashboard_today_schedule),
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
                        stringResource(R.string.today_no_activities),
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
                                occurrence.startTime.format(shortTimeFormatter),
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
                                    contentDescription = stringResource(R.string.dashboard_alarms_disabled),
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
                Text(stringResource(R.string.dashboard_view_full_week))
            }
        }
    }
}

@Preview(showBackground = true, name = "Today — no kids yet (welcome prompt)")
@Composable
fun WelcomeNoKidsContentPreview() {
    MaterialTheme {
        WelcomeNoKidsContent(onAddKidClick = {})
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
