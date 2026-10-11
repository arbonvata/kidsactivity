package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScheduleScreen(
    kids: List<Kid>,
    activities: List<Activity>,
    selectedKidIds: Set<Long>, // <--- NEW PARAMETER
    onEditActivityClick: (Activity) -> Unit,
    onBackClick: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Filter kids and activities to only those selected
    val filteredKids = kids.filter { it.id in selectedKidIds }
    val filteredActivities = activities.filter { it.kidId in selectedKidIds && it.isEnabled }

    // Get occurrences for the selected date (only for selected kids)
    val occurrences =
        expandActivitiesForDateRange(
            filteredKids,
            filteredActivities,
            selectedDate,
            selectedDate,
        )

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = selectedDate.format(dateFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${filteredKids.joinToString(", ") { it.name }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (occurrences.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No activities scheduled for this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(occurrences, key = { "${it.activity.id}_${it.date}" }) { occurrence ->
                    DailyActivityCard(
                        occurrence = occurrence,
                        onClick = { onEditActivityClick(occurrence.activity) },
                    )
                }
            }
        }
    }
}

@Composable
fun DailyActivityCard(
    occurrence: ActivityOccurence,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp),
            ) {
                Text(
                    text = occurrence.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = occurrence.activity.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Activity and Kid info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.activity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = occurrence.kidName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyScheduleScreenPreview() {
    KidsTrackerActivityTheme {
        DailyScheduleScreen(
            kids = DebugTestData.mockKids,
            activities = DebugTestData.mockActivities,
            selectedKidIds = setOf(1L, 2L),
            onEditActivityClick = {},
            onBackClick = {},
        )
    }
}
