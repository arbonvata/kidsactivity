package com.arbonvata.kidstrackeractivity.data.calendar

import com.arbonvata.kidstrackeractivity.data.OccurrenceException
import com.arbonvata.kidstrackeractivity.data.decodeExceptions
import com.arbonvata.kidstrackeractivity.data.encodeExceptions
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CalendarCodecTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val zone = ZoneId.of("Europe/Stockholm")
    private val kid = Kid(id = 1, name = "Åsa, junior")
    private val swim =
        Activity(
            id = 4,
            kidId = 1,
            name = "Swimming, fun; games",
            startTime = "17:00",
            endTime = "18:00",
            daysOfWeek = intArrayOf(1, 3),
            location = "Pool\\gate",
            pickupAdult = "Alex",
            pickupNotes = "First line\nSecond line",
        )

    @Test
    fun `weekly round trip preserves details exceptions and future recurrence`() {
        swim.isEnabled = false
        swim.occurrenceExceptions =
            encodeExceptions(
                listOf(
                    OccurrenceException(monday),
                    OccurrenceException(monday.plusDays(2), LocalTime.of(18, 0), LocalTime.of(19, 0)),
                ),
            )
        val text = CalendarCodec.export(kid, listOf(swim), emptyList(), monday)
        assertTrue(text.contains("RRULE:FREQ=WEEKLY;BYDAY=MO,WE"))
        val parsed = CalendarCodec.parse(text, zone)
        val imported = parsed.activities.single().copy(id = 4, kidId = 1)
        assertEquals(kid.name, parsed.childName)
        assertEquals(swim.name, imported.name)
        assertEquals(swim.location, imported.location)
        assertEquals(swim.pickupAdult, imported.pickupAdult)
        assertEquals(swim.pickupNotes, imported.pickupNotes)
        assertFalse(imported.isEnabled)
        assertEquals(decodeExceptions(swim.occurrenceExceptions), decodeExceptions(imported.occurrenceExceptions))
        val actual = expandActivitiesForDateRange(listOf(kid), listOf(imported), monday, monday.plusWeeks(3), includeDisabled = true)
        val expected = expandActivitiesForDateRange(listOf(kid), listOf(swim), monday, monday.plusWeeks(3), includeDisabled = true)
        assertEquals(expected.map { Triple(it.date, it.startTime, it.endTime) }, actual.map { Triple(it.date, it.startTime, it.endTime) })
    }

    @Test
    fun `school overrides transfer as activity date exceptions and no school wins`() {
        val school =
            swim.copy(
                isSchoolActivity = true,
                startTime = "08:00",
                endTime = "14:00",
                occurrenceExceptions = encodeExceptions(listOf(OccurrenceException(monday, LocalTime.of(9, 0), LocalTime.of(12, 0)))),
            )
        val overrides =
            listOf(
                DailySchedule(kidId = 1, date = monday.toString(), isNoSchool = true),
                DailySchedule(kidId = 1, date = monday.plusDays(2).toString(), endTime = "13:00"),
            )
        val imported = CalendarCodec.parse(CalendarCodec.export(kid, listOf(school), overrides, monday), zone).activities.single()
        assertTrue(imported.isSchoolActivity)
        val exceptions = decodeExceptions(imported.occurrenceExceptions)
        assertEquals(OccurrenceException(monday), exceptions.first())
        assertEquals(LocalTime.of(13, 0), exceptions.last().end)
    }

    @Test
    fun `one-offs on multiple weekdays stay on their dates`() {
        val oneOff = swim.copy(isPeriodic = false, firstDate = monday.toString())
        val parsed = CalendarCodec.parse(CalendarCodec.export(kid, listOf(oneOff), emptyList(), monday), zone)
        assertEquals(2, parsed.activities.size)
        assertTrue(parsed.activities.none { it.isPeriodic })
        val imported = parsed.activities.mapIndexed { index, it -> it.copy(id = index + 1L, kidId = 1) }
        val result = expandActivitiesForDateRange(listOf(kid), imported, monday, monday.plusWeeks(3))
        assertEquals(listOf(monday, monday.plusDays(2)), result.map { it.date })
    }

    @Test
    fun `folding respects utf8 octets and restores escaped text`() {
        val name = "🏊Å;夏,\\\n".repeat(30)
        val text = CalendarCodec.export(kid, listOf(swim.copy(name = name)), emptyList(), monday)
        assertTrue(text.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertEquals(
            name,
            CalendarCodec
                .parse(text, zone)
                .activities
                .single()
                .name,
        )
    }

    @Test
    fun `weekly DTSTART prevents imported events appearing before their start`() {
        val parsed = CalendarCodec.parse(calendar("RRULE:FREQ=WEEKLY;BYDAY=MO"), zone)
        val imported = parsed.activities.single().copy(kidId = 1)
        val result = expandActivitiesForDateRange(listOf(kid), listOf(imported), monday.minusWeeks(2), monday.plusWeeks(1))
        assertEquals(listOf(monday, monday.plusWeeks(1)), result.map { it.date })
    }

    @Test
    fun `bounded weekly recurrence expands to dated events without repeating forever`() {
        val parsed = CalendarCodec.parse(calendar("RRULE:FREQ=WEEKLY;COUNT=3\nEXDATE:20261026T170000"), zone)
        assertEquals(2, parsed.activities.size)
        assertEquals(listOf("2026-10-19", "2026-11-02"), parsed.activities.map { it.firstDate })
        assertTrue(parsed.activities.none { it.isPeriodic })
        val until = CalendarCodec.parse(calendar("RRULE:FREQ=WEEKLY;UNTIL=20261026T170000"), zone)
        assertEquals(2, until.activities.size)
    }

    @Test
    fun `UTC and named timezone one-offs convert to device time`() {
        val utc = calendar().replace("20261019T170000", "20261019T150000Z").replace("20261019T180000", "20261019T160000Z")
        assertEquals(
            "17:00",
            CalendarCodec
                .parse(utc, zone)
                .activities
                .single()
                .startTime,
        )
        val named = calendar().replace("DTSTART:", "DTSTART;TZID=Europe/London:").replace("DTEND:", "DTEND;TZID=Europe/London:")
        assertEquals(
            "18:00",
            CalendarCodec
                .parse(named, zone)
                .activities
                .single()
                .startTime,
        )
    }

    @Test
    fun `nested alarms are ignored rather than mistaken for activity fields`() {
        val text = calendar("BEGIN:VALARM\nACTION:DISPLAY\nDESCRIPTION:Alarm description\nTRIGGER:-PT15M\nEND:VALARM")
        assertEquals(
            "Swim",
            CalendarCodec
                .parse(text, zone)
                .activities
                .single()
                .name,
        )
    }

    @Test
    fun `unsupported or malformed calendars fail instead of partially importing`() {
        listOf(
            calendar("RRULE:FREQ=MONTHLY"),
            calendar("RRULE:FREQ=WEEKLY;INTERVAL=2"),
            calendar("RRULE:FREQ=WEEKLY;BYDAY=1MO"),
            calendar("RDATE:20261020T170000"),
            calendar().replace("20261019T170000", "20261019"),
            calendar().replace("20261019T180000", "20261020T180000"),
            calendar().replace("END:VCALENDAR", ""),
            calendar() + calendar(),
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { CalendarCodec.parse(invalid, zone) } }
        assertThrows(IllegalArgumentException::class.java) { CalendarCodec.parse("x".repeat(CalendarCodec.MAX_BYTES + 1), zone) }
    }

    @Test
    fun `repeated exports retain weekly event identity and only include the chosen child`() {
        val first = CalendarCodec.export(kid, listOf(swim, swim.copy(id = 9, kidId = 2)), emptyList(), monday, namespace = "device-a")
        val second = CalendarCodec.export(kid, listOf(swim), emptyList(), monday.plusWeeks(1), namespace = "device-a")
        assertEquals(first.lineSequence().single { it.startsWith("UID:") }, second.lineSequence().single { it.startsWith("UID:") })
        assertEquals(1, CalendarCodec.parse(first, zone).activities.size)
    }

    @Test
    fun `imported school reminder uses changed pickup time and skips no-school dates`() {
        val school = swim.copy(isSchoolActivity = true, startTime = "08:00", endTime = "14:00")
        val overrides =
            listOf(
                DailySchedule(kidId = 1, date = monday.toString(), isNoSchool = true),
                DailySchedule(kidId = 1, date = monday.plusDays(2).toString(), endTime = "13:00"),
            )
        val imported =
            CalendarCodec
                .parse(CalendarCodec.export(kid, listOf(school), overrides, monday), zone)
                .activities
                .single()
                .copy(kidId = 1, id = 4)
        val alarms =
            com.arbonvata.kidstrackeractivity.alarms.computeUpcomingAlarms(
                listOf(kid),
                listOf(imported),
                10,
                15,
                monday.atStartOfDay(),
            )
        assertEquals(listOf(monday.plusDays(2).atTime(13, 10)), alarms.map { it.triggerAt })
        assertTrue(alarms.single().isSchoolActivity)
    }

    @Test
    fun `invalid dates cannot be normalized silently and recurrence expansion is bounded`() {
        val invalidDate = calendar().replace("20261019", "20260230")
        assertThrows(java.time.DateTimeException::class.java) { CalendarCodec.parse(invalidDate, zone) }
        val many =
            calendar("RRULE:FREQ=WEEKLY;COUNT=2000")
                .replace("END:VCALENDAR", "") +
                calendar("RRULE:FREQ=WEEKLY;COUNT=2000")
                    .replace("BEGIN:VCALENDAR\nVERSION:2.0\n", "")
                    .replace("UID:test@example.com", "UID:second@example.com")
        assertThrows(IllegalArgumentException::class.java) { CalendarCodec.parse(many, zone) }
        val until = CalendarCodec.parse(calendar("RRULE:FREQ=WEEKLY;UNTIL=20261026T235959Z"), zone)
        assertEquals(2, until.activities.size)
    }

    private fun calendar(extra: String = "") =
        listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:test@example.com",
            "DTSTART:20261019T170000",
            "DTEND:20261019T180000",
            "SUMMARY:Swim",
            extra,
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\n")
}
