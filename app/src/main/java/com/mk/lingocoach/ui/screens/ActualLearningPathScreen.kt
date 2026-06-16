package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CurrentLearningPathResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ActualLearningPathScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (sublessonId: String) -> Unit,
    onNavigateToAILab: () -> Unit,
    onNavigateToVocab: () -> Unit,
    onNavigateToVault: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scrollState = rememberScrollState()

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: ""
    }

    var learningPath by remember {
        mutableStateOf<CurrentLearningPathResponse?>(AppCache.learningPath)
    }
    var isLoading by remember { mutableStateOf(learningPath == null) }
    var selectedTab by remember { mutableStateOf(0) }

    // Load learning path
    LaunchedEffect(userId) {
        AppCache.loadFromDisk(context)
        val cached = AppCache.learningPath
        if (cached != null) {
            learningPath = cached
            isLoading = false
        }
        if (AppCache.isLearningPathStale() && userId.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                AssessmentApi.getCurrentLearningPath(userId) { path ->
                    if (path != null) {
                        AppCache.learningPath = path
                        AppCache.learningPathAt = System.currentTimeMillis()
                        AppCache.saveToDisk(context)
                        scope.launch(Dispatchers.Main) {
                            learningPath = path
                            isLoading = false
                        }
                    } else {
                        scope.launch(Dispatchers.Main) { isLoading = false }
                    }
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Bar with Logo ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF0EEFF), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BrandPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    "LingoCoach",
                    style = TextStyle(
                        color = BrandPurple,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                // Settings Icon
                IconButton(
                    onClick = { /* Navigate to settings - will be wired up */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF0EEFF), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = BrandPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Scrollable Content ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Module Header ─────────────────────────────────────────
                if (learningPath != null) {
                    val currentModule = learningPath!!.modules.firstOrNull { it.status == "current" }
                    
                    if (currentModule != null) {
                        Text(
                            currentModule.level.uppercase(),
                            color = BrandPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currentModule.title,
                                color = TextDark,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Progress percentage
                            val totalLessons = currentModule.lessons.size
                            val completedLessons = currentModule.lessons.count { it.status == "completed" }
                            val progress = if (totalLessons > 0) (completedLessons * 100 / totalLessons) else 0
                            
                            Text(
                                "$progress%",
                                color = BrandPurple,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Lessons List ──────────────────────────────────────
                        currentModule.lessons.forEachIndexed { index, lesson ->
                            LessonCard(
                                order = lesson.order,
                                title = lesson.title,
                                description = lesson.description,
                                status = lesson.status,
                                sublessons = lesson.sublessons,
                                onTap = { subId -> onNavigateToLesson(subId) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        // ── Continue Learning Button ──────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val activeSub = currentModule.lessons
                                    .firstOrNull { it.status == "current" }
                                    ?.sublessons?.firstOrNull { it.status == "current" }
                                if (activeSub != null) {
                                    onNavigateToLesson(activeSub.id)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                            Text(
                                "Continue Learning",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Bottom Navigation ─────────────────────────────────────────
            HomeBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { idx ->
                    selectedTab = idx
                    when (idx) {
                        0 -> onNavigateToHome()
                        1 -> onNavigateToAILab()
                        2 -> onNavigateToVocab()
                        3 -> onNavigateToVault()
                    }
                }
            )
        }
    }
}

@Composable
private fun LessonCard(
    order: Int,
    title: String,
    description: String,
    status: String,
    sublessons: List<com.mk.lingocoach.network.CurrentSublesson>,
    onTap: (String) -> Unit
) {
    val isCurrent = status == "current"
    val isCompleted = status == "completed"
    val isLocked = status == "locked"

    val activeSub = sublessons.firstOrNull { it.status == "current" }
        ?: sublessons.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable(enabled = !isLocked) {
                activeSub?.let { onTap(it.id) }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isLocked -> CardWhite.copy(alpha = 0.6f)
                isCurrent -> Color(0xFFF5F3FF)
                else -> CardWhite
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> BrandGreen
                            isCurrent -> BrandPurple
                            isLocked -> Color(0xFFE0E0E0)
                            else -> BrandPurple.copy(0.3f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.Check
                        isLocked -> Icons.Default.Lock
                        isCurrent -> Icons.Default.PlayArrow
                        else -> Icons.Default.Circle
                    },
                    contentDescription = null,
                    tint = if (isLocked) TextLight else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$order. $title",
                    color = if (isLocked) TextLight else TextDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = TextLight,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(BrandPurple, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "CURRENT",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
