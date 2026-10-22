package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleCopyRepositoryTest {
    private val kids = mockk<Box<Kid>>()
    private val activities = mockk<Box<Activity>>()
    private val store = mockk<BoxStore>()
    private val copied = mutableListOf<Activity>()
    private val repository = ScheduleCopyRepositoryImpl(store, UnconfinedTestDispatcher())
    private val week = LocalDate.of(2026, 11, 4)
    private val original =
        Activity(
            id = 10,
            kidId = 1,
            name = "Swim",
            startTime = "17:00",
            endTime = "18:00",
            daysOfWeek = intArrayOf(1, 3),
            isEnabled = false,
            isPeriodic = false,
            firstDate = "2026-10-19",
            location = "Pool",
            pickupAdult = "Alex",
            pickupNotes = "Side door",
            occurrenceExceptions = "2026-10-19||",
        )

    @BeforeEach
    fun setup() {
        every { store.boxFor(Kid::class.java) } returns kids
        every { store.boxFor(Activity::class.java) } returns activities
        every { store.runInTx(any()) } answers { firstArg<Runnable>().run() }
        every { kids.get(1) } returns Kid(id = 1)
        every { kids.get(2) } returns Kid(id = 2)
        every { activities.get(10) } returns original
        every { activities.put(any<Activity>()) } answers {
            copied.add(firstArg())
            20L
        }
    }

    @Test
    fun `duplicate assigns a new identity and moves a one-off without mutating source`() =
        runTest {
            assertEquals(1, repository.copy(1, 2, 10, week, " New swim "))
            val copy = copied.single()
            assertEquals(0L, copy.id)
            assertEquals(2L, copy.kidId)
            assertEquals("New swim", copy.name)
            assertEquals("2026-11-02", copy.firstDate)
            assertEquals("Pool", copy.location)
            assertEquals("Alex", copy.pickupAdult)
            assertEquals("Side door", copy.pickupNotes)
            assertEquals(false, copy.isEnabled)
            assertNull(copy.occurrenceExceptions)
            assertNotSame(original.daysOfWeek, copy.daysOfWeek)
            assertEquals(10L, original.id)
            assertEquals("2026-10-19||", original.occurrenceExceptions)
            verify(exactly = 1) { store.runInTx(any()) }
        }

    @Test
    fun `full schedule preserves weekly school patterns and uses one transaction`() =
        runTest {
            val school = original.copy(id = 11, isSchoolActivity = true, isPeriodic = true)
            val builder = mockk<QueryBuilder<Activity>>()
            val query = mockk<Query<Activity>>(relaxUnitFun = true)
            every { activities.query() } returns builder
            every { builder.equal(any(), 1L) } returns builder
            every { builder.build() } returns query
            every { query.find() } returns listOf(original, school)
            assertEquals(2, repository.copy(1, 2, null, week, null))
            assertEquals(listOf("2026-11-02", "2026-10-19"), copied.map { it.firstDate })
            assertTrue(copied.last().isSchoolActivity)
            assertTrue(copied.last().isPeriodic)
            verify(exactly = 1) { store.runInTx(any()) }
            verify { query.close() }
        }

    @Test
    fun `invalid targets and wrong source activity are rejected before writing`() =
        runTest {
            every { kids.get(2) } returns null
            assertTrue(runCatching { repository.copy(1, 2, 10, week, "Swim") }.isFailure)
            every { kids.get(2) } returns Kid(id = 2)
            every { activities.get(10) } returns original.copy(kidId = 3)
            assertTrue(runCatching { repository.copy(1, 2, 10, week, "Swim") }.isFailure)
            assertTrue(runCatching { repository.copy(1, 1, null, week, null) }.isFailure)
            verify(exactly = 0) { activities.put(any<Activity>()) }
        }
}
