package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.mk.lingocoach.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeAboardScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToProfileSetup: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    val features = remember {
        listOf(
            OnboardingFeature(
                title = "AI Language Coach",
                description = "Get personalized feedback and guidance tailored to your learning pace.",
                icon = Icons.Default.Psychology,
                gradientColors = listOf(Color(0xFF6A5CFF), Color(0xFF9B7DFF))
            ),
            OnboardingFeature(
                title = "Interactive Conversations",
                description = "Practice real-life conversations with our intelligent AI tutor.",
                icon = Icons.Default.Forum,
                gradientColors = listOf(Color(0xFF00C9FF), Color(0xFF84FAB0))
            ),
            OnboardingFeature(
                title = "Real-time Feedback",
                description = "Instantly track your progress with detailed performance insights.",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFFE9669))
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { features.size })
    val coroutineScope = rememberCoroutineScope()

    // Animation states
    var pageOffset by remember { mutableStateOf(0f) }

    // Animated colors based on current page
    val primaryColor by animateColorAsState(
        targetValue = features[pagerState.currentPage].gradientColors.first(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (pagerState.currentPage + pageOffset) / (features.size - 1f),
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    // Background gradient overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Back arrow, Progress dots and Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back arrow (clickable)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryColor,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        }
                        .padding(8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    repeat(features.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) primaryColor else Color(0xFFE5E5E7)
                                )
                        )
                    }
                }

                // Skip button
                Text(
                    text = "Skip",
                    style = TextStyle(
                        color = Color(0xFF999999),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            sharedPreferences.edit()
                                .putBoolean("onboarding_completed", true)
                                .apply()
                            onNavigateToProfileSetup()
                        }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main content: Animation space + Text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animation area - shows on first page
                if (pagerState.currentPage == 0) {
                    DotLottieAnimation(
                        source = DotLottieSource.Url("https://lottie.host/adf6baea-da11-4d02-8fdc-4044fe8270f6/yEzG9jPCbt.lottie"),
                        autoplay = true,
                        loop = true,
                        speed = 1.5f,
                        useFrameInterpolation = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    )
                } else if (pagerState.currentPage == 1) {
                    DotLottieAnimation(
                        source = DotLottieSource.Url("https://lottie.host/ba7975e4-bb73-4984-a577-f4ff2220604e/AbMrjn9wby.lottie"),
                        autoplay = true,
                        loop = true,
                        speed = 1.5f,
                        useFrameInterpolation = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    )
                } else if (pagerState.currentPage == 2) {
                    DotLottieAnimation(
                        source = DotLottieSource.Url("https://lottie.host/04364580-082d-4f76-a6a2-19d8eac77e9c/A6BnSRhyix.lottie"),
                        autoplay = true,
                        loop = true,
                        speed = 1.5f,
                        useFrameInterpolation = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    )
                } else {
                    // Placeholder for other pages
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF0F0F5))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE5E5E7),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder text
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Feature content: Title + Description
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    userScrollEnabled = true,
                    pageSpacing = 0.dp
                ) { page ->
                    val feature = features[page]
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Label
                        Text(
                            text = "STEP ${page + 1}",
                            style = TextStyle(
                                color = feature.gradientColors[0],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                            text = feature.title,
                            style = TextStyle(
                                color = Color(0xFF1D1D1F),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = feature.description,
                            style = TextStyle(
                                color = Color(0xFF86868B),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            // Bottom: Action button
            val buttonColor = when (pagerState.currentPage) {
                1 -> Color(0xFFFFC83D)  // Page 2: Amber
                2 -> Color(0xFF4E80FF)  // Page 3: Light blue
                else -> primaryColor     // Page 1: Dynamic color
            }
            
            Button(
                onClick = {
                    if (pagerState.currentPage == features.size - 1) {
                        sharedPreferences.edit()
                            .putBoolean("onboarding_completed", true)
                            .apply()
                        onNavigateToProfileSetup()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == features.size - 1) "Get Started" else "Next",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

data class OnboardingFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)
