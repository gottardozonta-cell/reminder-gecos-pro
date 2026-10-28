package com.example.gecospro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {
    const val K_TURNO = "turnoId"
    const val K_DATA = "dataKey"
    const val K_START = "start"
    const val K_COUNT = "count"

    fun prossimo(c: Context): Triple<Turno, Calendar, String>? {
        val now = Calendar.getInstance()
        var bestT: Turno? = null
        var bestC: Calendar? = null
        for (t in Store.turni(c)) {
            if (!t.attivo) continue
            val cal = now.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, t.ora)
            cal.set(Calendar.MINUTE, t.minuto)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.DAY_OF_WEEK, t.giorno)
            if (!cal.after(now)) cal.add(Calendar.WEEK_OF_YEAR, 1)
            if (bestC == null || cal.before(bestC)) { bestT = t; bestC = cal }
        }
        bestC ?: return null
        return Triple(bestT!!, bestC, dataKey(bestC))
    }

    private fun pi(c: Context, i: Intent): PendingIntent =
        PendingIntent.getBroadcast(c, 2001, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun programma(c: Context, at: Long, turnoId: String, dataKey: String, start: Long, count: Int) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(c, AlarmReceiver::class.java)
            .putExtra(K_TURNO, turnoId)
            .putExtra(K_DATA, dataKey)
            .putExtra(K_START, start)
            .putExtra(K_COUNT, count)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c, i))
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c, i))
        }
    }

    fun programmaProssimo(c: Context) {
        val p = prossimo(c) ?: return
        programma(c, p.second.timeInMillis, p.first.id, p.third, p.second.timeInMillis, 0)
    }

    fun riprova(c: Context, turnoId: String, dataKey: String, start: Long, count: Int) {
        val st = Store.impostazioni(c)
        val elapsedMin = (System.currentTimeMillis() - start) / 60_000
        if (elapsedMin >= st.ripetiMaxMin) {
            programmaProssimo(c)
        } else {
            val at = System.currentTimeMillis() + st.ripetiOgniMin * 60_000L
            programma(c, at, turnoId, dataKey, start, count + 1)
        }
    }
}
