package com.livefutar.app.util

import android.content.Context
import android.content.Intent
import com.livefutar.app.model.MatchModel

/**
 * Meccs megosztása rendszer Share sheet-tel.
 */
object ShareMatch {

    fun share(context: Context, match: MatchModel) {
        val home = match.homeTeam?.displayName ?: "Hazai"
        val away = match.awayTeam?.displayName ?: "Vendég"
        val scoreLine = when {
            match.isNotStarted -> match.kickoffTime
            else -> "${match.homeScoreDisplay}–${match.awayScoreDisplay}"
        }
        val status = when {
            match.isLive -> match.liveMinuteLabel?.let { "ÉLŐ $it" } ?: "ÉLŐ"
            match.isFinished -> "Vége"
            match.isNotStarted -> "Kezdés"
            else -> match.statusLabel
        }
        val text = buildString {
            append("⚽ $home $scoreLine $away")
            append("\n")
            append("$status · ${match.leagueDisplayName}")
            append("\n")
            append("Live Futár")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "$home – $away")
        }
        context.startActivity(
            Intent.createChooser(intent, "Meccs megosztása")
        )
    }
}
