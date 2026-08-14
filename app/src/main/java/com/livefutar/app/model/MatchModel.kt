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
        get() =
            state?.score?.current
                ?.split("-")
                ?.getOrNull(0)
                ?.trim()
                ?: "-"

    val awayScoreDisplay: String
        get() =
            state?.score?.current
                ?.split("-")
                ?.getOrNull(1)
                ?.trim()
                ?: "-"

    val hasScore: Boolean
        get() =
            !state?.score?.current.isNullOrBlank()

    /**
     * Meccs kezdési ideje magyar idő szerint.
     */
    val kickoffTime: String
        get() =
            DateUtils.kickoffTime(date)

    /**
     * Meccs kezdési időpontja milliszekundumban.
     */
    val kickoffMillis: Long?
        get() =
            DateUtils.kickoffMillis(date)

    /**
     * Igaz, ha a kezdési idő már elmúlt.
     */
    val kickoffAlreadyPassed: Boolean
        get() =
            DateUtils.kickoffAlreadyPassed(date)

    /**
     * Ország + bajnokság magyarul.
     *
     * Például:
     * Anglia · Premier League
     */
    val leagueDisplayName: String
        get() =
            HungarianNames.display(
                country?.name,
                league?.name
            )

    /**
     * Magyar állapotfelirat.
     */
    val statusLabel: String
        get() =
            when (state?.description) {

                "Not started" ->
                    "Nem kezdődött el"

                "First half" ->
                    "1. félidő"

                "Second half" ->
                    "2. félidő"

                "Half time" ->
                    "Félidő"

                "Extra time" ->
                    "Hosszabbítás"

                "Break time" ->
                    "Szünet"

                "Penalties" ->
                    "Büntetők"

                "Finished" ->
                    "Vége"

                "Finished after penalties" ->
                    "Vége (11-esek)"

                "Finished after extra time" ->
                    "Vége (h.u.)"

                "Postponed" ->
                    "Elhalasztva"

                "Suspended" ->
                    "Felfüggesztve"

                "Cancelled" ->
                    "Törölve"

                "Awarded" ->
                    "Igazolva"

                "Interrupted" ->
                    "Megszakítva"

                "Abandoned" ->
                    "Félbeszakadt"

                "In progress" ->
                    "Folyamatban"

                "To be announced" ->
                    "Egyeztetés alatt"

                null ->
                    "Ismeretlen"

                else ->
                    state.description
            }

    /**
     * Élő mérkőzés.
     */
    val isLive: Boolean
        get() =
            when (state?.description) {

                "First half",
                "Second half",
                "Half time",
                "Extra time",
                "Break time",
                "Penalties",
                "In progress",
                "Suspended",
                "Interrupted" -> true

                else -> false
            }

    /**
     * Befejezett mérkőzés.
     */
    val isFinished: Boolean
        get() =
            when (state?.description) {

                "Finished",
                "Finished after penalties",
                "Finished after extra time",
                "Abandoned",
                "Awarded" -> true

                else -> false
            }

    /**
     * A mérkőzés még nem indult el az API állapota szerint.
     */
    val isNotStarted: Boolean
        get() =
            state?.description == "Not started" ||
                state?.description == null

    /**
     * Olyan mérkőzés, amelynek az API szerint még nincs
     * végleges kezdési ideje.
     */
    val isTimeToBeAnnounced: Boolean
        get() =
            state?.description == "To be announced"

    /**
     * Az API szerint még nem indult el,
     * de a megadott kezdési idő már elmúlt.
     *
     * Ez nagyon fontos diagnosztikai jel:
     * ilyenkor az API állapota valószínűleg még nem frissült.
     *
     * Nem állítjuk át automatikusan élőre.
     */
    val isStaleNotStarted: Boolean
        get() =
            isNotStarted &&
                !isTimeToBeAnnounced &&
                kickoffAlreadyPassed

    /**
     * Élő meccsek perc kijelzése.
     */
    val liveMinuteLabel: String?
        get() {

            val clock =
                state?.clock
                    ?: return null

            if (!isLive) {
                return null
            }

            return when (state?.description) {

                "Half time" ->
                    "SZ"

                "Break time" ->
                    "SZ"

                "Penalties" ->
                    "11"

                else ->
                    "$clock'"
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
