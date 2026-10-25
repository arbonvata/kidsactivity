package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ParentRepositoryImpl] against a mocked [BoxStore] and [Box];
 * they verify which box operations each call performs, not ObjectBox itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentRepositoryImplTest {
    private val parentBox = mockk<Box<Parent>>(relaxUnitFun = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    private val store =
        mockk<BoxStore>(relaxUnitFun = true) {
            every { boxFor(Parent::class.java) } returns parentBox
        }

    private val repository =
        ParentRepositoryImpl(
            store = store,
            ioDispatcher = testDispatcher,
        )

    @Test
    fun addParentBuildsParentFromArgumentsAndReturnsItWithAssignedId() =
        runTest {
            val parentSlot = slot<Parent>()
            every { parentBox.put(capture(parentSlot)) } returns 42L

            val result = repository.addParent("Anna", "mother", "+46 70 123 45 67")

            assertEquals("Anna", parentSlot.captured.name)
            assertEquals("mother", parentSlot.captured.relation)
            assertEquals("+46 70 123 45 67", parentSlot.captured.phone)
            assertSame(parentSlot.captured, result)
            assertEquals(42L, result.id)
            verify { parentBox.put(parentSlot.captured) }
        }
}
