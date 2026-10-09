package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import io.objectbox.BoxStore
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Exports the whole database to a JSON document and restores it (spec 2.4).
 * Restore always re-inserts entities with fresh ids and remaps kidId
 * references, so importing into a non-empty database never overwrites
 * existing rows.
 */
interface BackupRepository {
    fun exportAll(): String

    /**
     * Restores from a document produced by exportAll. With [replaceExisting]
     * the database is wiped first; either way the whole import runs in one
     * transaction and takes effect completely or not at all. Throws
     * JSONException on malformed input.
     */
    fun importAll(
        json: String,
        replaceExisting: Boolean,
    )
}

class BackupRepositoryImpl(
    private val store: BoxStore,
    private val kidRepository: KidRepository,
    private val activityRepository: ActivityRepository,
    private val scheduleOverrideRepository: ScheduleOverrideRepository,
) : BackupRepository {
    override fun exportAll(): String =
        JSONObject()
            .put("version", FORMAT_VERSION)
            .put("kids", JSONArray(kidRepository.getAllKids().map(::kidToJson)))
            .put("activities", JSONArray(activityRepository.getAllActivities().map(::activityToJson)))
            .put("overrides", JSONArray(scheduleOverrideRepository.getAll().map(::overrideToJson)))
            .toString(2)

    override fun importAll(
        json: String,
        replaceExisting: Boolean,
    ) {
        val root = JSONObject(json)
        val kids = root.optJSONArray("kids") ?: JSONArray()
        val activities = root.optJSONArray("activities") ?: JSONArray()
        val overrides = root.optJSONArray("overrides") ?: JSONArray()

        store.runInTx {
            if (replaceExisting) {
                scheduleOverrideRepository.clearAll()
                activityRepository.clearAll()
                kidRepository.clearAll()
            }

            val kidIdMap = mutableMapOf<Long, Long>()
            for (i in 0 until kids.length()) {
                val kidJson = kids.getJSONObject(i)
                val oldId = kidJson.optLong("id")
                val newId =
                    kidRepository
                        .addKid(
                            name = kidJson.optString("name"),
                            schoolName = kidJson.optString("schoolName"),
                        ).id
                if (oldId != 0L) kidIdMap[oldId] = newId
            }

            for (i in 0 until activities.length()) {
                val activityJson = activities.getJSONObject(i)
                val kidId = kidIdMap[activityJson.optLong("kidId")] ?: continue
                val days = activityJson.optJSONArray("daysOfWeek")
                activityRepository.addActivity(
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
                scheduleOverrideRepository.setOverride(
                    kidId = kidId,
                    date = LocalDate.parse(overrideJson.optString("date")),
                    endTime = overrideJson.optString("endTime").ifEmpty { null },
                    isNoSchool = overrideJson.optBoolean("isNoSchool"),
                )
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
