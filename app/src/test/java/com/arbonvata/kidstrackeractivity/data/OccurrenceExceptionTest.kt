package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class OccurrenceExceptionTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val kid = Kid(id = 1, name = "Leo")
    private val activity = Activity(id = 1, kidId = 1, name = "Swim", startTime = "17:00", endTime = "18:00", daysOfWeek = intArrayOf(1))

    @Test
    fun `cancellation affects only the selected date`() {
        activity.occurrenceExceptions = encodeExceptions(listOf(OccurrenceException(monday)))
        val result = expandActivitiesForDateRange(listOf(kid), listOf(activity), monday, monday.plusWeeks(1))
        assertEquals(listOf(monday.plusWeeks(1)), result.map { it.date })
    }

    @Test
    fun `changed times affect conflicts without changing the weekly template`() {
        val exception = OccurrenceException(monday, LocalTime.of(18, 0), LocalTime.of(19, 0))
        activity.occurrenceExceptions = encodeExceptions(listOf(exception))
        assertEquals(listOf(exception), decodeExceptions(activity.occurrenceExceptions))
        val second = activity.copy(id = 2, startTime = "18:30", endTime = "19:30", occurrenceExceptions = null)
        val result = expandActivitiesForDateRange(listOf(kid), listOf(activity, second), monday, monday.plusWeeks(1))
        assertEquals(1, findConflicts(result).size)
        assertEquals(30L, findConflicts(result).single().overlapMinutes)
        assertEquals(LocalTime.of(17, 0), result.first { it.date == monday.plusWeeks(1) }.startTime)
    }

    @Test
    fun `invalid and duplicate exception data is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { decodeExceptions("2026-10-19|18:00|17:00") }
        assertThrows(IllegalArgumentException::class.java) { decodeExceptions("2026-10-19||\n2026-10-19||") }
        assertEquals(emptyList<OccurrenceException>(), decodeExceptions(null))
    }

    @Test
    fun `legacy three-part lines still decode`() {
        assertEquals(
            listOf(OccurrenceException(monday, LocalTime.of(18, 0), LocalTime.of(19, 0))),
            decodeExceptions("2026-10-19|18:00|19:00"),
        )
        assertEquals(listOf(OccurrenceException(monday)), decodeExceptions("2026-10-19||"))
    }

    @Test
    fun `delegated date keeps its occurrence with the usual times`() {
        val exception = OccurrenceException(monday, delegatedTo = "Grandma")
        activity.occurrenceExceptions = encodeExceptions(listOf(exception))
        assertEquals(listOf(exception), decodeExceptions(activity.occurrenceExceptions))

        val result = expandActivitiesForDateRange(listOf(kid), listOf(activity), monday, monday)

        assertEquals(1, result.size)
        assertEquals("Grandma", result.single().delegatedTo)
        assertEquals(LocalTime.of(17, 0), result.single().startTime)
        assertEquals(LocalTime.of(18, 0), result.single().endTime)
    }

    @Test
    fun `delegation composes with changed times`() {
        val exception = OccurrenceException(monday, LocalTime.of(18, 0), LocalTime.of(19, 0), delegatedTo = "Grandpa")
        activity.occurrenceExceptions = encodeExceptions(listOf(exception))

        val result = expandActivitiesForDateRange(listOf(kid), listOf(activity), monday, monday)

        assertEquals(LocalTime.of(18, 0), result.single().startTime)
        assertEquals("Grandpa", result.single().delegatedTo)
    }

    @Test
    fun `delegated occurrences still conflict`() {
        // Delegation changes who drives, not the kid's calendar: the overlap
        // stays reportable.
        activity.occurrenceExceptions = encodeExceptions(listOf(OccurrenceException(monday, delegatedTo = "Grandma")))
        val second = activity.copy(id = 2, startTime = "17:30", endTime = "18:30", occurrenceExceptions = null)

        val result = expandActivitiesForDateRange(listOf(kid), listOf(activity, second), monday, monday)

        assertEquals(1, findConflicts(result).size)
    }

    @Test
    fun `delegated names must survive the encoding`() {
        assertThrows(IllegalArgumentException::class.java) { OccurrenceException(monday, delegatedTo = "a|b") }
        // An unnamed delegation round-trips as a four-part line with an
        // empty name — distinct from "not delegated".
        val unnamed = listOf(OccurrenceException(monday, delegatedTo = ""))
        assertEquals("2026-10-19|||", encodeExceptions(unnamed))
        assertEquals(unnamed, decodeExceptions("2026-10-19|||"))
        assertEquals("", decodeExceptions("2026-10-19|||").single().delegatedTo)
    }
}
