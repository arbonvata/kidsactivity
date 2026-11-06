package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.SchoolHolidayRange
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class SchoolOverridesUiState(
    val isLoading: Boolean = true,
    val kids: List<Kid> = emptyList(),
    val selectedKidId: Long? = null,
    /**
     * ISO day-of-week numbers (1 = Mon … 7 = Sun) the selected kid has
     * ENABLED school activities on. Overrides marked on other weekdays are
     * still allowed (pre-marking vacations) but currently have no effect.
     */
    val schoolWeekdays: Set<Int> = emptySet(),
    val overrides: Map<LocalDate, DailySchedule> = emptyMap(),
)

data class HolidayRangeState(
    val isWorking: Boolean = false,
    val error: Boolean = false,
    val completed: Boolean = false,
)

/**
 * School-day overrides per kid: "No School" days and custom school end
 * times, shown on a month calendar. Everything is reactive — saving an
 * override instantly updates the schedule tabs and the alarm planner,
 * because they observe the same overrides flow.
 */
class SchoolOverridesViewModel(
    private val kidRepository: KidRepository,
    private val activityRepository: ActivityRepository,
    private val scheduleOverrideRepository: ScheduleOverrideRepository,
) : ViewModel() {
    private val selectedKidId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SchoolOverridesUiState> =
        combine(
            kidRepository.getAllKidsFlow(),
            activityRepository.getAllActivitiesFlow(),
            scheduleOverrideRepository.getAllOverridesFlow(),
            selectedKidId,
        ) { kids, activities, overrides, selectedId ->
            val effectiveId = selectedId ?: kids.firstOrNull()?.id
            val schoolWeekdays =
                if (effectiveId == null) {
                    emptySet()
                } else {
                    activities
                        .filter { it.kidId == effectiveId && it.isSchoolActivity && it.isEnabled }
                        .flatMap { it.daysOfWeek.toList() }
                        .toSet()
                }
            SchoolOverridesUiState(
                isLoading = false,
                kids = kids,
                selectedKidId = effectiveId,
                schoolWeekdays = schoolWeekdays,
                overrides =
                    if (effectiveId == null) {
                        emptyMap()
                    } else {
                        // A corrupt stored date (e.g. a hand-edited backup)
                        // is skipped rather than crashing the screen.
                        overrides
                            .filter { it.kidId == effectiveId }
                            .mapNotNull { override ->
                                runCatching { LocalDate.parse(override.date) }.getOrNull()?.let { it to override }
                            }.toMap()
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolOverridesUiState())

    private val _holidayState = MutableStateFlow(HolidayRangeState())
    val holidayState = _holidayState.asStateFlow()

    fun setHolidayRange(
        start: LocalDate,
        end: LocalDate,
        allKids: Boolean,
        noSchool: Boolean,
    ) {
        if (_holidayState.value.isWorking) return
        val current = uiState.value
        val ids = if (allKids) current.kids.map { it.id }.toSet() else setOfNotNull(current.selectedKidId)
        if (ids.isEmpty() || runCatching { SchoolHolidayRange(start, end) }.isFailure) {
            _holidayState.value = HolidayRangeState(error = true)
            return
        }
        _holidayState.value = HolidayRangeState(isWorking = true)
        viewModelScope.launch {
            try {
                scheduleOverrideRepository.setNoSchoolRange(ids, start, end, noSchool)
                _holidayState.value = HolidayRangeState(completed = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _holidayState.value = HolidayRangeState(error = true)
            } finally {
                _holidayState.value = _holidayState.value.copy(isWorking = false)
            }
        }
    }

    fun clearHolidayResult() {
        if (!_holidayState.value.isWorking) _holidayState.value = HolidayRangeState()
    }

    fun onKidSelected(kidId: Long) {
        clearHolidayResult()
        selectedKidId.value = kidId
    }

    /**
     * Sets the day's override; the repository removes it again when neither
     * a custom end time nor No School is chosen.
     */
    fun setDayOverride(
        date: LocalDate,
        endTime: LocalTime?,
        isNoSchool: Boolean,
    ) {
        val kidId = uiState.value.selectedKidId ?: return
        viewModelScope.launch {
            scheduleOverrideRepository.setOverride(
                kidId = kidId,
                date = date,
                endTime = endTime?.format(TIME_FORMATTER),
                isNoSchool = isNoSchool,
            )
        }
    }

    private companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
