package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.alarms.computeUpcomingAlarms
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.widgets.agendaSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecurrenceEndDateTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val kid = Kid(id = 1, name = "Leo")
    private val activity =
        Activity(
            id = 1,
            kidId = 1,
            startTime = "17:00",
            endTime = "18:00",
            daysOfWeek = intArrayOf(1, 3),
            lastDate = "2026-11-04",
        )

    @Test
    fun `last date is included and occurrences stop across all subsequent weeks`() {
        val occurrences = expandActivitiesForDateRange(listOf(kid), listOf(activity), monday, monday.plusMonths(2))
        assertEquals(listOf(monday, monday.plusDays(2)), occurrences.map { it.date })
        assertTrue(expandActivitiesForDateRange(listOf(kid), listOf(activity), monday.plusWeeks(1), monday.plusMonths(1)).isEmpty())
    }

    @Test
    fun `clearing end date restores unlimited recurrence and legacy rows keep repeating`() {
        for (end in listOf(null, "")) {
            val result =
                expandActivitiesForDateRange(listOf(kid), listOf(activity.copy(lastDate = end)), monday, monday.plusWeeks(2))
            assertEquals(5, result.size)
        }
    }

    @Test
    fun `school overrides and occurrence exceptions cannot extend an ended schedule`() {
        val school =
            activity.copy(
                isSchoolActivity = true,
                occurrenceExceptions =
                    encodeExceptions(
                        listOf(OccurrenceException(monday.plusWeeks(1), java.time.LocalTime.NOON, java.time.LocalTime.of(13, 0))),
                    ),
            )
        val overrides = listOf(DailySchedule(kidId = 1, date = monday.plusWeeks(1).toString(), endTime = "19:00"))
        val result = expandActivitiesForDateRange(listOf(kid), listOf(school), monday, monday.plusWeeks(2), overrides = overrides)
        assertEquals(2, result.size)
    }

    @Test
    fun `alarms and widget exclude dates after end and conflicts do not reappear`() {
        val now = monday.plusDays(3).atStartOfDay()
        val occurrences =
            expandActivitiesForDateRange(listOf(kid), listOf(activity, activity.copy(id = 2)), now.toLocalDate(), monday.plusMonths(1))
        assertTrue(findConflicts(occurrences).isEmpty())
        assertNull(agendaSnapshot(occurrences, now).next)
        assertTrue(computeUpcomingAlarms(listOf(kid), listOf(activity), 10, 15, now).isEmpty())
        assertEquals(2, computeUpcomingAlarms(listOf(kid), listOf(activity), 10, 15, monday.atStartOfDay()).size)
    }

    @Test
    fun `one-offs respect the existing last date cutoff`() {
        val oneOff = activity.copy(isPeriodic = false, firstDate = monday.toString(), lastDate = monday.minusDays(1).toString())
        assertEquals(0, expandActivitiesForDateRange(listOf(kid), listOf(oneOff), monday, monday.plusWeeks(2)).size)
    }
}
