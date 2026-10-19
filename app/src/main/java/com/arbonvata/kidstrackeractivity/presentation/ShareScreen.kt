package com.arbonvata.kidstrackeractivity.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.BackupResult
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ShareViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

private const val BACKUP_FILE_NAME = "kidstracker-backup.json"

/**
 * Stateful entry point of the Backup & Share screen: owns the
 * [ShareViewModel], talks to the system file picker and share sheet, and
 * asks merge-vs-replace before restoring.
 */
@Composable
fun ShareScreen(
    viewModel: ShareViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val json = viewModel.exportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray())
                    }
                }
            }
        }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val json =
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                            reader.readText()
                        }
                    if (json != null) pendingImportJson = json
                }
            }
        }

    val backupSubject = stringResource(R.string.share_backup_subject)
    val shareChooserTitle = stringResource(R.string.share_backup_chooser_title)

    fun shareBackupFile() {
        scope.launch {
            val json = viewModel.exportBackup()
            val file =
                File(context.cacheDir, BACKUP_FILE_NAME).apply {
                    writeText(json)
                }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, backupSubject)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(intent, shareChooserTitle))
        }
    }

    ShareContent(
        isWorking = uiState.isWorking,
        onSaveToFileClick = { createDocumentLauncher.launch(BACKUP_FILE_NAME) },
        onShareFileClick = ::shareBackupFile,
        onImportClick = {
            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        },
        onBackClick = onBackClick,
    )

    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text(stringResource(R.string.share_restore_backup_title)) },
            text = {
                Text(stringResource(R.string.share_restore_backup_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportJson = null
                        viewModel.importBackup(json, replaceExisting = true)
                    },
                ) { Text(stringResource(R.string.share_replace)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImportJson = null
                        viewModel.importBackup(json, replaceExisting = false)
                    },
                ) { Text(stringResource(R.string.share_merge)) }
            },
        )
    }

    uiState.result?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::onResultHandled,
            title = {
                Text(
                    when (result) {
                        is BackupResult.Success -> stringResource(R.string.share_restore_complete_title)
                        is BackupResult.Error -> stringResource(R.string.share_restore_failed_title)
                    },
                )
            },
            text = {
                Text(
                    when (result) {
                        is BackupResult.Success ->
                            stringResource(R.string.share_restore_complete_message)
                        is BackupResult.Error -> stringResource(R.string.share_restore_failed_message)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onResultHandled) { Text(stringResource(R.string.share_ok)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareContent(
    isWorking: Boolean,
    onSaveToFileClick: () -> Unit,
    onShareFileClick: () -> Unit,
    onImportClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.share_back))
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.share_header_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.share_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }

            Text(
                stringResource(R.string.share_section_backup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ShareOptionRow(
                icon = Icons.Default.Save,
                title = stringResource(R.string.share_save_to_file_title),
                subtitle = stringResource(R.string.share_save_to_file_subtitle),
                onClick = onSaveToFileClick,
            )
            ShareOptionRow(
                icon = Icons.Default.Share,
                title = stringResource(R.string.share_share_backup_title),
                subtitle = stringResource(R.string.share_share_backup_subtitle),
                onClick = onShareFileClick,
            )

            Text(
                stringResource(R.string.share_section_restore),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ShareOptionRow(
                icon = Icons.Default.Download,
                title = stringResource(R.string.share_restore_from_file_title),
                subtitle = stringResource(R.string.share_restore_from_file_subtitle),
                onClick = onImportClick,
            )

            if (isWorking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.share_restoring),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun ShareOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, name = "Backup — idle")
@Composable
fun ShareContentPreview() {
    KidsTrackerActivityTheme {
        ShareContent(
            isWorking = false,
            onSaveToFileClick = {},
            onShareFileClick = {},
            onImportClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Restore — in progress")
@Composable
fun ShareContentWorkingPreview() {
    KidsTrackerActivityTheme {
        ShareContent(
            isWorking = true,
            onSaveToFileClick = {},
            onShareFileClick = {},
            onImportClick = {},
            onBackClick = {},
        )
    }
}
