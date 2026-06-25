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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.DailyStats
import com.mk.lingocoach.network.ProgressMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// ─── Analytics / Progress Screen ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scroll  = rememberScrollState()

    var weeklyStats by remember { mutableStateOf(AppCache.weeklyStats.orEmpty()) }
    var progressMetrics by remember { mutableStateOf(AppCache.progressMetrics) }
    var isLoading   by remember { mutableStateOf(AppCache.weeklyStats == null) }
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
                    if (stats != null) {
                        AppCache.weeklyStats = stats
                        AppCache.analyticsAt = System.currentTimeMillis()
                        weeklyStats = stats
                    }
                    isLoading   = false
                }
            }
        }
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getProgressMetrics(userId) { metrics ->
                scope.launch(Dispatchers.Main) {
                    if (metrics != null) {
                        AppCache.progressMetrics = metrics
                        AppCache.analyticsAt = System.currentTimeMillis()
                        progressMetrics = metrics
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()
        Scaffold(
            topBar = {
                CommonTopBar(
                    title = stringResource(R.string.analytics),
                    onBack = onNavigateBack,
                    onSettings = onNavigateToSettings,
                    backgroundColor = Color.White.copy(alpha = 0.93f)
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
                    progressMetrics = progressMetrics,
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
    progressMetrics: ProgressMetrics?,
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
    val pronunciationAttempts = weeklyStats.sumOf { it.pronunciation_attempts }
    val pronunciationScoreTotal = weeklyStats.sumOf { it.pronunciation_score_total }
    val duelSessions    = weeklyStats.sumOf { it.duel_sessions }
    val duelCorrect     = weeklyStats.sumOf { it.duel_correct }
    val vocabMastered   = weeklyStats.sumOf { it.vocab_words_mastered }
    val streak          = progressMetrics?.streak ?: (weeklyStats.lastOrNull()?.streak_day ?: 0)

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
    val fallbackGrammarScore = if (totalExercises > 0)
        (correctExercises * 100 / totalExercises).coerceIn(0, 100) else 0
    val fallbackVocabScore = if (isVocabLoaded)
        VocabTracker.getOverallProgressPercent().coerceIn(0, 100) else 0
    val fallbackPronunciationScore = if (pronunciationAttempts > 0)
        (pronunciationScoreTotal / pronunciationAttempts).coerceIn(0, 100)
    else 0
    val fallbackListeningScore  = if (totalExercises > 0)
        ((correctExercises * 65) / totalExercises.coerceAtLeast(1)).coerceIn(0, 100)
    else 0
    val grammarScore = progressMetrics?.grammar_score ?: fallbackGrammarScore
    val vocabScore = progressMetrics?.vocabulary_score ?: fallbackVocabScore
    val listeningScore = progressMetrics?.listening_score ?: fallbackListeningScore
    val pronunciationScore = progressMetrics?.pronunciation_score ?: fallbackPronunciationScore

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
    val prevPronunciationAttempts = weeklyStats.take(3).sumOf { it.pronunciation_attempts }
    val currPronunciationAttempts = weeklyStats.takeLast(3).sumOf { it.pronunciation_attempts }
    val prevPronunciationScore = if (prevPronunciationAttempts > 0)
        weeklyStats.take(3).sumOf { it.pronunciation_score_total } / prevPronunciationAttempts else 0
    val currPronunciationScore = if (currPronunciationAttempts > 0)
        weeklyStats.takeLast(3).sumOf { it.pronunciation_score_total } / currPronunciationAttempts else 0
    val pronounceTrendPct = currPronunciationScore - prevPronunciationScore

    // AI Insight text
    val insightText = buildInsightText(
        grammarScore     = grammarScore,
        pronunciationScore = pronunciationScore,
        vocabScore       = vocabScore
    )

    // Bar chart heights; today is always the final bar.
    val fallbackDayLabels = remember {
        val formatter = SimpleDateFormat("EEE", Locale.getDefault())
        val today = java.util.Calendar.getInstance()
        (6 downTo 0).map { offset ->
            val day = today.clone() as java.util.Calendar
            day.add(java.util.Calendar.DAY_OF_YEAR, -offset)
            formatter.format(day.time)
        }
    }
    val dayLabels = if (weeklyStats.size == 7) {
        weeklyStats.map { stat ->
            runCatching {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val formatter = SimpleDateFormat("EEE", Locale.getDefault())
                formatter.format(parser.parse(stat.date)!!)
            }.getOrDefault("")
        }.mapIndexed { index, label -> label.ifBlank { fallbackDayLabels[index] } }
    } else {
        fallbackDayLabels
    }
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
                        stringResource(R.string.learning_progress).uppercase(),
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
                            stringResource(R.string.percent_this_week, "$trendSign$weeklyTrendPct"),
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
                        stringResource(R.string.weekly),
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
                label = stringResource(R.string.daily_streak).uppercase(),
                value = "$streak ${stringResource(R.string.days)}",
                sub = stringResource(R.string.personal_best),
                subColor = BrandPurple,
                icon = Icons.Default.Whatshot,
                iconTint = BrandAmberDark
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.lessons).uppercase(),
                value = "$totalLessons",
                sub = stringResource(R.string.this_week),
                subColor = TextLight,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = BrandPurple
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.active_days).uppercase(),
                value = "$activeDays",
                sub = stringResource(R.string.in_progress),
                subColor = TextLight,
                icon = Icons.Default.CheckCircle,
                iconTint = BrandGreen
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.mistakes).uppercase(),
                value = "$totalMistakes",
                sub = if (mistakeTrendPct <= 0) stringResource(R.string.percent_vs_last_week, mistakeTrendPct)
                      else stringResource(R.string.percent_vs_last_week, mistakeTrendPct),
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
                stringResource(R.string.skills_overview).uppercase(),
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
                    label = stringResource(R.string.grammar_check).uppercase(),
                    score = grammarScore,
                    trendPct = grammarTrendPct
                )
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.listening).uppercase(),
                    score = listeningScore,
                    trendPct = null,
                    trendLabel = stringResource(R.string.stable)
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.vocabulary).uppercase(),
                    score = vocabScore,
                    trendPct = vocabTrendPct
                )
                SkillBar(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.pronunciation).uppercase(),
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
                    stringResource(R.string.ai_insight).uppercase(),
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
@Composable
private fun buildInsightText(
    grammarScore: Int,
    pronunciationScore: Int,
    vocabScore: Int
): String = when {
    pronunciationScore in 1..59 ->
        stringResource(R.string.insight_pronunciation, pronunciationScore)
    grammarScore in 1..59 ->
        stringResource(R.string.insight_grammar, grammarScore)
    vocabScore < 50 ->
        stringResource(R.string.insight_vocabulary, vocabScore)
    else ->
        stringResource(R.string.insight_great_week)
}
