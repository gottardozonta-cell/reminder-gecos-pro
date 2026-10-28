package com.example.gecospro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { gestisci(context, intent) } finally { result.finish() }
        }
    }

    private suspend fun gestisci(c: Context, i: Intent) {
        val imp = Store.impostazioni(c)
        val turnoId = i.getStringExtra(Scheduler.K_TURNO) ?: return Scheduler.programmaProssimo(c)
        val dataKey = i.getStringExtra(Scheduler.K_DATA) ?: ""
        val start = i.getLongExtra(Scheduler.K_START, System.currentTimeMillis())

        val t = Store.turni(c).find { it.id == turnoId }
        if (t == null || !imp.master) return Scheduler.programmaProssimo(c)
        if (Store.fatto(c, t.id, dataKey)) return Scheduler.programmaProssimo(c)

        Store.setOcc(c, Store.Occ(t.id, dataKey, start))
        val sede = Store.sedi(c).find { it.id == t.sedeId }
        val assegnazione = Store.assegnazioni(c).find { it.id == t.assegnazioneId }

        var suona = false
        var avviso = false
        if (sede == null || sede.lat == 0.0) {
            suona = true; avviso = true
        } else {
            val loc = Geo.posizione(c)
            if (loc != null && Geo.entroRaggio(loc, sede)) suona = true
        }

        if (suona) {
            Notifiche.mostra(c, t, sede, assegnazione, dataKey, avviso)
            Scheduler.riprova(c, t.id, dataKey, start, 0)
        } else {
            Scheduler.programmaProssimo(c)
        }
    }
}
