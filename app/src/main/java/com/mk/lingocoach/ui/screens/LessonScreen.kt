package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mk.lingocoach.R
import com.mk.lingocoach.network.*
import com.mk.lingocoach.viewmodel.*
import kotlin.random.Random

// ─── Uses the same design tokens as HomeScreen ───────────────────────────────
// BrandPurple, BrandPurpleLight, BrandPurpleSoft, BrandAmber, BrandRed,
// BrandGreen, TextDark, TextMid, TextLight, CardWhite, CardBorderColor
// are all imported from HomeScreen.kt (same package)

@Composable
fun LessonScreen(
    sublessonId: String,
    onNavigateBack: () -> Unit
) {
    val context     = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val userId      = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    val lessonViewModel: LessonViewModel = viewModel()
    val uiState by lessonViewModel.uiState.collectAsState()

    var activeSublessonId by remember { mutableStateOf(sublessonId) }
    var dropdownExpanded  by remember { mutableStateOf(false) }

    LaunchedEffect(activeSublessonId) {
        lessonViewModel.reset()
        lessonViewModel.loadSublesson(activeSublessonId, userId)
    }

    val nextSublessonId = remember(uiState.learningPath, activeSublessonId) {
        val path = uiState.learningPath ?: return@remember null
        var foundCurrent = false
        var nextId: String? = null
        for (module in path.modules) {
            for (lesson in module.lessons) {
                for (sub in lesson.sublessons) {
                    if (foundCurrent) { nextId = sub.id; break }
                    if (sub.id == activeSublessonId) foundCurrent = true
                }
                if (nextId != null) break
            }
            if (nextId != null) break
        }
        nextId
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()

        when {
            uiState.phase == Phase.LOADING -> LsLoadingView()

            uiState.phase == Phase.ERROR -> LsErrorView(
                onBack  = onNavigateBack,
                onRetry = { lessonViewModel.loadSublesson(activeSublessonId, userId) }
            )

            uiState.phase == Phase.COMPLETE -> LsProfessionalCompletionView(
                totalXp         = uiState.totalXpEarned,
                totalExercises  = uiState.originalExercises.size,
                correctCount    = uiState.correctOriginalCount,
                nextSublessonId = nextSublessonId,
                onContinueNext  = { nextId -> activeSublessonId = nextId },
                onBack          = onNavigateBack
            )

            else -> {
                val sub = uiState.sublesson ?: return@Box

                val currentModule = uiState.learningPath?.modules?.firstOrNull { module ->
                    module.lessons.any { lesson ->
                        lesson.sublessons.any { s -> s.id == activeSublessonId }
                    }
                }
                val currentLesson = currentModule?.lessons?.firstOrNull { lesson ->
                    lesson.sublessons.any { s -> s.id == activeSublessonId }
                }
                val currentSublessonsList = currentLesson?.sublessons ?: emptyList()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    LsTopBar(
                        title = sub.title,
                        exerciseCount = uiState.originalExercises.size,
                        currentIndex = uiState.answeredCount,
                        isContentPhase = uiState.phase == Phase.CONTENT,
                        retryCount = uiState.retryQueue.size,
                        onBack = onNavigateBack
                    )

                    // Dropdown Selector above question / content
                    if (currentSublessonsList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFF1EFFF))
                                    .border(1.dp, BrandPurple.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                    .clickable { dropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(Modifier.size(36.dp).background(BrandPurple, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("LESSON PART ${sub.order} OF ${currentSublessonsList.size}", color = BrandPurple, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                                        Text(sub.title, color = Color(0xFF17133B), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(CardWhite)
                            ) {
                                currentSublessonsList.forEach { subItem ->
                                    val isSelected = subItem.id == activeSublessonId
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Part ${subItem.order}: ${subItem.title}",
                                                color = if (isSelected) BrandPurple else TextDark,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            dropdownExpanded = false
                                            activeSublessonId = subItem.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.phase == Phase.CONTENT) {
                        LsContentPhase(
                            sublesson          = sub,
                            showCompleteButton = uiState.originalExercises.isEmpty(),
                            completionSent     = uiState.completionSent,
                            onStartExercises   = { lessonViewModel.startExercises() },
                            onComplete         = { lessonViewModel.completeSublesson(userId, activeSublessonId) }
                        )
                    } else {
                        // activeExercise is guaranteed set atomically in the VM — no null flash
                        val activeExercise = uiState.activeExercise
                        if (activeExercise != null) {
                            LsExercisePhase(
                                exercise           = activeExercise,
                                sublesson          = sub,
                                // Show "Exercise N of M" where N = answered so far + 1
                                exerciseNumber     = uiState.answeredCount + 1,
                                totalExercises     = uiState.originalExercises.size + uiState.retryQueue.size,
                                correctCount       = uiState.correctOriginalCount,
                                answerState        = uiState.answerState,
                                feedback           = uiState.feedback,
                                isSubmitting       = uiState.isSubmitting,
                                // VM owns this flag now — no inline derivation
                                showCompleteButton = uiState.showCompleteButton,
                                completionSent     = uiState.completionSent,
                                onSubmit           = { answer -> lessonViewModel.submitAnswer(answer, userId) },
                                onAdvance          = { lessonViewModel.advance() },
                                onComplete         = { lessonViewModel.completeSublesson(userId, activeSublessonId) }
                            )
                        }
                        // If activeExercise == null and phase == EXERCISE it means
                        // advance() computed allDone=true but completeSublesson hasn't been
                        // called yet (e.g. no exercises in lesson). Show complete button.
                        if (activeExercise == null && uiState.showCompleteButton) {
                            LsCompleteButton(
                                isVisible = true,
                                isEnabled = !uiState.completionSent,
                                sublesson = sub,
                                totalExercises = uiState.originalExercises.size,
                                correctCount = uiState.correctOriginalCount,
                                onClick   = { lessonViewModel.completeSublesson(userId, activeSublessonId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────
@Composable
fun LsTopBar(
    title: String,
    exerciseCount: Int,
    currentIndex: Int,
    isContentPhase: Boolean,
    retryCount: Int = 0,
    onBack: () -> Unit
) {
    val totalSteps = exerciseCount + 1
    val rawProgress = if (isContentPhase) 0.08f else (currentIndex + 1f) / totalSteps.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(rawProgress, tween(600), label = "lsProgress")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(0.5.dp, Color(0xFFE9E6F2))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BrandPurpleSoft)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BrandPurple,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    title,
                    style = TextStyle(color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isContentPhase) "Learn" else "Exercise $currentIndex of $exerciseCount",
                    color = TextLight,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF3D1), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFFE1A3), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = BrandAmberDark, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("20 XP", color = BrandAmberDark, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = BrandPurple,
            trackColor = Color(0xFFE8E4FF),
            strokeCap = StrokeCap.Round
        )

        AnimatedVisibility(visible = retryCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = BrandAmberDark, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retry round | $retryCount left", color = BrandAmberDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Content Phase ────────────────────────────────────────────────────────────
@Composable
fun LsContentPhase(
    sublesson: SublessonDetail,
    showCompleteButton: Boolean,
    completionSent: Boolean,
    onStartExercises: () -> Unit,
    onComplete: () -> Unit
) {
    val scroll = rememberScrollState()
    val explanations = remember(sublesson.content_blocks) { sublesson.content_blocks.filter { it.type == "explanation" } }
    val examples = remember(sublesson.content_blocks) { sublesson.content_blocks.filter { it.type == "example" } }
    val tips = remember(sublesson.content_blocks) { sublesson.content_blocks.filter { it.type == "tip" } }
    val supportingBlocks = remember(sublesson.content_blocks) {
        sublesson.content_blocks.filter { it.type !in setOf("explanation", "example", "tip") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF17133B), BrandPurple)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(Color.White.copy(0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoStories, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("LEARN", color = Color.White.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(15.dp))
                Text(sublesson.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Build the idea step by step, see it in context, then prove you can use it.",
                    color = Color.White.copy(0.72f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(17.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LsLessonMetric(Icons.Default.Schedule, "3-5 min", Modifier.weight(1f))
                    LsLessonMetric(Icons.Default.Lightbulb, "${explanations.size.coerceAtLeast(1)} ideas", Modifier.weight(1f))
                    LsLessonMetric(Icons.Default.EditNote, "${sublesson.exercises.size} checks", Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        LsLearningOutcomes(sublesson.title, examples.isNotEmpty(), tips.isNotEmpty())
        Spacer(Modifier.height(24.dp))

        LsLearningStepHeader(1, "Understand the concept", "Focus on the rule and why it works.")
        Spacer(Modifier.height(12.dp))
        (explanations + supportingBlocks).forEach { block ->
            LsContentBlockCard(block)
            Spacer(Modifier.height(12.dp))
        }
        if (explanations.isEmpty() && supportingBlocks.isEmpty()) {
            LsContentBlockCard(ContentBlock("explanation", "Read the examples carefully and notice the pattern they share."))
            Spacer(Modifier.height(12.dp))
        }

        if (examples.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LsLearningStepHeader(2, "See it in context", "Notice how the concept changes real communication.")
            Spacer(Modifier.height(12.dp))
            examples.forEach { block ->
                LsContentBlockCard(block)
                Spacer(Modifier.height(12.dp))
            }
        }

        if (tips.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LsLearningStepHeader(3, "Make it stick", "Use this shortcut to remember the idea.")
            Spacer(Modifier.height(12.dp))
            tips.forEach { block ->
                LsContentBlockCard(block)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        if (sublesson.exercises.isNotEmpty()) {
            LsReadyForPracticeCard(sublesson.exercises.size, onStartExercises)
        } else {
            LsCompleteButton(
                isVisible = showCompleteButton,
                isEnabled = !completionSent,
                sublesson = sublesson,
                totalExercises = sublesson.exercises.size,
                correctCount = sublesson.exercises.size,
                onClick   = onComplete
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun LsLessonMetric(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(Color.White.copy(alpha = 0.11f), RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LsLearningOutcomes(title: String, hasExamples: Boolean, hasTips: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFF1EFFF))
            .border(1.dp, BrandPurple.copy(alpha = 0.12f), RoundedCornerShape(22.dp)).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(BrandPurple, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Flag, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("YOUR GOAL", color = BrandPurple, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                Text("By the end, you can...", color = Color(0xFF17133B), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(15.dp))
        LsOutcomeRow("Explain ${title.lowercase()} in your own words")
        if (hasExamples) LsOutcomeRow("Recognize the concept in a real sentence")
        if (hasTips) LsOutcomeRow("Use the concept without the common mistake")
        LsOutcomeRow("Apply it independently in practice")
    }
}

@Composable
private fun LsOutcomeRow(text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 1.dp).size(20.dp).background(BrandGreen.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = BrandGreen, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color(0xFF3F3A59), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LsLearningStepHeader(number: Int, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(BrandPurple, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF17133B), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextLight, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun LsReadyForPracticeCard(questionCount: Int, onStartExercises: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp), spotColor = BrandPurple.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(24.dp)).background(CardWhite)
            .border(1.dp, BrandPurple.copy(alpha = 0.14f), RoundedCornerShape(24.dp)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFFFF4D6), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Psychology, null, tint = BrandAmberDark, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PAUSE AND RECALL", color = BrandAmberDark, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                Text("Can you explain the idea?", color = Color(0xFF17133B), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(13.dp))
        Text("Take a moment to say the rule in your own words. If you can, you are ready to use it.", color = TextLight, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(17.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight)))
                .clickable { onStartExercises() }.padding(17.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("I'm ready | $questionCount questions", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Content Block Card ───────────────────────────────────────────────────────
@Composable
fun LsContentBlockCard(block: ContentBlock) {
    val (icon, accent, label, _) = when (block.type) {
        "explanation" -> Quadruple(Icons.Default.Lightbulb, BrandPurple, "KEY IDEA", Color.White)
        "example" -> Quadruple(Icons.Default.FormatQuote, BrandAmberDark, "IN CONTEXT", Color.White)
        "tip" -> Quadruple(Icons.Default.Bolt, BrandGreen, "COACH TIP", Color.White)
        else -> Quadruple(Icons.Default.PushPin, BrandPurpleLight, block.type.uppercase(), Color.White)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(22.dp))
            .background(CardWhite)
            .border(1.dp, accent.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(accent.copy(alpha = 0.11f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(block.text, style = TextStyle(color = Color(0xFF2B2742), fontSize = 15.sp, lineHeight = 23.sp))
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─── Exercise Phase ───────────────────────────────────────────────────────────
@Composable
fun LsExercisePhase(
    exercise: Exercise,
    sublesson: SublessonDetail,
    exerciseNumber: Int,
    totalExercises: Int,
    correctCount: Int,
    answerState: AnswerState,
    feedback: String,
    isSubmitting: Boolean,
    showCompleteButton: Boolean,
    completionSent: Boolean,
    onSubmit: (String) -> Unit,
    onAdvance: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // ── Exercise card ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = BrandPurple.copy(alpha = 0.18f))
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF17133B), BrandPurple)))
                .padding(20.dp)
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD166).copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "EXERCISE $exerciseNumber",
                            color = Color(0xFFFFD166),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            exercise.type.replace("_", " ").uppercase(),
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    exercise.instruction,
                    style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 25.sp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    exercise.stimulus,
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.11f), RoundedCornerShape(14.dp)).padding(13.dp),
                    style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Input ──────────────────────────────────────────────────────────
        val correctAnswer = exercise.correct_answer.orEmpty()
        val options = exercise.options.orEmpty()
        val hasValidMultipleChoice = exercise.type == "multiple_choice" &&
            options.any { it.trim().equals(correctAnswer.trim(), ignoreCase = true) }

        when {
            hasValidMultipleChoice -> LsMultipleChoice(
                options       = options,
                correctAnswer = correctAnswer,
                answerState   = answerState,
                onSelected    = { onSubmit(it) }
            )
            else -> LsFillBlank(
                exerciseId    = exercise.id,
                answerState  = answerState,
                isSubmitting = isSubmitting,
                onSubmit     = { onSubmit(it) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Feedback banner ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = answerState !is AnswerState.Unanswered,
            enter   = slideInVertically { it / 2 } + fadeIn(),
            exit    = ExitTransition.None
        ) {
            LsFeedbackBanner(
                isCorrect = answerState is AnswerState.Correct,
                feedback  = feedback
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Continue button ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = answerState !is AnswerState.Unanswered,
            enter   = fadeIn(tween(300)),
            exit    = ExitTransition.None
        ) {
            val isCorrect = answerState is AnswerState.Correct
            val btnGradient = if (isCorrect)
                Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight))
            else
                Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFEF5350)))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(6.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(btnGradient)
                    .clickable { onAdvance() }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCorrect) "Continue  →" else "Try Next  →",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ── Complete button ────────────────────────────────────────────────
        LsCompleteButton(
            isVisible = showCompleteButton,
            isEnabled = !completionSent,
            sublesson = sublesson,
            totalExercises = totalExercises,
            correctCount = correctCount,
            onClick   = onComplete
        )
    }
}

// ─── Multiple Choice ──────────────────────────────────────────────────────────
@Composable
fun LsMultipleChoice(
    options: List<String>,
    correctAnswer: String,
    answerState: AnswerState,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        options.forEachIndexed { index, option ->
            val selectedAnswer = when (answerState) {
                is AnswerState.Correct   -> answerState.answer
                is AnswerState.Incorrect -> answerState.answer
                else -> null
            }
            val selected = selectedAnswer == option
            val isCorrectOpt = option.trim().lowercase() == correctAnswer.trim().lowercase()
            val revealed = answerState !is AnswerState.Unanswered

            val cardBg = when {
                revealed && isCorrectOpt              -> Color(0xFFEAFBF0)
                revealed && selected && !isCorrectOpt -> Color(0xFFFFECEC)
                selected                              -> BrandPurpleSoft
                else                                  -> Color(0xFFF8F7FF)
            }
            val borderColor = when {
                revealed && isCorrectOpt              -> BrandGreen
                revealed && selected && !isCorrectOpt -> BrandRed
                selected                              -> BrandPurple
                else                                  -> Color(0x1A000000)
            }
            val textColor = when {
                revealed && isCorrectOpt              -> BrandGreen
                revealed && selected && !isCorrectOpt -> BrandRed
                else                                  -> TextDark
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                    .clickable(enabled = answerState is AnswerState.Unanswered) { onSelected(option) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier.size(32.dp).background(
                        if (selected) borderColor.copy(alpha = 0.15f) else Color.White,
                        RoundedCornerShape(10.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(('A'.code + index).toChar().toString(), color = if (selected) borderColor else TextLight, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(12.dp))
                Text(option, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), lineHeight = 20.sp)
                if (revealed) {
                    Icon(
                        imageVector = if (isCorrectOpt) Icons.Default.CheckCircle else if (selected) Icons.Default.Cancel else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCorrectOpt) BrandGreen else if (selected) BrandRed else TextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Fill in the Blank ────────────────────────────────────────────────────────
// FIX: previously the outer Box had no minimum height, so on short / empty
// input the OutlinedTextField could collapse and the absolutely-positioned
// submit button (BottomEnd) would appear to "float" with nothing around it —
// looking like a blank screen with a stray button. We now:
//  1. give the field a guaranteed minHeight so the box never collapses
//  2. reserve real trailing space via trailingIcon instead of manual padding+overlay
//     hacks, so the button is always laid out in-flow with the text field
@Composable
fun LsFillBlank(
    exerciseId: String,
    answerState: AnswerState,
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    placeholder: String = "Type your answer…"
) {
    var text by remember(exerciseId) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val answered = answerState !is AnswerState.Unanswered

    val borderColor = when {
        answerState is AnswerState.Correct   -> BrandGreen
        answerState is AnswerState.Incorrect -> BrandRed
        text.isNotBlank()                    -> BrandPurple
        else                                 -> Color(0x1A000000)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (!answered) text = it },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .bringIntoViewOnFocus(),
            placeholder = { Text(placeholder, color = TextLight, fontSize = 14.sp) },
            minLines = 2,
            maxLines = 4,
            enabled = !answered,
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 6.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank() && !answered) BrandPurple else Color(0x22000000))
                        .clickable(enabled = text.isNotBlank() && !answered) {
                            keyboard?.hide(); onSubmit(text)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, "Submit", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor      = Color.Transparent,
                unfocusedBorderColor    = Color.Transparent,
                disabledBorderColor     = Color.Transparent,
                focusedTextColor        = TextDark,
                unfocusedTextColor      = TextDark,
                disabledTextColor       = TextDark,
                disabledContainerColor  = Color.Transparent
            ),
            textStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = TextDark),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboard?.hide()
                if (text.isNotBlank() && !answered) onSubmit(text)
            })
        )
    }
}

// ─── Feedback Banner ──────────────────────────────────────────────────────────
@Composable
fun LsFeedbackBanner(isCorrect: Boolean, feedback: String) {
    val bg     = if (isCorrect) Color(0xFFEAFBF0) else Color(0xFFFFECEC)
    val border = if (isCorrect) BrandGreen else BrandRed
    val icon   = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .padding(17.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(36.dp).background(border.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = border, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (isCorrect) "Correct!" else "Not quite...", color = border, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(feedback, color = TextDark, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────
@Composable
fun LsLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandPurple, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("Loading lesson…", color = TextMid, fontSize = 14.sp)
        }
    }
}

// ─── Error ────────────────────────────────────────────────────────────────────
@Composable
fun LsErrorView(onBack: () -> Unit, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SentimentDissatisfied, contentDescription = null, tint = TextMid, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Couldn't load this lesson. Check your connection.",
                color = TextMid,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleSoft)
                ) { Text("Go Back", color = BrandPurple) }
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                ) { Text("Retry", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─── Complete Button ──────────────────────────────────────────────────────────
@Composable
fun LsCompleteButton(
    isVisible: Boolean,
    isEnabled: Boolean,
    sublesson: SublessonDetail,
    totalExercises: Int,
    correctCount: Int,
    onClick: () -> Unit
) {
    if (!isVisible) return

    val accuracy = if (totalExercises == 0) 100 else (correctCount * 100 / totalExercises).coerceIn(0, 100)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardWhite)
            .border(1.dp, Color(0xFFE4E1EF), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).background(Color(0xFFFFF5DD), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.trophy),
                    contentDescription = "Trophy",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("READY TO COMPLETE", color = BrandGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                Text(sublesson.title, color = Color(0xFF17133B), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Color(0xFFECE9F3))
        Spacer(Modifier.height(16.dp))
        Text("Performance summary", color = Color(0xFF17133B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LsSummaryPill("ACCURACY", "$accuracy%", BrandPurple, Modifier.weight(1f))
            LsSummaryPill("CORRECT", "$correctCount/$totalExercises", BrandGreen, Modifier.weight(1f))
            LsSummaryPill("REWARD", "+20 XP", BrandAmberDark, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple, disabledContainerColor = Color(0xFFB8B3C9))
        ) {
            if (!isEnabled) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
            }
            Text(if (isEnabled) "Complete lesson" else "Saving result...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LsSummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ─── Confetti Celebration ──────────────────────────────────────────────────────
// Lightweight canvas-based confetti burst, no extra deps. Each particle falls
// and rotates with a randomized horizontal drift; the whole burst plays once
// when the completion screen appears.
private data class ConfettiParticle(
    val startX: Float,
    val color: Color,
    val size: Float,
    val fallDelay: Int,
    val drift: Float,
    val rotationSpeed: Float
)

@Composable
fun LsConfettiOverlay(modifier: Modifier = Modifier) {
    val confettiColors = listOf(BrandPurple, BrandPurpleLight, BrandAmber, BrandGreen, Color(0xFFE53935), Color(0xFF42A5F5))

    val particles = remember {
        List(60) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                color = confettiColors[Random.nextInt(confettiColors.size)],
                size = Random.nextFloat() * 6f + 5f,
                fallDelay = Random.nextInt(400),
                drift = (Random.nextFloat() - 0.5f) * 2f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(durationMillis = 2600, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        particles.forEach { p ->
            val localProgress = ((anim.value * 2600f - p.fallDelay) / 2200f).coerceIn(0f, 1f)
            if (localProgress <= 0f) return@forEach
            val y = -40f + localProgress * (h + 80f)
            val x = (p.startX * w) + (p.drift * 60f * localProgress)
            val alpha = if (localProgress > 0.8f) (1f - localProgress) / 0.2f else 1f

            rotate(degrees = p.rotationSpeed * localProgress, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                    topLeft = Offset(x - p.size / 2, y - p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }
        }
    }
}

// ─── Completion Screen ────────────────────────────────────────────────────────
private data class FireworkBurst(
    val centerX: Float,
    val centerY: Float,
    val delay: Int,
    val color: Color,
    val radius: Float
)

@Composable
fun LsFireworksOverlay(modifier: Modifier = Modifier) {
    val fireworkColors = listOf(BrandPurple, BrandPurpleLight, BrandAmber, BrandGreen, Color(0xFF42A5F5))
    val bursts = remember {
        List(7) {
            FireworkBurst(
                centerX = Random.nextFloat() * 0.76f + 0.12f,
                centerY = Random.nextFloat() * 0.30f + 0.08f,
                delay = Random.nextInt(700),
                color = fireworkColors[Random.nextInt(fireworkColors.size)],
                radius = Random.nextFloat() * 44f + 54f
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(durationMillis = 1600, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val duration = 1600f
        bursts.forEach { burst ->
            val localProgress = ((anim.value * duration - burst.delay) / 900f).coerceIn(0f, 1f)
            if (localProgress <= 0f || localProgress >= 1f) return@forEach

            val center = Offset(burst.centerX * size.width, burst.centerY * size.height)
            val alpha = (1f - localProgress).coerceIn(0f, 1f)
            val sparkRadius = burst.radius * localProgress

            repeat(14) { index ->
                val angle = (Math.PI * 2.0 * index / 14.0).toFloat()
                val end = Offset(
                    center.x + kotlin.math.cos(angle) * sparkRadius,
                    center.y + kotlin.math.sin(angle) * sparkRadius
                )
                val start = Offset(
                    center.x + kotlin.math.cos(angle) * sparkRadius * 0.62f,
                    center.y + kotlin.math.sin(angle) * sparkRadius * 0.62f
                )
                drawLine(
                    color = burst.color.copy(alpha = alpha),
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = burst.color.copy(alpha = alpha),
                    radius = 2.5.dp.toPx(),
                    center = end
                )
            }
        }
    }
}

@Composable
fun LsCompletionView(
    totalXp: Int,
    nextSublessonId: String?,
    onContinueNext: (String) -> Unit,
    onBack: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = 1.10f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    // Entrance pop for the trophy + text content
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti burst plays once on entry, behind/around the content
        LsConfettiOverlay(modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.85f, animationSpec = tween(450, easing = FastOutSlowInEasing))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size((150 * pulse).dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(BrandPurpleSoft, Color(0x00FFFFFF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.trophy),
                            contentDescription = "Trophy",
                            modifier = Modifier.size((122 * pulse).dp)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        "Lesson Complete!",
                        style = TextStyle(color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Great work! You're making real progress.", color = TextMid, fontSize = 14.sp, textAlign = TextAlign.Center)

                    Spacer(Modifier.height(28.dp))

                    Box(
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight)))
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("+$totalXp XP Earned", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    if (nextSublessonId != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight)))
                                .clickable { onContinueNext(nextSublessonId) }
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Next Lesson  →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardWhite)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(18.dp))
                            .clickable { onBack() }
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Back to Home", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LsProfessionalCompletionView(
    totalXp: Int,
    totalExercises: Int,
    correctCount: Int,
    nextSublessonId: String?,
    onContinueNext: (String) -> Unit,
    onBack: () -> Unit
) {
    val accuracy = if (totalExercises == 0) 100 else (correctCount * 100 / totalExercises).coerceIn(0, 100)
    val resultLabel = when {
        accuracy >= 90 -> "Excellent understanding"
        accuracy >= 70 -> "Good understanding"
        else -> "Lesson completed"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LsFireworksOverlay(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Box(Modifier.size(82.dp).background(Color(0xFFFFF5DD), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.trophy),
                contentDescription = "Lesson completed trophy",
                modifier = Modifier.size(62.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Lesson completed", color = Color(0xFF17133B), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(resultLabel, color = TextLight, fontSize = 14.sp, textAlign = TextAlign.Center)

        Spacer(Modifier.height(26.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardWhite)
                .border(1.dp, Color(0xFFE4E1EF), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Text("Result", color = Color(0xFF17133B), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Your performance for this lesson", color = TextLight, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LsSummaryPill("ACCURACY", "$accuracy%", BrandPurple, Modifier.weight(1f))
                LsSummaryPill("CORRECT", "$correctCount/$totalExercises", BrandGreen, Modifier.weight(1f))
                LsSummaryPill("EARNED", "+$totalXp XP", BrandAmberDark, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { accuracy / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = if (accuracy >= 70) BrandGreen else BrandAmber,
                trackColor = Color(0xFFECE9F3),
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(Modifier.height(24.dp))
        if (nextSublessonId != null) {
            Button(
                onClick = { onContinueNext(nextSublessonId) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("Continue to next lesson", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCD8E8)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3F3A59))
        ) {
            Text("Back to learning path", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        }
    }
}
