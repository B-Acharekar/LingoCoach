package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mk.lingocoach.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeAboardScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToAssessment: () -> Unit,
    onNavigateToProfileSetup: () -> Unit
) {
    val context = LocalContext.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val cards =
        listOf(
            OnboardingCarouselCard(
                title = stringResource(R.string.ai_language_coach_title),
                description = stringResource(R.string.ai_language_coach_desc),
                icon = Icons.Default.Psychology,
                color = Color(0xFF6A5CFF),
                animationAsset = "Translate.json"
            ),
            OnboardingCarouselCard(
                title = stringResource(R.string.interactive_conversations_title),
                description = stringResource(R.string.interactive_conversations_desc),
                icon = Icons.Default.Forum,
                color = Color(0xFF00A3FF),
                animationAsset = "contact.json"
            ),
            OnboardingCarouselCard(
                title = stringResource(R.string.progress_that_sticks_title),
                description = stringResource(R.string.progress_that_sticks_desc),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = Color(0xFFFF8A3D),
                animationAsset = "Completing Tasks.json"
            )
        )
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val compactLayout = screenHeightDp < 760
    val shortLayout = screenHeightDp < 700
    val cardHeight = when {
        shortLayout -> 210.dp
        compactLayout -> 238.dp
        else -> 270.dp
    }
    val animationPaneHeight = when {
        shortLayout -> 94.dp
        compactLayout -> 112.dp
        else -> 132.dp
    }
    val lottieHeight = when {
        shortLayout -> 90.dp
        compactLayout -> 106.dp
        else -> 126.dp
    }

    fun finishOnboarding() {
        sharedPreferences.edit()
            .putBoolean("onboarding_completed", true)
            .apply()
        onNavigateToProfileSetup()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appScreenBackgroundColor())
    ) {
        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 22.dp, top = 18.dp, end = 22.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .background(appTopBarColor(0.82f))
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
                            .background(appTopBarColor(0.82f))
                            .clickable { finishOnboarding() }
                            .padding(horizontal = 15.dp, vertical = 9.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = if (compactLayout) 2.dp else 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        stringResource(R.string.welcome_aboard_plain),
                        color = appTextPrimaryColor(),
                        fontSize = if (shortLayout) 21.sp else 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(if (compactLayout) 3.dp else 5.dp))
                    Text(
                        stringResource(R.string.welcome_swipe_intro),
                        color = appTextMutedColor(),
                        fontSize = if (shortLayout) 11.sp else 13.sp,
                        lineHeight = if (shortLayout) 15.sp else 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.height(if (compactLayout) 5.dp else 7.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        pageSpacing = 14.dp
                    ) { page ->
                        OnboardingAnimatedCard(
                            card = cards[page],
                            cardHeight = cardHeight,
                            animationPaneHeight = animationPaneHeight,
                            lottieHeight = lottieHeight,
                            compact = compactLayout
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                    Button(
                        onClick = ::finishOnboarding,
                        modifier = Modifier
                            .weight(1.35f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.continue_text), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            NativeAdSlot(
                placement = "native_onboarding",
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, appBorderColor())
            )
        }
    }
}

@Composable
private fun OnboardingAnimatedCard(
    card: OnboardingCarouselCard,
    cardHeight: Dp,
    animationPaneHeight: Dp,
    lottieHeight: Dp,
    compact: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = card.color.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = appSurfaceColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animationPaneHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(card.color.copy(alpha = 0.16f), appSoftPurpleColor())
                        )
                    )
                    .border(1.dp, card.color.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset(card.animationAsset))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    speed = 1.35f
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lottieHeight)
                )
            }

            Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
            Text(
                card.title,
                color = appTextPrimaryColor(),
                fontSize = if (compact) 16.sp else 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (compact) 3.dp else 4.dp))
            Text(
                card.description,
                color = appTextMutedColor(),
                fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = if (compact) 15.sp else 17.sp,
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
