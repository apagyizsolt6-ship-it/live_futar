package com.livefutar.app.model

import com.livefutar.app.util.DateUtils
import com.livefutar.app.util.HungarianNames

data class MatchModel(
    val id: Long,
    val date: String?,
    val homeTeam: TeamModel?,
    val awayTeam: TeamModel?,
    val league: LeagueModel?,
    val country: CountryModel? = null,
    val state: MatchState?,
    val round: String? = null
) {
    val homeScoreDisplay: String
        get() = state?.score?.current?.split("-")?.getOrNull(0)?.trim()
            ?: state?.score?.current?.split("-")?.getOrNull(0)?.trim()
            ?: "-"

    val awayScoreDisplay: String
        get() = state?.score?.current?.split("-")?.getOrNull(1)?.trim()
            ?: state?.score?.current?.split("-")?.getOrNull(1)?.trim()
            ?: "-"

    val hasScore: Boolean
        get() = state?.score?.current != null

    val kickoffTime: String
        get() = DateUtils.kickoffTime(date)

    /** Orszag + bajnoksag magyarul (pl. "Anglia · Premier League") */
    val leagueDisplayName: String
        get() = HungarianNames.display(country?.name, league?.name)

    val statusLabel: String
        get() = when (state?.description) {
            "Not started" -> "Nem kezdodott el"
            "First half" -> "1. felido"
            "Second half" -> "2. felido"
            "Half time" -> "Felido"
            "Extra time" -> "Hosszabbitas"
            "Break time" -> "Sziunet"
            "Penalties" -> "Buntetok"
            "Finished" -> "Vege"
            "Finished after penalties" -> "Vege (11-esek)"
            "Finished after extra time" -> "Vege (h.u.)"
            "Postponed" -> "Elhalasztva"
            "Suspended" -> "Felfuggesztve"
            "Cancelled" -> "Torolve"
            "Awarded" -> "Igazolva"
            "Interrupted" -> "Megszakitva"
            "Abandoned" -> "Felbeszakadt"
            "In progress" -> "Folyamatban"
            "To be announced" -> "Egyeztetes alatt"
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

    /** Perc kijelzes elo meccsekhez */
    val liveMinuteLabel: String?
        get() {
            val clock = state?.clock ?: return null
            if (!isLive) return null
            return when (state?.description) {
                "Half time" -> "SZ"
                "Break time" -> "SZ"
                "Penalties" -> "11"
                else -> clock.toString() + "'"
            }
        }
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

data class CountryModel(
    val code: String?,
    val name: String?,
    val logo: String?
)
