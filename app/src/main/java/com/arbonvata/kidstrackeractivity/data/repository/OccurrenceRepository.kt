package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.ActivityOccurence
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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
