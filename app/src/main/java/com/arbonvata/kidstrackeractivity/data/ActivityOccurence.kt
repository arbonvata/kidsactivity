package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.LocalDate
import java.time.LocalTime

// Represents a single, concrete instance of an activity
data class ActivityOccurence(
    val activity: Activity,
    val kidName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    /** Effective end: a school-day override replaces the school activity's end. */
    val endTime: LocalTime,
)

/**
 * Expands recurring activities into concrete dates between startDate and
 * endDate. Disabled activities are skipped unless [includeDisabled] is set —
 * the schedule views set it so muted activities stay visible (marked), while
 * alarm computation and conflict detection keep the default.
 *
 * School-day [overrides] apply to activities flagged isSchoolActivity: a
 * "No School" day hides them entirely, a custom end time replaces their end
 * (which shifts the pickup alarm and the displayed end time). Other
 * activities are unaffected by overrides.
 */
fun expandActivitiesForDateRange(
    kids: List<Kid>,
    activities: List<Activity>,
    startDate: LocalDate,
    endDate: LocalDate,
    includeDisabled: Boolean = false,
    overrides: List<DailySchedule> = emptyList(),
): List<ActivityOccurence> {
    val kidMap = kids.associateBy { it.id }
    val overrideMap = overrides.associateBy { "${it.kidId}|${it.date}" }
    val occurrences = mutableListOf<ActivityOccurence>()

    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        // java.time DayOfWeek: 1 = Monday, 7 = Sunday
        val dayOfWeekValue = currentDate.dayOfWeek.value

        for (activity in activities) {
            val override = overrideMap["${activity.kidId}|$currentDate"]
            if (activity.isSchoolActivity && override?.isNoSchool == true) continue

            val matchesDay = activity.daysOfWeek.contains(dayOfWeekValue)
            val active = activity.isEnabled || includeDisabled
            if (active && matchesDay && isWithinActivePeriod(activity, currentDate)) {
                val kid = kidMap[activity.kidId]
                if (kid != null) {
                    occurrences.add(
                        ActivityOccurence(
                            activity = activity,
                            kidName = kid.name,
                            date = currentDate,
                            startTime = LocalTime.parse(activity.startTime),
                            endTime = effectiveEndTime(activity, override),
                        ),
                    )
                }
            }
        }
        currentDate = currentDate.plusDays(1)
    }

    // Sort by Date, then by Time
    return occurrences.sortedWith(compareBy({ it.date }, { it.startTime }))
}

private fun effectiveEndTime(
    activity: Activity,
    override: DailySchedule?,
): LocalTime {
    val overriddenEnd =
        override
            ?.endTime
            ?.takeIf { it.isNotEmpty() && activity.isSchoolActivity }
            ?.let(LocalTime::parse)
    return overriddenEnd ?: LocalTime.parse(activity.endTime)
}

/**
 * Periodic activities recur on their weekdays indefinitely; one-offs are
 * limited to the 7-day window starting at firstDate, so each selected weekday
 * occurs exactly once in the creation week. A null/blank firstDate is treated
 * as unbounded: rows written before the column existed read null from the
 * database and keep their old repeating behaviour.
 */
private fun isWithinActivePeriod(
    activity: Activity,
    date: LocalDate,
): Boolean {
    if (activity.isPeriodic) return true
    val anchor = activity.firstDate?.takeIf { it.isNotEmpty() }?.let(LocalDate::parse) ?: return true
    return !date.isBefore(anchor) && date.isBefore(anchor.plusDays(7))
}
