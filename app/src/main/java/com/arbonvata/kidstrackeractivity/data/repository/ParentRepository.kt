package com.arbonvata.kidstrackeractivity.data.repository

import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ParentRepository {
    suspend fun addParent(
        name: String,
        relation: String,
        phone: String,
    ): Parent
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
}
