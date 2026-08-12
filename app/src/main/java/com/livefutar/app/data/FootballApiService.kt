package com.livefutar.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FootballApiService {

    @GET("countries")
    suspend fun getCountries(
        @Query("name") name: String? = null
    ): Any

    @GET("leagues/{id}")
    suspend fun getLeagueById(
        @Path("id") id: Int
    ): Any

    @GET("teams/{id}")
    suspend fun getTeamById(
        @Path("id") id: Int
    ): Any

    @GET("teams/statistics/{id}")
    suspend fun getTeamStatistics(
        @Path("id") id: Int,
        @Query("fromDate") fromDate: String,
        @Query("timezone") timezone: String? = "Etc/UTC"
    ): Any

    @GET("highlights")
    suspend fun getHighlights(): Any

    @GET("highlights/{id}")
    suspend fun getHighlightById(
        @Path("id") id: Int
    ): Any

    @GET("lineups/{matchId}")
    suspend fun getLineups(
        @Path("matchId") matchId: Int
    ): Any

    @GET("last-five-matches")
    suspend fun getLastFiveMatches(
        @Query("teamId") teamId: Int
    ): Any

    @GET("players")
    suspend fun getPlayers(
        @Query("name") name: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Any

    @GET("players/{id}/statistics")
    suspend fun getPlayerStatistics(
        @Path("id") id: Int
    ): Any

    @GET("box-score/{matchId}")
    suspend fun getBoxScore(
        @Path("matchId") matchId: Int
    ): Any
}
