package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class EditKidViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val kidRepository = mockk<KidRepository>(relaxUnitFun = true)

    @Test
    fun loadsStoredKidIntoTheForm() =
        runTest {
            coEvery { kidRepository.getKid(7L) } returns Kid(id = 7L, name = "Leo", schoolName = "Greenfield")

            val viewModel = EditKidViewModel(7L, kidRepository)

            val form = viewModel.uiState.value.form
            assertNotNull(form)
            assertEquals("Leo", form?.name)
            assertEquals("Greenfield", form?.schoolName)
            assertFalse(viewModel.uiState.value.isNotFound)
            coVerify { kidRepository.getKid(7L) }
        }

    @Test
    fun missingKidSetsNotFound() =
        runTest {
            coEvery { kidRepository.getKid(9L) } returns null

            val viewModel = EditKidViewModel(9L, kidRepository)

            assertTrue(viewModel.uiState.value.isNotFound)
            assertEquals(null, viewModel.uiState.value.form)
        }

    @Test
    fun saveUpdatesTrimmedFieldsViaTheRepository() =
        runTest {
            coEvery { kidRepository.getKid(7L) } returns Kid(id = 7L, name = "Leo", schoolName = "Greenfield")
            coEvery { kidRepository.getAllKids() } returns emptyList()
            val updatedKid = slot<Kid>()
            coEvery { kidRepository.updateKid(capture(updatedKid)) } returns Unit
            val viewModel = EditKidViewModel(7L, kidRepository)
            viewModel.onNameChange("  Leo K.  ")
            viewModel.onSchoolNameChange("  Greenfield Primary  ")

            viewModel.saveKid()

            coVerify { kidRepository.updateKid(any()) }
            assertEquals(Kid(id = 7L, name = "Leo K.", schoolName = "Greenfield Primary"), updatedKid.captured)
            assertTrue(
                viewModel.uiState.value.form
                    ?.isSaved == true,
            )
            assertFalse(
                viewModel.uiState.value.form
                    ?.isSaving == true,
            )
        }

    @Test
    fun duplicateCheckIgnoresTheKidItself() =
        runTest {
            coEvery { kidRepository.getKid(7L) } returns Kid(id = 7L, name = "Leo", schoolName = "Greenfield")
            // "leo" collides only with the kid being edited (case-insensitive).
            coEvery { kidRepository.getAllKids() } returns listOf(Kid(id = 7L, name = "Leo"), Kid(id = 2L, name = "Mia"))
            val viewModel = EditKidViewModel(7L, kidRepository)
            viewModel.onNameChange("leo")

            viewModel.saveKid()

            coVerify { kidRepository.updateKid(any()) }
            assertTrue(
                viewModel.uiState.value.form
                    ?.isSaved == true,
            )
        }

    @Test
    fun duplicateOfAnotherKidIsFlaggedAndSkipped() =
        runTest {
            coEvery { kidRepository.getKid(7L) } returns Kid(id = 7L, name = "Leo", schoolName = "Greenfield")
            coEvery { kidRepository.getAllKids() } returns listOf(Kid(id = 7L, name = "Leo"), Kid(id = 2L, name = "Mia"))
            val viewModel = EditKidViewModel(7L, kidRepository)
            viewModel.onNameChange("mia")

            viewModel.saveKid()

            assertEquals(
                KidNameError.Duplicate("Mia"),
                viewModel.uiState.value.form
                    ?.nameError,
            )
            assertFalse(
                viewModel.uiState.value.form
                    ?.isSaved == true,
            )
            coVerify(exactly = 0) { kidRepository.updateKid(any()) }
        }

    @Test
    fun blankFieldsAreBlockedWithoutHittingTheRepository() =
        runTest {
            coEvery { kidRepository.getKid(7L) } returns Kid(id = 7L, name = "Leo", schoolName = "Greenfield")
            val viewModel = EditKidViewModel(7L, kidRepository)
            viewModel.onNameChange("   ")
            viewModel.onSchoolNameChange("   ")

            viewModel.saveKid()

            assertEquals(
                KidNameError.Blank,
                viewModel.uiState.value.form
                    ?.nameError,
            )
            assertTrue(
                viewModel.uiState.value.form
                    ?.schoolNameError == true,
            )
            coVerify(exactly = 0) { kidRepository.getAllKids() }
            coVerify(exactly = 0) { kidRepository.updateKid(any()) }
        }
}
