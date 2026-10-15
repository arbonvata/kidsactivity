package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyScheduleViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>()
    private val scheduleOverrideRepository = mockk<ScheduleOverrideRepository>()

    @BeforeEach
    fun setUpOverridesFlow() {
        every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
    }

    @Test
    fun filtersToSelectedKidsAndExpandsTheSelectedDay() =
        runTest {
            val today = LocalDate.now()
            val kids =
                listOf(
                    Kid(id = 1L, name = "Leo"),
                    Kid(id = 2L, name = "Mia"),
                )
            val leosActivity =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(today.dayOfWeek.value),
                )
            val miasActivity =
                Activity(
                    id = 2L,
                    kidId = 2L,
                    name = "Piano",
                    startTime = "16:00",
                    endTime = "17:00",
                    daysOfWeek = intArrayOf(today.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(kids)
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(leosActivity, miasActivity))
            val viewModel =
                DailyScheduleViewModel(
                    kidRepository = kidRepository,
                    activityRepository = activityRepository,
                    scheduleOverrideRepository = scheduleOverrideRepository,
                    selectedKidIds = setOf(1L),
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf(kids[0]), state.kids)
            assertEquals(listOf(leosActivity), state.dayOccurrences.map { it.activity })
            assertEquals(listOf("Leo"), state.dayOccurrences.map { it.kidName })
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }

    @Test
    fun nextAndPreviousDayShiftTheSelectionAndReExpand() =
        runTest {
            val today = LocalDate.now()
            val kid = Kid(id = 1L, name = "Leo")
            // Occurs only on today's weekday: exactly one occurrence today,
            // none on the neighbouring days.
            val activity =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(today.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(activity))
            val viewModel =
                DailyScheduleViewModel(
                    kidRepository = kidRepository,
                    activityRepository = activityRepository,
                    scheduleOverrideRepository = scheduleOverrideRepository,
                    selectedKidIds = setOf(1L),
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.nextDay()

            // Tomorrow is a different weekday, so nothing occurs there.
            assertEquals(0, viewModel.uiState.value.dayOccurrences.size)
            assertEquals(today.plusDays(1), viewModel.uiState.value.selectedDate)

            viewModel.previousDay()

            assertEquals(1, viewModel.uiState.value.dayOccurrences.size)
            assertEquals(today, viewModel.uiState.value.selectedDate)
        }

    @Test
    fun disabledActivitiesStayVisibleForTheirDays() =
        runTest {
            val today = LocalDate.now()
            val kid = Kid(id = 1L, name = "Leo")
            val disabled =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    isEnabled = false,
                    daysOfWeek = intArrayOf(today.dayOfWeek.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(disabled))
            val viewModel =
                DailyScheduleViewModel(
                    kidRepository = kidRepository,
                    activityRepository = activityRepository,
                    scheduleOverrideRepository = scheduleOverrideRepository,
                    selectedKidIds = setOf(1L),
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf(disabled), state.dayOccurrences.map { it.activity })
        }
}
