package com.livefutar.app.data

import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

    @GET("matches")
    suspend fun getMatches(
        @Header("x-api-key") apiKey: String
    ): List<MatchModel>

    @GET("highlights")
    suspend fun getHighlights(
        @Header("x-api-key") apiKey: String
    ): List<HighlightModel>

    companion object {
        private const val BASE_URL = "https://api.highlightly.net/" // Ide jön az éles API alapcíme

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
