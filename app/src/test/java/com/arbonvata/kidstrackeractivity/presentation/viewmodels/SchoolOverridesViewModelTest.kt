package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coVerify
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolOverridesViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>()
    private val scheduleOverrideRepository = mockk<ScheduleOverrideRepository>(relaxUnitFun = true)

    @BeforeEach
    fun setUpActivitiesFlow() {
        every { activityRepository.getAllActivitiesFlow() } returns flowOf(emptyList())
    }

    @Test
    fun autoSelectsTheFirstKidAndExposesTheirOverrides() =
        runTest {
            val kids =
                listOf(
                    Kid(id = 1L, name = "Leo"),
                    Kid(id = 2L, name = "Mia"),
                )
            val overrides =
                listOf(
                    DailySchedule(id = 10L, kidId = 1L, date = "2026-10-21", endTime = "12:30", isNoSchool = false),
                    DailySchedule(id = 11L, kidId = 2L, date = "2026-10-22", endTime = "", isNoSchool = true),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(kids)
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(overrides)
            val viewModel = SchoolOverridesViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1L, state.selectedKidId)
            assertEquals(setOf(LocalDate.of(2026, 10, 21)), state.overrides.keys)
            verify { kidRepository.getAllKidsFlow() }
            verify { scheduleOverrideRepository.getAllOverridesFlow() }
        }

    @Test
    fun selectingAnotherKidFiltersToTheirOverrides() =
        runTest {
            val kids =
                listOf(
                    Kid(id = 1L, name = "Leo"),
                    Kid(id = 2L, name = "Mia"),
                )
            val overrides =
                listOf(
                    DailySchedule(id = 10L, kidId = 1L, date = "2026-10-21", endTime = "12:30", isNoSchool = false),
                    DailySchedule(id = 11L, kidId = 2L, date = "2026-10-22", endTime = "", isNoSchool = true),
                )
            every { kidRepository.getAllKidsFlow() } returns flowOf(kids)
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(overrides)
            val viewModel = SchoolOverridesViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.onKidSelected(2L)

            assertEquals(2L, viewModel.uiState.value.selectedKidId)
            assertEquals(setOf(LocalDate.of(2026, 10, 22)), viewModel.uiState.value.overrides.keys)
        }

    @Test
    fun setDayOverridePersistsTheFormattedEndTime() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(Kid(id = 1L, name = "Leo")))
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
            val viewModel = SchoolOverridesViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.setDayOverride(LocalDate.of(2026, 10, 21), LocalTime.of(12, 30), isNoSchool = false)

            coVerify {
                scheduleOverrideRepository.setOverride(
                    kidId = 1L,
                    date = LocalDate.of(2026, 10, 21),
                    endTime = "12:30",
                    isNoSchool = false,
                )
            }
        }

    @Test
    fun setDayOverrideForNoSchoolDropsTheEndTime() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(Kid(id = 1L, name = "Leo")))
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
            val viewModel = SchoolOverridesViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.setDayOverride(LocalDate.of(2026, 10, 21), LocalTime.of(12, 30), isNoSchool = true)

            coVerify {
                scheduleOverrideRepository.setOverride(
                    kidId = 1L,
                    date = LocalDate.of(2026, 10, 21),
                    endTime = null,
                    isNoSchool = true,
                )
            }
        }

    @Test
    fun withoutKidsNothingIsSelectedAndNothingPersists() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(emptyList())
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
            val viewModel = SchoolOverridesViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertNull(viewModel.uiState.value.selectedKidId)

            viewModel.setDayOverride(LocalDate.of(2026, 10, 21), null, isNoSchool = true)

            coVerify(exactly = 0) { scheduleOverrideRepository.setOverride(any(), any(), any(), any()) }
            assertTrue(
                viewModel.uiState.value.overrides
                    .isEmpty(),
            )
        }

    @Test
    fun schoolWeekdaysComeFromEnabledSchoolActivitiesOnly() =
        runTest {
            every { kidRepository.getAllKidsFlow() } returns flowOf(listOf(Kid(id = 1L, name = "Leo")))
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
            every { activityRepository.getAllActivitiesFlow() } returns
                flowOf(
                    listOf(
                        Activity(
                            id = 1L,
                            kidId = 1L,
                            name = "School",
                            startTime = "08:00",
                            endTime = "14:00",
                            isSchoolActivity = true,
                            daysOfWeek = intArrayOf(1, 2),
                        ),
                        Activity(
                            id = 2L,
                            kidId = 1L,
                            name = "Muted School",
                            startTime = "08:00",
                            endTime = "14:00",
                            isEnabled = false,
                            isSchoolActivity = true,
                            daysOfWeek = intArrayOf(3),
                        ),
                        Activity(
                            id = 3L,
                            kidId = 1L,
                            name = "Football",
                            startTime = "17:00",
                            endTime = "18:30",
                            daysOfWeek = intArrayOf(4),
                        ),
                    ),
                )
            val viewModel = SchoolOverridesViewModel(kidRepository, activityRepository, scheduleOverrideRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertEquals(setOf(1, 2), viewModel.uiState.value.schoolWeekdays)
            verify { activityRepository.getAllActivitiesFlow() }
        }
}
