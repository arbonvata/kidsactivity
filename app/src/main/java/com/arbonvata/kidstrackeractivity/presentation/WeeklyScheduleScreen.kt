package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import androidx.compose.ui.tooling.preview.Preview
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    kids: List<Kid>,
    activities: List<Activity>,
    onEditActivityClick: (Activity) -> Unit
) {
    // Calculate this week's Monday and Sunday
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    // Expand and group by Kid
    val occurrences = expandActivitiesForDateRange(kids, activities, weekStart, weekEnd)
    val groupedByKid = occurrences.groupBy { it.activity.kidId }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("This Week: ${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d"))}")
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (kids.isEmpty()) {
                item { Text("Add a kid to get started!", modifier = Modifier.padding(16.dp)) }
            }

            items(kids) { kid ->
                val kidOccurrences = groupedByKid[kid.id] ?: emptyList()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Kid Name Header
                        Text(
                            text = kid.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (kidOccurrences.isEmpty()) {
                            Text(
                                "No activities this week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        } else {
                            // List activities for this kid
                            kidOccurrences.forEach { occurrence ->
                                WeeklyActivityRow(
                                    occurrence = occurrence,
                                    dateFormatter = dateFormatter,
                                    onClick = { onEditActivityClick(occurrence.activity) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyActivityRow(
    occurrence: ActivityOccurrence,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day and Date
            Text(
                text = occurrence.date.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(90.dp)
            )

            // Time
            Text(
                text = occurrence.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(50.dp)
            )

            // Activity Name
            Text(
                text = occurrence.activity.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyScheduleScreenPreview() {
    KidsTrackerActivityTheme {
        val kids = listOf(
            Kid(id = 1, name = "Alice", defaultEndTime = "18:00"),
            Kid(id = 2, name = "Bob", defaultEndTime = "18:00")
        )
        val activities = listOf(
            Activity(
                id = 1,
                kidId = 1,
                name = "Soccer Practice",
                startTime = "15:00",
                endTime = "16:30",
                isEnabled = true,
                daysOfWeek = intArrayOf(1, 3, 5)
            ),
            Activity(
                id = 2,
                kidId = 2,
                name = "Piano Lesson",
                startTime = "17:00",
                endTime = "18:00",
                isEnabled = true,
                daysOfWeek = intArrayOf(2, 4)
            )
        )
        WeeklyScheduleScreen(
            kids = kids,
            activities = activities,
            onEditActivityClick = {}
        )
    }
}