package com.mk.lingocoach.data.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

enum class AppThemeMode(val value: String) {
    Light("light"),
    System("system"),
    Dark("dark");

    companion object {
        fun fromValue(value: String?): AppThemeMode =
            values().firstOrNull { it.value == value } ?: System
    }
}

object AppThemeManager {
    private const val PREFS = "LingoCoachPrefs"
    private const val KEY_APP_THEME = "app_theme_mode"

    fun currentMode(context: Context): AppThemeMode =
        AppThemeMode.fromValue(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_APP_THEME, AppThemeMode.System.value)
        )

    fun saveMode(context: Context, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_THEME, mode.value)
            .apply()
        applyMode(mode)
    }

    fun applyStoredMode(context: Context) {
        applyMode(currentMode(context))
    }

    private fun applyMode(mode: AppThemeMode) {
        val delegateMode = when (mode) {
            AppThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(delegateMode)
    }
}
