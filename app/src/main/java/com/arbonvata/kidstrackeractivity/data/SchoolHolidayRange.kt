package com.arbonvata.kidstrackeractivity.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Inclusive bulk operations are limited to one year to keep edits bounded and reviewable. */
data class SchoolHolidayRange(
    val first: LocalDate,
    val last: LocalDate,
) {
    private val span = ChronoUnit.DAYS.between(first, last)

    init {
        require(span in 0L..365L) { "Choose an inclusive date range of at most 366 days" }
    }

    fun dates(): List<LocalDate> = (0L..span).map { first.plusDays(it) }
}
