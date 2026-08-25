package com.mk.lingocoach.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mk.lingocoach.R
import com.mk.lingocoach.viewmodel.LanguageViewModel

/**
 * Language Selection Screen with full ViewModel integration
 * Uses AppCompatDelegate for locale management and DataStore for persistence
 * 
 * @param onNavigateToWelcome Callback to navigate to welcome screen after selection
 * @param onNavigateBack Callback to navigate back to previous screen
 */
@Composable
fun LanguageSelectionScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialize ViewModel with factory
    val viewModel: LanguageViewModel = viewModel(
        factory = LanguageViewModel.Factory(context)
    )

    // Collect state from ViewModel
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val filteredLanguages by viewModel.filteredLanguages.collectAsState()
    var draftLanguage by remember { mutableStateOf(selectedLanguage) }

    LaunchedEffect(Unit) {
        viewModel.updateSearchQuery("")
    }

    LaunchedEffect(selectedLanguage) {
        draftLanguage = selectedLanguage
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(appTopBarColor(0.92f))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = BrandPurple,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Language",
                        color = appTextPrimaryColor(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.selectLanguage(draftLanguage)
                            context.getSharedPreferences("LingoCoachPrefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("lang_selected", true)
                                .apply()
                            onNavigateToWelcome()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.done).uppercase(),
                        color = BrandPurple,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredLanguages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_languages_found, ""),
                        color = appTextMutedColor(),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 212.dp)
                ) {
                    items(filteredLanguages) { language ->
                        val isSelected = language.code == draftLanguage

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    draftLanguage = language.code
                                },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrandPurple else appSurfaceColor()
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CustomRadioButton(
                                    selected = isSelected,
                                    selectedColor = Color.White,
                                    unselectedColor = Color(0xFFD2D2D7)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.18f) else appSoftPurpleColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = language.flagEmoji,
                                        fontSize = 20.sp,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = localizedAppLanguageName(language.code),
                                    color = if (isSelected) Color.White else appTextPrimaryColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = language.nativeName,
                                    color = if (isSelected) Color.White.copy(alpha = 0.78f) else appTextMutedColor(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        NativeAdSlot(
            placement = "native_language_select",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(1.dp, appBorderColor())
        )
    }
}

@Composable
fun LanguageChangeOverlay() {
    val transition = rememberInfiniteTransition(label = "language_change")
    val fillProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pixel_fill"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC07051A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelFillIndicator(progress = fillProgress, pulse = pulse)
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.language_changing_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.language_changing_message),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PixelFillIndicator(progress: Float, pulse: Float) {
    val activeColor = Color(0xFF7C6CFF)
    val pixels = 36

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(6) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(6) { column ->
                    val index = row * 6 + column
                    val active = index < (progress * pixels).toInt()
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (active) {
                                    Brush.linearGradient(
                                        listOf(activeColor.copy(alpha = pulse), Color(0xFF58D7FF))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.08f))
                                    )
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun CustomRadioButton(
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(
                width = 2.dp,
                color = if (selected) selectedColor else unselectedColor,
                shape = CircleShape
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectedColor, CircleShape)
            )
        }
    }
}

@Composable
fun localizedAppLanguageName(code: String): String {
    return when (code) {
        "system" -> stringResource(R.string.lang_system)
        "en" -> stringResource(R.string.lang_english)
        "hi" -> stringResource(R.string.lang_hindi)
        "es" -> stringResource(R.string.lang_spanish)
        "fr" -> stringResource(R.string.lang_french)
        "de" -> stringResource(R.string.lang_german)
        "it" -> stringResource(R.string.lang_italian)
        "pt" -> stringResource(R.string.lang_portuguese)
        "ru" -> stringResource(R.string.lang_russian)
        "ja" -> stringResource(R.string.lang_japanese)
        "ko" -> stringResource(R.string.lang_korean)
        "zh" -> stringResource(R.string.lang_chinese)
        "ar" -> stringResource(R.string.lang_arabic)
        "tr" -> stringResource(R.string.lang_turkish)
        "vi" -> stringResource(R.string.lang_vietnamese)
        else -> code
    }
}

