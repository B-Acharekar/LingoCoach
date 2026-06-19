package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CurrentLesson
import com.mk.lingocoach.network.CurrentModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Only declare tokens that don't already exist in your theme.
// BrandPurple, BrandPurpleLight, TextDark, TextLight, CardWhite come from your
// existing theme file — they are NOT redeclared here to avoid "Conflicting
// declarations" errors. Only SuccessGreen is new and local to this screen.
private val SuccessGreen = Color(0xFF22C55E)

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * @param launchedFromAssessment  true  → back goes to Assessment, show "Start Learning" CTA
 *                                false → back goes to Home / Lesson (wherever the caller came from)
 */
@Composable
fun LearningPathRoadmapScreen(
    launchedFromAssessment: Boolean = false,
    onNavigateHome: () -> Unit,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBackToAssessment: () -> Unit = {},   // used when launchedFromAssessment == true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val userId = remember { sharedPrefs.getString("session_id", null) ?: "" }
    var learningPath by remember { mutableStateOf(AppCache.learningPath) }
    var isLoading by remember { mutableStateOf(learningPath == null) }

    // Back destination depends on how we arrived here
    val onBack: () -> Unit = if (launchedFromAssessment) onNavigateBackToAssessment else onNavigateHome

    LaunchedEffect(userId) {
        AppCache.loadFromDisk(context)
        AppCache.learningPath?.let {
            learningPath = it
            isLoading = false
        }
        if (userId.isNotBlank()) {
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
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {

            // ── Top Bar ───────────────────────────────────────────────────
            TopBar(
                onBack = onBack,
                onSettings = onNavigateToSettings
            )

            // ── Scrollable Content ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val modules = learningPath?.normalizedLearningPath()?.modules.orEmpty()
                Spacer(Modifier.height(18.dp))
                RoadmapHero(modules)
                Spacer(Modifier.height(26.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("PERSONALIZED ROUTE", color = BrandPurple, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Your milestones", color = Color(0xFF17133B), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("${modules.size} levels", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator(color = BrandPurple, modifier = Modifier.size(32.dp))
                    }
                    modules.isEmpty() -> {
                        Text(
                            stringResource(R.string.no_learning_path_found),
                            color = TextLight,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        modules.forEachIndexed { index, module ->
                            ExpandableModule(
                                stageNumber = index + 1,
                                module = module,
                                onLessonClick = onNavigateToLesson
                            )
                            if (index < modules.lastIndex) {
                                RoadmapConnector(unlocked = module.status != "locked")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }

            // ── Bottom CTA — only shown when arriving from Assessment ─────
            if (launchedFromAssessment) {
                BottomStartLearningBar(onClick = onNavigateHome)
            }
        }
    }
}

@Composable
private fun RoadmapHero(modules: List<CurrentModule>) {
    val completed = modules.count { it.status == "completed" }
    val currentIndex = modules.indexOfFirst { it.status == "current" }.let { if (it < 0) completed else it }
    val overallProgress = if (modules.isEmpty()) 0f else completed.toFloat() / modules.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = BrandPurple.copy(0.18f), spotColor = BrandPurple.copy(0.18f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF17133B), BrandPurple, Color(0xFF9B8CFF))))
            .padding(22.dp)
    ) {
        Box(Modifier.size(150.dp).offset(x = 210.dp, y = (-80).dp).background(Color.White.copy(0.07f), CircleShape))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Color.White.copy(0.14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Route, null, tint = Color.White, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("YOUR FLUENCY ROADMAP", color = Color.White.copy(0.68f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("A clear route forward", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RoadmapStat("LEVEL", if (modules.isEmpty()) "--" else "${currentIndex + 1}/${modules.size}", Modifier.weight(1f))
                RoadmapStat("COMPLETED", "$completed", Modifier.weight(1f))
                RoadmapStat("REMAINING", "${(modules.size - completed).coerceAtLeast(0)}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(0.18f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun RoadmapStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color.White.copy(0.11f), RoundedCornerShape(14.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.62f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF2F2F2), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            stringResource(R.string.learning_path),
            style = TextStyle(
                color = Color(0xFF1A1A1A),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )

        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF2F2F2), CircleShape)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BottomStartLearningBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 12.dp
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            Text(stringResource(R.string.start_learning), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
        }
    }
}

/** Vertical line connecting two modules. Tinted purple when the path is open. */
@Composable
private fun RoadmapConnector(unlocked: Boolean) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(28.dp)
            .background(
                if (unlocked) BrandPurple.copy(alpha = 0.35f) else Color(0xFFE0E0E0),
                RoundedCornerShape(2.dp)
            )
    )
}

/**
 * A card that expands to reveal lesson rows when tapped (only if not locked).
 */
@Composable
private fun ExpandableModule(
    stageNumber: Int,
    module: CurrentModule,
    onLessonClick: (String) -> Unit
) {
    val level = module.level.uppercase()
    val title = module.title
    val lessons = module.lessons
    val current = module.status == "current"
    val locked = module.status == "locked"
    val completed = module.status == "completed"
    var expanded by remember { mutableStateOf(current && !locked) }

    val cardBorder = if (current && !locked)
        androidx.compose.foundation.BorderStroke(1.5.dp, BrandPurple.copy(alpha = 0.45f))
    else
        null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (current) 8.dp else 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (current) BrandPurple.copy(0.18f) else Color.Black.copy(0.05f),
                spotColor = if (current) BrandPurple.copy(0.18f) else Color.Black.copy(0.05f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (locked) Color(0xFFF8F8FC) else CardWhite
        ),
        border = cardBorder,
        onClick = { if (!locked) expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {

            // ── Header row ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Numbered milestone marker
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            when {
                                locked    -> Brush.linearGradient(listOf(Color(0xFFF0F0F4), Color(0xFFF0F0F4)))
                                completed -> Brush.linearGradient(listOf(SuccessGreen, SuccessGreen))
                                else      -> Brush.linearGradient(listOf(BrandPurple, BrandPurpleLight))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        locked -> Icon(Icons.Default.Lock, null, tint = TextLight, modifier = Modifier.size(19.dp))
                        completed -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        else -> Text(stageNumber.toString().padStart(2, '0'), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        level,
                        color = if (locked) TextLight else BrandPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        title,
                        color = if (locked) TextLight else TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (current && !locked) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "In progress · ${lessons.size} lessons",
                            color = BrandPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (completed) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.completed),
                            color = SuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (locked) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.complete_previous_level_to_unlock),
                            color = TextLight,
                            fontSize = 11.sp
                        )
                    }
                }

                // Chevron — only when tappable
                if (!locked) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = BrandPurple
                    )
                }
            }

            if (!locked && lessons.isNotEmpty()) {
                val progress = module.progressPercent() / 100f
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
                        color = if (completed) SuccessGreen else BrandPurple,
                        trackColor = Color(0xFFEDEAF8),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("${module.progressPercent()}%", color = if (completed) SuccessGreen else BrandPurple, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // ── Lesson list (animated) ────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF0EEFF), thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))

                    if (lessons.isEmpty()) {
                        Text(
                            "This level is part of your path. Lessons will appear here after the curriculum syncs.",
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                        )
                    } else {
                        lessons.forEachIndexed { index, lesson ->
                            LessonRow(
                                index = index + 1,
                                lesson = lesson,
                                onClick = {
                                    val targetSublesson = lesson.sublessons.firstOrNull { it.status == "current" }
                                        ?: lesson.sublessons.firstOrNull()
                                    targetSublesson?.let { onLessonClick(it.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(index: Int, lesson: CurrentLesson, onClick: () -> Unit) {
    val locked = lesson.status == "locked"
    val completed = lesson.status == "completed"
    val current = lesson.status == "current"
    val accent = when {
        completed -> SuccessGreen
        current -> BrandPurple
        else -> Color(0xFFAAA6B8)
    }
    val container = when {
        completed -> Color(0xFFF1FBF4)
        current -> Color(0xFFF3F1FF)
        else -> Color(0xFFF7F6FA)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        color = container,
        shape = RoundedCornerShape(17.dp),
        enabled = !locked,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = if (current) 0.25f else 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            completed -> SuccessGreen
                            current -> BrandPurple
                            else -> Color(0xFFE8E6ED)
                        },
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    completed -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    locked -> Icon(Icons.Default.Lock, null, tint = Color(0xFF8F8B9B), modifier = Modifier.size(17.dp))
                    else -> Text(index.toString().padStart(2, '0'), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    lesson.title,
                    color = if (locked) Color(0xFF8F8B9B) else Color(0xFF17133B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    when {
                        completed -> "Completed | ${lesson.sublessons.size} parts"
                        current -> "Ready to learn | ${lesson.sublessons.size} parts"
                        else -> "Complete the previous lesson"
                    },
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(30.dp).background(if (locked) Color.Transparent else accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!locked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}
