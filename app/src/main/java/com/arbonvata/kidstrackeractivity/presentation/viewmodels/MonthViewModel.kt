package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
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
import java.time.YearMonth

data class MonthUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val monthOccurrences: List<ActivityOccurence> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
)

/**
 * Month tab: all kids' activities expanded onto every day of the visible
 * month. All sources are reactive ObjectBox queries, so writes anywhere
 * refresh the agenda automatically; moving to another month re-expands the
 * same flows for the new range. School-day overrides apply.
 */
class MonthViewModel(
    kidRepository: KidRepository,
    activityRepository: ActivityRepository,
    scheduleOverrideRepository: ScheduleOverrideRepository,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<MonthUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
            scheduleOverrideRepository.getAllOverridesFlow(),
            selectedMonth,
        ) { kids, activities, overrides, month ->
            MonthUiState(
                isLoading = false,
                kids = kids,
                monthOccurrences =
                    expandActivitiesForDateRange(
                        kids,
                        activities,
                        month.atDay(1),
                        month.atEndOfMonth(),
                        // Muted activities stay listed (marked); only alarms skip them.
                        includeDisabled = true,
                        overrides = overrides,
                    ),
                selectedMonth = month,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthUiState())

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }
}
