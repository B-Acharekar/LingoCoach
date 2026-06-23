package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.CurrentSublesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val PathNavy = Color(0xFF17133B)
private val PathSurface = Color(0xFFF8F7FF)
private val PathMuted = Color(0xFF77728F)

@Composable
fun ActualLearningPathScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (sublessonId: String) -> Unit,
    onNavigateToAILab: () -> Unit,
    onNavigateToVocab: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val userId = remember { sharedPrefs.getString("session_id", null) ?: "" }
    var learningPath by remember { mutableStateOf<CurrentLearningPathResponse?>(AppCache.learningPath?.takeIf { it.isBackendLearningPathReady() }) }
    var isLoading by remember { mutableStateOf(learningPath == null) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        AppCache.loadFromDisk(context)
        AppCache.learningPath?.takeIf { it.isBackendLearningPathReady() }?.let {
            learningPath = it
            isLoading = false
        }
        if (userId.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                AssessmentApi.getCurrentLearningPath(userId) { path ->
                    if (path != null && path.isBackendLearningPathReady()) {
                        AppCache.learningPath = AppCache.applyLocalLearningPathProgress(path)
                        AppCache.learningPathAt = System.currentTimeMillis()
                        AppCache.saveToDisk(context)
                    }
                    scope.launch(Dispatchers.Main) {
                        learningPath = AppCache.learningPath?.takeIf { it.isBackendLearningPathReady() }
                        isLoading = false
                    }
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackgroundTexture()
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            CommonTopBar(
                title = stringResource(R.string.learning_path),
                onBack = onNavigateToHome,
                onSettings = onNavigateToSettings
            )

            val currentModule = learningPath
                ?.normalizedLearningPath()
                ?.modules
                ?.firstOrNull { it.status == "current" }

            when {
                isLoading -> PathLoadingState(Modifier.weight(1f))
                currentModule == null -> PathEmptyState(Modifier.weight(1f), onNavigateBack)
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp)
                    ) {
                        Spacer(Modifier.height(18.dp))
                        PathProgressHero(
                            level = currentModule.level,
                            title = currentModule.title,
                            completed = currentModule.completedLessonCount(),
                            total = currentModule.lessons.size,
                            progress = currentModule.progressPercent()
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(stringResource(R.string.path_next_steps), color = BrandPurple, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.keep_your_momentum), color = PathNavy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(stringResource(R.string.lessons_count, currentModule.lessons.size), color = PathMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(14.dp))

                        currentModule.lessons.forEachIndexed { index, lesson ->
                            JourneyLessonCard(
                                order = lesson.order,
                                title = lesson.title,
                                description = lesson.description,
                                status = lesson.status,
                                sublessons = lesson.sublessons,
                                isLast = index == currentModule.lessons.lastIndex,
                                onTap = onNavigateToLesson
                            )
                        }

                        currentModule.currentSublesson()?.let { activeSub ->
                            Spacer(Modifier.height(12.dp))
                            PathContinueButton(activeSub, onNavigateToLesson)
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }

            HomeBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
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
private fun PathProgressHero(level: String, title: String, completed: Int, total: Int, progress: Int) {
    val animatedProgress by animateFloatAsState(progress / 100f, tween(700), label = "pathProgress")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = BrandPurple.copy(0.20f), spotColor = BrandPurple.copy(0.20f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(PathNavy, BrandPurple, BrandPurpleLight)))
            .padding(22.dp)
    ) {
        Box(Modifier.size(130.dp).offset(x = 220.dp, y = (-65).dp).background(Color.White.copy(0.07f), CircleShape))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(Color.White.copy(0.14f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(level.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFD166), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.on_track), color = Color.White.copy(0.86f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.current_focus), color = Color.White.copy(0.68f), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(15.dp))
                    Text(stringResource(R.string.lessons_complete, completed, total), color = Color.White.copy(0.78f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        trackColor = Color.White.copy(0.18f),
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text("$progress%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun JourneyLessonCard(
    order: Int,
    title: String,
    description: String,
    status: String,
    sublessons: List<CurrentSublesson>,
    isLast: Boolean,
    onTap: (String) -> Unit
) {
    val isCurrent = status == "current"
    val isCompleted = status == "completed"
    val isLocked = status == "locked"
    val target = sublessons.firstOrNull { it.status == "current" } ?: sublessons.firstOrNull()
    val completedParts = sublessons.count { it.status == "completed" }
    val progress = if (sublessons.isEmpty()) 0f else completedParts.toFloat() / sublessons.size
    val accent = when {
        isCompleted -> BrandGreen
        isCurrent -> BrandPurple
        else -> Color(0xFFB7B3C6)
    }

    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) BrandPurple else if (isCompleted) BrandGreen else Color(0xFFEDEBF3)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                else if (isLocked) Icon(Icons.Default.Lock, null, tint = Color(0xFF9994AA), modifier = Modifier.size(17.dp))
                else Text(order.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            if (!isLast) Box(Modifier.width(2.dp).height(82.dp).background(accent.copy(alpha = 0.22f)))
        }
        Spacer(Modifier.width(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .shadow(if (isCurrent) 7.dp else 2.dp, RoundedCornerShape(20.dp), ambientColor = accent.copy(0.14f), spotColor = accent.copy(0.14f))
                .clickable(enabled = !isLocked && target != null) { target?.let { onTap(it.id) } },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isCurrent) PathSurface else CardWhite),
            border = if (isCurrent) BorderStroke(1.dp, BrandPurple.copy(0.25f)) else BorderStroke(1.dp, Color(0x0F000000))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                isCurrent -> stringResource(R.string.in_progress)
                                isCompleted -> stringResource(R.string.completed)
                                else -> stringResource(R.string.locked)
                            },
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.9.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(title, color = if (isLocked) PathMuted else PathNavy, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (!isLocked) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent, modifier = Modifier.size(19.dp))
                }
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(description, color = PathMuted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (sublessons.isNotEmpty() && !isLocked) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { if (isCompleted) 1f else progress },
                            modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
                            color = accent,
                            trackColor = accent.copy(0.12f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.parts_count, completedParts, sublessons.size), color = PathMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PathContinueButton(activeSub: CurrentSublesson, onNavigate: (String) -> Unit) {
    Button(
        onClick = { onNavigate(activeSub.id) },
        modifier = Modifier.fillMaxWidth().height(60.dp).shadow(10.dp, RoundedCornerShape(20.dp), spotColor = BrandPurple.copy(0.24f)),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Box(Modifier.size(34.dp).background(Color.White.copy(0.16f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.continue_learning), color = Color.White.copy(0.72f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            Text(activeSub.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
    }
}

@Composable
private fun PathLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandPurple, strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.building_learning_path), color = PathMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PathEmptyState(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).background(BrandPurpleSoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BrandPurple, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.path_getting_ready), color = PathNavy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text(stringResource(R.string.complete_assessment_unlock_path), color = PathMuted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.go_back), color = BrandPurple) }
        }
    }
}
