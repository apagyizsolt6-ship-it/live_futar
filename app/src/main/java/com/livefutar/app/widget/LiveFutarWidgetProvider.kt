package com.livefutar.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A kezdőképernyő widget bejegyzési pontja. Két esetben fut le:
 * - a rendszer ~30 percenként automatikusan meghívja (onUpdate)
 * - a widget saját "↻" gombjára koppintva (ACTION_REFRESH broadcast)
 *
 * A tényleges adatlekérés és megjelenítés a [LiveFutarWidgetUpdater]-ben van,
 * ez az osztály csak a rendszertől kapott eseményeket köti hozzá.
 */
class LiveFutarWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.livefutar.app.widget.ACTION_REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refresh(context)
        }
    }

    private fun refresh(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                LiveFutarWidgetUpdater.refreshFromNetwork(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
