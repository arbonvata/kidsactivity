package com.arbonvata.kidstrackeractivity.widgets

import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.OccurrenceException
import com.arbonvata.kidstrackeractivity.data.encodeExceptions
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class AgendaSnapshotTest {
    private val date = LocalDate.of(2026, 10, 19)

    private fun occurrence(
        name: String,
        start: Int,
        end: Int,
        school: Boolean = false,
    ) = ActivityOccurrence(
        Activity(name = name, isSchoolActivity = school),
        "Leo",
        date,
        LocalTime.of(start, 0),
        LocalTime.of(end, 0),
    )

    @Test
    fun `ongoing school remains next until pickup and completed activities disappear`() {
        val school = occurrence("School", 8, 14, true)
        val swim = occurrence("Swim", 16, 17)
        val result = agendaSnapshot(listOf(occurrence("Breakfast", 7, 8), school, swim), date.atTime(12, 0))
        assertEquals(school, result.next)
        assertEquals(listOf(school, swim), result.remainingToday)
        assertEquals(swim, agendaSnapshot(listOf(school, swim), date.atTime(14, 1)).next)
    }

    @Test
    fun `tomorrow can be next without appearing in today agenda`() {
        val tomorrow = occurrence("Swim", 10, 11).copy(date = date.plusDays(1))
        val result = agendaSnapshot(listOf(tomorrow), date.atTime(22, 0))
        assertEquals(tomorrow, result.next)
        assertEquals(emptyList<ActivityOccurrence>(), result.remainingToday)
        assertNull(agendaSnapshot(emptyList(), date.atStartOfDay()).next)
    }

    @Test
    fun `widget uses effective times and excludes cancelled dates`() {
        val cancelled =
            Activity(
                id = 1,
                kidId = 1,
                startTime = "10:00",
                endTime = "11:00",
                daysOfWeek = intArrayOf(1),
                occurrenceExceptions = encodeExceptions(listOf(OccurrenceException(date))),
            )
        val moved =
            cancelled.copy(
                id = 2,
                occurrenceExceptions =
                    encodeExceptions(
                        listOf(
                            OccurrenceException(date, LocalTime.of(18, 0), LocalTime.of(19, 0)),
                        ),
                    ),
            )
        val occurrences = expandActivitiesForDateRange(listOf(Kid(id = 1)), listOf(cancelled, moved), date, date)
        val result = agendaSnapshot(occurrences, date.atTime(12, 0))
        assertEquals(2L, result.next?.activity?.id)
        assertEquals(LocalTime.of(18, 0), result.next?.startTime)
    }
}
