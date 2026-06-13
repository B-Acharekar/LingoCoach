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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.VocabBookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabBuilderScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    
    var bookmarks by remember { mutableStateOf<List<VocabBookmark>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getVocabBookmarks(userId) { b ->
                scope.launch(Dispatchers.Main) {
                    bookmarks = b ?: emptyList()
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocab Builder", fontWeight = FontWeight.Bold, color = TextDark) },
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
        } else if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No bookmarks yet!", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Highlight words in lessons to add them here.", color = TextMid, fontSize = 14.sp)
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
                items(bookmarks) { bookmark ->
                    VocabCard(bookmark)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun VocabCard(bookmark: VocabBookmark) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPurpleSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(bookmark.word, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    if (bookmark.pronunciation_url != null) {
                        Text(bookmark.pronunciation_url, color = TextMid, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    }
                }
                
                // Mastery score pill
                Box(
                    modifier = Modifier
                        .background(if (bookmark.mastery_score > 3) BrandGreen.copy(alpha=0.2f) else BrandAmber.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (bookmark.mastery_score > 3) "MASTERED" else "LEARNING",
                        color = if (bookmark.mastery_score > 3) BrandGreen else BrandAmberDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = CardBorderColor)
                Spacer(Modifier.height(16.dp))

                if (bookmark.definition != null) {
                    Text("Definition:", color = TextMid, fontSize = 12.sp)
                    Text(bookmark.definition, color = TextDark, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                }

                if (bookmark.context_sentence != null) {
                    Text("Found in:", color = TextMid, fontSize = 12.sp)
                    Text("\"${bookmark.context_sentence}\"", color = TextDark, fontSize = 14.sp, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}
