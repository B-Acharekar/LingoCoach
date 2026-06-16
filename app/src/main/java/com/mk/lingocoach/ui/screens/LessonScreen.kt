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
import com.mk.lingocoach.R
import com.mk.lingocoach.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val scope       = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val userId      = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    var activeSublessonId by remember { mutableStateOf(sublessonId) }
    var sublesson       by remember { mutableStateOf<SublessonDetail?>(null) }
    var learningPath    by remember { mutableStateOf<CurrentLearningPathResponse?>(null) }
    var isLoading       by remember { mutableStateOf(true) }
    var isError         by remember { mutableStateOf(false) }
    var isContentPhase  by remember { mutableStateOf(true) }
    var exerciseIndex   by remember { mutableStateOf(0) }
    var showCompletion  by remember { mutableStateOf(false) }
    var totalXpEarned   by remember { mutableStateOf(0) }

    fun loadSublesson(targetId: String) {
        isLoading = true; isError = false
        AssessmentApi.getSublesson(targetId) { detail ->
            scope.launch(Dispatchers.Main) {
                sublesson = detail
                if (detail != null) {
                    AssessmentApi.getCurrentLearningPath(userId) { path ->
                        scope.launch(Dispatchers.Main) {
                            learningPath = path
                            isLoading = false
                        }
                    }
                } else {
                    isLoading = false
                    isError = true
                }
            }
        }
    }

    LaunchedEffect(activeSublessonId) {
        isContentPhase = true
        exerciseIndex = 0
        showCompletion = false
        loadSublesson(activeSublessonId)
    }

    val nextSublessonId = remember(learningPath, activeSublessonId) {
        val path = learningPath ?: return@remember null
        var foundCurrent = false
        var nextId: String? = null
        for (module in path.modules) {
            for (lesson in module.lessons) {
                for (sub in lesson.sublessons) {
                    if (foundCurrent) {
                        nextId = sub.id
                        break
                    }
                    if (sub.id == activeSublessonId) {
                        foundCurrent = true
                    }
                }
                if (nextId != null) break
            }
            if (nextId != null) break
        }
        nextId
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Same background as HomeScreen ────────────────────────────────────
        Image(
            painter      = painterResource(R.drawable.background),
            contentDescription = null,
            modifier     = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when {
            isLoading -> LsLoadingView()
            isError   -> LsErrorView(onBack = onNavigateBack, onRetry = { loadSublesson(activeSublessonId) })
            showCompletion -> LsCompletionView(
                totalXp = totalXpEarned,
                nextSublessonId = nextSublessonId,
                onContinueNext = { nextId ->
                    activeSublessonId = nextId
                },
                onBack  = onNavigateBack
            )
            else -> {
                val sub = sublesson!!
                val currentModule = learningPath?.modules?.firstOrNull { module ->
                    module.lessons.any { lesson ->
                        lesson.sublessons.any { s -> s.id == activeSublessonId }
                    }
                }
                val currentLesson = currentModule?.lessons?.firstOrNull { lesson ->
                    lesson.sublessons.any { s -> s.id == activeSublessonId }
                }
                val currentSublessonsList = currentLesson?.sublessons ?: emptyList()

                var dropdownExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Top bar
                    LsTopBar(
                        title          = sub.title,
                        exerciseCount  = sub.exercises.size,
                        currentIndex   = if (isContentPhase) 0 else exerciseIndex + 1,
                        isContentPhase = isContentPhase,
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

                    if (isContentPhase) {
                        LsContentPhase(sublesson = sub, onStartExercises = {
                            if (sub.exercises.isNotEmpty()) {
                                isContentPhase = false
                            } else {
                                val idx = currentSublessonsList.indexOfFirst { it.id == activeSublessonId }
                                if (idx != -1 && idx < currentSublessonsList.size - 1) {
                                    activeSublessonId = currentSublessonsList[idx + 1].id
                                } else {
                                    AppCache.invalidateLearningPath()
                                    showCompletion = true
                                }
                            }
                        })
                    } else {
                        LsExercisePhase(
                            sublesson   = sub,
                            index       = exerciseIndex,
                            userId      = userId,
                            onCompleted = { xp ->
                                totalXpEarned += xp
                                if (exerciseIndex < sub.exercises.size - 1) {
                                    exerciseIndex++
                                } else {
                                    val idx = currentSublessonsList.indexOfFirst { it.id == activeSublessonId }
                                    if (idx != -1 && idx < currentSublessonsList.size - 1) {
                                        activeSublessonId = currentSublessonsList[idx + 1].id
                                    } else {
                                        AppCache.invalidateLearningPath()
                                        showCompletion = true
                                    }
                                }
                            }
                        )
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
            // Back
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

            // Title + subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = TextStyle(
                        color = TextDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isContentPhase) "Learn" else "Exercise $currentIndex of $exerciseCount",
                    color = TextLight,
                    fontSize = 11.sp
                )
            }

            // XP chip
            Box(
                modifier = Modifier
                    .background(BrandPurpleSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "+20 XP",
                        color = BrandPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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
    }
}

// ─── Content Phase ────────────────────────────────────────────────────────────
@Composable
fun LsContentPhase(sublesson: SublessonDetail, onStartExercises: () -> Unit) {
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

        // CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight))
                )
                .clickable { onStartExercises() }
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (sublesson.exercises.isNotEmpty())
                        "Start Exercises  (${sublesson.exercises.size})"
                    else "Mark Complete",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (sublesson.exercises.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
                    Text(
                        label,
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                block.text,
                style = TextStyle(
                    color = TextDark,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─── Exercise Phase ───────────────────────────────────────────────────────────
@Composable
fun LsExercisePhase(
    sublesson: SublessonDetail,
    index: Int,
    userId: String,
    onCompleted: (xpEarned: Int) -> Unit
) {
    val scope    = rememberCoroutineScope()
    val exercise = sublesson.exercises[index]

    var answerState  by remember(index) { mutableStateOf<LsAnswerState>(LsAnswerState.Unanswered) }
    var feedback     by remember(index) { mutableStateOf("") }
    var isSubmitting by remember(index) { mutableStateOf(false) }

    fun submit(answer: String) {
        if (answerState != LsAnswerState.Unanswered) return
        isSubmitting = true
        val req = CompleteExerciseRequest(
            user_id      = userId,
            sublesson_id = sublesson.id,
            exercise_id  = exercise.id,
            user_answer  = answer
        )
        AssessmentApi.completeExercise(req) { response ->
            scope.launch(Dispatchers.Main) {
                isSubmitting = false
                if (response != null) {
                    answerState = if (response.is_correct) LsAnswerState.Correct(answer) else LsAnswerState.Incorrect(answer)
                    feedback    = response.feedback
                } else {
                    val correct = exercise.correct_answer?.trim()?.lowercase()
                    val ok = correct != null && answer.trim().lowercase() == correct
                    answerState = if (ok) LsAnswerState.Correct(answer) else LsAnswerState.Incorrect(answer)
                    feedback    = if (ok) "Correct! Great job." else "Incorrect. Correct answer: ${exercise.correct_answer}"
                }
            }
        }
    }

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
                            "EXERCISE ${index + 1}",
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
                    style = TextStyle(
                        color = TextDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"${exercise.stimulus}\"",
                    style = TextStyle(
                        color = BrandPurple,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                onSelected    = { submit(it) }
            )
            else -> LsFillBlank(
                answerState  = answerState,
                isSubmitting = isSubmitting,
                onSubmit     = { submit(it) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Feedback banner ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = answerState != LsAnswerState.Unanswered,
            enter   = slideInVertically { it / 2 } + fadeIn(),
            exit    = fadeOut()
        ) {
            LsFeedbackBanner(
                isCorrect = answerState is LsAnswerState.Correct,
                feedback  = feedback
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Continue button ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = answerState != LsAnswerState.Unanswered,
            enter   = fadeIn(tween(300)),
            exit    = fadeOut()
        ) {
            val xp = if (answerState is LsAnswerState.Correct) 10 else 0
            val btnGradient = if (answerState is LsAnswerState.Correct)
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
                    .clickable { onCompleted(xp) }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (answerState is LsAnswerState.Correct) "Continue  →" else "Try Next  →",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

sealed class LsAnswerState {
    object Unanswered : LsAnswerState()
    data class Correct(val answer: String) : LsAnswerState()
    data class Incorrect(val answer: String) : LsAnswerState()
}

// ─── Multiple Choice ──────────────────────────────────────────────────────────
@Composable
fun LsMultipleChoice(
    options: List<String>,
    correctAnswer: String,
    answerState: LsAnswerState,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            val selected   = when (answerState) {
                is LsAnswerState.Correct   -> answerState.answer == option
                is LsAnswerState.Incorrect -> answerState.answer == option
                else -> false
            }
            val isCorrectOpt = option.trim().lowercase() == correctAnswer.trim().lowercase()
            val revealed = answerState != LsAnswerState.Unanswered

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
                    .clickable(enabled = answerState == LsAnswerState.Unanswered) { onSelected(option) }
                    .padding(16.dp),
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
    answerState: LsAnswerState,
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    placeholder: String = "Type your answer…"
) {
    var text by remember { mutableStateOf("") }
    val keyboard  = LocalSoftwareKeyboardController.current
    val answered  = answerState != LsAnswerState.Unanswered

    val borderColor = when {
        answerState is LsAnswerState.Correct   -> BrandGreen
        answerState is LsAnswerState.Incorrect -> BrandRed
        text.isNotBlank()                      -> BrandPurple
        else                                   -> Color(0x1A000000)
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
                Text(
                    if (isCorrect) "Correct!" else "Not quite...",
                    color = border,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
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
    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Pulsing trophy
            Box(
                modifier = Modifier
                    .size((110 * pulse).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(BrandPurpleSoft, Color(0x00FFFFFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrandAmber, modifier = Modifier.size((58 * pulse).dp))
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Lesson Complete!",
                style = TextStyle(
                    color = TextDark,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Great work! You're making real progress.",
                color = TextMid,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // XP chip
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight))
                    )
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "+$totalXp XP Earned",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (nextSublessonId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight))
                        )
                        .clickable { onContinueNext(nextSublessonId) }
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Next Lesson  →",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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
                Text(
                    "Back to Home",
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
