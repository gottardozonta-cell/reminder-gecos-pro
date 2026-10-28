package com.example.gecospro

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class Tipo { INGRESSO, USCITA }

data class Sede(
    val id: String = UUID.randomUUID().toString(),
    var nome: String,
    var indirizzo: String,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var raggio: Int = 500
)

data class Assegnazione(
    val id: String = UUID.randomUUID().toString(),
    var nome: String
)

data class Turno(
    val id: String = UUID.randomUUID().toString(),
    var sedeId: String,
    var assegnazioneId: String,
    var tipo: Tipo,
    var ora: Int,
    var minuto: Int,
    var giorno: Int,
    var attivo: Boolean = true
)

data class Impostazioni(
    var master: Boolean = true,
    var suonoUri: String? = null,
    var appPackage: String? = null,
    var ripetiOgniMin: Int = 5,
    var ripetiMaxMin: Int = 45
)

object Store {
    private val gson = Gson()
    private lateinit var p: SharedPreferences

    fun init(c: Context) {
        if (this::p.isInitialized) return
        p = c.getSharedPreferences("gecos_pro", Context.MODE_PRIVATE)
        if (!p.getBoolean("seeded", false)) {
            p.edit()
                .putString("sedi", gson.toJson(emptyList<Sede>()))
                .putString("assegnazioni", gson.toJson(emptyList<Assegnazione>()))
                .putString("turni", gson.toJson(emptyList<Turno>()))
                .putBoolean("seeded", true)
                .apply()
        }
    }

    private inline fun <reified T> leggi(key: String): List<T> {
        val json = p.getString(key, null) ?: return emptyList()
        return try {
            gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> salva(key: String, list: List<T>) =
        p.edit().putString(key, gson.toJson(list)).apply()

    fun sedi(c: Context): MutableList<Sede> { init(c); return leggi<Sede>("sedi").toMutableList() }
    fun assegnazioni(c: Context): MutableList<Assegnazione> { init(c); return leggi<Assegnazione>("assegnazioni").toMutableList() }
    fun turni(c: Context): MutableList<Turno> { init(c); return leggi<Turno>("turni").toMutableList() }
    fun impostazioni(c: Context): Impostazioni {
        init(c)
        return p.getString("imp", null)?.let { gson.fromJson(it, Impostazioni::class.java) } ?: Impostazioni()
    }

    fun salvaSedi(c: Context, list: List<Sede>) { init(c); salva("sedi", list) }
    fun salvaAssegnazioni(c: Context, list: List<Assegnazione>) { init(c); salva("assegnazioni", list) }
    fun salvaTurni(c: Context, list: List<Turno>) { init(c); salva("turni", list) }
    fun salvaImpostazioni(c: Context, i: Impostazioni) { init(c); p.edit().putString("imp", gson.toJson(i)).apply() }

    fun fatto(c: Context, turnoId: String, dataKey: String): Boolean {
        init(c); return p.getBoolean("$turnoId|$dataKey", false)
    }
    fun setFatto(c: Context, turnoId: String, dataKey: String) {
        init(c); p.edit().putBoolean("$turnoId|$dataKey", true).apply()
    }

    data class Occ(val turnoId: String, val dataKey: String, val start: Long)
    fun setOcc(c: Context, o: Occ?) { init(c); p.edit().putString("occ", o?.let { gson.toJson(it) }).apply() }
    fun occ(c: Context): Occ? { init(c); return p.getString("occ", null)?.let { gson.fromJson(it, Occ::class.java) } }
}

fun nomeGiorno(g: Int): String = when (g) {
    Calendar.MONDAY -> "Lunedì"
    Calendar.TUESDAY -> "Martedì"
    Calendar.WEDNESDAY -> "Mercoledì"
    Calendar.THURSDAY -> "Giovedì"
    Calendar.FRIDAY -> "Venerdì"
    Calendar.SATURDAY -> "Sabato"
    else -> "Domenica"
}

fun hhmm(h: Int, m: Int) = String.format(Locale.ITALY, "%02d:%02d", h, m)
fun dataKey(cal: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(cal.time)
fun dataKey(d: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(d)
