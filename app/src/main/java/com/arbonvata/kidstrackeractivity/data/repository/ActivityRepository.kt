package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import io.objectbox.BoxStore

interface ActivityRepository {
    fun addActivity(activity: Activity): Activity

    fun updateActivity(activity: Activity)

    fun deleteActivity(activityId: Long)

    /** Enables or disables an activity without deleting it (spec 2.2). */
    fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    )

    fun deleteAllActivitiesForKid(kidId: Long)

    /**
     * ObjectBox cannot query "int array contains day", so day filtering happens
     * in memory; see getActivitiesForKidOnDay.
     */
    fun getActivitiesForKid(kidId: Long): List<Activity>

    /** dayOfWeek follows java.time.DayOfWeek: 1 = Monday, 7 = Sunday. */
    fun getActivitiesForKidOnDay(
        kidId: Long,
        dayOfWeek: Int,
    ): List<Activity>

    fun getAllActivities(): List<Activity>

    fun clearAll()
}

class ActivityRepositoryImpl(
    store: BoxStore,
) : ActivityRepository {
    private val activityBox = store.boxFor(Activity::class.java)

    override fun addActivity(activity: Activity): Activity {
        activity.id = activityBox.put(activity)
        return activity
    }

    override fun updateActivity(activity: Activity) {
        activityBox.put(activity)
    }

    override fun deleteActivity(activityId: Long) {
        activityBox.remove(activityId)
    }

    override fun setActivityEnabled(
        activityId: Long,
        enabled: Boolean,
    ) {
        val activity = activityBox.get(activityId) ?: return
        activity.isEnabled = enabled
        activityBox.put(activity)
    }

    override fun deleteAllActivitiesForKid(kidId: Long) {
        activityBox
            .query()
            .equal(Activity_.kidId, kidId)
            .build()
            .remove()
    }

    override fun getActivitiesForKid(kidId: Long): List<Activity> =
        activityBox
            .query()
            .equal(Activity_.kidId, kidId)
            .order(Activity_.startTime)
            .build()
            .find()

    override fun getActivitiesForKidOnDay(
        kidId: Long,
        dayOfWeek: Int,
    ): List<Activity> = getActivitiesForKid(kidId).filter { it.daysOfWeek.contains(dayOfWeek) }

    override fun getAllActivities(): List<Activity> =
        activityBox
            .query()
            .build()
            .find()

    override fun clearAll() {
        activityBox.removeAll()
    }
}
