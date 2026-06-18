package com.mk.lingocoach.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import kotlinx.coroutines.launch
import java.util.Locale

// ─── Local Navigation State ──────────────────────────────────────────────────
enum class VocabViewState {
    Dashboard,
    ContextualDrill,
    DrillFeedback
}

// ─── Sort Order for All Words Browser ────────────────────────────────────────
enum class VocabSortOrder(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALPHA_ASC("A → Z", Icons.Default.SortByAlpha),
    ALPHA_DESC("Z → A", Icons.AutoMirrored.Filled.Sort),
    MOST_USED("Most Used", Icons.AutoMirrored.Filled.TrendingUp),
    MASTERED("Mastered First", Icons.Default.Star),
    LEARNING("Learning First", Icons.Default.AutoStories)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabBuilderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = onNavigateBack,
    onNavigateToAILab: () -> Unit = {},
    onNavigateToMistakes: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Vocab Load State ──────────────────────────────────────────────────────
    var isTrackerLoaded by remember { mutableStateOf(VocabTracker.isLoaded) }
    val userId = remember {
        context.getSharedPreferences("LingoCoachPrefs", android.content.Context.MODE_PRIVATE)
            .getString("session_id", "") ?: ""
    }

    LaunchedEffect(Unit) {
        if (!VocabTracker.isLoaded) {
            VocabTracker.init(context)
            isTrackerLoaded = true
        }
        // Sync vocab progress to backend in background
        if (userId.isNotBlank()) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                VocabTracker.syncToBackend(userId, context)
            }
        }
    }

    // ── TextToSpeech ──────────────────────────────────────────────────────────
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialized
            }
        }
        t.language = Locale.US
        tts = t
        onDispose {
            t.stop()
            t.shutdown()
        }
    }

    // ── View States ───────────────────────────────────────────────────────────
    var currentViewState by remember { mutableStateOf(VocabViewState.Dashboard) }
    var activeLevel by remember { mutableStateOf("A1") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAllWords by remember { mutableStateOf(false) }
    var progressVersion by remember { mutableIntStateOf(0) }

    // ── Drill Session State ───────────────────────────────────────────────
    var drillQuestions by remember { mutableStateOf<List<DrillQuestion>>(emptyList()) }
    // Queue of questions still remaining (wrong answers are re-appended)
    val drillQueue = remember { mutableStateListOf<DrillQuestion>() }
    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    var isCorrectFeedback by remember { mutableStateOf(false) }
    var reinforcementText by remember { mutableStateOf("") }
    var showReinforcementFeedback by remember { mutableStateOf(false) }

    // ── Back Handlers ─────────────────────────────────────────────────────────
    val handleBackPressed = {
        if (currentViewState != VocabViewState.Dashboard) {
            currentViewState = VocabViewState.Dashboard
        } else if (showAllWords) {
            showAllWords = false
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPressed()
    }

    // Helpers to start session
    val startDrillSession = { categoryName: String? ->
        val questions = VocabTracker.generateDrillSession(activeLevel, categoryName, 5)
        if (questions.isNotEmpty()) {
            drillQuestions = questions
            drillQueue.clear()
            drillQueue.addAll(questions)
            currentQuestionIdx = 0
            selectedOptionIdx = null
            isAnswered = false
            reinforcementText = ""
            showReinforcementFeedback = false
            currentViewState = VocabViewState.ContextualDrill
        } else {
            Toast.makeText(context, "No vocabulary found for $activeLevel / ${categoryName ?: "All"}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()

        // ── Core Layout ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            
            // ── Top App Bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { handleBackPressed() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
                Text(
                    text = "LingoCoach",
                    style = TextStyle(
                        color = TextDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                IconButton(
                    onClick = { /* Settings Action */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextDark)
                }
            }

            if (!isTrackerLoaded) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandPurple)
                }
                return@Column
            }

            // ── Screen Body Content ───────────────────────────────────────────
            when (currentViewState) {
                VocabViewState.Dashboard -> {
                    if (showAllWords) {
                        AllWordsBrowserView(
                            activeLevel = activeLevel,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { searchQuery = it },
                            onBackToCategories = { showAllWords = false }
                        )
                    } else {
                        DashboardView(
                            activeLevel = activeLevel,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            showAllWords = showAllWords,
                            onShowAllWordsChanged = { showAllWords = it },
                            onActiveLevelChanged = { activeLevel = it },
                            onSearchQueryChanged = { searchQuery = it },
                            onCategorySelected = { selectedCategory = it },
                            onContinueSession = { startDrillSession(selectedCategory) },
                            onStartDrillForCategory = { startDrillSession(it) },
                            onNavigateHome = onNavigateToHome,
                            onNavigateToAILab = onNavigateToAILab,
                            onNavigateToMistakes = onNavigateToMistakes,
                            progressVersion = progressVersion
                        )
                    }
                }
                VocabViewState.ContextualDrill -> {
                    // Show current head of drillQueue
                    if (drillQueue.isNotEmpty()) {
                        val question = drillQueue.first()
                        ContextualDrillView(
                            level = activeLevel,
                            question = question,
                            questionNum = currentQuestionIdx + 1,
                            totalQuestions = drillQuestions.size,
                            selectedOptionIdx = selectedOptionIdx,
                            onOptionSelected = { selectedOptionIdx = it },
                            onConfirm = {
                                if (selectedOptionIdx != null) {
                                    val isCorrect = selectedOptionIdx == question.correctIndex
                                    isCorrectFeedback = isCorrect
                                    isAnswered = true
                                    VocabTracker.updateWordMastery(question.word.word, isCorrect, context)
                                    progressVersion++
                                    if (!isCorrect) {
                                        val chosen = question.options.getOrNull(selectedOptionIdx!!) ?: ""
                                        VocabTracker.addLocalMistake(
                                            word = question.word.word,
                                            mistakeType = "VOCAB_DRILL",
                                            userAnswer = chosen,
                                            correctAnswer = question.word.word,
                                            explanation = "Meaning: ${question.word.meaning}",
                                            context = context
                                        )
                                        // Also log to backend (fire-and-forget)
                                        if (userId.isNotBlank()) {
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                com.mk.lingocoach.network.AssessmentApi.logMistake(
                                                    userId          = userId,
                                                    word            = question.word.word,
                                                    mistakeType     = "VOCAB_DRILL",
                                                    userSentence    = chosen,
                                                    correctSentence = question.word.word,
                                                    explanation     = "Meaning: ${question.word.meaning}"
                                                )
                                            }
                                        }
                                    }
                                    currentViewState = VocabViewState.DrillFeedback
                                }
                            },
                            onNotMastered = {
                                isCorrectFeedback = false
                                isAnswered = true
                                VocabTracker.updateWordMastery(question.word.word, false, context)
                                progressVersion++
                                VocabTracker.addLocalMistake(
                                    word = question.word.word,
                                    mistakeType = "VOCAB_DRILL",
                                    userAnswer = "(skipped)",
                                    correctAnswer = question.word.word,
                                    explanation = "Meaning: ${question.word.meaning}",
                                    context = context
                                )
                                // Also log to backend (fire-and-forget)
                                if (userId.isNotBlank()) {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        com.mk.lingocoach.network.AssessmentApi.logMistake(
                                            userId          = userId,
                                            word            = question.word.word,
                                            mistakeType     = "VOCAB_DRILL",
                                            userSentence    = "(skipped)",
                                            correctSentence = question.word.word,
                                            explanation     = "Meaning: ${question.word.meaning}"
                                        )
                                    }
                                }
                                currentViewState = VocabViewState.DrillFeedback
                            },
                            tts = tts
                        )
                    } else {
                        currentViewState = VocabViewState.Dashboard
                    }
                }
                VocabViewState.DrillFeedback -> {
                    if (drillQueue.isNotEmpty()) {
                        val question = drillQueue.first()
                        DrillFeedbackView(
                            word = question.word,
                            isCorrect = isCorrectFeedback,
                            reinforcementText = reinforcementText,
                            onReinforcementChanged = { reinforcementText = it },
                            showReinforcementFeedback = showReinforcementFeedback,
                            onCheckReinforcement = {
                                showReinforcementFeedback = true
                            },
                            onSpeakWord = {
                                tts?.speak(question.word.word, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            tts = tts,
                            onContinue = {
                                if (isCorrectFeedback) {
                                    // Correct: remove from queue permanently
                                    drillQueue.removeAt(0)
                                } else {
                                    // Wrong: move to end of queue for retry
                                    val failed = drillQueue.removeAt(0)
                                    drillQueue.add(failed)
                                }
                                currentQuestionIdx++
                                selectedOptionIdx = null
                                isAnswered = false
                                reinforcementText = ""
                                showReinforcementFeedback = false
                                if (drillQueue.isEmpty()) {
                                    if (userId.isNotBlank()) {
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            com.mk.lingocoach.network.AssessmentApi.awardXp(userId, 10, "vocab")
                                            VocabTracker.syncToBackend(userId, context)
                                        }
                                    }
                                    currentViewState = VocabViewState.Dashboard
                                } else {
                                    currentViewState = VocabViewState.ContextualDrill
                                }
                            }
                        )
                    } else {
                        currentViewState = VocabViewState.Dashboard
                    }
                }
            }
        }
    }
}

// ─── Dashboard Component (Image 1) ───────────────────────────────────────────
@Composable
fun ColumnScope.DashboardView(
    activeLevel: String,
    searchQuery: String,
    selectedCategory: String?,
    showAllWords: Boolean,
    onShowAllWordsChanged: (Boolean) -> Unit,
    onActiveLevelChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onContinueSession: () -> Unit,
    onStartDrillForCategory: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToAILab: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    progressVersion: Int
) {
    val context = LocalContext.current
    val categoriesStats = remember(activeLevel, progressVersion) { VocabTracker.getCategoryStats(activeLevel) }
    val levelProgress = remember(activeLevel, progressVersion) { VocabTracker.getLevelProgress(activeLevel) }
    
    // Auto-select first category if none is selected, to match mockup design layout
    LaunchedEffect(activeLevel) {
        if (categoriesStats.isNotEmpty() && selectedCategory == null) {
            onCategorySelected(categoriesStats.first().name)
        }
    }

    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // ── 1. Mastery Progress Card ──────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), clip = true),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT PROFICIENCY",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mastery Progress",
                                color = TextDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        // Tier Badge
                        Box(
                            modifier = Modifier
                                .background(BrandPurpleSoft, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (activeLevel) {
                                    "A1", "A2" -> "Beginner Tier"
                                    "B1", "B2" -> "Intermediate Tier"
                                    else -> "Advanced Tier"
                                },
                                color = BrandPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Bar Chart (60% width)
                        Row(
                            modifier = Modifier.weight(1.2f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
                            
                            levels.forEachIndexed { index, lvl ->
                                val progress = remember(lvl, progressVersion) { VocabTracker.getLevelProgress(lvl) }
                                val resolvedHeight = 0.05f + (progress / 100f) * 0.95f
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            onActiveLevelChanged(lvl)
                                            onCategorySelected(null)
                                        }
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height((60 * resolvedHeight).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (activeLevel == lvl) BrandPurple else Color(0xFFDDDAFF))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = lvl,
                                        color = if (activeLevel == lvl) BrandPurple else TextLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Stats (40% width)
                        Column(
                            modifier = Modifier.weight(0.8f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Score Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFAFAFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(text = "$activeLevel SCORE", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "$levelProgress%", color = BrandPurple, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            // Target Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFAFAFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(text = "DAILY TARGET", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(text = "10", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(text = "words", color = TextMid, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Continue Session Button
                    Button(
                        onClick = onContinueSession,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Continue Session >",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── 2. Search Bar ─────────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .bringIntoViewOnFocus(),
                placeholder = { Text("Search vocabulary topics...", color = TextLight, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextLight) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = BrandPurple
                ),
                singleLine = true
            )
        }

        // ── Search Results or Topic Curations ─────────────────────────────────
        if (searchQuery.isNotBlank()) {
            val searchedWords = VocabTracker.searchWords(searchQuery, activeLevel)
            
            item {
                Text(
                    text = "Search Results (${searchedWords.size} words)",
                    color = TextDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (searchedWords.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No words matching \"$searchQuery\"", color = TextMid, fontSize = 14.sp)
                    }
                }
            } else {
                items(searchedWords) { w ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(w.word, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(w.partOfSpeech.uppercase(), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(w.meaning, color = TextMid, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Category: ${w.category}", color = TextLight, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        } else {
            // Topic Curations
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Topic Curations",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View All",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onShowAllWordsChanged(true) }
                    )
                }
            }

            // Featured Topic Card
            val featuredCat = categoriesStats.firstOrNull { it.name == selectedCategory } ?: categoriesStats.firstOrNull()
            if (featuredCat != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartDrillForCategory(featuredCat.name) }
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = true),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(BrandPurple, BrandPurpleLight)
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            getCategoryIcon(featuredCat.name),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // Stars
                                    Row {
                                        repeat(3) { starIndex ->
                                            Icon(
                                                imageVector = if (starIndex < featuredCat.starRating) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = featuredCat.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simple custom progress bar for featured card
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(featuredCat.averageMastery / 100f)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${featuredCat.averageMastery}% Mastery",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = featuredCat.description,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // List of other category cards
            val otherCategories = categoriesStats.filter { it.name != (featuredCat?.name ?: "") }
            items(otherCategories) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(cat.name) }
                        .border(0.5.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(getCategoryIconBackground(cat.name), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat.name),
                                contentDescription = null,
                                tint = getCategoryIconTint(cat.name),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                color = TextDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${cat.totalWords} words  •  Mastery ${cat.averageMastery}%",
                                color = TextLight,
                                fontSize = 11.sp
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // ── Bottom Nav Bar ──
    HomeBottomNav(
        selectedTab = 2,
        onTabSelected = { index ->
            when (index) {
                0 -> onNavigateHome()
                1 -> onNavigateToAILab()
                2 -> Unit
                3 -> onNavigateToMistakes()
            }
        }
    )
}

// ─── Contextual Drill Component (Image 3) ────────────────────────────────────
@Composable
fun ColumnScope.ContextualDrillView(
    level: String,
    question: DrillQuestion,
    questionNum: Int,
    totalQuestions: Int,
    selectedOptionIdx: Int?,
    onOptionSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    onNotMastered: () -> Unit,
    tts: TextToSpeech? = null
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // C1 Mastery Progress bar
        val levelProgress = remember(level) { VocabTracker.getLevelProgress(level) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$level MASTERY PROGRESS",
                    color = TextLight,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$levelProgress%",
                    color = BrandPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { levelProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = BrandPurple,
                trackColor = Color(0xFFDDDAFF)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Word Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Word + part of speech
                Text(
                    text = question.word.word,
                    color = TextDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "(${question.word.partOfSpeech.replaceFirstChar { it.uppercase() }}) /${question.word.pronunciation}/",
                    color = TextLight,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic
                )

                Spacer(modifier = Modifier.height(14.dp))
                // Centered pronunciation: speaker icon + S M F pills
                CardPronunciation(
                    word = question.word.word,
                    tts = tts
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0x0D000000))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = question.word.meaning,
                    color = TextMid,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Contextual Drill section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "CONTEXTUAL DRILL",
                color = TextLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = question.questionText,
                color = TextDark,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Options A, B, C, D
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(question.options.size) { index ->
                val optionLetter = ('A' + index).toString()
                val isSelected = selectedOptionIdx == index
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(index) }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) BrandPurple else CardBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFF6F5FF) else CardWhite
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isSelected) BrandPurpleSoft else Color(0xFFFAFAFF),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLetter,
                                color = if (isSelected) BrandPurple else TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = question.options[index],
                            color = TextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNotMastered,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEAEA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Not Mastered", color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Button(
                onClick = onConfirm,
                enabled = selectedOptionIdx != null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPurple,
                    disabledContainerColor = BrandPurple.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = "Confirm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ─── Drill Feedback Component (Image 2) ──────────────────────────────────────
@Composable
fun ColumnScope.DrillFeedbackView(
    word: VocabWord,
    isCorrect: Boolean,
    reinforcementText: String,
    onReinforcementChanged: (String) -> Unit,
    showReinforcementFeedback: Boolean,
    onCheckReinforcement: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakWordAtRate: (Float) -> Unit = { onSpeakWord() },
    tts: TextToSpeech? = null,
    onContinue: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Banner and Word Info Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    // Success Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) BrandGreen else BrandRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCorrect) "PERFECT!" else "INCORRECT",
                                color = if (isCorrect) BrandGreen else BrandRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Word + pronunciation text
                        Text(
                            text = word.word,
                            color = TextDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "(${word.partOfSpeech.replaceFirstChar { it.uppercase() }}) /${word.pronunciation}/",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        // Centered speaker + S M F pills
                        CardPronunciation(
                            word = word.word,
                            tts = tts
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x0D000000))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = word.meaning,
                            color = TextMid,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Usage Refinement Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "USAGE REFINEMENT",
                    color = TextLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BrandPurpleSoft, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = word.mappedCategory,
                                    color = TextDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Used in contexts relating to ${word.category.lowercase()}.",
                                    color = TextLight,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Example box with left vertical accent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFAFF), RoundedCornerShape(8.dp))
                        ) {
                            // Left accent line
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(BrandPurple)
                            )
                            
                            val exampleSent = word.examples.firstOrNull()?.english ?: ""
                            Text(
                                text = "\"$exampleSent\"",
                                color = TextDark,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Reinforcement Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "QUICK REINFORCEMENT",
                    color = TextLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Type the word to match the definition:",
                            color = TextDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Synonym match indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFAFF), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"${word.word.replace(Regex("[a-zA-Z]"), "_ ")}\"",
                                    color = TextMid,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextLight, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Type Input Box
                        val isReinforcementCorrect = reinforcementText.trim().lowercase() == word.word.trim().lowercase()
                        OutlinedTextField(
                            value = reinforcementText,
                            onValueChange = onReinforcementChanged,
                            modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                            placeholder = { Text("Type the match...", color = TextLight, fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    onCheckReinforcement()
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (showReinforcementFeedback) {
                                    Icon(
                                        imageVector = if (isReinforcementCorrect) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isReinforcementCorrect) BrandGreen else BrandRed
                                    )
                                } else {
                                    IconButton(onClick = {
                                        keyboardController?.hide()
                                        onCheckReinforcement()
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Verify", tint = BrandPurple)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0D0D0D),
                                unfocusedTextColor = Color(0xFF0D0D0D),
                                focusedBorderColor = if (showReinforcementFeedback) (if (isReinforcementCorrect) BrandGreen else BrandRed) else BrandPurple,
                                unfocusedBorderColor = if (showReinforcementFeedback) (if (isReinforcementCorrect) BrandGreen else BrandRed) else CardBorderColor
                            )
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        
        // Progress Bar & Next Button
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = BrandPurple,
                    trackColor = Color(0xFFDDDAFF)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue to next word",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}


@Composable
fun ColumnScope.AllWordsBrowserView(
    activeLevel: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onBackToCategories: () -> Unit
) {
    val context = LocalContext.current
    var bookmarksVersion by remember { mutableStateOf(0) }
    var sortOrder by remember { mutableStateOf(VocabSortOrder.ALPHA_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val words = remember(activeLevel, searchQuery, bookmarksVersion, sortOrder) {
        val base = VocabTracker.searchWords(searchQuery, activeLevel)
        when (sortOrder) {
            VocabSortOrder.ALPHA_ASC  -> base.sortedBy { it.word.lowercase() }
            VocabSortOrder.ALPHA_DESC -> base.sortedByDescending { it.word.lowercase() }
            VocabSortOrder.MOST_USED  -> base.sortedByDescending { VocabTracker.getMasteryScore(it.word) }
            VocabSortOrder.MASTERED   -> base.sortedByDescending { VocabTracker.getMasteryScore(it.word) >= 80 }
            VocabSortOrder.LEARNING   -> base.sortedBy { VocabTracker.getMasteryScore(it.word) }
        }
    }

    // TTS voice support inside words list
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val t = TextToSpeech(context) { }
        t.language = Locale.US
        tts = t
        onDispose {
            t.stop()
            t.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All $activeLevel Vocabulary",
                color = TextDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Show Categories",
                color = BrandPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBackToCategories() }
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .bringIntoViewOnFocus(),
            placeholder = { Text("Search words, meanings...", color = TextLight, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextLight) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = BrandPurple
            ),
            singleLine = true
        )

        // ── Filter / Sort Bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = TextLight,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Sort by:",
                color = TextLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            // Scrollable chips row
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(VocabSortOrder.values().size) { idx ->
                    val order = VocabSortOrder.values()[idx]
                    val isActive = sortOrder == order
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) BrandPurple else Color.White.copy(alpha = 0.85f))
                            .border(
                                0.5.dp,
                                if (isActive) BrandPurple else Color(0xFFDDDAFF),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { sortOrder = order }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                order.icon,
                                contentDescription = null,
                                tint = if (isActive) Color.White else BrandPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = order.label,
                                color = if (isActive) Color.White else TextMid,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Words count label
        Text(
            text = "${words.size} words",
            color = TextLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        if (words.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No words found matching \"$searchQuery\"", color = TextMid, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(words) { w ->
                    var expanded by remember { mutableStateOf(false) }
                    val isStarred = VocabTracker.isBookmarked(w.word)
                    val score = VocabTracker.getMasteryScore(w.word)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .animateContentSize()
                            .border(0.5.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            getCategoryIconBackground(w.mappedCategory),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(w.mappedCategory),
                                        contentDescription = null,
                                        tint = getCategoryIconTint(w.mappedCategory),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = w.word,
                                        color = TextDark,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "(${w.partOfSpeech}) /${w.pronunciation}/",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }

                                // Mastery tag
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (score >= 80) BrandGreen.copy(alpha = 0.15f) else BrandAmber.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            if (score >= 80) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (score >= 80) BrandGreen else BrandAmberDark,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = if (score >= 80) "MASTERED" else "LEARNING",
                                            color = if (score >= 80) BrandGreen else BrandAmberDark,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Bookmark Toggle
                                IconButton(
                                    onClick = {
                                        VocabTracker.toggleBookmark(w.word, context)
                                        bookmarksVersion++
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isStarred) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isStarred) BrandPurple else TextLight
                                    )
                                }
                            }

                            if (expanded) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0x0D000000))
                                Spacer(Modifier.height(12.dp))

                                Text("Definition", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(w.meaning, color = TextDark, fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Category", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(w.category, color = TextDark, fontSize = 13.sp)

                                if (w.examples.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Example", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    val ex = w.examples.first()
                                    Text("\"${ex.english}\"", color = TextDark, fontSize = 13.sp, fontStyle = FontStyle.Italic)

                                    val translation = ex.translations.values.firstOrNull()
                                    if (translation != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(translation, color = TextMid, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                // Pronunciation bar with speed control
                                PronunciationBar(
                                    word = w.word,
                                    tts = tts,
                                    label = "Listen Pronunciation"
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ─── Category Icon Helpers ─────────────────────────────────────────────────────

fun getCategoryIcon(category: String): ImageVector {
    val c = category.lowercase()
    return when {
        c.contains("animal")                             -> Icons.Default.Pets
        c.contains("emotion") || c.contains("feeling")  -> Icons.Default.Mood
        c.contains("food") || c.contains("cook") || c.contains("drink") -> Icons.Default.Restaurant
        c.contains("health") || c.contains("body")      -> Icons.Default.FavoriteBorder
        c.contains("travel") || c.contains("transport") -> Icons.Default.Flight
        c.contains("business") || c.contains("work") || c.contains("job") -> Icons.Default.Work
        c.contains("nature") || c.contains("environ")   -> Icons.Default.Park
        c.contains("tech") || c.contains("internet")    -> Icons.Default.Laptop
        c.contains("school") || c.contains("education") || c.contains("academic") -> Icons.Default.School
        c.contains("sport") || c.contains("activ")      -> Icons.Default.SportsSoccer
        c.contains("family") || c.contains("relation")  -> Icons.Default.People
        c.contains("home") || c.contains("house")       -> Icons.Default.Home
        c.contains("cloth") || c.contains("fashion")    -> Icons.Default.Checkroom
        c.contains("color") || c.contains("shape")      -> Icons.Default.Palette
        c.contains("number") || c.contains("time")      -> Icons.Default.Schedule
        c.contains("weather")                            -> Icons.Default.WbCloudy
        c.contains("place") || c.contains("city")       -> Icons.Default.LocationOn
        c.contains("greeting") || c.contains("personal") -> Icons.Default.EmojiPeople
        c.contains("music") || c.contains("movie") || c.contains("media") -> Icons.Default.MusicNote
        c.contains("finance") || c.contains("econom") || c.contains("money") -> Icons.Default.AccountBalance
        c.contains("social") || c.contains("communic")  -> Icons.Default.Forum
        c.contains("politic") || c.contains("news")     -> Icons.Default.Newspaper
        c.contains("psychol")                            -> Icons.Default.Psychology
        c.contains("debate") || c.contains("opinion")   -> Icons.Default.RecordVoiceOver
        c.contains("grammar") || c.contains("connect")  -> Icons.Default.Spellcheck
        c.contains("idiom") || c.contains("phrasal")    -> Icons.Default.FormatQuote
        c.contains("product")                            -> Icons.Default.CheckCircle
        c.contains("culture")                            -> Icons.Default.Language
        c.contains("shopping")                           -> Icons.Default.ShoppingBag
        c.contains("daily") || c.contains("activit")    -> Icons.Default.CalendarToday
        c.contains("goal")                               -> Icons.Default.Flag
        c.contains("vocab") || c.contains("word")       -> Icons.AutoMirrored.Filled.MenuBook
        else                                             -> Icons.AutoMirrored.Filled.MenuBook
    }
}

fun getCategoryIconTint(category: String): androidx.compose.ui.graphics.Color {
    val c = category.lowercase()
    return when {
        c.contains("animal")                             -> Color(0xFF4CAF50)
        c.contains("emotion") || c.contains("feeling")  -> Color(0xFFE91E63)
        c.contains("food") || c.contains("cook") || c.contains("drink") -> Color(0xFFFF5722)
        c.contains("health") || c.contains("body")      -> Color(0xFFF44336)
        c.contains("travel") || c.contains("transport") -> Color(0xFF2196F3)
        c.contains("business") || c.contains("work") || c.contains("job") -> Color(0xFF607D8B)
        c.contains("nature") || c.contains("environ")   -> Color(0xFF4CAF50)
        c.contains("tech") || c.contains("internet")    -> Color(0xFF3F51B5)
        c.contains("school") || c.contains("education") || c.contains("academic") -> Color(0xFF9C27B0)
        c.contains("sport") || c.contains("activ")      -> Color(0xFF00BCD4)
        c.contains("family") || c.contains("relation")  -> Color(0xFFFF9800)
        c.contains("home") || c.contains("house")       -> Color(0xFF795548)
        c.contains("cloth") || c.contains("fashion")    -> Color(0xFFE91E63)
        c.contains("color") || c.contains("shape")      -> Color(0xFF9C27B0)
        c.contains("number") || c.contains("time")      -> Color(0xFF607D8B)
        c.contains("weather")                            -> Color(0xFF03A9F4)
        c.contains("place") || c.contains("city")       -> Color(0xFFFF5722)
        c.contains("finance") || c.contains("econom") || c.contains("money") -> Color(0xFF4CAF50)
        c.contains("politic") || c.contains("news")     -> Color(0xFF607D8B)
        c.contains("psychol")                            -> Color(0xFF9C27B0)
        c.contains("music") || c.contains("movie") || c.contains("media") -> Color(0xFFE91E63)
        else                                             -> BrandPurple
    }
}

fun getCategoryIconBackground(category: String): androidx.compose.ui.graphics.Color {
    return getCategoryIconTint(category).copy(alpha = 0.12f)
}
