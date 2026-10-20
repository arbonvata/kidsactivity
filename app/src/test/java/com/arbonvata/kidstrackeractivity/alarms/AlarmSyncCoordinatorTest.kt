package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests the ON_RESUME recovery of [AlarmSyncCoordinator.onAppResumed]: every
 * sync that ran without the exact-alarm permission cancelled the queued
 * alarms, and the grant itself (made in system Settings, while this process
 * stays alive) is invisible to the reactive combine — the resume hook is
 * what brings reminders back.
 */
class AlarmSyncCoordinatorTest {
    private val kidRepository = mockk<KidRepository>()
    private val activityRepository = mockk<ActivityRepository>()
    private val scheduleOverrideRepository = mockk<ScheduleOverrideRepository>()
    private val alarmSettingsRepository = mockk<AlarmSettingsRepository>()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)

    @BeforeEach
    fun setUpResyncSources() {
        coEvery { kidRepository.getAllKids() } returns listOf(Kid(id = 1L, name = "Leo"))
        coEvery { activityRepository.getAllActivities() } returns emptyList()
        coEvery { scheduleOverrideRepository.getAll() } returns emptyList()
        every { alarmSettingsRepository.schoolAlarmMinutes } returns flowOf(10)
        every { alarmSettingsRepository.activityAlarmMinutes } returns flowOf(15)
    }

    private fun coordinator() =
        AlarmSyncCoordinator(
            kidRepository,
            activityRepository,
            scheduleOverrideRepository,
            alarmSettingsRepository,
            alarmScheduler,
            TestScope(),
        )

    @Test
    fun resumeAfterThePermissionReturnedReschedulesAlarms() =
        runTest {
            coEvery { alarmScheduler.missedExactAlarmPermissionOnLastSync() } returns true
            every { alarmScheduler.canScheduleExactAlarms() } returns true

            coordinator().onAppResumed()

            coVerify(exactly = 1) { alarmScheduler.syncAlarms(any()) }
            coVerify { kidRepository.getAllKids() }
        }

    @Test
    fun resumeWhileThePermissionIsStillMissingDoesNotResync() =
        runTest {
            coEvery { alarmScheduler.missedExactAlarmPermissionOnLastSync() } returns true
            every { alarmScheduler.canScheduleExactAlarms() } returns false

            coordinator().onAppResumed()

            coVerify(exactly = 0) { alarmScheduler.syncAlarms(any()) }
        }

    @Test
    fun resumeWithoutAPriorDeniedSyncDoesNotResync() =
        runTest {
            coEvery { alarmScheduler.missedExactAlarmPermissionOnLastSync() } returns false
            every { alarmScheduler.canScheduleExactAlarms() } returns true

            coordinator().onAppResumed()

            coVerify(exactly = 0) { alarmScheduler.syncAlarms(any()) }
        }
}
