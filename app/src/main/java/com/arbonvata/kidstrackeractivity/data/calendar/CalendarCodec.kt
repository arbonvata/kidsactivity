package com.arbonvata.kidstrackeractivity.data.calendar

import com.arbonvata.kidstrackeractivity.data.OccurrenceException
import com.arbonvata.kidstrackeractivity.data.decodeExceptions
import com.arbonvata.kidstrackeractivity.data.encodeExceptions
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/** A deliberately strict timed-event subset of RFC 5545; unsupported rules never silently become weekly schedules. */
data class CalendarImport(
    val childName: String,
    val activities: List<Activity>,
)

object CalendarCodec {
    const val MAX_BYTES = 2_000_000
    private val dateTime = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss").withResolverStyle(java.time.format.ResolverStyle.STRICT)
    private val dayCodes = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

    fun export(
        kid: Kid,
        activities: List<Activity>,
        overrides: List<DailySchedule>,
        today: LocalDate = LocalDate.now(),
        now: Instant = Instant.now(),
        namespace: String = UUID.randomUUID().toString(),
    ): String {
        val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Kids Tracker Activity//Calendar//EN", "CALSCALE:GREGORIAN")
        lines += "X-WR-CALNAME:${escape(kid.name)}"
        lines += "X-KIDSTRACKER-CHILD:${escape(kid.name)}"
        val stamp = dateTime.format(now.atOffset(ZoneOffset.UTC)) + "Z"
        activities.filter { it.kidId == kid.id }.forEach { activity ->
            require(activity.daysOfWeek.isNotEmpty() && activity.daysOfWeek.all { it in 1..7 }) { "Activity has invalid weekdays" }
            val start = LocalTime.parse(activity.startTime)
            val end = LocalTime.parse(activity.endTime)
            require(end.isAfter(start)) { "Activity must end after it starts" }
            val weekly = activity.isPeriodic || activity.isSchoolActivity
            val anchor =
                if (weekly) {
                    maxOf(today, activity.calendarStartDate?.let(LocalDate::parse) ?: today)
                } else {
                    LocalDate.parse(requireNotNull(activity.firstDate))
                }
            val dates = (0L..6L).map { anchor.plusDays(it) }.filter { it.dayOfWeek.value in activity.daysOfWeek }
            val exceptions = linkedMapOf<LocalDate, OccurrenceException>()
            if (activity.isSchoolActivity) {
                overrides.filter { it.kidId == kid.id }.forEach {
                    val date = LocalDate.parse(it.date)
                    if (it.isNoSchool) {
                        exceptions[date] = OccurrenceException(date)
                    } else if (it.endTime.isNotBlank()) {
                        exceptions[date] = OccurrenceException(date, start, LocalTime.parse(it.endTime))
                    }
                }
            }
            // No-school wins over an activity exception, matching occurrence expansion.
            decodeExceptions(activity.occurrenceExceptions).forEach {
                if (exceptions[it.date]?.start != null || !exceptions.containsKey(it.date)) exceptions[it.date] = it
            }
            val bases = if (weekly) dates.take(1) else dates
            bases.forEach { date ->
                val uid = "$namespace-${activity.id}-${if (weekly) "weekly" else date}@kidstracker"

                fun details() {
                    lines += "SUMMARY:${escape(activity.name)}"
                    lines += "LOCATION:${escape(activity.location.orEmpty())}"
                    val description =
                        listOfNotNull(
                            activity.pickupAdult?.takeIf { it.isNotBlank() }?.let { "Pickup: $it" },
                            activity.pickupNotes?.takeIf { it.isNotBlank() },
                        ).joinToString("\n")
                    lines += "DESCRIPTION:${escape(description)}"
                    lines += "X-KIDSTRACKER-PICKUP:${escape(activity.pickupAdult.orEmpty())}"
                    lines += "X-KIDSTRACKER-NOTES:${escape(activity.pickupNotes.orEmpty())}"
                    lines += "X-KIDSTRACKER-SCHOOL:${activity.isSchoolActivity}"
                    lines += "X-KIDSTRACKER-ENABLED:${activity.isEnabled}"
                }
                lines += listOf("BEGIN:VEVENT", "UID:$uid", "DTSTAMP:$stamp")
                lines += "DTSTART:${date.atTime(start).format(dateTime)}"
                lines += "DTEND:${date.atTime(end).format(dateTime)}"
                details()
                if (weekly) {
                    val weekdays = activity.daysOfWeek.sorted().joinToString(",") { dayCodes[it - 1] }
                    lines += "RRULE:FREQ=WEEKLY;BYDAY=$weekdays"
                }
                val relevant =
                    exceptions.values.filter {
                        !it.date.isBefore(date) && (weekly || it.date == date) && it.date.dayOfWeek.value in activity.daysOfWeek
                    }
                relevant.filter { it.start == null }.forEach { lines += "EXDATE:${it.date.atTime(start).format(dateTime)}" }
                lines += "END:VEVENT"
                relevant.filter { it.start != null }.forEach {
                    lines += listOf("BEGIN:VEVENT", "UID:$uid", "DTSTAMP:$stamp")
                    lines += "RECURRENCE-ID:${it.date.atTime(start).format(dateTime)}"
                    lines += "DTSTART:${it.date.atTime(it.start).format(dateTime)}"
                    lines += "DTEND:${it.date.atTime(it.end).format(dateTime)}"
                    details()
                    lines += "END:VEVENT"
                }
            }
        }
        lines += "END:VCALENDAR"
        return lines.joinToString("\r\n", postfix = "\r\n") { fold(it) }
    }

    private data class Property(
        val name: String,
        val parameters: Map<String, String>,
        val value: String,
    )

    private data class Event(
        val properties: List<Property>,
    ) {
        fun all(name: String) = properties.filter { it.name == name }

        fun one(name: String): Property? = all(name).also { require(it.size <= 1) { "Duplicate $name property" } }.firstOrNull()

        fun text(name: String) = one(name)?.value?.let(::unescape).orEmpty()
    }

    fun parse(
        text: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CalendarImport {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Calendar file is too large (maximum 2 MB)" }
        val unfolded =
            text
                .removePrefix("\uFEFF")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace(Regex("\n[ \t]"), "")
        val events = mutableListOf<Event>()
        val stack = mutableListOf<String>()
        var eventProperties: MutableList<Property>? = null
        var childName = ""
        var calendars = 0
        unfolded.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val colon = line.indexOf(':')
            require(colon > 0) { "Invalid calendar property" }
            val header = line.substring(0, colon).split(';')
            val name = header.first().uppercase()
            val value = line.substring(colon + 1)
            when (name) {
                "BEGIN" -> {
                    val component = value.uppercase()
                    if (stack.isEmpty()) {
                        require(component == "VCALENDAR" && calendars++ == 0) { "Expected one calendar" }
                    }
                    if (component == "VEVENT") {
                        require(stack.lastOrNull() == "VCALENDAR") { "Invalid event nesting" }
                        eventProperties = mutableListOf()
                    }
                    stack += component
                }
                "END" -> {
                    require(stack.lastOrNull() == value.uppercase()) { "Incomplete calendar" }
                    if (stack.removeAt(stack.lastIndex) == "VEVENT") {
                        events += Event(requireNotNull(eventProperties))
                        require(events.size <= 2000) { "Calendar contains too many events" }
                        eventProperties = null
                    }
                }
                else -> {
                    require(stack.isNotEmpty()) { "Property outside calendar" }
                    val parameters =
                        header.drop(1).associate {
                            val pair = it.split('=', limit = 2)
                            require(pair.size == 2) { "Invalid calendar parameter" }
                            pair[0].uppercase() to pair[1].trim('"')
                        }
                    val property = Property(name, parameters, value)
                    if (stack.last() == "VEVENT") eventProperties?.add(property)
                    if (stack.last() == "VCALENDAR" && name == "X-KIDSTRACKER-CHILD") childName = unescape(value)
                }
            }
        }
        require(calendars == 1 && stack.isEmpty() && events.isNotEmpty()) { "No complete calendar events found" }
        val grouped = events.groupBy { it.one("UID")?.value ?: error("An event is missing its UID") }
        val activities = mutableListOf<Activity>()
        grouped.values.forEach { group ->
            activities += parseGroup(group, zone)
            require(activities.size <= 2000) { "Calendar expands to too many activities" }
        }
        require(activities.isNotEmpty()) { "Calendar has no active timed events" }
        return CalendarImport(childName, activities)
    }

    private fun parseGroup(
        group: List<Event>,
        zone: ZoneId,
    ): List<Activity> {
        val bases = group.filter { it.one("RECURRENCE-ID") == null }
        require(bases.size == 1) { "Missing or duplicate calendar event" }
        val base = bases.single()
        if (base.text("STATUS").uppercase() == "CANCELLED") return emptyList()
        require(base.one("RDATE") == null && base.one("EXRULE") == null) { "RDATE and EXRULE calendars are not supported" }
        val start = readTime(requireNotNull(base.one("DTSTART")) { "Event is missing its start time" }, zone)
        val end =
            base.one("DTEND")?.let { readTime(it, zone) }
                ?: base.one("DURATION")?.let { start.plus(java.time.Duration.parse(it.value)) }
                ?: error("Event is missing its end time")
        requireSameDay(start, end)
        val exceptions = linkedMapOf<LocalDate, OccurrenceException>()
        base.all("EXDATE").forEach { property ->
            property.value.split(',').forEach {
                val date = readTime(property.copy(value = it), zone).toLocalDate()
                exceptions[date] = OccurrenceException(date)
            }
        }
        group.filter { it.one("RECURRENCE-ID") != null }.forEach { event ->
            val recurrence = requireNotNull(event.one("RECURRENCE-ID"))
            require(recurrence.parameters["RANGE"] == null) { "This-and-future changes are not supported" }
            val original = readTime(recurrence, zone)
            val date = original.toLocalDate()
            require(!exceptions.containsKey(date)) { "Conflicting changes for a calendar date" }
            exceptions[date] =
                if (event.text("STATUS").uppercase() == "CANCELLED") {
                    OccurrenceException(date)
                } else {
                    val movedStart = readTime(requireNotNull(event.one("DTSTART")), zone)
                    val movedEnd = readTime(requireNotNull(event.one("DTEND")), zone)
                    requireSameDay(movedStart, movedEnd)
                    require(movedStart.toLocalDate() == date) { "Moving an event to another date is not supported" }
                    OccurrenceException(date, movedStart.toLocalTime(), movedEnd.toLocalTime())
                }
        }
        val rule =
            base.one("RRULE")?.value?.split(';')?.associate {
                val parts = it.split('=', limit = 2)
                require(parts.size == 2) { "Invalid recurrence rule" }
                parts[0].uppercase() to parts[1].uppercase()
            }
        if (rule != null) {
            require(
                rule["FREQ"] == "WEEKLY" &&
                    (rule["INTERVAL"] ?: "1") == "1" &&
                    rule.keys.all { it in setOf("FREQ", "INTERVAL", "BYDAY", "WKST", "UNTIL", "COUNT") },
            ) {
                "Only weekly recurrence with interval 1 is supported"
            }
            require(!(rule.containsKey("COUNT") && rule.containsKey("UNTIL"))) { "A rule cannot have both COUNT and UNTIL" }
            val sourceZone = base.one("DTSTART")?.parameters?.get("TZID")
            require((sourceZone == null && !requireNotNull(base.one("DTSTART")).value.endsWith('Z')) || sourceZone == zone.id) {
                "Recurring events must use local time or this device's time zone"
            }
        }
        val days =
            rule
                ?.get("BYDAY")
                ?.split(',')
                ?.map {
                    dayCodes.indexOf(it).also { index -> require(index >= 0) { "Unsupported weekly weekday" } } + 1
                }?.distinct() ?: listOf(start.dayOfWeek.value)
        require(start.dayOfWeek.value in days) { "Weekly start date must match one of its weekdays" }
        val activity =
            Activity(
                name = base.text("SUMMARY").ifBlank { "Imported activity" },
                startTime = start.toLocalTime().toString(),
                endTime = end.toLocalTime().toString(),
                daysOfWeek = days.toIntArray(),
                isPeriodic = rule != null,
                firstDate = start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString(),
                calendarStartDate = if (rule == null) null else start.toLocalDate().toString(),
                isSchoolActivity = rule != null && base.text("X-KIDSTRACKER-SCHOOL").equals("true", true),
                isEnabled = !base.text("X-KIDSTRACKER-ENABLED").equals("false", true),
                location = base.text("LOCATION"),
                pickupAdult = base.text("X-KIDSTRACKER-PICKUP"),
                pickupNotes = base.one("X-KIDSTRACKER-NOTES")?.let { unescape(it.value) } ?: base.text("DESCRIPTION"),
                occurrenceExceptions = encodeExceptions(exceptions.values.toList()),
            )
        if (rule == null) {
            val exception = exceptions[start.toLocalDate()]
            if (exception != null && exception.start == null) return emptyList()
            return listOf(
                activity.copy(
                    startTime = (exception?.start ?: start.toLocalTime()).toString(),
                    endTime = (exception?.end ?: end.toLocalTime()).toString(),
                    occurrenceExceptions = null,
                ),
            )
        }
        val count = rule["COUNT"]?.toInt()?.also { require(it in 1..2000) { "Recurrence count must be between 1 and 2000" } }
        val until =
            rule["UNTIL"]?.let {
                if (it.length == 8) {
                    LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE).atTime(LocalTime.MAX)
                } else {
                    readTime(Property("UNTIL", emptyMap(), it), zone)
                }
            }
        if (count == null && until == null) return listOf(activity)
        val result = mutableListOf<Activity>()
        var date = start.toLocalDate()
        var seen = 0
        var scanned = 0
        while ((count == null || seen < count) && (until == null || !date.atTime(start.toLocalTime()).isAfter(until))) {
            require(scanned++ < 15000) { "Recurrence spans too many years" }
            if (date.dayOfWeek.value in days) {
                seen++
                val exception = exceptions[date]
                if (exception == null || exception.start != null) {
                    result +=
                        activity.copy(
                            isPeriodic = false,
                            isSchoolActivity = false,
                            calendarStartDate = null,
                            daysOfWeek = intArrayOf(date.dayOfWeek.value),
                            firstDate = date.toString(),
                            occurrenceExceptions = null,
                            startTime = (exception?.start ?: start.toLocalTime()).toString(),
                            endTime = (exception?.end ?: end.toLocalTime()).toString(),
                        )
                    require(result.size <= 2000) { "Calendar expands to too many activities" }
                }
            }
            date = date.plusDays(1)
        }
        return result
    }

    private fun readTime(
        property: Property,
        zone: ZoneId,
    ): LocalDateTime {
        require(
            property.value.length != 8 && property.parameters["VALUE"] != "DATE",
        ) { "All-day events are not supported; use timed activities" }
        val utc = property.value.endsWith('Z')
        val local = LocalDateTime.parse(property.value.removeSuffix("Z"), dateTime)
        require(property.name == "UNTIL" || local.second == 0) { "Activity times must use whole minutes" }
        val source = if (utc) ZoneOffset.UTC else property.parameters["TZID"]?.let(ZoneId::of)
        return if (source == null) local else local.atZone(source).withZoneSameInstant(zone).toLocalDateTime()
    }

    private fun requireSameDay(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        require(start.second == 0 && end.second == 0) { "Activity times must use whole minutes" }
        require(start.toLocalDate() == end.toLocalDate() && end.isAfter(start)) {
            "Activities must start and end on the same day, with end after start"
        }
    }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")

    private fun unescape(value: String): String {
        val result = StringBuilder()
        var escaped = false
        value.forEach {
            if (escaped) {
                result.append(if (it == 'n' || it == 'N') '\n' else it)
                escaped = false
            } else if (it == '\\') {
                escaped = true
            } else {
                result.append(it)
            }
        }
        require(!escaped) { "Invalid text escape" }
        return result.toString()
    }

    /** Fold on UTF-8 character boundaries; continuation whitespace counts toward the 75-octet limit. */
    private fun fold(value: String): String {
        val result = StringBuilder()
        var bytes = 0
        value.codePoints().forEach { point ->
            val character = String(Character.toChars(point))
            val size = character.toByteArray(Charsets.UTF_8).size
            if (bytes + size > 75) {
                result.append("\r\n ")
                bytes = 1
            }
            result.append(character)
            bytes += size
        }
        return result.toString()
    }
}
