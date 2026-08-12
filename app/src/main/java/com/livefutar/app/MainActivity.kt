package com.livefutar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LiveFutarBottomBar
import com.livefutar.app.ui.screens.HighlightsScreen
import com.livefutar.app.ui.screens.HomeScreen
import com.livefutar.app.ui.screens.MatchDetailScreen
import com.livefutar.app.ui.screens.SettingsScreen
import com.livefutar.app.ui.theme.LiveFutarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveFutarTheme {
                var currentScreen by remember { mutableStateOf("home") }
                var selectedMatch by remember { mutableStateOf<MatchModel?>(null) }

                // Teszt adatok
                val sampleMatches = listOf(
                    MatchModel(1, "78'", "LIVE", "Real Madrid", "Barcelona", 2, 1),
                    MatchModel(2, "22'", "LIVE", "Arsenal", "Chelsea", 0, 0),
                    MatchModel(3, "Holnap", "UPCOMING", "Juventus", "AC Milan", 0, 0)
                )

                val sampleHighlights = listOf(
                    HighlightModel(1, "Real Madrid vs Barcelona - Gólklipek", null, null, null),
                    HighlightModel(2, "Arsenal vs Chelsea - Legjobb pillanatok", null, null, null)
                )

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
                    Box(modifier = Modifier.padding(paddingValues)) {
                        if (selectedMatch != null) {
                            MatchDetailScreen(
                                match = selectedMatch!!,
                                onBackClick = { selectedMatch = null }
                            )
                        } else {
                            when (currentScreen) {
                                "home" -> HomeScreen(
                                    matches = sampleMatches,
                                    onMatchClick = { match -> selectedMatch = match }
                                )
                                "highlights" -> HighlightsScreen(
                                    highlights = sampleHighlights,
                                    onHighlightClick = { highlight ->
                                        // Itt majd nyithatjuk a videó lejátszót
                                    }
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
