package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.data.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddParentUiState(
    val name: String = "",
    val relation: ParentRelation = ParentRelation.FATHER,
    val phone: String = "",
    val nameError: ParentNameError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/** Validation failures for the parent name field; [Duplicate] carries the name it collided with. */
sealed interface ParentNameError {
    data object Blank : ParentNameError

    data class Duplicate(
        val existingName: String,
    ) : ParentNameError
}

/**
 * Form state and validation for the "Add Parent" screen. Mirrors the kid
 * form: blank and case-insensitive duplicate names are rejected, everything
 * is trimmed, and the database work happens off the main thread while
 * [AddParentUiState.isSaving] is set.
 */
class AddParentViewModel(
    private val parentRepository: ParentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddParentUiState())
    val uiState: StateFlow<AddParentUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onRelationChange(relation: ParentRelation) {
        _uiState.update { it.copy(relation = relation) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun saveParent() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val trimmedPhone = state.phone.trim()

        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(nameError = ParentNameError.Blank) }
            return
        }

        _uiState.update { it.copy(isSaving = true, nameError = null) }
        viewModelScope.launch {
            val duplicate =
                parentRepository.getAllParents().firstOrNull { parent ->
                    parent.name.equals(trimmedName, ignoreCase = true)
                }
            if (duplicate != null) {
                _uiState.update {
                    it.copy(
                        nameError = ParentNameError.Duplicate(duplicate.name),
                        isSaving = false,
                    )
                }
            } else {
                parentRepository.addParent(
                    name = trimmedName,
                    relation = state.relation.key,
                    phone = trimmedPhone,
                )
                _uiState.update { it.copy(isSaved = true, isSaving = false) }
            }
        }
    }

    /** Called by the UI after it has reacted to [AddParentUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
