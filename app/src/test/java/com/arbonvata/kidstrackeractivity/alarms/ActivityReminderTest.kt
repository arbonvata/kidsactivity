package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.OccurrenceException
import com.arbonvata.kidstrackeractivity.data.encodeExceptions
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ActivityReminderTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val kids = listOf(Kid(id = 1))
    private val activity = Activity(id = 1, kidId = 1, startTime = "17:00", endTime = "18:30", daysOfWeek = intArrayOf(1))

    @Test
    fun `custom timing overrides global settings and legacy rows keep global timing`() {
        val custom = activity.copy(hasCustomReminder = true, reminderMinutes = 30)
        assertEquals(day.atTime(16, 30), computeUpcomingAlarms(kids, listOf(custom), 10, 15, day.atStartOfDay()).single().triggerAt)
        val legacy = activity.copy(hasCustomReminder = false, reminderMinutes = 0)
        assertEquals(day.atTime(16, 45), computeUpcomingAlarms(kids, listOf(legacy), 10, 15, day.atStartOfDay()).single().triggerAt)
        val school = custom.copy(isSchoolActivity = true, reminderMinutes = 20, hasPickupReminder = true)
        assertEquals(day.atTime(18, 50), computeUpcomingAlarms(kids, listOf(school), 10, 15, day.atStartOfDay()).single().triggerAt)
    }

    @Test
    fun `pickup and primary reminders have different identities even at the same time`() {
        val source =
            activity.copy(
                endTime = "17:30",
                hasCustomReminder = true,
                reminderMinutes = 0,
                hasPickupReminder = true,
                pickupLeadMinutes = 30,
            )
        val alarms = computeUpcomingAlarms(kids, listOf(source), 10, 15, day.atStartOfDay())
        assertEquals(2, alarms.size)
        assertEquals(1, alarms.map { it.triggerAt }.distinct().size)
        assertEquals(2, alarms.map { it.requestCode }.distinct().size)
        assertEquals(1, alarms.count { it.isPickupReminder })
    }

    @Test
    fun `muting cancelling and delegation silence both reminders`() {
        val source = activity.copy(hasPickupReminder = true)
        for (silent in listOf(
            source.copy(isEnabled = false),
            source.copy(occurrenceExceptions = encodeExceptions(listOf(OccurrenceException(day)))),
            source.copy(occurrenceExceptions = encodeExceptions(listOf(OccurrenceException(day, delegatedTo = "Alex")))),
        )) {
            assertTrue(computeUpcomingAlarms(kids, listOf(silent), 10, 15, day.atStartOfDay(), horizonDays = 1).isEmpty())
        }
    }

    @Test
    fun `offsets crossing midnight remain inside the reminder window`() {
        val early = activity.copy(startTime = "00:30", endTime = "01:30", hasCustomReminder = true, reminderMinutes = 120)
        assertEquals(
            day.minusDays(1).atTime(22, 30),
            computeUpcomingAlarms(
                kids,
                listOf(early),
                10,
                15,
                day.minusDays(1).atTime(22, 0),
                horizonDays = 1,
            ).single().triggerAt,
        )
        val lateSchool =
            activity.copy(
                startTime = "08:00",
                endTime = "23:50",
                daysOfWeek = intArrayOf(7),
                isSchoolActivity = true,
                hasCustomReminder = true,
                reminderMinutes = 30,
                lastDate = day.minusDays(1).toString(),
            )
        assertEquals(
            day.atTime(0, 20),
            computeUpcomingAlarms(kids, listOf(lateSchool), 10, 15, day.atStartOfDay(), horizonDays = 1).single().triggerAt,
        )
    }
}
