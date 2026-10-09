package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ScheduleViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailScreen(
    kidId: Int,
    kidName: String,
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("$kidName's Schedule") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... Your existing list of activities for this kid ...

            Spacer(modifier = Modifier.weight(1f))

            // Add the Delete Button at the bottom
            DeleteAllActivitiesButton(
                kidName = kidName,
                onDeleteClick = {
                    viewModel.deleteAllActivitiesForKid(kidId)
                    // Optional: Navigate back or show a Snackbar
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KidDetailScreenPreview() {
    KidsTrackerActivityTheme {
        KidDetailScreen(
            kidId = 1,
            kidName = "Alex",
            viewModel = ScheduleViewModel(),
            onNavigateBack = {}
        )
    }
}