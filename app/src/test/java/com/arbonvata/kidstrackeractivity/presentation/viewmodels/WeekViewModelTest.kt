package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>()

    @Test
    fun initialStateExpandsCurrentWeekOccurrencesWithKidNames() =
        runTest {
            val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val kid = Kid(id = 1L, name = "Leo")
            val activity =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(monday.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(activity))
            val viewModel = WeekViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(monday, state.weekStart)
            assertEquals(listOf(monday), state.weekOccurrences.map { it.date })
            assertEquals(listOf(activity), state.weekOccurrences.map { it.activity })
            assertEquals(listOf("Leo"), state.weekOccurrences.map { it.kidName })
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }

    @Test
    fun disabledActivitiesStayVisibleForTheSchedule() =
        runTest {
            // Pins that Week passes includeDisabled = true: muted activities
            // stay listed (rendered dimmed) while alarms skip them.
            val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val kid = Kid(id = 1L, name = "Leo")
            val muted =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    isEnabled = false,
                    daysOfWeek = intArrayOf(monday.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(muted))
            val viewModel = WeekViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertEquals(
                listOf(monday),
                viewModel.uiState.value.weekOccurrences
                    .map { it.date },
            )
            assertEquals(
                listOf(muted),
                viewModel.uiState.value.weekOccurrences
                    .map { it.activity },
            )
        }

    @Test
    fun nextAndPreviousWeekShiftTheRangeAndReExpandOccurrences() =
        runTest {
            val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val kid = Kid(id = 1L, name = "Leo")
            // Mondays only, so every visible week expands to exactly its Monday.
            val activity =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(activity))
            val viewModel = WeekViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.nextWeek()

            assertEquals(monday.plusWeeks(1), viewModel.uiState.value.weekStart)
            assertEquals(
                listOf(monday.plusWeeks(1)),
                viewModel.uiState.value.weekOccurrences
                    .map { it.date },
            )

            viewModel.previousWeek()

            assertEquals(monday, viewModel.uiState.value.weekStart)
            assertEquals(
                listOf(monday),
                viewModel.uiState.value.weekOccurrences
                    .map { it.date },
            )
        }

    @Test
    fun emptyKidsAndActivitiesYieldEmptyOccurrences() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(emptyList())
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(emptyList())
            val viewModel = WeekViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.kids.isEmpty())
            assertTrue(state.weekOccurrences.isEmpty())
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }
}
