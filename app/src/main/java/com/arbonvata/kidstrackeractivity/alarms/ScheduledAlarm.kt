package com.arbonvata.kidstrackeractivity.alarms

import java.time.LocalDateTime

/**
 * One scheduled reminder, computed from a single activity occurrence. Only
 * ids and the reminder kind are carried — the notification text is built
 * from the database at fire time (see AlarmReceiver), so it follows the
 * locale current when the alarm fires and picks up renames made after
 * scheduling.
 */
data class ScheduledAlarm(
    val activityId: Long,
    val kidId: Long,
    val triggerAt: LocalDateTime,
    /** School activities remind AFTER their end (pickup), others before start. */
    val isSchoolActivity: Boolean,
    /** The configured offset in minutes (after end / before start). */
    val offsetMinutes: Int,
) {
    /**
     * Stable across recomputes and unique per activity per day: recomputing
     * the same alarm reuses the id, so AlarmManager updates in place instead
     * of piling up duplicates.
     */
    val requestCode: Int =
        (activityId * 10_000L + triggerAt.toLocalDate().toEpochDay() % 10_000L).toInt()
}
