package com.livefutar.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.data.ScoreWatchChecker
import com.livefutar.app.fetchAllMatches
import com.livefutar.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Háttérben lekéri a mai meccseket és értesít kedvenc gól / kezdés / vége esetén.
 */
class LiveMatchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val apiKey = ApiKeyManager.getApiKey(applicationContext)
            if (apiKey.isBlank()) return@withContext Result.success()

            val api = FootballApiService.create()
            val today = DateUtils.today()
            val matches = fetchAllMatches(api, apiKey, today)
            // Csak élő / mai relevant – a checker a kedvenceket szűri
            ScoreWatchChecker.process(applicationContext, matches)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
