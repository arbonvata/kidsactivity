package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent_
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import io.objectbox.reactive.DataObserver
import io.objectbox.reactive.DataSubscription
import io.objectbox.reactive.SubscriptionBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun updateParentPutsParent() =
        runTest {
            val parent = Parent(id = 7L, name = "Anna", relation = "mother", phone = "")
            every { parentBox.put(parent) } returns 7L

            repository.updateParent(parent)

            verify { parentBox.put(parent) }
        }

    @Test
    fun getParentReturnsParentFromBox() =
        runTest {
            val parent = Parent(id = 7L, name = "Anna")
            every { parentBox.get(7L) } returns parent

            assertSame(parent, repository.getParent(7L))
            verify { parentBox.get(7L) }
        }

    @Test
    fun getParentReturnsNullWhenMissing() =
        runTest {
            every { parentBox.get(9L) } returns null

            assertNull(repository.getParent(9L))
            verify { parentBox.get(9L) }
        }

    @Test
    fun getAllParentsOrdersByName() =
        runTest {
            val expected = listOf(Parent(id = 1L, name = "Anna"), Parent(id = 2L, name = "Mark"))
            val queryBuilder = mockk<QueryBuilder<Parent>>()
            val query = mockk<Query<Parent>>()
            every { parentBox.query() } returns queryBuilder
            every { queryBuilder.order(any()) } returns queryBuilder
            every { queryBuilder.build() } returns query
            every { query.find() } returns expected

            assertEquals(expected, repository.getAllParents())
            verify { queryBuilder.order(Parent_.name) }
        }

    @Test
    fun deleteParentRemovesOnlyTheRowNoCascadeNeeded() =
        runTest {
            every { parentBox.remove(7L) } returns true

            repository.deleteParent(7L)

            // Delegations and pickup adults store names, not ids, so there is
            // nothing to cascade from and no transaction required.
            verify { parentBox.remove(7L) }
            verify(exactly = 0) { store.runInTx(any()) }
        }

    @Test
    fun getAllParentsFlowEmitsCurrentResultAndCancelsSubscriptionWhenCollectionEnds() =
        runTest {
            val expected = listOf(Parent(id = 1L, name = "Anna"))
            val queryBuilder = mockk<QueryBuilder<Parent>>()
            val query = mockk<Query<Parent>>()
            val subscriptionBuilder = mockk<SubscriptionBuilder<List<Parent>>>()
            val subscription = mockk<DataSubscription>(relaxUnitFun = true)
            every { parentBox.query() } returns queryBuilder
            every { queryBuilder.order(any()) } returns queryBuilder
            every { queryBuilder.build() } returns query
            every { query.subscribe() } returns subscriptionBuilder
            every { subscriptionBuilder.observer(any()) } answers {
                firstArg<DataObserver<List<Parent>>>().onData(expected)
                subscription
            }

            val emissions = repository.getAllParentsFlow().take(1).toList()

            assertEquals(listOf(expected), emissions)
            verify { queryBuilder.order(Parent_.name) }
            verify { subscription.cancel() }
        }
}
