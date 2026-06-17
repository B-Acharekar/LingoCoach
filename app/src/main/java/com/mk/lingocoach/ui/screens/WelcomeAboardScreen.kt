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
import com.mk.lingocoach.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeAboardScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    val features = remember {
        listOf(
            OnboardingFeature(
                title = "AI Language Coach",
                description = "Master any language with personalized feedback tailored to your unique pace.",
                icon = Icons.Default.Psychology,
                gradientColors = listOf(Color(0xFF6A5CFF), Color(0xFF9B7DFF))
            ),
            OnboardingFeature(
                title = "Interactive Conversations",
                description = "Practice speaking naturally with our AI tutor in a variety of real-life scenarios.",
                icon = Icons.Default.Forum,
                gradientColors = listOf(Color(0xFF00C9FF), Color(0xFF84FAB0))
            ),
            OnboardingFeature(
                title = "Real-time Feedback",
                description = "Receive instant feedback on your accent, grammar, and fluency to track daily progress.",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFFE9669))
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { features.size })
    val coroutineScope = rememberCoroutineScope()

    // Animation states
    var pageOffset by remember { mutableStateOf(0f) }
    val iconRotation = remember { Animatable(0f) }
    val titleScale = remember { Animatable(1f) }

    // Animated colors based on current page
    val primaryColor by animateColorAsState(
        targetValue = features[pagerState.currentPage].gradientColors.first(),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (pagerState.currentPage + pageOffset) / (features.size - 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
    )

    // Load icons with animation when page changes
    LaunchedEffect(pagerState.currentPage) {
        // Rotate icon on page change
        iconRotation.animateTo(
            targetValue = (pagerState.currentPage + 1) * 360f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        
        // Pulse title on page change
        titleScale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        ).apply {
            titleScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
            )
        }
    }

    // Background gradient overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Background Image with reduced opacity for readability
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Back and Skip Buttons
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400)
                ),
                exit = shrinkOut(
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable Back button area to navigate back to LanguageSelection
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToLanguage() }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Languages",
                            style = TextStyle(
                                color = primaryColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(primaryColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                sharedPreferences.edit()
                                    .putBoolean("onboarding_completed", true)
                                    .apply()
                                onNavigateToAssessment()
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SKIP",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Title and Subtitle Header with animated entrance
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500)
                ),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Animated Title with dynamic scale
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Draw background glow
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.1f),
                                    radius = 80.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height / 2f - 40.dp.toPx())
                                )
                            }
                    ) {
                        Text(
                            text = "Welcome Aboard!",
                            style = TextStyle(
                                color = Color(0xFF1D1D1F),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = ((1f - titleScale.value) * 20f).dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your journey to linguistic mastery starts with LingoCoach.",
                        style = TextStyle(
                            color = Color(0xFF6E6E73),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Horizontal Pager Carousel with animated cards
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 24.dp,
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                val feature = features[page]
                
                // Calculate animation progress for this page
                val animatedScale by animateFloatAsState(
                    targetValue = if (pagerState.currentPage == page) 1f else 0.95f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
                
                val animatedOpacity by animateFloatAsState(
                    targetValue = if (pagerState.currentPage == page) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .scale(animatedScale)
                        .alpha(animatedOpacity)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            ambientColor = primaryColor.copy(alpha = 0.15f),
                            spotColor = primaryColor.copy(alpha = 0.1f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated Icon Container with gradient
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    color = feature.gradientColors[0]
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating Icon Animation
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = feature.title,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(42.dp)
                                    .rotate(iconRotation.value.toFloat())
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Feature Title with animated color
                        Text(
                            text = feature.title,
                            style = TextStyle(
                                color = feature.gradientColors[0],
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Feature Description
                        Text(
                            text = feature.description,
                            style = TextStyle(
                                color = Color(0xFF6E6E73),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Decorative bottom accent
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(feature.gradientColors[0])
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Animated Dot Page Indicators
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(features.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val progress by derivedStateOf {
                            if (features.size > 1) {
                                val totalOffset = pagerState.currentPage + pageOffset
                                val targetProgress = (totalOffset - index).coerceIn(0f, 1f)
                                targetProgress
                            } else {
                                1f
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) primaryColor else Color(0xFFD2D2D7)
                                )
                        ) {
                            // Progress fill animation for selected dot
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFFFFF))
                                        .alpha(
                                            animateFloatAsState(
                                                targetValue = if (progress > 0.5f) 0.3f else 0f,
                                                animationSpec = tween(100)
                                            ).value
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Get Started Button with animated progress
            Button(
                onClick = {
                    sharedPreferences.edit()
                        .putBoolean("onboarding_completed", true)
                        .apply()
                    onNavigateToAssessment()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(60.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(30.dp),
                        clip = false,
                        ambientColor = primaryColor.copy(alpha = 0.3f),
                        spotColor = primaryColor.copy(alpha = 0.2f)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Get Started",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Arrow Forward",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(
                                if (pagerState.currentPage == features.size - 1) 0f else 180f
                            )
                    )
                }
            }
            
            // Progress bar at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(primaryColor)
                )
            }
        }
    }
}

data class OnboardingFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)