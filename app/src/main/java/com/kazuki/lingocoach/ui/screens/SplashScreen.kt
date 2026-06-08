package com.kazuki.lingocoach.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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

@Composable
fun SplashScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        // 1. Prompt for notifications permission.
        // requestPermission is a suspend function in OneSignal v5.x
        try {
            Log.d("SplashScreen", "Requesting OneSignal notification permissions...")
            OneSignal.Notifications.requestPermission(true)
        } catch (e: Exception) {
            Log.e("SplashScreen", "Failed to request notification permission: ${e.message}", e)
        }

        // 2. Wait at least 2.5 seconds to display the logo and lottie animation beautifully
        delay(2500)

        // 3. Navigate to Language Selection or Home screen based on preference
        val isLangSelected = sharedPreferences.getBoolean("lang_selected", false)
        if (isLangSelected) {
            onNavigateToHome()
        } else {
            onNavigateToLanguage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07051A),
                        Color(0xFF0F0C2F),
                        Color(0xFF1B1748)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background circle 1
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-100).dp)
                .background(Color(0x127649FE), shape = CircleShape)
                .blur(80.dp)
        )

        // Glowing background circle 2
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 100.dp, y = 180.dp)
                .background(Color(0x0C8A2BE2), shape = CircleShape)
                .blur(60.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // White rounded box containing the Lottie Animation
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFE5E5ED))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Play Lottie Animation using dotlottie-android SDK
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
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tagline
            Text(
                text = "AI-POWERED LANGUAGE COACH",
                style = TextStyle(
                    color = Color(0xFF8F8EA0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            )
        }
    }
}
