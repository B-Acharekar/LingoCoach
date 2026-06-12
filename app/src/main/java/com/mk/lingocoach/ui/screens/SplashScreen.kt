package com.mk.lingocoach.ui.screens

import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.onesignal.OneSignal
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        // 1. Prompt for notifications permission.
        try {
            Log.d("SplashScreen", "Requesting OneSignal notification permissions...")
            OneSignal.Notifications.requestPermission(true)
        } catch (e: Exception) {
            Log.e("SplashScreen", "Failed to request notification permission: ${e.message}", e)
        }

        // 2. Wait at least 2.5 seconds to display the logo and lottie animation beautifully
        delay(2500)

        // 3. Navigate to appropriate screen based on onboarding & language selection status
        val isLangSelected = sharedPreferences.getBoolean("lang_selected", false)
        val onboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)
        val assessmentCompleted = sharedPreferences.getBoolean("assessment_completed", false)

        if (isLangSelected && onboardingCompleted && assessmentCompleted) {
            onNavigateToHome()
        } else if (!isLangSelected) {
            onNavigateToLanguage()
        } else if (!onboardingCompleted) {
            onNavigateToWelcome()
        } else {
            onNavigateToAssessment()
        }
    }

    // Animate angle for the continuous gradient flow
    val infiniteTransition = rememberInfiniteTransition(label = "gradientFlow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Calculate dynamic start/end coordinates based on the rotation angle
        val angleRad = Math.toRadians(angle.toDouble())
        val xOffset = cos(angleRad).toFloat()
        val yOffset = sin(angleRad).toFloat()

        val startOffset = Offset(
            x = (width / 2f) + xOffset * (width / 2f),
            y = (height / 2f) + yOffset * (height / 2f)
        )
        val endOffset = Offset(
            x = (width / 2f) - xOffset * (width / 2f),
            y = (height / 2f) - yOffset * (height / 2f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6A5CFF), // primary
                            Color(0xFF8A7CFF), // secondary
                            Color(0xFFFFC83D)  // tertiary
                        ),
                        start = startOffset,
                        end = endOffset
                    )
                )
        ) {
            // Glowing background circle 1
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .offset(x = (-80).dp, y = (-100).dp)
                    .background(Color(0x1A6A5CFF), shape = CircleShape)
                    .blur(80.dp)
            )

            // Glowing background circle 2
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = 100.dp, y = 180.dp)
                    .background(Color(0x15FFC83D), shape = CircleShape)
                    .blur(60.dp)
            )

            // Main Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // White rounded box containing the Lottie Animation with premium shadow
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp), clip = false)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DotLottieAnimation(
                        source = DotLottieSource.Asset("LingoCoach.lottie"),
                        autoplay = true,
                        loop = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Application Brand Name
                Text(
                    text = "LingoCoach",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sub-tagline with side line dividers matching Image 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0x40FFFFFF))
                    )
                    Text(
                        text = "SPEAK WITH CONFIDENCE",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
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
                            .background(Color(0x40FFFFFF))
                    )
                }
            }

            // Bottom decorative glowing gradient bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6A5CFF), // primary
                                Color(0xFFFFC83D)  // tertiary
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
                    .offset(y = (-60).dp)
            )
        }
    }
}
