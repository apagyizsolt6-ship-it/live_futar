package com.livefutar.app.data

import com.livefutar.app.model.ApiResponse
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
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
