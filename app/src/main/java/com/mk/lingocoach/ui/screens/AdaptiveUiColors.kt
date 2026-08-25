package com.mk.lingocoach.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun appSurfaceColor(): Color = if (isSystemInDarkTheme()) Color(0xFF242033) else Color(0xFFFFFEFF)

@Composable
internal fun appElevatedSurfaceColor(): Color = if (isSystemInDarkTheme()) Color(0xFF302A40) else Color(0xFFFFFFFF)

@Composable
internal fun appTopBarColor(alpha: Float = 0.92f): Color =
    (if (isSystemInDarkTheme()) Color(0xFF181521) else Color.White).copy(alpha = alpha)

@Composable
internal fun appSoftPurpleColor(): Color = if (isSystemInDarkTheme()) Color(0xFF3A3156) else BrandPurpleSoft

@Composable
internal fun appSubtleTrackColor(): Color = if (isSystemInDarkTheme()) Color(0xFF4A4166) else SubtlePurpleTrack

@Composable
internal fun appBorderColor(): Color = if (isSystemInDarkTheme()) Color(0x55FFFFFF) else CardBorderColor

@Composable
internal fun appTextPrimaryColor(): Color = if (isSystemInDarkTheme()) Color(0xFFF6F3FF) else TextDark

@Composable
internal fun appTextSecondaryColor(): Color = if (isSystemInDarkTheme()) Color(0xFFD6D0E8) else TextMid

@Composable
internal fun appTextMutedColor(): Color = if (isSystemInDarkTheme()) Color(0xFFAFA7C2) else TextLight

@Composable
internal fun appScreenBackgroundColor(): Color = if (isSystemInDarkTheme()) Color(0xFF0E0D14) else Color(0xFFF6F4FF)

@Composable
internal fun appInputColor(): Color = if (isSystemInDarkTheme()) Color(0xFF2B2638) else Color.White