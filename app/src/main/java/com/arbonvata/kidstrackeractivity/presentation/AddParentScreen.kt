package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddParentUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddParentViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel

/** Localized label of a relation, shown on the form's chips. */
@Composable
fun ParentRelation.label(): String =
    when (this) {
        ParentRelation.FATHER -> stringResource(R.string.parent_relation_father)
        ParentRelation.MOTHER -> stringResource(R.string.parent_relation_mother)
        ParentRelation.GUARDIAN -> stringResource(R.string.parent_relation_guardian)
        ParentRelation.OTHER -> stringResource(R.string.parent_relation_other)
    }

/**
 * Stateful entry point of the Add Parent screen: owns the
 * [AddParentViewModel], collects its state and turns the one-shot `isSaved`
 * flag into navigation.
 */
@Composable
fun AddParentScreen(
    viewModel: AddParentViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedHandled()
            onNavigateBack()
        }
    }

    AddParentContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onRelationChange = viewModel::onRelationChange,
        onPhoneChange = viewModel::onPhoneChange,
        onSaveClick = viewModel::saveParent,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateless form: plain state in, callbacks out, so it previews and UI-tests
 * without a database or fake repositories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParentContent(
    uiState: AddParentUiState,
    onNameChange: (String) -> Unit,
    onRelationChange: (ParentRelation) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    screenTitle: String = stringResource(R.string.add_parent_title),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_parent_back_content_description),
                        )
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.add_parent_name_label)) },
                isError = uiState.nameError,
                supportingText = {
                    if (uiState.nameError) {
                        Text(stringResource(R.string.add_parent_name_error_blank))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(
                    stringResource(R.string.add_parent_relation_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ParentRelation.entries.forEach { relation ->
                        FilterChip(
                            selected = uiState.relation == relation,
                            onClick = { onRelationChange(relation) },
                            label = { Text(relation.label()) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.add_parent_phone_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
            ) {
                Text(stringResource(R.string.add_parent_save))
            }
        }
    }
}

@Preview(showBackground = true, name = "Add parent — empty form")
@Composable
fun AddParentContentPreview() {
    KidsTrackerActivityTheme {
        AddParentContent(
            uiState = AddParentUiState(),
            onNameChange = {},
            onRelationChange = {},
            onPhoneChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}
