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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeekUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val weekOccurrences: List<ActivityOccurence> = emptyList(),
    val weekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
)

/**
 * Week tab: all kids' activities expanded onto the visible week, Monday to
 * Sunday. All sources are reactive ObjectBox queries, so writes anywhere
 * refresh the week automatically; moving to another week re-expands the
 * same flows for the new range. School-day overrides apply.
 */
class WeekViewModel(
    kidRepository: KidRepository,
    activityRepository: ActivityRepository,
    scheduleOverrideRepository: ScheduleOverrideRepository,
) : ViewModel() {
    private val weekStart =
        MutableStateFlow(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))

    val uiState: StateFlow<WeekUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
            scheduleOverrideRepository.getAllOverridesFlow(),
            weekStart,
        ) { kids, activities, overrides, start ->
            WeekUiState(
                isLoading = false,
                kids = kids,
                weekOccurrences =
                    expandActivitiesForDateRange(
                        kids,
                        activities,
                        start,
                        start.plusDays(6),
                        includeDisabled = true,
                        overrides = overrides,
                    ),
                weekStart = start,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekUiState())

    fun previousWeek() {
        weekStart.update { it.minusWeeks(1) }
    }

    fun nextWeek() {
        weekStart.update { it.plusWeeks(1) }
    }
}
