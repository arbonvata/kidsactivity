package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.LocalDateTime

/** Computes primary and optional pickup reminders; muted, cancelled and delegated dates stay silent. */
fun computeUpcomingAlarms(
    kids: List<Kid>,
    activities: List<Activity>,
    schoolAlarmMinutes: Int,
    activityAlarmMinutes: Int,
    now: LocalDateTime,
    horizonDays: Int = 7,
    overrides: List<DailySchedule> = emptyList(),
): List<ScheduledAlarm> {
    require(horizonDays > 0)
    val windowEnd = now.toLocalDate().plusDays(horizonDays.toLong()).atStartOfDay()
    // Include neighboring occurrences because their offsets can cross midnight.
    val occurrences =
        expandActivitiesForDateRange(
            kids,
            activities,
            now.toLocalDate().minusDays(1),
            windowEnd.toLocalDate(),
            overrides = overrides,
        )
    return occurrences
        .filter { it.endTime.isAfter(it.startTime) && it.delegatedTo == null }
        .flatMap { occurrence ->
            val activity = occurrence.activity
            val minutes =
                if (activity.hasCustomReminder) {
                    activity.reminderMinutes.coerceIn(0, 120)
                } else if (activity.isSchoolActivity) {
                    schoolAlarmMinutes
                } else {
                    activityAlarmMinutes
                }
            val primaryTime =
                if (activity.isSchoolActivity) {
                    occurrence.date.atTime(occurrence.endTime).plusMinutes(minutes.toLong())
                } else {
                    occurrence.date.atTime(occurrence.startTime).minusMinutes(minutes.toLong())
                }
            val primary = ScheduledAlarm(activity.id, activity.kidId, primaryTime, activity.isSchoolActivity, minutes)
            val pickup =
                if (!activity.isSchoolActivity && activity.hasPickupReminder) {
                    val lead = activity.pickupLeadMinutes.coerceIn(0, 60)
                    ScheduledAlarm(
                        activity.id,
                        activity.kidId,
                        occurrence.date.atTime(occurrence.endTime).minusMinutes(lead.toLong()),
                        isSchoolActivity = false,
                        offsetMinutes = lead,
                        isPickupReminder = true,
                    )
                } else {
                    null
                }
            listOfNotNull(primary, pickup)
        }.filter { it.triggerAt.isAfter(now) && it.triggerAt.isBefore(windowEnd) }
        .sortedBy { it.triggerAt }
}
