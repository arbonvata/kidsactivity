package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.data.repository.ParentRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AddParentViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val parentRepository = mockk<ParentRepository>()
    private val viewModel = AddParentViewModel(parentRepository)

    @Test
    fun saveParentBlocksBlankNameWithoutHittingRepository() =
        runTest {
            viewModel.onNameChange("   ")

            viewModel.saveParent()

            assertEquals(ParentNameError.Blank, viewModel.uiState.value.nameError)
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { parentRepository.addParent(any(), any(), any()) }
        }

    @Test
    fun saveParentFlagsCaseInsensitiveDuplicateWithStoredNameAndSkipsInsert() =
        runTest {
            coEvery { parentRepository.getAllParents() } returns
                listOf(Parent(id = 1L, name = "Anna", relation = "mother", phone = ""))
            viewModel.onNameChange("anna")

            viewModel.saveParent()

            assertEquals(ParentNameError.Duplicate("Anna"), viewModel.uiState.value.nameError)
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { parentRepository.addParent(any(), any(), any()) }
        }

    @Test
    fun saveParentInsertsTrimmedFieldsWithRelationKeyAndSignalsSaved() =
        runTest {
            coEvery { parentRepository.getAllParents() } returns emptyList()
            coEvery { parentRepository.addParent(any(), any(), any()) } returns com.arbonvata.kidstrackeractivity.data.objectbox.Parent(id = 42L)
            viewModel.onNameChange("  Anna  ")
            viewModel.onRelationChange(ParentRelation.MOTHER)
            viewModel.onPhoneChange("  +46 70 111 22 33  ")

            viewModel.saveParent()

            coVerify { parentRepository.addParent("Anna", "mother", "+46 70 111 22 33") }
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun onNameChangeClearsStaleNameError() =
        runTest {
            viewModel.onNameChange("   ")
            viewModel.saveParent()
            assertEquals(ParentNameError.Blank, viewModel.uiState.value.nameError)

            viewModel.onNameChange("Anna")

            assertEquals(null, viewModel.uiState.value.nameError)
        }

    @Test
    fun onSavedHandledResetsIsSaved() =
        runTest {
            coEvery { parentRepository.getAllParents() } returns emptyList()
            coEvery { parentRepository.addParent(any(), any(), any()) } returns com.arbonvata.kidstrackeractivity.data.objectbox.Parent()
            viewModel.onNameChange("Anna")
            viewModel.saveParent()
            assertTrue(viewModel.uiState.value.isSaved)

            viewModel.onSavedHandled()

            assertFalse(viewModel.uiState.value.isSaved)
        }
}
