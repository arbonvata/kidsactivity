package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid

object DebugTestData {
    val mockKids =
        listOf(
            Kid(id = 1, name = "Leo", schoolName = "Greenfield Primary"),
            Kid(id = 2, name = "Mia", schoolName = "St. Mary's"),
        )

    val mockActivities =
        listOf(
            Activity(
                id = 101,
                kidId = 1,
                name = "Soccer Practice",
                startTime = "17:00",
                endTime = "18:30",
                isEnabled = true,
                daysOfWeek = intArrayOf(1, 3, 5), // Mon, Wed, Fri
            ),
            Activity(
                id = 102,
                kidId = 2,
                name = "Piano Lesson",
                startTime = "16:00",
                endTime = "17:00",
                isEnabled = true,
                daysOfWeek = intArrayOf(2, 4), // Tue, Thu
            ),
            // Muted fixture: previews render it dimmed with the
            // notifications-off marker.
            Activity(
                id = 103,
                kidId = 1,
                name = "Swimming",
                startTime = "15:00",
                endTime = "16:00",
                isEnabled = false,
                daysOfWeek = intArrayOf(2, 4), // Tue, Thu
            ),
        )

    // Sample navigation route arguments for debugging
    val sampleKidIds = "1,2"
    val sampleActivityId = 101L
}
