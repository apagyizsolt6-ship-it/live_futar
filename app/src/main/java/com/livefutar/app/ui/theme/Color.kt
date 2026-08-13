package com.livefutar.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Prémium sötét paletta ──────────────────────────────────────────────
val DarkBackground = Color(0xFF0B0F14)
val DarkSurface = Color(0xFF151B24)
val DarkSurfaceVariant = Color(0xFF1C2430)
val DarkBorder = Color(0xFF2A3444)

// ── Világos paletta ────────────────────────────────────────────────────
val LightBackground = Color(0xFFF0F2F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF7F8FA)
val LightBorder = Color(0xFFE2E6ED)

// ── Accent színek ──────────────────────────────────────────────────────
val AccentBlue = Color(0xFF3B9EFF)
val AccentGreen = Color(0xFF00E676)
val AccentGold = Color(0xFFFFD54F)
val AccentOrange = Color(0xFFFF8A00)
val AccentPurple = Color(0xFFB388FF)
val AccentRed = Color(0xFFFF5252)
val AccentCyan = Color(0xFF00E5FF)

// Élő meccs
val LiveGlow = Color(0xFF00E676).copy(alpha = 0.12f)
val LiveBorder = Color(0xFF00E676).copy(alpha = 0.45f)

// Szöveg segéd
val TextSecondaryDark = Color(0xFF8B9BB4)
val TextSecondaryLight = Color(0xFF6B7A90)

fun accentFromKey(key: String): Color = when (key) {
    "green" -> AccentGreen
    "gold" -> AccentGold
    "purple" -> AccentPurple
    "orange" -> AccentOrange
    else -> AccentBlue
}
