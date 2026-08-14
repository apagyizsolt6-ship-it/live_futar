package com.livefutar.app.data

import com.livefutar.app.model.ApiResponse
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchEventModel
import com.livefutar.app.model.MatchLineups
import com.livefutar.app.model.MatchModel
import com.livefutar.app.model.OddsApiResponse
import com.livefutar.app.model.StandingsResponse
import com.livefutar.app.model.TeamStatistics
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface FootballApiService {

    @GET("matches")
    suspend fun getMatches(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/Budapest",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): ApiResponse<MatchModel>

    @GET("highlights")
    suspend fun getHighlights(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/Budapest"
    ): ApiResponse<HighlightModel>

    @GET("events/{id}")
    suspend fun getMatchEvents(
        @Header("x-rapidapi-key") apiKey: String,
        @Path("id") matchId: Long
    ): List<MatchEventModel>

    @GET("standings")
    suspend fun getStandings(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("leagueId") leagueId: Long,
        @Query("season") season: Int
    ): StandingsResponse

    @GET("head-2-head")
    suspend fun getHeadToHead(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("teamIdOne") teamIdOne: Long,
        @Query("teamIdTwo") teamIdTwo: Long
    ): List<MatchModel>

    /**
     * Prematch / live odds – Highlightly
     */
    @GET("odds")
    suspend fun getOdds(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("matchId") matchId: Long,
        @Query("oddsType") oddsType: String = "prematch",
        @Query("limit") limit: Int = 5
    ): OddsApiResponse

    /**
     * Lineups – Ultra plan
     */
    @GET("lineups/{matchId}")
    suspend fun getLineups(
        @Header("x-rapidapi-key") apiKey: String,
        @Path("matchId") matchId: Long
    ): MatchLineups

    /**
     * Match statistics – Ultra plan
     */
    @GET("statistics/{matchId}")
    suspend fun getMatchStatistics(
        @Header("x-rapidapi-key") apiKey: String,
        @Path("matchId") matchId: Long
    ): List<TeamStatistics>

    /**
     * Last 5 games forma
     */
    @GET("last-five-games")
    suspend fun getLastFiveGames(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("teamId") teamId: Long
    ): List<MatchModel>

    companion object {
        fun create(): FootballApiService {
            return Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FootballApiService::class.java)
        }
    }
}
