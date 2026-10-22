package com.arbonvata.kidstrackeractivity.presentation

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.calendar.CalendarCodec
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.CalendarShareViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@Composable
fun CalendarSharingSection(viewModel: CalendarShareViewModel = koinViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val chooserTitle = stringResource(R.string.calendar_share)
    val fileName = "kids-activities.ics"
    val saveFile =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val calendar = viewModel.export() ?: return@launch
                        withContext(Dispatchers.IO) {
                            requireNotNull(context.contentResolver.openOutputStream(uri)) { "Unable to open calendar file" }
                                .use { it.write(calendar.toByteArray(Charsets.UTF_8)) }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        viewModel.reportFailure(e)
                    }
                }
            }
        }
    val openFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val text =
                            withContext(Dispatchers.IO) {
                                val stream =
                                    requireNotNull(context.contentResolver.openInputStream(uri)) {
                                        "Unable to open calendar file"
                                    }
                                stream.use { input ->
                                    val output = ByteArrayOutputStream()
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        require(
                                            output.size() + count <= CalendarCodec.MAX_BYTES,
                                        ) { "Calendar file is too large (maximum 2 MB)" }
                                        output.write(buffer, 0, count)
                                    }
                                    output.toString(Charsets.UTF_8.name())
                                }
                            }
                        viewModel.preview(text)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        viewModel.reportFailure(e)
                    }
                }
            }
        }
    Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.calendar_hint))
    Text(stringResource(R.string.calendar_supported), style = MaterialTheme.typography.bodySmall)
    KidPickerField(state.kids, state.exportKidId, viewModel::selectExportKid)
    Button(
        enabled = !state.isWorking && state.kids.any { it.id == state.exportKidId },
        onClick = {
            scope.launch {
                try {
                    val calendar = viewModel.export() ?: return@launch
                    val file =
                        withContext(Dispatchers.IO) {
                            File(context.cacheDir, "calendar-${UUID.randomUUID()}.ics").apply { writeText(calendar, Charsets.UTF_8) }
                        }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val send =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/calendar"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri(fileName, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    context.startActivity(Intent.createChooser(send, chooserTitle))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    viewModel.reportFailure(e)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.calendar_share)) }
    OutlinedButton(
        enabled = !state.isWorking && state.kids.any { it.id == state.exportKidId },
        onClick = { saveFile.launch(fileName) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.calendar_save)) }
    OutlinedButton(
        enabled = !state.isWorking,
        onClick = { openFile.launch(arrayOf("text/calendar", "application/ics", "text/*", "application/octet-stream")) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.calendar_import)) }
    if (state.isWorking) CircularProgressIndicator()
    state.pending?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text(stringResource(R.string.calendar_preview_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.calendar_preview_count, pending.activities.size))
                    Text(stringResource(R.string.calendar_import_hint))
                    KidPickerField(state.kids, state.targetKidId, viewModel::selectTargetKid)
                    TextButton(onClick = { viewModel.selectTargetKid(0) }, enabled = !state.isWorking) {
                        Text(stringResource(R.string.calendar_new_child))
                    }
                    if (state.targetKidId == 0L) {
                        OutlinedTextField(
                            value = state.newChildName,
                            onValueChange = viewModel::changeChildName,
                            label = { Text(stringResource(R.string.calendar_child_name)) },
                            singleLine = true,
                        )
                    }
                    pending.activities.take(5).forEach {
                        Text("${it.name} · ${it.startTime}–${it.endTime}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::importCalendar,
                    enabled = !state.isWorking && (state.targetKidId != 0L || state.newChildName.isNotBlank()),
                ) { Text(stringResource(R.string.calendar_import)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissImport, enabled = !state.isWorking) {
                    Text(stringResource(R.string.add_activity_date_picker_cancel))
                }
            },
        )
    }
    if (state.error != null || state.result != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text(stringResource(if (state.error != null) R.string.calendar_error else R.string.calendar_complete)) },
            text = {
                Text(
                    state.error ?: stringResource(R.string.calendar_import_result, state.result?.added ?: 0, state.result?.duplicates ?: 0),
                )
            },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.share_ok)) } },
        )
    }
}
