package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.OccurrenceException
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityUiState
import java.time.LocalDate
import java.time.LocalTime

/** Exceptions are staged alongside the form and committed by its Save button. */
@Composable
fun OccurrenceExceptionEditor(
    form: AddActivityUiState,
    onChange: (OccurrenceException) -> Unit,
    onRemove: (LocalDate) -> Unit,
    /** Saved parents offered as one-tap choices for the delegation field. */
    parents: List<Parent> = emptyList(),
) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var start by remember { mutableStateOf<LocalTime?>(null) }
    var end by remember { mutableStateOf<LocalTime?>(null) }
    var delegatedName by remember { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    Text(stringResource(R.string.occurrence_title))
    Text(stringResource(R.string.occurrence_hint))
    form.exceptions.sortedBy { it.date }.forEach { exception ->
        Column {
            Text(
                when {
                    exception.isCancelled ->
                        stringResource(R.string.occurrence_cancelled, exception.date.format(weekdayDateFormatter))
                    exception.delegatedTo != null && exception.start != null ->
                        stringResource(
                            R.string.occurrence_delegated_times_on_format,
                            exception.date.format(weekdayDateFormatter),
                            exception.start!!.format(shortTimeFormatter),
                            exception.end!!.format(shortTimeFormatter),
                            exception.delegatedTo,
                        )
                    exception.delegatedTo != null ->
                        stringResource(
                            R.string.occurrence_delegated_on_format,
                            exception.date.format(weekdayDateFormatter),
                            exception.delegatedTo,
                        )
                    else -> "${exception.date.format(weekdayDateFormatter)}: ${exception.start}–${exception.end}"
                },
            )
            TextButton(onClick = { onRemove(exception.date) }) { Text(stringResource(R.string.occurrence_reset)) }
        }
    }
    OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
        Text(date?.format(weekdayDateFormatter) ?: stringResource(R.string.occurrence_choose_date))
    }
    date?.let { selected ->
        val withinEnd = form.lastDate == null || !selected.isAfter(form.lastDate)
        val withinStart =
            !(form.isPeriodic || form.isSchoolActivity) || form.calendarStartDate == null || !selected.isBefore(form.calendarStartDate)
        val matches =
            withinEnd &&
                withinStart &&
                selected.dayOfWeek.value in form.selectedDays &&
                (
                    form.isPeriodic ||
                        form.isSchoolActivity ||
                        (!selected.isBefore(form.firstDate) && selected.isBefore(form.firstDate.plusDays(7)))
                )
        if (!matches) {
            Text(stringResource(R.string.occurrence_no_activity))
        } else {
            TextButton(onClick = {
                onChange(OccurrenceException(selected))
                date = null
            }) {
                Text(stringResource(R.string.occurrence_cancel))
            }
            OutlinedButton(onClick = { pickingStart = true }) {
                Text(stringResource(R.string.add_activity_start_time, (start ?: form.startTime).format(shortTimeFormatter)))
            }
            OutlinedButton(onClick = { pickingEnd = true }) {
                Text(stringResource(R.string.add_activity_end_time, (end ?: form.endTime).format(shortTimeFormatter)))
            }
            val effectiveStart = start ?: form.startTime
            val effectiveEnd = end ?: form.endTime
            Button(
                enabled = effectiveEnd.isAfter(effectiveStart),
                onClick = {
                    onChange(OccurrenceException(selected, effectiveStart, effectiveEnd))
                    date = null
                },
            ) { Text(stringResource(R.string.occurrence_change)) }
            if (!effectiveEnd.isAfter(effectiveStart)) Text(stringResource(R.string.add_activity_end_before_start_error))

            // Delegation: hand this date to someone else. Staged times compose
            // — untouched times keep the usual schedule, picked ones change it.
            if (parents.isNotEmpty()) {
                Text(
                    stringResource(R.string.occurrence_delegate_pick_parent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    parents.forEach { parent ->
                        FilterChip(
                            selected = delegatedName == parent.name,
                            onClick = { delegatedName = parent.name },
                            label = { Text(parent.name) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = delegatedName,
                onValueChange = { delegatedName = it },
                label = { Text(stringResource(R.string.occurrence_delegate_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = {
                // The name travels inside the pipe-delimited exception
                // encoding; strip anything that would corrupt it. Empty is
                // allowed — it means "someone, unnamed".
                val cleaned =
                    delegatedName
                        .replace("|", "")
                        .replace("\n", " ")
                        .trim()
                onChange(OccurrenceException(selected, start, end, delegatedTo = cleaned))
                date = null
            }) {
                Text(stringResource(R.string.occurrence_delegate))
            }
        }
    }
    if (pickingDate) {
        WeekPickerDialog(
            initialDate = date ?: LocalDate.now(),
            onConfirm = { selected ->
                date = selected
                val existing = form.exceptions.firstOrNull { it.date == selected }
                start = existing?.start
                end = existing?.end
                delegatedName = existing?.delegatedTo.orEmpty()
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
    if (pickingStart) {
        TimePickerDialog(start ?: form.startTime, {
            start = it
            pickingStart = false
        }, { pickingStart = false })
    }
    if (pickingEnd) {
        TimePickerDialog(end ?: form.endTime, {
            end = it
            pickingEnd = false
        }, { pickingEnd = false })
    }
}
