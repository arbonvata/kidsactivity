package com.arbonvata.kidstrackeractivity.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.alarmSettingsDataStore by preferencesDataStore(name = "alarm_settings")

interface AlarmSettingsRepository {
    /** Minutes after a school activity ends before the pickup reminder fires. */
    val schoolAlarmMinutes: Flow<Int>

    /** Minutes before an activity starts that the reminder fires. */
    val activityAlarmMinutes: Flow<Int>

    suspend fun setSchoolAlarmMinutes(minutes: Int)

    suspend fun setActivityAlarmMinutes(minutes: Int)
}

/**
 * Persisted alarm offsets, kept out of ObjectBox on purpose: they are app
 * preferences, not domain data. Values are clamped to the ranges the Settings
 * sliders offer, so stale or hand-edited values cannot drift outside them.
 */
class AlarmSettingsRepositoryImpl(
    private val context: Context,
) : AlarmSettingsRepository {
    override val schoolAlarmMinutes: Flow<Int> =
        context.alarmSettingsDataStore.data
            .map { preferences ->
                preferences[SCHOOL_ALARM_MINUTES]
                    ?.coerceIn(MIN_SCHOOL_MINUTES, MAX_SCHOOL_MINUTES)
                    ?: DEFAULT_SCHOOL_MINUTES
            }

    override val activityAlarmMinutes: Flow<Int> =
        context.alarmSettingsDataStore.data
            .map { preferences ->
                preferences[ACTIVITY_ALARM_MINUTES]
                    ?.coerceIn(MIN_ACTIVITY_MINUTES, MAX_ACTIVITY_MINUTES)
                    ?: DEFAULT_ACTIVITY_MINUTES
            }

    override suspend fun setSchoolAlarmMinutes(minutes: Int) {
        context.alarmSettingsDataStore.edit { preferences ->
            preferences[SCHOOL_ALARM_MINUTES] = minutes.coerceIn(MIN_SCHOOL_MINUTES, MAX_SCHOOL_MINUTES)
        }
    }

    override suspend fun setActivityAlarmMinutes(minutes: Int) {
        context.alarmSettingsDataStore.edit { preferences ->
            preferences[ACTIVITY_ALARM_MINUTES] = minutes.coerceIn(MIN_ACTIVITY_MINUTES, MAX_ACTIVITY_MINUTES)
        }
    }

    private companion object {
        val SCHOOL_ALARM_MINUTES = intPreferencesKey("school_alarm_minutes")
        val ACTIVITY_ALARM_MINUTES = intPreferencesKey("activity_alarm_minutes")
        const val MIN_SCHOOL_MINUTES = 5
        const val MAX_SCHOOL_MINUTES = 30
        const val DEFAULT_SCHOOL_MINUTES = 10
        const val MIN_ACTIVITY_MINUTES = 5
        const val MAX_ACTIVITY_MINUTES = 60
        const val DEFAULT_ACTIVITY_MINUTES = 15
    }
}
