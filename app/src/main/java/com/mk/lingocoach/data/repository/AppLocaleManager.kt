package com.mk.lingocoach.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLocaleManager {
    private val _languageCode = MutableStateFlow("system")
    val languageCode = _languageCode.asStateFlow()

    fun setLanguage(languageCode: String) {
        _languageCode.value = languageCode.ifBlank { "system" }
    }
}
