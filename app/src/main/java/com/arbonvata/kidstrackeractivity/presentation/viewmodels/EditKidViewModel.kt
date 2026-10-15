package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditKidUiState(
    /** The loaded form; null while loading. */
    val form: AddKidUiState? = null,
    val isNotFound: Boolean = false,
)

/**
 * Edit form for one existing kid. Reuses [AddKidUiState] so the Add screen's
 * composable renders this too. The duplicate check ignores the kid being
 * edited, so keeping (or case-correcting) its own name stays valid.
 */
class EditKidViewModel(
    private val kidId: Long,
    private val kidRepository: KidRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditKidUiState())
    val uiState: StateFlow<EditKidUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val kid = kidRepository.getKid(kidId)
            if (kid == null) {
                _uiState.update { it.copy(isNotFound = true) }
            } else {
                _uiState.update {
                    it.copy(form = AddKidUiState(name = kid.name, schoolName = kid.schoolName))
                }
            }
        }
    }

    fun onNameChange(name: String) = updateForm { it.copy(name = name, nameError = null) }

    fun onSchoolNameChange(schoolName: String) = updateForm { it.copy(schoolName = schoolName, schoolNameError = false) }

    fun saveKid() {
        val form = _uiState.value.form ?: return
        val trimmedName = form.name.trim()
        val trimmedSchool = form.schoolName.trim()
        val blankName = trimmedName.isEmpty()
        val blankSchool = trimmedSchool.isEmpty()
        if (blankName || blankSchool) {
            _uiState.update { state ->
                state.copy(
                    form =
                        form.copy(
                            nameError = KidNameError.Blank.takeIf { blankName },
                            schoolNameError = blankSchool,
                        ),
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(form = form.copy(isSaving = true, nameError = null, schoolNameError = false))
        }
        viewModelScope.launch {
            val duplicate =
                kidRepository.getAllKids().firstOrNull { kid ->
                    kid.id != kidId && kid.name.equals(trimmedName, ignoreCase = true)
                }
            if (duplicate != null) {
                _uiState.update { state ->
                    state.copy(
                        form =
                            state.form?.copy(
                                nameError = KidNameError.Duplicate(duplicate.name),
                                isSaving = false,
                            ),
                    )
                }
            } else {
                kidRepository.updateKid(Kid(id = kidId, name = trimmedName, schoolName = trimmedSchool))
                _uiState.update { state ->
                    state.copy(form = state.form?.copy(isSaved = true, isSaving = false))
                }
            }
        }
    }

    /** Called by the UI after it has reacted to the form's isSaved flag. */
    fun onSavedHandled() {
        _uiState.update { state -> state.copy(form = state.form?.copy(isSaved = false)) }
    }

    private fun updateForm(transform: (AddKidUiState) -> AddKidUiState) {
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }
    }
}
