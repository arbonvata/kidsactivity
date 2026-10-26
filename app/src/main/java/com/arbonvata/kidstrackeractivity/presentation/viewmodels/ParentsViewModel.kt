package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.repository.ParentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Serves the parent list for the Parents management screen. The list is a
 * reactive ObjectBox query: any write from anywhere (e.g. saving a parent in
 * [AddParentViewModel]) pushes a fresh list automatically, so the screen
 * never needs a manual refresh.
 */
class ParentsViewModel(
    parentRepository: ParentRepository,
) : ViewModel() {
    val parents: StateFlow<List<Parent>> =
        parentRepository
            .getAllParentsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
