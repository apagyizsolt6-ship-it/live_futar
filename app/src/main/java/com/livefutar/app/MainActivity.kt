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
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LiveFutarBottomBar
import com.livefutar.app.ui.screens.HighlightsScreen
import com.livefutar.app.ui.screens.HomeScreen
import com.livefutar.app.ui.screens.MatchDetailScreen
import com.livefutar.app.ui.screens.SettingsScreen
import com.livefutar.app.ui.screens.VideoPlayerScreen
import com.livefutar.app.ui.theme.LiveFutarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Élő adatok esetén ennyi időnként frissítünk automatikusan a háttérben.
private const val AUTO_REFRESH_INTERVAL_MS = 60_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveFutarTheme {
                var currentScreen by remember { mutableStateOf("home") }
                var selectedMatch by remember { mutableStateOf<MatchModel?>(null) }
                var selectedHighlight by remember { mutableStateOf<HighlightModel?>(null) }

                var matches by remember { mutableStateOf<List<MatchModel>>(emptyList()) }
                var highlights by remember { mutableStateOf<List<HighlightModel>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var isRefreshing by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                // Minden alkalommal, amikor ez a szám nő, azonnal újratöltjük az adatokat
                // (pl. a Beállításokban elmentett API kulcs után, vagy "Újrapróbálkozás" gombra).
                var reloadTrigger by remember { mutableStateOf(0) }

                val context = androidx.compose.ui.platform.LocalContext.current
                val apiService = remember { FootballApiService.create() }

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

                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    try {
                        withContext(Dispatchers.IO) {
                            val newMatches = apiService.getMatches(apiKey, today).data ?: emptyList()
                            val newHighlights = apiService.getHighlights(apiKey, today).data ?: emptyList()
                            matches = newMatches
                            highlights = newHighlights
                        }
                        errorMessage = null
                    } catch (e: Exception) {
                        // Háttérben futó (csendes) frissítésnél nem zavarjuk meg a felhasználót
                        // egy hibaüzenettel, ha épp csak az internet akadt egy pillanatra.
                        if (!isBackground) {
                            errorMessage = "Hiba történt az adatok betöltésekor: ${e.localizedMessage}"
                        }
                    } finally {
                        isLoading = false
                        isRefreshing = false
                    }
                }

                // Azonnali (teljes képernyős töltő) betöltés induláskor és manuális újrapróbálkozáskor.
                LaunchedEffect(reloadTrigger) {
                    fetchData(isBackground = false)
                }

                // Csendes, automatikus élő frissítés a háttérben, villogás nélkül.
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(AUTO_REFRESH_INTERVAL_MS)
                        fetchData(isBackground = true)
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (selectedMatch == null && selectedHighlight == null) {
                            LiveFutarBottomBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    currentScreen = screen
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
                        if (isRefreshing) {
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
                                selectedMatch != null -> {
                                    MatchDetailScreen(
                                        match = selectedMatch!!,
                                        onBackClick = { selectedMatch = null }
                                    )
                                }
                                // A Beállítások fül mindig elérhető, függetlenül attól,
                                // hogy van-e még API kulcs vagy hiba történt-e a betöltéskor.
                                currentScreen == "settings" -> {
                                    SettingsScreen(
                                        onApiKeySaved = { reloadTrigger++ }
                                    )
                                }
                                isLoading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                errorMessage != null -> {
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
                                    }
                                }
                                currentScreen == "home" -> HomeScreen(
                                    matches = matches,
                                    onMatchClick = { match -> selectedMatch = match }
                                )
                                currentScreen == "highlights" -> HighlightsScreen(
                                    highlights = highlights,
                                    onHighlightClick = { highlight -> selectedHighlight = highlight }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
