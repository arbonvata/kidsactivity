package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Persisted alarm offsets shown by the Settings sliders. Writes go to
 * [AlarmSettingsRepository]; the alarm sync coordinator observes the same
 * flows and reschedules reminders when a value changes.
 */
class SettingsViewModel(
    private val alarmSettingsRepository: AlarmSettingsRepository,
) : ViewModel() {
    val schoolAlarmMinutes: StateFlow<Int> =
        alarmSettingsRepository.schoolAlarmMinutes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val activityAlarmMinutes: StateFlow<Int> =
        alarmSettingsRepository.activityAlarmMinutes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    fun onSchoolAlarmChange(minutes: Int) {
        viewModelScope.launch { alarmSettingsRepository.setSchoolAlarmMinutes(minutes) }
    }

    fun onActivityAlarmChange(minutes: Int) {
        viewModelScope.launch { alarmSettingsRepository.setActivityAlarmMinutes(minutes) }
    }
}
