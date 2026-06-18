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
import androidx.compose.ui.graphics.Color
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
                Spacer(Modifier.height(28.dp))

                // Hero label
                Text(
                    stringResource(R.string.your_journey),
                    style = TextStyle(
                        color = BrandPurple,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    stringResource(R.string.personalized_route_to_fluency),
                    style = TextStyle(
                        color = TextLight,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                val modules = learningPath?.normalizedLearningPath()?.modules.orEmpty()
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

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.97f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(BrandPurpleLight, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = BrandPurple,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            stringResource(R.string.learning_path),
            style = TextStyle(
                color = TextDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )

        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(40.dp)
                .background(BrandPurpleLight, CircleShape)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                
                tint = BrandPurple,
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
            .width(3.dp)
            .height(36.dp)
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
        androidx.compose.foundation.BorderStroke(2.dp, BrandPurple)
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

                // Status icon bubble
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                locked    -> Color(0xFFF0F0F4)
                                completed -> SuccessGreen.copy(alpha = 0.12f)
                                else      -> BrandPurple.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            locked    -> Icons.Default.Lock
                            completed -> Icons.Default.Check
                            else      -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        tint = when {
                            locked    -> TextLight
                            completed -> SuccessGreen
                            else      -> BrandPurple
                        },
                        modifier = Modifier.size(22.dp)
                    )
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

            // ── Lesson list (animated) ────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
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
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        enabled = !locked
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lesson number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(BrandPurpleLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    color = if (completed) SuccessGreen else BrandPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                lesson.title,
                color = if (locked) TextLight else TextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = when {
                    locked -> TextLight
                    completed -> SuccessGreen
                    else -> BrandPurple
                },
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
