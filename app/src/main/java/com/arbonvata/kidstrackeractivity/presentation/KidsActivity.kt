package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.EditActivityViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Stateful entry point of the Add Activity screen: owns the
 * [AddActivityViewModel] (parameterised with the kidId from the route) and
 * turns the one-shot `isSaved` flag into navigation.
 */
@Composable
fun AddActivityScreen(
    kidId: Long,
    viewModel: AddActivityViewModel = koinViewModel(parameters = { parametersOf(kidId) }),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val kids by viewModel.kids.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedHandled()
            onNavigateBack()
        }
    }

    AddActivityContent(
        uiState = uiState,
        kids = kids,
        isKidPickerEnabled = kidId == 0L,
        onKidSelected = viewModel::onKidSelected,
        onNameChange = viewModel::onNameChange,
        onToggleDay = viewModel::onToggleDay,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onPeriodicChange = viewModel::onPeriodicChange,
        onSchoolActivityChange = viewModel::onSchoolActivityChange,
        onSaveClick = viewModel::saveActivity,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateful entry point of the Edit Activity screen: loads the activity into
 * the shared Add form, saves via updateActivity (preserving id, kid, enable
 * flag and one-off anchor), deletes with confirmation, and turns the
 * one-shot isSaved/isDeleted flags into navigation.
 */
@Composable
fun EditActivityScreen(
    activityId: Long,
    viewModel: EditActivityViewModel = koinViewModel(parameters = { parametersOf(activityId) }),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val kids by viewModel.kids.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedHandled()
            onNavigateBack()
        }
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            viewModel.onDeletedHandled()
            onNavigateBack()
        }
    }

    val form = uiState.form
    when {
        uiState.isNotFound -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Activity not found")
            }
        }

        form == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            AddActivityContent(
                uiState = form,
                kids = kids,
                isKidPickerEnabled = false,
                onKidSelected = {},
                onNameChange = viewModel::onNameChange,
                onToggleDay = viewModel::onToggleDay,
                onStartTimeChange = viewModel::onStartTimeChange,
                onEndTimeChange = viewModel::onEndTimeChange,
                onPeriodicChange = viewModel::onPeriodicChange,
                onSchoolActivityChange = viewModel::onSchoolActivityChange,
                onSaveClick = viewModel::saveActivity,
                onNavigateBack = onNavigateBack,
                screenTitle = "Edit Activity",
                onDeleteClick = { showDeleteDialog = true },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete activity?") },
            text = { Text("The activity and its reminders will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteActivity()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityContent(
    uiState: AddActivityUiState,
    kids: List<Kid>,
    isKidPickerEnabled: Boolean,
    onKidSelected: (Long) -> Unit,
    onNameChange: (String) -> Unit,
    onToggleDay: (Int) -> Unit,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit,
    onPeriodicChange: (Boolean) -> Unit,
    onSchoolActivityChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    screenTitle: String = "Add Activity",
    onDeleteClick: (() -> Unit)? = null,
) {
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
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    onDeleteClick?.let { onDelete ->
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isKidPickerEnabled) {
                KidPickerField(
                    kids = kids,
                    selectedKidId = uiState.selectedKidId,
                    onKidSelected = onKidSelected,
                )
            } else {
                kids.firstOrNull { it.id == uiState.selectedKidId }?.let { kid ->
                    Text(
                        "For ${kid.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text("Activity Name (e.g., Football)") },
                isError = uiState.nameError,
                supportingText = {
                    if (uiState.nameError) {
                        Text("Please enter an activity name")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Select Days:", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                daysOfWeek.forEach { (label, dayValue) ->
                    FilterChip(
                        selected = dayValue in uiState.selectedDays,
                        onClick = { onToggleDay(dayValue) },
                        label = { Text(label) },
                    )
                }
            }
            if (uiState.daysError) {
                Text(
                    "Select at least one day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // School activities are always weekly, so the periodic toggle is
            // meaningless for them (checking School Activity forces it on).
            if (!uiState.isSchoolActivity) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = uiState.isPeriodic,
                        onCheckedChange = onPeriodicChange,
                    )
                    Text(
                        text = "Periodic for the entire year",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = uiState.isSchoolActivity,
                    onCheckedChange = onSchoolActivityChange,
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "School Activity",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Repeats weekly · reminds after it ends instead of before it starts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start: ${uiState.startTime.format(timeFormat)}")
                }
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("End: ${uiState.endTime.format(timeFormat)}")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedKidId != 0L && uiState.name.isNotBlank() && uiState.selectedDays.isNotEmpty(),
            ) {
                Text("Save Activity")
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = uiState.startTime,
            onConfirm = {
                onStartTimeChange(it)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
    }
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = uiState.endTime,
            onConfirm = {
                onEndTimeChange(it)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidPickerField(
    kids: List<Kid>,
    selectedKidId: Long,
    onKidSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedKid = kids.firstOrNull { it.id == selectedKidId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedKid?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kid") },
            supportingText =
                if (kids.isEmpty()) {
                    {
                        Text("No kids yet — add a kid first")
                    }
                } else {
                    null
                },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            kids.forEach { kid ->
                DropdownMenuItem(
                    text = { Text(kid.name) },
                    onClick = {
                        onKidSelected(kid.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = timePickerState) },
    )
}

@Preview(showBackground = true, name = "Add — empty form")
@Composable
fun AddActivityContentPreview() {
    KidsTrackerActivityTheme {
        AddActivityContent(
            uiState = AddActivityUiState(),
            kids = DebugTestData.mockKids,
            isKidPickerEnabled = true,
            onKidSelected = {},
            onNameChange = {},
            onToggleDay = {},
            onStartTimeChange = {},
            onEndTimeChange = {},
            onPeriodicChange = {},
            onSchoolActivityChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Add — both flags checked")
@Composable
fun AddActivityContentCheckedPreview() {
    KidsTrackerActivityTheme {
        AddActivityContent(
            uiState =
                AddActivityUiState(
                    name = "Football",
                    selectedDays = setOf(1, 3, 5),
                    isPeriodic = true,
                    isSchoolActivity = true,
                ),
            kids = DebugTestData.mockKids,
            isKidPickerEnabled = true,
            onKidSelected = {},
            onNameChange = {},
            onToggleDay = {},
            onStartTimeChange = {},
            onEndTimeChange = {},
            onPeriodicChange = {},
            onSchoolActivityChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Edit — filled school activity with delete")
@Composable
fun EditActivityContentPreview() {
    KidsTrackerActivityTheme {
        AddActivityContent(
            uiState =
                AddActivityUiState(
                    selectedKidId = 1L,
                    name = "School",
                    selectedDays = setOf(1, 3, 5),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(14, 0),
                    isPeriodic = true,
                    isSchoolActivity = true,
                ),
            kids = DebugTestData.mockKids,
            isKidPickerEnabled = false,
            onKidSelected = {},
            onNameChange = {},
            onToggleDay = {},
            onStartTimeChange = {},
            onEndTimeChange = {},
            onPeriodicChange = {},
            onSchoolActivityChange = {},
            onSaveClick = {},
            onNavigateBack = {},
            screenTitle = "Edit Activity",
            onDeleteClick = {},
        )
    }
}
