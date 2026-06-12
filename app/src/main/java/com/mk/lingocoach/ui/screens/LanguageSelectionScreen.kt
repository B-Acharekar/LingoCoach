package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R

@Composable
fun LanguageSelectionScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    // Load previously selected language or default to "system"
    var selectedLanguageCode by remember { 
        mutableStateOf(sharedPreferences.getString("selected_lang", "system") ?: "system") 
    }
    
    var searchQuery by remember { mutableStateOf("") }

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

    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            languages
        } else {
            languages.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.nativeName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // 1. Header Row (Back Arrow, Title LingoCoach, and Done Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Back button area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF6A5CFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LingoCoach",
                        style = TextStyle(
                            color = Color(0xFF6A5CFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Done Button in capsule style (navigates to welcome onboarding)
                Button(
                    onClick = {
                        sharedPreferences.edit()
                            .putString("selected_lang", selectedLanguageCode)
                            .putBoolean("lang_selected", true)
                            .apply()
                        onNavigateToWelcome()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A5CFF)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Title and Description
            Text(
                text = "Choose Language",
                style = TextStyle(
                    color = Color(0xFF1D1D1F),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select your preferred language",
                style = TextStyle(
                    color = Color(0xFF6E6E73),
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = {
                    Text(
                        text = "Search languages...",
                        color = Color(0xFF8E8D9F),
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF8E8D9F),
                        modifier = Modifier.size(22.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF4F4F6),
                    unfocusedContainerColor = Color(0xFFF4F4F6),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color(0xFF1D1D1F),
                    unfocusedTextColor = Color(0xFF1D1D1F)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Scrollable List
            if (filteredLanguages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No languages found for \"$searchQuery\"",
                        color = Color(0xFF8E8D9F),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLanguages) { language ->
                        val isSelected = language.code == selectedLanguageCode

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLanguageCode = language.code }
                                .shadow(
                                    elevation = if (isSelected) 12.dp else 4.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    clip = false,
                                    ambientColor = Color(0x05000000),
                                    spotColor = if (isSelected) Color(0x1F6A5CFF) else Color(0x0A000000)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF6A5CFF) else Color.White
                            ),
                            border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E2E6)) else null
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
                                    // Circular Flag container
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0x33FFFFFF) else Color(0xFFF4F4F6)),
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
                                                color = if (isSelected) Color.White else Color(0xFF1D1D1F),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = language.nativeName,
                                            style = TextStyle(
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF8E8D9F),
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }

                                // Premium Custom Radio Button
                                CustomRadioButton(
                                    selected = isSelected,
                                    selectedColor = Color.White,
                                    unselectedColor = Color(0xFFD2D2D7)
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
}

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)

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
