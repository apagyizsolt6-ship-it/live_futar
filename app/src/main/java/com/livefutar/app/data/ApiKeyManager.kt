package com.livefutar.app.data

import android.content.Context
import android.content.SharedPreferences

object ApiKeyManager {
    private const val PREF_NAME = "live_futar_prefs"
    private const val KEY_API_KEY = "api_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(context: Context): String {
        // Visszaadja a mentett kulcsot, vagy egy üres stringet, ha még nincs megadva
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun saveApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }
}
