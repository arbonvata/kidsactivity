package com.arbonvata.kidstrackeractivity.widgets

import android.content.Context
import android.util.Log
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/** Live updates while the process is running; the provider also gets periodic launcher updates. */
class AgendaWidgetSync(
    private val context: Context,
    private val kids: KidRepository,
    private val activities: ActivityRepository,
    private val overrides: ScheduleOverrideRepository,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            combine(
                kids.getAllKidsFlow(),
                activities.getAllActivitiesFlow(),
                overrides.getAllOverridesFlow(),
                flow {
                    while (true) {
                        emit(Unit)
                        delay(60_000)
                    }
                },
            ) { _, _, _, _ -> Unit }.collect {
                try {
                    updateAgendaWidgets(context)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("AgendaWidget", "Unable to update agenda", e)
                }
            }
        }
    }
}
