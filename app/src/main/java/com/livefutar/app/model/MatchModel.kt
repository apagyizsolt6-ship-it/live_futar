package com.livefutar.app.model

data class MatchModel(
    val id: Int,
    val time: String?,
    val status: String?,
    val homeTeam: String?,
    val awayTeam: String?,
    val homeScore: Int?,
    val awayScore: Int?
)
