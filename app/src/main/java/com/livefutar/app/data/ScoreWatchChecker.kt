package com.livefutar.app.data

import android.content.Context
import com.livefutar.app.model.MatchModel

/**
 * Közös gól / kezdés / vége detektálás – ViewModel + WorkManager.
 */
object ScoreWatchChecker {

    fun process(context: Context, matches: List<MatchModel>) {
        if (!PreferencesManager.getNotifyFavorites(context)) return
        val favTeams = FavoritesManager.getFavoriteTeamIds(context)
        if (favTeams.isEmpty()) return

        val keepIds = matches.map { it.id }.toSet()
        matches.forEach { match ->
            val homeId = match.homeTeam?.id
            val awayId = match.awayTeam?.id
            val isFavorite =
                (homeId != null && homeId in favTeams) ||
                    (awayId != null && awayId in favTeams)
            if (!isFavorite) return@forEach

            val homeName = match.homeTeam?.displayName ?: "Hazai"
            val awayName = match.awayTeam?.displayName ?: "Vendég"
            val newHome = match.homeScoreDisplay.toIntOrNull() ?: 0
            val newAway = match.awayScoreDisplay.toIntOrNull() ?: 0
            val statusKey = statusKeyOf(match)

            val prev = ScoreWatchStore.getSnapshot(context, match.id)
            if (prev != null) {
                if (prev.statusKey == "not_started" && statusKey == "live") {
                    NotificationHelper.notifyKickoff(
                        context,
                        "⚽ Elkezdődött: $homeName – $awayName",
                        match.leagueDisplayName,
                        match.id
                    )
                }
                if (statusKey == "live" &&
                    (newHome > prev.homeScore || newAway > prev.awayScore)
                ) {
                    NotificationHelper.notifyGoal(
                        context,
                        "⚽ Gól! $homeName $newHome - $newAway $awayName",
                        match.leagueDisplayName,
                        match.id
                    )
                }
                if (prev.statusKey == "live" && statusKey == "finished") {
                    NotificationHelper.notifyFullTime(
                        context,
                        "🏁 Vége: $homeName $newHome - $newAway $awayName",
                        match.leagueDisplayName,
                        match.id
                    )
                }
            }

            ScoreWatchStore.putSnapshot(
                context,
                match.id,
                ScoreWatchStore.Snapshot(newHome, newAway, statusKey)
            )
        }
        ScoreWatchStore.clearOld(context, keepIds)
    }

    private fun statusKeyOf(match: MatchModel): String = when {
        match.isLive -> "live"
        match.isFinished -> "finished"
        match.isNotStarted -> "not_started"
        else -> "other"
    }
}
