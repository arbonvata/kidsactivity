package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.LocalDateTime

/**
 * Expands the next [horizonDays] days of activities and computes one alarm
 * per occurrence: school activities remind [schoolAlarmMinutes] AFTER they
 * end (pickup; a school-day override shifts the effective end or removes the
 * day entirely), everything else [activityAlarmMinutes] BEFORE it starts.
 * Triggers already in the past are dropped. Pure function — unit tested.
 */
fun computeUpcomingAlarms(
    kids: List<Kid>,
    activities: List<Activity>,
    schoolAlarmMinutes: Int,
    activityAlarmMinutes: Int,
    now: LocalDateTime,
    horizonDays: Int = 7,
    overrides: List<DailySchedule> = emptyList(),
): List<ScheduledAlarm> {
    val occurrences =
        expandActivitiesForDateRange(
            kids,
            activities,
            now.toLocalDate(),
            now.toLocalDate().plusDays((horizonDays - 1).toLong()),
            overrides = overrides,
        )
    return occurrences
        .map { occurrence ->
            val activity = occurrence.activity
            if (activity.isSchoolActivity) {
                ScheduledAlarm(
                    activityId = activity.id,
                    kidId = activity.kidId,
                    triggerAt = occurrence.date.atTime(occurrence.endTime).plusMinutes(schoolAlarmMinutes.toLong()),
                    isSchoolActivity = true,
                    offsetMinutes = schoolAlarmMinutes,
                )
            } else {
                ScheduledAlarm(
                    activityId = activity.id,
                    kidId = activity.kidId,
                    triggerAt = occurrence.date.atTime(occurrence.startTime).minusMinutes(activityAlarmMinutes.toLong()),
                    isSchoolActivity = false,
                    offsetMinutes = activityAlarmMinutes,
                )
            }
        }.filter { it.triggerAt.isAfter(now) }
        .sortedBy { it.triggerAt }
}
