package com.livefutar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun buildDarkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    secondary = AccentGreen,
    tertiary = AccentGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder
)

private fun buildLightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    secondary = AccentGreen,
    tertiary = AccentGold,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF0B0F14),
    onSurface = Color(0xFF0B0F14),
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder
)

@Composable
fun LiveFutarTheme(
    themeMode: String = "system",   // system | light | dark
    accentKey: String = "blue",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val accent = accentFromKey(accentKey)
    val colorScheme = if (darkTheme) buildDarkScheme(accent) else buildLightScheme(accent)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
