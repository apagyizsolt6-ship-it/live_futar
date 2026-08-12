package com.livefutar.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LiveFutarBottomBar(
    currentScreen: String,
    onScreenSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            icon = { Text("⚽") },
            label = { Text("Meccsek") },
            selected = currentScreen == "home",
            onClick = { onScreenSelected("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray
            )
        )
        NavigationBarItem(
            icon = { Text("🎥") },
            label = { Text("Videók") },
            selected = currentScreen == "highlights",
            onClick = { onScreenSelected("highlights") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray
            )
        )
        NavigationBarItem(
            icon = { Text("⚙️") },
            label = { Text("Beállítások") },
            selected = currentScreen == "settings",
            onClick = { onScreenSelected("settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray
            )
        )
    }
}
