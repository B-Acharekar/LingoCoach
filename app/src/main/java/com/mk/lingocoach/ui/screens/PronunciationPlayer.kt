package com.mk.lingocoach.ui.screens

import android.speech.tts.TextToSpeech
import android.os.Bundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── TTS Speed Enum ──────────────────────────────────────────────────────────

enum class SpeechSpeed(val label: String, val rate: Float) {
    SLOW("S", 0.6f),
    MEDIUM("M", 1.0f),
    FAST("F", 1.4f)
}

// ─── Speak helper (max volume) ────────────────────────────────────────────────

fun speakWord(tts: TextToSpeech?, word: String, speed: SpeechSpeed) {
    if (tts == null) return
    val params = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
    }
    tts.setSpeechRate(speed.rate)
    tts.setPitch(1.0f)
    tts.speak(word, TextToSpeech.QUEUE_FLUSH, params, "tts_${System.currentTimeMillis()}")
}

// ─── Card Pronunciation Widget ────────────────────────────────────────────────
// Centered speaker icon on one line, S M F pills below it.
// Drop this anywhere inside a Card to keep pronunciation self-contained.
//
//        ╔══════════════════════╗
//        ║    [🔊 VolumeUp]     ║
//        ║   [S]  [M]  [F]     ║
//        ╚══════════════════════╝

@Composable
fun CardPronunciation(
    word: String,
    tts: TextToSpeech?,
    modifier: Modifier = Modifier,
    initialSpeed: SpeechSpeed = SpeechSpeed.MEDIUM
) {
    var selectedSpeed by remember { mutableStateOf(initialSpeed) }
    var isPlaying by remember { mutableStateOf(false) }

    // Brief visual flash when icon is tapped
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            kotlinx.coroutines.delay(350)
            isPlaying = false
        }
    }

    val iconBg by animateColorAsState(
        targetValue = if (isPlaying) BrandPurple else BrandPurpleSoft,
        animationSpec = tween(200),
        label = "iconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isPlaying) Color.White else BrandPurple,
        animationSpec = tween(200),
        label = "iconTint"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Speaker icon — centered
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(if (isPlaying) 4.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .background(iconBg)
                .clickable {
                    isPlaying = true
                    speakWord(tts, word, selectedSpeed)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Listen",
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        // S M F speed pills — centered row
        SpeedPills(
            selectedSpeed = selectedSpeed,
            onSpeedSelected = { speed ->
                selectedSpeed = speed
                isPlaying = true
                speakWord(tts, word, speed)
            }
        )
    }
}

// ─── Speed Pills Row ─────────────────────────────────────────────────────────

@Composable
fun SpeedPills(
    selectedSpeed: SpeechSpeed,
    compact: Boolean = false,           // kept for legacy call-sites
    onSpeedSelected: (SpeechSpeed) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpeechSpeed.values().forEach { speed ->
            val isActive = speed == selectedSpeed
            val bgColor by animateColorAsState(
                targetValue = if (isActive) BrandPurple else Color(0xFFF0EEFF),
                animationSpec = tween(150),
                label = "speedPill_${speed.label}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) Color.White else BrandPurple,
                animationSpec = tween(150),
                label = "speedPillText_${speed.label}"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isActive) BrandPurple else Color(0xFFDDDAFF),
                animationSpec = tween(150),
                label = "speedPillBorder_${speed.label}"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onSpeedSelected(speed) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = speed.label,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─── Inline Pronunciation Button (legacy / compact use) ──────────────────────
// Kept for existing call-sites in TimelyDuelScreen (SPELLING header row).
// Shows: 🔊 [S][M][F] horizontally.

@Composable
fun PronunciationButton(
    word: String,
    tts: TextToSpeech?,
    modifier: Modifier = Modifier,
    initialSpeed: SpeechSpeed = SpeechSpeed.MEDIUM,
    compact: Boolean = false
) {
    var selectedSpeed by remember { mutableStateOf(initialSpeed) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Speaker icon
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 36.dp)
                .clip(CircleShape)
                .background(BrandPurpleSoft)
                .clickable { speakWord(tts, word, selectedSpeed) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Listen",
                tint = BrandPurple,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp)
            )
        }

        SpeedPills(
            selectedSpeed = selectedSpeed,
            compact = compact,
            onSpeedSelected = { speed ->
                selectedSpeed = speed
                speakWord(tts, word, speed)
            }
        )
    }
}

// ─── Full-Width Pronunciation Bar ─────────────────────────────────────────────
// For game screens. Shows: 🔊 Label      [S] [M] [F]

@Composable
fun PronunciationBar(
    word: String,
    tts: TextToSpeech?,
    modifier: Modifier = Modifier,
    label: String = "Tap to listen",
    initialSpeed: SpeechSpeed = SpeechSpeed.MEDIUM
) {
    var selectedSpeed by remember { mutableStateOf(initialSpeed) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandPurpleSoft)
            .border(1.dp, Color(0xFFDDDAFF), RoundedCornerShape(14.dp))
            .clickable { speakWord(tts, word, selectedSpeed) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: icon + label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrandPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    color = BrandPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right: speed pills
            SpeedPills(
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { speed ->
                    selectedSpeed = speed
                    speakWord(tts, word, speed)
                }
            )
        }
    }
}
