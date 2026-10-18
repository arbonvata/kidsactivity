package com.arbonvata.kidstrackeractivity.alarms

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Round-trips [ScheduledAlarmCodesStore] against a real JVM DataStore backed
 * by a temp file; only the mapping between Int codes and the stored string
 * set is under test, not DataStore itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduledAlarmCodesStoreTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun loadRequestCodesReturnsEmptySetWhenNothingWasSaved() =
        runTest {
            val store = ScheduledAlarmCodesStore(newDataStore())

            assertEquals(emptySet<Int>(), store.loadRequestCodes())
        }

    @Test
    fun saveRequestCodesRoundTripsThroughDisk() =
        runTest {
            val store = ScheduledAlarmCodesStore(newDataStore())

            store.saveRequestCodes(setOf(1, 2, 3))

            assertEquals(setOf(1, 2, 3), store.loadRequestCodes())
        }

    @Test
    fun saveRequestCodesReplacesPreviouslySavedCodes() =
        runTest {
            val store = ScheduledAlarmCodesStore(newDataStore())
            store.saveRequestCodes(setOf(1, 2, 3))

            store.saveRequestCodes(setOf(4))

            assertEquals(setOf(4), store.loadRequestCodes())
        }

    private fun TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
            produceFile = { File(tempDir, "scheduled_alarm_codes.preferences_pb") },
        )
}
