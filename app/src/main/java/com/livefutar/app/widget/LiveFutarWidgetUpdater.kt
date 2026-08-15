package com.livefutar.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.livefutar.app.MainActivity
import com.livefutar.app.R
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FavoritesManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.model.MatchModel
import com.livefutar.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A widget megjelenítési logikája. Két belépési pontja van:
 *
 * 1. [updateWithMatches] - amikor az app már úgyis lekérte a friss adatot
 *    (induláskor / 60 mp-es háttérfrissítés), nincs extra hálózati kérés.
 *
 * 2. [refreshFromNetwork] - amikor az app nincs megnyitva: a rendszer
 *    ~30 percenkénti automatikus frissítése, vagy a widget saját "↻" gombja
 *    hívja meg, ilyenkor a widget saját maga kér le adatot.
 *
 * FONTOS KORLÁT: Android rendszerszinten legfeljebb 30 percenként engedi
 * az automatikus widget-frissítést (ez nem a mi döntésünk, hanem OS-szintű
 * akkumulátorvédelem). Ennél gyakoribb, valóban "élő" frissítés csak akkor
 * történik, amíg maga az app nyitva van a háttérben, illetve manuális
 * koppintással a "↻" gombra.
 */
object LiveFutarWidgetUpdater {

    private const val MAX_ROWS = 4

    fun updateWithMatches(context: Context, matches: List<MatchModel>) {
        pushToAllWidgets(context, matches, hasApiKey = true)
    }

    suspend fun refreshFromNetwork(context: Context) {
        val apiKey = ApiKeyManager.getApiKey(context)
        if (apiKey.isBlank()) {
            pushToAllWidgets(context, emptyList(), hasApiKey = false)
            return
        }

        try {
            val apiService = FootballApiService.create()
            val today = DateUtils.today()
            val matches = withContext(Dispatchers.IO) {
                apiService.getMatches(apiKey, today).data ?: emptyList()
            }
            pushToAllWidgets(context, matches, hasApiKey = true)
        } catch (e: Exception) {
            // Hálózati hiba esetén nem törli a widget korábbi tartalmát -
            // egyszerűen kihagyjuk ezt a frissítési kört.
        }
    }

    private fun pushToAllWidgets(context: Context, allMatches: List<MatchModel>, hasApiKey: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, LiveFutarWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        val views = buildRemoteViews(context, allMatches, hasApiKey)
        ids.forEach { id ->
            manager.updateAppWidget(id, views)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        allMatches: List<MatchModel>,
        hasApiKey: Boolean
    ): RemoteViews {

        val views = RemoteViews(context.packageName, R.layout.widget_live_futar)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

        val refreshIntent = Intent(context, LiveFutarWidgetProvider::class.java).apply {
            action = LiveFutarWidgetProvider.ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

        if (!hasApiKey) {
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty_message, "Add meg az API kulcsot a Beállításokban")
            hideAllRows(views)
            return views
        }

        val favoriteTeamIds = FavoritesManager.getFavoriteTeamIds(context)
        val favoriteLeagueIds = FavoritesManager.getFavoriteLeagueIds(context)

        val favoriteMatches = allMatches.filter { match ->
            (match.homeTeam?.id != null && favoriteTeamIds.contains(match.homeTeam.id)) ||
                (match.awayTeam?.id != null && favoriteTeamIds.contains(match.awayTeam.id)) ||
                (match.league?.id != null && favoriteLeagueIds.contains(match.league.id))
        }.sortedWith(
            compareByDescending<MatchModel> { it.isLive }
                .thenBy { it.isFinished }
        ).take(MAX_ROWS)

        if (favoriteMatches.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_empty_message,
                if (favoriteTeamIds.isEmpty() && favoriteLeagueIds.isEmpty())
                    "Jelölj ki kedvenc csapatot a ☆ ikonnal"
                else
                    "Nincs kedvenc meccs ma"
            )
            hideAllRows(views)
            return views
        }

        views.setViewVisibility(R.id.widget_empty_message, View.GONE)

        val rowIds = listOf(
            Triple(R.id.widget_row_1, R.id.widget_dot_1, R.id.widget_match_1 to R.id.widget_score_1),
            Triple(R.id.widget_row_2, R.id.widget_dot_2, R.id.widget_match_2 to R.id.widget_score_2),
            Triple(R.id.widget_row_3, R.id.widget_dot_3, R.id.widget_match_3 to R.id.widget_score_3),
            Triple(R.id.widget_row_4, R.id.widget_dot_4, R.id.widget_match_4 to R.id.widget_score_4)
        )

        rowIds.forEachIndexed { index, (rowId, dotId, textIds) ->
            val (matchTextId, scoreTextId) = textIds
            val match = favoriteMatches.getOrNull(index)

            if (match == null) {
                views.setViewVisibility(rowId, View.GONE)
                return@forEachIndexed
            }

            views.setViewVisibility(rowId, View.VISIBLE)
            views.setViewVisibility(dotId, if (match.isLive) View.VISIBLE else View.GONE)

            val homeShort = match.homeTeam?.name ?: "?"
            val awayShort = match.awayTeam?.name ?: "?"
            views.setTextViewText(matchTextId, "$homeShort – $awayShort")

            val scoreText = when {
                match.isNotStarted -> match.kickoffTime
                else -> "${match.homeScoreDisplay}:${match.awayScoreDisplay}"
            }
            views.setTextViewText(scoreTextId, scoreText)
        }

        return views
    }

    private fun hideAllRows(views: RemoteViews) {
        listOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3, R.id.widget_row_4).forEach {
            views.setViewVisibility(it, View.GONE)
        }
    }
}
