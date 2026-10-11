package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.LocalDate
import java.time.LocalTime

// Represents a single, concrete instance of an activity
data class ActivityOccurence(
    val activity: Activity,
    val kidName: String,
    val date: LocalDate,
    val startTime: LocalTime,
)

// Expands recurring activities into concrete dates between startDate and endDate
fun expandActivitiesForDateRange(
    kids: List<Kid>,
    activities: List<Activity>,
    startDate: LocalDate,
    endDate: LocalDate,
): List<ActivityOccurence> {
    val kidMap = kids.associateBy { it.id }
    val occurrences = mutableListOf<ActivityOccurence>()

    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        // java.time DayOfWeek: 1 = Monday, 7 = Sunday
        val dayOfWeekValue = currentDate.dayOfWeek.value

        for (activity in activities) {
            val matchesDay = activity.daysOfWeek.contains(dayOfWeekValue)
            if (activity.isEnabled && matchesDay && isWithinActivePeriod(activity, currentDate)) {
                val kid = kidMap[activity.kidId]
                if (kid != null) {
                    occurrences.add(
                        ActivityOccurence(
                            activity = activity,
                            kidName = kid.name,
                            date = currentDate,
                            startTime = LocalTime.parse(activity.startTime),
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
