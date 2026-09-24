package com.livefutar.app.ui.navigation

/**
 * Type-safe navigációs útvonalak.
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
        val bottomBarRoutes = setOf(
            Home.route,
            Live.route,
            Highlights.route,
            Settings.route
        )
    }
}
