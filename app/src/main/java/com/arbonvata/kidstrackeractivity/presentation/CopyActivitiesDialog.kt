package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ActivityCopyViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CopyActivitiesDialog(
    sourceKidId: Long,
    activityId: Long = 0L,
    onDismiss: () -> Unit,
    viewModel: ActivityCopyViewModel =
        koinViewModel(key = "copy:$sourceKidId:$activityId", parameters = { parametersOf(sourceKidId, activityId) }),
) {
    val state by viewModel.uiState.collectAsState()
    var pickingWeek by remember { mutableStateOf(false) }
    val close = {
        viewModel.resetResult()
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = { if (!state.isWorking) close() },
        title = { Text(stringResource(if (activityId == 0L) R.string.copy_schedule else R.string.copy_activity)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.copiedCount != null) {
                    Text(stringResource(R.string.copy_success, state.copiedCount ?: 0))
                } else if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.copy_hint))
                    Text(stringResource(R.string.copy_source_count, state.sourceCount))
                    if (state.kids.isEmpty()) {
                        Text(stringResource(R.string.copy_no_target))
                    } else {
                        KidPickerField(state.kids, state.selectedKidId, viewModel::selectKid)
                    }
                    if (activityId != 0L) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::changeName,
                            label = { Text(stringResource(R.string.add_activity_name_label)) },
                            isError = state.name.isBlank(),
                        )
                    }
                    TextButton(onClick = { pickingWeek = true }, enabled = !state.isWorking) {
                        Text(stringResource(R.string.copy_one_off_week, state.week.format(weekdayDateFormatter)))
                    }
                    if (state.error) Text(stringResource(R.string.copy_error))
                    if (state.isWorking) CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            if (state.copiedCount != null) {
                TextButton(onClick = close) { Text(stringResource(R.string.copy_done)) }
            } else {
                TextButton(
                    onClick = viewModel::copy,
                    enabled =
                        !state.isLoading &&
                            !state.isWorking &&
                            state.sourceCount > 0 &&
                            state.kids.any { it.id == state.selectedKidId } &&
                            (activityId == 0L || state.name.isNotBlank()),
                ) { Text(stringResource(R.string.copy_confirm)) }
            }
        },
        dismissButton = {
            if (state.copiedCount == null) {
                TextButton(onClick = close, enabled = !state.isWorking) { Text(stringResource(R.string.add_activity_date_picker_cancel)) }
            }
        },
    )
    if (pickingWeek) {
        WeekPickerDialog(state.week, {
            viewModel.changeWeek(it)
            pickingWeek = false
        }, { pickingWeek = false })
    }
}
