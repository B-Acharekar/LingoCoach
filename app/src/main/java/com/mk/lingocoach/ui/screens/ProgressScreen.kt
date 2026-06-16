package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scrollState = rememberScrollState()
    
    var weeklyStats by remember { mutableStateOf<List<com.mk.lingocoach.network.DailyStats>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val userId = remember {
        prefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }
    
    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getWeeklyAnalytics(userId) { stats ->
                scope.launch(Dispatchers.Main) {
                    weeklyStats = stats ?: emptyList()
                    isLoading = false
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.92f))
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandPurple)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    
                    // Overall Progress Summary
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Weekly Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(Modifier.height(16.dp))
                            
                            val totalLessons = weeklyStats.sumOf { it.lessons_completed }
                            val totalExercises = weeklyStats.sumOf { it.exercises_attempted }
                            val correctExercises = weeklyStats.sumOf { it.exercises_correct }
                            val totalXP = weeklyStats.sumOf { it.xp_earned }
                            val totalMinutes = weeklyStats.sumOf { it.ai_lab_minutes }
                            
                            ProgressStatRow("Lessons Completed", "$totalLessons", BrandPurple)
                            Spacer(Modifier.height(12.dp))
                            ProgressStatRow("Exercises Done", "$totalExercises", BrandPurple)
                            Spacer(Modifier.height(12.dp))
                            ProgressStatRow("Correct Answers", "$correctExercises", BrandGreen)
                            Spacer(Modifier.height(12.dp))
                            ProgressStatRow("Total XP Earned", "$totalXP XP", BrandAmber)
                            Spacer(Modifier.height(12.dp))
                            ProgressStatRow("AI Lab Minutes", "$totalMinutes min", BrandPurple)
                        }
                    }
                    
                    // Daily Breakdown
                    Text("Daily Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    
                    weeklyStats.forEach { day ->
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(day.date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Lessons: ${day.lessons_completed}", fontSize = 12.sp, color = TextMid)
                                        Text("Exercises: ${day.exercises_attempted}", fontSize = 12.sp, color = TextMid)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("XP: ${day.xp_earned}", fontSize = 12.sp, color = BrandAmber, fontWeight = FontWeight.Bold)
                                        Text("Accuracy: ${if (day.exercises_attempted > 0) (day.exercises_correct * 100 / day.exercises_attempted) else 0}%", 
                                            fontSize = 12.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, color = TextMid)
        }
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}
