package com.arbonvata.kidstrackeractivity.alarms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.scheduledAlarmCodesDataStore by preferencesDataStore(name = "scheduled_alarm_codes")

/**
 * Remembers which alarm request codes are live in AlarmManager. Alarms
 * survive process death but a fresh process has no way to enumerate them,
 * so the scheduler needs this persisted record to know which codes to
 * cancel; request codes are derived from activity id and day, so codes of
 * deleted activities cannot be recomputed.
 */
class ScheduledAlarmCodesStore(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(
        context
            .scheduledAlarmCodesDataStore,
    )

    suspend fun loadRequestCodes(): Set<Int> =
        dataStore.data
            .first()[REQUEST_CODES]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    suspend fun saveRequestCodes(codes: Set<Int>) {
        dataStore.edit { preferences -> preferences[REQUEST_CODES] = codes.map(Int::toString).toSet() }
    }

    private companion object {
        val REQUEST_CODES = stringSetPreferencesKey("scheduled_request_codes")
    }
}
