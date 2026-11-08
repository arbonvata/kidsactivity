package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.encodeExceptions
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import java.time.format.DateTimeFormatter

internal fun AddActivityUiState.toActivity(original: Activity? = null): Activity =
    Activity(
        id = original?.id ?: 0L,
        kidId = original?.kidId ?: selectedKidId,
        name = name.trim(),
        location = location.trim(),
        pickupAdult = pickupAdult.trim(),
        pickupNotes = pickupNotes.trim(),
        hasCustomReminder = hasCustomReminder,
        reminderMinutes = reminderMinutes,
        hasPickupReminder = hasPickupReminder && !isSchoolActivity,
        pickupLeadMinutes = pickupLeadMinutes,
        startTime = startTime.format(AddActivityViewModel.TIME_FORMATTER),
        endTime = endTime.format(AddActivityViewModel.TIME_FORMATTER),
        isEnabled = original?.isEnabled ?: true,
        daysOfWeek = selectedDays.sorted().toIntArray(),
        isPeriodic = isPeriodic,
        isSchoolActivity = isSchoolActivity,
        firstDate = firstDate.format(DateTimeFormatter.ISO_DATE),
        lastDate = lastDate?.format(DateTimeFormatter.ISO_DATE),
        calendarStartDate = calendarStartDate?.takeIf { original != null || isPeriodic || isSchoolActivity }?.toString(),
        occurrenceExceptions = original?.let { encodeExceptions(exceptions) },
    )
