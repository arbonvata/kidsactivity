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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidsViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful entry point of the Kids Management screen: owns the
 * [KidsViewModel], whose reactive list updates itself whenever the kid
 * table changes (including kids added on the Add Kid screen).
 */
@Composable
fun KidsManagementScreen(
    viewModel: KidsViewModel = koinViewModel(),
    onAddKidClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val kids by viewModel.kids.collectAsState()

    KidsManagementContent(
        kids = kids,
        onAddKidClick = onAddKidClick,
        onBackClick = onBackClick,
    )
}

/** Stateless list; plain data and callbacks so it previews without a database. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidsManagementContent(
    kids: List<Kid>,
    onAddKidClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kids") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddKidClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Kid")
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
                    KidsManagementRow(kid)
                }
            }
        }
    }
}

@Composable
fun KidsManagementRow(kid: Kid) {
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

@Preview(showBackground = true)
@Composable
fun KidsManagementContentPreview() {
    KidsTrackerActivityTheme {
        KidsManagementContent(
            kids =
                listOf(
                    Kid(id = 1, name = "Alice", schoolName = "Greenfield Primary"),
                    Kid(id = 2, name = "Bob", schoolName = "St. Mary's"),
                ),
            onAddKidClick = {},
            onBackClick = {},
        )
    }
}
