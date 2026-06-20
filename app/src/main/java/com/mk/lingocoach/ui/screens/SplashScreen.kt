package com.mk.lingocoach.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.onesignal.OneSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToProfileSetup: () -> Unit = onNavigateToAssessment,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A5CFF)),
        contentAlignment = Alignment.Center
    ) {
        DotLottieAnimation(
            source = DotLottieSource.Asset("splash.lottie"),
            autoplay = true,
            loop = false,
            speed = 1f,
            useFrameInterpolation = false,
            modifier = Modifier.size(300.dp)
        )
    }

    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

        launch {
            try {
                OneSignal.Notifications.requestPermission(true)
            } catch (error: Exception) {
                Log.e("SplashScreen", "Notification permission failed: ${error.message}", error)
            }
        }

        // splash.lottie is 150 frames at 30 fps.
        delay(5_000)

        val languageSelected = preferences.getBoolean("lang_selected", false)
        val onboardingCompleted = preferences.getBoolean("onboarding_completed", false)
        val personalizationDone = preferences.getBoolean("personalization_done", false)
        val assessmentCompleted = preferences.getBoolean("assessment_completed", false)

        when {
            assessmentCompleted -> onNavigateToHome()
            languageSelected && onboardingCompleted && personalizationDone -> onNavigateToAssessment()
            languageSelected && onboardingCompleted -> onNavigateToProfileSetup()
            languageSelected -> onNavigateToWelcome()
            else -> onNavigateToLanguage()
        }
    }
}
