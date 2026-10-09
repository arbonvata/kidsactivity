package com.arbonvata.kidstrackeractivity.data

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid

object DebugTestData {
    val mockKids =
        listOf(
            Kid(id = 1, name = "Leo", defaultEndTime = "16:00"),
            Kid(id = 2, name = "Mia", defaultEndTime = "15:30"),
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
        )

    // Sample navigation route arguments for debugging
    val sampleKidIds = "1,2"
    val sampleActivityId = 101L
}
