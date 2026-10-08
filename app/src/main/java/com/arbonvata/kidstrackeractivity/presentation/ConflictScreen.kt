package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import java.time.LocalDate
import java.time.LocalTime

data class Conflict(
    val activity1: ActivityOccurrence,
    val activity2: ActivityOccurrence,
    val overlapMinutes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictScreen(
    conflicts: List<Conflict>,
    onResolveClick: (Conflict) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Conflicts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (conflicts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No conflicts found!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "All activities are properly scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${conflicts.size} conflict${if (conflicts.size > 1) "s" else ""} detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                items(conflicts) { conflict ->
                    ConflictCard(conflict = conflict, onResolveClick = { onResolveClick(conflict) })
                }
            }
        }
    }
}

@Composable
fun ConflictCard(conflict: Conflict, onResolveClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${conflict.overlapMinutes} min overlap",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))

            ConflictActivityRow(occurrence = conflict.activity1, label = "Activity 1")
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            ConflictActivityRow(occurrence = conflict.activity2, label = "Activity 2")

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onResolveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Resolve Conflict")
            }
        }
    }
}

@Composable
fun ConflictActivityRow(occurrence: ActivityOccurrence, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(80.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${occurrence.startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))} - ${occurrence.activity.endTime}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(occurrence.activity.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(occurrence.kidName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConflictScreenPreview() {
    MaterialTheme {
        val activity1 = Activity(1, 1, "Football", "17:00", "18:30", true, intArrayOf(2))
        val activity2 = Activity(2, 1, "Piano", "17:30", "18:30", true, intArrayOf(2))
        val today = LocalDate.now()
        val conflicts = listOf(
            Conflict(
                ActivityOccurrence(activity1, "Leo", today, LocalTime.of(17, 0)),
                ActivityOccurrence(activity2, "Leo", today, LocalTime.of(17, 30)),
                overlapMinutes = 60
            )
        )
        ConflictScreen(conflicts = conflicts, onResolveClick = {}, onBackClick = {})
    }
}