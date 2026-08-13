package com.livefutar.app.data

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREF_NAME = "live_futar_prefs"
    private const val KEY_THEME_MODE = "theme_mode"       // system | light | dark
    private const val KEY_ACCENT = "accent_color"         // blue | green | gold | purple | orange
    private const val KEY_NOTIFY_FAVORITES = "notify_favorites"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): String =
        getPrefs(context).getString(KEY_THEME_MODE, "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getAccent(context: Context): String =
        getPrefs(context).getString(KEY_ACCENT, "blue") ?: "blue"

    fun setAccent(context: Context, accent: String) {
        getPrefs(context).edit().putString(KEY_ACCENT, accent).apply()
    }

    fun getNotifyFavorites(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_NOTIFY_FAVORITES, false)

    fun setNotifyFavorites(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFY_FAVORITES, enabled).apply()
    }
}
