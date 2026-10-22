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

data class EditActivityUiState(
    /** The loaded form; null while loading. */
    val form: AddActivityUiState? = null,
    val isNotFound: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
)

/**
 * Edit form for one existing activity. The form fields reuse
 * [AddActivityUiState] so the Add screen's composable renders this too;
 * the stored id, kidId and isEnabled are preserved on save. The one-off
 * anchor loads into the form's week picker (snapped to its Monday — legacy
 * rows anchored mid-week shift to their calendar week) and can be re-picked.
 */
class EditActivityViewModel(
    private val activityId: Long,
    private val activityRepository: ActivityRepository,
    kidRepository: KidRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditActivityUiState())
    val uiState: StateFlow<EditActivityUiState> = _uiState.asStateFlow()

    val kids: StateFlow<List<Kid>> =
        kidRepository
            .getAllKidsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var original: Activity? = null

    init {
        viewModelScope.launch {
            val activity = activityRepository.getActivity(activityId)
            original = activity
            val form = activity?.let(::toForm)
            if (activity == null || form == null) {
                // Also reached for a corrupt row (unparseable time/date
                // strings, e.g. from a hand-edited backup): the form cannot
                // be filled, so it is treated like a missing activity — the
                // row itself stays listed in Kid Detail for deletion.
                _uiState.update { it.copy(isNotFound = true) }
            } else {
                _uiState.update { it.copy(form = form) }
            }
        }
    }

    fun onLocationChange(value: String) = updateForm { it.copy(location = value) }

    fun onPickupAdultChange(value: String) = updateForm { it.copy(pickupAdult = value) }

    fun onPickupNotesChange(value: String) = updateForm { it.copy(pickupNotes = value) }

    fun onNameChange(name: String) = updateForm { it.copy(name = name, nameError = name.isBlank()) }

    fun onToggleDay(day: Int) =
        updateForm {
            it.copy(
                selectedDays = if (day in it.selectedDays) it.selectedDays - day else it.selectedDays + day,
                daysError = false,
            )
        }

    fun onStartTimeChange(time: LocalTime) = updateForm { it.copy(startTime = time, timeError = false) }

    fun onEndTimeChange(time: LocalTime) = updateForm { it.copy(endTime = time, timeError = false) }

    fun onPeriodicChange(isPeriodic: Boolean) = updateForm { it.copy(isPeriodic = isPeriodic) }

    fun onFirstDateChange(date: LocalDate) = updateForm { it.copy(firstDate = AddActivityViewModel.weekStartOf(date)) }

    fun onSchoolActivityChange(isSchoolActivity: Boolean) =
        updateForm {
            // School schedules repeat every week, so the school flag locks the
            // form to periodic (one-off would hide past weekdays this week).
            it.copy(isSchoolActivity = isSchoolActivity, isPeriodic = it.isPeriodic || isSchoolActivity)
        }

    fun onExceptionChange(exception: com.arbonvata.kidstrackeractivity.data.OccurrenceException) =
        updateForm { it.copy(exceptions = it.exceptions.filterNot { entry -> entry.date == exception.date } + exception) }

    fun onExceptionRemoved(date: LocalDate) = updateForm { it.copy(exceptions = it.exceptions.filterNot { entry -> entry.date == date }) }

    fun saveActivity() {
        val form = _uiState.value.form ?: return
        val original = original ?: return
        val nameError = form.name.isBlank()
        val daysError = form.selectedDays.isEmpty()
        val timeError = form.hasInvertedTimeRange
        if (nameError || daysError || timeError) {
            _uiState.update { state ->
                state.copy(form = form.copy(nameError = nameError, daysError = daysError, timeError = timeError))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            activityRepository.updateActivity(
                Activity(
                    id = original.id,
                    calendarStartDate = original.calendarStartDate,
                    occurrenceExceptions =
                        com.arbonvata.kidstrackeractivity.data
                            .encodeExceptions(form.exceptions),
                    kidId = original.kidId,
                    name = form.name.trim(),
                    location = form.location.trim(),
                    pickupAdult = form.pickupAdult.trim(),
                    pickupNotes = form.pickupNotes.trim(),
                    startTime = form.startTime.format(AddActivityViewModel.TIME_FORMATTER),
                    endTime = form.endTime.format(AddActivityViewModel.TIME_FORMATTER),
                    isEnabled = original.isEnabled,
                    daysOfWeek = form.selectedDays.sorted().toIntArray(),
                    isPeriodic = form.isPeriodic,
                    isSchoolActivity = form.isSchoolActivity,
                    firstDate = form.firstDate.format(DateTimeFormatter.ISO_DATE),
                ),
            )
            _uiState.update { it.copy(isSaved = true, isSaving = false) }
        }
    }

    fun deleteActivity() {
        viewModelScope.launch {
            activityRepository.deleteActivity(activityId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    /** Called by the UI after it has reacted to [EditActivityUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }

    /** Called by the UI after it has reacted to [EditActivityUiState.isDeleted]. */
    fun onDeletedHandled() {
        _uiState.update { it.copy(isDeleted = false) }
    }

    private fun updateForm(transform: (AddActivityUiState) -> AddActivityUiState) {
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }
    }

    /** Null when the stored time/date strings do not parse (corrupt row). */
    private fun toForm(activity: Activity): AddActivityUiState? =
        runCatching {
            AddActivityUiState(
                selectedKidId = activity.kidId,
                name = activity.name,
                location = activity.location.orEmpty(),
                pickupAdult = activity.pickupAdult.orEmpty(),
                pickupNotes = activity.pickupNotes.orEmpty(),
                exceptions =
                    com.arbonvata.kidstrackeractivity.data
                        .decodeExceptions(activity.occurrenceExceptions),
                selectedDays = activity.daysOfWeek.toSet(),
                startTime = LocalTime.parse(activity.startTime),
                endTime = LocalTime.parse(activity.endTime),
                isPeriodic = activity.isPeriodic,
                isSchoolActivity = activity.isSchoolActivity,
                firstDate =
                    AddActivityViewModel.weekStartOf(
                        activity.firstDate
                            ?.takeIf { it.isNotBlank() }
                            ?.let(LocalDate::parse)
                            ?: LocalDate.now(),
                    ),
            )
        }.getOrNull()
}
