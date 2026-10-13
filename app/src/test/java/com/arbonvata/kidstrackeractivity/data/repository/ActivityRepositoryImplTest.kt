package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.objectbox.Box
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ActivityRepositoryImpl] against a mocked [Box]; they verify
 * which box/query operations each call performs, not ObjectBox behaviour
 * itself. Query results are stubbed, so "ordered by start time" for example is
 * asserted as "the query is built with an order clause", not re-sorted data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityRepositoryImplTest {
    private val activityBox = mockk<Box<Activity>>(relaxUnitFun = true)
    private val queryBuilder = mockk<QueryBuilder<Activity>>()
    private val query = mockk<Query<Activity>>()
    private val subscriptionBuilder = mockk<SubscriptionBuilder<List<Activity>>>()
    private val subscription = mockk<DataSubscription>(relaxUnitFun = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository =
        ActivityRepositoryImpl(
            store =
                mockk {
                    every { boxFor(Activity::class.java) } returns activityBox
                },
            ioDispatcher = testDispatcher,
        )

    @BeforeEach
    fun setUpQueryChain() {
        every { activityBox.query() } returns queryBuilder
        every { queryBuilder.equal(any(), any<Long>()) } returns queryBuilder
        every { queryBuilder.order(any()) } returns queryBuilder
        every { queryBuilder.build() } returns query
        every { query.find() } returns emptyList()
        every { query.remove() } returns 0L
        every { query.subscribe() } returns subscriptionBuilder
    }

    @Test
    fun addActivityPutsActivityAndReturnsItWithAssignedId() =
        runTest {
            val activity =
                Activity(
                    kidId = 1L,
                    name = "Football",
                    startTime = "17:00",
                    endTime = "18:30",
                    daysOfWeek = intArrayOf(2),
                )
            every { activityBox.put(activity) } returns 42L

            val result = repository.addActivity(activity)

            verify { activityBox.put(activity) }
            assertSame(activity, result)
            assertEquals(42L, result.id)
        }

    @Test
    fun updateActivityPutsEntity() =
        runTest {
            val activity = Activity(id = 5L, kidId = 1L, name = "Football")
            every { activityBox.put(activity) } returns 5L

            repository.updateActivity(activity)

            verify { activityBox.put(activity) }
        }

    @Test
    fun deleteActivityRemovesById() =
        runTest {
            every { activityBox.remove(7L) } returns true

            repository.deleteActivity(7L)

            verify { activityBox.remove(7L) }
        }

    @Test
    fun getActivityReturnsActivityFromTheBox() =
        runTest {
            val stored = Activity(id = 5L, kidId = 1L, name = "Football")
            every { activityBox.get(5L) } returns stored

            assertSame(stored, repository.getActivity(5L))

            verify { activityBox.get(5L) }
        }

    @Test
    fun getActivityReturnsNullWhenMissing() =
        runTest {
            every { activityBox.get(9L) } returns null

            assertNull(repository.getActivity(9L))

            verify { activityBox.get(9L) }
        }

    @Test
    fun setActivityEnabledMutatesFetchedActivityAndPutsIt() =
        runTest {
            val stored = Activity(id = 5L, kidId = 1L, name = "Football", isEnabled = true)
            every { activityBox.get(5L) } returns stored
            every { activityBox.put(stored) } returns 5L

            repository.setActivityEnabled(5L, enabled = false)

            assertFalse(stored.isEnabled)
            verify { activityBox.put(stored) }
        }

    @Test
    fun setActivityEnabledDoesNotPutWhenActivityMissing() =
        runTest {
            every { activityBox.get(9L) } returns null

            repository.setActivityEnabled(9L, enabled = true)

            verify(exactly = 0) { activityBox.put(any<Activity>()) }
        }

    @Test
    fun getActivitiesForKidFiltersByKidIdAndOrdersByStartTime() =
        runTest {
            val expected = listOf(Activity(id = 1L, kidId = 3L, name = "Piano", startTime = "16:00"))
            every { query.find() } returns expected

            val result = repository.getActivitiesForKid(3L)

            assertEquals(expected, result)
            verify { queryBuilder.equal(Activity_.kidId, 3L) }
            verify { queryBuilder.order(Activity_.startTime) }
        }

    @Test
    fun getActivitiesForKidOnDayFiltersDaysInMemory() =
        runTest {
            val stored =
                listOf(
                    Activity(id = 1L, kidId = 3L, name = "Football", startTime = "17:00", daysOfWeek = intArrayOf(2, 4)),
                    Activity(id = 2L, kidId = 3L, name = "Piano", startTime = "16:00", daysOfWeek = intArrayOf(1, 3)),
                    Activity(id = 3L, kidId = 3L, name = "Swimming", startTime = "18:00", daysOfWeek = intArrayOf(2)),
                )
            every { query.find() } returns stored

            val result = repository.getActivitiesForKidOnDay(3L, dayOfWeek = 2)

            assertEquals(listOf(stored[0], stored[2]), result)
            verify { queryBuilder.equal(Activity_.kidId, 3L) }
        }

    @Test
    fun getAllActivitiesFindsAll() =
        runTest {
            val expected = listOf(Activity(id = 1L), Activity(id = 2L))
            every { query.find() } returns expected

            assertEquals(expected, repository.getAllActivities())
            verify { query.find() }
            verify(exactly = 0) { queryBuilder.equal(any(), any<Long>()) }
        }

    @Test
    fun deleteAllActivitiesForKidRemovesQueryMatches() =
        runTest {
            repository.deleteAllActivitiesForKid(3L)

            verify { queryBuilder.equal(Activity_.kidId, 3L) }
            verify { query.remove() }
        }

    @Test
    fun clearAllRemovesEverythingFromTheBox() =
        runTest {
            repository.clearAll()

            verify { activityBox.removeAll() }
        }

    @Test
    fun getAllActivitiesFlowEmitsCurrentResultAndCancelsSubscriptionWhenCollectionEnds() =
        runTest {
            val expected = listOf(Activity(id = 1L))
            every { subscriptionBuilder.observer(any()) } answers {
                firstArg<DataObserver<List<Activity>>>().onData(expected)
                subscription
            }

            val emissions = repository.getAllActivitiesFlow().take(1).toList()

            assertEquals(listOf(expected), emissions)
            verify { subscription.cancel() }
        }

    @Test
    fun getActivitiesForKidFlowFiltersByKidIdAndCancelsSubscriptionWhenCollectionEnds() =
        runTest {
            val expected = listOf(Activity(id = 1L, kidId = 3L))
            every { subscriptionBuilder.observer(any()) } answers {
                firstArg<DataObserver<List<Activity>>>().onData(expected)
                subscription
            }

            val emissions = repository.getActivitiesForKidFlow(3L).take(1).toList()

            assertEquals(listOf(expected), emissions)
            verify { queryBuilder.equal(Activity_.kidId, 3L) }
            verify { subscription.cancel() }
        }
}
