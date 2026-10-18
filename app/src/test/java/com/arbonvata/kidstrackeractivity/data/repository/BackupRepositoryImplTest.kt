package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [BackupRepositoryImpl] against a mocked [BoxStore]/[Box]es
 * and a real org.json, so the import path is exercised with actual parsing.
 * The malformed-document tests are the load-bearing ones: a document that is
 * structurally incomplete must throw before the transaction opens, because a
 * replace import that accepted it would wipe the database and insert
 * nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryImplTest {
    private val kidBox = mockk<Box<Kid>>(relaxUnitFun = true)
    private val activityBox = mockk<Box<Activity>>(relaxUnitFun = true)
    private val scheduleBox = mockk<Box<DailySchedule>>(relaxUnitFun = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    private val store =
        mockk<BoxStore>(relaxUnitFun = true) {
            every { boxFor(Kid::class.java) } returns kidBox
            every { boxFor(Activity::class.java) } returns activityBox
            every { boxFor(DailySchedule::class.java) } returns scheduleBox
        }

    private val repository =
        BackupRepositoryImpl(
            store = store,
            ioDispatcher = testDispatcher,
        )

    @BeforeEach
    fun setUpTransaction() {
        // Run the runnable inline so verifications below cover what happens
        // inside the transaction callback.
        every { store.runInTx(any()) } answers { firstArg<Runnable>().run() }
    }

    @Test
    fun importAllThrowsOnEmptyObjectAndNeverOpensTheTransaction() =
        runTest {
            val thrown = assertImportThrows("{}")

            assertInstanceOf(JSONException::class.java, thrown)
            verify(exactly = 0) { store.runInTx(any()) }
            verify(exactly = 0) { scheduleBox.removeAll() }
            verify(exactly = 0) { activityBox.removeAll() }
            verify(exactly = 0) { kidBox.removeAll() }
        }

    @Test
    fun importAllThrowsWhenAnArrayIsMissing() =
        runTest {
            val json = """{"version": 1, "kids": [], "activities": []}"""

            assertInstanceOf(JSONException::class.java, assertImportThrows(json))
            verify(exactly = 0) { store.runInTx(any()) }
        }

    @Test
    fun importAllThrowsWhenAnArrayHasTheWrongType() =
        runTest {
            val json = """{"version": 1, "kids": 3, "activities": [], "overrides": []}"""

            assertInstanceOf(JSONException::class.java, assertImportThrows(json))
            verify(exactly = 0) { store.runInTx(any()) }
        }

    @Test
    fun importAllThrowsOnBackupVersionFromANewerApp() =
        runTest {
            val json = validBackupJson.replace("\"version\": 1", "\"version\": 2")

            assertInstanceOf(JSONException::class.java, assertImportThrows(json))
            verify(exactly = 0) { store.runInTx(any()) }
        }

    @Test
    fun importAllReplacesEverythingAndRemapsKidIds() =
        runTest {
            val kidSlot = slot<Kid>()
            val activitySlot = slot<Activity>()
            val overrideSlot = slot<DailySchedule>()
            every { kidBox.put(capture(kidSlot)) } returns 100L
            every { activityBox.put(capture(activitySlot)) } returns 200L
            every { scheduleBox.put(capture(overrideSlot)) } returns 300L

            repository.importAll(validBackupJson, replaceExisting = true)

            verify { scheduleBox.removeAll() }
            verify { activityBox.removeAll() }
            verify { kidBox.removeAll() }
            assertEquals("Ada", kidSlot.captured.name)
            assertEquals(100L, activitySlot.captured.kidId)
            assertEquals(100L, overrideSlot.captured.kidId)
        }

    @Test
    fun importAllMergesWithoutClearingBoxes() =
        runTest {
            every { kidBox.put(any<Kid>()) } returns 100L
            every { activityBox.put(any<Activity>()) } returns 200L
            every { scheduleBox.put(any<DailySchedule>()) } returns 300L

            repository.importAll(validBackupJson, replaceExisting = false)

            verify(exactly = 0) { scheduleBox.removeAll() }
            verify(exactly = 0) { activityBox.removeAll() }
            verify(exactly = 0) { kidBox.removeAll() }
            verify { kidBox.put(any<Kid>()) }
            verify { activityBox.put(any<Activity>()) }
            verify { scheduleBox.put(any<DailySchedule>()) }
        }

    /** JUnit's assertThrows cannot wrap a suspend call, so catch manually. */
    private suspend fun assertImportThrows(json: String): Throwable? =
        runCatching { repository.importAll(json, replaceExisting = true) }.exceptionOrNull()

    private val validBackupJson =
        """
        {
          "version": 1,
          "kids": [{ "id": 7, "name": "Ada", "schoolName": "Fairview" }],
          "activities": [{
            "id": 3, "kidId": 7, "name": "Swim", "startTime": "15:00", "endTime": "16:00",
            "isEnabled": true, "daysOfWeek": [1, 3], "isPeriodic": true,
            "isSchoolActivity": false, "firstDate": ""
          }],
          "overrides": [{
            "id": 1, "kidId": 7, "date": "2026-10-26", "endTime": "14:00", "isNoSchool": false
          }]
        }
        """.trimIndent()
}
