package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AddKidViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>()
    private val viewModel = AddKidViewModel(kidRepository)

    @Test
    fun saveKidBlocksBlankNameAndBlankSchoolWithoutHittingRepository() =
        runTest {
            viewModel.onNameChange("   ")
            viewModel.onSchoolNameChange("   ")

            viewModel.saveKid()

            assertEquals(KidNameError.Blank, viewModel.uiState.value.nameError)
            assertTrue(viewModel.uiState.value.schoolNameError)
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify(exactly = 0) { kidRepository.getAllKids() }
            coVerify(exactly = 0) { kidRepository.addKid(any(), any()) }
        }

    @Test
    fun saveKidBlocksBlankNameOnlyWhenSchoolIsValid() =
        runTest {
            viewModel.onNameChange("")
            viewModel.onSchoolNameChange("Sunshine School")

            viewModel.saveKid()

            assertEquals(KidNameError.Blank, viewModel.uiState.value.nameError)
            assertFalse(viewModel.uiState.value.schoolNameError)
            coVerify(exactly = 0) { kidRepository.addKid(any(), any()) }
        }

    @Test
    fun saveKidFlagsCaseInsensitiveDuplicateWithStoredNameAndSkipsInsert() =
        runTest {
            coEvery { kidRepository.getAllKids() } returns
                listOf(Kid(id = 1L, name = "Leo", schoolName = "Other School"))
            viewModel.onNameChange("leo")
            viewModel.onSchoolNameChange("Sunshine School")

            viewModel.saveKid()

            assertEquals(KidNameError.Duplicate("Leo"), viewModel.uiState.value.nameError)
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSaved)
            coVerify { kidRepository.getAllKids() }
            coVerify(exactly = 0) { kidRepository.addKid(any(), any()) }
        }

    @Test
    fun saveKidInsertsTrimmedFieldsAndSignalsSaved() =
        runTest {
            coEvery { kidRepository.getAllKids() } returns emptyList()
            coEvery { kidRepository.addKid(any(), any()) } returns Kid(id = 42L)
            viewModel.onNameChange("  Leo  ")
            viewModel.onSchoolNameChange("  Sunshine School  ")

            viewModel.saveKid()

            coVerify { kidRepository.getAllKids() }
            coVerify { kidRepository.addKid("Leo", "Sunshine School") }
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun onSavedHandledResetsIsSaved() =
        runTest {
            coEvery { kidRepository.getAllKids() } returns emptyList()
            coEvery { kidRepository.addKid(any(), any()) } returns Kid()
            viewModel.onNameChange("Leo")
            viewModel.onSchoolNameChange("Sunshine School")
            viewModel.saveKid()
            assertTrue(viewModel.uiState.value.isSaved)

            viewModel.onSavedHandled()

            assertFalse(viewModel.uiState.value.isSaved)
        }

    @Test
    fun onNameChangeClearsStaleNameError() =
        runTest {
            viewModel.onNameChange("   ")
            viewModel.saveKid()
            assertEquals(KidNameError.Blank, viewModel.uiState.value.nameError)

            viewModel.onNameChange("Leo")

            assertNull(viewModel.uiState.value.nameError)
        }
}
