package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.objectbox.ParentRelation
import com.arbonvata.kidstrackeractivity.data.repository.ParentRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class EditParentViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val parentRepository = mockk<ParentRepository>(relaxUnitFun = true)

    @Test
    fun initLoadsStoredParentIntoTheForm() =
        runTest {
            coEvery { parentRepository.getParent(7L) } returns
                Parent(id = 7L, name = "Anna", relation = "mother", phone = "+46 70 111 22 33")

            val viewModel = EditParentViewModel(7L, parentRepository)

            assertEquals("Anna", viewModel.uiState.value.form?.name)
            assertEquals(ParentRelation.MOTHER, viewModel.uiState.value.form?.relation)
            assertEquals("+46 70 111 22 33", viewModel.uiState.value.form?.phone)
            assertFalse(viewModel.uiState.value.isNotFound)
        }

    @Test
    fun initFlagsMissingParentAsNotFound() =
        runTest {
            coEvery { parentRepository.getParent(9L) } returns null

            val viewModel = EditParentViewModel(9L, parentRepository)

            assertTrue(viewModel.uiState.value.isNotFound)
        }

    @Test
    fun duplicateCheckIgnoresTheEditedParentItself() =
        runTest {
            coEvery { parentRepository.getParent(7L) } returns Parent(id = 7L, name = "Anna", relation = "mother", phone = "")
            coEvery { parentRepository.getAllParents() } returns listOf(Parent(id = 7L, name = "Anna", relation = "mother", phone = ""))
            val viewModel = EditParentViewModel(7L, parentRepository)
            viewModel.onNameChange("anna")

            viewModel.saveParent()

            assertNull(viewModel.uiState.value.form?.nameError)
            assertTrue(viewModel.uiState.value.form?.isSaved == true)
        }

    @Test
    fun saveParentUpdatesTrimmedFieldsWithRelationKey() =
        runTest {
            coEvery { parentRepository.getParent(7L) } returns Parent(id = 7L, name = "Anna", relation = "mother", phone = "")
            coEvery { parentRepository.getAllParents() } returns emptyList()
            val viewModel = EditParentViewModel(7L, parentRepository)
            val saved = slot<Parent>()
            coEvery { parentRepository.updateParent(capture(saved)) } just Runs
            viewModel.onNameChange("  Anna  ")
            viewModel.onRelationChange(ParentRelation.GUARDIAN)
            viewModel.onPhoneChange("  +46 70 999 88 77  ")

            viewModel.saveParent()

            assertEquals("Anna", saved.captured.name)
            assertEquals("guardian", saved.captured.relation)
            assertEquals("+46 70 999 88 77", saved.captured.phone)
            assertTrue(viewModel.uiState.value.form?.isSaved == true)
        }

    @Test
    fun deleteParentRemovesByIdAndSignalsDeleted() =
        runTest {
            coEvery { parentRepository.getParent(7L) } returns Parent(id = 7L, name = "Anna", relation = "mother", phone = "")
            val viewModel = EditParentViewModel(7L, parentRepository)

            viewModel.deleteParent()

            coVerify { parentRepository.deleteParent(7L) }
            assertTrue(viewModel.uiState.value.isDeleted)

            viewModel.onDeletedHandled()

            assertFalse(viewModel.uiState.value.isDeleted)
        }

    @Test
    fun unknownRelationKeyDegradesToOtherWhenLoading() =
        runTest {
            // A hand-edited backup can carry an unknown relation key.
            coEvery { parentRepository.getParent(7L) } returns Parent(id = 7L, name = "Anna", relation = "aunt", phone = "")

            val viewModel = EditParentViewModel(7L, parentRepository)

            assertEquals(ParentRelation.OTHER, viewModel.uiState.value.form?.relation)
        }
}
