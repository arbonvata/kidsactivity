package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests the pairwise overlap detection of [findConflicts]. Fixed October
 * 2026 dates so weekday arithmetic is stable; Oct 19 and 26 are Mondays.
 */
class ConflictTest {
    private val monday = LocalDate.of(2026, 10, 19)

    private fun occurrence(
        activityId: Long,
        kidId: Long = 1L,
        kidName: String = "Leo",
        date: LocalDate = monday,
        start: LocalTime,
        end: LocalTime,
    ): ActivityOccurrence =
        ActivityOccurrence(
            activity =
                Activity(
                    id = activityId,
                    kidId = kidId,
                    name = "Activity $activityId",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(1),
                ),
            kidName = kidName,
            date = date,
            startTime = start,
            endTime = end,
        )

    @Test
    fun overlappingPairIsReportedWithIntersectionMinutes() {
        val first = occurrence(1, start = LocalTime.of(17, 0), end = LocalTime.of(18, 30))
        val second = occurrence(2, start = LocalTime.of(17, 30), end = LocalTime.of(18, 30))

        val conflicts = findConflicts(listOf(first, second))

        assertEquals(1, conflicts.size)
        assertEquals(first, conflicts.single().activity1)
        assertEquals(second, conflicts.single().activity2)
        assertEquals(60L, conflicts.single().overlapMinutes)
    }

    @Test
    fun touchingRangesDoNotConflict() {
        val first = occurrence(1, start = LocalTime.of(17, 0), end = LocalTime.of(18, 0))
        val second = occurrence(2, start = LocalTime.of(18, 0), end = LocalTime.of(19, 0))

        assertTrue(findConflicts(listOf(first, second)).isEmpty())
    }

    @Test
    fun containedActivityOverlapsOnlyUntilItsOwnEnd() {
        // Piano 17:15–17:45 inside Football 17:00–19:00: the intersection is
        // 30 minutes, not the 105 minutes until Football's end.
        val first = occurrence(1, start = LocalTime.of(17, 0), end = LocalTime.of(19, 0))
        val second = occurrence(2, start = LocalTime.of(17, 15), end = LocalTime.of(17, 45))

        val conflicts = findConflicts(listOf(first, second))

        assertEquals(30L, conflicts.single().overlapMinutes)
    }

    @Test
    fun differentKidsOrDifferentDatesDoNotConflict() {
        val leoFootball = occurrence(1, start = LocalTime.of(17, 0), end = LocalTime.of(18, 30))
        val miaFootball =
            occurrence(2, kidId = 2L, kidName = "Mia", start = LocalTime.of(17, 0), end = LocalTime.of(18, 30))
        val nextWeekPiano =
            occurrence(3, start = LocalTime.of(17, 0), end = LocalTime.of(18, 30), date = monday.plusWeeks(1))

        assertTrue(findConflicts(listOf(leoFootball, miaFootball, nextWeekPiano)).isEmpty())
    }

    @Test
    fun invertedRangeIsIgnored() {
        // End at or before start (overnight ranges are not representable) —
        // the pair is skipped even though the ranges would "overlap".
        val inverted = occurrence(1, start = LocalTime.of(18, 30), end = LocalTime.of(17, 0))
        val normal = occurrence(2, start = LocalTime.of(17, 30), end = LocalTime.of(18, 30))

        assertTrue(findConflicts(listOf(inverted, normal)).isEmpty())
    }

    @Test
    fun threeWayClashReportsEveryOverlappingPair() {
        // Football 17:00–19:00 contains Piano 17:30–18:00 and also overlaps
        // Swimming 18:30–19:00 — all three pairs must be listed, otherwise
        // resolving the reported pair leaves the Football–Swimming clash on
        // the same day.
        val football = occurrence(1, start = LocalTime.of(17, 0), end = LocalTime.of(19, 0))
        val piano = occurrence(2, start = LocalTime.of(17, 30), end = LocalTime.of(18, 0))
        val swimming = occurrence(3, start = LocalTime.of(18, 30), end = LocalTime.of(19, 0))

        val conflicts = findConflicts(listOf(football, piano, swimming))

        val pairs = conflicts.map { it.activity1 to it.activity2 }
        assertEquals(
            listOf(football to piano, football to swimming),
            pairs,
        )
        assertEquals(30L, conflicts[0].overlapMinutes)
        assertEquals(30L, conflicts[1].overlapMinutes)
    }

    @Test
    fun conflictsAreSortedByDateThenStartTime() {
        val lateMonday =
            occurrence(1, start = LocalTime.of(19, 0), end = LocalTime.of(20, 0), date = monday)
        val earlyMonday =
            occurrence(2, start = LocalTime.of(19, 15), end = LocalTime.of(20, 0), date = monday)
        val nextMonday =
            occurrence(3, start = LocalTime.of(17, 0), end = LocalTime.of(18, 0), date = monday.plusWeeks(1))
        val nextMondayPiano =
            occurrence(4, start = LocalTime.of(17, 30), end = LocalTime.of(18, 0), date = monday.plusWeeks(1))

        val conflicts = findConflicts(listOf(nextMonday, nextMondayPiano, lateMonday, earlyMonday))

        assertEquals(
            listOf(monday, monday.plusWeeks(1)),
            conflicts.map { it.activity1.date },
        )
    }
}
