package com.mk.lingocoach.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mk.lingocoach.R

@Composable
fun AppBackgroundTexture(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val base = if (isDark) Color.Black else Color(0xFFF7F6FF)
    val overlay = if (isDark) {
        listOf(
            Color.Black.copy(alpha = 0.72f),
            Color(0xFF050505).copy(alpha = 0.86f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.28f),
            Color(0xFFF7F6FF).copy(alpha = 0.42f)
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
    ) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isDark) 0.18f else 0.52f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        overlay
                    )
                )
        )
    }
}
