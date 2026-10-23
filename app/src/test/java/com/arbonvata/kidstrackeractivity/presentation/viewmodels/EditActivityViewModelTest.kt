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
import kotlinx.coroutines.CompletableDeferred
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
    fun correctingInvalidTimesThenSavingPersistsTheEditedTimes() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val updated = slot<Activity>()
            coEvery { activityRepository.updateActivity(capture(updated)) } returns Unit
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onStartTimeChange(LocalTime.of(19, 0))
            viewModel.saveActivity()
            assertTrue(
                viewModel.uiState.value.form
                    ?.timeError == true,
            )
            coVerify(exactly = 0) { activityRepository.updateActivity(any()) }

            viewModel.onEndTimeChange(LocalTime.of(20, 30))
            viewModel.saveActivity()
            assertEquals("19:00", updated.captured.startTime)
            assertEquals("20:30", updated.captured.endTime)
            assertTrue(viewModel.uiState.value.isSaved)
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(
                viewModel.uiState.value.form
                    ?.timeError == true,
            )
        }

    @Test
    fun saveFailureKeepsEditsAndAllowsRetry() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            coEvery { activityRepository.updateActivity(any()) } throws IllegalStateException("Database unavailable")
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onEndTimeChange(LocalTime.of(20, 30))
            viewModel.saveActivity()
            assertTrue(viewModel.uiState.value.saveError)
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSaved)
            assertEquals(
                LocalTime.of(20, 30),
                viewModel.uiState.value.form
                    ?.endTime,
            )

            val updated = slot<Activity>()
            coEvery { activityRepository.updateActivity(capture(updated)) } returns Unit
            viewModel.saveActivity()
            assertEquals("20:30", updated.captured.endTime)
            assertTrue(viewModel.uiState.value.isSaved)
            assertFalse(viewModel.uiState.value.saveError)
        }

    @Test
    fun repeatedSaveWhileWriteIsPendingDoesNotStartAnotherWrite() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val write = CompletableDeferred<Unit>()
            coEvery { activityRepository.updateActivity(any()) } coAnswers { write.await() }
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onEndTimeChange(LocalTime.of(20, 30))
            viewModel.saveActivity()
            viewModel.saveActivity()
            assertTrue(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 1) { activityRepository.updateActivity(any()) }
            write.complete(Unit)
            assertTrue(viewModel.uiState.value.isSaved)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun savingTimeChangesPreservesPickupDetailsCalendarStartAndExceptions() =
        runTest {
            val original =
                storedActivity().copy(
                    location = "Pitch",
                    pickupAdult = "Alex",
                    pickupNotes = "Side gate",
                    calendarStartDate = "2026-10-19",
                    occurrenceExceptions = "2026-10-20||",
                )
            coEvery { activityRepository.getActivity(5L) } returns original
            val updated = slot<Activity>()
            coEvery { activityRepository.updateActivity(capture(updated)) } returns Unit
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onEndTimeChange(LocalTime.of(20, 30))
            viewModel.saveActivity()
            assertEquals("20:30", updated.captured.endTime)
            assertEquals(original.location, updated.captured.location)
            assertEquals(original.pickupAdult, updated.captured.pickupAdult)
            assertEquals(original.pickupNotes, updated.captured.pickupNotes)
            assertEquals(original.calendarStartDate, updated.captured.calendarStartDate)
            assertEquals(original.occurrenceExceptions, updated.captured.occurrenceExceptions)
        }

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
    fun saveActivityWithEndBeforeStartFlagsErrorAndSkipsRepository() =
        runTest {
            coEvery { activityRepository.getActivity(5L) } returns storedActivity()
            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)
            viewModel.onStartTimeChange(LocalTime.of(19, 0))
            viewModel.onEndTimeChange(LocalTime.of(18, 0))

            viewModel.saveActivity()

            assertTrue(
                viewModel.uiState.value.form
                    ?.timeError == true,
            )
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { activityRepository.updateActivity(any()) }
        }

    @Test
    fun corruptStoredTimesLoadAsNotFound() =
        runTest {
            // "9:00" does not parse; the form cannot be filled, so the row is
            // treated like a missing activity (it stays listed for deletion).
            coEvery { activityRepository.getActivity(5L) } returns storedActivity().copy(startTime = "9:00")

            val viewModel = EditActivityViewModel(5L, activityRepository, kidRepository)

            assertTrue(viewModel.uiState.value.isNotFound)
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
