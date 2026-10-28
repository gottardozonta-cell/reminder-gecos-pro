package com.example.gecospro

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val A_DONE = "com.example.gecospro.DONE"
        const val A_SNOOZE = "com.example.gecospro.SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        when (intent.action) {
            A_DONE -> {
                val tid = intent.getStringExtra(Scheduler.K_TURNO)
                val dk = intent.getStringExtra(Scheduler.K_DATA)
                if (tid != null && dk != null) Store.setFatto(context, tid, dk)
                nm.cancel(Notifiche.NOTIF_ID)
                Store.setOcc(context, null)
                Scheduler.programmaProssimo(context)
            }
            A_SNOOZE -> {
                val tid = intent.getStringExtra(Scheduler.K_TURNO)
                val dk = intent.getStringExtra(Scheduler.K_DATA)
                nm.cancel(Notifiche.NOTIF_ID)
                if (tid != null && dk != null) {
                    val start = Store.occ(context)?.start ?: System.currentTimeMillis()
                    Scheduler.riprova(context, tid, dk, start, 0)
                } else Scheduler.programmaProssimo(context)
            }
        }
    }
}
