package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.calendar.CalendarImport
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.CalendarImportResult
import com.arbonvata.kidstrackeractivity.data.repository.CalendarRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarShareState(
    val kids: List<Kid> = emptyList(),
    val exportKidId: Long = 0,
    val targetKidId: Long = 0,
    val newChildName: String = "",
    val pending: CalendarImport? = null,
    val isWorking: Boolean = false,
    val error: String? = null,
    val result: CalendarImportResult? = null,
)

class CalendarShareViewModel(
    private val repository: CalendarRepository,
    kidRepository: KidRepository,
) : ViewModel() {
    private val state = MutableStateFlow(CalendarShareState())
    val uiState =
        combine(state, kidRepository.getAllKidsFlow()) { current, kids -> current.copy(kids = kids) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), state.value)

    fun selectExportKid(id: Long) {
        state.update { it.copy(exportKidId = id) }
    }

    fun selectTargetKid(id: Long) {
        state.update { it.copy(targetKidId = id) }
    }

    fun changeChildName(name: String) {
        state.update { it.copy(newChildName = name) }
    }

    fun dismissMessage() {
        state.update { it.copy(error = null, result = null) }
    }

    fun dismissImport() {
        if (!state.value.isWorking) state.update { it.copy(pending = null) }
    }

    fun reportFailure(error: Exception) {
        state.update {
            it.copy(
                isWorking = false,
                error =
                    error.message ?: "Unable to read or write calendar",
            )
        }
    }

    suspend fun export(): String? {
        if (state.value.isWorking || state.value.exportKidId == 0L) return null
        state.update { it.copy(isWorking = true, error = null) }
        return try {
            repository.export(state.value.exportKidId).also { state.update { it.copy(isWorking = false) } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure(e)
            null
        }
    }

    fun preview(text: String) {
        if (state.value.isWorking) return
        state.update { it.copy(isWorking = true, error = null, result = null) }
        viewModelScope.launch {
            try {
                val parsed = repository.preview(text)
                val existing = uiState.value.kids.firstOrNull { it.name.equals(parsed.childName, ignoreCase = true) }
                state.update {
                    it.copy(isWorking = false, pending = parsed, targetKidId = existing?.id ?: 0, newChildName = parsed.childName)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportFailure(e)
            }
        }
    }

    fun importCalendar() {
        val current = state.value
        val pending = current.pending ?: return
        if (current.isWorking || (current.targetKidId == 0L && current.newChildName.isBlank())) return
        state.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.importCalendar(pending, current.targetKidId.takeIf { it != 0L }, current.newChildName)
                state.update { it.copy(isWorking = false, pending = null, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportFailure(e)
            }
        }
    }
}
