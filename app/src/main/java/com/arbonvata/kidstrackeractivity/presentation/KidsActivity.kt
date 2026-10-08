package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    kidId: Int,
    onSaveClick: (name: String, startTime: String, endTime: String, days: List<Int>, isPeriodic: Boolean) -> Unit,
) {
    var activityName by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(18, 30)) }
    var selectedDays by remember { mutableStateOf(mutableListOf<Int>()) }

    // 1. NEW STATE FOR THE CHECKBOX
    var isPeriodic by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val daysOfWeek =
        listOf(
            "Mon" to 1,
            "Tue" to 2,
            "Wed" to 3,
            "Thu" to 4,
            "Fri" to 5,
            "Sat" to 6,
            "Sun" to 7,
        )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Activity") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Activity Name
            OutlinedTextField(
                value = activityName,
                onValueChange = { activityName = it },
                label = { Text("Activity Name (e.g., Football)") },
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Days of the Week (Chips)
            Text("Select Days:", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                daysOfWeek.forEach { (label, dayValue) ->
                    FilterChip(
                        selected = selectedDays.contains(dayValue),
                        onClick = {
                            if (selectedDays.contains(dayValue)) {
                                selectedDays.remove(dayValue)
                            } else {
                                selectedDays.add(dayValue)
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }

            // 3. NEW CHECKBOX UI
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = isPeriodic,
                    onCheckedChange = { isPeriodic = it },
                )
                Text(
                    text = "Periodic for the entire year",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            // 4. Time Pickers
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start: ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("End: ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
            }

            Spacer(Modifier.weight(1f))

            // 5. Save Button
            Button(
                onClick = {
                    if (activityName.isNotBlank() && selectedDays.isNotEmpty()) {
                        // Pass the new isPeriodic state to the save function
                        onSaveClick(
                            activityName,
                            startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            selectedDays.toList(),
                            isPeriodic, // <--- Passed here
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = activityName.isNotBlank() && selectedDays.isNotEmpty(),
            ) {
                Text("Save Activity")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddActivityScreenPreview() {
    KidsTrackerActivityTheme {
        AddActivityScreen(
            kidId = 1,
            onSaveClick = { string, string1, string2, ints, bool -> },
        )
    }
}
