package com.livefutar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.SportsSoccer,
                    contentDescription = "Meccsek",
                    modifier = Modifier.size(if (currentScreen == "home") 26.dp else 24.dp)
                )
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
            colors = navItemColors(primary)
        )

        NavigationBarItem(
            icon = {
                Box {
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = "Élő",
                        modifier = Modifier.size(if (currentScreen == "live") 26.dp else 24.dp),
                        tint = if (currentScreen == "live") AccentGreen else muted
                    )
                    if (liveCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-4).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(AccentGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (liveCount > 9) "9+" else liveCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
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
            colors = navItemColors(AccentGreen)
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "Videók",
                    modifier = Modifier.size(if (currentScreen == "highlights") 26.dp else 24.dp)
                )
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
            colors = navItemColors(primary)
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Beállítások",
                    modifier = Modifier.size(if (currentScreen == "settings") 26.dp else 24.dp)
                )
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
            colors = navItemColors(primary)
        )
    }
}

@Composable
private fun navItemColors(selectedColor: Color) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = selectedColor,
        selectedTextColor = selectedColor,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = selectedColor.copy(alpha = 0.14f)
    )
