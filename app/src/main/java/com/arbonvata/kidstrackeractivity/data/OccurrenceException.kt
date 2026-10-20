package com.arbonvata.kidstrackeractivity.data

import java.time.LocalDate
import java.time.LocalTime

/** An exception belongs to the original date; null times mean cancelled. */
data class OccurrenceException(
    val date: LocalDate,
    val start: LocalTime? = null,
    val end: LocalTime? = null,
) {
    init {
        require((start == null && end == null) || (start != null && end != null && end.isAfter(start)))
    }
}

/** Compact, version-independent storage without database relations to remap on restore. */
fun encodeExceptions(exceptions: List<OccurrenceException>): String =
    exceptions.sortedBy { it.date }.joinToString("\n") { "${it.date}|${it.start ?: ""}|${it.end ?: ""}" }

fun decodeExceptions(value: String?): List<OccurrenceException> =
    value
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split('|')
            require(parts.size == 3)
            OccurrenceException(
                LocalDate.parse(parts[0]),
                parts[1].takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                parts[2].takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
            )
        }.toList()
        .also { require(it.map { entry -> entry.date }.distinct().size == it.size) }
