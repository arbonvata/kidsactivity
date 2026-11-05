package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.SchoolHolidayRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SchoolOverridesViewModel
import java.time.LocalDate

@Composable
fun SchoolHolidayControls(
    viewModel: SchoolOverridesViewModel,
    kids: List<Kid>,
    selectedId: Long?,
) {
    val state by viewModel.holidayState.collectAsState()
    var first by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var last by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var allKids by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val start = LocalDate.parse(first)
    val end = LocalDate.parse(last)
    val valid = runCatching { SchoolHolidayRange(start, end) }.isSuccess
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.holiday_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.holiday_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { picker = "start" }, enabled = !state.isWorking) {
                Text(stringResource(R.string.holiday_from, start.format(weekdayDateFormatter)))
            }
            OutlinedButton(onClick = { picker = "end" }, enabled = !state.isWorking) {
                Text(stringResource(R.string.holiday_to, end.format(weekdayDateFormatter)))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(allKids, {
                    allKids = it
                    viewModel.clearHolidayResult()
                }, enabled = !state.isWorking)
                Text(stringResource(R.string.holiday_all_kids))
            }
            if (!valid) Text(stringResource(R.string.holiday_invalid_range), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { confirmation = true }, enabled = valid && !state.isWorking) {
                Text(stringResource(R.string.holiday_apply))
            }
            TextButton(onClick = { confirmation = false }, enabled = valid && !state.isWorking) {
                Text(stringResource(R.string.holiday_restore))
            }
            if (state.isWorking) CircularProgressIndicator()
            if (state.error) Text(stringResource(R.string.holiday_error), color = MaterialTheme.colorScheme.error)
            if (state.completed) Text(stringResource(R.string.holiday_done))
        }
    }
    picker?.let { field ->
        WeekPickerDialog(if (field == "start") start else end, { date ->
            if (field == "start") {
                first = date.toString()
                if (end.isBefore(date)) last = first
            } else {
                last = date.toString()
            }
            viewModel.clearHolidayResult()
            picker = null
        }, { picker = null })
    }
    confirmation?.let { noSchool ->
        val targets = kids.filter { allKids || it.id == selectedId }
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(stringResource(if (noSchool) R.string.holiday_apply else R.string.holiday_restore)) },
            text = {
                Text(
                    stringResource(
                        R.string.holiday_confirm,
                        start.format(
                            weekdayDateFormatter,
                        ),
                        end.format(weekdayDateFormatter),
                        targets.joinToString {
                            it.name
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(enabled = targets.isNotEmpty(), onClick = {
                    confirmation = null
                    viewModel.setHolidayRange(start, end, allKids, noSchool)
                }) { Text(stringResource(R.string.share_ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmation = null },
                ) { Text(stringResource(R.string.add_activity_date_picker_cancel)) }
            },
        )
    }
}
