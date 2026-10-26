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
