package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val todayOccurrences: List<ActivityOccurence> = emptyList(),
)

/**
 * Today tab: all kids' activities expanded onto today's date. All sources
 * are reactive ObjectBox queries, so adding, editing or deleting kids,
 * activities or school-day overrides anywhere updates the list
 * automatically.
 */
class TodayViewModel(
    kidRepository: KidRepository,
    activityRepository: ActivityRepository,
    scheduleOverrideRepository: ScheduleOverrideRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
            scheduleOverrideRepository.getAllOverridesFlow(),
        ) { kids, activities, overrides ->
            TodayUiState(
                isLoading = false,
                kids = kids,
                todayOccurrences =
                    expandActivitiesForDateRange(
                        kids,
                        activities,
                        LocalDate.now(),
                        LocalDate.now(),
                        // Muted activities stay listed (marked); only alarms skip them.
                        includeDisabled = true,
                        overrides = overrides,
                    ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    /**
     * The one-time reminder permission prompt: once dismissed or confirmed it
     * must never return, so the fact that it was shown is persisted.
     */
    val reminderPromptShown: StateFlow<Boolean> =
        alarmSettingsRepository.reminderPromptShown
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onReminderPromptDismissed() {
        viewModelScope.launch { alarmSettingsRepository.setReminderPromptShown() }
    }
}
