package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

@Composable
fun DeleteAllActivitiesButton(
    kidName: String,
    onDeleteClick: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 1. The Button
    OutlinedButton(
        onClick = { showConfirmDialog = true },
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error // Red text/icon
        )
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete All Activities")
    }

    // 2. The Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete All Activities?") },
            text = {
                Text("Are you sure you want to delete all activities and cancel all reminders for $kidName? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick() // Trigger the ViewModel deletion
                        showConfirmDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteAllActivitiesButtonPreview() {
    KidsTrackerActivityTheme {
        DeleteAllActivitiesButton(
            kidName = "Alex",
            onDeleteClick = {}
        )
    }
}