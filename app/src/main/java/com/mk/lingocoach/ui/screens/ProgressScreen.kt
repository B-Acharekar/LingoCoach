package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.DailyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─── Analytics / Progress Screen ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scroll  = rememberScrollState()

    var weeklyStats by remember { mutableStateOf<List<DailyStats>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var isVocabLoaded by remember { mutableStateOf(VocabTracker.isLoaded) }

    val userId = remember {
        prefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            if (!VocabTracker.isLoaded) {
                VocabTracker.init(context)
            }
            scope.launch(Dispatchers.Main) {
                isVocabLoaded = VocabTracker.isLoaded
            }
        }
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getWeeklyAnalytics(userId) { stats ->
                scope.launch(Dispatchers.Main) {
                    weeklyStats = stats ?: emptyList()
                    isLoading   = false
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
                    title = {
                        Text(
                            "Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextDark
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = TextDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.93f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = BrandPurple) }
            } else {
                AnalyticsContent(
                    weeklyStats = weeklyStats,
                    isVocabLoaded = isVocabLoaded,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    weeklyStats: List<DailyStats>,
    isVocabLoaded: Boolean,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()
    // ── Derived metrics ───────────────────────────────────────────────────────
    val totalXp         = weeklyStats.sumOf { it.xp_earned }
    val totalXpHours    = totalXp / 60f          // rough "hours" proxy (1 XP ~ 1 min)
    val totalLessons    = weeklyStats.sumOf { it.lessons_completed }
    val totalMistakes   = weeklyStats.sumOf { it.mistakes_logged }
    val totalExercises  = weeklyStats.sumOf { it.exercises_attempted }
    val correctExercises = weeklyStats.sumOf { it.exercises_correct }
    val aiLabSessions   = weeklyStats.sumOf { it.ai_lab_sessions }
    val aiLabMinutes    = weeklyStats.sumOf { it.ai_lab_minutes }
    val duelSessions    = weeklyStats.sumOf { it.duel_sessions }
    val duelCorrect     = weeklyStats.sumOf { it.duel_correct }
    val vocabMastered   = weeklyStats.sumOf { it.vocab_words_mastered }
    val streak          = weeklyStats.lastOrNull()?.streak_day ?: 0

    // Weekly XP trend vs first-half avg
    val prevHalfXp = weeklyStats.take(3).sumOf { it.xp_earned }
    val currHalfXp = weeklyStats.takeLast(3).sumOf { it.xp_earned }
    val weeklyTrendPct = if (prevHalfXp > 0)
        ((currHalfXp - prevHalfXp) * 100 / prevHalfXp)
    else if (currHalfXp > 0) 100 else 0

    // Active modules = days with at least one lesson completed
    val activeDays = weeklyStats.count { it.lessons_completed > 0 }

    // Mistakes vs previous half
    val prevMistakes = weeklyStats.take(3).sumOf { it.mistakes_logged }
    val currMistakes = weeklyStats.takeLast(3).sumOf { it.mistakes_logged }
    val mistakeTrendPct = if (prevMistakes > 0)
        ((currMistakes - prevMistakes) * 100 / prevMistakes)
    else 0

    // Skills derived from real data
    val grammarScore    = if (totalExercises > 0)
        (correctExercises * 100 / totalExercises).coerceIn(0, 100) else 0
    val vocabScore      = if (isVocabLoaded)
        VocabTracker.getOverallProgressPercent().coerceIn(0, 100) else 0
    val fluencyScore    = (aiLabMinutes.coerceIn(0, 60) * 100 / 60).coerceIn(0, 100)
    val pronunciationScore = if (aiLabSessions > 0)
        ((aiLabMinutes.coerceAtLeast(aiLabSessions) * 100) / 30).coerceIn(0, 100)
    else 0
    val listeningScore  = if (totalExercises > 0)
        ((correctExercises * 65) / totalExercises.coerceAtLeast(1)).coerceIn(0, 100)
    else 0

    // Grammar trend vs prev half
    val prevGrammarCorrect  = weeklyStats.take(3).sumOf { it.exercises_correct }
    val prevGrammarAttempts = weeklyStats.take(3).sumOf { it.exercises_attempted }
    val currGrammarCorrect  = weeklyStats.takeLast(3).sumOf { it.exercises_correct }
    val currGrammarAttempts = weeklyStats.takeLast(3).sumOf { it.exercises_attempted }
    val grammarTrendPct = if (prevGrammarAttempts > 0 && currGrammarAttempts > 0) {
        val prev = prevGrammarCorrect * 100 / prevGrammarAttempts
        val curr = currGrammarCorrect * 100 / currGrammarAttempts
        curr - prev
    } else 0

    val prevVocab = weeklyStats.take(3).sumOf { it.vocab_words_mastered }
    val currVocab = weeklyStats.takeLast(3).sumOf { it.vocab_words_mastered }
    val vocabTrendPct = currVocab - prevVocab
    val prevAiMinutes = weeklyStats.take(3).sumOf { it.ai_lab_minutes }
    val currAiMinutes = weeklyStats.takeLast(3).sumOf { it.ai_lab_minutes }
    val pronounceTrendPct = currAiMinutes - prevAiMinutes

    // AI Insight text
    val insightText = buildInsightText(
        grammarScore     = grammarScore,
        pronunciationScore = pronunciationScore,
        aiLabSessions    = aiLabSessions,
        fluencyScore     = fluencyScore,
        vocabScore       = vocabScore
    )

    // Bar chart heights
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val xpValues  = if (weeklyStats.size == 7) weeklyStats.map { it.xp_earned } else List(7) { 0 }
    val maxXp     = xpValues.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barHeights = xpValues.map { (it.toFloat() / maxXp).coerceIn(0.05f, 1f) }

    val displayHours = if (totalXpHours >= 1f) "%.1fh".format(totalXpHours) else "${(totalXpHours * 60).toInt()}m"
    val trendSign    = if (weeklyTrendPct >= 0) "+" else ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Learning Progress card ────────────────────────────────────────────
        AnalyticsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "LEARNING PROGRESS",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            displayHours,
                            color = TextDark,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$trendSign$weeklyTrendPct% this week",
                            color = if (weeklyTrendPct >= 0) BrandGreen else BrandRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(BrandPurpleSoft, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Weekly",
                        color = BrandPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            AnalyticsBarChart(barHeights = barHeights, dayLabels = dayLabels)
        }

        // ── Stats grid (2x2) ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "DAILY STREAK",
                value = "${streak} Days",
                sub = "Personal Best",
                subColor = BrandPurple,
                icon = Icons.Default.Whatshot,
                iconTint = BrandAmberDark
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "LESSONS",
                value = "$totalLessons",
                sub = "This Week",
                subColor = TextLight,
                icon = Icons.Default.MenuBook,
                iconTint = BrandPurple
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "ACTIVE DAYS",
                value = "$activeDays",
                sub = "In Progress",
                subColor = TextLight,
                icon = Icons.Default.CheckCircle,
                iconTint = BrandGreen
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "MISTAKES",
                value = "$totalMistakes",
                sub = if (mistakeTrendPct <= 0) "${mistakeTrendPct}% vs last week"
                      else "+${mistakeTrendPct}% vs last week",
                subColor = if (mistakeTrendPct <= 0) BrandGreen else BrandRed,
                icon = Icons.Default.Warning,
                iconTint = BrandAmberDark
            )
        }

        // ── AI Insight ────────────────────────────────────────────────────────
        AiInsightCard(insightText = insightText)

        // ── Skills Overview ───────────────────────────────────────────────────
        AnalyticsCard {
            Text(
                "SKILLS OVERVIEW",
                color = TextLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = "GRAMMAR",
                    score = grammarScore,
                    trendPct = grammarTrendPct
                )
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = "LISTENING",
                    score = listeningScore,
                    trendPct = null,
                    trendLabel = "Stable"
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = "VOCABULARY",
                    score = vocabScore,
                    trendPct = vocabTrendPct
                )
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = "PRONUNCIATION",
                    score = pronunciationScore,
                    trendPct = pronounceTrendPct
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Reusable sub-composables ─────────────────────────────────────────────────

@Composable
private fun AnalyticsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false),
        colors  = CardDefaults.cardColors(containerColor = Color.White),
        shape   = RoundedCornerShape(20.dp),
        content = { Column(modifier = Modifier.padding(20.dp), content = content) }
    )
}

@Composable
private fun AnalyticsBarChart(
    barHeights: List<Float>,
    dayLabels: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        barHeights.forEachIndexed { idx, h ->
            val isToday = (idx == barHeights.size - 1)
            val animH by animateFloatAsState(
                targetValue = h,
                animationSpec = tween(700 + idx * 60),
                label = "bar$idx"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height((72 * animH).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (isToday) BrandPurple else Color(0xFFDDDAFF))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    dayLabels[idx],
                    color = if (isToday) BrandPurple else TextLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    subColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(20.dp), clip = false),
        colors  = CardDefaults.cardColors(containerColor = Color.White),
        shape   = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = subColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AiInsightCard(insightText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F2FF)),
        shape  = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI INSIGHT",
                    color = BrandPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    insightText,
                    color = TextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun SkillBar(
    modifier: Modifier = Modifier,
    label: String,
    score: Int,
    trendPct: Int?,
    trendLabel: String? = null
) {
    val animScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900),
        label = "skill_$label"
    )
    val trendColor = when {
        trendPct == null   -> TextLight
        trendPct > 0       -> BrandGreen
        trendPct < 0       -> BrandRed
        else               -> TextLight
    }
    val trendText = when {
        trendLabel != null -> trendLabel
        trendPct != null && trendPct >= 0 -> "+${trendPct}%"
        trendPct != null   -> "$trendPct%"
        else               -> ""
    }

    Column(modifier = modifier) {
        Text(label, color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$score%", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(trendText, color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEECFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animScore)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(BrandPurple)
            )
        }
    }
}

// ─── Insight text builder ─────────────────────────────────────────────────────
private fun buildInsightText(
    grammarScore: Int,
    pronunciationScore: Int,
    aiLabSessions: Int,
    fluencyScore: Int,
    vocabScore: Int
): String = when {
    pronunciationScore in 1..59 && aiLabSessions == 0 ->
        "Your pronunciation score is low. Recommended: Speaking Practice."
    pronunciationScore in 1..59 ->
        "Your pronunciation score has dropped ${100 - pronunciationScore}%. Recommended: Speaking Practice."
    grammarScore in 1..59 ->
        "Your grammar accuracy is at $grammarScore%. Recommended: Grammar Drills."
    fluencyScore in 1..59 ->
        "You have not practised speaking this week. Recommended: AI Lab session."
    vocabScore < 50 ->
        "Your vocabulary mastery is at $vocabScore%. Recommended: Vocab Builder session."
    else ->
        "Great work this week! Keep practising daily to maintain your streak."
}
