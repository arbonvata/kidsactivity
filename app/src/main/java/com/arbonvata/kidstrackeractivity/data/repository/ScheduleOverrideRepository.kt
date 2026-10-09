package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule_
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import java.time.LocalDate

interface ScheduleOverrideRepository {
    /**
     * Creates or updates the override for one kid on one date. An override
     * without an end time and without the no-school flag carries no meaning
     * and is removed instead of stored.
     */
    fun setOverride(
        kidId: Long,
        date: LocalDate,
        endTime: String?,
        isNoSchool: Boolean,
    )

    fun getOverride(
        kidId: Long,
        date: LocalDate,
    ): DailySchedule?

    /**
     * ObjectBox has no range comparison for strings, so dates are filtered in
     * memory after querying by kid. ISO dates sort lexicographically.
     */
    fun getOverridesBetween(
        kidId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<DailySchedule>

    fun deleteOverride(overrideId: Long)

    fun deleteOverridesForKid(kidId: Long)

    fun getAll(): List<DailySchedule>

    /**
     * The school end time that applies on the given date (spec 8.3): the
     * override if present, otherwise the kid's default. Null means no school
     * that day, so no alarm should be scheduled at all.
     */
    fun getEffectiveEndTime(
        kid: Kid,
        date: LocalDate,
    ): String?

    fun clearAll()
}

class ScheduleOverrideRepositoryImpl(
    store: BoxStore,
) : ScheduleOverrideRepository {
    private val scheduleBox = store.boxFor(DailySchedule::class.java)

    override fun setOverride(
        kidId: Long,
        date: LocalDate,
        endTime: String?,
        isNoSchool: Boolean,
    ) {
        val existing = getOverride(kidId, date)
        if (endTime == null && !isNoSchool) {
            existing?.let { deleteOverride(it.id) }
            return
        }

        val schedule = existing ?: DailySchedule(kidId = kidId, date = date.toString())
        schedule.endTime = endTime ?: ""
        schedule.isNoSchool = isNoSchool
        scheduleBox.put(schedule)
    }

    override fun getOverride(
        kidId: Long,
        date: LocalDate,
    ): DailySchedule? =
        scheduleBox
            .query()
            .equal(DailySchedule_.kidId, kidId)
            .equal(DailySchedule_.date, date.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()

    override fun getOverridesBetween(
        kidId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<DailySchedule> =
        scheduleBox
            .query()
            .equal(DailySchedule_.kidId, kidId)
            .build()
            .find()
            .filter { it.date >= start.toString() && it.date <= end.toString() }

    override fun deleteOverride(overrideId: Long) {
        scheduleBox.remove(overrideId)
    }

    override fun deleteOverridesForKid(kidId: Long) {
        scheduleBox
            .query()
            .equal(DailySchedule_.kidId, kidId)
            .build()
            .remove()
    }

    override fun getAll(): List<DailySchedule> =
        scheduleBox
            .query()
            .build()
            .find()

    override fun getEffectiveEndTime(
        kid: Kid,
        date: LocalDate,
    ): String? {
        val override = getOverride(kid.id, date) ?: return kid.defaultEndTime
        if (override.isNoSchool) return null
        return override.endTime.ifEmpty { kid.defaultEndTime }
    }

    override fun clearAll() {
        scheduleBox.removeAll()
    }
}
