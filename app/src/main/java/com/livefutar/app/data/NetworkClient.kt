package com.livefutar.app.data

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

object NetworkClient {

    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            // 10 MB Cache méret a hálózati adatok helyi tárolásához
            val cacheSize = 10 * 1024 * 1024L
            val cacheDirectory = File(context.cacheDir, "http_cache")
            val cache = Cache(cacheDirectory, cacheSize)

            // Interceptor a kulcs hozzáadásához (közvetlen Highlightly API-hoz)
            val headerInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val apiKey = ApiKeyManager.getApiKey(context)

                val newRequest = originalRequest.newBuilder()
                    .header("x-rapidapi-key", apiKey) // Vagy ha a Highlightly saját fejlécet kér, itt tudjuk módosítani
                    .build()

                chain.proceed(newRequest)
            }

            val okHttpClient = OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(headerInterceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}
