package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.data.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditParentUiState(
    /** The loaded form; null while loading. */
    val form: AddParentUiState? = null,
    val isNotFound: Boolean = false,
    /** One-shot: the parent was deleted, the screen should navigate back. */
    val isDeleted: Boolean = false,
)

/**
 * Edit form for one existing parent. Reuses [AddParentUiState] so the Add
 * screen's composable renders this too. The duplicate check ignores the
 * parent being edited, so keeping (or case-correcting) its own name stays
 * valid. Deleting has no cascade — delegations store names, not ids — so
 * existing "picked by" text simply keeps the old name.
 */
class EditParentViewModel(
    private val parentId: Long,
    private val parentRepository: ParentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditParentUiState())
    val uiState: StateFlow<EditParentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val parent = parentRepository.getParent(parentId)
            if (parent == null) {
                _uiState.update { it.copy(isNotFound = true) }
            } else {
                _uiState.update {
                    it.copy(form = AddParentUiState(name = parent.name, relation = ParentRelation.fromKey(parent.relation), phone = parent.phone))
                }
            }
        }
    }

    fun onNameChange(name: String) = updateForm { it.copy(name = name, nameError = null) }

    fun onRelationChange(relation: ParentRelation) = updateForm { it.copy(relation = relation) }

    fun onPhoneChange(phone: String) = updateForm { it.copy(phone = phone) }

    fun saveParent() {
        val form = _uiState.value.form ?: return
        val trimmedName = form.name.trim()
        val trimmedPhone = form.phone.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { state ->
                state.copy(form = form.copy(nameError = ParentNameError.Blank))
            }
            return
        }

        _uiState.update { state ->
            state.copy(form = form.copy(isSaving = true, nameError = null))
        }
        viewModelScope.launch {
            val duplicate =
                parentRepository.getAllParents().firstOrNull { parent ->
                    parent.id != parentId && parent.name.equals(trimmedName, ignoreCase = true)
                }
            if (duplicate != null) {
                _uiState.update { state ->
                    state.copy(
                        form =
                            state.form?.copy(
                                nameError = ParentNameError.Duplicate(duplicate.name),
                                isSaving = false,
                            ),
                    )
                }
            } else {
                parentRepository.updateParent(
                    Parent(
                        id = parentId,
                        name = trimmedName,
                        relation = form.relation.key,
                        phone = trimmedPhone,
                    ),
                )
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

    fun deleteParent() {
        viewModelScope.launch {
            parentRepository.deleteParent(parentId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    /** Called by the UI after it has reacted to [EditParentUiState.isDeleted]. */
    fun onDeletedHandled() {
        _uiState.update { it.copy(isDeleted = false) }
    }

    private fun updateForm(transform: (AddParentUiState) -> AddParentUiState) {
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }
    }
}
