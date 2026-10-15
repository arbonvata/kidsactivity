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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MonthViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>()

    @Test
    fun initialStateExpandsCurrentMonthOccurrencesWithKidNames() =
        runTest {
            val month = YearMonth.now()
            val firstDay = month.atDay(1)
            val kid = Kid(id = 1L, name = "Leo")
            // Occurs only on the weekday of the month's first day.
            val activity =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(firstDay.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(activity))
            val viewModel = MonthViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(month, state.selectedMonth)
            // The activity recurs on the first day's weekday: every such day
            // of the month, starting with the 1st.
            val dates = state.monthOccurrences.map { it.date }
            assertEquals(firstDay, dates.first())
            assertTrue(dates.all { it.dayOfWeek == firstDay.dayOfWeek })
            assertTrue(state.monthOccurrences.map { it.activity }.all { it == activity })
            assertTrue(state.monthOccurrences.map { it.kidName }.all { it == "Leo" })
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }

    @Test
    fun disabledActivitiesStayVisibleForTheSchedule() =
        runTest {
            // Pins that Month passes includeDisabled = true: muted activities
            // stay listed (rendered dimmed) while alarms skip them.
            val month = YearMonth.now()
            val firstDay = month.atDay(1)
            val kid = Kid(id = 1L, name = "Leo")
            val muted =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    isEnabled = false,
                    daysOfWeek = intArrayOf(firstDay.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(muted))
            val viewModel = MonthViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val dates =
                viewModel.uiState.value.monthOccurrences
                    .map { it.date }
            assertEquals(firstDay, dates.first())
            assertTrue(dates.all { it.dayOfWeek == firstDay.dayOfWeek })
            assertTrue(
                viewModel.uiState.value.monthOccurrences
                    .map { it.activity }
                    .all { !it.isEnabled },
            )
        }

    @Test
    fun nextAndPreviousMonthShiftTheRangeAndReExpandOccurrences() =
        runTest {
            val month = YearMonth.now()
            val kid = Kid(id = 1L, name = "Leo")
            // Mondays only, so every visible month expands to its Mondays.
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
            val viewModel = MonthViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.nextMonth()

            val nextMonth = month.plusMonths(1)
            assertEquals(nextMonth, viewModel.uiState.value.selectedMonth)
            assertTrue(
                viewModel.uiState.value.monthOccurrences
                    .map { it.date }
                    .isNotEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.monthOccurrences
                    .map { YearMonth.from(it.date) }
                    .all { it == nextMonth },
            )

            viewModel.previousMonth()

            assertEquals(month, viewModel.uiState.value.selectedMonth)
            assertTrue(
                viewModel.uiState.value.monthOccurrences
                    .map { YearMonth.from(it.date) }
                    .all { it == month },
            )
        }

    @Test
    fun emptyKidsAndActivitiesYieldEmptyOccurrences() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(emptyList())
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(emptyList())
            val viewModel = MonthViewModel(kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.kids.isEmpty())
            assertTrue(state.monthOccurrences.isEmpty())
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }
}
