package com.mk.lingocoach.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.mk.lingocoach.R
import com.onesignal.OneSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Brand colors
private val SplashPurple  = Color(0xFF6A5CFF)
private val SplashViolet  = Color(0xFFBA7CFF)
private val SplashAmber   = Color(0xFFFFC83D)

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToProfileSetup: () -> Unit = onNavigateToAssessment,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    // Lottie animation plays automatically — no manual scale animation needed
    val textAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Start notification permission request
        try {
            OneSignal.Notifications.requestPermission(true)
        } catch (e: Exception) {
            Log.e("SplashScreen", "Notification permission failed: ${e.message}", e)
        }

        // Fade in brand text after a short delay while Lottie plays
        delay(600)
        textAlpha.animateTo(1f, tween(500))

        // Hold then navigate
        delay(1200)

        val isLangSelected      = sharedPreferences.getBoolean("lang_selected", false)
        val onboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)
        val profileSetupDone    = sharedPreferences.getBoolean("profile_setup_done", false)
        val assessmentCompleted = sharedPreferences.getBoolean("assessment_completed", false)

        when {
            isLangSelected && onboardingCompleted && profileSetupDone && assessmentCompleted ->
                onNavigateToHome()
            isLangSelected && onboardingCompleted && profileSetupDone ->
                onNavigateToAssessment()
            isLangSelected && onboardingCompleted ->
                onNavigateToProfileSetup()
            isLangSelected ->
                onNavigateToWelcome()
            else ->
                onNavigateToLanguage()
        }
    }

    // Continuously rotating gradient angle
    val infiniteTransition = rememberInfiniteTransition(label = "gradFlow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradAngle"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        val rad    = Math.toRadians(angle.toDouble())
        val cosA   = cos(rad).toFloat()
        val sinA   = sin(rad).toFloat()
        val startX = w / 2f + cosA * (w * 0.8f)
        val startY = h / 2f + sinA * (h * 0.8f)
        val endX   = w / 2f - cosA * (w * 0.8f)
        val endY   = h / 2f - sinA * (h * 0.8f)

        // Pure rotating 3-color gradient — no circles, no blur boxes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f  to SplashPurple,
                            0.45f to SplashViolet,
                            1.0f  to SplashAmber
                        ),
                        start = Offset(startX, startY),
                        end   = Offset(endX, endY)
                    )
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Lottie animation — LingoCoach.lottie from assets
                DotLottieAnimation(
                    source    = DotLottieSource.Asset("LingoCoach.lottie"),
                    autoplay  = true,
                    loop      = true,
                    modifier  = Modifier.size(220.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Brand name — fades in after logo
                Box(modifier = Modifier.graphicsLayer { alpha = textAlpha.value }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LingoCoach",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Divider + tagline
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                            Text(
                                text = "SPEAK WITH CONFIDENCE",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }

            // Bottom accent bar — gradient pulse matching the background colors
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(SplashPurple, SplashViolet, SplashAmber)
                        )
                    )
                    .align(Alignment.BottomCenter)
                    .offset(y = (-56).dp)
            )
        }
    }
}
