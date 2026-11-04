package com.arbonvata.kidstrackeractivity.data

import java.time.LocalDate

/** Optional inclusive activity bounds, shared by scheduling, forms and file import/export. */
data class ActivityDateRange(
    val first: LocalDate? = null,
    val last: LocalDate? = null,
) {
    val isValid: Boolean get() = first == null || last == null || !last.isBefore(first)

    fun contains(date: LocalDate): Boolean = isValid && (first == null || !date.isBefore(first)) && (last == null || !date.isAfter(last))
}
