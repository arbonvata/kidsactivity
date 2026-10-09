package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Serves the kid list for screens that display or pick kids. The list is a
 * reactive ObjectBox query: any write from anywhere (e.g. saving a kid in
 * [AddKidViewModel]) pushes a fresh list automatically, so screens never
 * need a manual refresh.
 */
class KidsViewModel(
    kidRepository: KidRepository,
) : ViewModel() {
    val kids: StateFlow<List<Kid>> =
        kidRepository
            .getAllKidsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
