package com.arbonvata.kidstrackeractivity.presentation

import java.time.format.DateTimeFormatter

/*
 * Shared, immutable formatters for the schedule UI (DateTimeFormatter is
 * thread-safe). Screen-specific patterns that appear once stay local to
 * their screen; only repeated ones live here.
 */

/** e.g. "17:30" — the time column in schedule rows and pickers. */
val shortTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** e.g. "Mon, Oct 26" — the day column in Week rows and conflict cards. */
val weekdayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
