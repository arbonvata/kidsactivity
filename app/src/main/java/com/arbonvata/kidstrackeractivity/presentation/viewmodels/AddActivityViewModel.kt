package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AddActivityUiState(
    val name: String = "",
    val selectedDays: Set<Int> = emptySet(),
    val startTime: LocalTime = LocalTime.of(17, 0),
    val endTime: LocalTime = LocalTime.of(18, 30),
    val nameError: Boolean = false,
    val daysError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/**
 * Form state and validation for the "Add Activity" screen, scoped to one
 * kid (kidId 0 means no kid is selected yet and saving is disabled). The
 * insert is a reactive-query write, so any screen observing that kid's
 * activities updates automatically.
 */
class AddActivityViewModel(
    private val kidId: Long,
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddActivityUiState())
    val uiState: StateFlow<AddActivityUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = false) }
    }

    fun onToggleDay(day: Int) {
        _uiState.update {
            it.copy(
                selectedDays = if (day in it.selectedDays) it.selectedDays - day else it.selectedDays + day,
                daysError = false,
            )
        }
    }

    fun onStartTimeChange(time: LocalTime) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun onEndTimeChange(time: LocalTime) {
        _uiState.update { it.copy(endTime = time) }
    }

    fun saveActivity() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val nameError = trimmedName.isEmpty()
        val daysError = state.selectedDays.isEmpty()
        if (nameError || daysError) {
            _uiState.update { it.copy(nameError = nameError, daysError = daysError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            activityRepository.addActivity(
                Activity(
                    kidId = kidId,
                    name = trimmedName,
                    startTime = state.startTime.format(TIME_FORMATTER),
                    endTime = state.endTime.format(TIME_FORMATTER),
                    isEnabled = true,
                    daysOfWeek = state.selectedDays.sorted().toIntArray(),
                ),
            )
            _uiState.update { it.copy(isSaved = true, isSaving = false) }
        }
    }

    /** Called by the UI after it has reacted to [AddActivityUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }

    companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
