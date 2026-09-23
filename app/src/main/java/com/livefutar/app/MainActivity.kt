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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.data.NotificationHelper
import com.livefutar.app.data.PreferencesManager
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.LeagueModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LiveFutarBottomBar
import com.livefutar.app.ui.components.SkeletonMatchList
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

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var pendingMatchIdState = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingMatchIdState.value = extractMatchId(intent)

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            var themeMode by remember {
                mutableStateOf(PreferencesManager.getThemeMode(context))
            }
            var accentKey by remember {
                mutableStateOf(PreferencesManager.getAccent(context))
            }

            val ui by viewModel.ui.collectAsState()
            val pendingMatchId by pendingMatchIdState

            LiveFutarTheme(themeMode = themeMode, accentKey = accentKey) {
                var currentScreen by remember { mutableStateOf("home") }
                var selectedMatch by remember { mutableStateOf<MatchModel?>(null) }
                var selectedHighlight by remember { mutableStateOf<HighlightModel?>(null) }
                var standingsLeague by remember { mutableStateOf<LeagueModel?>(null) }
                var showBetSlip by remember { mutableStateOf(false) }

                /*
                 * Adatbetöltés: dátum / képernyő váltáskor.
                 * preferToday = Élő fül → mindig a mai nap.
                 */
                LaunchedEffect(ui.selectedDate, currentScreen) {
                    viewModel.load(
                        isBackground = false,
                        preferToday = currentScreen == "live"
                    )
                }

                val liveCount = ui.todayMatches.count { it.isLive }
                val showBottomBar =
                    selectedMatch == null &&
                        selectedHighlight == null &&
                        standingsLeague == null &&
                        !showBetSlip

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            LiveFutarBottomBar(
                                currentScreen = currentScreen,
                                liveCount = liveCount,
                                onScreenSelected = { currentScreen = it }
                            )
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    ) {
                        if (ui.isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (ui.isOffline && !ui.isLoading) {
                            OfflineBanner()
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            when {
                                selectedHighlight != null -> {
                                    VideoPlayerScreen(
                                        highlight = selectedHighlight!!,
                                        onBackClick = { selectedHighlight = null }
                                    )
                                }

                                standingsLeague != null -> {
                                    StandingsScreen(
                                        league = standingsLeague!!,
                                        onBackClick = { standingsLeague = null }
                                    )
                                }

                                showBetSlip -> {
                                    BetSlipScreen(
                                        onBackClick = { showBetSlip = false }
                                    )
                                }

                                selectedMatch != null -> {
                                    MatchDetailScreen(
                                        match = selectedMatch!!,
                                        onBackClick = { selectedMatch = null },
                                        onOpenBetSlip = { showBetSlip = true },
                                        onStandingsClick = { match ->
                                            match.league?.let { standingsLeague = it }
                                        }
                                    )
                                }

                                currentScreen == "settings" -> {
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

                                ui.isLoading && ui.matches.isEmpty() && ui.todayMatches.isEmpty() -> {
                                    SkeletonMatchList(count = 7)
                                }

                                ui.errorMessage != null && currentScreen != "settings" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = ui.errorMessage!!)
                                        Spacer(modifier = Modifier.padding(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.load(
                                                    isBackground = false,
                                                    preferToday = currentScreen == "live"
                                                )
                                            }
                                        ) {
                                            Text("Újrapróbálkozás")
                                        }
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Button(
                                            onClick = { currentScreen = "settings" }
                                        ) {
                                            Text("Beállítások")
                                        }
                                    }
                                }

                                currentScreen == "live" -> {
                                    LiveScreen(
                                        matches = ui.todayMatches,
                                        favoriteTeamIds = ui.favoriteTeamIds,
                                        onToggleTeamFavorite = viewModel::toggleTeamFavorite,
                                        isRefreshing = ui.isRefreshing,
                                        onRefresh = {
                                            viewModel.refresh(preferToday = true)
                                        },
                                        onMatchClick = { selectedMatch = it }
                                    )
                                }

                                currentScreen == "home" -> {
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
                                        onMatchClick = { selectedMatch = it },
                                        onStandingsClick = { match ->
                                            match.league?.let { standingsLeague = it }
                                        }
                                    )
                                }

                                currentScreen == "highlights" -> {
                                    HighlightsScreen(
                                        highlights = ui.highlights,
                                        onHighlightClick = { selectedHighlight = it }
                                    )
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(pendingMatchId, ui.matches, ui.todayMatches) {
                    val id = pendingMatchId ?: return@LaunchedEffect
                    val found =
                        ui.todayMatches.find { it.id == id }
                            ?: ui.matches.find { it.id == id }
                    if (found != null) {
                        selectedMatch = found
                        pendingMatchIdState.value = null
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

@androidx.compose.runtime.Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentGold.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📡", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Offline – legutóbbi mentett adatok",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
