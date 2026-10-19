package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TodayUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val todayOccurrences: List<ActivityOccurrence> = emptyList(),
    /** Refreshed by a one-minute ticker so the screen follows midnight rollovers. */
    val today: LocalDate = LocalDate.now(),
    val now: LocalTime = LocalTime.now(),
)

/**
 * Today tab: all kids' activities expanded onto today's date. All sources
 * are reactive ObjectBox queries, so adding, editing or deleting kids,
 * activities or school-day overrides anywhere updates the list
 * automatically. A one-minute ticker re-emits even without data changes, so
 * the tab keeps up across midnight and the "up next" card stays fresh while
 * the screen stays open.
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
            nowTicker(),
        ) { kids, activities, overrides, _ ->
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
                today = LocalDate.now(),
                now = LocalTime.now(),
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

    /** Emits immediately, then every minute — see the class KDoc. */
    private fun nowTicker() =
        flow {
            while (true) {
                emit(Unit)
                delay(60_000)
            }
        }
}
