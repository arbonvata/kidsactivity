package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.calendar.CalendarImport
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.CalendarImportResult
import com.arbonvata.kidstrackeractivity.data.repository.CalendarRepository
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarShareViewModelTest {
    @RegisterExtension val mainDispatcherRule = MainDispatcherRule()
    private val repository = mockk<CalendarRepository>()
    private val kids =
        mockk<KidRepository> {
            every { getAllKidsFlow() } returns flowOf(listOf(Kid(id = 7, name = "Leo")))
        }
    private val preview = CalendarImport("Leo", listOf(Activity(name = "Swim")))

    @Test
    fun `single activity export does not require choosing a child and reports errors`() =
        runTest {
            coEvery { repository.exportActivity(10) } returns "single-event-calendar"
            val vm = CalendarShareViewModel(repository, kids)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            assertEquals("single-event-calendar", vm.exportActivity(10))
            assertFalse(vm.uiState.value.isWorking)
            coVerify(exactly = 0) { repository.export(any()) }
            coEvery { repository.exportActivity(10) } throws IllegalArgumentException("Activity no longer exists")
            assertNull(vm.exportActivity(10))
            assertEquals("Activity no longer exists", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isWorking)
        }

    @Test
    fun `preview chooses matching child but waits for confirmation and cancel writes nothing`() =
        runTest {
            coEvery { repository.preview("calendar") } returns preview
            val vm = CalendarShareViewModel(repository, kids)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.preview("calendar")
            assertEquals(7L, vm.uiState.value.targetKidId)
            assertNotNull(vm.uiState.value.pending)
            vm.dismissImport()
            assertNull(vm.uiState.value.pending)
            coVerify(exactly = 0) { repository.importCalendar(any(), any(), any()) }
        }

    @Test
    fun `confirmed import reports added and duplicate counts and clears preview`() =
        runTest {
            coEvery { repository.preview("calendar") } returns preview
            coEvery { repository.importCalendar(preview, 7, "Leo") } returns CalendarImportResult(1, 2)
            val vm = CalendarShareViewModel(repository, kids)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.preview("calendar")
            vm.importCalendar()
            vm.importCalendar()
            assertEquals(CalendarImportResult(1, 2), vm.uiState.value.result)
            assertNull(vm.uiState.value.pending)
            assertFalse(vm.uiState.value.isWorking)
            coVerify(exactly = 1) { repository.importCalendar(preview, 7, "Leo") }
        }

    @Test
    fun `parse errors and export failures are visible and clear busy state`() =
        runTest {
            coEvery { repository.preview(any()) } throws IllegalArgumentException("Unsupported recurrence")
            coEvery { repository.export(7) } throws IllegalStateException("Unable to export")
            val vm = CalendarShareViewModel(repository, kids)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.preview("broken")
            assertEquals("Unsupported recurrence", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isWorking)
            vm.selectExportKid(7)
            assertNull(vm.export())
            assertEquals("Unable to export", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isWorking)
        }
}
