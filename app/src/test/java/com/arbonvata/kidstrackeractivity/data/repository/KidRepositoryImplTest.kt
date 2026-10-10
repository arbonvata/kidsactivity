package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule_
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid_
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KidRepositoryImpl] against a mocked [BoxStore] and [Box]es;
 * they verify which box/query/transaction operations each call performs, not
 * ObjectBox behaviour itself. Query results are stubbed, so "ordered by name"
 * is asserted as "the query is built with an order clause", not re-sorted data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KidRepositoryImplTest {
    private val kidBox = mockk<Box<Kid>>(relaxUnitFun = true)
    private val activityBox = mockk<Box<Activity>>(relaxUnitFun = true)
    private val scheduleBox = mockk<Box<DailySchedule>>(relaxUnitFun = true)

    private val kidQueryBuilder = mockk<QueryBuilder<Kid>>()
    private val kidQuery = mockk<Query<Kid>>()
    private val kidSubscriptionBuilder = mockk<SubscriptionBuilder<List<Kid>>>()
    private val kidSubscription = mockk<DataSubscription>(relaxUnitFun = true)

    private val activityQueryBuilder = mockk<QueryBuilder<Activity>>()
    private val activityQuery = mockk<Query<Activity>>()
    private val scheduleQueryBuilder = mockk<QueryBuilder<DailySchedule>>()
    private val scheduleQuery = mockk<Query<DailySchedule>>()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val store =
        mockk<BoxStore>(relaxUnitFun = true) {
            // Needed during the constructor; the other boxes are only
            // touched by deleteKid and are stubbed per test.
            every { boxFor(Kid::class.java) } returns kidBox
        }

    private val repository =
        KidRepositoryImpl(
            store = store,
            ioDispatcher = testDispatcher,
        )

    @BeforeEach
    fun setUpBoxesAndQueryChains() {
        every { store.boxFor(Activity::class.java) } returns activityBox
        every { store.boxFor(DailySchedule::class.java) } returns scheduleBox

        every { kidBox.query() } returns kidQueryBuilder
        every { kidQueryBuilder.order(any()) } returns kidQueryBuilder
        every { kidQueryBuilder.build() } returns kidQuery
        every { kidQuery.find() } returns emptyList()
        every { kidQuery.subscribe() } returns kidSubscriptionBuilder

        every { activityBox.query() } returns activityQueryBuilder
        every { activityQueryBuilder.equal(any(), any<Long>()) } returns activityQueryBuilder
        every { activityQueryBuilder.build() } returns activityQuery
        every { activityQuery.remove() } returns 0L

        every { scheduleBox.query() } returns scheduleQueryBuilder
        every { scheduleQueryBuilder.equal(any(), any<Long>()) } returns scheduleQueryBuilder
        every { scheduleQueryBuilder.build() } returns scheduleQuery
        every { scheduleQuery.remove() } returns 0L

        // Run the runnable inline so deleteKid's cascade verifications below
        // cover what actually happens inside the transaction callback.
        every { store.runInTx(any()) } answers { firstArg<Runnable>().run() }
    }

    @Test
    fun addKidBuildsKidFromArgumentsAndReturnsItWithAssignedId() =
        runTest {
            val kidSlot = slot<Kid>()
            every { kidBox.put(capture(kidSlot)) } returns 42L

            val result = repository.addKid("Leo", "Sunshine School")

            assertEquals("Leo", kidSlot.captured.name)
            assertEquals("Sunshine School", kidSlot.captured.schoolName)
            assertSame(kidSlot.captured, result)
            assertEquals(42L, result.id)
            verify { kidBox.put(kidSlot.captured) }
        }

    @Test
    fun updateKidPutsKid() =
        runTest {
            val kid = Kid(id = 7L, name = "Leo", schoolName = "Sunshine School")
            every { kidBox.put(kid) } returns 7L

            repository.updateKid(kid)

            verify { kidBox.put(kid) }
        }

    @Test
    fun getKidReturnsKidFromBox() =
        runTest {
            val kid = Kid(id = 7L, name = "Leo")
            every { kidBox.get(7L) } returns kid

            assertSame(kid, repository.getKid(7L))
            verify { kidBox.get(7L) }
        }

    @Test
    fun getKidReturnsNullWhenMissing() =
        runTest {
            every { kidBox.get(9L) } returns null

            assertNull(repository.getKid(9L))
            verify { kidBox.get(9L) }
        }

    @Test
    fun getAllKidsOrdersByName() =
        runTest {
            val expected = listOf(Kid(id = 1L, name = "Anna"), Kid(id = 2L, name = "Leo"))
            every { kidQuery.find() } returns expected

            assertEquals(expected, repository.getAllKids())
            verify { kidQueryBuilder.order(Kid_.name) }
        }

    @Test
    fun deleteKidCascadesToActivitiesAndScheduleOverridesInsideSingleTransaction() =
        runTest {
            every { kidBox.remove(7L) } returns true

            repository.deleteKid(7L)

            verify { store.runInTx(any()) }
            verify { activityQueryBuilder.equal(Activity_.kidId, 7L) }
            verify { activityQuery.remove() }
            verify { scheduleQueryBuilder.equal(DailySchedule_.kidId, 7L) }
            verify { scheduleQuery.remove() }
            verify { kidBox.remove(7L) }
        }

    @Test
    fun getAllKidsFlowEmitsCurrentResultAndCancelsSubscriptionWhenCollectionEnds() =
        runTest {
            val expected = listOf(Kid(id = 1L, name = "Leo"))
            every { kidSubscriptionBuilder.observer(any()) } answers {
                firstArg<DataObserver<List<Kid>>>().onData(expected)
                kidSubscription
            }

            val emissions = repository.getAllKidsFlow().take(1).toList()

            assertEquals(listOf(expected), emissions)
            verify { kidQueryBuilder.order(Kid_.name) }
            verify { kidSubscription.cancel() }
        }

    @Test
    fun clearAllRemovesEverythingFromTheBox() =
        runTest {
            repository.clearAll()

            verify { kidBox.removeAll() }
        }
}
