package com.livefutar.app.model

data class StandingsResponse(
    val groups: List<StandingsGroup>?,
    val league: LeagueModel?
)

data class StandingsGroup(
    val name: String?,
    val standings: List<StandingRow>?
)

data class StandingRow(
    val team: TeamModel?,
    val points: Int?,
    val position: Int?,
    val total: StandingStats?
)

data class StandingStats(
    val games: Int?,
    val wins: Int?,
    val draws: Int?,
    val loses: Int?,
    val scoredGoals: Int?,
    val receivedGoals: Int?
) {
    val goalDifference: Int
        get() = (scoredGoals ?: 0) - (receivedGoals ?: 0)
}
