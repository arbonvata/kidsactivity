package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddParentUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddParentViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.EditParentViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ParentNameError
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Localized label of a relation; shared by the list rows and the form's chips. */
@Composable
fun ParentRelation.label(): String =
    when (this) {
        ParentRelation.FATHER -> stringResource(R.string.parent_relation_father)
        ParentRelation.MOTHER -> stringResource(R.string.parent_relation_mother)
        ParentRelation.GUARDIAN -> stringResource(R.string.parent_relation_guardian)
        ParentRelation.OTHER -> stringResource(R.string.parent_relation_other)
    }

/**
 * Stateful entry point of the Add Parent screen: owns the [AddParentViewModel],
 * collects its state and turns the one-shot `isSaved` flag into navigation.
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
 * Stateful entry point of the Edit Parent screen: loads the stored parent
 * into the shared Add Parent form, saves via updateParent (the duplicate
 * check ignores the parent itself) and hosts the delete action — parents
 * have no detail screen, so delete lives here behind a confirmation dialog.
 */
@Composable
fun EditParentScreen(
    parentId: Long,
    viewModel: EditParentViewModel = koinViewModel(parameters = { parametersOf(parentId) }),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.form?.isSaved) {
        if (uiState.form?.isSaved == true) {
            viewModel.onSavedHandled()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            viewModel.onDeletedHandled()
            onNavigateBack()
        }
    }

    val form = uiState.form
    when {
        uiState.isNotFound -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.edit_parent_not_found))
            }
        }

        form == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.parent_delete_dialog_title)) },
                    text = { Text(stringResource(R.string.parent_delete_dialog_message, form.name.trim())) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            viewModel.deleteParent()
                        }) { Text(stringResource(R.string.parent_delete_dialog_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.parent_delete_dialog_cancel))
                        }
                    },
                )
            }
            AddParentContent(
                uiState = form,
                onNameChange = viewModel::onNameChange,
                onRelationChange = viewModel::onRelationChange,
                onPhoneChange = viewModel::onPhoneChange,
                onSaveClick = viewModel::saveParent,
                onNavigateBack = onNavigateBack,
                screenTitle = stringResource(R.string.edit_parent_title),
                onDeleteClick = { showDeleteDialog = true },
            )
        }
    }
}

/**
 * Stateless form: plain state in, callbacks out, so it previews and UI-tests
 * without a database or fake repositories. `onDeleteClick` is non-null only
 * in edit mode and adds the top-bar delete action.
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
    onDeleteClick: (() -> Unit)? = null,
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
                actions = {
                    onDeleteClick?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.parent_delete_content_description),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
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
                isError = uiState.nameError != null,
                supportingText = {
                    when (val nameError = uiState.nameError) {
                        ParentNameError.Blank -> Text(stringResource(R.string.add_parent_name_error_blank))
                        is ParentNameError.Duplicate ->
                            Text(
                                stringResource(
                                    R.string.add_parent_name_error_duplicate,
                                    nameError.existingName,
                                ),
                            )

                        null -> Unit
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

@Preview(showBackground = true, name = "Edit parent — filled")
@Composable
fun EditParentContentPreview() {
    KidsTrackerActivityTheme {
        AddParentContent(
            uiState = AddParentUiState(name = "Anna", relation = ParentRelation.MOTHER, phone = "+46 70 111 22 33"),
            onNameChange = {},
            onRelationChange = {},
            onPhoneChange = {},
            onSaveClick = {},
            onNavigateBack = {},
            screenTitle = stringResource(R.string.edit_parent_title),
            onDeleteClick = {},
        )
    }
}
