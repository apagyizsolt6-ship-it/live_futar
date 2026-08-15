package com.livefutar.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefutar.app.model.BetSlipSelection

/**
 * Egyszerű SharedPreferences-alapú fogadási szelvény.
 * Max 10 sor. Kombinált odds = szorzata az egyes oddsoknak.
 */
object BetSlipManager {

    private const val PREFS = "live_futar_betslip"
    private const val KEY = "selections"
    private const val KEY_STAKE = "stake"
    private const val MAX_SELECTIONS = 10
    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSelections(context: Context): List<BetSlipSelection> {
        val json = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BetSlipSelection>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addSelection(context: Context, selection: BetSlipSelection): Boolean {
        val current = getSelections(context).toMutableList()
        // Ugyanarra a meccsre csak 1 sor
        current.removeAll { it.matchId == selection.matchId }
        if (current.size >= MAX_SELECTIONS) return false
        current.add(selection)
        save(context, current)
        return true
    }

    fun removeSelection(context: Context, matchId: Long) {
        val current = getSelections(context).filter { it.matchId != matchId }
        save(context, current)
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    fun combinedOdd(context: Context): Double {
        val list = getSelections(context)
        if (list.isEmpty()) return 0.0
        return list.fold(1.0) { acc, s -> acc * s.odd }
    }

    fun count(context: Context): Int = getSelections(context).size

    /** A mentett tét (Ft), alapértelmezetten 1000 Ft. */
    fun getStake(context: Context): Double {
        val raw = prefs(context).getString(KEY_STAKE, null) ?: return 1000.0
        return raw.toDoubleOrNull() ?: 1000.0
    }

    fun setStake(context: Context, stake: Double) {
        prefs(context).edit().putString(KEY_STAKE, stake.toString()).apply()
    }

    private fun save(context: Context, list: List<BetSlipSelection>) {
        prefs(context).edit().putString(KEY, gson.toJson(list)).apply()
    }
}
