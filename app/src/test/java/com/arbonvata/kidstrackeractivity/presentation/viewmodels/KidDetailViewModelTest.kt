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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class KidDetailViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>(relaxUnitFun = true)

    @Test
    fun exposesKidAndActivitiesOnceLoaded() =
        runTest {
            val kid = Kid(id = 7L, name = "Leo", schoolName = "Greenfield")
            val activities =
                listOf(
                    Activity(id = 1L, kidId = 7L, name = "Football", startTime = "17:00", endTime = "18:30"),
                )
            coEvery { kidRepository.getKid(7L) } returns kid
            every { activityRepository.getActivitiesForKidFlow(7L) } returns flowOf(activities)
            val viewModel = KidDetailViewModel(7L, kidRepository, activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(kid, state.kid)
            assertEquals(activities, state.activities)
        }

    @Test
    fun setActivityEnabledPersistsThroughTheRepository() =
        runTest {
            every { activityRepository.getActivitiesForKidFlow(7L) } returns flowOf(emptyList())
            val viewModel = KidDetailViewModel(7L, kidRepository, activityRepository)

            viewModel.setActivityEnabled(3L, enabled = false)
            viewModel.setActivityEnabled(3L, enabled = true)

            coVerify {
                activityRepository.setActivityEnabled(3L, false)
                activityRepository.setActivityEnabled(3L, true)
            }
        }

    @Test
    fun deleteAllActivitiesTargetsOnlyThisKid() =
        runTest {
            every { activityRepository.getActivitiesForKidFlow(7L) } returns flowOf(emptyList())
            val viewModel = KidDetailViewModel(7L, kidRepository, activityRepository)

            viewModel.deleteAllActivities()

            coVerify { activityRepository.deleteAllActivitiesForKid(7L) }
        }
}
