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
                // Corrupt time strings (a hand-edited backup, a row written
                // before import validation existed) are skipped rather than
                // crashing every screen that expands the schedule.
                val startTime = parseTimeOrNull(activity.startTime) ?: continue
                val endTime = parseTimeOrNull(effectiveEndTime(activity, override)) ?: continue
                if (kid != null) {
                    occurrences.add(
                        ActivityOccurence(
                            activity = activity,
                            kidName = kid.name,
                            date = currentDate,
                            startTime = startTime,
                            endTime = endTime,
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

/** The override's end time replaces a school activity's stored one. */
private fun effectiveEndTime(
    activity: Activity,
    override: DailySchedule?,
): String = override?.endTime?.takeIf { it.isNotEmpty() && activity.isSchoolActivity } ?: activity.endTime

/** Null when the stored string does not parse — see the skip in the loop above. */
private fun parseTimeOrNull(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

/**
 * Periodic activities recur on their weekdays indefinitely; one-offs are
 * limited to the 7-day window starting at firstDate, so each selected weekday
 * occurs exactly once in the creation week. School activities are inherently
 * weekly, so they always recur regardless of the stored flag — without this,
 * a school activity saved as a one-off anchored mid-week would hide its
 * already-past weekdays of the current week. A null/blank firstDate is treated
 * as unbounded: rows written before the column existed read null from the
 * database and keep their old repeating behaviour.
 */
private fun isWithinActivePeriod(
    activity: Activity,
    date: LocalDate,
): Boolean {
    if (activity.isPeriodic || activity.isSchoolActivity) return true
    val anchorString = activity.firstDate?.takeIf { it.isNotEmpty() } ?: return true
    // A corrupt anchor hides the one-off (its week is unknowable) instead of
    // crashing; blank is the legacy unbounded case.
    val anchor = runCatching { LocalDate.parse(anchorString) }.getOrNull() ?: return false
    return !date.isBefore(anchor) && date.isBefore(anchor.plusDays(7))
}
