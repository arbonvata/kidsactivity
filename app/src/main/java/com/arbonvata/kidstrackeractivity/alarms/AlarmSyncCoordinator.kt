package com.arbonvata.kidstrackeractivity.alarms

import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Keeps AlarmManager in sync with the database and settings: any change to
 * kids, activities, or the alarm offsets recomputes the next 7 days of
 * reminders and re-syncs. Started once from the Application; the boot
 * receiver uses [resyncOnce] because no collector is running that early.
 */
class AlarmSyncCoordinator(
    private val kidRepository: KidRepository,
    private val activityRepository: ActivityRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            combine(
                kidRepository.getAllKidsFlow(),
                activityRepository.getAllActivitiesFlow(),
                alarmSettingsRepository.schoolAlarmMinutes,
                alarmSettingsRepository.activityAlarmMinutes,
            ) { kids, activities, schoolMinutes, activityMinutes ->
                computeUpcomingAlarms(kids, activities, schoolMinutes, activityMinutes, LocalDateTime.now())
            }.collect { alarms -> alarmScheduler.syncAlarms(alarms) }
        }
    }

    suspend fun resyncOnce() {
        val kids = kidRepository.getAllKids()
        val activities = activityRepository.getAllActivities()
        val schoolMinutes = alarmSettingsRepository.schoolAlarmMinutes.first()
        val activityMinutes = alarmSettingsRepository.activityAlarmMinutes.first()
        alarmScheduler.syncAlarms(
            computeUpcomingAlarms(kids, activities, schoolMinutes, activityMinutes, LocalDateTime.now()),
        )
    }
}
