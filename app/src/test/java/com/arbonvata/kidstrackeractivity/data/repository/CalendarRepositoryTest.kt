package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.calendar.CalendarImport
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarRepositoryTest {
    private val kids = mockk<Box<Kid>>()
    private val activities = mockk<Box<Activity>>()
    private val store = mockk<BoxStore>()
    private val repository = CalendarRepositoryImpl(store, UnconfinedTestDispatcher())
    private val activity =
        Activity(
            name = "Swim",
            startTime = "17:00",
            endTime = "18:00",
            daysOfWeek = intArrayOf(1),
            isPeriodic = false,
            firstDate = "2026-10-19",
            location = "Pool",
            pickupAdult = "Alex",
        )

    @BeforeEach
    fun setup() {
        every { store.boxFor(Kid::class.java) } returns kids
        every { store.boxFor(Activity::class.java) } returns activities
        every { store.runInTx(any()) } answers { firstArg<Runnable>().run() }
        every { kids.get(7) } returns Kid(id = 7, name = "Leo")
        every { kids.all } returns listOf(Kid(id = 7, name = "Leo"))
        every { activities.all } returns emptyList()
        every { activities.put(any<Activity>()) } returns 20L
    }

    @Test
    fun `import existing child adds fresh records without changing child or clearing existing data`() =
        runTest {
            val captured = slot<Activity>()
            every { activities.put(capture(captured)) } returns 20L
            val result = repository.importCalendar(CalendarImport("Other name", listOf(activity)), 7, "Ignored")
            assertEquals(CalendarImportResult(1, 0), result)
            assertEquals(7L, captured.captured.kidId)
            assertEquals(0L, captured.captured.id)
            assertEquals("Alex", captured.captured.pickupAdult)
            verify(exactly = 0) { kids.put(any<Kid>()) }
            verify(exactly = 1) { store.runInTx(any()) }
        }

    @Test
    fun `reimport skips duplicate activities in database and within the file`() =
        runTest {
            every { activities.all } returns listOf(activity.copy(id = 10, kidId = 7))
            val result = repository.importCalendar(CalendarImport("Leo", listOf(activity, activity)), 7, "")
            assertEquals(CalendarImportResult(0, 2), result)
            verify(exactly = 0) { activities.put(any<Activity>()) }
        }

    @Test
    fun `creates a new child and all activities in one transaction`() =
        runTest {
            val child = slot<Kid>()
            val imported = slot<Activity>()
            every { kids.put(capture(child)) } returns 8L
            every { activities.put(capture(imported)) } returns 20L
            repository.importCalendar(CalendarImport("Emma", listOf(activity)), null, " Emma ")
            assertEquals("Emma", child.captured.name)
            assertEquals(8L, imported.captured.kidId)
            verify(exactly = 1) { store.runInTx(any()) }
        }

    @Test
    fun `invalid file never starts transaction and missing targets never write activities`() =
        runTest {
            assertTrue(runCatching { repository.preview("not a calendar") }.isFailure)
            verify(exactly = 0) { store.runInTx(any()) }
            every { kids.get(7) } returns null
            assertTrue(runCatching { repository.importCalendar(CalendarImport("Leo", listOf(activity)), 7, "") }.isFailure)
            assertTrue(runCatching { repository.importCalendar(CalendarImport("Leo", listOf(activity)), null, "Leo") }.isFailure)
            verify(exactly = 0) { activities.put(any<Activity>()) }
            verify(exactly = 0) { kids.put(any<Kid>()) }
        }
}
