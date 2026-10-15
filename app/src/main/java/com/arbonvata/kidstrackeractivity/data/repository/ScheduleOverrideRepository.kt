package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule_
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

interface ScheduleOverrideRepository {
    /**
     * Creates or updates the override for one kid on one date. An override
     * without an end time and without the no-school flag carries no meaning
     * and is removed instead of stored.
     */
    suspend fun setOverride(
        kidId: Long,
        date: LocalDate,
        endTime: String?,
        isNoSchool: Boolean,
    )

    suspend fun getOverride(
        kidId: Long,
        date: LocalDate,
    ): DailySchedule?

    /**
     * ObjectBox has no range comparison for strings, so dates are filtered in
     * memory after querying by kid. ISO dates sort lexicographically.
     */
    suspend fun getOverridesBetween(
        kidId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<DailySchedule>

    suspend fun deleteOverride(overrideId: Long)

    suspend fun deleteOverridesForKid(kidId: Long)

    suspend fun getAll(): List<DailySchedule>

    /** Reactive: all overrides ordered by date, now and on every change. */
    fun getAllOverridesFlow(): Flow<List<DailySchedule>>

    suspend fun clearAll()
}

class ScheduleOverrideRepositoryImpl(
    store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ScheduleOverrideRepository {
    private val scheduleBox = store.boxFor(DailySchedule::class.java)

    override suspend fun setOverride(
        kidId: Long,
        date: LocalDate,
        endTime: String?,
        isNoSchool: Boolean,
    ) {
        withContext(ioDispatcher) {
            val existing = getOverride(kidId, date)
            if (endTime == null && !isNoSchool) {
                existing?.let { deleteOverride(it.id) }
            } else {
                val schedule = existing ?: DailySchedule(kidId = kidId, date = date.toString())
                schedule.endTime = endTime ?: ""
                schedule.isNoSchool = isNoSchool
                scheduleBox.put(schedule)
            }
        }
    }

    override suspend fun getOverride(
        kidId: Long,
        date: LocalDate,
    ): DailySchedule? =
        withContext(ioDispatcher) {
            scheduleBox
                .query()
                .equal(DailySchedule_.kidId, kidId)
                .equal(DailySchedule_.date, date.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)
                .build()
                .findFirst()
        }

    override suspend fun getOverridesBetween(
        kidId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<DailySchedule> =
        withContext(ioDispatcher) {
            scheduleBox
                .query()
                .equal(DailySchedule_.kidId, kidId)
                .build()
                .find()
                .filter { it.date >= start.toString() && it.date <= end.toString() }
        }

    override suspend fun deleteOverride(overrideId: Long) {
        withContext(ioDispatcher) { scheduleBox.remove(overrideId) }
    }

    override suspend fun deleteOverridesForKid(kidId: Long) {
        withContext(ioDispatcher) {
            scheduleBox
                .query()
                .equal(DailySchedule_.kidId, kidId)
                .build()
                .remove()
        }
    }

    override suspend fun getAll(): List<DailySchedule> =
        withContext(ioDispatcher) {
            scheduleBox
                .query()
                .build()
                .find()
        }

    override fun getAllOverridesFlow(): Flow<List<DailySchedule>> =
        queryFlow {
            scheduleBox
                .query()
                .order(DailySchedule_.date)
                .build()
        }

    override suspend fun clearAll() {
        withContext(ioDispatcher) { scheduleBox.removeAll() }
    }
}
