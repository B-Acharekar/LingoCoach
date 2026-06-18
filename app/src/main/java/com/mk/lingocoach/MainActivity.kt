package com.mk.lingocoach

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mk.lingocoach.ui.screens.*
import com.mk.lingocoach.ui.theme.LingoCoachTheme
import com.mk.lingocoach.notifications.NotificationScheduler
import com.onesignal.OneSignal

enum class Screen {
    Splash,
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

        // Restore locale on cold start
        val savedLang = getSharedPreferences("language_preferences_mirror", MODE_PRIVATE)
            .getString("selected_language", null)
        if (!savedLang.isNullOrEmpty() && savedLang != "system") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
        }

        // Initialize OneSignal
        OneSignal.initWithContext(this, "46957d03-f3f9-435c-b76c-e5cd0b8089b5")

        // Schedule daily reminder notifications at 10am and 7pm
        NotificationScheduler.scheduleDailyReminders(this)

        enableEdgeToEdge()
        setContent {
            LingoCoachTheme(dynamicColor = false) {
                var currentScreenName by rememberSaveable { mutableStateOf(Screen.Splash.name) }
                var currentSublessonId by rememberSaveable { mutableStateOf("") }
                val currentScreen = runCatching { Screen.valueOf(currentScreenName) }.getOrDefault(Screen.Splash)

                // Tracks where the user came from before opening the Roadmap,
                // so the back arrow can return to the correct screen.
                var roadmapLaunchedFromAssessment by rememberSaveable { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07051A)
                ) {
                    when (currentScreen) {
                        Screen.Splash -> {
                            SplashScreen(
                                onNavigateToWelcome = { currentScreenName = Screen.WelcomeAboard.name },
                                onNavigateToLanguage = { currentScreenName = Screen.LanguageSelection.name },
                                onNavigateToAssessment = { currentScreenName = Screen.Assessment.name },
                                onNavigateToProfileSetup = { currentScreenName = Screen.UserProfileSetup.name },
                                onNavigateToHome = { currentScreenName = Screen.Home.name }
                            )
                        }
                        Screen.LanguageSelection -> {
                            LanguageSelectionScreen(
                                onNavigateToWelcome = { currentScreenName = Screen.WelcomeAboard.name },
                                onNavigateBack = { currentScreenName = Screen.Splash.name }
                            )
                        }
                        Screen.WelcomeAboard -> {
                            WelcomeAboardScreen(
                                onNavigateToLanguage = { currentScreenName = Screen.LanguageSelection.name },
                                onNavigateToAssessment = { currentScreenName = Screen.UserProfileSetup.name },
                                onNavigateToProfileSetup = { currentScreenName = Screen.UserProfileSetup.name }
                            )
                        }
                        Screen.UserProfileSetup -> {
                            UserProfileSetupScreen(
                                onNavigateBack = { currentScreenName = Screen.WelcomeAboard.name },
                                onSetupComplete = { currentScreenName = Screen.Assessment.name }
                            )
                        }
                        Screen.Assessment -> {
                            AssessmentScreen(
                                onNavigateToLearningPath = {
                                    roadmapLaunchedFromAssessment = true
                                    currentScreenName = Screen.LearningPathRoadmap.name
                                },
                                onNavigateBack = { currentScreenName = Screen.WelcomeAboard.name }
                            )
                        }
                        Screen.LearningPathRoadmap -> {
                            LearningPathRoadmapScreen(
                                launchedFromAssessment = roadmapLaunchedFromAssessment,
                                // Back: go to Assessment if we came from there, otherwise Home
                                onNavigateHome = { currentScreenName = Screen.Home.name },
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreenName = Screen.Lesson.name
                                },
                                onNavigateToSettings = { currentScreenName = Screen.Settings.name },
                                onNavigateBackToAssessment = { currentScreenName = Screen.Assessment.name }
                            )
                        }
                        Screen.ActualLearningPath -> {
                            ActualLearningPathScreen(
                                onNavigateToHome = { currentScreenName = Screen.Home.name },
                                onNavigateBack = { currentScreenName = Screen.LearningPathRoadmap.name },
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreenName = Screen.Lesson.name
                                },
                                onNavigateToAILab = { currentScreenName = Screen.AILab.name },
                                onNavigateToVocab = { currentScreenName = Screen.VocabBuilder.name },
                                onNavigateToVault = { currentScreenName = Screen.MistakeVault.name }
                            )
                        }
                        Screen.Home -> {
                            HomeScreen(
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreenName = Screen.Lesson.name
                                },
                                onNavigateToVocab = { currentScreenName = Screen.VocabBuilder.name },
                                onNavigateToMistakes = { currentScreenName = Screen.MistakeVault.name },
                                onNavigateToFlashcards = { currentScreenName = Screen.Flashcards.name },
                                onNavigateToDuel = { currentScreenName = Screen.TimelyDuel.name },
                                onNavigateToAILab = { currentScreenName = Screen.AILab.name },
                                onNavigateToSettings = { currentScreenName = Screen.Settings.name },
                                onNavigateToRoadmap = {
                                    roadmapLaunchedFromAssessment = false
                                    currentScreenName = Screen.LearningPathRoadmap.name
                                },
                                onNavigateToActualLearningPath = { currentScreenName = Screen.ActualLearningPath.name },
                                onNavigateToProgress = { currentScreenName = Screen.Analytics.name }
                            )
                        }
                        Screen.Lesson -> {
                            LessonScreen(
                                sublessonId = currentSublessonId,
                                onNavigateBack = { currentScreenName = Screen.ActualLearningPath.name }
                            )
                        }
                        Screen.VocabBuilder -> {
                            VocabBuilderScreen(
                                onNavigateBack = { currentScreenName = Screen.Home.name },
                                onNavigateToHome = { currentScreenName = Screen.Home.name },
                                onNavigateToAILab = { currentScreenName = Screen.AILab.name },
                                onNavigateToMistakes = { currentScreenName = Screen.MistakeVault.name }
                            )
                        }
                        Screen.MistakeVault -> {
                            MistakeVaultScreen(
                                onNavigateBack    = { currentScreenName = Screen.Home.name },
                                onNavigateToHome  = { currentScreenName = Screen.Home.name },
                                onNavigateToVocab = { currentScreenName = Screen.VocabBuilder.name },
                                onNavigateToAILab = { currentScreenName = Screen.AILab.name }
                            )
                        }
                        Screen.Flashcards -> {
                            FlashcardScreen(
                                onNavigateBack = { currentScreenName = Screen.Home.name }
                            )
                        }
                        Screen.TimelyDuel -> {
                            TimelyDuelScreen(
                                onNavigateBack = { currentScreenName = Screen.Home.name }
                            )
                        }
                        Screen.AILab -> {
                            AILabScreen(
                                onNavigateBack = { currentScreenName = Screen.Home.name },
                                onNavigateToHome = { currentScreenName = Screen.Home.name },
                                onNavigateToVocab = { currentScreenName = Screen.VocabBuilder.name },
                                onNavigateToMistakes = { currentScreenName = Screen.MistakeVault.name }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreenName = Screen.Home.name },
                                onLogout = { currentScreenName = Screen.Splash.name }
                            )
                        }
                        Screen.Analytics -> {
                            ProgressScreen(
                                onNavigateBack = { currentScreenName = Screen.Home.name }
                            )
                        }
                    }
                }
            }
        }
    }
}

