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
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/**
 * Form state for the "Add Parent" screen: fills the fields, validates the
 * name, and persists one Parent row via [ParentRepository.addParent].
 */
class AddParentViewModel(
    private val parentRepository: ParentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddParentUiState())
    val uiState: StateFlow<AddParentUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = false) }
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
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            parentRepository.addParent(
                name = trimmedName,
                relation = state.relation.key,
                phone = state.phone.trim(),
            )
            _uiState.update { it.copy(isSaved = true, isSaving = false) }
        }
    }

    /** Called by the UI after it has reacted to [AddParentUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
