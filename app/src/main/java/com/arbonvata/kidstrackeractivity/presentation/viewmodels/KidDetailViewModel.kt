package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KidDetailUiState(
    val isLoading: Boolean = true,
    val kid: Kid? = null,
    val activities: List<Activity> = emptyList(),
)

/**
 * One kid's activity overview. The activity list is a reactive ObjectBox
 * query, so adding, disabling or deleting that kid's activities anywhere
 * updates the screen automatically. `kid` is null once loading finished for
 * a kid that no longer exists.
 */
class KidDetailViewModel(
    private val kidId: Long,
    private val kidRepository: KidRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    val uiState: StateFlow<KidDetailUiState> =
        combine(
            flow { emit(kidRepository.getKid(kidId)) },
            activityRepository.getActivitiesForKidFlow(kidId),
        ) { kid, activities ->
            KidDetailUiState(isLoading = false, kid = kid, activities = activities)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KidDetailUiState())

    fun deleteAllActivities() {
        viewModelScope.launch { activityRepository.deleteAllActivitiesForKid(kidId) }
    }

    /** Mutes or un-mutes one activity's reminders without deleting it. */
    fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch { activityRepository.setActivityEnabled(activityId, enabled) }
    }
}
