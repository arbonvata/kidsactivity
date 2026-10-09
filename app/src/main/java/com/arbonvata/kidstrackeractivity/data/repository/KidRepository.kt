package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity_
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule_
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid_
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface KidRepository {
    suspend fun addKid(
        name: String,
        schoolName: String,
    ): Kid

    suspend fun updateKid(kid: Kid)

    /**
     * Deletes the kid together with all their activities and school-day
     * overrides in a single transaction (spec 8.4 cascade). Once the alarm
     * scheduler exists, its alarms for this kid must be cancelled before
     * calling this.
     */
    suspend fun deleteKid(kidId: Long)

    suspend fun getKid(kidId: Long): Kid?

    suspend fun getAllKids(): List<Kid>

    /** Reactive: all kids ordered by name, now and on every kid-table change. */
    fun getAllKidsFlow(): Flow<List<Kid>>

    suspend fun clearAll()
}

class KidRepositoryImpl(
    private val store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KidRepository {
    private val kidBox = store.boxFor(Kid::class.java)

    override suspend fun addKid(
        name: String,
        schoolName: String,
    ): Kid =
        withContext(ioDispatcher) {
            val kid = Kid(name = name, schoolName = schoolName)
            kid.id = kidBox.put(kid)
            kid
        }

    override suspend fun updateKid(kid: Kid) {
        withContext(ioDispatcher) { kidBox.put(kid) }
    }

    override suspend fun deleteKid(kidId: Long) {
        withContext(ioDispatcher) {
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
    }

    override suspend fun getKid(kidId: Long): Kid? = withContext(ioDispatcher) { kidBox.get(kidId) }

    override suspend fun getAllKids(): List<Kid> =
        withContext(ioDispatcher) {
            kidBox
                .query()
                .order(Kid_.name)
                .build()
                .find()
        }

    override fun getAllKidsFlow(): Flow<List<Kid>> =
        queryFlow {
            kidBox
                .query()
                .order(Kid_.name)
                .build()
        }

    override suspend fun clearAll() {
        withContext(ioDispatcher) { kidBox.removeAll() }
    }
}
