package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AddKidUiState(
    val name: String = "",
    val schoolName: String = "",
    val nameError: KidNameError? = null,
    val schoolNameError: Boolean = false,
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
 * the kid list itself lives in [KidsViewModel].
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

        val nameError: KidNameError? =
            if (trimmedName.isEmpty()) {
                KidNameError.Blank
            } else {
                kidRepository
                    .getAllKids()
                    .firstOrNull { kid -> kid.name.equals(trimmedName, ignoreCase = true) }
                    ?.let { kid -> KidNameError.Duplicate(kid.name) }
            }
        val schoolNameError = trimmedSchool.isEmpty()

        if (nameError == null && !schoolNameError) {
            kidRepository.addKid(
                name = trimmedName,
                schoolName = trimmedSchool,
            )
            _uiState.update { it.copy(isSaved = true) }
        } else {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    schoolNameError = schoolNameError,
                )
            }
        }
    }

    /** Called by the UI after it has reacted to [AddKidUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
