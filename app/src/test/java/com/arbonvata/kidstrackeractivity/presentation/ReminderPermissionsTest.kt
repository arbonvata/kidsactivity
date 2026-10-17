package com.arbonvata.kidstrackeractivity.presentation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReminderPermissionsTest {
    @Test
    fun remindersBlockedWhenExactAlarmsAreMissing() {
        val permissions =
            ReminderPermissions(
                exactAlarmGranted = false,
                notificationsGranted = true,
                batteryOptimizationDisabled = true,
            )

        assertTrue(permissions.remindersBlocked)
    }

    @Test
    fun remindersBlockedWhenNotificationsAreMissing() {
        val permissions =
            ReminderPermissions(
                exactAlarmGranted = true,
                notificationsGranted = false,
                batteryOptimizationDisabled = true,
            )

        assertTrue(permissions.remindersBlocked)
    }

    @Test
    fun remindersBlockedWhenBothRequiredPermissionsAreMissing() {
        val permissions =
            ReminderPermissions(
                exactAlarmGranted = false,
                notificationsGranted = false,
                batteryOptimizationDisabled = true,
            )

        assertTrue(permissions.remindersBlocked)
    }

    // Pins that battery optimization is advisory only: without it alarms can
    // be delayed under Doze but still fire, so it must never raise the banner.
    @Test
    fun remindersStillFireWhenOnlyBatteryOptimizationIsMissing() {
        val permissions =
            ReminderPermissions(
                exactAlarmGranted = true,
                notificationsGranted = true,
                batteryOptimizationDisabled = false,
            )

        assertFalse(permissions.remindersBlocked)
    }
}
