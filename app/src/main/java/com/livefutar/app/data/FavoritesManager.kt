package com.livefutar.app.data

import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {
    private const val PREF_NAME = "live_futar_prefs"
    private const val KEY_FAVORITE_TEAMS = "favorite_teams"
    private const val KEY_FAVORITE_LEAGUES = "favorite_leagues"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getFavoriteTeamIds(context: Context): Set<Long> =
        (getPrefs(context).getStringSet(KEY_FAVORITE_TEAMS, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun getFavoriteLeagueIds(context: Context): Set<Long> =
        (getPrefs(context).getStringSet(KEY_FAVORITE_LEAGUES, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun toggleTeamFavorite(context: Context, teamId: Long) {
        val current = (getPrefs(context).getStringSet(KEY_FAVORITE_TEAMS, emptySet()) ?: emptySet())
            .toMutableSet()
        val key = teamId.toString()
        if (!current.add(key)) current.remove(key)
        getPrefs(context).edit().putStringSet(KEY_FAVORITE_TEAMS, current).apply()
    }

    fun toggleLeagueFavorite(context: Context, leagueId: Long) {
        val current = (getPrefs(context).getStringSet(KEY_FAVORITE_LEAGUES, emptySet()) ?: emptySet())
            .toMutableSet()
        val key = leagueId.toString()
        if (!current.add(key)) current.remove(key)
        getPrefs(context).edit().putStringSet(KEY_FAVORITE_LEAGUES, current).apply()
    }
}
