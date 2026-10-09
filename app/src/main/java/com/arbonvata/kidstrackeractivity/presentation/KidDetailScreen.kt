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
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful entry point: owns the [ScheduleViewModel] and forwards the bulk
 * delete of the kid's activities to it.
 */
@Composable
fun KidDetailScreen(
    kidId: Int,
    kidName: String,
    viewModel: ScheduleViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    KidDetailContent(
        kidName = kidName,
        onDeleteAllClick = { viewModel.deleteAllActivitiesForKid(kidId) },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailContent(
    kidName: String,
    onDeleteAllClick: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("$kidName's Schedule") }) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ... list of activities for this kid ...

            Spacer(modifier = Modifier.weight(1f))

            DeleteAllActivitiesButton(
                kidName = kidName,
                onDeleteClick = onDeleteAllClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KidDetailContentPreview() {
    KidsTrackerActivityTheme {
        KidDetailContent(
            kidName = "Alex",
            onDeleteAllClick = {},
            onNavigateBack = {},
        )
    }
}
