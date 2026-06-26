package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mk.lingocoach.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeAboardScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToProfileSetup: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val cards =
        listOf(
            OnboardingCarouselCard(
                title = stringResource(R.string.ai_language_coach_title),
                description = stringResource(R.string.ai_language_coach_desc),
                icon = Icons.Default.Psychology,
                color = Color(0xFF6A5CFF),
                animationAsset = "AI_LANG.lottie"
            ),
            OnboardingCarouselCard(
                title = stringResource(R.string.interactive_conversations_title),
                description = stringResource(R.string.interactive_conversations_desc),
                icon = Icons.Default.Forum,
                color = Color(0xFF00A3FF),
                animationAsset = "interact_convo.lottie"
            ),
            OnboardingCarouselCard(
                title = stringResource(R.string.progress_that_sticks_title),
                description = stringResource(R.string.progress_that_sticks_desc),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = Color(0xFFFF8A3D),
                animationAsset = "progresstick.lottie"
            )
        )
    val pagerState = rememberPagerState(pageCount = { cards.size })

    fun finishOnboarding() {
        sharedPreferences.edit()
            .putBoolean("onboarding_completed", true)
            .apply()
        onNavigateToProfileSetup()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F4FF))
    ) {
        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.82f))
                        .clickable { onNavigateToLanguage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF6A5CFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    stringResource(R.string.app_brand_short),
                    color = Color(0xFF6A5CFF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    stringResource(R.string.skip),
                    color = Color(0xFF6A5CFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.82f))
                        .clickable { finishOnboarding() }
                        .padding(horizontal = 15.dp, vertical = 9.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.welcome_aboard_plain),
                    color = Color(0xFF101018),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.welcome_swipe_intro),
                    color = Color(0xFF686875),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(18.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    pageSpacing = 14.dp
                ) { page ->
                    OnboardingAnimatedCard(cards[page])
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    repeat(cards.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (selected) 22.dp else 7.dp, height = 7.dp)
                                .clip(CircleShape)
                                .background(if (selected) cards[index].color else Color(0xFFD8D3EE))
                        )
                    }
                }
            }

            Button(
                onClick = ::finishOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.continue_text), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OnboardingAnimatedCard(card: OnboardingCarouselCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = card.color.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(card.color.copy(alpha = 0.10f), Color(0xFFF8F7FF))
                        )
                    )
                    .border(1.dp, card.color.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                DotLottieAnimation(
                    source = DotLottieSource.Asset(card.animationAsset),
                    autoplay = true,
                    loop = true,
                    speed = 1.35f,
                    useFrameInterpolation = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(238.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(card.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(card.icon, contentDescription = null, tint = card.color, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                card.title,
                color = Color(0xFF171722),
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(7.dp))
            Text(
                card.description,
                color = Color(0xFF747481),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class OnboardingCarouselCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val animationAsset: String
)
