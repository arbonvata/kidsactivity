package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddKidUiState(
    val name: String = "",
    val schoolName: String = "",
    val nameError: KidNameError? = null,
    val schoolNameError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/** Validation failures for the kid name field; [Duplicate] carries the name it collided with. */
sealed interface KidNameError {
    data object Blank : KidNameError

    data class Duplicate(
        val existingName: String,
    ) : KidNameError
}

/**
 * Form state and validation for the "Add Kid" screen. Owns nothing else:
 * the kid list itself lives in [KidsViewModel]. The duplicate check and the
 * insert hit the database off the main thread, so [saveKid] sets
 * [AddKidUiState.isSaving] while it runs.
 */
class AddKidViewModel(
    private val kidRepository: KidRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddKidUiState())
    val uiState: StateFlow<AddKidUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onSchoolNameChange(schoolName: String) {
        _uiState.update { it.copy(schoolName = schoolName, schoolNameError = false) }
    }

    fun saveKid() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val trimmedSchool = state.schoolName.trim()

        val blankName = trimmedName.isEmpty()
        val blankSchool = trimmedSchool.isEmpty()
        if (blankName || blankSchool) {
            _uiState.update {
                it.copy(
                    nameError = KidNameError.Blank.takeIf { blankName },
                    schoolNameError = blankSchool,
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, nameError = null, schoolNameError = false) }
        viewModelScope.launch {
            val duplicate =
                kidRepository.getAllKids().firstOrNull { kid ->
                    kid.name.equals(trimmedName, ignoreCase = true)
                }
            if (duplicate != null) {
                _uiState.update {
                    it.copy(
                        nameError = KidNameError.Duplicate(duplicate.name),
                        isSaving = false,
                    )
                }
            } else {
                kidRepository.addKid(
                    name = trimmedName,
                    schoolName = trimmedSchool,
                )
                _uiState.update { it.copy(isSaved = true, isSaving = false) }
            }
        }
    }

    /** Called by the UI after it has reacted to [AddKidUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
