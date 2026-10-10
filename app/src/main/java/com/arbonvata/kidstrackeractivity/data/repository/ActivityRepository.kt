package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ActivityRepository {
    suspend fun addActivity(activity: Activity): Activity

    suspend fun updateActivity(activity: Activity)

    suspend fun deleteActivity(activityId: Long)

    /** Enables or disables an activity without deleting it (spec 2.2). */
    suspend fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    )

    suspend fun deleteAllActivitiesForKid(kidId: Long)

    /**
     * ObjectBox cannot query "int array contains day", so day filtering happens
     * in memory; see getActivitiesForKidOnDay.
     */
    suspend fun getActivitiesForKid(kidId: Long): List<Activity>

    /** Reactive: one kid's activities ordered by start time, now and on every change. */
    fun getActivitiesForKidFlow(kidId: Long): Flow<List<Activity>>

    /** dayOfWeek follows java.time.DayOfWeek: 1 = Monday, 7 = Sunday. */
    suspend fun getActivitiesForKidOnDay(
        kidId: Long,
        dayOfWeek: Int,
    ): List<Activity>

    suspend fun getAllActivities(): List<Activity>

    /** Reactive: all activities, now and on every activity-table change. */
    fun getAllActivitiesFlow(): Flow<List<Activity>>

    suspend fun clearAll()
}

class ActivityRepositoryImpl(
    store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ActivityRepository {
    private val activityBox = store.boxFor(Activity::class.java)

    override suspend fun addActivity(activity: Activity): Activity =
        withContext(ioDispatcher) {
            activity.id = activityBox.put(activity)
            activity
        }

    override suspend fun updateActivity(activity: Activity) {
        withContext(ioDispatcher) { activityBox.put(activity) }
    }

    override suspend fun deleteActivity(activityId: Long) {
        withContext(ioDispatcher) { activityBox.remove(activityId) }
    }

    override suspend fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    ) {
        withContext(ioDispatcher) {
            val activity = activityBox.get(activityId) ?: return@withContext
            activity.isEnabled = enabled
            activityBox.put(activity)
        }
    }

    override suspend fun deleteAllActivitiesForKid(kidId: Long) {
        withContext(ioDispatcher) {
            activityBox
                .query()
                .equal(Activity_.kidId, kidId)
                .build()
                .remove()
        }
    }

    override suspend fun getActivitiesForKid(kidId: Long): List<Activity> =
        withContext(ioDispatcher) {
            activityBox
                .query()
                .equal(Activity_.kidId, kidId)
                .order(Activity_.startTime)
                .build()
                .find()
        }

    override fun getActivitiesForKidFlow(kidId: Long): Flow<List<Activity>> =
        queryFlow {
            activityBox
                .query()
                .equal(Activity_.kidId, kidId)
                .order(Activity_.startTime)
                .build()
        }

    override suspend fun getActivitiesForKidOnDay(
        kidId: Long,
        dayOfWeek: Int,
    ): List<Activity> = getActivitiesForKid(kidId).filter { it.daysOfWeek.contains(dayOfWeek) }

    override suspend fun getAllActivities(): List<Activity> =
        withContext(ioDispatcher) {
            activityBox
                .query()
                .build()
                .find()
        }

    override fun getAllActivitiesFlow(): Flow<List<Activity>> =
        queryFlow {
            activityBox
                .query()
                .build()
        }

    override suspend fun clearAll() {
        withContext(ioDispatcher) { activityBox.removeAll() }
    }
}
