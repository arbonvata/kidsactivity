package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityUiState
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

class ActivitySaveFeedbackTest {
    @get:Rule
    val compose = createComposeRule()

    private val validForm =
        AddActivityUiState(
            name = "Football",
            selectedKidId = 1,
            selectedDays = setOf(2),
            startTime = LocalTime.of(17, 0),
            endTime = LocalTime.of(18, 30),
        )

    @Test
    fun invalidTimeFeedbackIsVisibleWithoutScrollingAndSaveWorksAfterCorrection() {
        var form by mutableStateOf(validForm.copy(startTime = LocalTime.of(19, 0)))
        var saves = 0
        compose.setContent { Editor(form) { saves++ } }
        compose.onNodeWithText(text(R.string.add_activity_end_before_start_error)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.add_activity_save)).assertIsNotEnabled()
        compose.runOnIdle { form = form.copy(endTime = LocalTime.of(20, 30)) }
        compose.onNodeWithText(text(R.string.add_activity_save)).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun saveFailureIsVisibleWithoutScrollingAndRetryRemainsEnabled() {
        compose.setContent { Editor(validForm.copy(saveError = true)) {} }
        compose.onNodeWithText(text(R.string.edit_activity_save_error)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.add_activity_save)).assertIsEnabled()
    }

    @Test
    fun savingShowsProgressAndDisablesAnotherSave() {
        compose.setContent { Editor(validForm.copy(isSaving = true)) {} }
        compose.onNodeWithText(text(R.string.edit_activity_saving)).assertIsDisplayed().assertIsNotEnabled()
    }

    @Composable
    private fun Editor(
        form: AddActivityUiState,
        onSave: () -> Unit,
    ) {
        KidsTrackerActivityTheme {
            Box(Modifier.height(360.dp)) {
                AddActivityContent(
                    uiState = form,
                    kids = emptyList(),
                    isKidPickerEnabled = false,
                    onKidSelected = {},
                    onNameChange = {},
                    onToggleDay = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onPeriodicChange = {},
                    onSchoolActivityChange = {},
                    onFirstDateChange = {},
                    onSaveClick = onSave,
                    onNavigateBack = {},
                    extraContent = { repeat(20) { Text("Extra activity details $it") } },
                )
            }
        }
    }

    private fun text(resource: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
