package com.mk.lingocoach.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─── Duel Enums & Data ───────────────────────────────────────────────────────

enum class DuelDifficulty(
    val label: String,
    val subtitle: String,
    val timePerQuestion: Int, // seconds
    val xpGain: Int,
    val xpLoss: Int,
    val level: String
) {
    BEGINNER("Beginner", "A1 · A2 words  ·  30s per Q", 30, 5, 2, "A1"),
    INTERMEDIATE("Intermediate", "B1 · B2 words  ·  20s per Q", 20, 10, 5, "B1"),
    ADVANCED("Advanced", "C1 words  ·  15s per Q", 15, 20, 10, "C1"),
    MASTER("Master", "C1 · C2 words  ·  10s per Q", 10, 50, 25, "C1")
}

enum class DuelQuestionType { FILL_BLANK, SPELLING, PRONUNCIATION, SENTENCE }

enum class DuelScreen { SETUP, PLAYING, RESULT }

data class DuelQuestion(
    val vocabWord: VocabWord,
    val type: DuelQuestionType,
    val fillBlankText: String = ""
)

// ─── Main Timely Duel Screen ─────────────────────────────────────────────────

@Composable
fun TimelyDuelScreen(onNavigateBack: () -> Unit, onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember {
        context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
            .getString("session_id", "") ?: ""
    }

    var screen by remember { mutableStateOf(DuelScreen.SETUP) }
    var selectedDifficulty by remember { mutableStateOf(DuelDifficulty.INTERMEDIATE) }

    // Session state
    var questions by remember { mutableStateOf<List<DuelQuestion>>(emptyList()) }
    var currentIdx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var xpDelta by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val t = TextToSpeech(context) {}
        t.language = Locale.US
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    // Load vocab if needed
    LaunchedEffect(Unit) {
        if (!VocabTracker.isLoaded) VocabTracker.init(context)
    }

    BackHandler { onNavigateBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()
        Column(Modifier.fillMaxSize()) {
            CommonTopBar(
                title = stringResource(R.string.timely_duel),
                onBack = onNavigateBack,
                onSettings = onNavigateToSettings
            )
            Box(Modifier.weight(1f)) {
        when (screen) {
            DuelScreen.SETUP -> DuelSetupScreen(
                selectedDifficulty = selectedDifficulty,
                onDifficultySelected = { selectedDifficulty = it },
                onStart = {
                    // Build question list – 8 questions, cycling through types
                    val words = VocabTracker.generateDrillSession(selectedDifficulty.level, null, 8)
                    if (words.isNotEmpty()) {
                        val types = DuelQuestionType.values()
                        questions = words.mapIndexed { i, q ->
                            val type = types[i % types.size]
                            val blank = if (type == DuelQuestionType.FILL_BLANK) {
                                q.word.examples.firstOrNull()?.english?.replace(
                                    q.word.word, "_____", ignoreCase = true
                                ) ?: "The word that means \"${q.word.meaning}\" is _____."
                            } else ""
                            DuelQuestion(vocabWord = q.word, type = type, fillBlankText = blank)
                        }
                        currentIdx = 0; score = 0; xpDelta = 0
                        correctCount = 0; wrongCount = 0
                        screen = DuelScreen.PLAYING
                    }
                },
                onBack = onNavigateBack
            )

            DuelScreen.PLAYING -> {
                if (currentIdx < questions.size) {
                    DuelGameScreen(
                        question = questions[currentIdx],
                        questionNum = currentIdx + 1,
                        totalQuestions = questions.size,
                        difficulty = selectedDifficulty,
                        tts = tts,
                        context = context,
                        onCorrect = {
                            correctCount++
                            xpDelta += selectedDifficulty.xpGain
                            score += selectedDifficulty.xpGain
                            if (currentIdx + 1 >= questions.size) screen = DuelScreen.RESULT
                            else currentIdx++
                        },
                        onWrong = { word ->
                            wrongCount++
                            xpDelta -= selectedDifficulty.xpLoss
                            score -= selectedDifficulty.xpLoss
                            // Log locally
                            VocabTracker.addLocalMistake(
                                word = word.word,
                                mistakeType = "TIMELY_DUEL",
                                userAnswer = "(time/wrong)",
                                correctAnswer = word.word,
                                explanation = word.meaning,
                                context = context
                            )
                            // Log to backend (fire-and-forget)
                            if (userId.isNotBlank()) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    com.mk.lingocoach.network.AssessmentApi.logMistake(
                                        userId          = userId,
                                        word            = word.word,
                                        mistakeType     = "TIMELY_DUEL",
                                        userSentence    = "(missed in timed duel)",
                                        correctSentence = word.word,
                                        explanation     = "Meaning: ${word.meaning}",
                                        source          = "timely_duel"
                                    )
                                }
                            }
                            if (currentIdx + 1 >= questions.size) screen = DuelScreen.RESULT
                            else currentIdx++
                        },
                        onBack = onNavigateBack
                    )
                } else {
                    screen = DuelScreen.RESULT
                }
            }

            DuelScreen.RESULT -> DuelResultScreen(
                correctCount = correctCount,
                wrongCount = wrongCount,
                xpDelta = xpDelta,
                difficulty = selectedDifficulty,
                onPlayAgain = {
                    screen = DuelScreen.SETUP
                },
                onHome = onNavigateBack
            )
        }
            }
        }
    }
}

// ─── Setup Screen ─────────────────────────────────────────────────────────────

@Composable
fun DuelSetupScreen(
    selectedDifficulty: DuelDifficulty,
    onDifficultySelected: (DuelDifficulty) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val amber = Color(0xFFFFCA28)
    val amberDeep = Color(0xFFFF8F00)
    val dark = Color(0xFF1A1040)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(22.dp))

        // Hero card with crossed swords SVG centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .shadow(10.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(amber, amberDeep)))
        ) {
            // Real crossed-swords icon — large, centered, faded dark overlay
            Icon(
                painter = painterResource(R.drawable.ic_crossed_swords),
                contentDescription = null,
                tint = Color(0xFF3E2000).copy(alpha = 0.14f),
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.Center)
            )

            // Text content on top
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = dark, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.battle_against_time), color = dark, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.answer_before_clock), color = dark.copy(0.65f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.choose_difficulty), color = dark, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        DuelDifficulty.values().forEach { diff ->
            val isSelected = diff == selectedDifficulty
            val cardBg = if (isSelected) Brush.linearGradient(listOf(Color(0xFF6A5CFF), Color(0xFF8A79FF)))
                        else Brush.linearGradient(listOf(Color.White, Color.White))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .clickable { onDifficultySelected(diff) }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(0.2f) else Color(0xFFF0EEFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (diff) {
                                DuelDifficulty.BEGINNER     -> Icons.Default.Eco
                                DuelDifficulty.INTERMEDIATE -> Icons.Default.Bolt
                                DuelDifficulty.ADVANCED     -> Icons.Default.Whatshot
                                DuelDifficulty.MASTER       -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else BrandPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(diff.label, color = if (isSelected) Color.White else dark,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(diff.subtitle, color = if (isSelected) Color.White.copy(0.75f) else Color(0xFF9B96B0),
                            fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("+${diff.xpGain} XP", color = if (isSelected) Color(0xFFFFD54F) else BrandGreen,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("-${diff.xpLoss} XP", color = if (isSelected) Color.White.copy(0.6f) else BrandRed,
                            fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF)),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(6.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_duel), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Game Screen (per question) ───────────────────────────────────────────────

@Composable
fun DuelGameScreen(
    question: DuelQuestion,
    questionNum: Int,
    totalQuestions: Int,
    difficulty: DuelDifficulty,
    tts: TextToSpeech?,
    context: Context,
    onCorrect: () -> Unit,
    onWrong: (VocabWord) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val timeLimit = difficulty.timePerQuestion
    var timeLeft by remember(questionNum) { mutableStateOf(timeLimit) }
    var answered by remember(questionNum) { mutableStateOf(false) }
    var userInput by remember(questionNum) { mutableStateOf("") }
    var selectedOpt by remember(questionNum) { mutableStateOf<Int?>(null) }
    var showResult by remember(questionNum) { mutableStateOf<Boolean?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Generate MC options for FILL_BLANK
    val mcOptions = remember(questionNum) {
        if (question.type == DuelQuestionType.FILL_BLANK) {
            VocabTracker.generateDrillSession(difficulty.level, null, 5)
                .take(3).map { it.word.word }
                .toMutableList().also {
                    it.add(question.vocabWord.word)
                    it.shuffle()
                }
        } else emptyList()
    }

    // Countdown timer
    LaunchedEffect(questionNum) {
        // TTS auto-speak for spelling/pronunciation
        if (question.type == DuelQuestionType.SPELLING || question.type == DuelQuestionType.PRONUNCIATION) {
            delay(400)
            tts?.setSpeechRate(1.0f)
            tts?.speak(question.vocabWord.word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        while (timeLeft > 0 && !answered) {
            delay(1000)
            timeLeft--
        }
        if (!answered) {
            answered = true
            showResult = false
            delay(1200)
            onWrong(question.vocabWord)
        }
    }

    val timerFraction = timeLeft / timeLimit.toFloat()
    val timerColor = when {
        timerFraction > 0.5f -> BrandGreen
        timerFraction > 0.25f -> BrandAmber
        else -> BrandRed
    }

    fun submitAnswer(isCorrect: Boolean) {
        if (answered) return
        answered = true
        showResult = isCorrect
        keyboardController?.hide()
        scope.launch {
            delay(1000)
            if (isCorrect) onCorrect() else onWrong(question.vocabWord)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.question_fraction, questionNum, totalQuestions), color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { questionNum / totalQuestions.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = BrandPurple,
                    trackColor = Color(0xFFDDDAFF)
                )
            }
            Spacer(Modifier.width(12.dp))
            // Timer circle
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFFEEEEEE), style = androidx.compose.ui.graphics.drawscope.Stroke(8f))
                    drawArc(
                        color = timerColor,
                        startAngle = -90f,
                        sweepAngle = 360f * timerFraction,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(8f, cap = StrokeCap.Round)
                    )
                }
                Text("$timeLeft", color = timerColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Question type badge
        val (typeBadge, typeColor) = when (question.type) {
            DuelQuestionType.FILL_BLANK    -> "FILL IN THE BLANK" to Color(0xFF6A5CFF)
            DuelQuestionType.SPELLING      -> "SPELL IT" to Color(0xFF00897B)
            DuelQuestionType.PRONUNCIATION -> "PRONUNCIATION" to Color(0xFFFF6D00)
            DuelQuestionType.SENTENCE      -> "FRAME A SENTENCE" to Color(0xFFD81B60)
        }
        Box(
            modifier = Modifier
                .background(typeColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(typeBadge, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(16.dp))

        // Word / Question card
        Card(
            modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                when (question.type) {
                    DuelQuestionType.SPELLING -> {
                        Text(stringResource(R.string.listen_type_word), color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("/${question.vocabWord.pronunciation}/", color = TextLight,
                            fontSize = 13.sp, fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(6.dp))
                        Text(question.vocabWord.meaning, color = TextMid, fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        // Centered speaker + S M F
                        CardPronunciation(
                            word = question.vocabWord.word,
                            tts = tts
                        )
                    }
                    DuelQuestionType.PRONUNCIATION -> {
                        Text(stringResource(R.string.speak_word_aloud), color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(question.vocabWord.word, color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("(${question.vocabWord.partOfSpeech}) /${question.vocabWord.pronunciation}/",
                            color = TextLight, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(6.dp))
                        Text(question.vocabWord.meaning, color = TextMid, fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        // Centered speaker + S M F
                        CardPronunciation(
                            word = question.vocabWord.word,
                            tts = tts
                        )
                    }
                    DuelQuestionType.FILL_BLANK -> {
                        Text(stringResource(R.string.complete_sentence), color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text(question.fillBlankText, color = TextDark, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, lineHeight = 22.sp)
                    }
                    DuelQuestionType.SENTENCE -> {
                        Text(stringResource(R.string.frame_sentence_using), color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text(question.vocabWord.word, color = TextDark, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${question.vocabWord.partOfSpeech} · ${question.vocabWord.meaning}",
                            color = TextMid, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Answer input area
        when (question.type) {
            DuelQuestionType.FILL_BLANK -> {
                // Multiple choice buttons
                mcOptions.forEachIndexed { idx, opt ->
                    val isSelected = selectedOpt == idx
                    val bgColor = when {
                        showResult == null && isSelected -> Color(0xFFF0EEFF)
                        showResult == true && opt == question.vocabWord.word -> Color(0xFFE8F5E9)
                        showResult == false && isSelected -> Color(0xFFFFEBEE)
                        else -> CardWhite
                    }
                    val borderColor = when {
                        showResult == null && isSelected -> BrandPurple
                        showResult == true && opt == question.vocabWord.word -> BrandGreen
                        showResult == false && isSelected -> BrandRed
                        else -> CardBorderColor
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable(enabled = !answered) {
                                selectedOpt = idx
                                submitAnswer(opt == question.vocabWord.word)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(opt, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            DuelQuestionType.SPELLING, DuelQuestionType.SENTENCE -> {
                val placeholder = if (question.type == DuelQuestionType.SPELLING)
                    stringResource(R.string.type_word_placeholder) else stringResource(R.string.write_sentence_placeholder)
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { if (!answered) userInput = it },
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    placeholder = { Text(placeholder, color = TextLight, fontSize = 13.sp) },
                    singleLine = question.type == DuelQuestionType.SPELLING,
                    maxLines = if (question.type == DuelQuestionType.SENTENCE) 4 else 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (question.type == DuelQuestionType.SPELLING) {
                            submitAnswer(userInput.trim().lowercase() == question.vocabWord.word.lowercase())
                        } else {
                            // Sentence: correct if it contains the word
                            submitAnswer(userInput.lowercase().contains(question.vocabWord.word.lowercase()))
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        disabledTextColor = Color.Black,
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = CardBorderColor
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (question.type == DuelQuestionType.SPELLING) {
                            submitAnswer(userInput.trim().lowercase() == question.vocabWord.word.lowercase())
                        } else {
                            submitAnswer(userInput.lowercase().contains(question.vocabWord.word.lowercase()))
                        }
                    },
                    enabled = userInput.isNotBlank() && !answered,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple,
                        disabledContainerColor = BrandPurple.copy(0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.submit), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            DuelQuestionType.PRONUNCIATION -> {
                // "I said it correctly" / "I got it wrong" buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { submitAnswer(false) },
                        enabled = !answered,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEAEA)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = BrandRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.wrong), color = BrandRed, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { submitAnswer(true) },
                        enabled = !answered,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.got_it), color = BrandGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                PronunciationBar(
                    word = question.vocabWord.word,
                    tts = tts,
                    label = stringResource(R.string.listen_again)
                )
            }
        }

        // Result overlay
        AnimatedVisibility(visible = showResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (showResult == true) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (showResult == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (showResult == true) BrandGreen else BrandRed,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (showResult == true) stringResource(R.string.correct_xp, difficulty.xpGain)
                            else stringResource(R.string.wrong_xp, difficulty.xpLoss),
                            color = if (showResult == true) BrandGreen else BrandRed,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                        if (showResult == false) {
                            Text(stringResource(R.string.answer_format, question.vocabWord.word), color = TextDark, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Result Screen ─────────────────────────────────────────────────────────────

@Composable
fun DuelResultScreen(
    correctCount: Int,
    wrongCount: Int,
    xpDelta: Int,
    difficulty: DuelDifficulty,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val total       = correctCount + wrongCount
    val accuracy    = if (total > 0) (correctCount * 100 / total) else 0
    val isPositive  = xpDelta >= 0

    // Fire XP to backend once on result screen (fire-and-forget)
    val userId = remember {
        context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
            .getString("session_id", "") ?: ""
    }
    LaunchedEffect(Unit) {
        if (userId.isNotBlank() && xpDelta != 0) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.mk.lingocoach.network.AssessmentApi.awardXp(
                    userId   = userId,
                    xpDelta  = xpDelta,
                    source   = "timely_duel"
                )
                AppCache.invalidateLearningPath()   // streak/XP changed
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Trophy or practice icon
        Icon(
            if (accuracy >= 60) Icons.Default.EmojiEvents else Icons.Default.Replay,
            contentDescription = null,
            tint = if (accuracy >= 60) BrandAmber else BrandPurple,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (accuracy >= 80) stringResource(R.string.excellent) else if (accuracy >= 60) stringResource(R.string.good_job) else stringResource(R.string.duel_keep_practicing),
            color = TextDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
        )
        Text(stringResource(R.string.duel_complete, difficulty.label), color = TextMid, fontSize = 14.sp)

        Spacer(Modifier.height(28.dp))

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ResultStat(stringResource(R.string.stat_correct).uppercase(), "$correctCount", BrandGreen)
                    ResultStat(stringResource(R.string.stat_wrong).uppercase(), "$wrongCount", BrandRed)
                    ResultStat(stringResource(R.string.stat_accuracy).uppercase(), "$accuracy%", BrandPurple)
                }
                HorizontalDivider(color = CardBorderColor)
                // XP summary
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isPositive) Icons.Default.Bolt else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isPositive) BrandGreen else BrandRed,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (isPositive) stringResource(R.string.xp_gained) else stringResource(R.string.xp_lost),
                                color = TextMid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${if (isPositive) "+" else ""}${stringResource(R.string.xp_amount, kotlin.math.abs(xpDelta))}",
                                color = if (isPositive) BrandGreen else BrandRed,
                                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(6.dp)
        ) {
            Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.play_again), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(Icons.Default.Home, null, tint = BrandPurple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.back_to_home), color = BrandPurple, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResultStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}
