package com.livefutar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

                val context = androidx.compose.ui.platform.LocalContext.current
                
                val apiService = remember {
                    Retrofit.Builder()
                        .baseUrl("https://api.highlightly.net/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(FootballApiService::class.java)
                }

                LaunchedEffect(Unit) {
                    val apiKey = ApiKeyManager.getApiKey(context)
                    if (apiKey.isBlank()) {
                        errorMessage = "Kérlek add meg az API kulcsot a Beállításokban!"
                        isLoading = false
                        return@LaunchedEffect
                    }

                    try {
                        withContext(Dispatchers.IO) {
                            matches = apiService.getMatches(apiKey)
                            highlights = apiService.getHighlights(apiKey)
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
                            isLoading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            errorMessage != null && matches.isEmpty() -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = errorMessage!!)
                                }
                            }
                            selectedMatch != null -> {
                                MatchDetailScreen(
                                    match = selectedMatch!!,
                                    onBackClick = { selectedMatch = null }
                                )
                            }
                            else -> {
                                when (currentScreen) {
                                    "home" -> HomeScreen(
                                        matches = matches,
                                        onMatchClick = { match -> selectedMatch = match }
                                    )
                                    "highlights" -> HighlightsScreen(
                                        highlights = highlights,
                                        onHighlightClick = { highlight -> }
                                    )
                                    "settings" -> SettingsScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
