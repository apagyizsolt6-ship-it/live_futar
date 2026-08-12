package com.livefutar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.livefutar.app.ui.theme.LiveFutarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveFutarTheme {
                var currentScreen by remember { mutableStateOf("home") }
                var selectedMatch by remember { mutableStateOf<MatchModel?>(null) }

                var matches by remember { mutableStateOf<List<MatchModel>>(emptyList()) }
                var highlights by remember { mutableStateOf<List<HighlightModel>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                // Minden alkalommal, amikor ez a szám nő, újra lefut a betöltés
                // (pl. a Beállításokban elmentett API kulcs után).
                var reloadTrigger by remember { mutableStateOf(0) }

                val context = androidx.compose.ui.platform.LocalContext.current

                val apiService = remember { FootballApiService.create() }

                LaunchedEffect(reloadTrigger) {
                    isLoading = true
                    errorMessage = null

                    val apiKey = ApiKeyManager.getApiKey(context)
                    if (apiKey.isBlank()) {
                        errorMessage = "Kérlek add meg az API kulcsot a Beállításokban!"
                        isLoading = false
                        return@LaunchedEffect
                    }

                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    try {
                        withContext(Dispatchers.IO) {
                            matches = apiService.getMatches(apiKey, today).data ?: emptyList()
                            highlights = apiService.getHighlights(apiKey, today).data ?: emptyList()
                        }
                    } catch (e: Exception) {
                        errorMessage = "Hiba történt az adatok betöltésekor: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (selectedMatch == null) {
                            LiveFutarBottomBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    currentScreen = screen
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        when {
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
                                onHighlightClick = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
