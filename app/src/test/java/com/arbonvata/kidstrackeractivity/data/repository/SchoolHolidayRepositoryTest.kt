package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolHolidayRepositoryTest {
    private val schedules = mockk<Box<DailySchedule>>()
    private val kids = mockk<Box<Kid>>()
    private val store = mockk<BoxStore>()
    private val rows = mutableMapOf<Long, DailySchedule>()
    private var nextId = 10L
    private lateinit var repository: ScheduleOverrideRepositoryImpl
    private val monday = LocalDate.of(2026, 11, 2)

    @BeforeEach
    fun setup() {
        every { store.boxFor(DailySchedule::class.java) } returns schedules
        every { store.boxFor(Kid::class.java) } returns kids
        every { kids.get(any<Long>()) } answers { firstArg<Long>().takeIf { it in 1L..2L }?.let { Kid(id = it) } }
        every { store.runInTx(any()) } answers { firstArg<Runnable>().run() }
        every { schedules.all } answers { rows.values.toList() }
        every { schedules.put(any<DailySchedule>()) } answers {
            val row = firstArg<DailySchedule>()
            val id = row.id.takeIf { it != 0L } ?: nextId++
            rows[id] = row.copy(id = id)
            id
        }
        every { schedules.remove(any<Long>()) } answers { rows.remove(firstArg()) != null }
        repository = ScheduleOverrideRepositoryImpl(store, UnconfinedTestDispatcher())
    }

    @Test
    fun `inclusive range updates all selected children atomically and leaves regular activities visible`() =
        runTest {
            repository.setNoSchoolRange(setOf(1, 2), monday, monday.plusDays(2), true)
            assertEquals(6, rows.size)
            assertTrue(rows.values.all { it.isNoSchool })
            assertEquals(
                setOf(monday.toString(), monday.plusDays(1).toString(), monday.plusDays(2).toString()),
                rows.values.map { it.date }.toSet(),
            )
            val child = Kid(id = 1)
            val school =
                Activity(id = 1, kidId = 1, startTime = "08:00", endTime = "14:00", daysOfWeek = intArrayOf(1, 3), isSchoolActivity = true)
            val regular = school.copy(id = 2, isSchoolActivity = false)
            val result =
                expandActivitiesForDateRange(
                    listOf(child),
                    listOf(school, regular),
                    monday,
                    monday.plusDays(2),
                    overrides = rows.values.toList(),
                )
            assertEquals(listOf(2L, 2L), result.map { it.activity.id })
            verify(exactly = 1) { store.runInTx(any()) }
        }

    @Test
    fun `restoring a range retains previous early release times and does not alter other children`() =
        runTest {
            rows[1] = DailySchedule(id = 1, kidId = 1, date = monday.toString(), endTime = "13:00")
            rows[2] = DailySchedule(id = 2, kidId = 2, date = monday.toString(), isNoSchool = true)
            repository.setNoSchoolRange(setOf(1), monday, monday.plusDays(1), true)
            assertTrue(rows.getValue(1).isNoSchool)
            assertEquals("13:00", rows.getValue(1).endTime)
            repository.setNoSchoolRange(setOf(1), monday, monday.plusDays(1), false)
            assertEquals(false, rows.getValue(1).isNoSchool)
            assertEquals("13:00", rows.getValue(1).endTime)
            assertTrue(rows.getValue(2).isNoSchool)
            assertEquals(2, rows.size)
        }

    @Test
    fun `invalid ranges and missing children never write partial holidays`() =
        runTest {
            assertTrue(runCatching { repository.setNoSchoolRange(setOf(1), monday, monday.minusDays(1), true) }.isFailure)
            assertTrue(runCatching { repository.setNoSchoolRange(setOf(1), monday, monday.plusDays(366), true) }.isFailure)
            verify(exactly = 0) { store.runInTx(any()) }
            assertTrue(runCatching { repository.setNoSchoolRange(setOf(1, 99), monday, monday.plusDays(1), true) }.isFailure)
            verify(exactly = 0) { schedules.put(any<DailySchedule>()) }
            assertTrue(rows.isEmpty())
        }
}
