package com.arbonvata.kidstrackeractivity.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/** Alarms do not survive reboots; reschedule everything once. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val koin = GlobalContext.get()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                koin.get<AlarmSyncCoordinator>().resyncOnce()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
