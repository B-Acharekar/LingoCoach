package com.mk.lingocoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mk.lingocoach.ui.screens.HomeScreen
import com.mk.lingocoach.ui.screens.LanguageSelectionScreen
import com.mk.lingocoach.ui.screens.LessonScreen
import com.mk.lingocoach.ui.screens.SplashScreen
import com.mk.lingocoach.ui.theme.LingoCoachTheme

enum class Screen {
    Splash,
    LanguageSelection,
    WelcomeAboard,
    Assessment,
    LearningPath,
    Home,
    Lesson
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LingoCoachTheme(dynamicColor = false) {
                var currentScreen by remember { mutableStateOf(Screen.Splash) }
                var currentSublessonId by remember { mutableStateOf("") }

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
                            com.mk.lingocoach.ui.screens.WelcomeAboardScreen(
                                onNavigateToLanguage = { currentScreen = Screen.LanguageSelection },
                                onNavigateToAssessment = { currentScreen = Screen.Assessment }
                            )
                        }
                        Screen.Assessment -> {
                            com.mk.lingocoach.ui.screens.AssessmentScreen(
                                onNavigateToLearningPath = { currentScreen = Screen.LearningPath },
                                onNavigateBack = { currentScreen = Screen.WelcomeAboard }
                            )
                        }
                        Screen.LearningPath -> {
                            com.mk.lingocoach.ui.screens.LearningPathScreen(
                                onNavigateToHome = { currentScreen = Screen.Home },
                                onNavigateBack = { currentScreen = Screen.Assessment }
                            )
                        }
                        Screen.Home -> {
                            HomeScreen(
                                onNavigateToLesson = { sublessonId ->
                                    currentSublessonId = sublessonId
                                    currentScreen = Screen.Lesson
                                }
                            )
                        }
                        Screen.Lesson -> {
                            LessonScreen(
                                sublessonId = currentSublessonId,
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}