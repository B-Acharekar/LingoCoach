package com.mk.lingocoach.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mk.lingocoach.data.model.LanguageItem
import com.mk.lingocoach.data.model.appLanguages
import com.mk.lingocoach.data.repository.LanguagePreferencesRepository
import com.mk.lingocoach.data.repository.AppLocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing language selection state and operations
 * Uses StateFlow for reactive UI updates and handles locale changes via AppCompatDelegate
 */
class LanguageViewModel(
    private val repository: LanguagePreferencesRepository,
    private val appContext: Context
) : ViewModel() {

    // All available languages
    private val _availableLanguages = MutableStateFlow(getLanguageList())
    val availableLanguages: StateFlow<List<LanguageItem>> = _availableLanguages.asStateFlow()

    // Currently selected language
    private val _selectedLanguage = MutableStateFlow("system")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // Search query for filtering languages
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered languages based on search query
    private val _filteredLanguages = MutableStateFlow(getLanguageList())
    val filteredLanguages: StateFlow<List<LanguageItem>> = _filteredLanguages.asStateFlow()

    init {
        // Initialize selected language from DataStore
        viewModelScope.launch {
            repository.selectedLanguageFlow.collect { languageCode ->
                _selectedLanguage.value = languageCode
            }
        }

    }

    /**
     * Update the search query and filter languages accordingly
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterLanguages()
    }

    /**
     * Filter languages based on the current search query
     */
    private fun filterLanguages() {
        val query = _searchQuery.value
        _filteredLanguages.value = if (query.isBlank()) {
            _availableLanguages.value
        } else {
            _availableLanguages.value.filter { language ->
                language.name.contains(query, ignoreCase = true) ||
                language.nativeName.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * Select a language and persist the choice
     * Also updates the app locale via AppCompatDelegate
     */
    fun selectLanguage(languageCode: String) {
        viewModelScope.launch {
            _selectedLanguage.value = languageCode
            repository.saveSelectedLanguage(languageCode)
            applyLanguageToSystem(languageCode)
        }
    }

    /**
     * Apply the selected language to the Android system using AppCompatDelegate
     * @param languageCode The ISO 639-1 language code or "system" for default
     */
    private fun applyLanguageToSystem(languageCode: String) {
        AppLocaleManager.setLanguage(languageCode)
    }

    /**
     * Get the complete list of supported languages
     */
    private fun getLanguageList(): List<LanguageItem> = appLanguages

    /**
     * Factory for creating LanguageViewModel instances
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LanguageViewModel::class.java)) {
                val repository = LanguagePreferencesRepository(context)
                return LanguageViewModel(repository, context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
