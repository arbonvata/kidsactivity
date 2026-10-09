package com.arbonvata.kidstrackeractivity.data.repository

import io.objectbox.query.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Wraps an ObjectBox query in a cold Flow using the database's reactive
 * queries: on collection the observer emits the current result, and again on
 * every change to that entity type, no matter which code performed the
 * write. Cancelling the flow cancels the subscription.
 */
internal fun <T> queryFlow(buildQuery: () -> Query<T>): Flow<List<T>> =
    callbackFlow {
        val subscription =
            buildQuery()
                .subscribe()
                .observer { data -> trySend(data) }
        awaitClose { subscription.cancel() }
    }.distinctUntilChanged()
