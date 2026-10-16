package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddKidUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddKidViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.EditKidViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidNameError
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Stateful entry point of the Add Kid screen: owns the [AddKidViewModel],
 * collects its state and turns the one-shot `isSaved` flag into navigation.
 */
@Composable
fun AddKidScreen(
    viewModel: AddKidViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedHandled()
            onNavigateBack()
        }
    }

    AddKidContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onSchoolNameChange = viewModel::onSchoolNameChange,
        onSaveClick = viewModel::saveKid,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateful entry point of the Edit Kid screen: loads the stored kid into the
 * shared Add Kid form and saves via updateKid (the duplicate check ignores
 * the kid itself).
 */
@Composable
fun EditKidScreen(
    kidId: Long,
    viewModel: EditKidViewModel = koinViewModel(parameters = { parametersOf(kidId) }),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.form?.isSaved) {
        if (uiState.form?.isSaved == true) {
            viewModel.onSavedHandled()
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
                Text(stringResource(R.string.edit_kid_not_found))
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
            AddKidContent(
                uiState = form,
                onNameChange = viewModel::onNameChange,
                onSchoolNameChange = viewModel::onSchoolNameChange,
                onSaveClick = viewModel::saveKid,
                onNavigateBack = onNavigateBack,
                screenTitle = stringResource(R.string.edit_kid_title),
            )
        }
    }
}

/**
 * Stateless form: plain state in, callbacks out, so it previews and UI-tests
 * without a database or fake repositories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKidContent(
    uiState: AddKidUiState,
    onNameChange: (String) -> Unit,
    onSchoolNameChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    screenTitle: String = stringResource(R.string.add_kid_title),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_kid_back_content_description),
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
                label = { Text(stringResource(R.string.add_kid_name_label)) },
                isError = uiState.nameError != null,
                supportingText = {
                    when (val nameError = uiState.nameError) {
                        KidNameError.Blank -> Text(stringResource(R.string.add_kid_name_error_blank))
                        is KidNameError.Duplicate ->
                            Text(
                                stringResource(
                                    R.string.add_kid_name_error_duplicate,
                                    nameError.existingName,
                                ),
                            )

                        null -> Unit
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.schoolName,
                onValueChange = onSchoolNameChange,
                label = { Text(stringResource(R.string.add_kid_school_name_label)) },
                isError = uiState.schoolNameError,
                supportingText = {
                    if (uiState.schoolNameError) {
                        Text(stringResource(R.string.add_kid_school_name_error))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
            ) {
                Text(stringResource(R.string.add_kid_save))
            }
        }
    }
}

@Preview(showBackground = true, name = "Add kid — empty form")
@Composable
fun AddKidContentPreview() {
    KidsTrackerActivityTheme {
        AddKidContent(
            uiState = AddKidUiState(),
            onNameChange = {},
            onSchoolNameChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Edit kid — filled")
@Composable
fun EditKidContentPreview() {
    KidsTrackerActivityTheme {
        AddKidContent(
            uiState = AddKidUiState(name = "Leo", schoolName = "Greenfield Primary"),
            onNameChange = {},
            onSchoolNameChange = {},
            onSaveClick = {},
            onNavigateBack = {},
            screenTitle = stringResource(R.string.edit_kid_title),
        )
    }
}
