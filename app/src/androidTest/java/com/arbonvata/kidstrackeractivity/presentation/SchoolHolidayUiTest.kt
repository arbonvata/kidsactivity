package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.fixtures.SchoolOverrideFixture
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SchoolHolidayUiTest {
    @get:Rule
    val compose = createComposeRule()
    private lateinit var fixture: SchoolOverrideFixture

    @Before
    fun showSchoolOverrides() {
        fixture = SchoolOverrideFixture()
        compose.setContent {
            KidsTrackerActivityTheme {
                SchoolOverrideScreen(viewModel = fixture.viewModel, onBackClick = {})
            }
        }
        waitForSavedDay(noSchool = false)
    }

    @After
    fun cleanUp() {
        if (::fixture.isInitialized) {
            compose.runOnIdle { fixture.clearViewModel() }
            fixture.close()
        }
    }

    @Test
    fun holidayRangeDaySaveAndRestorePreserveEarlyReleaseTime() {
        clickRangeAction(R.string.holiday_apply)
        compose.onNodeWithText(text(R.string.share_ok)).performClick()
        waitForSavedDay(noSchool = true)

        openDay()
        dayCheckbox().assertIsOn()
        endTimeButton().assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.school_override_save)).performClick()
        waitForSavedDay(noSchool = true)

        clickRangeAction(R.string.holiday_restore)
        compose.onNodeWithText(text(R.string.share_ok)).performClick()
        waitForSavedDay(noSchool = false)
        openDay()
        dayCheckbox().assertIsOff()
        endTimeButton().assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun togglingNoSchoolAndReopeningDayPreserveEarlyReleaseTime() {
        openDay()
        dayCheckbox().assertIsOff().performClick()
        endTimeButton().assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.school_override_save)).performClick()
        waitForSavedDay(noSchool = true)

        openDay()
        dayCheckbox().assertIsOn().performClick()
        endTimeButton().assertIsEnabled()
        compose.onNodeWithText(text(R.string.school_override_save)).performClick()
        waitForSavedDay(noSchool = false)
        openDay()
        endTimeButton().assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun cancellingHolidayConfirmationLeavesSchoolAndEarlyReleaseUnchanged() {
        clickRangeAction(R.string.holiday_apply)
        compose.onNodeWithText(text(R.string.add_activity_date_picker_cancel)).performClick()
        compose.runOnIdle {
            assertFalse(fixture.savedDay().isNoSchool)
            assertEquals(fixture.earlyEnd, fixture.savedDay().endTime)
        }
        openDay()
        dayCheckbox().assertIsOff()
        endTimeButton().assertIsDisplayed().assertIsEnabled()
    }

    private fun clickRangeAction(resource: Int) {
        compose.onNodeWithText(text(resource)).performScrollTo().performClick()
    }

    private fun openDay() {
        compose.onNodeWithText(fixture.day.dayOfMonth.toString()).performScrollTo().performClick()
    }

    private fun dayCheckbox() = compose.onNode(isToggleable() and hasAnyAncestor(isDialog()))

    private fun endTimeButton() = compose.onNodeWithText(text(R.string.school_override_school_ends, fixture.earlyEnd))

    private fun waitForSavedDay(noSchool: Boolean) {
        compose.waitUntil(timeoutMillis = 5_000) {
            fixture.viewModel.uiState.value.overrides[fixture.day]?.let {
                it.isNoSchool == noSchool && it.endTime == fixture.earlyEnd
            } == true
        }
        compose.runOnIdle {
            assertEquals(noSchool, fixture.savedDay().isNoSchool)
            assertEquals(fixture.earlyEnd, fixture.savedDay().endTime)
        }
    }

    private fun text(
        resource: Int,
        vararg args: Any,
    ) = InstrumentationRegistry.getInstrumentation().targetContext.getString(resource, *args)
}
