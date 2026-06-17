package com.mk.lingocoach.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing language preferences using DataStore
 * Provides a reactive way to store and retrieve the selected language
 */
class LanguagePreferencesRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "language_preferences"
        )
        private val SELECTED_LANGUAGE_KEY = stringPreferencesKey("selected_language")
        private const val DEFAULT_LANGUAGE = "system"
    }

    /**
     * Flow that emits the currently selected language code
     * Defaults to "system" if no language has been selected
     */
    val selectedLanguageFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
        }

    /**
     * Save the selected language code to DataStore
     * @param languageCode The ISO 639-1 language code or "system"
     */
    suspend fun saveSelectedLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] = languageCode
        }
    }

    /**
     * Clear all language preferences (useful for logout/reset)
     */
    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
