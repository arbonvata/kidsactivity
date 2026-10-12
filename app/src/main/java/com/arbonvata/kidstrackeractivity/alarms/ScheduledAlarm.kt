package com.arbonvata.kidstrackeractivity.alarms

import java.time.LocalDateTime

/** One scheduled reminder, computed from a single activity occurrence. */
data class ScheduledAlarm(
    val activityId: Long,
    val kidId: Long,
    val triggerAt: LocalDateTime,
    val title: String,
    val text: String,
) {
    /**
     * Stable across recomputes and unique per activity per day: recomputing
     * the same alarm reuses the id, so AlarmManager updates in place instead
     * of piling up duplicates.
     */
    val requestCode: Int =
        (activityId * 10_000L + triggerAt.toLocalDate().toEpochDay() % 10_000L).toInt()
}
