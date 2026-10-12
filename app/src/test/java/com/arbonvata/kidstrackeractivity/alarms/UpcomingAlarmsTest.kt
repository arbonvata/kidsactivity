package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Tests the alarm computation rules. Fixed October 2026 dates: Oct 19 and 26
 * are Mondays, Oct 24 is a Saturday.
 */
class UpcomingAlarmsTest {
    private val kids = listOf(Kid(id = 1L, name = "Leo"))

    private fun schoolActivity() =
        Activity(
            id = 1L,
            kidId = 1L,
            name = "School",
            startTime = "08:00",
            endTime = "14:00",
            daysOfWeek = intArrayOf(java.time.DayOfWeek.MONDAY.value),
            isSchoolActivity = true,
        )

    private fun footballActivity() =
        Activity(
            id = 2L,
            kidId = 1L,
            name = "Football",
            startTime = "17:00",
            endTime = "18:30",
            daysOfWeek = intArrayOf(java.time.DayOfWeek.MONDAY.value),
        )

    @Test
    fun schoolActivityRemindsAfterItsEnd() {
        val now = LocalDateTime.of(2026, 10, 19, 7, 0)

        val alarms = computeUpcomingAlarms(kids, listOf(schoolActivity()), schoolAlarmMinutes = 10, activityAlarmMinutes = 15, now = now)

        assertEquals(listOf(LocalDateTime.of(2026, 10, 19, 14, 10)), alarms.map { it.triggerAt })
        assertEquals("School pickup", alarms.single().title)
        assertEquals("Leo: School ended 10 min ago", alarms.single().text)
    }

    @Test
    fun normalActivityRemindsBeforeItsStart() {
        val now = LocalDateTime.of(2026, 10, 19, 7, 0)

        val alarms = computeUpcomingAlarms(kids, listOf(footballActivity()), schoolAlarmMinutes = 10, activityAlarmMinutes = 15, now = now)

        assertEquals(listOf(LocalDateTime.of(2026, 10, 19, 16, 45)), alarms.map { it.triggerAt })
        assertEquals("Activity reminder", alarms.single().title)
        assertEquals("Leo: Football starts in 15 min", alarms.single().text)
    }

    @Test
    fun customSettingsValuesAreHonored() {
        val now = LocalDateTime.of(2026, 10, 19, 7, 0)

        val alarms =
            computeUpcomingAlarms(
                kids,
                listOf(schoolActivity(), footballActivity()),
                schoolAlarmMinutes = 20,
                activityAlarmMinutes = 30,
                now = now,
            )

        assertEquals(
            listOf(LocalDateTime.of(2026, 10, 19, 14, 20), LocalDateTime.of(2026, 10, 19, 16, 30)),
            alarms.map { it.triggerAt },
        )
    }

    @Test
    fun triggersAlreadyInThePastAreDropped() {
        // 16:50 is past both the school trigger (14:10) and the football
        // trigger (16:45) of Monday Oct 19; next Monday (Oct 26) is already
        // beyond the 7-day horizon (Oct 19..25), so nothing remains.
        val now = LocalDateTime.of(2026, 10, 19, 16, 50)

        val alarms =
            computeUpcomingAlarms(
                kids,
                listOf(schoolActivity(), footballActivity()),
                schoolAlarmMinutes = 10,
                activityAlarmMinutes = 15,
                now = now,
            )

        assertTrue(alarms.isEmpty())
    }

    @Test
    fun horizonCoversSevenDaysOnly() {
        // Saturday Oct 24: horizon is Oct 24..30, so only Monday Oct 26 is
        // included — not Nov 2.
        val now = LocalDateTime.of(2026, 10, 24, 12, 0)

        val alarms = computeUpcomingAlarms(kids, listOf(footballActivity()), schoolAlarmMinutes = 10, activityAlarmMinutes = 15, now = now)

        assertEquals(listOf(LocalDateTime.of(2026, 10, 26, 16, 45)), alarms.map { it.triggerAt })
    }

    @Test
    fun disabledActivitiesProduceNoAlarms() {
        val now = LocalDateTime.of(2026, 10, 19, 7, 0)

        val alarms =
            computeUpcomingAlarms(
                kids,
                listOf(footballActivity().copy(isEnabled = false)),
                schoolAlarmMinutes = 10,
                activityAlarmMinutes = 15,
                now = now,
            )

        assertTrue(alarms.isEmpty())
    }

    @Test
    fun requestCodesAreStablePerAlarmAndUniquePerActivityAndDay() {
        val monday =
            ScheduledAlarm(activityId = 5L, kidId = 1L, triggerAt = LocalDateTime.of(2026, 10, 19, 14, 10), title = "t", text = "x")
        val nextMonday = monday.copy(triggerAt = LocalDateTime.of(2026, 10, 26, 14, 10))
        val otherActivity = monday.copy(activityId = 6L)

        assertEquals(monday.requestCode, monday.copy(title = "changed").requestCode)
        assertEquals(3, setOf(monday.requestCode, nextMonday.requestCode, otherActivity.requestCode).size)
    }
}
