package com.livefutar.app.data

import android.content.Context

/**
 * Utolsó keresések – chip-ként a kereső alatt.
 */
object SearchHistoryManager {

    private const val PREF = "search_history"
    private const val KEY = "queries"
    private const val MAX = 8

    fun get(context: Context): List<String> {
        val raw = prefs(context).getString(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("\u0001")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX)
    }

    fun add(context: Context, query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val next = (listOf(q) + get(context).filter { !it.equals(q, ignoreCase = true) })
            .take(MAX)
        prefs(context).edit()
            .putString(KEY, next.joinToString("\u0001"))
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
