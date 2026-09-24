package com.livefutar.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefutar.app.data.db.AppDatabase
import com.livefutar.app.data.db.MatchCacheEntity
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Offline cache: Room (elsődleges) + SharedPreferences fallback.
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

    private fun dao(context: Context) = AppDatabase.get(context).cacheDao()

    private suspend fun roomPut(context: Context, key: String, json: String) {
        dao(context).put(
            MatchCacheEntity(
                cacheKey = key,
                json = json,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun roomGet(context: Context, key: String): String? =
        dao(context).get(key)?.json

    fun saveMatches(context: Context, date: String, matches: List<MatchModel>) {
        try {
            val json = gson.toJson(matches)
            runBlocking {
                withContext(Dispatchers.IO) {
                    roomPut(context, KEY_MATCHES_PREFIX + date, json)
                }
            }
            prefs(context).edit()
                .putString(KEY_MATCHES_PREFIX + date, json)
                .putLong(KEY_SAVED_AT + date, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadMatches(context: Context, date: String): List<MatchModel>? {
        return try {
            val key = KEY_MATCHES_PREFIX + date
            val json = runBlocking {
                withContext(Dispatchers.IO) { roomGet(context, key) }
            } ?: prefs(context).getString(key, null) ?: return null
            val type = object : TypeToken<List<MatchModel>>() {}.type
            gson.fromJson<List<MatchModel>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    fun saveToday(context: Context, date: String, matches: List<MatchModel>) {
        try {
            val json = gson.toJson(matches)
            runBlocking {
                withContext(Dispatchers.IO) {
                    roomPut(context, KEY_TODAY, json)
                    roomPut(context, KEY_TODAY_DATE, date)
                }
            }
            prefs(context).edit()
                .putString(KEY_TODAY, json)
                .putString(KEY_TODAY_DATE, date)
                .putLong(KEY_SAVED_AT + "today", System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadToday(context: Context, expectedDate: String): List<MatchModel>? {
        return try {
            val storedDate = runBlocking {
                withContext(Dispatchers.IO) { roomGet(context, KEY_TODAY_DATE) }
            } ?: prefs(context).getString(KEY_TODAY_DATE, null)
            if (storedDate != expectedDate) return null
            val json = runBlocking {
                withContext(Dispatchers.IO) { roomGet(context, KEY_TODAY) }
            } ?: prefs(context).getString(KEY_TODAY, null) ?: return null
            val type = object : TypeToken<List<MatchModel>>() {}.type
            gson.fromJson<List<MatchModel>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    fun saveHighlights(context: Context, date: String, highlights: List<HighlightModel>) {
        try {
            val json = gson.toJson(highlights)
            runBlocking {
                withContext(Dispatchers.IO) {
                    roomPut(context, KEY_HIGHLIGHTS_PREFIX + date, json)
                }
            }
            prefs(context).edit()
                .putString(KEY_HIGHLIGHTS_PREFIX + date, json)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadHighlights(context: Context, date: String): List<HighlightModel>? {
        return try {
            val key = KEY_HIGHLIGHTS_PREFIX + date
            val json = runBlocking {
                withContext(Dispatchers.IO) { roomGet(context, key) }
            } ?: prefs(context).getString(key, null) ?: return null
            val type = object : TypeToken<List<HighlightModel>>() {}.type
            gson.fromJson<List<HighlightModel>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    fun savedAt(context: Context, date: String): Long =
        prefs(context).getLong(KEY_SAVED_AT + date, 0L)
}
