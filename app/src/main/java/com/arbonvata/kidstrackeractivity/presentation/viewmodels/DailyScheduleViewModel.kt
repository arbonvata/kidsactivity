package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class DailyScheduleUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dayOccurrences: List<ActivityOccurrence> = emptyList(),
)

/**
 * Daily view for the kids picked on the selection screen: their enabled
 * activities expanded onto the selected date. All sources are reactive
 * ObjectBox queries, so writes anywhere refresh the day; school-day
 * overrides apply (No School hides school activities, custom ends shift).
 */
class DailyScheduleViewModel(
    kidRepository: KidRepository,
    activityRepository: ActivityRepository,
    scheduleOverrideRepository: ScheduleOverrideRepository,
    selectedKidIds: Set<Long>,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DailyScheduleUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
            scheduleOverrideRepository.getAllOverridesFlow(),
            selectedDate,
        ) { kids, activities, overrides, date ->
            val filteredKids = kids.filter { it.id in selectedKidIds }
            // Muted activities stay listed (marked); only alarms skip them.
            val filteredActivities = activities.filter { it.kidId in selectedKidIds }
            DailyScheduleUiState(
                isLoading = false,
                kids = filteredKids,
                selectedDate = date,
                dayOccurrences =
                    expandActivitiesForDateRange(
                        filteredKids,
                        filteredActivities,
                        date,
                        date,
                        includeDisabled = true,
                        overrides = overrides,
                    ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyScheduleUiState())

    fun previousDay() {
        selectedDate.update { it.minusDays(1) }
    }

    fun nextDay() {
        selectedDate.update { it.plusDays(1) }
    }
}
