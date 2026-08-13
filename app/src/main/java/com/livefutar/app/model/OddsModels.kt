package com.livefutar.app.model

/** Egy bookmaker egy piacának egyik kimenetele */
data class OddValue(
    val odd: Double? = null,
    val value: String? = null   // "Home", "Draw", "Away", "Over", "Under", "Yes", "No"...
)

/** Egy bookmaker egy piaca */
data class BookmakerOdd(
    val bookmakerId: Long? = null,
    val bookmakerName: String? = null,
    val type: String? = null,       // "prematch" | "live"
    val market: String? = null,     // "Full Time Result", "Total Goals", "Both Teams to Score"...
    val values: List<OddValue>? = null
)

/** Egy meccs odds válasz eleme */
data class MatchOddsItem(
    val matchId: Long? = null,
    val odds: List<BookmakerOdd>? = null
)

data class OddsApiResponse(
    val data: List<MatchOddsItem>? = null
)

/** Predikció (match detail-ből) */
data class PredictionProbabilities(
    val home: String? = null,
    val draw: String? = null,
    val away: String? = null
)

data class PredictionItem(
    val type: String? = null,
    val modelType: String? = null,
    val generatedAt: String? = null,
    val description: String? = null,
    val probabilities: PredictionProbabilities? = null
)

data class MatchPredictions(
    val prematch: List<PredictionItem>? = null,
    val live: List<PredictionItem>? = null
)

/** Lineup */
data class LineupPlayer(
    val name: String? = null,
    val number: Int? = null,
    val position: String? = null
)

data class TeamLineup(
    val formation: String? = null,
    val initialLineup: List<List<LineupPlayer>>? = null,
    val substitutes: List<LineupPlayer>? = null
)

data class MatchLineups(
    val home: TeamLineup? = null,
    val away: TeamLineup? = null
)

/** Match statistics */
data class StatItem(
    val value: Any? = null,
    val displayName: String? = null,
    val name: String? = null
)

data class TeamStatistics(
    val team: TeamModel? = null,
    val statistics: List<StatItem>? = null
)

/** Fogadási szelvény egy sora */
data class BetSlipSelection(
    val matchId: Long,
    val homeName: String,
    val awayName: String,
    val leagueName: String,
    val market: String,
    val selection: String,      // "Home", "Draw", "Away", "Over 2.5"...
    val odd: Double,
    val bookmakerName: String? = null
) {
    val label: String
        get() = when (selection.lowercase()) {
            "home" -> homeName
            "away" -> awayName
            "draw" -> "Döntetlen"
            else -> selection
        }

    val matchLabel: String
        get() = "$homeName – $awayName"
}
