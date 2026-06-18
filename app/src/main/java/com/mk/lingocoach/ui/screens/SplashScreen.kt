package com.mk.lingocoach.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.material3.Text
import com.onesignal.OneSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Brand colors
private val SplashPurple = Color(0xFF6A5CFF)
private val SplashViolet = Color(0xFFBA7CFF)
private val SplashAmber  = Color(0xFFFFC83D)

@Composable
private fun SplashLogoVideo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mediaPlayer = remember(context) {
        MediaPlayer().apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer.setOnPreparedListener(null)
            mediaPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextureView(viewContext).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var dataSourceSet = false
                    private var prepared = false

                    override fun onSurfaceTextureAvailable(
                        surfaceTexture: android.graphics.SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        val surface = Surface(surfaceTexture)
                        mediaPlayer.setSurface(surface)
                        surface.release()

                        if (!dataSourceSet) {
                            viewContext.assets.openFd("icon.mp4").use { asset ->
                                mediaPlayer.setDataSource(
                                    asset.fileDescriptor,
                                    asset.startOffset,
                                    asset.length
                                )
                            }
                            dataSourceSet = true
                            mediaPlayer.setOnPreparedListener { player ->
                                prepared = true
                                player.start()
                            }
                            mediaPlayer.prepareAsync()
                        } else if (prepared) {
                            mediaPlayer.start()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surfaceTexture: android.graphics.SurfaceTexture,
                        width: Int,
                        height: Int
                    ) = Unit

                    override fun onSurfaceTextureDestroyed(
                        surfaceTexture: android.graphics.SurfaceTexture
                    ): Boolean {
                        if (prepared && mediaPlayer.isPlaying) mediaPlayer.pause()
                        mediaPlayer.setSurface(null)
                        return true
                    }

                    override fun onSurfaceTextureUpdated(
                        surfaceTexture: android.graphics.SurfaceTexture
                    ) = Unit
                }
            }
        }
    )
}

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

    // Entry animation states for the central content layout
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.85f) }
    val contentOffsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        // Handle notification registration safely
        try {
            OneSignal.Notifications.requestPermission(true)
        } catch (e: Exception) {
            Log.e("SplashScreen", "Notification permission failed: ${e.message}", e)
        }

        // Run entry animations smoothly in parallel
        launch {
            delay(200)
            contentAlpha.animateTo(1f, tween(600))
        }
        launch {
            delay(200)
            contentScale.animateTo(1f, tween(600))
        }
        launch {
            delay(200)
            contentOffsetY.animateTo(0f, tween(600))
        }

        // Total viewport screen hold duration
        delay(2200)

        val isLangSelected      = sharedPreferences.getBoolean("lang_selected", false)
        val onboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)
        val personalizationDone = sharedPreferences.getBoolean("personalization_done", false)
        val assessmentCompleted = sharedPreferences.getBoolean("assessment_completed", false)

        // Navigation state machine logic: Splash -> Language -> Onboarding -> Personalization -> Assessment -> Home
        // assessment_completed is the terminal flag — if it's set, the user has finished everything
        when {
            assessmentCompleted ->
                onNavigateToHome()
            isLangSelected && onboardingCompleted && personalizationDone ->
                onNavigateToAssessment()
            isLangSelected && onboardingCompleted ->
                onNavigateToProfileSetup()  // This is Personalization screen
            isLangSelected ->
                onNavigateToWelcome()       // Go to onboarding
            else ->
                onNavigateToLanguage()      // Go to language selection
        }
    }

    // Background linear gradient rotation pipeline
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

    // Dynamic alpha pulsing loop for the lower visual bar accent
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barPulse"
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

        // Ambient background color canvas
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
            // Animated UI Container Block (Logo + Typography Elements)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        scaleX = contentScale.value
                        scaleY = contentScale.value
                        translationY = contentOffsetY.value
                    }
            ) {
                SplashLogoVideo(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(30.dp))
                )

                Spacer(modifier = Modifier.height(36.dp))

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

            // Bottom accent indicator bar with dynamic pulse behavior applied
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .graphicsLayer { alpha = pulseAlpha }
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
