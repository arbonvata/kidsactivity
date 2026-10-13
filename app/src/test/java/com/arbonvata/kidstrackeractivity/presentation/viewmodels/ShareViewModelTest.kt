package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.repository.BackupRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ShareViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val backupRepository = mockk<BackupRepository>(relaxUnitFun = true)

    @Test
    fun exportBackupReturnsTheRepositoryDocument() =
        runTest {
            coEvery { backupRepository.exportAll() } returns """{"kids": []}"""
            val viewModel = ShareViewModel(backupRepository)

            val json = viewModel.exportBackup()

            assertEquals("""{"kids": []}""", json)
            coVerify { backupRepository.exportAll() }
        }

    @Test
    fun importBackupRunsTheTransactionAndSignalsSuccess() =
        runTest {
            val viewModel = ShareViewModel(backupRepository)

            viewModel.importBackup("""{"kids": []}""", replaceExisting = true)

            coVerify { backupRepository.importAll("""{"kids": []}""", true) }
            assertTrue(viewModel.uiState.value.result is BackupResult.Success)
            assertEquals(false, viewModel.uiState.value.isWorking)
        }

    @Test
    fun importBackupKeepsMergeAndReplaceDistinct() =
        runTest {
            val viewModel = ShareViewModel(backupRepository)

            viewModel.importBackup("""{"a": 1}""", replaceExisting = false)

            coVerify { backupRepository.importAll("""{"a": 1}""", false) }
            coVerify(exactly = 0) { backupRepository.importAll(any(), true) }
        }

    @Test
    fun importBackupSignalsErrorOnInvalidInput() =
        runTest {
            coEvery { backupRepository.importAll(any(), any()) } throws RuntimeException("not json")
            val viewModel = ShareViewModel(backupRepository)

            viewModel.importBackup("not json", replaceExisting = false)

            val result = viewModel.uiState.value.result
            assertTrue(result is BackupResult.Error)
            assertEquals(false, viewModel.uiState.value.isWorking)
        }

    @Test
    fun onResultHandledClearsTheOneShot() =
        runTest {
            val viewModel = ShareViewModel(backupRepository)
            viewModel.importBackup("""{"kids": []}""", replaceExisting = true)
            assertTrue(viewModel.uiState.value.result != null)

            viewModel.onResultHandled()

            assertNull(viewModel.uiState.value.result)
        }
}
