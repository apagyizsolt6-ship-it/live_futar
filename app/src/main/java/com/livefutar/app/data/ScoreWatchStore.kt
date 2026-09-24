package com.livefutar.app.data

import android.content.Context

/**
 * Előző meccs-állapotok tárolása (gól / kezdés / vége detektáláshoz).
 * Formátum: matchId → "homeScore|awayScore|statusKey"
 */
object ScoreWatchStore {

    private const val PREFS = "score_watch"
    private const val KEY_PREFIX = "m_"

    fun getSnapshot(context: Context, matchId: Long): Snapshot? {
        val raw = prefs(context).getString(KEY_PREFIX + matchId, null) ?: return null
        val parts = raw.split("|")
        if (parts.size < 3) return null
        return Snapshot(
            homeScore = parts[0].toIntOrNull() ?: 0,
            awayScore = parts[1].toIntOrNull() ?: 0,
            statusKey = parts[2]
        )
    }

    fun putSnapshot(context: Context, matchId: Long, snapshot: Snapshot) {
        prefs(context).edit()
            .putString(
                KEY_PREFIX + matchId,
                "${snapshot.homeScore}|${snapshot.awayScore}|${snapshot.statusKey}"
            )
            .apply()
    }

    fun clearOld(context: Context, keepIds: Set<Long>) {
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .forEach { key ->
                val id = key.removePrefix(KEY_PREFIX).toLongOrNull()
                if (id != null && id !in keepIds) {
                    editor.remove(key)
                }
            }
        editor.apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Snapshot(
        val homeScore: Int,
        val awayScore: Int,
        val statusKey: String
    )
}
