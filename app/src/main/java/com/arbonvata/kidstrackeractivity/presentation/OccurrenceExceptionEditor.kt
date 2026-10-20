package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.OccurrenceException
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityUiState
import java.time.LocalDate
import java.time.LocalTime

/** Exceptions are staged alongside the form and committed by its Save button. */
@Composable
fun OccurrenceExceptionEditor(
    form: AddActivityUiState,
    onChange: (OccurrenceException) -> Unit,
    onRemove: (LocalDate) -> Unit,
) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var start by remember { mutableStateOf<LocalTime?>(null) }
    var end by remember { mutableStateOf<LocalTime?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    Text(stringResource(R.string.occurrence_title))
    Text(stringResource(R.string.occurrence_hint))
    form.exceptions.sortedBy { it.date }.forEach { exception ->
        Column {
            Text(
                if (exception.start == null) {
                    stringResource(R.string.occurrence_cancelled, exception.date.format(weekdayDateFormatter))
                } else {
                    "${exception.date.format(weekdayDateFormatter)}: ${exception.start}–${exception.end}"
                },
            )
            TextButton(onClick = { onRemove(exception.date) }) { Text(stringResource(R.string.occurrence_reset)) }
        }
    }
    OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
        Text(date?.format(weekdayDateFormatter) ?: stringResource(R.string.occurrence_choose_date))
    }
    date?.let { selected ->
        val matches =
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
