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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Activities, occurrences, overrides and backup. Kid CRUD lives in
 * [KidsViewModel] and [AddKidViewModel] instead of being bundled here.
 * `activities` is a reactive ObjectBox query, so writes (from this or any
 * other ViewModel) push fresh data automatically. Mutations are
 * fire-and-forget and run in viewModelScope; reads that return a value are
 * suspend and must be called from a coroutine.
 */
class ScheduleViewModel(
    private val activityRepository: ActivityRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val scheduleOverrideRepository: ScheduleOverrideRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    val activities: StateFlow<List<Activity>> =
        activityRepository
            .getAllActivitiesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addActivity(activity: Activity) {
        viewModelScope.launch { activityRepository.addActivity(activity) }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch { activityRepository.updateActivity(activity) }
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch { activityRepository.deleteActivity(activityId) }
    }

    fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch { activityRepository.setActivityEnabled(activityId, enabled) }
    }

    fun setOverride(
        kidId: Long,
        date: LocalDate,
        endTime: String?,
        isNoSchool: Boolean,
    ) {
        viewModelScope.launch {
            scheduleOverrideRepository.setOverride(kidId, date, endTime, isNoSchool)
        }
    }

    suspend fun getOccurrencesForDate(date: LocalDate): List<ActivityOccurence> = occurrenceRepository.getOccurrencesForDate(date)

    suspend fun getOccurrencesForWeek(weekStart: LocalDate): List<ActivityOccurence> = occurrenceRepository.getOccurrencesForWeek(weekStart)

    suspend fun getOccurrencesForMonth(month: YearMonth): List<ActivityOccurence> = occurrenceRepository.getOccurrencesForMonth(month)

    suspend fun findConflicts(
        daysAhead: Int = 30,
        from: LocalDate = LocalDate.now(),
    ): List<Conflict> = occurrenceRepository.findConflicts(daysAhead, from)

    suspend fun exportBackup(): String = backupRepository.exportAll()

    fun importBackup(
        json: String,
        replaceExisting: Boolean,
    ) {
        viewModelScope.launch { backupRepository.importAll(json, replaceExisting) }
    }
}
