package com.mk.lingocoach.data.model

val appLanguages = listOf(
    LanguageItem("system", "System Default", "Default Settings", "🌐"),
    LanguageItem("en", "English", "English", "🇺🇸"),
    LanguageItem("hi", "Hindi", "हिन्दी", "🇮🇳"),
    LanguageItem("es", "Spanish", "Español", "🇪🇸"),
    LanguageItem("fr", "French", "Français", "🇫🇷"),
    LanguageItem("de", "German", "Deutsch", "🇩🇪"),
    LanguageItem("it", "Italian", "Italiano", "🇮🇹"),
    LanguageItem("pt", "Portuguese", "Português", "🇵🇹"),
    LanguageItem("ru", "Russian", "Русский", "🇷🇺"),
    LanguageItem("ja", "Japanese", "日本語", "🇯🇵"),
    LanguageItem("ko", "Korean", "한국어", "🇰🇷"),
    LanguageItem("zh", "Mandarin Chinese", "简体中文", "🇨🇳"),
    LanguageItem("ar", "Arabic", "العربية", "🇸🇦"),
    LanguageItem("tr", "Turkish", "Türkçe", "🇹🇷"),
    LanguageItem("vi", "Vietnamese", "Tiếng Việt", "🇻🇳")
)

fun appLanguageLabel(code: String): String {
    val language = appLanguages.firstOrNull { it.code == code } ?: appLanguages.first()
    return "${language.flagEmoji} ${language.name}"
}
