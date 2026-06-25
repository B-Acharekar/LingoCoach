package com.mk.lingocoach

import android.os.Bundle
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mk.lingocoach.ui.screens.*
import com.mk.lingocoach.ui.theme.LingoCoachTheme
import com.mk.lingocoach.notifications.NotificationScheduler
import com.mk.lingocoach.data.repository.AppLocaleManager
import java.util.Locale

enum class Screen {
    LanguageSelection,
    WelcomeAboard,
    UserProfileSetup,
    Assessment,
    LearningPathRoadmap,
    ActualLearningPath,
    Home,
    Lesson,
    VocabBuilder,
    MistakeVault,
    Flashcards,
    TimelyDuel,
    AILab,
    Settings,
    Analytics
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupScreen = resolveStartScreen()

        // Restore locale on cold start
        val savedLang = getSharedPreferences("language_preferences_mirror", MODE_PRIVATE)
            .getString("selected_language", null)
        AppLocaleManager.setLanguage(savedLang ?: "system")

        // Schedule reminders away from the first UI frame so cold start stays snappy.
        lifecycleScope.launch(Dispatchers.IO) {
            NotificationScheduler.scheduleDailyReminders(this@MainActivity)
        }

        enableEdgeToEdge()
        setContent {
            val languageCode by AppLocaleManager.languageCode.collectAsState()
            val baseContext = LocalContext.current
            val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current ?: this
            val localizedConfiguration = remember(baseContext, languageCode) {
                Configuration(baseContext.resources.configuration).apply {
                    val locale = if (languageCode == "system") Locale.getDefault() else Locale.forLanguageTag(languageCode)
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val localizedContext = remember(baseContext, localizedConfiguration) {
                baseContext.createConfigurationContext(localizedConfiguration)
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides activityResultRegistryOwner
            ) {
                LingoCoachTheme(dynamicColor = false) {
                var currentScreenName by rememberSaveable {
                    mutableStateOf(startupScreen.name)
                }
                var currentSublessonId by rememberSaveable { mutableStateOf("") }
                val currentScreen = runCatching { Screen.valueOf(currentScreenName) }.getOrDefault(Screen.LanguageSelection)
                val screenBackStack = remember { mutableStateListOf<String>() }

                fun navigateTo(screen: Screen) {
                    if (currentScreenName != screen.name) {
                        screenBackStack.add(currentScreenName)
                        currentScreenName = screen.name
                    }
                }

                fun replaceWith(screen: Screen) {
                    currentScreenName = screen.name
                }

                fun resetTo(screen: Screen) {
                    screenBackStack.clear()
                    currentScreenName = screen.name
                }

                // Tracks where the user came from before opening the Roadmap,
                // so the back arrow can return to the correct screen.
                var roadmapLaunchedFromAssessment by rememberSaveable { mutableStateOf(false) }

                fun goBack() {
                    if (screenBackStack.isNotEmpty()) {
                        currentScreenName = screenBackStack.removeAt(screenBackStack.lastIndex)
                        return
                    }

                    currentScreenName = when (currentScreen) {
                        Screen.LanguageSelection -> Screen.LanguageSelection.name
                        Screen.WelcomeAboard -> Screen.LanguageSelection.name
                        Screen.UserProfileSetup -> Screen.WelcomeAboard.name
                        Screen.Assessment -> Screen.UserProfileSetup.name
                        Screen.LearningPathRoadmap -> if (roadmapLaunchedFromAssessment) Screen.Assessment.name else Screen.Home.name
                        Screen.ActualLearningPath -> Screen.Home.name
                        Screen.Lesson -> Screen.ActualLearningPath.name
                        Screen.VocabBuilder,
                        Screen.MistakeVault,
                        Screen.Flashcards,
                        Screen.TimelyDuel,
                        Screen.AILab,
                        Screen.Settings,
                        Screen.Analytics -> Screen.Home.name
                        Screen.Home -> Screen.Home.name
                    }
                }

                BackHandler(enabled = true) {
                    goBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07051A)
                ) {
                    when (currentScreen) {
                        Screen.LanguageSelection -> {
                            val openedFromSettings = screenBackStack.lastOrNull() == Screen.Settings.name
                            LanguageSelectionScreen(
                                onNavigateToWelcome = {
                                    if (openedFromSettings) goBack() else navigateTo(Screen.WelcomeAboard)
                                },
                                onNavigateBack = { goBack() }
                            )
                        }
                        Screen.WelcomeAboard -> {
                            WelcomeAboardScreen(
                                onNavigateToLanguage = { goBack() },
                                onNavigateToAssessment = { navigateTo(Screen.UserProfileSetup) },
                                onNavigateToProfileSetup = { navigateTo(Screen.UserProfileSetup) }
                            )
                        }
                        Screen.UserProfileSetup -> {
                            UserProfileSetupScreen(
                                onNavigateBack = { goBack() },
                                onSetupComplete = { navigateTo(Screen.Assessment) },
                                onExistingUserRestored = { navigateTo(Screen.Home) }
                            )
                        }
                        Screen.Assessment -> {
                            AssessmentScreen(
                                onNavigateToLearningPath = {
                                    roadmapLaunchedFromAssessment = true
                                    navigateTo(Screen.LearningPathRoadmap)
                                },
                                onNavigateHome = {
                                    roadmapLaunchedFromAssessment = false
                                    resetTo(Screen.Home)
                                },
                                onNavigateBack = { goBack() }
                            )
                        }
                        Screen.LearningPathRoadmap -> {
                            LearningPathRoadmapScreen(
                                launchedFromAssessment = roadmapLaunchedFromAssessment,
                                // Back: go to Assessment if we came from there, otherwise Home
                                onNavigateHome = { resetTo(Screen.Home) },
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    navigateTo(Screen.Lesson)
                                },
                                onNavigateToSettings = { navigateTo(Screen.Settings) },
                                onNavigateBackToAssessment = { goBack() }
                            )
                        }
                        Screen.ActualLearningPath -> {
                            ActualLearningPathScreen(
                                onNavigateToHome = { resetTo(Screen.Home) },
                                onNavigateBack = { goBack() },
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    navigateTo(Screen.Lesson)
                                },
                                onNavigateToAILab = { navigateTo(Screen.AILab) },
                                onNavigateToVocab = { navigateTo(Screen.VocabBuilder) },
                                onNavigateToVault = { navigateTo(Screen.MistakeVault) },
                                onNavigateToSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                        Screen.Home -> {
                            HomeScreen(
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    navigateTo(Screen.Lesson)
                                },
                                onNavigateToVocab = { navigateTo(Screen.VocabBuilder) },
                                onNavigateToMistakes = { navigateTo(Screen.MistakeVault) },
                                onNavigateToFlashcards = { navigateTo(Screen.Flashcards) },
                                onNavigateToDuel = { navigateTo(Screen.TimelyDuel) },
                                onNavigateToAILab = { navigateTo(Screen.AILab) },
                                onNavigateToSettings = { navigateTo(Screen.Settings) },
                                onNavigateToRoadmap = {
                                    roadmapLaunchedFromAssessment = false
                                    navigateTo(Screen.LearningPathRoadmap)
                                },
                                onNavigateToActualLearningPath = { navigateTo(Screen.ActualLearningPath) },
                                onNavigateToProgress = { navigateTo(Screen.Analytics) }
                            )
                        }
                        Screen.Lesson -> {
                            LessonScreen(
                                sublessonId = currentSublessonId,
                                onNavigateBack = { goBack() }
                            )
                        }
                        Screen.VocabBuilder -> {
                            VocabBuilderScreen(
                                onNavigateBack = { goBack() },
                                onNavigateToHome = { resetTo(Screen.Home) },
                                onNavigateToAILab = { navigateTo(Screen.AILab) },
                                onNavigateToMistakes = { navigateTo(Screen.MistakeVault) },
                                onNavigateToSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                        Screen.MistakeVault -> {
                            MistakeVaultScreen(
                                onNavigateBack    = { goBack() },
                                onNavigateToHome  = { resetTo(Screen.Home) },
                                onNavigateToVocab = { navigateTo(Screen.VocabBuilder) },
                                onNavigateToAILab = { navigateTo(Screen.AILab) },
                                onNavigateToSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                        Screen.Flashcards -> {
                            FlashcardScreen(
                                onNavigateBack = { goBack() }
                            )
                        }
                        Screen.TimelyDuel -> {
                            TimelyDuelScreen(
                                onNavigateBack = { goBack() },
                                onNavigateToSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                        Screen.AILab -> {
                            AILabScreen(
                                onNavigateBack = { goBack() },
                                onNavigateToHome = { resetTo(Screen.Home) },
                                onNavigateToVocab = { navigateTo(Screen.VocabBuilder) },
                                onNavigateToMistakes = { navigateTo(Screen.MistakeVault) },
                                onNavigateToSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { goBack() },
                                onNavigateToLanguage = { navigateTo(Screen.LanguageSelection) },
                                onLogout = {
                                    startActivity(
                                        Intent(this@MainActivity, MainActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        }
                                    )
                                    finish()
                                }
                            )
                        }
                        Screen.Analytics -> {
                            ProgressScreen(
                                onNavigateBack = { goBack() },
                                onNavigateToSettings = { navigateTo(Screen.Settings) }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    private fun resolveStartScreen(): Screen {
        val preferences = getSharedPreferences("LingoCoachPrefs", MODE_PRIVATE)
        val languageSelected = preferences.getBoolean("lang_selected", false)
        val onboardingCompleted = preferences.getBoolean("onboarding_completed", false)
        val personalizationDone = preferences.getBoolean("personalization_done", false)
        val assessmentCompleted = preferences.getBoolean("assessment_completed", false)
        val sessionId = preferences.getString("session_id", "").orEmpty()
        val assessmentJson = preferences.getString("assessment_response_json", "").orEmpty()
        val hasValidAssessment = assessmentCompleted && sessionId.isNotBlank() && assessmentJson.isNotBlank()

        if (assessmentCompleted && !hasValidAssessment) {
            preferences.edit()
                .remove("assessment_completed")
                .remove("session_id")
                .remove("assessment_response_json")
                .apply()
        }

        return when {
            hasValidAssessment -> Screen.Home
            languageSelected && onboardingCompleted && personalizationDone -> Screen.Assessment
            languageSelected && onboardingCompleted -> Screen.UserProfileSetup
            languageSelected -> Screen.WelcomeAboard
            else -> Screen.LanguageSelection
        }
    }

}
