package com.livefutar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.FiberManualRecord
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
import androidx.compose.ui.graphics.vector.ImageVector
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
        NavItem(
            selected = currentScreen == "home",
            icon = Icons.Filled.SportsSoccer,
            label = "Meccsek",
            onClick = { onScreenSelected("home") }
        )
        NavigationBarItem(
            icon = {
                Box {
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = "Élő",
                        modifier = Modifier.size(if (currentScreen == "live") 26.dp else 24.dp),
                        tint = if (currentScreen == "live") {
                            AccentGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
            colors = navColors(AccentGreen)
        )
        NavItem(
            selected = currentScreen == "highlights",
            icon = Icons.Filled.PlayCircle,
            label = "Videók",
            onClick = { onScreenSelected("highlights") }
        )
        NavItem(
            selected = currentScreen == "settings",
            icon = Icons.Filled.Settings,
            label = "Beállítások",
            onClick = { onScreenSelected("settings") }
        )
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selectedColor: Color? = null
) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(if (selected) 26.dp else 24.dp)
            )
        },
        label = {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        selected = selected,
        onClick = onClick,
        colors = navColors(selectedColor)
    )
}

@Composable
private fun navColors(selectedColor: Color? = null) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = selectedColor ?: MaterialTheme.colorScheme.primary,
        selectedTextColor = selectedColor ?: MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = (selectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.14f)
    )
