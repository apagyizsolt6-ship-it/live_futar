package com.livefutar.app.model

data class MatchModel(
    val id: Long,
    val date: String?,
    val homeTeam: TeamModel?,
    val awayTeam: TeamModel?,
    val state: MatchState?
) {
    val statusText: String?
        get() = state?.description

    val homeScoreDisplay: String
        get() = state?.score?.current?.split("-")?.getOrNull(0)?.trim() ?: "0"

    val awayScoreDisplay: String
        get() = state?.score?.current?.split("-")?.getOrNull(1)?.trim() ?: "0"
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
