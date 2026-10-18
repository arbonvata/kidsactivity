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
 * shifted school ends are respected. Every overlapping pair within a kid's
 * day is reported (a three-way clash yields all three pairs), so resolving
 * conflicts one by one never leaves a hidden overlap behind. Occurrences
 * ending at or before their start (overnight ranges are not representable)
 * are ignored, and touching ranges (end == start) do not conflict.
 */
fun findConflicts(occurrences: List<ActivityOccurence>): List<Conflict> {
    val conflicts =
        occurrences
            .filter { it.endTime.isAfter(it.startTime) }
            .groupBy { it.activity.kidId to it.date }
            .flatMap { (_, sameKidAndDay) ->
                // Pairwise within the day's sorted list: a neighbour scan is
                // not enough — Football 17–19 vs Swimming 18:30–19 would stay
                // hidden behind a short activity in between.
                val sorted = sameKidAndDay.sortedBy { it.startTime }
                sorted.flatMapIndexed { index, first ->
                    sorted.drop(index + 1).mapNotNull { second ->
                        // Second starts at or after first (sorted), so the
                        // overlap runs until the earlier of the two ends.
                        val overlapEnd = minOf(first.endTime, second.endTime)
                        val overlap = Duration.between(second.startTime, overlapEnd).toMinutes()
                        if (overlap > 0) Conflict(first, second, overlap) else null
                    }
                }
            }
    return conflicts.sortedWith(compareBy({ it.activity1.date }, { it.activity1.startTime }))
}
