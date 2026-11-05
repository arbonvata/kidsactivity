package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityUiState
import kotlin.math.roundToInt

@Composable
fun ActivityReminderFields(
    form: AddActivityUiState,
    onCustomChange: (Boolean) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onPickupChange: (Boolean) -> Unit,
    onPickupLeadChange: (Int) -> Unit,
) {
    Text(stringResource(R.string.activity_reminders_title), style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(form.hasCustomReminder, onCustomChange)
        Text(stringResource(R.string.activity_custom_reminder))
    }
    if (form.hasCustomReminder) {
        Text(
            stringResource(
                if (form.isSchoolActivity) R.string.activity_minutes_after_school else R.string.activity_minutes_before_start,
                form.reminderMinutes,
            ),
        )
        Slider(form.reminderMinutes.toFloat(), { onMinutesChange(it.roundToInt()) }, valueRange = 0f..120f, steps = 23)
    } else {
        Text(stringResource(R.string.activity_global_reminder_hint))
    }
    if (!form.isSchoolActivity) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(form.hasPickupReminder, onPickupChange)
            Text(stringResource(R.string.activity_pickup_reminder))
        }
        if (form.hasPickupReminder) {
            Text(stringResource(R.string.activity_minutes_before_pickup, form.pickupLeadMinutes))
            Slider(form.pickupLeadMinutes.toFloat(), { onPickupLeadChange(it.roundToInt()) }, valueRange = 0f..60f, steps = 11)
        }
    }
}
