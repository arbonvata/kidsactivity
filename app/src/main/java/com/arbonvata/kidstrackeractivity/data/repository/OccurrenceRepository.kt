package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/** Two activities of the same kid that overlap on the same date (spec 8.2). */
data class Conflict(
    val kidId: Long,
    val kidName: String,
    val date: LocalDate,
    val first: Activity,
    val second: Activity,
    val overlapMinutes: Long,
)

/**
 * Pure composition over [KidRepository] and [ActivityRepository]; the data
 * access underneath is dispatched to IO by those repositories, so this class
 * adds no dispatcher of its own.
 */
interface OccurrenceRepository {
    suspend fun getOccurrencesForDate(date: LocalDate): List<ActivityOccurence>

    suspend fun getOccurrencesForWeek(weekStart: LocalDate): List<ActivityOccurence>

    suspend fun getOccurrencesForMonth(month: YearMonth): List<ActivityOccurence>

    /**
     * The next occurrence that starts strictly after [after]. Activities repeat
     * weekly, so scanning eight consecutive days is guaranteed to encounter
     * every enabled activity at least once.
     */
    suspend fun getNextUpcomingOccurrence(after: LocalDateTime): ActivityOccurence?

    /**
     * Detects overlapping activity pairs per kid within the next [daysAhead]
     * days (spec 8.2). With activities sorted by start time, any non-adjacent
     * overlap implies an adjacent one, so comparing neighbours is sufficient.
     * Activities ending at or before their start time (overnight ranges are
     * not representable in the schema) are ignored.
     */
    suspend fun findConflicts(
        daysAhead: Int = 30,
        from: LocalDate = LocalDate.now(),
    ): List<Conflict>
}

class OccurrenceRepositoryImpl(
    private val kidRepository: KidRepository,
    private val activityRepository: ActivityRepository,
) : OccurrenceRepository {
    override suspend fun getOccurrencesForDate(date: LocalDate): List<ActivityOccurence> = expand(date, date)

    override suspend fun getOccurrencesForWeek(weekStart: LocalDate) = expand(weekStart, weekStart.plusDays(6))

    override suspend fun getOccurrencesForMonth(month: YearMonth) = expand(month.atDay(1), month.atEndOfMonth())

    override suspend fun getNextUpcomingOccurrence(after: LocalDateTime): ActivityOccurence? {
        var date = after.toLocalDate()
        repeat(8) {
            getOccurrencesForDate(date)
                .firstOrNull { occurrence -> occurrence.date.atTime(occurrence.startTime).isAfter(after) }
                ?.let { return it }
            date = date.plusDays(1)
        }
        return null
    }

    override suspend fun findConflicts(
        daysAhead: Int,
        from: LocalDate,
    ): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()
        val endDate = from.plusDays(daysAhead.toLong())

        for (kid in kidRepository.getAllKids()) {
            val activities =
                activityRepository
                    .getActivitiesForKid(kid.id)
                    .filter { it.isEnabled && it.startTime < it.endTime }
            if (activities.size < 2) continue

            var date = from
            while (!date.isAfter(endDate)) {
                val sameDay =
                    activities
                        .filter { it.daysOfWeek.contains(date.dayOfWeek.value) }
                        .sortedBy { it.startTime }
                for (i in 0 until sameDay.size - 1) {
                    val overlap = overlapMinutes(sameDay[i], sameDay[i + 1])
                    if (overlap > 0) {
                        conflicts.add(
                            Conflict(
                                kidId = kid.id,
                                kidName = kid.name,
                                date = date,
                                first = sameDay[i],
                                second = sameDay[i + 1],
                                overlapMinutes = overlap,
                            ),
                        )
                    }
                }
                date = date.plusDays(1)
            }
        }
        return conflicts
    }

    private fun overlapMinutes(
        first: Activity,
        second: Activity,
    ): Long =
        Duration
            .between(LocalTime.parse(second.startTime), LocalTime.parse(first.endTime))
            .toMinutes()
            .coerceAtLeast(0)

    private suspend fun expand(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ActivityOccurence> =
        expandActivitiesForDateRange(
            kids = kidRepository.getAllKids(),
            activities = activityRepository.getAllActivities(),
            startDate = startDate,
            endDate = endDate,
        )
}
