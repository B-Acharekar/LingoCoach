package com.mk.lingocoach.ui.screens

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.Mistake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

// ─── Vault Design Tokens ──────────────────────────────────────────────────────
private val VaultBg          = Color(0xFFF5F4FF)
private val VaultCardBg      = Color(0xFFFFFFFF)
private val VaultPurple      = Color(0xFF6A5CFF)
private val VaultPurpleSoft  = Color(0xFFF0EEFF)
private val VaultPurpleMid   = Color(0xFF8A79FF)
private val VaultTextDark    = Color(0xFF0D0D0D)
private val VaultTextMid     = Color(0xFF3A3A3A)
private val VaultTextLight   = Color(0xFF6B6B6B)
private val VaultRed         = Color(0xFFE53935)
private val VaultGreen       = Color(0xFF4CAF50)
private val VaultAmber       = Color(0xFFFFB300)
private val VaultTagPronun   = Color(0xFFF3EFFF)  // light purple chip background
private val VaultTagGrammar  = Color(0xFFE8F5E9)  // light green chip background
private val VaultTagVocab    = Color(0xFFFFF3E0)  // light amber chip background
private val VaultWrongBg     = Color(0xFFFFF0F0)
private val VaultRightBg     = Color(0xFFF0FFF4)

// ─── Unified display model ────────────────────────────────────────────────────
data class DisplayMistake(
    val id: String = "",
    val word: String,
    val mistakeType: String,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val timesMissed: Int,
    val source: String,           // "server" | "local"
    val originSource: String = "unknown",
    val mastered: Boolean = false,
    val masteryScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// Filter tabs
enum class VaultTab { ALL, RECENT, PAST_LOGS }

// Retest session state
enum class RetestCardState { IDLE, RECORDING, RECORDED, MARKED_CORRECT, MARKED_WRONG, NEEDS_REVIEW }

private fun cleanMistakeText(value: String?): String {
    val cleaned = value?.trim().orEmpty()
    return if (cleaned.lowercase() in setOf("null", "none", "\"\"", "\"", "'", "''")) "" else cleaned
}

private fun mistakeOrigin(source: String?, mistakeType: String): String {
    val explicit = cleanMistakeText(source).lowercase()
    if (explicit != "unknown" && explicit.isNotBlank()) return explicit
    return when {
        mistakeType.contains("TIMELY_DUEL", true) -> "timely_duel"
        mistakeType.contains("VOCAB", true) -> "vocab_builder"
        mistakeType.contains("FLASHCARD", true) -> "flashcards"
        else -> "lessons"
    }
}

private fun sourceLabel(source: String): String = when (source.lowercase()) {
    "ai_lab" -> "AI LAB"
    "vocab_builder" -> "VOCAB BUILDER"
    "timely_duel" -> "TIMELY DUEL"
    "lessons", "lesson" -> "LESSONS"
    "flashcards" -> "FLASHCARDS"
    else -> "OTHER"
}

private fun DisplayMistake.hasContent(): Boolean =
    listOf(word, userAnswer, correctAnswer, explanation).any { cleanMistakeText(it).isNotBlank() }

private fun Mistake.toDisplayMistake(): DisplayMistake = DisplayMistake(
    id = cleanMistakeText(id),
    word = cleanMistakeText(word),
    mistakeType = cleanMistakeText(mistake_type).ifBlank { "grammar" },
    userAnswer = cleanMistakeText(user_sentence),
    correctAnswer = cleanMistakeText(correct_sentence),
    explanation = cleanMistakeText(explanation),
    timesMissed = times_missed,
    mastered = mastered,
    masteryScore = mastery_score,
    source = "server",
    originSource = mistakeOrigin(source, mistake_type),
    createdAt = runCatching { java.time.Instant.parse(created_at).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
)

// ─── Main Vault Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeVaultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = onNavigateBack,
    onNavigateToVocab: () -> Unit = {},
    onNavigateToAILab: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    var allMistakes   by remember {
        mutableStateOf(AppCache.mistakes.orEmpty().map { it.toDisplayMistake() }.filter { it.hasContent() })
    }
    var isLoading     by remember { mutableStateOf(AppCache.mistakes == null) }
    var selectedTab   by remember { mutableStateOf(VaultTab.ALL) }
    var showRetest    by remember { mutableStateOf(false) }
    var retestList    by remember { mutableStateOf<List<DisplayMistake>>(emptyList()) }

    val userId = remember {
        prefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    // Load mistakes
    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            val localMistakes = VocabTracker.getLocalMistakes(context).map { e ->
                DisplayMistake(
                    word        = e.word,
                    mistakeType = e.mistakeType,
                    userAnswer  = e.userAnswer,
                    correctAnswer = e.correctAnswer,
                    explanation = e.explanation,
                    timesMissed = e.timesMissed,
                    source      = "local",
                    originSource = mistakeOrigin(null, e.mistakeType),
                    createdAt   = e.createdAt
                )
            }.filter { it.hasContent() }
            val cachedServer = AppCache.mistakes.orEmpty()
                .map { it.toDisplayMistake() }
                .filter { it.hasContent() }
            scope.launch(Dispatchers.Main) {
                val cachedWords = cachedServer.map { it.word.lowercase() }.toSet()
                allMistakes = cachedServer + localMistakes.filter { it.word.lowercase() !in cachedWords }
                isLoading   = false
            }
            AssessmentApi.getMistakes(userId) { serverList ->
                if (serverList != null) {
                    AppCache.mistakes = serverList
                    AppCache.mistakesAt = System.currentTimeMillis()
                }
                val serverMapped = (serverList ?: AppCache.mistakes.orEmpty())
                    .map { it.toDisplayMistake() }
                    .filter { it.hasContent() }
                val serverWords = serverMapped.map { it.word.lowercase() }.toSet()
                val uniqueLocal = localMistakes.filter { it.word.lowercase() !in serverWords }
                scope.launch(Dispatchers.Main) {
                    allMistakes = serverMapped + uniqueLocal
                }
            }
        }
    }

    // Derived filtered lists
    val now = System.currentTimeMillis()
    val oneDayMs = TimeUnit.HOURS.toMillis(24)
    val recentMistakes  = allMistakes.filter { now - it.createdAt < oneDayMs }
    val pastLogMistakes = allMistakes.filter { it.mastered || it.timesMissed >= 3 }

    val displayList = when (selectedTab) {
        VaultTab.ALL       -> allMistakes
        VaultTab.RECENT    -> recentMistakes
        VaultTab.PAST_LOGS -> pastLogMistakes
    }

    // Retest overlay
    if (showRetest) {
        RetestModeOverlay(
            mistakes = retestList,
            userId = userId,
            onMistakeResolved = { resolvedId ->
                allMistakes = allMistakes.map { mistake ->
                    if (mistake.id == resolvedId) {
                        mistake.copy(mastered = true, masteryScore = 100)
                    } else {
                        mistake
                    }
                }
            },
            onDismiss = { showRetest = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.mistake_vault),
                onBack = onNavigateBack,
                onSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            HomeBottomNav(
                selectedTab = 3,
                onTabSelected = { index ->
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> onNavigateToAILab()
                        2 -> onNavigateToVocab()
                        3 -> { /* already here */ }
                    }
                }
            )
        },
        containerColor = VaultBg
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VaultPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { VaultSummaryHeader(count = allMistakes.size) }
                item {
                    Spacer(Modifier.height(12.dp))
                    SmartReviewButton(count = allMistakes.size) {
                        val reviewList = allMistakes.take(12)
                        if (reviewList.isNotEmpty()) {
                            retestList = reviewList
                            showRetest = true
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    VaultFilterTabs(
                        selected   = selectedTab,
                        allCount   = allMistakes.size,
                        recentCount = recentMistakes.size,
                        pastCount  = pastLogMistakes.size,
                        onSelect   = { selectedTab = it }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
                if (displayList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint     = VaultGreen,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No slips here!",
                                    color = VaultTextDark,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Keep practising to fill your vault.",
                                    color = VaultTextLight,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(displayList) { mistake ->
                        VaultSlipCard(
                            mistake  = mistake,
                            onPracticeCard = {
                                retestList  = listOf(mistake)
                                showRetest = true
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ─── Summary Header ───────────────────────────────────────────────────────────
@Composable
private fun VaultSummaryHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "AVAILABLE REVIEW",
                color = VaultTextLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = VaultTextDark)) {
                        append("$count Active Slips\n")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = VaultTextDark)) {
                        append("Remaining")
                    }
                }
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(VaultPurpleSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = VaultPurple, modifier = Modifier.size(28.dp))
        }
    }
}

// ─── Smart Review Button ──────────────────────────────────────────────────────
@Composable
private fun SmartReviewButton(count: Int, onClick: () -> Unit) {
    val estimatedMin = (count * 50 / 60).coerceAtLeast(1)
    Button(
        onClick   = onClick,
        enabled   = count > 0,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor = VaultPurple,
            disabledContainerColor = VaultPurple.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.85f)
        )
    ) {
        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "✦ Smart Review Session (${estimatedMin}m)",
            color      = Color.White,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Filter Tabs ──────────────────────────────────────────────────────────────
@Composable
private fun VaultFilterTabs(
    selected: VaultTab,
    allCount: Int,
    recentCount: Int,
    pastCount: Int,
    onSelect: (VaultTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VaultTabChip(
            label    = stringResource(R.string.all_slips_count, allCount),
            selected = selected == VaultTab.ALL,
            onClick  = { onSelect(VaultTab.ALL) }
        )
        VaultTabChip(
            label    = stringResource(R.string.recent_count, recentCount),
            selected = selected == VaultTab.RECENT,
            onClick  = { onSelect(VaultTab.RECENT) }
        )
        VaultTabChip(
            label    = stringResource(R.string.past_logs_count, pastCount),
            selected = selected == VaultTab.PAST_LOGS,
            onClick  = { onSelect(VaultTab.PAST_LOGS) }
        )
    }
}

@Composable
private fun VaultTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) VaultPurple else VaultCardBg)
            .border(1.dp, if (selected) VaultPurple else Color(0xFFE0DFF8), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color      = if (selected) Color.White else VaultTextLight,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─── Slip Card ────────────────────────────────────────────────────────────────
@Composable
private fun VaultSlipCard(
    mistake: DisplayMistake,
    onPracticeCard: () -> Unit
) {
    val isPronun   = mistake.mistakeType.contains("PRONUN", ignoreCase = true)
                  || mistake.mistakeType.contains("PRONUNCIATION", ignoreCase = true)
    val isGrammar  = mistake.mistakeType.contains("GRAMMAR", ignoreCase = true)
    val isVocab    = mistake.mistakeType.contains("VOCAB", ignoreCase = true)

    val tagBg      = when {
        isPronun  -> VaultTagPronun
        isGrammar -> VaultTagGrammar
        else      -> VaultTagVocab
    }
    val tagColor   = when {
        isPronun  -> VaultPurple
        isGrammar -> VaultGreen
        else      -> VaultAmber
    }
    val tagLabel   = when {
        isPronun  -> "PRONUNCIATION"
        isGrammar -> mistake.mistakeType.replace("_", " ").uppercase()
        isVocab   -> "VOCABULARY"
        else      -> mistake.mistakeType.replace("_", " ").uppercase()
    }

    // Relative time
    val timeAgo = run {
        val diff = System.currentTimeMillis() - mistake.createdAt
        when {
            diff < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} mins ago"
            diff < TimeUnit.HOURS.toMillis(24)   -> "${TimeUnit.MILLISECONDS.toHours(diff)} hrs ago"
            else                                  -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        }
    }
    val displayTerm = cleanMistakeText(mistake.word)
        .ifBlank { cleanMistakeText(mistake.correctAnswer) }
        .ifBlank { cleanMistakeText(mistake.userAnswer) }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp), clip = true),
        colors    = CardDefaults.cardColors(containerColor = VaultCardBg),
        shape     = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header row: slip title + tag + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${tagLabel.lowercase().replaceFirstChar { it.uppercase() }} Slip",
                        color      = VaultTextDark,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 24.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    if (displayTerm.isNotBlank()) {
                        Text(
                            displayTerm,
                            color      = VaultPurple,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(tagLabel, color = tagColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VaultPurpleSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "FROM ${sourceLabel(mistake.originSource)}",
                            color = VaultPurple,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = VaultTextLight, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(timeAgo, color = VaultTextLight, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Explanation / context quote
            if (mistake.explanation.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F7FF))
                        .border(1.5.dp, VaultPurple.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(50.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(VaultPurple.copy(alpha = 0.35f))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "\"${mistake.explanation}\"",
                            color      = VaultTextMid,
                            fontSize   = 13.sp,
                            fontStyle  = FontStyle.Italic,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Wrong / Correct rows (grammar mistakes)
            if (!isPronun && mistake.userAnswer.isNotBlank()) {
                VaultAnswerRow(isCorrect = false, label = stringResource(R.string.your_answer), text = mistake.userAnswer)
                Spacer(Modifier.height(8.dp))
                VaultAnswerRow(isCorrect = true,  label = stringResource(R.string.correct), text = mistake.correctAnswer)
                Spacer(Modifier.height(12.dp))
            }

            // Practice Card CTA
            Row(
                modifier = Modifier
                    .clickable { onPracticeCard() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Practice Card",
                    color      = VaultPurple,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = VaultPurple, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun VaultAnswerRow(isCorrect: Boolean, label: String, text: String) {
    val bg    = if (isCorrect) VaultRightBg else VaultWrongBg
    val color = if (isCorrect) VaultGreen   else VaultRed
    val icon  = if (isCorrect) Icons.Default.Check else Icons.Default.Close

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(1.dp))
                Text(
                    buildAnnotatedString {
                        val parts = text.split(" ")
                        parts.forEachIndexed { i, word ->
                            val emphasize = word.length > 4 && i % 3 == 0
                            if (emphasize) {
                                withStyle(SpanStyle(color = VaultPurple, textDecoration = TextDecoration.Underline)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                            if (i < parts.lastIndex) append(" ")
                        }
                    },
                    color    = VaultTextDark,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ─── Retest Mode Full-Screen Overlay ─────────────────────────────────────────
@Composable
fun RetestModeOverlay(
    mistakes: List<DisplayMistake>,
    userId: String,
    onMistakeResolved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var sessionMistakes by remember(mistakes) { mutableStateOf(mistakes) }
    var currentIndex    by remember { mutableStateOf(0) }
    var cardState       by remember { mutableStateOf(RetestCardState.IDLE) }
    var isRecording     by remember { mutableStateOf(false) }
    var masteredIndices by remember { mutableStateOf(setOf<Int>()) }
    var reviewIndices   by remember { mutableStateOf(setOf<Int>()) }
    var sessionDone     by remember { mutableStateOf(false) }
    var typedAnswer     by remember { mutableStateOf("") }
    var feedbackText    by remember { mutableStateOf("") }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current

    val total    = sessionMistakes.size
    val progress = if (total > 0) (currentIndex.toFloat() / total) else 0f

    // Pulse animation for mic button
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFEFECFF), Color(0xFFF8F7FF), Color.White)
                    )
                )
        ) {
            if (total == 0) {
                EmptyRetestSession(onClose = onDismiss)
            } else if (sessionDone) {
                RetestSessionSummary(
                    total         = total,
                    masteredCount = masteredIndices.size,
                    reviewCount   = reviewIndices.size,
                    onClose       = onDismiss,
                    onRetryWrong  = {
                        val wrongList = reviewIndices.mapNotNull { sessionMistakes.getOrNull(it) }
                        if (wrongList.isNotEmpty()) {
                            sessionMistakes = wrongList
                            currentIndex    = 0
                            cardState       = RetestCardState.IDLE
                            masteredIndices = emptySet()
                            reviewIndices   = emptySet()
                            sessionDone     = false
                        } else {
                            onDismiss()
                        }
                    }
                )
            } else {
                val mistake = sessionMistakes.getOrNull(currentIndex)
                if (mistake == null) {
                    EmptyRetestSession(onClose = onDismiss)
                    return@Box
                }
                Column(modifier = Modifier.fillMaxSize().imePadding()) {

                    // Top bar
                    RetestTopBar(
                        current   = currentIndex + 1,
                        total     = total,
                        progress  = progress,
                        onClose   = onDismiss
                    )

                    Spacer(Modifier.height(16.dp))

                    // Flashcard
                    RetestFlashcard(
                        mistake   = mistake,
                        cardState = cardState,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.weight(1f))

                    // Microphone / action zone
                    RetestActionZone(
                        cardState  = cardState,
                        micScale   = if (isRecording) micScale else 1f,
                        onMicTap   = {
                            if (cardState == RetestCardState.IDLE || cardState == RetestCardState.NEEDS_REVIEW) {
                                isRecording = true
                                cardState   = RetestCardState.RECORDING
                                scope.launch {
                                    delay(3000)          // simulate 3-second recording
                                    isRecording = false
                                    cardState   = RetestCardState.RECORDED
                                }
                            } else if (cardState == RetestCardState.RECORDING) {
                                isRecording = false
                                cardState   = RetestCardState.RECORDED
                            }
                        },
                        onMarkCorrect = {
                            val expected = mistake.correctAnswer.ifBlank { mistake.word }
                            val isCorrect = normalizeRetestAnswer(typedAnswer) == normalizeRetestAnswer(expected)
                            if (isCorrect) {
                                masteredIndices = masteredIndices + currentIndex
                                feedbackText = context.getString(R.string.slip_mastered_feedback)
                                cardState = RetestCardState.MARKED_CORRECT
                                if (mistake.source == "server" && mistake.id.isNotBlank()) {
                                    AssessmentApi.markMistakeResolved(userId, mistake.id) { ok ->
                                        if (ok) {
                                            scope.launch(Dispatchers.Main) {
                                                onMistakeResolved(mistake.id)
                                            }
                                        }
                                    }
                                }
                                scope.launch {
                                    delay(750)
                                    typedAnswer = ""
                                    feedbackText = ""
                                    advanceCard(currentIndex, total, { currentIndex = it }, { sessionDone = true })
                                    cardState = RetestCardState.IDLE
                                }
                            } else {
                                reviewIndices = reviewIndices + currentIndex
                                feedbackText = context.getString(R.string.slip_retry_feedback)
                                cardState = RetestCardState.MARKED_WRONG
                                scope.launch {
                                    delay(900)
                                    typedAnswer = ""
                                    feedbackText = ""
                                    advanceCard(currentIndex, total, { currentIndex = it }, { sessionDone = true })
                                    cardState = RetestCardState.IDLE
                                }
                            }
                        },
                        onMarkWrong = {
                            reviewIndices = reviewIndices + currentIndex
                            cardState = RetestCardState.MARKED_WRONG
                            scope.launch {
                                delay(600)
                                typedAnswer = ""
                                feedbackText = ""
                                advanceCard(currentIndex, total, { currentIndex = it }, { sessionDone = true })
                                cardState = RetestCardState.IDLE
                            }
                        },
                        onNeedsReview = {
                            reviewIndices = reviewIndices + currentIndex
                            cardState = RetestCardState.IDLE
                            typedAnswer = ""
                            feedbackText = ""
                            advanceCard(currentIndex, total, { currentIndex = it }, { sessionDone = true })
                        },
                        typedAnswer = typedAnswer,
                        onTypedAnswerChange = {
                            typedAnswer = it
                            feedbackText = ""
                        },
                        feedbackText = feedbackText
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun advanceCard(current: Int, total: Int, setIndex: (Int) -> Unit, setDone: () -> Unit) {
    if (current + 1 >= total) setDone() else setIndex(current + 1)
}

private fun normalizeRetestAnswer(value: String): String {
    return value
        .lowercase()
        .replace(Regex("[^a-z0-9']+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

// ─── Retest: Top Bar ──────────────────────────────────────────────────────────
@Composable
private fun EmptyRetestSession(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = VaultGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No slips to review",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = VaultTextDark,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your mistake vault is empty right now.",
            color = VaultTextLight,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VaultPurple)
        ) {
            Text(stringResource(R.string.back_to_vault), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RetestTopBar(current: Int, total: Int, progress: Float, onClose: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = VaultTextMid)
            }
            Text(
                "Slip $current of $total",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = VaultTextDark
            )
            // Dot indicators (show up to 5)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val showDots = minOf(total, 5)
                repeat(showDots) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == (current - 1) % showDots) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (i == (current - 1) % showDots) VaultPurple else Color(0xFFDDDAFF))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress    = { progress },
            modifier    = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
            color       = VaultPurple,
            trackColor  = Color(0xFFDDDAFF)
        )
    }
}

// ─── Retest: Flashcard ────────────────────────────────────────────────────────
@Composable
private fun RetestFlashcard(
    mistake: DisplayMistake,
    cardState: RetestCardState,
    modifier: Modifier = Modifier
) {
    val isPronun = mistake.mistakeType.contains("PRONUN", ignoreCase = true)
                || mistake.mistakeType.contains("PRONUNCIATION", ignoreCase = true)
    val isGrammar = mistake.mistakeType.contains("GRAMMAR", ignoreCase = true)

    val tagLabel = when {
        isPronun  -> "Pronunciation Remediation"
        isGrammar -> "Grammar Remediation"
        else      -> "Vocabulary Review"
    }

    // Card elevation animation on state change
    val cardElevation by animateFloatAsState(
        targetValue  = if (cardState == RetestCardState.MARKED_CORRECT || cardState == RetestCardState.MARKED_WRONG) 8f else 4f,
        animationSpec = tween(300),
        label        = "card_elevation"
    )
    val cardBorderColor = when (cardState) {
        RetestCardState.MARKED_CORRECT -> VaultGreen
        RetestCardState.MARKED_WRONG   -> VaultRed
        RetestCardState.RECORDED       -> VaultPurple
        else                           -> Color.Transparent
    }

    Card(
        modifier = modifier
            .shadow(cardElevation.dp, RoundedCornerShape(24.dp))
            .border(2.dp, cardBorderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape  = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tag chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VaultPurpleSoft)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(tagLabel, color = VaultPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            // Word
            Text(
                mistake.word,
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = VaultTextDark,
                textAlign  = TextAlign.Center
            )

            // Phonetic (use explanation as phonetic hint if it looks like IPA, else derive)
            val phoneticHint = if (mistake.explanation.contains("/") || mistake.explanation.contains("ˈ"))
                mistake.explanation.substringBefore(" ").trim()
            else
                "/${mistake.word.lowercase()}/"

            Spacer(Modifier.height(6.dp))
            Text(phoneticHint, color = VaultPurple, fontSize = 14.sp, fontStyle = FontStyle.Italic)

            Spacer(Modifier.height(20.dp))

            // Play audio button with TTS
            val context = LocalContext.current
            OutlinedButton(
                onClick  = {
                    // Use Android Text-to-Speech
                    var ttsEngine: android.speech.tts.TextToSpeech? = null
                    ttsEngine = android.speech.tts.TextToSpeech(context) { status ->
                        if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                            ttsEngine?.language = java.util.Locale.US
                            ttsEngine?.speak(mistake.word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = VaultTextDark),
                border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0DFFA))
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = VaultPurple, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.hear_pronunciation), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Instruction or feedback
            val instructionText = when (cardState) {
                RetestCardState.IDLE, RetestCardState.NEEDS_REVIEW ->
                    "Tap the microphone below and speak the word out loud. Focus on the /${mistake.word.take(4).lowercase()}/ syllable stress."
                RetestCardState.RECORDING -> "Recording… speak clearly now."
                RetestCardState.RECORDED  -> "Recording complete. How did you do?"
                RetestCardState.MARKED_CORRECT -> "Great job! Moving on…"
                RetestCardState.MARKED_WRONG   -> "Noted. Keep practising!"
            }

            Text(
                instructionText,
                color     = VaultTextMid,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Retest: Action Zone (Mic + post-recording buttons) ──────────────────────
@Composable
private fun RetestActionZone(
    cardState: RetestCardState,
    micScale: Float,
    onMicTap: () -> Unit,
    onMarkCorrect: () -> Unit,
    onMarkWrong: () -> Unit,
    onNeedsReview: () -> Unit,
    typedAnswer: String,
    onTypedAnswerChange: (String) -> Unit,
    feedbackText: String
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState  = cardState,
            transitionSpec = {
                fadeIn(tween(250)) togetherWith fadeOut(tween(250))
            },
            label = "action_zone"
        ) { state ->
            when (state) {
                RetestCardState.IDLE, RetestCardState.NEEDS_REVIEW -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RetestTypedAnswerBox(
                            typedAnswer = typedAnswer,
                            onTypedAnswerChange = onTypedAnswerChange,
                            feedbackText = feedbackText,
                            onVerify = onMarkCorrect
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onNeedsReview) {
                            Text(stringResource(R.string.review_rule_again), color = VaultTextLight, fontSize = 12.sp)
                        }
                    }
                }
                RetestCardState.RECORDING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MicButton(scale = micScale, isActive = true, onClick = onMicTap)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = VaultRed, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Recording… tap to stop",
                                color      = VaultRed,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp
                            )
                        }
                    }
                }
                RetestCardState.RECORDED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RetestTypedAnswerBox(
                            typedAnswer = typedAnswer,
                            onTypedAnswerChange = onTypedAnswerChange,
                            feedbackText = feedbackText,
                            onVerify = onMarkCorrect
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier            = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick  = onMarkWrong,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = VaultRed),
                                border   = androidx.compose.foundation.BorderStroke(1.5.dp, VaultRed.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.again), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = onNeedsReview) {
                            Text(stringResource(R.string.skip_for_now), color = VaultTextLight, fontSize = 12.sp)
                        }
                    }
                }
                RetestCardState.MARKED_CORRECT -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VaultGreen, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.mastered_exclaim), color = VaultGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
                RetestCardState.MARKED_WRONG -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = VaultRed, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.added_for_review), color = VaultRed, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

// ─── Mic Button ───────────────────────────────────────────────────────────────
@Composable
private fun RetestTypedAnswerBox(
    typedAnswer: String,
    onTypedAnswerChange: (String) -> Unit,
    feedbackText: String,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Type the corrected answer",
            color = VaultTextDark,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = onTypedAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.write_correction_here), color = VaultTextLight) },
            minLines = 1,
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = VaultTextDark,
                unfocusedTextColor = VaultTextDark,
                cursorColor = VaultPurple,
                focusedBorderColor = VaultPurple,
                unfocusedBorderColor = Color(0xFFE0DFFA)
            )
        )
        if (feedbackText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                feedbackText,
                color = if (feedbackText.startsWith("Correct")) VaultGreen else VaultRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onVerify,
            enabled = typedAnswer.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VaultPurple)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.verify_answer), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MicButton(scale: Float, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier            = Modifier
            .size((72 * scale).dp)
            .clip(CircleShape)
            .background(if (isActive) VaultRed else VaultPurple)
            .clickable { onClick() },
        contentAlignment    = Alignment.Center
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = stringResource(R.string.record),
            tint               = Color.White,
            modifier           = Modifier.size(32.dp)
        )
    }
    if (isActive) {
        // Outer ring pulse
        Box(
            modifier = Modifier
                .size((72 * scale * 1.3f).dp)
                .clip(CircleShape)
                .background(VaultRed.copy(alpha = 0.12f))
        )
    }
}

// ─── Retest: Session Summary Screen ──────────────────────────────────────────
@Composable
private fun RetestSessionSummary(
    total: Int,
    masteredCount: Int,
    reviewCount: Int,
    onClose: () -> Unit,
    onRetryWrong: () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VaultAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint     = VaultAmber,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Session Complete!",
            fontSize   = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = VaultTextDark,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You reviewed $total slip${if (total != 1) "s" else ""}.",
            color    = VaultTextLight,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Stats row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStatBox(icon = Icons.Default.CheckCircle, label = stringResource(R.string.mastered), value = masteredCount, color = VaultGreen)
            SummaryStatBox(icon = Icons.Default.Refresh,      label = stringResource(R.string.review),   value = reviewCount,   color = VaultAmber)
            SummaryStatBox(icon = Icons.AutoMirrored.Filled.MenuBook, label = stringResource(R.string.total), value = total, color = VaultPurple)
        }

        Spacer(Modifier.height(40.dp))

        if (reviewCount > 0) {
            Button(
                onClick  = onRetryWrong,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = VaultPurple)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.retry_slips, reviewCount), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick  = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = VaultPurple),
            border   = androidx.compose.foundation.BorderStroke(1.5.dp, VaultPurple.copy(alpha = 0.4f))
        ) {
            Text(stringResource(R.string.back_to_vault), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryStatBox(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = VaultTextLight, fontWeight = FontWeight.Medium)
    }
}

// ─── Backward-compat wrappers (kept for old call sites) ──────────────────────

@Composable
fun MistakeCard(mistake: Mistake) {
    VaultSlipCard(
        mistake = mistake.toDisplayMistake(),
        onPracticeCard = {}
    )
}

@Composable
fun DisplayMistakeCard(mistake: DisplayMistake) {
    VaultSlipCard(mistake = mistake, onPracticeCard = {})
}
