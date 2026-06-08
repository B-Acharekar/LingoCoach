package com.kazuki.lingocoach

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
import com.kazuki.lingocoach.ui.screens.HomeScreen
import com.kazuki.lingocoach.ui.screens.LanguageSelectionScreen
import com.kazuki.lingocoach.ui.screens.SplashScreen
import com.kazuki.lingocoach.ui.theme.LingoCoachTheme

enum class Screen {
    Splash,
    LanguageSelection,
    Home
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LingoCoachTheme(dynamicColor = false) {
                var currentScreen by remember { mutableStateOf(Screen.Splash) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07051A)
                ) {
                    when (currentScreen) {
                        Screen.Splash -> {
                            SplashScreen(
                                onNavigateToLanguage = { currentScreen = Screen.LanguageSelection },
                                onNavigateToHome = { currentScreen = Screen.Home }
                            )
                        }
                        Screen.LanguageSelection -> {
                            LanguageSelectionScreen(
                                onNavigateToHome = { currentScreen = Screen.Home }
                            )
                        }
                        Screen.Home -> {
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}