package com.mk.lingocoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

class MainActivity : ComponentActivity() {
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
                var currentScreen by remember { mutableStateOf(Screen.Splash) }
                var currentSublessonId by remember { mutableStateOf("") }

                // Tracks where the user came from before opening the Roadmap,
                // so the back arrow can return to the correct screen.
                var roadmapLaunchedFromAssessment by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07051A)
                ) {
                    when (currentScreen) {
                        Screen.Splash -> {
                            SplashScreen(
                                onNavigateToWelcome = { currentScreen = Screen.WelcomeAboard },
                                onNavigateToLanguage = { currentScreen = Screen.LanguageSelection },
                                onNavigateToAssessment = { currentScreen = Screen.Assessment },
                                onNavigateToProfileSetup = { currentScreen = Screen.UserProfileSetup },
                                onNavigateToHome = { currentScreen = Screen.Home }
                            )
                        }
                        Screen.LanguageSelection -> {
                            LanguageSelectionScreen(
                                onNavigateToWelcome = { currentScreen = Screen.WelcomeAboard },
                                onNavigateBack = { currentScreen = Screen.Splash }
                            )
                        }
                        Screen.WelcomeAboard -> {
                            WelcomeAboardScreen(
                                onNavigateToLanguage = { currentScreen = Screen.LanguageSelection },
                                onNavigateToAssessment = { currentScreen = Screen.UserProfileSetup },
                                onNavigateToProfileSetup = { currentScreen = Screen.UserProfileSetup }
                            )
                        }
                        Screen.UserProfileSetup -> {
                            UserProfileSetupScreen(
                                onNavigateBack = { currentScreen = Screen.WelcomeAboard },
                                onSetupComplete = { currentScreen = Screen.Assessment }
                            )
                        }
                        Screen.Assessment -> {
                            AssessmentScreen(
                                onNavigateToLearningPath = {
                                    roadmapLaunchedFromAssessment = true
                                    currentScreen = Screen.LearningPathRoadmap
                                },
                                onNavigateBack = { currentScreen = Screen.WelcomeAboard }
                            )
                        }
                        Screen.LearningPathRoadmap -> {
                            LearningPathRoadmapScreen(
                                launchedFromAssessment = roadmapLaunchedFromAssessment,
                                // Back: go to Assessment if we came from there, otherwise Home
                                onNavigateHome = { currentScreen = Screen.Home },
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreen = Screen.Lesson
                                },
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                onNavigateBackToAssessment = { currentScreen = Screen.Assessment }
                            )
                        }
                        Screen.ActualLearningPath -> {
                            ActualLearningPathScreen(
                                onNavigateToHome = { currentScreen = Screen.Home },
                                onNavigateBack = { currentScreen = Screen.LearningPathRoadmap },
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreen = Screen.Lesson
                                },
                                onNavigateToAILab = { currentScreen = Screen.AILab },
                                onNavigateToVocab = { currentScreen = Screen.VocabBuilder },
                                onNavigateToVault = { currentScreen = Screen.MistakeVault }
                            )
                        }
                        Screen.Home -> {
                            HomeScreen(
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreen = Screen.Lesson
                                },
                                onNavigateToVocab = { currentScreen = Screen.VocabBuilder },
                                onNavigateToMistakes = { currentScreen = Screen.MistakeVault },
                                onNavigateToFlashcards = { currentScreen = Screen.Flashcards },
                                onNavigateToDuel = { currentScreen = Screen.TimelyDuel },
                                onNavigateToAILab = { currentScreen = Screen.AILab },
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                onNavigateToRoadmap = {
                                    roadmapLaunchedFromAssessment = false
                                    currentScreen = Screen.LearningPathRoadmap
                                },
                                onNavigateToActualLearningPath = { currentScreen = Screen.ActualLearningPath },
                                onNavigateToProgress = { currentScreen = Screen.Analytics }
                            )
                        }
                        Screen.Lesson -> {
                            LessonScreen(
                                sublessonId = currentSublessonId,
                                onNavigateBack = { currentScreen = Screen.ActualLearningPath }
                            )
                        }
                        Screen.VocabBuilder -> {
                            VocabBuilderScreen(
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                        Screen.MistakeVault -> {
                            MistakeVaultScreen(
                                onNavigateBack    = { currentScreen = Screen.Home },
                                onNavigateToHome  = { currentScreen = Screen.Home },
                                onNavigateToVocab = { currentScreen = Screen.VocabBuilder },
                                onNavigateToAILab = { currentScreen = Screen.AILab }
                            )
                        }
                        Screen.Flashcards -> {
                            FlashcardScreen(
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                        Screen.TimelyDuel -> {
                            TimelyDuelScreen(
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                        Screen.AILab -> {
                            AILabScreen(
                                onNavigateBack = { currentScreen = Screen.Home },
                                onNavigateToHome = { currentScreen = Screen.Home },
                                onNavigateToVocab = { currentScreen = Screen.VocabBuilder },
                                onNavigateToMistakes = { currentScreen = Screen.MistakeVault }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreen = Screen.Home },
                                onLogout = { currentScreen = Screen.Splash }
                            )
                        }
                        Screen.Analytics -> {
                            ProgressScreen(
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}