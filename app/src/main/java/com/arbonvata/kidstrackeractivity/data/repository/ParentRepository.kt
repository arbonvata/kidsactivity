package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent_
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ParentRepository {
    suspend fun addParent(
        name: String,
        relation: String,
        phone: String,
    ): Parent

    suspend fun updateParent(parent: Parent)

    /**
     * Nothing references a parent by id — delegations and pickup adults store
     * names — so deleting has no cascade and never breaks existing text.
     */
    suspend fun deleteParent(parentId: Long)

    suspend fun getParent(parentId: Long): Parent?

    suspend fun getAllParents(): List<Parent>

    /** Reactive: all parents ordered by name, now and on every parent-table change. */
    fun getAllParentsFlow(): Flow<List<Parent>>
}

class ParentRepositoryImpl(
    private val store: BoxStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ParentRepository {
    private val parentBox = store.boxFor(Parent::class.java)

    override suspend fun addParent(
        name: String,
        relation: String,
        phone: String,
    ): Parent =
        withContext(ioDispatcher) {
            val parent = Parent(name = name, relation = relation, phone = phone)
            parent.id = parentBox.put(parent)
            parent
        }

    override suspend fun updateParent(parent: Parent) {
        withContext(ioDispatcher) { parentBox.put(parent) }
    }

    override suspend fun deleteParent(parentId: Long) {
        withContext(ioDispatcher) { parentBox.remove(parentId) }
    }

    override suspend fun getParent(parentId: Long): Parent? = withContext(ioDispatcher) { parentBox.get(parentId) }

    override suspend fun getAllParents(): List<Parent> =
        withContext(ioDispatcher) {
            parentBox
                .query()
                .order(Parent_.name)
                .build()
                .find()
        }

    override fun getAllParentsFlow(): Flow<List<Parent>> =
        queryFlow {
            parentBox
                .query()
                .order(Parent_.name)
                .build()
        }
}
