package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports the whole database to a JSON document and restores it (spec 2.4).
 * Restore always re-inserts entities with fresh ids and remaps kidId
 * references, so importing into a non-empty database never overwrites
 * existing rows.
 */
interface BackupRepository {
    suspend fun exportAll(): String

    /**
     * Restores from a document produced by exportAll. With [replaceExisting]
     * the database is wiped first; either way the whole import runs in one
     * transaction and takes effect completely or not at all. Throws
     * JSONException on malformed input.
     */
    suspend fun importAll(
        json: String,
        replaceExisting: Boolean,
    )
}

/**
 * Reads and writes the boxes directly instead of going through the
 * repositories: the one-transaction guarantee requires running inside
 * [BoxStore.runInTx], whose lambda is not a coroutine and therefore cannot
 * call the repositories' suspend functions.
 */
class BackupRepositoryImpl(
    private val store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BackupRepository {
    private val kidBox = store.boxFor(Kid::class.java)
    private val activityBox = store.boxFor(Activity::class.java)
    private val scheduleBox = store.boxFor(DailySchedule::class.java)

    override suspend fun exportAll(): String =
        withContext(ioDispatcher) {
            val kids = kidBox.query().build().find()
            val activities = activityBox.query().build().find()
            val overrides = scheduleBox.query().build().find()

            JSONObject()
                .put("version", FORMAT_VERSION)
                .put("kids", JSONArray(kids.map(::kidToJson)))
                .put("activities", JSONArray(activities.map(::activityToJson)))
                .put("overrides", JSONArray(overrides.map(::overrideToJson)))
                .toString(2)
        }

    override suspend fun importAll(
        json: String,
        replaceExisting: Boolean,
    ) {
        withContext(ioDispatcher) {
            val root = JSONObject(json)
            val kids = root.optJSONArray("kids") ?: JSONArray()
            val activities = root.optJSONArray("activities") ?: JSONArray()
            val overrides = root.optJSONArray("overrides") ?: JSONArray()

            store.runInTx {
                if (replaceExisting) {
                    scheduleBox.removeAll()
                    activityBox.removeAll()
                    kidBox.removeAll()
                }

                val kidIdMap = mutableMapOf<Long, Long>()
                for (i in 0 until kids.length()) {
                    val kidJson = kids.getJSONObject(i)
                    val oldId = kidJson.optLong("id")
                    val newId =
                        kidBox.put(
                            Kid(
                                name = kidJson.optString("name"),
                                schoolName = kidJson.optString("schoolName"),
                            ),
                        )
                    if (oldId != 0L) kidIdMap[oldId] = newId
                }

                for (i in 0 until activities.length()) {
                    val activityJson = activities.getJSONObject(i)
                    val kidId = kidIdMap[activityJson.optLong("kidId")] ?: continue
                    val days = activityJson.optJSONArray("daysOfWeek")
                    activityBox.put(
                        Activity(
                            kidId = kidId,
                            name = activityJson.optString("name"),
                            startTime = activityJson.optString("startTime"),
                            endTime = activityJson.optString("endTime"),
                            isEnabled = activityJson.optBoolean("isEnabled", true),
                            daysOfWeek = days?.let { IntArray(days.length()) { days.optInt(it) } } ?: intArrayOf(),
                        ),
                    )
                }

                for (i in 0 until overrides.length()) {
                    val overrideJson = overrides.getJSONObject(i)
                    val kidId = kidIdMap[overrideJson.optLong("kidId")] ?: continue
                    val endTime = overrideJson.optString("endTime").ifEmpty { null }
                    val isNoSchool = overrideJson.optBoolean("isNoSchool")
                    // Meaningless overrides are skipped, matching
                    // ScheduleOverrideRepository.setOverride.
                    if (endTime == null && !isNoSchool) continue
                    scheduleBox.put(
                        DailySchedule(
                            kidId = kidId,
                            date = overrideJson.optString("date"),
                            endTime = endTime ?: "",
                            isNoSchool = isNoSchool,
                        ),
                    )
                }
            }
        }
    }

    private fun kidToJson(kid: Kid): JSONObject =
        JSONObject()
            .put("id", kid.id)
            .put("name", kid.name)
            .put("schoolName", kid.schoolName)

    private fun activityToJson(activity: Activity): JSONObject =
        JSONObject()
            .put("id", activity.id)
            .put("kidId", activity.kidId)
            .put("name", activity.name)
            .put("startTime", activity.startTime)
            .put("endTime", activity.endTime)
            .put("isEnabled", activity.isEnabled)
            .put("daysOfWeek", JSONArray(activity.daysOfWeek.toList()))

    private fun overrideToJson(override: DailySchedule): JSONObject =
        JSONObject()
            .put("id", override.id)
            .put("kidId", override.kidId)
            .put("date", override.date)
            .put("endTime", override.endTime)
            .put("isNoSchool", override.isNoSchool)

    private companion object {
        const val FORMAT_VERSION = 1
    }
}
