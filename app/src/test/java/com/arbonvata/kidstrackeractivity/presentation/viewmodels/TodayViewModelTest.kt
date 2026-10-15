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
class TodayViewModelTest {
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
    fun expandsTodaysOccurrencesWithKidNames() =
        runTest {
            val today = LocalDate.now()
            val kid = Kid(id = 1L, name = "Leo")
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
            val viewModel = TodayViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf(activity), state.todayOccurrences.map { it.activity })
            assertEquals(listOf("Leo"), state.todayOccurrences.map { it.kidName })
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }

    @Test
    fun disabledActivitiesStayVisibleForTheSchedule() =
        runTest {
            // Pins that Today passes includeDisabled = true: muted activities
            // stay listed (rendered dimmed) while alarms skip them.
            val today = LocalDate.now()
            val kid = Kid(id = 1L, name = "Leo")
            val muted =
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
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(muted))
            val viewModel = TodayViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertEquals(
                listOf(muted),
                viewModel.uiState.value.todayOccurrences
                    .map { it.activity },
            )
        }
}
