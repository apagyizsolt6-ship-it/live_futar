package com.livefutar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.livefutar.app.data.NotificationHelper
import com.livefutar.app.data.PreferencesManager
import com.livefutar.app.model.LeagueModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LiveFutarBottomBar
import com.livefutar.app.ui.components.SkeletonMatchList
import com.livefutar.app.ui.navigation.AppScreen
import com.livefutar.app.ui.screens.BetSlipScreen
import com.livefutar.app.ui.screens.HighlightsScreen
import com.livefutar.app.ui.screens.HomeScreen
import com.livefutar.app.ui.screens.LiveScreen
import com.livefutar.app.ui.screens.MatchDetailScreen
import com.livefutar.app.ui.screens.SettingsScreen
import com.livefutar.app.ui.screens.StandingsScreen
import com.livefutar.app.ui.screens.VideoPlayerScreen
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.LiveFutarTheme
import com.livefutar.app.worker.LiveMatchWorkScheduler

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var pendingMatchIdState = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingMatchIdState.value = extractMatchId(intent)

        // Háttér score-watch (WorkManager, ~15 perc)
        LiveMatchWorkScheduler.schedule(applicationContext)

        splash.setKeepOnScreenCondition {
            viewModel.ui.value.isLoading &&
                viewModel.ui.value.matches.isEmpty() &&
                viewModel.ui.value.todayMatches.isEmpty()
        }

        setContent {
            val context = LocalContext.current
            var themeMode by remember {
                mutableStateOf(PreferencesManager.getThemeMode(context))
            }
            var accentKey by remember {
                mutableStateOf(PreferencesManager.getAccent(context))
            }

            val ui by viewModel.ui.collectAsState()
            val pendingMatchId by pendingMatchIdState
            val navController = rememberNavController()
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route ?: AppScreen.ROUTE_HOME

            LiveFutarTheme(themeMode = themeMode, accentKey = accentKey) {
                // Fő tab betöltés
                LaunchedEffect(ui.selectedDate, currentRoute) {
                    val preferToday = currentRoute == AppScreen.Live.route
                    if (currentRoute in AppScreen.bottomBarRoutes) {
                        viewModel.load(
                            isBackground = false,
                            preferToday = preferToday
                        )
                    }
                }

                // Deep link / értesítés → meccs
                LaunchedEffect(pendingMatchId, ui.matches, ui.todayMatches) {
                    val id = pendingMatchId ?: return@LaunchedEffect
                    val found =
                        ui.todayMatches.find { it.id == id }
                            ?: ui.matches.find { it.id == id }
                    if (found != null) {
                        navController.navigate(AppScreen.MatchDetail.createRoute(id)) {
                            launchSingleTop = true
                        }
                        pendingMatchIdState.value = null
                    }
                }

                val liveCount = ui.todayMatches.count { it.isLive }
                val showBottomBar = currentRoute in AppScreen.bottomBarRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            val tab = when (currentRoute) {
                                AppScreen.Live.route -> "live"
                                AppScreen.Highlights.route -> "highlights"
                                AppScreen.Settings.route -> "settings"
                                else -> "home"
                            }
                            LiveFutarBottomBar(
                                currentScreen = tab,
                                liveCount = liveCount,
                                onScreenSelected = { screen ->
                                    val route = when (screen) {
                                        "live" -> AppScreen.Live.route
                                        "highlights" -> AppScreen.Highlights.route
                                        "settings" -> AppScreen.Settings.route
                                        else -> AppScreen.Home.route
                                    }
                                    navController.navigate(route) {
                                        popUpTo(AppScreen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    ) {
                        if (ui.isRefreshing && showBottomBar) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (ui.isOffline && !ui.isLoading && showBottomBar) {
                            OfflineBanner()
                        }

                        NavHost(
                            navController = navController,
                            startDestination = AppScreen.ROUTE_HOME,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(AppScreen.Home.route) {
                                when {
                                    ui.isLoading && ui.matches.isEmpty() && ui.todayMatches.isEmpty() -> {
                                        SkeletonMatchList(count = 7)
                                    }
                                    ui.errorMessage != null -> {
                                        ErrorPane(
                                            message = ui.errorMessage!!,
                                            onRetry = {
                                                viewModel.load(
                                                    isBackground = false,
                                                    preferToday = false
                                                )
                                            },
                                            onSettings = {
                                                navController.navigate(AppScreen.Settings.route)
                                            }
                                        )
                                    }
                                    else -> {
                                        HomeScreen(
                                            matches = ui.matches,
                                            selectedDate = ui.selectedDate,
                                            onDateSelected = viewModel::setSelectedDate,
                                            favoriteTeamIds = ui.favoriteTeamIds,
                                            favoriteLeagueIds = ui.favoriteLeagueIds,
                                            onToggleTeamFavorite = viewModel::toggleTeamFavorite,
                                            onToggleLeagueFavorite = viewModel::toggleLeagueFavorite,
                                            showOnlyFavorites = ui.showOnlyFavorites,
                                            onToggleShowOnlyFavorites = viewModel::toggleShowOnlyFavorites,
                                            showOnlyLive = ui.showOnlyLive,
                                            onToggleShowOnlyLive = viewModel::toggleShowOnlyLive,
                                            isRefreshing = ui.isRefreshing,
                                            onRefresh = {
                                                viewModel.refresh(preferToday = false)
                                            },
                                            onMatchClick = { match ->
                                                navController.navigate(
                                                    AppScreen.MatchDetail.createRoute(match.id)
                                                )
                                            },
                                            onStandingsClick = { match ->
                                                match.league?.id?.let { lid ->
                                                    navController.navigate(
                                                        AppScreen.Standings.createRoute(lid)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            composable(AppScreen.Live.route) {
                                when {
                                    ui.isLoading && ui.todayMatches.isEmpty() -> {
                                        SkeletonMatchList(count = 5)
                                    }
                                    ui.errorMessage != null && ui.todayMatches.isEmpty() -> {
                                        ErrorPane(
                                            message = ui.errorMessage!!,
                                            onRetry = {
                                                viewModel.load(
                                                    isBackground = false,
                                                    preferToday = true
                                                )
                                            },
                                            onSettings = {
                                                navController.navigate(AppScreen.Settings.route)
                                            }
                                        )
                                    }
                                    else -> {
                                        LiveScreen(
                                            matches = ui.todayMatches,
                                            favoriteTeamIds = ui.favoriteTeamIds,
                                            onToggleTeamFavorite = viewModel::toggleTeamFavorite,
                                            isRefreshing = ui.isRefreshing,
                                            onRefresh = {
                                                viewModel.refresh(preferToday = true)
                                            },
                                            onMatchClick = { match ->
                                                navController.navigate(
                                                    AppScreen.MatchDetail.createRoute(match.id)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            composable(AppScreen.Highlights.route) {
                                HighlightsScreen(
                                    highlights = ui.highlights,
                                    onHighlightClick = { h ->
                                        navController.navigate(
                                            AppScreen.Video.createRoute(h.id.toString())
                                        )
                                    }
                                )
                            }

                            composable(AppScreen.Settings.route) {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    accentKey = accentKey,
                                    onThemeModeChanged = { themeMode = it },
                                    onAccentChanged = { accentKey = it },
                                    onApiKeySaved = {
                                        viewModel.load(
                                            isBackground = false,
                                            preferToday = false
                                        )
                                    }
                                )
                            }

                            composable(AppScreen.BetSlip.route) {
                                BetSlipScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = AppScreen.MatchDetail.route,
                                arguments = listOf(
                                    navArgument("matchId") { type = NavType.LongType }
                                )
                            ) { entry ->
                                val matchId = entry.arguments?.getLong("matchId") ?: return@composable
                                val match = findMatch(ui, matchId)
                                if (match == null) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Meccs betöltése…",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LaunchedEffect(matchId) {
                                        viewModel.load(
                                            isBackground = true,
                                            preferToday = true
                                        )
                                    }
                                } else {
                                    MatchDetailScreen(
                                        match = match,
                                        onBackClick = { navController.popBackStack() },
                                        onOpenBetSlip = {
                                            navController.navigate(AppScreen.BetSlip.route)
                                        },
                                        onStandingsClick = { m ->
                                            m.league?.id?.let { lid ->
                                                navController.navigate(
                                                    AppScreen.Standings.createRoute(lid)
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            composable(
                                route = AppScreen.Standings.route,
                                arguments = listOf(
                                    navArgument("leagueId") { type = NavType.LongType }
                                )
                            ) { entry ->
                                val leagueId = entry.arguments?.getLong("leagueId") ?: return@composable
                                val league = findLeague(ui, leagueId)
                                if (league != null) {
                                    StandingsScreen(
                                        league = league,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                } else {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Tabella nem elérhető",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            composable(
                                route = AppScreen.Video.route,
                                arguments = listOf(
                                    navArgument("highlightId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val hid = entry.arguments?.getString("highlightId")
                                val highlight = ui.highlights.find {
                                    it.id.toString() == hid
                                }
                                if (highlight != null) {
                                    VideoPlayerScreen(
                                        highlight = highlight,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                } else {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Videó nem található",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMatchIdState.value = extractMatchId(intent)
    }

    private fun extractMatchId(intent: Intent?): Long? {
        if (intent == null) return null
        if (intent.hasExtra(NotificationHelper.EXTRA_MATCH_ID)) {
            val id = intent.getLongExtra(NotificationHelper.EXTRA_MATCH_ID, -1L)
            if (id > 0L) return id
        }
        return null
    }
}

private fun findMatch(ui: MainUiState, matchId: Long): MatchModel? =
    ui.todayMatches.find { it.id == matchId }
        ?: ui.matches.find { it.id == matchId }

private fun findLeague(ui: MainUiState, leagueId: Long): LeagueModel? {
    val fromToday = ui.todayMatches.mapNotNull { it.league }.find { it.id == leagueId }
    if (fromToday != null) return fromToday
    return ui.matches.mapNotNull { it.league }.find { it.id == leagueId }
}

@Composable
private fun ErrorPane(
    message: String,
    onRetry: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message)
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = onRetry) { Text("Újrapróbálkozás") }
        Spacer(modifier = Modifier.padding(4.dp))
        Button(onClick = onSettings) { Text("Beállítások") }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentGold.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Offline mód",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = AccentGold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Utolsó mentett adatok",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
