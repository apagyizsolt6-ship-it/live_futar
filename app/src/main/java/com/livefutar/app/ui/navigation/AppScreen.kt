package com.livefutar.app.ui.navigation

/**
 * Type-safe navigációs útvonalak.
 * Minden route string literál – nincs companion hivatkozás object <clinit> közben.
 */
sealed class AppScreen(val route: String) {
    data object Home : AppScreen("home")
    data object Live : AppScreen("live")
    data object Highlights : AppScreen("highlights")
    data object Settings : AppScreen("settings")
    data object BetSlip : AppScreen("bet_slip")
    data object MatchDetail : AppScreen("match/{matchId}") {
        fun createRoute(matchId: Long) = "match/$matchId"
    }
    data object Standings : AppScreen("standings/{leagueId}") {
        fun createRoute(leagueId: Long) = "standings/$leagueId"
    }
    data object Video : AppScreen("video/{highlightId}") {
        fun createRoute(highlightId: String) = "video/$highlightId"
    }

    companion object {
        const val ROUTE_HOME = "home"
        const val ROUTE_LIVE = "live"
        const val ROUTE_HIGHLIGHTS = "highlights"
        const val ROUTE_SETTINGS = "settings"

        val bottomBarRoutes = setOf("home", "live", "highlights", "settings")
    }
}
