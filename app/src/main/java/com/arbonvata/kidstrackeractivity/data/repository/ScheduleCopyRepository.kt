package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Copies templates atomically; date exceptions and school-day overrides belong to the source child. */
interface ScheduleCopyRepository {
    suspend fun copy(
        sourceKidId: Long,
        targetKidId: Long,
        activityId: Long?,
        oneOffWeek: LocalDate,
        activityName: String?,
    ): Int
}

class ScheduleCopyRepositoryImpl(
    private val store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ScheduleCopyRepository {
    override suspend fun copy(
        sourceKidId: Long,
        targetKidId: Long,
        activityId: Long?,
        oneOffWeek: LocalDate,
        activityName: String?,
    ): Int =
        withContext(ioDispatcher) {
            val kids = store.boxFor(Kid::class.java)
            val activities = store.boxFor(Activity::class.java)
            var count = 0
            store.runInTx {
                require(kids.get(sourceKidId) != null && kids.get(targetKidId) != null) { "Child no longer exists" }
                require(activityId != null || sourceKidId != targetKidId) { "Choose another child for a full schedule" }
                val source =
                    if (activityId != null) {
                        listOf(requireNotNull(activities.get(activityId)).also { require(it.kidId == sourceKidId) })
                    } else {
                        activities
                            .query()
                            .equal(Activity_.kidId, sourceKidId)
                            .build()
                            .use { it.find() }
                    }
                require(source.isNotEmpty()) { "No activities to copy" }
                val weekStart = oneOffWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                source.forEach { original ->
                    val name =
                        if (activityId != null) {
                            requireNotNull(activityName).trim().also { require(it.isNotBlank()) }
                        } else {
                            original.name
                        }
                    activities.put(
                        original.copy(
                            id = 0,
                            kidId = targetKidId,
                            name = name,
                            daysOfWeek = original.daysOfWeek.copyOf(),
                            firstDate =
                                if (!original.isPeriodic &&
                                    !original.isSchoolActivity
                                ) {
                                    weekStart.toString()
                                } else {
                                    original.firstDate
                                },
                            occurrenceExceptions = null,
                        ),
                    )
                }
                count = source.size
            }
            count
        }
}
