package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.Mistake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Unified display model – covers both server Mistake and local LocalMistakeEntry
data class DisplayMistake(
    val word: String,
    val mistakeType: String,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val timesMissed: Int,
    val source: String  // "server" | "local"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeVaultScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    var displayMistakes by remember { mutableStateOf<List<DisplayMistake>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            // Load local vocab mistakes immediately
            val localMistakes = VocabTracker.getLocalMistakes(context).map { entry ->
                DisplayMistake(
                    word = entry.word,
                    mistakeType = entry.mistakeType,
                    userAnswer = entry.userAnswer,
                    correctAnswer = entry.correctAnswer,
                    explanation = entry.explanation,
                    timesMissed = entry.timesMissed,
                    source = "local"
                )
            }

            scope.launch(Dispatchers.Main) {
                displayMistakes = localMistakes
                isLoading = false
            }

            // Fetch server mistakes in background and merge
            AssessmentApi.getMistakes(userId) { serverList ->
                val serverMapped = (serverList ?: emptyList()).map { m ->
                    DisplayMistake(
                        word = m.word,
                        mistakeType = m.mistake_type,
                        userAnswer = m.user_sentence,
                        correctAnswer = m.correct_sentence,
                        explanation = m.explanation,
                        timesMissed = m.times_missed,
                        source = "server"
                    )
                }
                // Merge: server entries first, then local entries not already in server
                val serverWords = serverMapped.map { it.word.lowercase() }.toSet()
                val uniqueLocal = localMistakes.filter { it.word.lowercase() !in serverWords }
                scope.launch(Dispatchers.Main) {
                    displayMistakes = serverMapped + uniqueLocal
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mistake Vault", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFF),
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark
                )
            )
        },
        containerColor = Color(0xFFFAFAFF)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPurple)
            }
        } else if (displayMistakes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your vault is empty!", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Keep practicing to discover your weak points.", color = TextMid, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${displayMistakes.size} mistake${if (displayMistakes.size != 1) "s" else ""} recorded",
                            color = TextMid, fontSize = 13.sp
                        )
                        val localCount = displayMistakes.count { it.source == "local" }
                        if (localCount > 0) {
                            Text("$localCount from practice", color = BrandPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(displayMistakes) { mistake ->
                    DisplayMistakeCard(mistake)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun DisplayMistakeCard(mistake: DisplayMistake) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (mistake.source == "local") BrandPurpleSoft else Color(0xFFFFECEC)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (mistake.source == "local") Icons.Default.Book else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (mistake.source == "local") BrandPurple else BrandRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            mistake.mistakeType.replace("_", " "),
                            color = if (mistake.source == "local") BrandPurple else BrandRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(mistake.word, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Missed ${mistake.timesMissed}×", color = TextMid, fontSize = 12.sp)
                    Text(
                        if (mistake.source == "local") "Offline" else "Synced",
                        color = if (mistake.source == "local") BrandAmber else BrandGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = CardBorderColor)
                Spacer(Modifier.height(16.dp))

                Text("You answered:", color = TextMid, fontSize = 12.sp)
                Text(mistake.userAnswer, color = BrandRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(8.dp))
                Text("Correct answer:", color = TextMid, fontSize = 12.sp)
                Text(mistake.correctAnswer, color = BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandPurpleSoft, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Explanation", color = BrandPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(mistake.explanation, color = TextDark, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Keep backward-compat overload for old Mistake type (server model)
@Composable
fun MistakeCard(mistake: Mistake) {
    DisplayMistakeCard(
        DisplayMistake(
            word = mistake.word,
            mistakeType = mistake.mistake_type,
            userAnswer = mistake.user_sentence,
            correctAnswer = mistake.correct_sentence,
            explanation = mistake.explanation,
            timesMissed = mistake.times_missed,
            source = "server"
        )
    )
}
