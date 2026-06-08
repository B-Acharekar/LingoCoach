package com.kazuki.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)

@Composable
fun LanguageSelectionScreen(
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    // Default to "system" selection
    var selectedLanguageCode by remember { mutableStateOf("system") }

    val languages = remember {
        listOf(
            LanguageItem("system", "System Default", "Default Settings", "🌐"),
            LanguageItem("en", "English", "English", "🇺🇸"),
            LanguageItem("hi", "Hindi", "हिन्दी", "🇮🇳"),
            LanguageItem("es", "Spanish", "Español", "🇪🇸"),
            LanguageItem("fr", "French", "Français", "🇫🇷"),
            LanguageItem("de", "German", "Deutsch", "🇩🇪"),
            LanguageItem("it", "Italian", "Italiano", "🇮🇹"),
            LanguageItem("pt", "Portuguese", "Português", "🇵🇹"),
            LanguageItem("ru", "Russian", "Русский", "🇷🇺"),
            LanguageItem("ja", "Japanese", "日本語", "🇯🇵"),
            LanguageItem("ko", "Korean", "한국어", "🇰🇷"),
            LanguageItem("zh", "Mandarin Chinese", "简体中文", "🇨🇳"),
            LanguageItem("ar", "Arabic", "العربية", "🇸🇦"),
            LanguageItem("tr", "Turkish", "Türkçe", "🇹🇷"),
            LanguageItem("vi", "Vietnamese", "Tiếng Việt", "🇻🇳")
        )
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
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header Row with Title and Done Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Choose Language",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your preferred language",
                        style = TextStyle(
                            color = Color(0xFF8E8D9F),
                            fontSize = 14.sp
                        )
                    )
                }

                Button(
                    onClick = {
                        // Save selection to preferences
                        sharedPreferences.edit()
                            .putString("selected_lang", selectedLanguageCode)
                            .putBoolean("lang_selected", true)
                            .apply()
                        onNavigateToHome()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF635BFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable List of Languages
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(languages) { language ->
                    val isSelected = language.code == selectedLanguageCode

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguageCode = language.code },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0x28635BFF) else Color(0x0EFFFFFF)
                        ),
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) Color(0xFF635BFF) else Color(0x1AFFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Flag Circle
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x15FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = language.flagEmoji,
                                        fontSize = 24.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Names
                                Column {
                                    Text(
                                        text = language.name,
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = language.nativeName,
                                        style = TextStyle(
                                            color = Color(0xFF8E8D9F),
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }

                            // Radio Indicator
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedLanguageCode = language.code },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF8A79FF),
                                    unselectedColor = Color(0xFF555469)
                                )
                            )
                        }
                    }
                }

                // Extra spacing at the bottom
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
