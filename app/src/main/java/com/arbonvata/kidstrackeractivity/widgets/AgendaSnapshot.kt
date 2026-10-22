package com.arbonvata.kidstrackeractivity.widgets

import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import java.time.LocalDateTime

/** School's next action is pickup, even when the school day has already begun. */
fun ActivityOccurrence.agendaTime(): LocalDateTime = date.atTime(if (activity.isSchoolActivity) endTime else startTime)

data class AgendaSnapshot(
    val next: ActivityOccurrence?,
    val remainingToday: List<ActivityOccurrence>,
)

fun agendaSnapshot(
    occurrences: List<ActivityOccurrence>,
    now: LocalDateTime,
): AgendaSnapshot =
    AgendaSnapshot(
        next = occurrences.filter { !it.agendaTime().isBefore(now) }.minByOrNull { it.agendaTime() },
        remainingToday =
            occurrences
                .filter {
                    it.date == now.toLocalDate() &&
                        it.endTime.isAfter(
                            now.toLocalTime(),
                        )
                }.sortedBy { it.agendaTime() },
    )
