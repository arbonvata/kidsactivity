package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Outcome of a restore, shown once and consumed via [ShareViewModel.onResultHandled]; the screen maps each variant to its localized message. */
sealed interface BackupResult {
    data object Success : BackupResult

    data object Error : BackupResult
}

data class ShareUiState(
    val isWorking: Boolean = false,
    val result: BackupResult? = null,
)

/**
 * Backup and restore for the whole database. Exporting returns the JSON
 * document; the screen layer decides where it goes (file picker or share
 * sheet). Importing runs in one transaction per BackupRepository, so a
 * failed restore leaves the database untouched.
 */
class ShareViewModel(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    suspend fun exportBackup(): String = backupRepository.exportAll()

    fun importBackup(
        json: String,
        replaceExisting: Boolean,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            _uiState.update {
                try {
                    backupRepository.importAll(json, replaceExisting)
                    it.copy(isWorking = false, result = BackupResult.Success)
                } catch (e: Exception) {
                    it.copy(isWorking = false, result = BackupResult.Error)
                }
            }
        }
    }

    /** Called by the UI after it has reacted to [ShareUiState.result]. */
    fun onResultHandled() {
        _uiState.update { it.copy(result = null) }
    }
}
