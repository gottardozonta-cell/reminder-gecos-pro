package com.example.gecospro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val occ = Store.occ(context)
        if (occ != null && !Store.fatto(context, occ.turnoId, occ.dataKey)) {
            Scheduler.riprova(context, occ.turnoId, occ.dataKey, occ.start, 0)
        } else {
            Scheduler.programmaProssimo(context)
        }
    }
}
