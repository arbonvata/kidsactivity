package com.arbonvata.kidstrackeractivity.alarms

import android.util.Log
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

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

    private fun coordinator(scope: CoroutineScope = TestScope()) =
        AlarmSyncCoordinator(
            kidRepository,
            activityRepository,
            scheduleOverrideRepository,
            alarmSettingsRepository,
            alarmScheduler,
            scope,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun liveSyncRetriesReadFailureAndKeepsCollectingAfterScheduleFailure() =
        runTest {
            val activityMinutes = MutableStateFlow(15)
            var reads = 0
            every { kidRepository.getAllKidsFlow() } returns
                flow {
                    if (reads++ == 0) throw IOException("Settings temporarily unavailable")
                    emit(emptyList())
                }
            every { activityRepository.getAllActivitiesFlow() } returns flowOf(emptyList())
            every { scheduleOverrideRepository.getAllOverridesFlow() } returns flowOf(emptyList())
            every { alarmSettingsRepository.activityAlarmMinutes } returns activityMinutes
            coEvery { alarmScheduler.syncAlarms(any()) } throws SecurityException("Exact alarm permission revoked") andThen Unit

            mockkStatic(Log::class) {
                every { Log.e(any(), any(), any()) } returns 0
                coordinator(backgroundScope).start()
                runCurrent()
                coVerify(exactly = 0) { alarmScheduler.syncAlarms(any()) }

                advanceTimeBy(1_000)
                runCurrent()
                coVerify(exactly = 1) { alarmScheduler.syncAlarms(any()) }

                activityMinutes.value = 20
                runCurrent()
                coVerify(exactly = 2) { alarmScheduler.syncAlarms(any()) }
            }
        }

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
