package com.livefutar.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.ui.theme.AccentGreen

@Composable
fun LiveFutarBottomBar(
    currentScreen: String,
    liveCount: Int = 0,
    onScreenSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Text("⚽", fontSize = if (currentScreen == "home") 22.sp else 20.sp)
            },
            label = {
                Text(
                    "Meccsek",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == "home") FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentScreen == "home",
            onClick = { onScreenSelected("home") },
            colors = navColors()
        )

        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (liveCount > 0) {
                            Badge(
                                containerColor = AccentGreen,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    if (liveCount > 99) "99+" else liveCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Text("🔴", fontSize = if (currentScreen == "live") 20.sp else 18.sp)
                }
            },
            label = {
                Text(
                    "Élő",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == "live") FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentScreen == "live",
            onClick = { onScreenSelected("live") },
            colors = navColors(selectedColor = AccentGreen)
        )

        NavigationBarItem(
            icon = {
                Text("🎥", fontSize = if (currentScreen == "highlights") 22.sp else 20.sp)
            },
            label = {
                Text(
                    "Videók",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == "highlights") FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentScreen == "highlights",
            onClick = { onScreenSelected("highlights") },
            colors = navColors()
        )

        NavigationBarItem(
            icon = {
                Text("⚙️", fontSize = if (currentScreen == "settings") 22.sp else 20.sp)
            },
            label = {
                Text(
                    "Beállítások",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == "settings") FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentScreen == "settings",
            onClick = { onScreenSelected("settings") },
            colors = navColors()
        )
    }
}

@Composable
private fun navColors(selectedColor: androidx.compose.ui.graphics.Color? = null) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = selectedColor ?: MaterialTheme.colorScheme.primary,
        selectedTextColor = selectedColor ?: MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = (selectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
    )
