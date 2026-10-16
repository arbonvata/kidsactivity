package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ConflictViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>()
    private val scheduleOverrideRepository = mockk<ScheduleOverrideRepository>()

    @BeforeEach
    fun setUpOverridesFlow() {
        every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
    }

    // Mirrors the ViewModel's horizon: every Monday in [today, today + 30].
    private fun mondaysInHorizon(): List<LocalDate> {
        val today = LocalDate.now()
        return generateSequence(today) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today.plusDays(30)) }
            .filter { it.dayOfWeek == DayOfWeek.MONDAY }
            .toList()
    }

    @Test
    fun overlappingSameKidActivitiesConflictOnEveryHorizonMonday() =
        runTest {
            val kid = Kid(id = 1L, name = "Leo")
            val football =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            val piano =
                Activity(
                    id = 2L,
                    kidId = 1L,
                    name = "Piano",
                    startTime = "17:30",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(football, piano))
            val viewModel = ConflictViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(mondaysInHorizon(), state.conflicts.map { it.activity1.date })
            assertTrue(state.conflicts.all { it.overlapMinutes == 60L })
            verify { kidRepository.getAllKidsFlow() }
            verify { activityRepository.getAllActivitiesFlow() }
        }

    @Test
    fun mutedActivitiesAreSkippedLikeAlarms() =
        runTest {
            // Pins that conflicts use the expansion default: disabled
            // activities keep alarms off and drop out of conflicts too.
            val kid = Kid(id = 1L, name = "Leo")
            val football =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            val mutedPiano =
                Activity(
                    id = 2L,
                    kidId = 1L,
                    name = "Piano",
                    startTime = "17:30",
                    endTime = "18:30",
                    isEnabled = false,
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(football, mutedPiano))
            val viewModel = ConflictViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val conflicts = viewModel.uiState.value.conflicts
            assertTrue(conflicts.isEmpty())
        }

    @Test
    fun noSchoolOverrideDropsOnlyThatDaysConflict() =
        runTest {
            val kid = Kid(id = 1L, name = "Leo")
            val school =
                Activity(
                    id = 1L,
                    kidId = 1L,
                    name = "School",
                    startTime = "08:00",
                    endTime = "14:00",
                    isSchoolActivity = true,
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            val football =
                Activity(
                    id = 2L,
                    kidId = 1L,
                    name = "Football",
                    startTime = "13:00",
                    endTime = "14:30",
                    daysOfWeek = intArrayOf(DayOfWeek.MONDAY.value),
                )
            val mondays = mondaysInHorizon()
            val noSchoolFirstMonday =
                DailySchedule(
                    kidId = 1L,
                    date = mondays.first().toString(),
                    endTime = "",
                    isNoSchool = true,
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(kid))
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(listOf(school, football))
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(listOf(noSchoolFirstMonday))
            val viewModel = ConflictViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            // Without the override every horizon Monday conflicts; the
            // No-School day hides the school occurrence so that Monday drops.
            val conflicts = viewModel.uiState.value.conflicts
            assertEquals(mondays.drop(1), conflicts.map { it.activity1.date })
        }

    @Test
    fun emptyKidsAndActivitiesYieldNoConflicts() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(emptyList())
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(emptyList())
            val viewModel = ConflictViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.conflicts.isEmpty())
        }
}
