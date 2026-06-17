package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.mk.lingocoach.ui.viewmodel.*

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
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when {
            uiState.phase == Phase.LOADING -> LsLoadingView()

            uiState.phase == Phase.ERROR -> LsErrorView(
                onBack  = onNavigateBack,
                onRetry = { lessonViewModel.loadSublesson(activeSublessonId, userId) }
            )

            uiState.phase == Phase.COMPLETE -> LsCompletionView(
                totalXp         = uiState.totalXpEarned,
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
                        title          = sub.title,
                        exerciseCount  = uiState.originalExercises.size,
                        currentIndex   = if (uiState.phase == Phase.CONTENT) 0 else uiState.correctOriginalCount,
                        isContentPhase = uiState.phase == Phase.CONTENT,
                        retryCount     = uiState.retryQueue.size,
                        onBack         = onNavigateBack
                    )

                    // Dropdown Selector above question / content
                    if (currentSublessonsList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardWhite)
                                    .border(1.dp, Color(0x1A000000), RoundedCornerShape(12.dp))
                                    .clickable { dropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Lesson Parts: Part ${sub.order} - ${sub.title}",
                                        color = TextDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
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
                        val activeExercise = lessonViewModel.activeExercise(uiState)
                        if (activeExercise != null) {
                            LsExercisePhase(
                                exercise           = activeExercise,
                                exerciseNumber     = uiState.currentOriginalIndex + 1,
                                answerState        = uiState.answerState,
                                feedback           = uiState.feedback,
                                isSubmitting       = uiState.isSubmitting,
                                showCompleteButton = uiState.retryQueue.isEmpty() && uiState.correctOriginalCount == uiState.originalExercises.size && uiState.originalExercises.isNotEmpty(),
                                completionSent     = uiState.completionSent,
                                onSubmit           = { answer -> lessonViewModel.submitAnswer(answer, userId) },
                                onAdvance          = { lessonViewModel.advance() },
                                onComplete         = { lessonViewModel.completeSublesson(userId, activeSublessonId) }
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
            .background(Color(0xFFFAFAFF))
            .padding(horizontal = 20.dp, vertical = 14.dp)
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = TextStyle(color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold),
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
                    .background(BrandPurpleSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("+20 XP", color = BrandPurple, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
            trackColor = BrandPurpleSoft,
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
                Icon(Icons.Default.Replay, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retrying $retryCount", color = BrandAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        sublesson.content_blocks.forEach { block ->
            LsContentBlockCard(block = block)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        if (sublesson.exercises.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight)))
                    .clickable { onStartExercises() }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Start Exercises  (${sublesson.exercises.size})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            LsCompleteButton(
                isVisible = showCompleteButton,
                isEnabled = !completionSent,
                onClick   = onComplete
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ─── Content Block Card ───────────────────────────────────────────────────────
@Composable
fun LsContentBlockCard(block: ContentBlock) {
    val (icon, accent, label, cardBg) = when (block.type) {
        "explanation" -> Quadruple(Icons.Default.Lightbulb,        BrandPurple,      "EXPLANATION", Color(0xFFF5F3FF))
        "example"     -> Quadruple(Icons.Default.Edit,             BrandAmber,       "EXAMPLE",     Color(0xFFFFFBF0))
        "tip"         -> Quadruple(Icons.Default.TipsAndUpdates,   BrandGreen,       "TIP",         Color(0xFFF0FBF4))
        else          -> Quadruple(Icons.Default.PushPin,          BrandPurpleLight, block.type.uppercase(), Color(0xFFF8F8FF))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(block.text, style = TextStyle(color = TextDark, fontSize = 14.sp, lineHeight = 21.sp))
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─── Exercise Phase ───────────────────────────────────────────────────────────
@Composable
fun LsExercisePhase(
    exercise: Exercise,
    exerciseNumber: Int,
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
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // ── Exercise card ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(5.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(CardWhite)
                .padding(20.dp)
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(7.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "EXERCISE $exerciseNumber",
                            color = BrandAmberDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(BrandPurpleSoft, RoundedCornerShape(7.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            exercise.type.replace("_", " ").uppercase(),
                            color = BrandPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    exercise.instruction,
                    style = TextStyle(color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"${exercise.stimulus}\"",
                    style = TextStyle(color = BrandPurple, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Input ──────────────────────────────────────────────────────────
        when (exercise.type) {
            "multiple_choice" -> LsMultipleChoice(
                options       = exercise.options ?: emptyList(),
                correctAnswer = exercise.correct_answer ?: "",
                answerState   = answerState,
                onSelected    = { onSubmit(it) }
            )
            else -> LsFillBlank(
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
            exit    = fadeOut()
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
            exit    = fadeOut()
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
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
                else                                  -> CardWhite
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
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = answerState is AnswerState.Unanswered) { onSelected(option) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(option, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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
@Composable
fun LsFillBlank(
    answerState: AnswerState,
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    placeholder: String = "Type your answer…"
) {
    var text by remember { mutableStateOf("") }
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
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (!answered) text = it },
            modifier = Modifier.fillMaxWidth().padding(end = 56.dp),
            placeholder = { Text(placeholder, color = TextLight, fontSize = 14.sp) },
            minLines = 2,
            maxLines = 4,
            enabled = !answered,
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

        Box(
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 6.dp)
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
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = border, modifier = Modifier.size(22.dp))
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
fun LsCompleteButton(isVisible: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter   = fadeIn(tween(400)) + expandVertically(),
        exit    = fadeOut() + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isEnabled)
                        Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF43A047)))
                    else
                        Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFFCFD8DC)))
                )
                .clickable(enabled = isEnabled) { onClick() }
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Complete Lesson ✓",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─── Completion Screen ────────────────────────────────────────────────────────
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size((110 * pulse).dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(BrandPurpleSoft, Color(0x00FFFFFF)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrandAmber, modifier = Modifier.size((58 * pulse).dp))
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
