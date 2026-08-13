package com.livefutar.app.model

data class MatchEventModel(
    val team: TeamModel?,
    val time: String?,
    val type: String?,
    val playerId: Long?,
    val player: String?,
    val assistingPlayerId: Long?,
    val assist: String?,
    val substituted: String?
) {
    // A percet vesszük ki az elejéről rendezéshez (pl. "45+1" -> 45).
    val minuteSortKey: Int
        get() {
            val digits = time?.takeWhile { it.isDigit() }
            return digits?.toIntOrNull() ?: 0
        }

    val typeLabel: String
        get() = when (type) {
            "Goal" -> "Gól"
            "Own Goal" -> "Öngól"
            "Penalty" -> "Büntetőgól"
            "Missed Penalty" -> "Kihagyott büntető"
            "Yellow Card" -> "Sárga lap"
            "Red Card" -> "Piros lap"
            "Substitution" -> "Csere"
            "VAR Goal Confirmed" -> "VAR: Gól jóváhagyva"
            "VAR Goal Cancelled" -> "VAR: Gól törölve"
            "VAR Penalty" -> "VAR: Büntető megítélve"
            "VAR Penalty Cancelled" -> "VAR: Büntető törölve"
            "VAR Goal Cancelled - Offside" -> "VAR: Les miatt törölve"
            null -> "Esemény"
            else -> type
        }

    val icon: String
        get() = when (type) {
            "Goal", "Penalty", "Own Goal" -> "⚽"
            "Missed Penalty" -> "❌"
            "Yellow Card" -> "🟨"
            "Red Card" -> "🟥"
            "Substitution" -> "🔄"
            "VAR Goal Confirmed", "VAR Goal Cancelled", "VAR Penalty",
            "VAR Penalty Cancelled", "VAR Goal Cancelled - Offside" -> "📺"
            else -> "•"
        }
}
