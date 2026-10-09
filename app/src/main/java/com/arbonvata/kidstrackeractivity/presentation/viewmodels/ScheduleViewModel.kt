package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.BackupRepository
import com.arbonvata.kidstrackeractivity.data.repository.Conflict
import com.arbonvata.kidstrackeractivity.data.repository.OccurrenceRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/**
 * Activities, occurrences, overrides and backup. Kid CRUD lives in
 * [KidsViewModel] and [AddKidViewModel] instead of being bundled here.
 */
class ScheduleViewModel : ViewModel {
    private val activityRepository: ActivityRepository?
    private val occurrenceRepository: OccurrenceRepository?
    private val scheduleOverrideRepository: ScheduleOverrideRepository?
    private val backupRepository: BackupRepository?

    constructor(
        activityRepository: ActivityRepository,
        occurrenceRepository: OccurrenceRepository,
        scheduleOverrideRepository: ScheduleOverrideRepository,
        backupRepository: BackupRepository,
    ) : super() {
        this.activityRepository = activityRepository
        this.occurrenceRepository = occurrenceRepository
        this.scheduleOverrideRepository = scheduleOverrideRepository
        this.backupRepository = backupRepository
    }

    constructor() : super() {
        this.activityRepository = null
        this.occurrenceRepository = null
        this.scheduleOverrideRepository = null
        this.backupRepository = null
    }

    private val refreshTrigger = MutableStateFlow(0)

    val activities: StateFlow<List<Activity>> =
        refreshTrigger
            .map { activityRepository?.getAllActivities() ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        refreshTrigger.value++
    }

    fun addActivity(activity: Activity): Activity? {
        val result = activityRepository?.addActivity(activity)
        refresh()
        return result
    }

    fun updateActivity(activity: Activity) {
        activityRepository?.updateActivity(activity)
        refresh()
    }

    fun deleteActivity(activityId: Long) {
        activityRepository?.deleteActivity(activityId)
        refresh()
    }

    fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    ) {
        activityRepository?.setActivityEnabled(activityId, enabled)
        refresh()
    }

    fun deleteAllActivitiesForKid(kidId: Long) {
        activityRepository?.deleteAllActivitiesForKid(kidId)
        refresh()
    }

    fun deleteAllActivitiesForKid(kidId: Int) {
        deleteAllActivitiesForKid(kidId.toLong())
    }

    fun setOverride(
        kidId: Long,
        date: LocalDate,
        endTime: String?,
        isNoSchool: Boolean,
    ) {
        scheduleOverrideRepository?.setOverride(kidId, date, endTime, isNoSchool)
        refresh()
    }

    fun getOccurrencesForDate(date: LocalDate): List<ActivityOccurence> = occurrenceRepository?.getOccurrencesForDate(date) ?: emptyList()

    fun getOccurrencesForWeek(weekStart: LocalDate): List<ActivityOccurence> =
        occurrenceRepository?.getOccurrencesForWeek(weekStart) ?: emptyList()

    fun getOccurrencesForMonth(month: YearMonth): List<ActivityOccurence> =
        occurrenceRepository?.getOccurrencesForMonth(month) ?: emptyList()

    fun findConflicts(
        daysAhead: Int = 30,
        from: LocalDate = LocalDate.now(),
    ): List<Conflict> = occurrenceRepository?.findConflicts(daysAhead, from) ?: emptyList()

    fun exportBackup(): String = backupRepository?.exportAll() ?: "{}"

    fun importBackup(
        json: String,
        replaceExisting: Boolean,
    ) {
        backupRepository?.importAll(json, replaceExisting)
        refresh()
    }
}
