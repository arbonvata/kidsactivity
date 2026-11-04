package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.CalendarShareViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Shares the persisted activity, including its recurrence and individual-date exceptions. */
@Composable
fun ActivityCalendarShare(
    activityId: Long,
    viewModel: CalendarShareViewModel = koinViewModel(key = "activity-calendar:$activityId"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val chooserTitle = stringResource(R.string.calendar_share_activity)
    OutlinedButton(
        enabled = !state.isWorking,
        onClick = {
            scope.launch {
                try {
                    val calendar = viewModel.exportActivity(activityId) ?: return@launch
                    shareCalendarFile(context, calendar, chooserTitle, "activity.ics")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    viewModel.reportFailure(e)
                }
            }
        },
    ) { Text(chooserTitle) }
    Text(stringResource(R.string.calendar_share_activity_hint))
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text(stringResource(R.string.calendar_error)) },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.share_ok)) } },
        )
    }
}
