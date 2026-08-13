package com.livefutar.app.data

import com.livefutar.app.model.ApiResponse
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchEventModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.model.StandingsResponse
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
        @Query("date") date: String
    ): ApiResponse<MatchModel>

    @GET("highlights")
    suspend fun getHighlights(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("date") date: String
    ): ApiResponse<HighlightModel>

    // Egy adott meccs eseményei (gólok, lapok, cserék), percre lebontva.
    @GET("events/{id}")
    suspend fun getMatchEvents(
        @Header("x-rapidapi-key") apiKey: String,
        @Path("id") matchId: Long
    ): List<MatchEventModel>

    // Bajnoksági tabella - leagueId és season együtt kötelező.
    @GET("standings")
    suspend fun getStandings(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("leagueId") leagueId: Long,
        @Query("season") season: Int
    ): StandingsResponse

    // Két csapat utolsó (max 10) egymás elleni találkozója.
    @GET("head-2-head")
    suspend fun getHeadToHead(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("teamIdOne") teamIdOne: Long,
        @Query("teamIdTwo") teamIdTwo: Long
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
