package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import androidx.compose.ui.tooling.preview.Preview
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidSelectionScreen(
    kids: List<Kid>,
    onContinueClick: (Set<Long>) -> Unit
) {
    var selectedKidIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Kids") },
                actions = {
                    // Select All / Deselect All toggle
                    val allSelected = kids.isNotEmpty() && selectedKidIds.size == kids.size
                    TextButton(onClick = {
                        selectedKidIds = if (allSelected) emptySet() else kids.map { it.id }.toSet()
                    }) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                }
            )
        },
        bottomBar = {
            // Continue button at the bottom
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onContinueClick(selectedKidIds) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = selectedKidIds.isNotEmpty()
                ) {
                    Text(
                        "View Schedule (${selectedKidIds.size} selected)"
                    )
                }
            }
        }
    ) { padding ->
        if (kids.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No kids added yet. Add a kid to get started!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(kids, key = { it.id }) { kid ->
                    KidSelectionRow(
                        kid = kid,
                        isSelected = selectedKidIds.contains(kid.id),
                        onToggle = { isSelected ->
                            selectedKidIds = if (isSelected) {
                                selectedKidIds + kid.id
                            } else {
                                selectedKidIds - kid.id
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KidSelectionRow(
    kid: Kid,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        onClick = { onToggle(!isSelected) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = kid.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Default end time: ${kid.defaultEndTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KidSelectionScreenPreview() {
    KidsTrackerActivityTheme {
        val kids = listOf(
            Kid(id = 1, name = "Alice", defaultEndTime = "18:00"),
            Kid(id = 2, name = "Bob", defaultEndTime = "18:00")
        )
        KidSelectionScreen(
            kids = kids,
            onContinueClick = {}
        )
    }
}