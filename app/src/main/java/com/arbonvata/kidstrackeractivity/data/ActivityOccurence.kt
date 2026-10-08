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
            if (activity.isEnabled && activity.daysOfWeek.contains(dayOfWeekValue)) {
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
