package com.mk.lingocoach.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.mk.lingocoach.R
import com.mk.lingocoach.network.LearningPathResponse
import com.mk.lingocoach.network.Module

@Composable
fun LearningPathScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val gson = remember { Gson() }

    // Retrieve LearningPathResponse from SharedPreferences
    val learningPath = remember {
        val json = sharedPrefs.getString("learning_path_json", null)
        if (json != null) {
            try {
                gson.fromJson(json, LearningPathResponse::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    var expandedModuleIndex by remember { mutableStateOf<Int?>(1) } // Default expand Level 2 (index 1)
    var selectedCoach by remember { mutableStateOf(sharedPrefs.getString("selected_coach", "Amélie") ?: "Amélie") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF4F3FF),
                        Color(0xFFEBEBFF),
                        Color(0xFFFFF9EB)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF6A5CFF)
                    )
                }

                Text(
                    text = "Learning Path",
                    style = TextStyle(
                        color = Color(0xFF1D1D1F),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                IconButton(
                    onClick = { /* Menu option placeholder */ },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF6E6E73)
                    )
                }
            }

            // 2. Scrollable Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // Journey Subtitle
                Text(
                    text = "YOUR JOURNEY",
                    color = Color(0xFF6A5CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(letterSpacing = 1.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Personalized route to fluency",
                    color = Color(0xFF1D1D1F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                // 3. Timeline Layout
                val modules = learningPath?.modules ?: emptyList()
                if (modules.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Vertical Connecting line
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawLine(
                                color = Color(0xFFD2D2D7),
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            modules.forEachIndexed { index, module ->
                                ModuleCard(
                                    module = module,
                                    isEven = index % 2 == 0,
                                    expanded = expandedModuleIndex == index,
                                    onModuleClicked = {
                                        expandedModuleIndex = if (expandedModuleIndex == index) null else index
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Fallback in case of empty API response
                    Text(
                        text = "No learning path found. Please complete the assessment first.",
                        color = Color(0xFF6E6E73),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }

                Spacer(Modifier.height(36.dp))

                // 4. Coach Selection Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Coach Voice",
                        color = Color(0xFF1D1D1F),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF2D4), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Native TTS Engine",
                            color = Color(0xFFFB8C00),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 5. Coach list (Amélie & Noah)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        CoachCard(
                            name = "Amélie",
                            description = "Warm & Encouraging",
                            avatarResId = R.drawable.amelie_avatar,
                            isSelected = selectedCoach == "Amélie",
                            onClick = {
                                selectedCoach = "Amélie"
                                sharedPrefs.edit().putString("selected_coach", "Amélie").apply()
                            },
                            onPreviewClick = {
                                Toast.makeText(context, "Bonjour! I am Amélie, your LingoCoach. ☕", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item {
                        CoachCard(
                            name = "Noah",
                            description = "Professional & Clear",
                            avatarResId = R.drawable.noah_avatar,
                            isSelected = selectedCoach == "Noah",
                            onClick = {
                                selectedCoach = "Noah"
                                sharedPrefs.edit().putString("selected_coach", "Noah").apply()
                            },
                            onPreviewClick = {
                                Toast.makeText(context, "Hello! I am Noah, let's practice together. 💼", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // 6. Bottom CTA Button
                Button(
                    onClick = onNavigateToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp), clip = false),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF6A5CFF), Color(0xFF8A79FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Start Next Lesson",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Start",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ModuleCard(
    module: Module,
    isEven: Boolean,
    expanded: Boolean,
    onModuleClicked: () -> Unit
) {
    val isCurrent = module.status == "current"
    val isLocked = module.status == "locked"
    val isCompleted = module.status == "completed" || module.level == "Level 1"

    val cardBg = if (isLocked) Color.White.copy(alpha = 0.5f) else Color.White
    val borderStroke = if (isCurrent) {
        BorderStroke(2.dp, Brush.horizontalGradient(listOf(Color(0xFF6A5CFF), Color(0xFF8A79FF))))
    } else if (isCompleted) {
        BorderStroke(1.dp, Color(0xFF34C759).copy(alpha = 0.3f))
    } else if (isLocked) {
        BorderStroke(1.dp, Color(0xFFE5E5EA).copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, Color(0xFFE5E5EA))
    }

    val shadowElevation = when {
        isCurrent -> 8.dp
        isLocked -> 0.dp
        else -> 3.dp
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .width(230.dp)
                .align(if (isEven) Alignment.CenterStart else Alignment.CenterEnd)
                .shadow(shadowElevation, RoundedCornerShape(20.dp))
                .clickable { onModuleClicked() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = borderStroke
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> Color(0xFF6A5CFF).copy(alpha = 0.1f)
                                    isCompleted -> Color(0xFF34C759).copy(alpha = 0.1f)
                                    else -> Color(0x0A000000)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(18.dp)
                            )
                            isCurrent -> Text("☕", fontSize = 16.sp)
                            else -> Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isCurrent) "CURRENT" else module.level.uppercase(),
                            color = if (isCurrent) Color(0xFF6A5CFF) else Color(0xFF8E8E93),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(letterSpacing = 0.5.sp)
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = module.title,
                            color = if (isLocked) Color(0xFF8E8E93) else Color(0xFF1D1D1F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (expanded && module.sub_learning_path.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    Spacer(Modifier.height(8.dp))

                    module.sub_learning_path.forEach { lesson ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (lesson.status) {
                                            "completed" -> Color(0xFF34C759)
                                            "current" -> Color(0xFF6A5CFF)
                                            else -> Color(0xFFD2D2D7)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = lesson.title,
                                    color = if (lesson.status == "locked") Color(0xFF8E8E93) else Color(0xFF1D1D1F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = lesson.description,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachCard(
    name: String,
    description: String,
    avatarResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF6A5CFF) else Color(0xFFE5E5EA)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(avatarResId),
                    contentDescription = name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        color = Color(0xFF1D1D1F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onPreviewClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Preview",
                        tint = Color(0xFF6A5CFF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Preview",
                        color = Color(0xFF6A5CFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
