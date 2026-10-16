package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

@Composable
fun DeleteAllActivitiesButton(
    kidName: String,
    onDeleteClick: () -> Unit,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 1. The Button
    OutlinedButton(
        onClick = { showConfirmDialog = true },
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error, // Red text/icon
            ),
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.delete_all_activities_button))
    }

    // 2. The Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_all_activities_dialog_title)) },
            text = {
                Text(stringResource(R.string.delete_all_activities_dialog_message_format, kidName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick() // Trigger the ViewModel deletion
                        showConfirmDialog = false
                    },
                ) {
                    Text(stringResource(R.string.delete_all_activities_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.delete_all_activities_cancel))
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteAllActivitiesButtonPreview() {
    KidsTrackerActivityTheme {
        DeleteAllActivitiesButton(
            kidName = "Alex",
            onDeleteClick = {},
        )
    }
}
