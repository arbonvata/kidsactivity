package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule_
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid_
import io.objectbox.BoxStore

interface KidRepository {
    fun addKid(
        name: String,
        schoolName: String,
    ): Kid

    fun updateKid(kid: Kid)

    /**
     * Deletes the kid together with all their activities and school-day
     * overrides in a single transaction (spec 8.4 cascade). Once the alarm
     * scheduler exists, its alarms for this kid must be cancelled before
     * calling this.
     */
    fun deleteKid(kidId: Long)

    fun getKid(kidId: Long): Kid?

    fun getAllKids(): List<Kid>

    fun clearAll()
}

class KidRepositoryImpl(
    private val store: BoxStore,
) : KidRepository {
    private val kidBox = store.boxFor(Kid::class.java)

    override fun addKid(
        name: String,
        schoolName: String,
    ): Kid {
        val kid = Kid(name = name, schoolName = schoolName)
        kid.id = kidBox.put(kid)
        return kid
    }

    override fun updateKid(kid: Kid) {
        kidBox.put(kid)
    }

    override fun deleteKid(kidId: Long) {
        store.runInTx {
            store
                .boxFor(Activity::class.java)
                .query()
                .equal(Activity_.kidId, kidId)
                .build()
                .remove()
            store
                .boxFor(DailySchedule::class.java)
                .query()
                .equal(DailySchedule_.kidId, kidId)
                .build()
                .remove()
            kidBox.remove(kidId)
        }
    }

    override fun getKid(kidId: Long): Kid? = kidBox.get(kidId)

    override fun getAllKids(): List<Kid> =
        kidBox
            .query()
            .order(Kid_.name)
            .build()
            .find()

    override fun clearAll() {
        kidBox.removeAll()
    }
}
