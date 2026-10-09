package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Serves the kid list for screens that display or pick kids. Editing and
 * adding live in their own view models; call [refresh] when returning to a
 * screen after another one may have changed the data.
 */
class KidsViewModel(
    private val kidRepository: KidRepository,
) : ViewModel() {
    private val refreshTrigger = MutableStateFlow(0)

    val kids: StateFlow<List<Kid>> =
        refreshTrigger
            .map { kidRepository.getAllKids() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        refreshTrigger.value++
    }
}
