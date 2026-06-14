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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeVaultScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    
    var mistakes by remember { mutableStateOf<List<Mistake>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getMistakes(userId) { m ->
                scope.launch(Dispatchers.Main) {
                    mistakes = m ?: emptyList()
                    isLoading = false
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
        } else if (mistakes.isEmpty()) {
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
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(mistakes) { mistake ->
                    MistakeCard(mistake)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun MistakeCard(mistake: Mistake) {
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
                            .background(Color(0xFFFFECEC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BrandRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(mistake.mistake_type.uppercase(), color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text(mistake.word, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Missed ${mistake.times_missed}x", color = TextMid, fontSize = 12.sp)
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = CardBorderColor)
                Spacer(Modifier.height(16.dp))

                Text("You wrote:", color = TextMid, fontSize = 12.sp)
                Text(mistake.user_sentence, color = BrandRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(8.dp))
                Text("Correction:", color = TextMid, fontSize = 12.sp)
                Text(mistake.correct_sentence, color = BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)

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
