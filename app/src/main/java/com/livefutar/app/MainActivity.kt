package com.livefutar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FavoritesManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.data.PreferencesManager
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.LeagueModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LiveFutarBottomBar
import com.livefutar.app.ui.screens.HighlightsScreen
import com.livefutar.app.ui.screens.HomeScreen
import com.livefutar.app.ui.screens.LiveScreen
import com.livefutar.app.ui.screens.MatchDetailScreen
import com.livefutar.app.ui.screens.BetSlipScreen
import com.livefutar.app.ui.screens.SettingsScreen
import com.livefutar.app.ui.screens.StandingsScreen
import com.livefutar.app.ui.screens.VideoPlayerScreen
import com.livefutar.app.ui.theme.LiveFutarTheme
import com.livefutar.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val AUTO_REFRESH_INTERVAL_MS = 60_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current

            var themeMode by remember { mutableStateOf(PreferencesManager.getThemeMode(context)) }
            var accentKey by remember { mutableStateOf(PreferencesManager.getAccent(context)) }

            LiveFutarTheme(themeMode = themeMode, accentKey = accentKey) {
                var currentScreen by remember { mutableStateOf("home") }
                var selectedMatch by remember { mutableStateOf<MatchModel?>(null) }
                var selectedHighlight by remember { mutableStateOf<HighlightModel?>(null) }
                var standingsLeague by remember { mutableStateOf<LeagueModel?>(null) }
                var showBetSlip by remember { mutableStateOf(false) }

                var matches by remember { mutableStateOf<List<MatchModel>>(emptyList()) }
                var highlights by remember { mutableStateOf<List<HighlightModel>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var isRefreshing by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var selectedDate by remember { mutableStateOf(DateUtils.today()) }
                var reloadTrigger by remember { mutableStateOf(0) }

                val apiService = remember { FootballApiService.create() }

                var favoriteTeamIds by remember { mutableStateOf(FavoritesManager.getFavoriteTeamIds(context)) }
                var favoriteLeagueIds by remember { mutableStateOf(FavoritesManager.getFavoriteLeagueIds(context)) }
                var showOnlyFavorites by remember { mutableStateOf(false) }
                var showOnlyLive by remember { mutableStateOf(false) }

                fun toggleTeamFavorite(teamId: Long) {
                    FavoritesManager.toggleTeamFavorite(context, teamId)
                    favoriteTeamIds = FavoritesManager.getFavoriteTeamIds(context)
                }

                fun toggleLeagueFavorite(leagueId: Long) {
                    FavoritesManager.toggleLeagueFavorite(context, leagueId)
                    favoriteLeagueIds = FavoritesManager.getFavoriteLeagueIds(context)
                }

                suspend fun fetchData(isBackground: Boolean) {
                    if (isBackground) {
                        isRefreshing = true
                    } else {
                        isLoading = true
                        errorMessage = null
                    }

                    val apiKey = ApiKeyManager.getApiKey(context)
                    if (apiKey.isBlank()) {
                        if (!isBackground) {
                            errorMessage = "Kérlek add meg az API kulcsot a Beállításokban!"
                        }
                        isLoading = false
                        isRefreshing = false
                        return
                    }

                    val today = selectedDate

                    try {
                        withContext(Dispatchers.IO) {
                            val newMatches = apiService.getMatches(apiKey, today).data ?: emptyList()
                            val newHighlights = apiService.getHighlights(apiKey, today).data ?: emptyList()
                            matches = newMatches
                            highlights = newHighlights
                        }
                        errorMessage = null
                    } catch (e: Exception) {
                        if (!isBackground) {
                            errorMessage = "Hiba történt az adatok betöltésekor: ${e.localizedMessage}"
                        }
                    } finally {
                        isLoading = false
                        isRefreshing = false
                    }
                }

                LaunchedEffect(reloadTrigger, selectedDate) {
                    fetchData(isBackground = false)
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(AUTO_REFRESH_INTERVAL_MS)
                        fetchData(isBackground = true)
                    }
                }

                val liveCount = matches.count { it.isLive }
                val showBottomBar =
                    selectedMatch == null && selectedHighlight == null && standingsLeague == null && !showBetSlip

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            LiveFutarBottomBar(
                                currentScreen = currentScreen,
                                liveCount = liveCount,
                                onScreenSelected = { screen -> currentScreen = screen }
                            )
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    ) {
                        if (isRefreshing && currentScreen != "live") {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                                    BetSlipScreen(onBackClick = { showBetSlip = false })
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
                                        onApiKeySaved = { reloadTrigger++ }
                                    )
                                }
                                isLoading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                errorMessage != null && currentScreen != "settings" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = errorMessage!!)
                                        Spacer(modifier = Modifier.padding(8.dp))
                                        Button(onClick = { reloadTrigger++ }) {
                                            Text("Újrapróbálkozás")
                                        }
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Button(onClick = { currentScreen = "settings" }) {
                                            Text("Beállítások")
                                        }
                                    }
                                }
                                currentScreen == "live" -> {
                                    LiveScreen(
                                        matches = matches,
                                        favoriteTeamIds = favoriteTeamIds,
                                        onToggleTeamFavorite = { id -> toggleTeamFavorite(id) },
                                        isRefreshing = isRefreshing,
                                        onRefresh = { reloadTrigger++ },
                                        onMatchClick = { match -> selectedMatch = match }
                                    )
                                }
                                currentScreen == "home" -> {
                                    HomeScreen(
                                        matches = matches,
                                        selectedDate = selectedDate,
                                        onDateSelected = { newDate -> selectedDate = newDate },
                                        favoriteTeamIds = favoriteTeamIds,
                                        favoriteLeagueIds = favoriteLeagueIds,
                                        onToggleTeamFavorite = { id -> toggleTeamFavorite(id) },
                                        onToggleLeagueFavorite = { id -> toggleLeagueFavorite(id) },
                                        showOnlyFavorites = showOnlyFavorites,
                                        onToggleShowOnlyFavorites = {
                                            showOnlyFavorites = !showOnlyFavorites
                                        },
                                        showOnlyLive = showOnlyLive,
                                        onToggleShowOnlyLive = { showOnlyLive = !showOnlyLive },
                                        isRefreshing = isRefreshing,
                                        onRefresh = { reloadTrigger++ },
                                        onMatchClick = { match -> selectedMatch = match },
                                        onStandingsClick = { match ->
                                            match.league?.let { standingsLeague = it }
                                        }
                                    )
                                }
                                currentScreen == "highlights" -> {
                                    HighlightsScreen(
                                        highlights = highlights,
                                        onHighlightClick = { highlight ->
                                            selectedHighlight = highlight
                                        }
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
