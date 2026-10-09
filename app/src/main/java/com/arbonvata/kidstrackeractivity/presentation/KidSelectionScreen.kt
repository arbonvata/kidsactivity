package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidSelectionScreen(
    kids: List<Kid>,
    onAddKidClick: () -> Unit,
    onContinueClick: (Set<Long>) -> Unit,
) {
    var selectedKidIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Kids") },
                actions = {
                    IconButton(onClick = onAddKidClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Kid")
                    }
                    // Select All / Deselect All toggle
                    val allSelected = kids.isNotEmpty() && selectedKidIds.size == kids.size
                    TextButton(onClick = {
                        selectedKidIds = if (allSelected) emptySet() else kids.map { it.id }.toSet()
                    }) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                },
            )
        },
        bottomBar = {
            // Continue button at the bottom
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onContinueClick(selectedKidIds) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    enabled = selectedKidIds.isNotEmpty(),
                ) {
                    Text(
                        "View Schedule (${selectedKidIds.size} selected)",
                    )
                }
            }
        },
    ) { padding ->
        if (kids.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No kids added yet.")
                    Button(
                        onClick = onAddKidClick,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Add Kid")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(kids, key = { it.id }) { kid ->
                    KidSelectionRow(
                        kid = kid,
                        isSelected = selectedKidIds.contains(kid.id),
                        onToggle = { isSelected ->
                            selectedKidIds =
                                if (isSelected) {
                                    selectedKidIds + kid.id
                                } else {
                                    selectedKidIds - kid.id
                                }
                        },
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
    onToggle: (Boolean) -> Unit,
) {
    Card(
        onClick = { onToggle(!isSelected) },
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = kid.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "School: ${kid.schoolName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KidSelectionScreenPreview() {
    KidsTrackerActivityTheme {
        val kids =
            listOf(
                Kid(id = 1, name = "Alice", schoolName = "Greenfield Primary"),
                Kid(id = 2, name = "Bob", schoolName = "Greenfield Primary"),
            )
        KidSelectionScreen(
            kids = kids,
            onAddKidClick = {},
            onContinueClick = {},
        )
    }
}
