package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tests the weekly-recurring vs one-off expansion rules of
 * [expandActivitiesForDateRange]. Dates are fixed October 2026 days so the
 * weekday arithmetic is stable: Oct 19/26 are Mondays, Oct 20/27 Tuesdays,
 * Oct 21/28 Wednesdays, Oct 22/29 Thursdays.
 */
class ActivityOccurenceTest {
    private val kids = listOf(Kid(id = 1L, name = "Leo"))

    private fun activity(
        daysOfWeek: IntArray,
        isPeriodic: Boolean,
        firstDate: String?,
    ): Activity =
        Activity(
            id = 1L,
            kidId = 1L,
            name = "Football",
            startTime = "17:00",
            endTime = "18:30",
            daysOfWeek = daysOfWeek,
            isPeriodic = isPeriodic,
            firstDate = firstDate,
        )

    @Test
    fun periodicActivityRepeatsOnItsWeekdaysEveryWeek() {
        // Periodic means unbounded: every Monday in the range, even before
        // the creation anchor (firstDate only bounds one-offs).
        val activity = activity(intArrayOf(DayOfWeek.MONDAY.value), isPeriodic = true, firstDate = "2026-10-19")

        val occurrences = expandActivitiesForDateRange(kids, listOf(activity), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))

        assertEquals(
            listOf("2026-10-05", "2026-10-12", "2026-10-19", "2026-10-26"),
            occurrences.map { it.date.toString() },
        )
    }

    @Test
    fun oneOffActivityOccursOnlyDuringItsCreationWeek() {
        val activity = activity(intArrayOf(DayOfWeek.MONDAY.value), isPeriodic = false, firstDate = "2026-10-19")

        val occurrences = expandActivitiesForDateRange(kids, listOf(activity), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))

        assertEquals(listOf("2026-10-19"), occurrences.map { it.date.toString() })
    }

    @Test
    fun oneOffActivityWithMultipleDaysOccursOncePerSelectedDay() {
        val activity = activity(intArrayOf(DayOfWeek.TUESDAY.value, DayOfWeek.THURSDAY.value), isPeriodic = false, firstDate = "2026-10-20")

        val occurrences = expandActivitiesForDateRange(kids, listOf(activity), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))

        assertEquals(listOf("2026-10-20", "2026-10-22"), occurrences.map { it.date.toString() })
    }

    @Test
    fun oneOffActivityWithMissingFirstDateIsUnboundedForLegacyRows() {
        // Rows written before the column existed read firstDate as null from
        // the DB; old backups import it as blank. Both keep old behaviour.
        val legacyNull = activity(intArrayOf(DayOfWeek.MONDAY.value), isPeriodic = false, firstDate = null)
        val legacyBlank =
            activity(intArrayOf(DayOfWeek.MONDAY.value), isPeriodic = false, firstDate = "")
                .copy(id = 2L)

        val occurrences =
            expandActivitiesForDateRange(kids, listOf(legacyNull, legacyBlank), LocalDate.of(2026, 10, 19), LocalDate.of(2026, 11, 1))

        assertEquals(
            listOf("2026-10-19", "2026-10-19", "2026-10-26", "2026-10-26"),
            occurrences.map { it.date.toString() },
        )
    }

    @Test
    fun disabledActivitiesNeverOccur() {
        val activity = activity(intArrayOf(DayOfWeek.MONDAY.value), isPeriodic = true, firstDate = "2026-10-19").copy(isEnabled = false)

        val occurrences = expandActivitiesForDateRange(kids, listOf(activity), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun includeDisabledKeepsMutedActivitiesForTheScheduleViews() {
        // The schedule views show muted activities (marked); alarms and
        // conflicts use the default and skip them.
        val activity = activity(intArrayOf(DayOfWeek.MONDAY.value), isPeriodic = true, firstDate = "2026-10-19").copy(isEnabled = false)

        val occurrences =
            expandActivitiesForDateRange(
                kids,
                listOf(activity),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                includeDisabled = true,
            )

        assertEquals(
            listOf("2026-10-05", "2026-10-12", "2026-10-19", "2026-10-26"),
            occurrences.map { it.date.toString() },
        )
    }
}
