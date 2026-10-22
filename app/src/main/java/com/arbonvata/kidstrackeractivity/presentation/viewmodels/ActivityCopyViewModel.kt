package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleCopyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ActivityCopyUiState(
    val kids: List<Kid> = emptyList(),
    val selectedKidId: Long = 0,
    val name: String = "",
    val week: LocalDate = AddActivityViewModel.weekStartOf(LocalDate.now()),
    val sourceCount: Int = 0,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: Boolean = false,
    val copiedCount: Int? = null,
)

class ActivityCopyViewModel(
    private val sourceKidId: Long,
    private val activityId: Long,
    private val copyRepository: ScheduleCopyRepository,
    activityRepository: ActivityRepository,
    kidRepository: KidRepository,
) : ViewModel() {
    private val state = MutableStateFlow(ActivityCopyUiState(selectedKidId = if (activityId == 0L) 0 else sourceKidId))
    val uiState =
        combine(state, kidRepository.getAllKidsFlow()) { current, kids ->
            current.copy(kids = kids.filter { activityId != 0L || it.id != sourceKidId })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), state.value)

    init {
        viewModelScope.launch {
            try {
                val source =
                    if (activityId == 0L) {
                        activityRepository.getAllActivities().filter { it.kidId == sourceKidId }
                    } else {
                        listOfNotNull(activityRepository.getActivity(activityId))
                    }
                state.update { it.copy(isLoading = false, sourceCount = source.size, name = source.firstOrNull()?.name.orEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    fun selectKid(id: Long) {
        state.update { it.copy(selectedKidId = id) }
    }

    fun changeName(name: String) {
        state.update { it.copy(name = name) }
    }

    fun changeWeek(date: LocalDate) {
        state.update { it.copy(week = AddActivityViewModel.weekStartOf(date)) }
    }

    fun resetResult() {
        state.update { it.copy(error = false, copiedCount = null) }
    }

    fun copy() {
        val current = state.value
        if (current.isLoading ||
            current.isWorking ||
            current.copiedCount != null ||
            current.sourceCount == 0 ||
            current.selectedKidId == 0L ||
            (activityId != 0L && current.name.isBlank())
        ) {
            return
        }
        state.update { it.copy(isWorking = true, error = false) }
        viewModelScope.launch {
            try {
                val count =
                    copyRepository.copy(
                        sourceKidId,
                        current.selectedKidId,
                        activityId.takeIf { it != 0L },
                        current.week,
                        current.name,
                    )
                state.update { it.copy(isWorking = false, copiedCount = count) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(isWorking = false, error = true) }
            }
        }
    }
}
