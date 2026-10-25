package com.arbonvata.kidstrackeractivity.data

import java.time.LocalDate
import java.time.LocalTime

/**
 * An exception belongs to the original date. Null times mean cancelled —
 * unless [delegatedTo] is set, in which case the date keeps its (possibly
 * changed) times but someone else handles the pickup.
 *
 * [delegatedTo]: null = not delegated; "" = delegated, nobody named;
 * any other value = delegated, picked by that person.
 */
data class OccurrenceException(
    val date: LocalDate,
    val start: LocalTime? = null,
    val end: LocalTime? = null,
    val delegatedTo: String? = null,
) {
    init {
        require((start == null && end == null) || (start != null && end != null && end.isAfter(start)))
        // The name travels inside the pipe-delimited encoding.
        require(delegatedTo == null || ('|' !in delegatedTo && '\n' !in delegatedTo))
    }

    /** Cancelled outright: no occurrence this date, and nobody covers it. */
    val isCancelled: Boolean
        get() = start == null && end == null && delegatedTo == null
}

/** Compact, version-independent storage without database relations to remap on restore. */
fun encodeExceptions(exceptions: List<OccurrenceException>): String =
    exceptions.sortedBy { it.date }.joinToString("\n") { exception ->
        // The fourth field exists only on delegated dates, so non-delegated
        // lines stay byte-identical with the pre-delegation format.
        if (exception.delegatedTo != null) {
            "${exception.date}|${exception.start ?: ""}|${exception.end ?: ""}|${exception.delegatedTo}"
        } else {
            "${exception.date}|${exception.start ?: ""}|${exception.end ?: ""}"
        }
    }

fun decodeExceptions(value: String?): List<OccurrenceException> =
    value
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split('|')
            // Three parts is the pre-delegation format (never delegated);
            // four marks a delegated date, possibly with an empty name.
            require(parts.size == 3 || parts.size == 4)
            OccurrenceException(
                LocalDate.parse(parts[0]),
                parts[1].takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                parts[2].takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                if (parts.size == 4) parts[3] else null,
            )
        }.toList()
        .also { require(it.map { entry -> entry.date }.distinct().size == it.size) }
