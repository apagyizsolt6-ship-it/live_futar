package com.livefutar.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel

/**
 * Egyszerű offline cache: utolsó sikeres meccs-/highlight-lista dátumonként.
 * SharedPreferences + Gson – nincs Room függőség.
 */
object MatchesCache {

    private const val PREF = "live_futar_cache"
    private const val KEY_MATCHES_PREFIX = "matches_"
    private const val KEY_HIGHLIGHTS_PREFIX = "highlights_"
    private const val KEY_TODAY = "today_matches"
    private const val KEY_TODAY_DATE = "today_date"
    private const val KEY_SAVED_AT = "saved_at_"

    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun saveMatches(context: Context, date: String, matches: List<MatchModel>) {
        try {
            prefs(context).edit()
                .putString(KEY_MATCHES_PREFIX + date, gson.toJson(matches))
                .putLong(KEY_SAVED_AT + date, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
            // cache hiba nem kritikus
        }
    }

    fun loadMatches(context: Context, date: String): List<MatchModel>? {
        return try {
            val json = prefs(context).getString(KEY_MATCHES_PREFIX + date, null)
                ?: return null
            val type = object : TypeToken<List<MatchModel>>() {}.type
            gson.fromJson<List<MatchModel>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    fun saveToday(context: Context, date: String, matches: List<MatchModel>) {
        try {
            prefs(context).edit()
                .putString(KEY_TODAY, gson.toJson(matches))
                .putString(KEY_TODAY_DATE, date)
                .putLong(KEY_SAVED_AT + "today", System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadToday(context: Context, expectedDate: String): List<MatchModel>? {
        return try {
            val storedDate = prefs(context).getString(KEY_TODAY_DATE, null)
            if (storedDate != expectedDate) return null
            val json = prefs(context).getString(KEY_TODAY, null) ?: return null
            val type = object : TypeToken<List<MatchModel>>() {}.type
            gson.fromJson<List<MatchModel>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    fun saveHighlights(context: Context, date: String, highlights: List<HighlightModel>) {
        try {
            prefs(context).edit()
                .putString(KEY_HIGHLIGHTS_PREFIX + date, gson.toJson(highlights))
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadHighlights(context: Context, date: String): List<HighlightModel>? {
        return try {
            val json = prefs(context).getString(KEY_HIGHLIGHTS_PREFIX + date, null)
                ?: return null
            val type = object : TypeToken<List<HighlightModel>>() {}.type
            gson.fromJson<List<HighlightModel>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    /** Cache mentés ideje millis-ben, vagy 0. */
    fun savedAt(context: Context, date: String): Long =
        prefs(context).getLong(KEY_SAVED_AT + date, 0L)
}
