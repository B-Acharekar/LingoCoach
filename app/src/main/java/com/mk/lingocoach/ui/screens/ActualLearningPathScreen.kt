package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
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

    val userId = remember { sharedPrefs.getString("session_id", null) ?: "" }

    var learningPath by remember { mutableStateOf<CurrentLearningPathResponse?>(AppCache.learningPath) }
    var isLoading by remember { mutableStateOf(learningPath == null) }
    var selectedTab by remember { mutableStateOf(0) }

    // Always fetch a fresh copy when this screen is entered/returned to, so that
    // lessons you just completed (e.g. coming back from LessonScreen) show up
    // immediately instead of relying on a possibly-stale cached snapshot.
    LaunchedEffect(userId) {
        AppCache.loadFromDisk(context)
        val cached = AppCache.learningPath
        if (cached != null) {
            learningPath = cached
            isLoading = false
        }
        if (userId.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                AssessmentApi.getCurrentLearningPath(userId) { path ->
                    if (path != null) {
                        AppCache.learningPath = AppCache.applyLocalLearningPathProgress(path)
                        AppCache.learningPathAt = System.currentTimeMillis()
                        AppCache.saveToDisk(context)
                        scope.launch(Dispatchers.Main) {
                            learningPath = AppCache.learningPath
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
        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            CommonTopBar(
                title = stringResource(R.string.learning_path),
                onBack = onNavigateToHome
            )
            // ── Scrollable Content ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                if (learningPath != null) {
                    val currentModule = learningPath!!.normalizedLearningPath().modules.firstOrNull { it.status == "current" }

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
                            val totalLessons = currentModule.lessons.size
                            val completedLessons = currentModule.completedLessonCount()
                            val progress = currentModule.progressPercent()
                            Text(
                                "$progress%",
                                color = BrandPurple,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        currentModule.lessons.forEachIndexed { _, lesson ->
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

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val activeSub = currentModule.currentSublesson()
                                if (activeSub != null) onNavigateToLesson(activeSub.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.continue_learning),
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
    val isCurrent   = status == "current"
    val isCompleted = status == "completed"
    val isLocked    = status == "locked"

    val activeSub = sublessons.firstOrNull { it.status == "current" }
        ?: sublessons.firstOrNull()

    // Unified card bg — locked is only slightly dimmed, no border difference
    val cardBg = when {
        isCurrent   -> Color(0xFFF5F3FF)
        isCompleted -> CardWhite
        else        -> CardWhite  // locked — same white, just icon communicates state
    }

    // Icon bubble colours
    val bubbleBg = when {
        isCompleted -> BrandGreen
        isCurrent   -> BrandPurple
        else        -> Color(0xFFEEEEEE)   // locked — soft neutral, no harsh grey
    }
    val iconTint = when {
        isLocked -> Color(0xFFBBBBBB)      // muted icon, not white-on-grey clash
        else     -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Fixed min-height so locked and unlocked cards are always the same size
            .defaultMinSize(minHeight = 80.dp)
            .shadow(
                elevation = if (isLocked) 1.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isCurrent) BrandPurple.copy(0.12f) else Color.Black.copy(0.04f),
                spotColor   = if (isCurrent) BrandPurple.copy(0.12f) else Color.Black.copy(0.04f)
            )
            .clickable(enabled = !isLocked) { activeSub?.let { onTap(it.id) } },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        // No border — removed entirely
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            // Enforce a minimum row height so both card types match
            horizontalArrangement = Arrangement.Start
        ) {
            // ── Status bubble ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bubbleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.Check
                        isLocked    -> Icons.Default.Lock
                        isCurrent   -> Icons.Default.PlayArrow
                        else        -> Icons.Default.Circle
                    },
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // ── Text block ────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$order. $title",
                    // Locked text is muted but same size — keeps layout identical
                    color = if (isLocked) Color(0xFFAAAAAA) else TextDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = if (isLocked) Color(0xFFCCCCCC) else TextLight,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Right-side badge / icon ───────────────────────────────────
            Spacer(Modifier.width(8.dp))
            when {
                isCurrent -> Box(
                    modifier = Modifier
                        .background(BrandPurple, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.now),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                isCompleted -> Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(18.dp)
                )
                // Locked — intentionally empty, no extra element needed
                else -> Spacer(Modifier.size(18.dp))
            }
        }
    }
}
