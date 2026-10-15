package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
                        overrides
                            .filter { it.kidId == effectiveId }
                            .associateBy { LocalDate.parse(it.date) }
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolOverridesUiState())

    fun onKidSelected(kidId: Long) {
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
                endTime = if (isNoSchool) null else endTime?.format(TIME_FORMATTER),
                isNoSchool = isNoSchool,
            )
        }
    }

    private companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
