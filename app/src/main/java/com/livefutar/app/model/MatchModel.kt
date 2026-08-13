package com.livefutar.app.model

import com.livefutar.app.util.DateUtils

data class MatchModel(
    val id: Long,
    val date: String?,
    val homeTeam: TeamModel?,
    val awayTeam: TeamModel?,
    val league: LeagueModel?,
    val state: MatchState?
) {
    val homeScoreDisplay: String
        get() = state?.score?.current?.split("-")?.getOrNull(0)?.trim() ?: "-"

    val awayScoreDisplay: String
        get() = state?.score?.current?.split("-")?.getOrNull(1)?.trim() ?: "-"

    val hasScore: Boolean
        get() = state?.score?.current != null

    val kickoffTime: String
        get() = DateUtils.kickoffTime(date)

    // Az API angol nyelvű, gépi státuszait fordítjuk le és kategorizáljuk,
    // hogy a UI tudja, mikor mutasson "élő" jelzést.
    val statusLabel: String
        get() = when (state?.description) {
            "Not started" -> "Nem kezdődött el"
            "First half" -> "1. félidő"
            "Second half" -> "2. félidő"
            "Half time" -> "Félidő"
            "Extra time" -> "Hosszabbítás"
            "Break time" -> "Szünet"
            "Penalties" -> "Büntetők"
            "Finished" -> "Vége"
            "Finished after penalties" -> "Vége (11-esek)"
            "Finished after extra time" -> "Vége (h.u.)"
            "Postponed" -> "Elhalasztva"
            "Suspended" -> "Felfüggesztve"
            "Cancelled" -> "Törölve"
            "Awarded" -> "Igazolva"
            "Interrupted" -> "Megszakítva"
            "Abandoned" -> "Félbeszakadt"
            "In progress" -> "Folyamatban"
            "To be announced" -> "Egyeztetés alatt"
            null -> "Ismeretlen"
            else -> state.description
        }

    val isLive: Boolean
        get() = when (state?.description) {
            "First half", "Second half", "Half time", "Extra time",
            "Break time", "Penalties", "In progress", "Suspended", "Interrupted" -> true
            else -> false
        }

    val isFinished: Boolean
        get() = when (state?.description) {
            "Finished", "Finished after penalties", "Finished after extra time",
            "Abandoned", "Awarded" -> true
            else -> false
        }

    val isNotStarted: Boolean
        get() = state?.description == "Not started" || state?.description == null
}

data class MatchState(
    val description: String?,
    val clock: Int?,
    val score: MatchScore?
)

data class MatchScore(
    val current: String?,
    val penalties: String?
)

data class LeagueModel(
    val id: Long,
    val name: String?,
    val logo: String?,
    val season: Int?
)
