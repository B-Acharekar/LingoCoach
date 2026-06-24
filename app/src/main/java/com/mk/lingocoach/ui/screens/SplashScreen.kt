package com.mk.lingocoach.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.onesignal.OneSignal
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
    val splashProgress = remember { Animatable(0f) }
    val progressPercent = (splashProgress.value * 100).toInt().coerceIn(0, 100)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(220.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$progressPercent%",
                color = Color(0xFF6A5CFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { splashProgress.value },
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(200.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFF6A5CFF),
                trackColor = Color(0xFF6A5CFF).copy(alpha = 0.18f)
            )
        }
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

        splashProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_800, easing = LinearEasing)
        )

        val languageSelected = preferences.getBoolean("lang_selected", false)
        val onboardingCompleted = preferences.getBoolean("onboarding_completed", false)
        val personalizationDone = preferences.getBoolean("personalization_done", false)
        val assessmentCompleted = preferences.getBoolean("assessment_completed", false)
        val sessionId = preferences.getString("session_id", "").orEmpty()
        val assessmentJson = preferences.getString("assessment_response_json", "").orEmpty()
        val hasValidCompletedAssessment = assessmentCompleted && sessionId.isNotBlank() && assessmentJson.isNotBlank()

        if (assessmentCompleted && !hasValidCompletedAssessment) {
            preferences.edit()
                .remove("assessment_completed")
                .remove("session_id")
                .remove("assessment_response_json")
                .apply()
        }

        when {
            hasValidCompletedAssessment -> onNavigateToHome()
            languageSelected && onboardingCompleted && personalizationDone -> onNavigateToAssessment()
            languageSelected && onboardingCompleted -> onNavigateToProfileSetup()
            languageSelected -> onNavigateToWelcome()
            else -> onNavigateToLanguage()
        }
    }
}
