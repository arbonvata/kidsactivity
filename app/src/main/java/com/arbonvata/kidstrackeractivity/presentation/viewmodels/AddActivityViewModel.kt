package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AddActivityUiState(
    val name: String = "",
    val selectedKidId: Long = 0L,
    val selectedDays: Set<Int> = emptySet(),
    val startTime: LocalTime = LocalTime.of(17, 0),
    val endTime: LocalTime = LocalTime.of(18, 30),
    val isPeriodic: Boolean = false,
    val nameError: Boolean = false,
    val daysError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/**
 * Form state and validation for the "Add Activity" screen. When opened with
 * a kidId the activity is scoped to that kid; with kidId 0 (e.g. from the
 * Today tab) the form shows a kid picker and saving stays disabled until a
 * kid is selected. The form opens with today's weekday preselected, matching
 * the "add an activity for today" expectation. The insert is a reactive-query
 * write, so any screen observing that kid's activities updates automatically.
 */
class AddActivityViewModel(
    kidId: Long,
    private val activityRepository: ActivityRepository,
    kidRepository: KidRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            AddActivityUiState(
                selectedKidId = kidId,
                selectedDays = setOf(LocalDate.now().dayOfWeek.value),
            ),
        )
    val uiState: StateFlow<AddActivityUiState> = _uiState.asStateFlow()

    val kids: StateFlow<List<Kid>> =
        kidRepository
            .getAllKidsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onKidSelected(kidId: Long) {
        _uiState.update { it.copy(selectedKidId = kidId) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = !isValidName(name)) }
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

    fun onPeriodicChange(isPeriodic: Boolean) {
        _uiState.update { it.copy(isPeriodic = isPeriodic) }
    }

    private fun isValidName(name: String) = name.isNotBlank()

    fun saveActivity() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val nameError = !isValidName(state.name)
        val daysError = state.selectedDays.isEmpty()
        if (nameError || daysError || state.selectedKidId == 0L) {
            _uiState.update { it.copy(nameError = nameError, daysError = daysError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            activityRepository.addActivity(
                Activity(
                    kidId = state.selectedKidId,
                    name = trimmedName,
                    startTime = state.startTime.format(TIME_FORMATTER),
                    endTime = state.endTime.format(TIME_FORMATTER),
                    isEnabled = true,
                    daysOfWeek = state.selectedDays.sorted().toIntArray(),
                    isPeriodic = state.isPeriodic,
                    firstDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
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
