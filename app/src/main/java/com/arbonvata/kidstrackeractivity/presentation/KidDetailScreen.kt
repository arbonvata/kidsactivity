package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidDetailUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidDetailViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Stateful entry point of one kid's activity overview: owns the
 * [KidDetailViewModel] (scoped to this destination and parameterised with
 * the kidId from the route).
 */
@Composable
fun KidDetailScreen(
    kidId: Long,
    viewModel: KidDetailViewModel = koinViewModel(parameters = { parametersOf(kidId) }),
    onNavigateBack: () -> Unit,
    onEditKidClick: () -> Unit,
    onActivityClick: (activityId: Long) -> Unit,
    onAddActivityClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isKidDeleted by viewModel.isKidDeleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isKidDeleted) {
        if (isKidDeleted) {
            viewModel.onKidDeletedHandled()
            onNavigateBack()
        }
    }

    KidDetailContent(
        uiState = uiState,
        onEditKidClick = onEditKidClick,
        onDeleteKidClick = { showDeleteDialog = true },
        onDeleteAllClick = viewModel::deleteAllActivities,
        onActivityClick = onActivityClick,
        onActivityEnabledChange = viewModel::setActivityEnabled,
        onAddActivityClick = onAddActivityClick,
        onNavigateBack = onNavigateBack,
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete kid?") },
            text = {
                Text(
                    "All of ${uiState.kid?.name ?: "this kid"}'s activities and school-day " +
                        "overrides will be removed too.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteKid()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailContent(
    uiState: KidDetailUiState,
    onEditKidClick: () -> Unit,
    onDeleteKidClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onActivityClick: (activityId: Long) -> Unit,
    onActivityEnabledChange: (activityId: Long, enabled: Boolean) -> Unit,
    onAddActivityClick: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val kid = uiState.kid
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kid?.let { "${it.name}'s Activities" } ?: "Kid") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditKidClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit kid")
                    }
                    IconButton(onClick = onDeleteKidClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete kid")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddActivityClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add New Activity") },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

            kid == null ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("This kid no longer exists.")
                }

            uiState.activities.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No activities yet.")
                        Spacer(Modifier.height(16.dp))
                        DeleteAllActivitiesButton(
                            kidName = kid.name,
                            onDeleteClick = onDeleteAllClick,
                        )
                    }
                }

            else ->
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.activities, key = { it.id }) { activity ->
                        KidActivityRow(
                            activity = activity,
                            onClick = { onActivityClick(activity.id) },
                            onEnabledChange = { enabled ->
                                onActivityEnabledChange(activity.id, enabled)
                            },
                        )
                    }
                    item {
                        DeleteAllActivitiesButton(
                            kidName = kid.name,
                            onDeleteClick = onDeleteAllClick,
                        )
                    }
                }
        }
    }
}

@Composable
fun KidActivityRow(
    activity: Activity,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (activity.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                )
                Text(
                    text =
                        "${dayLabels(activity.daysOfWeek)} · ${activity.startTime} – ${activity.endTime}" +
                            if (activity.isPeriodic) " · weekly" else " · once",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = activity.isEnabled,
                onCheckedChange = onEnabledChange,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private val DAY_LABELS = arrayOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun dayLabels(days: IntArray): String = days.sorted().joinToString { DAY_LABELS[it] }

@Preview(showBackground = true, name = "Detail — with activities (muted included)")
@Composable
fun KidDetailContentPreview() {
    KidsTrackerActivityTheme {
        KidDetailContent(
            uiState =
                KidDetailUiState(
                    isLoading = false,
                    kid = DebugTestData.mockKids[0],
                    activities = DebugTestData.mockActivities.filter { it.kidId == DebugTestData.mockKids[0].id },
                ),
            onDeleteAllClick = {},
            onEditKidClick = {},
            onDeleteKidClick = {},
            onActivityClick = {},
            onActivityEnabledChange = { _, _ -> },
            onAddActivityClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Detail — loading")
@Composable
fun KidDetailContentLoadingPreview() {
    KidsTrackerActivityTheme {
        KidDetailContent(
            uiState = KidDetailUiState(),
            onDeleteAllClick = {},
            onEditKidClick = {},
            onDeleteKidClick = {},
            onActivityClick = {},
            onActivityEnabledChange = { _, _ -> },
            onAddActivityClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Detail — kid no longer exists")
@Composable
fun KidDetailContentMissingKidPreview() {
    KidsTrackerActivityTheme {
        KidDetailContent(
            uiState = KidDetailUiState(isLoading = false),
            onDeleteAllClick = {},
            onEditKidClick = {},
            onDeleteKidClick = {},
            onActivityClick = {},
            onActivityEnabledChange = { _, _ -> },
            onAddActivityClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Detail — no activities yet")
@Composable
fun KidDetailContentNoActivitiesPreview() {
    KidsTrackerActivityTheme {
        KidDetailContent(
            uiState =
                KidDetailUiState(
                    isLoading = false,
                    kid = DebugTestData.mockKids[0],
                ),
            onDeleteAllClick = {},
            onEditKidClick = {},
            onDeleteKidClick = {},
            onActivityClick = {},
            onActivityEnabledChange = { _, _ -> },
            onAddActivityClick = {},
            onNavigateBack = {},
        )
    }
}
