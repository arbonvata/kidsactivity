package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val todayOccurrences: List<ActivityOccurence> = emptyList(),
)

/**
 * Today tab: all kids' activities expanded onto today's date. Both sources
 * are reactive ObjectBox queries, so adding, editing or deleting activities
 * anywhere updates the list automatically.
 */
class TodayViewModel(
    kidRepository: KidRepository,
    activityRepository: ActivityRepository,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
        ) { kids, activities ->
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
                    ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())
}
