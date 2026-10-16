package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.Conflict
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.findConflicts
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

private const val CONFLICT_HORIZON_DAYS = 30L

data class ConflictUiState(
    val isLoading: Boolean = true,
    val conflicts: List<Conflict> = emptyList(),
)

/**
 * Conflicts screen: expands all kids' activities over the next
 * [CONFLICT_HORIZON_DAYS] days (spec 8.2) and reports same-kid overlaps via
 * [findConflicts]. All sources are reactive ObjectBox queries, so resolving
 * or editing an activity anywhere refreshes the list automatically.
 * School-day overrides apply (a No-School day hides school occurrences
 * entirely, custom ends shift them); muted activities are skipped, matching
 * alarm computation.
 */
class ConflictViewModel(
    kidRepository: KidRepository,
    activityRepository: ActivityRepository,
    scheduleOverrideRepository: ScheduleOverrideRepository,
) : ViewModel() {
    val uiState: StateFlow<ConflictUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
            scheduleOverrideRepository.getAllOverridesFlow(),
        ) { kids, activities, overrides ->
            val today = LocalDate.now()
            ConflictUiState(
                isLoading = false,
                conflicts =
                    findConflicts(
                        expandActivitiesForDateRange(
                            kids = kids,
                            activities = activities,
                            startDate = today,
                            endDate = today.plusDays(CONFLICT_HORIZON_DAYS),
                            overrides = overrides,
                        ),
                    ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConflictUiState())
}
