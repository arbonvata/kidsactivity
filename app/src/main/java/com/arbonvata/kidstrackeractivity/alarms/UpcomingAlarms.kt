package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Expands the next [horizonDays] days of activities and computes one alarm
 * per occurrence: school activities remind [schoolAlarmMinutes] AFTER they
 * end (pickup), everything else [activityAlarmMinutes] BEFORE it starts.
 * Triggers already in the past are dropped. Pure function — unit tested.
 */
fun computeUpcomingAlarms(
    kids: List<Kid>,
    activities: List<Activity>,
    schoolAlarmMinutes: Int,
    activityAlarmMinutes: Int,
    now: LocalDateTime,
    horizonDays: Int = 7,
): List<ScheduledAlarm> {
    val kidNames = kids.associate { it.id to it.name }
    val occurrences =
        expandActivitiesForDateRange(
            kids,
            activities,
            now.toLocalDate(),
            now.toLocalDate().plusDays((horizonDays - 1).toLong()),
        )
    return occurrences
        .mapNotNull { occurrence ->
            val activity = occurrence.activity
            val kidName = kidNames[activity.kidId] ?: return@mapNotNull null
            if (activity.isSchoolActivity) {
                ScheduledAlarm(
                    activityId = activity.id,
                    kidId = activity.kidId,
                    triggerAt = occurrence.date.atTime(LocalTime.parse(activity.endTime)).plusMinutes(schoolAlarmMinutes.toLong()),
                    title = "School pickup",
                    text = "$kidName: ${activity.name} ended $schoolAlarmMinutes min ago",
                )
            } else {
                ScheduledAlarm(
                    activityId = activity.id,
                    kidId = activity.kidId,
                    triggerAt = occurrence.date.atTime(occurrence.startTime).minusMinutes(activityAlarmMinutes.toLong()),
                    title = "Activity reminder",
                    text = "$kidName: ${activity.name} starts in $activityAlarmMinutes min",
                )
            }
        }.filter { it.triggerAt.isAfter(now) }
        .sortedBy { it.triggerAt }
}
