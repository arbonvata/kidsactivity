package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.MonthViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Stateful entry point of the Month tab: owns the [MonthViewModel] and
 * delegates rendering to the stateless [MonthlyAgendaScreen].
 */
@Composable
fun MonthScreen(
    viewModel: MonthViewModel = koinViewModel(),
    onEditActivityClick: (Activity) -> Unit,
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
            MonthlyAgendaScreen(
                selectedMonth = uiState.selectedMonth,
                occurrences = uiState.monthOccurrences,
                onPreviousMonthClick = viewModel::previousMonth,
                onNextMonthClick = viewModel::nextMonth,
                onEditActivityClick = onEditActivityClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAgendaScreen(
    selectedMonth: YearMonth,
    occurrences: List<ActivityOccurrence>,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onEditActivityClick: (Activity) -> Unit,
) {
    val monthStart = selectedMonth.atDay(1)
    val monthEnd = selectedMonth.atEndOfMonth()
    val groupedByDate = occurrences.groupBy { it.date }

    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedMonth.format(monthFormatter))
                },
                navigationIcon = {
                    IconButton(onClick = onPreviousMonthClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.month_previous_month),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNextMonthClick) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.month_next_month))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Generate an item for every day in the month
            var currentDate = monthStart
            while (!currentDate.isAfter(monthEnd)) {
                val dateKey = currentDate

                // Sticky Header for the Date
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = dateKey.format(dayFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Items for that specific date
                val dayOccurrences = groupedByDate[dateKey] ?: emptyList()

                if (dayOccurrences.isEmpty()) {
                    item(key = "${dateKey}_empty") {
                        Text(
                            stringResource(R.string.month_no_activities),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    items(dayOccurrences, key = { "${it.activity.id}_${it.date}" }) { occurrence ->
                        MonthlyActivityRow(
                            occurrence = occurrence,
                            onClick = { onEditActivityClick(occurrence.activity) },
                        )
                    }
                }

                currentDate = currentDate.plusDays(1)
            }
        }
    }
}

@Composable
fun MonthlyActivityRow(
    occurrence: ActivityOccurrence,
    onClick: () -> Unit,
) {
    val alarmsDisabled = !occurrence.activity.isEnabled
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Kid Name (Since this view mixes all kids, we show whose activity it is)
            Text(
                text = occurrence.kidName,
                style = MaterialTheme.typography.labelLarge,
                color = if (alarmsDisabled) dimmed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(80.dp),
            )

            // Time
            Text(
                text = occurrence.startTime.format(shortTimeFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = if (alarmsDisabled) dimmed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(50.dp),
            )

            // Activity Name
            Text(
                text = occurrence.activity.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (alarmsDisabled) dimmed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (alarmsDisabled) {
                Icon(
                    Icons.Default.NotificationsOff,
                    contentDescription = stringResource(R.string.month_alarms_disabled),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Preview(showBackground = true, name = "Month — with activities (muted included)")
@Composable
fun MonthlyAgendaScreenPreview() {
    KidsTrackerActivityTheme {
        // A fixed month so the preview is deterministic.
        val selectedMonth = YearMonth.of(2026, 10)
        val occurrences =
            expandActivitiesForDateRange(
                DebugTestData.mockKids,
                DebugTestData.mockActivities,
                selectedMonth.atDay(1),
                selectedMonth.atEndOfMonth(),
                includeDisabled = true,
            )
        MonthlyAgendaScreen(
            selectedMonth = selectedMonth,
            occurrences = occurrences,
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onEditActivityClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Month — empty (no occurrences)")
@Composable
fun MonthlyAgendaScreenEmptyPreview() {
    KidsTrackerActivityTheme {
        MonthlyAgendaScreen(
            selectedMonth = YearMonth.of(2026, 10),
            occurrences = emptyList(),
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onEditActivityClick = {},
        )
    }
}
