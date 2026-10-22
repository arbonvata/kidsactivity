package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleCopyRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityCopyViewModelTest {
    @RegisterExtension val mainDispatcherRule = MainDispatcherRule()
    private val copyRepository = mockk<ScheduleCopyRepository>()
    private val activities = mockk<ActivityRepository>()
    private val kids =
        mockk<KidRepository> {
            every { getAllKidsFlow() } returns flowOf(listOf(Kid(id = 1), Kid(id = 2)))
        }

    @Test
    fun `whole schedule requires another child and guards repeated copy after success`() =
        runTest {
            coEvery { activities.getAllActivities() } returns listOf(Activity(id = 10, kidId = 1))
            coEvery { copyRepository.copy(any(), any(), any(), any(), any()) } returns 1
            val vm = ActivityCopyViewModel(1, 0, copyRepository, activities, kids)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            assertEquals(
                listOf(2L),
                vm.uiState.value.kids
                    .map { it.id },
            )
            vm.copy()
            coVerify(exactly = 0) { copyRepository.copy(any(), any(), any(), any(), any()) }
            vm.selectKid(2)
            vm.copy()
            vm.copy()
            assertEquals(1, vm.uiState.value.copiedCount)
            coVerify(exactly = 1) { copyRepository.copy(1, 2, null, any(), any()) }
        }

    @Test
    fun `copy failures clear busy state and allow retry`() =
        runTest {
            coEvery { activities.getActivity(10) } returns Activity(id = 10, kidId = 1, name = "Swim")
            coEvery { copyRepository.copy(any(), any(), any(), any(), any()) } throws IllegalStateException("Unavailable")
            val vm = ActivityCopyViewModel(1, 10, copyRepository, activities, kids)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.copy()
            assertTrue(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isWorking)
            coEvery { copyRepository.copy(any(), any(), any(), any(), any()) } returns 1
            vm.copy()
            assertEquals(1, vm.uiState.value.copiedCount)
            assertFalse(vm.uiState.value.error)
        }
}
