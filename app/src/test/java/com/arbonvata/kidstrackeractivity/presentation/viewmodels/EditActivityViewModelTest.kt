package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EditActivityViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val activityRepository = mockk<ActivityRepository>(relaxUnitFun = true)
    private val kidRepository = mockk<KidRepository>()

    @BeforeEach
    fun setUpKidsFlow() {
        // The constructor reads getAllKidsFlow() for the kids StateFlow, so a
        // strict mock needs an answer before the ViewModel is built.
        every { kidRepository.getAllKidsFlow() } returns emptyFlow()
    }

    private fun storedActivity() =
        Activity(
            id = 5L,
            kidId = 2L,
            name = "Football",
            startTime = "17:00",
            endTime = "18:30",
            isEnabled = false,
            daysOfWeek = intArrayOf(2),
            isPeriodic = false,
            isSchoolActivity = false,
            firstDate = "2026-10-20",
        )

    @Test
    fun loadsStoredActivityIntoTheForm() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()

            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)

            val form = viewModel.uiState.value.form
            assertNotNull(form)
            assertEquals(2L, form?.selectedKidId)
            assertEquals("Football", form?.name)
            assertEquals(setOf(2), form?.selectedDays)
            assertEquals(LocalTime.of(17, 0), form?.startTime)
            assertEquals(LocalTime.of(18, 30), form?.endTime)
            assertFalse(form?.isPeriodic == true)
            assertFalse(form?.isSchoolActivity == true)
            // The stored anchor 2026-10-20 is a Tuesday; the form shows the
            // Monday of its calendar week.
            assertEquals(LocalDate.of(2026, 10, 19), form?.firstDate)
            assertFalse(viewModel.uiState.value.isNotFound)
            coVerify { activityRepository.getActivity(5L) }
        }

    @Test
    fun missingActivitySetsNotFound() =
        runTest {
            coEvery { activityRepository.getActivity(9L) } returns null

            val viewModel = EditActivityViewModel(9L, activityRepository, kidRepository)

            assertTrue(viewModel.uiState.value.isNotFound)
            assertEquals(null, viewModel.uiState.value.form)
        }

    @Test
    fun saveUpdatesFieldsButPreservesIdentityEnableFlagAndAnchor() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val updated = slot<Activity>()
            coEvery { activityRepository.updateActivity(capture(updated)) } returns Unit
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onNameChange("  Swimming  ")
            viewModel.onToggleDay(4)
            viewModel.onStartTimeChange(LocalTime.of(18, 0))
            viewModel.onEndTimeChange(LocalTime.of(19, 30))
            viewModel.onPeriodicChange(true)
            viewModel.onSchoolActivityChange(true)

            viewModel.saveActivity()

            coVerify { activityRepository.updateActivity(any()) }
            assertEquals(5L, updated.captured.id)
            assertEquals(2L, updated.captured.kidId)
            assertEquals("Swimming", updated.captured.name)
            assertEquals("18:00", updated.captured.startTime)
            assertEquals("19:30", updated.captured.endTime)
            assertEquals(setOf(2, 4).sorted().toIntArray().toList(), updated.captured.daysOfWeek.toList())
            assertTrue(updated.captured.isPeriodic)
            assertTrue(updated.captured.isSchoolActivity)
            assertFalse(updated.captured.isEnabled)
            // Untouched, the loaded (snapped) anchor saves back unchanged.
            assertEquals("2026-10-19", updated.captured.firstDate)
            assertTrue(viewModel.uiState.value.isSaved)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun editingAOneOffsWeekReAnchorsIt() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val updated = slot<Activity>()
            coEvery { activityRepository.updateActivity(capture(updated)) } returns Unit
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)

            // Wednesday 2026-11-04 snaps to its Monday, 2026-11-02.
            viewModel.onFirstDateChange(LocalDate.of(2026, 11, 4))

            viewModel.saveActivity()

            assertEquals("2026-11-02", updated.captured.firstDate)
        }

    @Test
    fun turningOnSchoolActivityForcesPeriodicOnSave() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val updated = slot<Activity>()
            coEvery { activityRepository.updateActivity(capture(updated)) } returns Unit
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)

            viewModel.onSchoolActivityChange(true)

            val form = viewModel.uiState.value.form
            assertTrue(form?.isPeriodic == true)

            viewModel.saveActivity()

            assertTrue(updated.captured.isSchoolActivity)
            assertTrue(updated.captured.isPeriodic)
        }

    @Test
    fun saveWithBlankNameFlagsErrorAndSkipsRepository() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onNameChange("   ")

            viewModel.saveActivity()

            assertTrue(
                viewModel.uiState.value.form
                    ?.nameError == true,
            )
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { activityRepository.updateActivity(any()) }
        }

    @Test
    fun deleteRemovesTheActivityAndSignalsDeleted() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)

            viewModel.deleteActivity()

            coVerify { activityRepository.deleteActivity(5L) }
            assertTrue(viewModel.uiState.value.isDeleted)
        }
}
