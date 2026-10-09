package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddKidUiState
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddKidViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidNameError
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel

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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Kid") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                label = { Text("Kid's Name") },
                isError = uiState.nameError != null,
                supportingText = {
                    when (val nameError = uiState.nameError) {
                        KidNameError.Blank -> Text("Please enter the kid's name")
                        is KidNameError.Duplicate ->
                            Text("A kid named ${nameError.existingName} already exists")

                        null -> Unit
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.schoolName,
                onValueChange = onSchoolNameChange,
                label = { Text("School Name") },
                isError = uiState.schoolNameError,
                supportingText = {
                    if (uiState.schoolNameError) {
                        Text("Please enter the school name")
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
                Text("Save Kid")
            }
        }
    }
}

@Preview(showBackground = true)
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
