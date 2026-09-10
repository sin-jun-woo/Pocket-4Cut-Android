package com.pocket4cut.presentation.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

object AppSettings {
    private const val PREFS_NAME = "pocket4cut_settings"
    private lateinit var prefs: SharedPreferences

    const val COUNTDOWN_MIN = 1
    const val COUNTDOWN_MAX = 10

    var keepScreenOn: Boolean by mutableStateOf(false)
        private set

    var preferFrontCamera: Boolean by mutableStateOf(true)
        private set

    var countdownSeconds: Int by mutableIntStateOf(3)
        private set

    var autoSaveToGallery: Boolean by mutableStateOf(false)
        private set

    var showDateByDefault: Boolean by mutableStateOf(false)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        keepScreenOn = prefs.getBoolean("keepScreenOn", false)
        preferFrontCamera = prefs.getBoolean("preferFrontCamera", true)
        countdownSeconds = prefs.getInt("countdownSeconds", 3).coerceIn(COUNTDOWN_MIN, COUNTDOWN_MAX)
        autoSaveToGallery = prefs.getBoolean("autoSaveToGallery", false)
        showDateByDefault = prefs.getBoolean("showDateByDefault", false)
    }

    fun updateKeepScreenOn(value: Boolean) {
        keepScreenOn = value
        prefs.edit { putBoolean("keepScreenOn", value) }
    }

    fun updatePreferFrontCamera(value: Boolean) {
        preferFrontCamera = value
        prefs.edit().putBoolean("preferFrontCamera", value).apply()
    }

    fun updateCountdownSeconds(value: Int) {
        val clamped = value.coerceIn(COUNTDOWN_MIN, COUNTDOWN_MAX)
        countdownSeconds = clamped
        prefs.edit().putInt("countdownSeconds", clamped).apply()
    }

    fun updateAutoSaveToGallery(value: Boolean) {
        autoSaveToGallery = value
        prefs.edit().putBoolean("autoSaveToGallery", value).apply()
    }

    fun updateShowDateByDefault(value: Boolean) {
        showDateByDefault = value
        prefs.edit().putBoolean("showDateByDefault", value).apply()
    }
}
