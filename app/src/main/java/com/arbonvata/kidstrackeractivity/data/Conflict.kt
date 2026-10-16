package com.arbonvata.kidstrackeractivity.data

import java.time.Duration

/** Two of the same kid's occurrences that overlap on the same date (spec 8.2). */
data class Conflict(
    val activity1: ActivityOccurence,
    val activity2: ActivityOccurence,
    val overlapMinutes: Long,
)

/**
 * Detects same-kid occurrence overlaps per date. The occurrences are already
 * expanded (weekly/one-off windows and school-day overrides applied), so a
 * No-School day simply contributes no school occurrence to overlap with, and
 * shifted school ends are respected. Within a group sorted by start time any
 * non-adjacent overlap implies an adjacent one, so comparing neighbours
 * surfaces every conflict — a three-way clash is reported as its adjacent
 * pairs rather than all three combinations. Occurrences ending at or before
 * their start (overnight ranges are not representable) are ignored, and
 * touching ranges (end == start) do not conflict.
 */
fun findConflicts(occurrences: List<ActivityOccurence>): List<Conflict> {
    val conflicts =
        occurrences
            .filter { it.endTime.isAfter(it.startTime) }
            .groupBy { it.activity.kidId to it.date }
            .flatMap { (_, sameKidAndDay) ->
                val sorted = sameKidAndDay.sortedBy { it.startTime }
                sorted
                    .zipWithNext()
                    .mapNotNull { (first, second) ->
                        // Second starts at or after first (sorted), so the
                        // overlap runs until the earlier of the two ends.
                        val overlapEnd = minOf(first.endTime, second.endTime)
                        val overlap = Duration.between(second.startTime, overlapEnd).toMinutes()
                        if (overlap > 0) Conflict(first, second, overlap) else null
                    }
            }
    return conflicts.sortedWith(compareBy({ it.activity1.date }, { it.activity1.startTime }))
}
