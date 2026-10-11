package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class AddActivityViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val activityRepository = mockk<ActivityRepository>()
    private val kidRepository = mockk<KidRepository>()
    private val todaysDayOfWeek = LocalDate.now().dayOfWeek.value

    @BeforeEach
    fun setUpKidsFlow() {
        // The constructor reads getAllKidsFlow() for the kids StateFlow, so a
        // strict mock needs an answer before the ViewModel is built.
        every { kidRepository.getAllKidsFlow() } returns emptyFlow()
    }

    @Test
    fun initialStatePreselectsTodaysWeekdayForTheScopedKid() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)

            val state = viewModel.uiState.value

            assertEquals(5L, state.selectedKidId)
            assertEquals(setOf(todaysDayOfWeek), state.selectedDays)
            assertEquals(LocalTime.of(17, 0), state.startTime)
            assertEquals(LocalTime.of(18, 30), state.endTime)
            assertFalse(state.nameError)
            assertFalse(state.isPeriodic)
        }

    @Test
    fun onToggleDayAddsAndRemovesDaysAroundThePreselection() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)

            viewModel.onToggleDay(3)
            viewModel.onToggleDay(5)

            assertEquals(setOf(todaysDayOfWeek, 3, 5), viewModel.uiState.value.selectedDays)

            viewModel.onToggleDay(3)

            assertEquals(setOf(todaysDayOfWeek, 5), viewModel.uiState.value.selectedDays)
        }

    @Test
    fun onNameChangeFlagsBlankNameAndClearsTheFlagOnceValid() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)

            viewModel.onNameChange("   ")

            assertTrue(viewModel.uiState.value.nameError)

            viewModel.onNameChange("Football")

            assertFalse(viewModel.uiState.value.nameError)
        }

    @Test
    fun timeChangeHandlersUpdateStartAndEndTime() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)

            viewModel.onStartTimeChange(LocalTime.of(18, 0))
            viewModel.onEndTimeChange(LocalTime.of(19, 30))

            assertEquals(LocalTime.of(18, 0), viewModel.uiState.value.startTime)
            assertEquals(LocalTime.of(19, 30), viewModel.uiState.value.endTime)
        }

    @Test
    fun onKidSelectedUpdatesSelectedKidId() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 0L, activityRepository, kidRepository)

            viewModel.onKidSelected(9L)

            assertEquals(9L, viewModel.uiState.value.selectedKidId)
        }

    @Test
    fun saveActivityWithoutSelectedKidSkipsRepository() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 0L, activityRepository, kidRepository)
            viewModel.onNameChange("Football")

            viewModel.saveActivity()

            assertFalse(viewModel.uiState.value.isSaved)
            assertFalse(viewModel.uiState.value.isSaving)
            coVerify(exactly = 0) { activityRepository.addActivity(any()) }
        }

    @Test
    fun saveActivityWithBlankNameFlagsErrorAndSkipsRepository() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)
            viewModel.onNameChange("   ")

            viewModel.saveActivity()

            assertTrue(viewModel.uiState.value.nameError)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { activityRepository.addActivity(any()) }
        }

    @Test
    fun saveActivityWithNoDaysFlagsErrorAndSkipsRepository() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)
            viewModel.onNameChange("Football")
            viewModel.onToggleDay(todaysDayOfWeek) // clears today's preselection

            viewModel.saveActivity()

            assertTrue(viewModel.uiState.value.daysError)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { activityRepository.addActivity(any()) }
        }

    @Test
    fun saveActivityInsertsFormattedActivityWithSortedDaysAndSignalsSaved() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)
            val savedActivity = slot<Activity>()
            coEvery { activityRepository.addActivity(capture(savedActivity)) } returns Activity(id = 1L)
            viewModel.onNameChange("  Football  ")
            viewModel.onToggleDay(5)
            viewModel.onToggleDay(3)
            viewModel.onStartTimeChange(LocalTime.of(18, 0))
            viewModel.onEndTimeChange(LocalTime.of(19, 30))

            viewModel.saveActivity()

            coVerify { activityRepository.addActivity(any()) }
            assertEquals(5L, savedActivity.captured.kidId)
            assertEquals("Football", savedActivity.captured.name)
            assertEquals("18:00", savedActivity.captured.startTime)
            assertEquals("19:30", savedActivity.captured.endTime)
            assertTrue(savedActivity.captured.isEnabled)
            assertFalse(savedActivity.captured.isPeriodic)
            assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_DATE), savedActivity.captured.firstDate)
            val expectedDays = setOf(todaysDayOfWeek, 5, 3).sorted().toIntArray()
            assertTrue(savedActivity.captured.daysOfWeek.contentEquals(expectedDays))
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun saveActivityPersistsPeriodicFlagWhenChecked() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)
            val savedActivity = slot<Activity>()
            coEvery { activityRepository.addActivity(capture(savedActivity)) } returns Activity(id = 1L)
            viewModel.onNameChange("Football")
            viewModel.onPeriodicChange(true)
            assertTrue(viewModel.uiState.value.isPeriodic)

            viewModel.saveActivity()

            coVerify { activityRepository.addActivity(any()) }
            assertTrue(savedActivity.captured.isPeriodic)
        }

    @Test
    fun onSavedHandledResetsIsSaved() =
        runTest {
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)
            coEvery { activityRepository.addActivity(any()) } returns Activity(id = 1L)
            viewModel.onNameChange("Football")
            viewModel.saveActivity()
            assertTrue(viewModel.uiState.value.isSaved)

            viewModel.onSavedHandled()

            assertFalse(viewModel.uiState.value.isSaved)
        }

    @Test
    fun kidsFlowMirrorsRepositoryKids() =
        runTest {
            val kids = listOf(Kid(id = 1L, name = "Leo"))
            every { kidRepository.getAllKidsFlow() } returns flowOf(kids)
            val viewModel = AddActivityViewModel(kidId = 5L, activityRepository, kidRepository)

            // stateIn(WhileSubscribed) only starts the upstream flow once
            // somebody collects; keep a subscriber alive for the assertion.
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.kids.collect()
            }

            assertEquals(kids, viewModel.kids.value)
        }
}
