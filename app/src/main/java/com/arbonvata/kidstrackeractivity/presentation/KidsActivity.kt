package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.EditActivityViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

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
        onFirstDateChange = viewModel::onFirstDateChange,
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
                Text(stringResource(R.string.edit_activity_not_found))
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
                onFirstDateChange = viewModel::onFirstDateChange,
                onSaveClick = viewModel::saveActivity,
                onNavigateBack = onNavigateBack,
                screenTitle = stringResource(R.string.edit_activity_title),
                onDeleteClick = { showDeleteDialog = true },
                extraContent = {
                    OccurrenceExceptionEditor(form, viewModel::onExceptionChange, viewModel::onExceptionRemoved)
                },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_activity_delete_dialog_title)) },
            text = { Text(stringResource(R.string.edit_activity_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteActivity()
                    },
                ) { Text(stringResource(R.string.edit_activity_delete_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.edit_activity_delete_dialog_cancel))
                }
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
    onFirstDateChange: (LocalDate) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    screenTitle: String = stringResource(R.string.add_activity_title),
    onDeleteClick: (() -> Unit)? = null,
    extraContent: @Composable () -> Unit = {},
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val weekdayAbbreviations = stringArrayResource(R.array.weekday_abbreviations)
    val daysOfWeek = weekdayAbbreviations.mapIndexed { index, label -> label to index + 1 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_activity_back_content_description),
                        )
                    }
                },
                actions = {
                    onDeleteClick?.let { onDelete ->
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.add_activity_delete_content_description),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onSaveClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                enabled = uiState.selectedKidId != 0L && uiState.name.isNotBlank() && uiState.selectedDays.isNotEmpty(),
            ) {
                Text(stringResource(R.string.add_activity_save))
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
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
                        stringResource(R.string.add_activity_for_kid, kid.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.add_activity_name_label)) },
                isError = uiState.nameError,
                supportingText = {
                    if (uiState.nameError) {
                        Text(stringResource(R.string.add_activity_name_error))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                stringResource(R.string.add_activity_select_days_label),
                style = MaterialTheme.typography.titleMedium,
            )
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
                    stringResource(R.string.add_activity_days_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            extraContent()

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
                        text = stringResource(R.string.add_activity_periodic_label),
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
                        text = stringResource(R.string.add_activity_school_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.add_activity_school_supporting_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // One-offs belong to a single calendar week, chosen with the
            // date picker; school activities are always weekly and never
            // reach here (their periodic flag stays on).
            if (!uiState.isPeriodic) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.add_activity_week_of_format,
                            uiState.firstDate.format(weekdayDateFormatter),
                        ),
                    )
                }
                Text(
                    text = stringResource(R.string.add_activity_one_off_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            R.string.add_activity_start_time,
                            uiState.startTime.format(shortTimeFormatter),
                        ),
                    )
                }
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            R.string.add_activity_end_time,
                            uiState.endTime.format(shortTimeFormatter),
                        ),
                    )
                }
            }
            if (uiState.timeError) {
                Text(
                    stringResource(R.string.add_activity_end_before_start_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
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
    if (showDatePicker) {
        WeekPickerDialog(
            initialDate = uiState.firstDate,
            onConfirm = {
                onFirstDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
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
            label = { Text(stringResource(R.string.add_activity_kid_label)) },
            supportingText =
                if (kids.isEmpty()) {
                    {
                        Text(stringResource(R.string.add_activity_no_kids_yet))
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
                Text(stringResource(R.string.add_activity_time_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_activity_time_picker_cancel))
            }
        },
        text = { TimePicker(state = timePickerState) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekPickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // Material3 date pickers work in UTC epoch millis at midnight; converting
    // through ZoneOffset.UTC keeps the picked calendar day intact.
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        ?.let(onConfirm)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.add_activity_date_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_activity_date_picker_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
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
            onFirstDateChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Add — one-off with week picker")
@Composable
fun AddActivityContentOneOffPreview() {
    KidsTrackerActivityTheme {
        AddActivityContent(
            uiState =
                AddActivityUiState(
                    name = "Dentist",
                    selectedDays = setOf(4),
                    isPeriodic = false,
                    firstDate = LocalDate.of(2026, 10, 19),
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
            onFirstDateChange = {},
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
            onFirstDateChange = {},
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
            onFirstDateChange = {},
            onSaveClick = {},
            onNavigateBack = {},
            screenTitle = stringResource(R.string.edit_activity_title),
            onDeleteClick = {},
        )
    }
}
