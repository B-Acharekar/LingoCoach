package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.CurrentModule
import com.mk.lingocoach.network.CurrentSublesson
import com.mk.lingocoach.network.Mistake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─── Shared Design Tokens (same across Home + Lesson) ────────────────────────
internal val BrandPurple      = Color(0xFF6A5CFF)
internal val BrandPurpleLight = Color(0xFF8A79FF)
internal val BrandPurpleSoft  = Color(0xFFF0EEFF)
internal val BrandAmber       = Color(0xFFFFB300)
internal val BrandAmberDark   = Color(0xFFFF6D00)
internal val BrandRed         = Color(0xFFE53935)
internal val BrandGreen       = Color(0xFF4CAF50)
internal val TextDark         = Color(0xFF0D0D0D)
internal val TextMid          = Color(0xFF3A3A3A)
internal val TextLight        = Color(0xFF6B6B6B)
internal val CardWhite        = Color(0xFFFFFEFF)
internal val CardBorderColor  = Color(0x18000000)

@Composable
fun HomeScreen(
    onNavigateToLesson: (sublessonId: String) -> Unit,
    onNavigateToVocab: () -> Unit = {},
    onNavigateToMistakes: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToDuel: () -> Unit = {},
    onNavigateToAILab: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRoadmap: () -> Unit = {},
    onNavigateToActualLearningPath: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {}
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope         = rememberCoroutineScope()
    val sharedPrefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scrollState   = rememberScrollState()

    var selectedTab   by remember { mutableStateOf(0) }
    var learningPath  by remember { mutableStateOf<CurrentLearningPathResponse?>(null) }
    var mistakes      by remember { mutableStateOf<List<Mistake>>(emptyList()) }
    var weeklyStats   by remember { mutableStateOf<List<com.mk.lingocoach.network.DailyStats>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var isVocabLoaded by remember { mutableStateOf(VocabTracker.isLoaded) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    DisposableEffect(lifecycleOwner, userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val cached = AppCache.learningPath
                if (cached != null) {
                    learningPath = cached
                    isLoading = false
                }

                if (AppCache.isLearningPathStale()) {
                    scope.launch(Dispatchers.IO) {
                        AssessmentApi.getCurrentLearningPath(userId) { path ->
                            scope.launch(Dispatchers.Main) {
                                if (path != null) {
                                    AppCache.learningPath = AppCache.applyLocalLearningPathProgress(path)
                                    AppCache.learningPathAt = System.currentTimeMillis()
                                    AppCache.saveToDisk(context)

                                    learningPath = AppCache.learningPath
                                }
                                isLoading = false
                            }
                        }
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(userId) {
        // ── Vocab init (background, no spinner) ──────────────────────────────
        scope.launch(Dispatchers.IO) {
            if (!VocabTracker.isLoaded) {
                VocabTracker.init(context)
                scope.launch(Dispatchers.Main) { isVocabLoaded = true }
            }
        }

        // ── Learning path: show from cache instantly, refresh if stale ────────
        scope.launch(Dispatchers.Main) {
            AppCache.loadFromDisk(context)
            val cached = AppCache.learningPath
            if (cached != null) {
                learningPath = cached
                isLoading = false          // no spinner — show stale data
            }
        }

        scope.launch(Dispatchers.IO) {
            if (AppCache.isLearningPathStale()) {
                AssessmentApi.getCurrentLearningPath(userId) { path ->
                    if (path != null) {
                        AppCache.learningPath  = AppCache.applyLocalLearningPathProgress(path)
                        AppCache.learningPathAt = System.currentTimeMillis()
                        scope.launch(Dispatchers.Main) {
                            AppCache.saveToDisk(context)
                            learningPath = AppCache.learningPath
                            isLoading = false
                        }
                    } else {
                        scope.launch(Dispatchers.Main) { isLoading = false }
                    }
                }
            }
        }

        // ── Mistakes: cache-aware fetch ───────────────────────────────────────
        scope.launch(Dispatchers.IO) {
            val cachedMistakes = AppCache.mistakes
            if (cachedMistakes != null) {
                scope.launch(Dispatchers.Main) { mistakes = cachedMistakes }
            }
            if (AppCache.isMistakesStale()) {
                AssessmentApi.getMistakes(userId) { m ->
                    if (m != null) {
                        AppCache.mistakes  = m
                        AppCache.mistakesAt = System.currentTimeMillis()
                    }
                    scope.launch(Dispatchers.Main) { mistakes = m ?: emptyList() }
                }
            }
        }

        // ── Weekly analytics (hourly refresh) ────────────────────────────────
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getWeeklyAnalytics(userId) { stats ->
                if (stats != null) {
                    scope.launch(Dispatchers.Main) { weeklyStats = stats }
                }
            }
        }
    }

    val activeModule    = learningPath?.normalizedLearningPath()?.modules?.firstOrNull { it.status == "current" }
    val activeLesson    = activeModule?.lessons?.firstOrNull { it.status == "current" }
    val activeSublesson = activeModule?.currentSublesson()
    val streak = learningPath?.streak ?: 7
    val tier   = learningPath?.tier ?: "B2 Level"

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background ──────────────────────────────────────────────────────
        Image(
            painter      = painterResource(R.drawable.background),
            contentDescription = null,
            modifier     = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ── Content ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Header (Alex, Streak, Settings) ─────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val displayName = sharedPrefs.getString("display_name", "there") ?: "there"
                        val greetingHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        val greeting = when {
                            greetingHour < 12 -> "Good morning"
                            greetingHour < 17 -> "Good afternoon"
                            else              -> "Good evening"
                        }
                        Text(
                            "$greeting, $displayName",
                            color = TextMid,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.home),
                            style = TextStyle(
                                color = TextDark,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Streak chip
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF8E1), RoundedCornerShape(20.dp))
                                .border(0.5.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = BrandAmberDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "$streak",
                                    color = BrandAmberDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        // Settings Button
                        IconButton(
                            onClick = { onNavigateToSettings() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── Daily Stats Section ─────────────────────────────────────
                Column {
                    Text(
                        "Daily Stats",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    HomeDailyStatsCard(
                        tier = tier, 
                        streak = streak, 
                        weeklyStats = weeklyStats,
                        onClick = onNavigateToProgress
                    )
                }

                // ── Learning Path header ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Learning Path",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "View Map",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToRoadmap() }
                    )
                }

                // ── Current Module Card ──────────────────────────────────────
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .shadow(4.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(CardWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandPurple,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (activeModule != null) {
                    HomeDynamicLearningPathCard(
                        module = activeModule,
                        onClick = { onNavigateToActualLearningPath() }
                    )
                } else {
                    HomeNoModuleCard()
                }

                // ── Quick Feature Grid (Timely Duel / AI Lab) ───────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimelyDuelCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDuel
                    )
                    AILabCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAILab
                    )
                }

                // ── Vocab Builder Card ───────────────────────────────────────
                HomeVocabBuilderCard(isLoaded = isVocabLoaded, onClick = onNavigateToVocab)

                // ── Mistake Vault Card ───────────────────────────────────────
                HomeMistakeVaultCard(mistakes = mistakes, onClick = onNavigateToMistakes)

                // ── Activity Section ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activity",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Details",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToProgress() }
                    )
                }

                HomeSpeakingStats(weeklyStats = weeklyStats)

                Spacer(Modifier.height(16.dp))
            }

            // ── Bottom Navigation ────────────────────────────────────────────
            HomeBottomNav(
                selectedTab   = selectedTab,
                onTabSelected = { 
                    selectedTab = it
                    when (it) {
                        1 -> onNavigateToAILab()
                        2 -> onNavigateToVocab()
                        3 -> onNavigateToMistakes()
                    }
                }
            )
        }
    }
}

// ─── Daily Stats Card Component ──────────────────────────────────────────────
@Composable
fun HomeDailyStatsCard(
    tier: String, 
    streak: Int, 
    weeklyStats: List<com.mk.lingocoach.network.DailyStats> = emptyList(),
    onClick: () -> Unit = {}
) {
    val isVocabLoaded = VocabTracker.isLoaded
    val vocabProgress = if (isVocabLoaded) VocabTracker.getOverallProgressPercent() / 100f else 0f
    
    // Calculate dynamic progress from weekly stats
    val totalLessons = weeklyStats.sumOf { it.lessons_completed }
    val totalExercises = weeklyStats.sumOf { it.exercises_attempted }
    val correctExercises = weeklyStats.sumOf { it.exercises_correct }
    
    val lessonsProgress = (totalLessons.coerceIn(0, 10) / 10f).coerceIn(0.05f, 1f)
    val grammarProgress = if (totalExercises > 0) (correctExercises.toFloat() / totalExercises).coerceIn(0.05f, 1f) else 0f
    val speakingMinutes = weeklyStats.sumOf { it.ai_lab_minutes }
    val aiLabSessions = weeklyStats.sumOf { it.ai_lab_sessions }
    val pronunciationProgress = if (aiLabSessions > 0) {
        (speakingMinutes.coerceAtLeast(aiLabSessions).coerceIn(0, 30) / 30f).coerceIn(0.05f, 1f)
    } else 0f
    val fluencyProgress = if (speakingMinutes > 0) (speakingMinutes.coerceIn(0, 60) / 60f).coerceIn(0.05f, 1f) else 0f
    
    // Calculate accuracy - show 0 if no data
    val accuracy = if (totalExercises > 0) {
        ((correctExercises * 100) / totalExercises).coerceIn(0, 100)
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Capsules Row - now dynamic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapsuleProgressIndicator(stringResource(R.string.lessons).uppercase(), lessonsProgress)
                CapsuleProgressIndicator(stringResource(R.string.grammar_check).uppercase(), grammarProgress)
                CapsuleProgressIndicator(stringResource(R.string.vocabulary).uppercase(), vocabProgress)
                CapsuleProgressIndicator(stringResource(R.string.pronunciation).uppercase(), pronunciationProgress)
                CapsuleProgressIndicator(stringResource(R.string.speaking).uppercase(), fluencyProgress)
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0x0D000000))
            Spacer(modifier = Modifier.height(16.dp))

            // Bottom stats row - now dynamic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overall level
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Overall", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(tier, color = BrandPurple, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                // Vertical divider
                Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color(0x1A000000)))

                // Accuracy - now calculated from stats
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.accuracy), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("$accuracy%", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Vertical divider
                Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color(0x1A000000)))

                // Streak
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.daily_streak), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("$streak ${stringResource(R.string.days)}", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun CapsuleProgressIndicator(label: String, progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0EEFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .align(Alignment.BottomCenter)
                    .background(BrandPurple)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextLight,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Current Module Card Component ───────────────────────────────────────────
@Composable
fun HomeCurrentModuleCard(
    module: CurrentModule,
    activeSublesson: CurrentSublesson?,
    onContinue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContinue() }
            .shadow(6.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented circular progress ring with play icon in center
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.5.dp.toPx()
                    val numSegments = 6
                    val sweepAngle = 360f / numSegments - 12f
                    for (i in 0 until numSegments) {
                        val startAngle = i * (360f / numSegments)
                        drawArc(
                            color = if (i < 4) BrandPurple else Color(0xFFDDDAFF),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CURRENT MODULE",
                    color = BrandPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    module.title,
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    activeSublesson?.let { "Lesson ${it.order}: ${it.title}" } ?: module.description,
                    color = TextMid,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Action arrow button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Dynamic Learning Path Card with Progress Circle ─────────────────────────
@Composable
fun HomeDynamicLearningPathCard(
    module: CurrentModule,
    onClick: () -> Unit
) {
    val totalLessons = module.lessons.size
    val completedLessons = module.completedLessonCount()
    val progress = if (totalLessons > 0) completedLessons.toFloat() / totalLessons else 0f
    val progressPercent = (progress * 100).toInt()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)                          // ← unchanged
            .clickable { onClick() }
            .shadow(6.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp), // tighter than 20.dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Animated circular progress indicator ──────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)      // 90 → 80 frees vertical room
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 7.dp.toPx()
                    drawCircle(
                        color = Color(0xFFF0EEFF),
                        radius = size.minDimension / 2,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 7.dp.toPx()
                    drawArc(
                        color = BrandPurple,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
                Text(
                    "$progressPercent%",
                    color = BrandPurple,
                    fontSize = 18.sp,                // 22 → 18, keeps it readable
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(16.dp))            // 20 → 16

            // ── Module info ───────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    module.level.uppercase(),
                    color = BrandPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))        // 4 → 2
                Text(
                    module.title,
                    color = TextDark,
                    fontSize = 16.sp,                // 18 → 16 prevents 2-line wrap on short cards
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))        // 4 → 2
                Text(
                    "${module.lessons.size} Lessons",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))        // 6 → 3
                Text(
                    "${completedLessons}/${totalLessons} Completed",
                    color = BrandGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Arrow icon ────────────────────────────────────────────────
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = BrandPurple,
                modifier = Modifier.size(20.dp)      // 24 → 20
            )
        }
    }
}

@Composable
fun HomeNoModuleCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = BrandPurple,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Complete the assessment to start your path!",
                color = TextMid,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Quick Actions Feature Card (Vocab Duel / AI Lab) ────────────────────────
@Composable
fun HomeFeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(144.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                title,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = textColor.copy(alpha = 0.70f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─── Timely Duel Card Component ───────────────────────────────────────────────
@Composable
fun TimelyDuelCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val amberGold = Color(0xFFFFCA28)
    val amberDeep = Color(0xFFFF8F00)
    val darkBrown = Color(0xFF3E2000)

    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(amberGold, amberDeep)))
            .clickable { onClick() }
    ) {
        // Crossed-swords icon — top-right, partially cropped out, faded grayish
        Icon(
            painter = painterResource(R.drawable.ic_crossed_swords),
            contentDescription = null,
            tint = Color(0xFF5A4000).copy(alpha = 0.18f),
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-16).dp)  // crop it out partially
        )

        // Card content sits on top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = darkBrown,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.timely_duel), color = darkBrown, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "BATTLE AGAINST TIME",
                color = darkBrown.copy(alpha = 0.65f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─── AI Lab Card Component ────────────────────────────────────────────────────
@Composable
fun AILabCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BrandPurple)
            .clickable { onClick() }
    ) {
        // WaveForm icon — top-right, partially cropped out, faded grayish white
        Icon(
            painter = painterResource(R.drawable.ic_waveform),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-10).dp)
        )

        // Card content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.ai_lab), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.pronunciation).uppercase(),
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                maxLines = 1
            )
        }
    }
}

// ─── Vocab Builder Card Component ────────────────────────────────────────────
@Composable
fun HomeVocabBuilderCard(
    isLoaded: Boolean = VocabTracker.isLoaded,
    onClick: () -> Unit = {}
) {
    val progressPercent = if (isLoaded) VocabTracker.getLevelProgress("A1") else 0
    val wordsCountText = if (isLoaded) VocabTracker.getLevelWordsCountText("A1") else "Loading A1 words"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.vocab_builder),
                            color = TextDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = wordsCountText.uppercase(),
                            color = TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(BrandPurpleSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${progressPercent}% A1",
                        color = BrandPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = BrandPurple,
                trackColor = BrandPurpleSoft
            )
        }
    }
}

// ─── Mistake Vault Card Component ────────────────────────────────────────────
@Composable
fun HomeMistakeVaultCard(mistakes: List<Mistake>, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFECEC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = BrandRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.mistake_vault),
                        color = TextDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.review_mistakes),
                        color = TextLight,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Activity / Speaking Stats Chart Card Component ──────────────────────────
@Composable
fun HomeSpeakingStats(weeklyStats: List<com.mk.lingocoach.network.DailyStats> = emptyList()) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    // Normalise XP into bar heights (0.05 min so bars are always visible)
    val heights = if (weeklyStats.size == 7) {
        val maxXp = weeklyStats.maxOf { it.xp_earned }.coerceAtLeast(1)
        weeklyStats.map { (it.xp_earned.toFloat() / maxXp).coerceIn(0.05f, 1f) }
    } else {
        listOf(0.30f, 0.50f, 0.20f, 0.65f, 1.0f, 0.40f, 0.15f)
    }

    // Today's real stats (last item)
    val todayStats   = weeklyStats.lastOrNull()
    val mistakesFixed = todayStats?.mistakes_logged ?: 0
    val lessonsDone  = weeklyStats.sumOf { it.lessons_completed }

    // Weekly XP delta vs previous week (simplified: today vs yesterday)
    val todayXp    = weeklyStats.lastOrNull()?.xp_earned ?: 0
    val yesterdayXp = weeklyStats.getOrNull(weeklyStats.size - 2)?.xp_earned ?: 0
    val xpDiff     = todayXp - yesterdayXp
    val trendText  = if (xpDiff >= 0) "+${xpDiff} XP" else "${xpDiff} XP"
    val trendColor = if (xpDiff >= 0) BrandGreen else BrandRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weekly Overview", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Your XP activity this week", color = TextLight, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (xpDiff >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = trendColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(trendText, color = trendColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bar chart — today is the last bar (index 6)
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { idx, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((60 * heights[idx]).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (idx == 6) BrandPurple else Color(0xFFDDDAFF))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(day,
                            color = if (idx == 6) BrandPurple else TextLight,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0x0D000000))
            Spacer(Modifier.height(16.dp))

            // Stats row — real data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MISTAKES LOGGED", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$mistakesFixed", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text("today", color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("LESSONS DONE", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$lessonsDone", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text("this week", color = BrandPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────
@Composable
fun HomeBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFF))
            .border(
                width = 0.5.dp,
                color = Color(0x1A000000)
            )
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNavItem("HOME", Icons.Default.Home, selectedTab == 0) { onTabSelected(0) }
            HomeNavItem("AI LAB", Icons.Default.Science, selectedTab == 1) { onTabSelected(1) }
            HomeNavItem("VOCAB", Icons.Default.Book, selectedTab == 2) { onTabSelected(2) }
            HomeNavItem("VAULT", Icons.Default.VerifiedUser, selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun HomeNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFCA28)) // Yellow capsule pill highlight
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = TextDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = TextLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (isSelected) TextDark else TextLight,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
        )
    }
}

// ─── Legacy kept composables (backward compat) ────────────────────────────────
@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("See All", color = BrandPurple, fontSize = 12.sp, modifier = Modifier.clickable { })
    }
}

@Composable
private fun ProgressStatRow(label: String, percentage: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextMid, fontSize = 12.sp)
        }
        Text(percentage, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) = HomeNavItem(label, icon, isSelected, onClick)
