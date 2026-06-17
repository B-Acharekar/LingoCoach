package com.mk.lingocoach.data.model

/**
 * Data class representing a language option in the app
 * @param code The ISO 639-1 language code (e.g., "en", "hi", "es") or "system" for default
 * @param name The English name of the language
 * @param nativeName The native name of the language (in its own script)
 * @param flagEmoji The emoji flag representing the language/country
 */
data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)
