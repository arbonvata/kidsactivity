package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.calendar.CalendarCodec
import com.arbonvata.kidstrackeractivity.data.calendar.CalendarImport
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CalendarRepository {
    suspend fun export(kidId: Long): String

    suspend fun exportActivity(activityId: Long): String

    suspend fun preview(calendar: String): CalendarImport

    suspend fun importCalendar(
        calendar: CalendarImport,
        targetKidId: Long?,
        newChildName: String,
    ): CalendarImportResult
}

data class CalendarImportResult(
    val added: Int,
    val duplicates: Int,
)

class CalendarRepositoryImpl(
    private val store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val exportNamespace: String =
        java.util.UUID
            .randomUUID()
            .toString(),
) : CalendarRepository {
    override suspend fun export(kidId: Long): String =
        withContext(ioDispatcher) {
            val kid = requireNotNull(store.boxFor(Kid::class.java).get(kidId)) { "Child no longer exists" }
            val activities = store.boxFor(Activity::class.java).all.filter { it.kidId == kidId }
            require(activities.isNotEmpty()) { "This child has no activities to share" }
            CalendarCodec.export(kid, activities, store.boxFor(DailySchedule::class.java).all, namespace = exportNamespace)
        }

    override suspend fun exportActivity(activityId: Long): String =
        withContext(ioDispatcher) {
            val activity = requireNotNull(store.boxFor(Activity::class.java).get(activityId)) { "Activity no longer exists" }
            val kid = requireNotNull(store.boxFor(Kid::class.java).get(activity.kidId)) { "Child no longer exists" }
            CalendarCodec.export(kid, listOf(activity), store.boxFor(DailySchedule::class.java).all, namespace = exportNamespace)
        }

    override suspend fun preview(calendar: String): CalendarImport = withContext(ioDispatcher) { CalendarCodec.parse(calendar) }

    override suspend fun importCalendar(
        calendar: CalendarImport,
        targetKidId: Long?,
        newChildName: String,
    ): CalendarImportResult =
        withContext(ioDispatcher) {
            require(calendar.activities.isNotEmpty()) { "No activities to import" }
            val kids = store.boxFor(Kid::class.java)
            val activities = store.boxFor(Activity::class.java)
            var added = 0
            var duplicates = 0
            store.runInTx {
                val kidId =
                    if (targetKidId != null) {
                        require(kids.get(targetKidId) != null) { "Selected child no longer exists" }
                        targetKidId
                    } else {
                        val name = newChildName.trim()
                        require(name.isNotBlank()) { "Enter a name for the new child" }
                        require(
                            kids.all.none { it.name.equals(name, ignoreCase = true) },
                        ) { "A child with this name already exists; choose that child" }
                        kids.put(Kid(name = name))
                    }
                val existing =
                    activities.all
                        .filter { it.kidId == kidId }
                        .map(::identity)
                        .toMutableSet()
                calendar.activities.forEach { incoming ->
                    if (!existing.add(identity(incoming))) {
                        duplicates++
                    } else {
                        activities.put(incoming.copy(id = 0, kidId = kidId, daysOfWeek = incoming.daysOfWeek.copyOf()))
                        added++
                    }
                }
            }
            CalendarImportResult(added, duplicates)
        }

    /** IDs and export DTSTART don't turn the same weekly pattern into a new activity on every import. */
    private fun identity(activity: Activity): Activity =
        activity.copy(
            id = 0,
            kidId = 0,
            daysOfWeek = activity.daysOfWeek.sortedArray(),
            firstDate = if (activity.isPeriodic || activity.isSchoolActivity) null else activity.firstDate,
            calendarStartDate = null,
            location = activity.location.orEmpty(),
            pickupAdult = activity.pickupAdult.orEmpty(),
            pickupNotes = activity.pickupNotes.orEmpty(),
            occurrenceExceptions = activity.occurrenceExceptions.orEmpty(),
        )
}
