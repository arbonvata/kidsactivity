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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.DebugTestData
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ParentsViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful entry point of the Parents Management screen: owns the
 * [ParentsViewModel], whose reactive list updates itself whenever the parent
 * table changes (including parents added on the Add Parent screen).
 */
@Composable
fun ParentsManagementScreen(
    viewModel: ParentsViewModel = koinViewModel(),
    onAddParentClick: () -> Unit,
    onParentClick: (Parent) -> Unit,
    onBackClick: () -> Unit,
) {
    val parents by viewModel.parents.collectAsState()

    ParentsManagementContent(
        parents = parents,
        onAddParentClick = onAddParentClick,
        onParentClick = onParentClick,
        onBackClick = onBackClick,
    )
}

/** Stateless list; plain data and callbacks so it previews without a database. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentsManagementContent(
    parents: List<Parent>,
    onAddParentClick: () -> Unit,
    onParentClick: (Parent) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parents_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.parents_management_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddParentClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.parents_management_add_parent))
            }
        },
    ) { padding ->
        if (parents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.parents_management_no_parents_added_yet))
                    Button(
                        onClick = onAddParentClick,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.parents_management_add_parent))
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
                items(parents, key = { it.id }) { parent ->
                    ParentsManagementRow(
                        parent = parent,
                        onClick = { onParentClick(parent) },
                    )
                }
            }
        }
    }
}

@Composable
fun ParentsManagementRow(
    parent: Parent,
    onClick: () -> Unit,
) {
    val relation = ParentRelation.fromKey(parent.relation)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parent.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        if (parent.phone.isBlank()) {
                            relation.label()
                        } else {
                            stringResource(R.string.parents_management_subtitle_format, relation.label(), parent.phone)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ParentsManagementContentPreview() {
    KidsTrackerActivityTheme {
        ParentsManagementContent(
            parents = DebugTestData.mockParents,
            onAddParentClick = {},
            onParentClick = {},
            onBackClick = {},
        )
    }
}
