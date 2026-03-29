package com.pocket4cut.ui.designsystem.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    private const val PREFS_NAME = "pocket4cut_theme"
    private const val KEY_SEASON = "selected_season"

    var currentSeason: Season by mutableStateOf(Season.SPRING)
        private set

    val currentTheme: AppThemeData get() = currentSeason.theme()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SEASON, null)
        currentSeason = Season.entries.firstOrNull { it.name == raw } ?: Season.SPRING
    }

    fun setTheme(context: Context, season: Season) {
        currentSeason = season
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEASON, season.name)
            .apply()
    }
}
