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
import androidx.compose.ui.res.stringResource
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
    onNavigateToMistakes: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
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
            if (status == TextToSpeech.SUCCESS) { }
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

    // ── Drill Session State ───────────────────────────────────────────────────
    var drillQuestions by remember { mutableStateOf<List<DrillQuestion>>(emptyList()) }
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

    // ── Start Drill Session ───────────────────────────────────────────────────
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
            Toast.makeText(
                context,
                context.getString(R.string.no_vocabulary_found, activeLevel, categoryName ?: context.getString(R.string.all)),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {

            // ── Top App Bar ───────────────────────────────────────────────────
            CommonTopBar(
                title = stringResource(R.string.vocab_builder),
                onBack = { handleBackPressed() },
                onSettings = onNavigateToSettings
            )
            if (!isTrackerLoaded) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandPurple)
                }
                return@Column
            }

            // ── Screen Body ───────────────────────────────────────────────────
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
                                        if (userId.isNotBlank()) {
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                com.mk.lingocoach.network.AssessmentApi.logMistake(
                                                    userId          = userId,
                                                    word            = question.word.word,
                                                    mistakeType     = "VOCAB_DRILL",
                                                    userSentence    = chosen,
                                                    correctSentence = question.word.word,
                                                    explanation     = "Meaning: ${question.word.meaning}",
                                                    source          = "vocab_builder"
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
                                if (userId.isNotBlank()) {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        com.mk.lingocoach.network.AssessmentApi.logMistake(
                                            userId          = userId,
                                            word            = question.word.word,
                                            mistakeType     = "VOCAB_DRILL",
                                            userSentence    = "(skipped)",
                                            correctSentence = question.word.word,
                                            explanation     = "Meaning: ${question.word.meaning}",
                                            source          = "vocab_builder"
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
                            onCheckReinforcement = { showReinforcementFeedback = true },
                            onSpeakWord = {
                                tts?.speak(question.word.word, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            tts = tts,
                            onContinue = {
                                if (isCorrectFeedback) {
                                    drillQueue.removeAt(0)
                                } else {
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

// ─── Level Selector Tabs ──────────────────────────────────────────────────────
@Composable
private fun LevelSelectorTabs(
    activeLevel: String,
    onActiveLevelChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    progressVersion: Int
) {
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        levels.forEach { level ->
            val isActive = activeLevel == level
            val progress = remember(level, progressVersion) {
                VocabTracker.getLevelProgress(level)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) BrandPurple else Color.Transparent)
                    .clickable {
                        onActiveLevelChanged(level)
                        onCategorySelected(null)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = level,
                        color = if (isActive) Color.White else TextLight,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    // 3 mini progress dots
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(3) { i ->
                            val filled = progress >= ((i + 1) * 33)
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive && filled -> Color.White
                                            isActive           -> Color.White.copy(alpha = 0.35f)
                                            filled             -> BrandPurple.copy(alpha = 0.55f)
                                            else               -> Color(0xFFDDDAFF)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Dashboard ────────────────────────────────────────────────────────────────
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
    val categoriesStats = remember(activeLevel, progressVersion) {
        VocabTracker.getCategoryStats(activeLevel)
            .sortedWith(compareBy(
                { it.averageMastery >= 100 },   // fully done → sink to bottom
                { -it.totalWords }              // within same tier: more words first
            ))
    }
    val levelProgress = remember(activeLevel, progressVersion) { VocabTracker.getLevelProgress(activeLevel) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
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

                    // Stats row (no bar chart — level selection moved to tabs)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Score Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFAFAFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(text = "$activeLevel SCORE", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(text = "$levelProgress%", color = BrandPurple, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        // Daily Target Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFAFAFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(text = "DAILY TARGET", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(text = "10", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(text = "words", color = TextMid, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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

        // ── 2. Level Selector Tabs ────────────────────────────────────────────
        item {
            LevelSelectorTabs(
                activeLevel = activeLevel,
                onActiveLevelChanged = onActiveLevelChanged,
                onCategorySelected = onCategorySelected,
                progressVersion = progressVersion
            )
        }

        // ── 3. Search Bar ─────────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .bringIntoViewOnFocus(),
                placeholder = { Text(stringResource(R.string.search_vocabulary_topics), color = TextLight, fontSize = 14.sp) },
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

        // ── 4. Search Results or Topic Curations ──────────────────────────────
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
                        Text("${stringResource(R.string.category)}: ${w.category}", color = TextLight, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        } else {
            // ── Topic Curations ───────────────────────────────────────────────
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
                                    Brush.linearGradient(colors = listOf(BrandPurple, BrandPurpleLight))
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

            // Other category cards
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

    // ── Bottom Nav Bar ────────────────────────────────────────────────────────
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

// ─── Contextual Drill ─────────────────────────────────────────────────────────
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
        val levelProgress = remember(level) { VocabTracker.getLevelProgress(level) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "$level MASTERY PROGRESS", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "$levelProgress%", color = BrandPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { levelProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = BrandPurple,
                trackColor = Color(0xFFDDDAFF)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth().shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = question.word.word, color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "(${question.word.partOfSpeech.replaceFirstChar { it.uppercase() }}) /${question.word.pronunciation}/",
                    color = TextLight, fontSize = 13.sp, fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(14.dp))
                CardPronunciation(word = question.word.word, tts = tts)
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0x0D000000))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = question.word.meaning, color = TextMid, fontSize = 14.sp, lineHeight = 18.sp)
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "CONTEXTUAL DRILL", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = question.questionText, color = TextDark, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF6F5FF) else CardWhite),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (isSelected) BrandPurpleSoft else Color(0xFFFAFAFF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = optionLetter, color = if (isSelected) BrandPurple else TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = question.options[index], color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNotMastered,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEAEA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Not Mastered", color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Button(
                onClick = onConfirm,
                enabled = selectedOptionIdx != null,
                modifier = Modifier.weight(1f).height(48.dp),
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

// ─── Drill Feedback ───────────────────────────────────────────────────────────
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
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
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
                        Text(text = word.word, color = TextDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "(${word.partOfSpeech.replaceFirstChar { it.uppercase() }}) /${word.pronunciation}/",
                            color = TextLight, fontSize = 13.sp, fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CardPronunciation(word = word.word, tts = tts)
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x0D000000))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = word.meaning, color = TextMid, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "USAGE REFINEMENT", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, CardBorderColor, RoundedCornerShape(16.dp)),
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
                                Text(text = word.mappedCategory, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Used in contexts relating to ${word.category.lowercase()}.", color = TextLight, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFF), RoundedCornerShape(8.dp))
                        ) {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(BrandPurple))
                            val exampleSent = word.examples.firstOrNull()?.english ?: ""
                            Text(
                                text = "\"$exampleSent\"",
                                color = TextDark, fontSize = 12.sp, fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(12.dp), lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "QUICK REINFORCEMENT", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Type the word to match the definition:", color = TextDark, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFF), RoundedCornerShape(10.dp)).padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"${word.word.replace(Regex("[a-zA-Z]"), "_ ")}\"",
                                    color = TextMid, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextLight, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val isReinforcementCorrect = reinforcementText.trim().lowercase() == word.word.trim().lowercase()
                        OutlinedTextField(
                            value = reinforcementText,
                            onValueChange = onReinforcementChanged,
                            modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                            placeholder = { Text(stringResource(R.string.type_the_match), color = TextLight, fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                onCheckReinforcement()
                            }),
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

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Continue to next word", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── All Words Browser ────────────────────────────────────────────────────────
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

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val t = TextToSpeech(context) { }
        t.language = Locale.US
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.all_level_vocabulary, activeLevel), color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Show Categories",
                color = BrandPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBackToCategories() }
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)).bringIntoViewOnFocus(),
            placeholder = { Text(stringResource(R.string.search_words_meanings), color = TextLight, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextLight) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = BrandPurple
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextLight, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.sort_by), color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            .border(0.5.dp, if (isActive) BrandPurple else Color(0xFFDDDAFF), RoundedCornerShape(20.dp))
                            .clickable { sortOrder = order }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(order.icon, contentDescription = null, tint = if (isActive) Color.White else BrandPurple, modifier = Modifier.size(12.dp))
                            Text(text = order.label, color = if (isActive) Color.White else TextMid, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Text(text = "${words.size} words", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)

        if (words.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No words found matching \"$searchQuery\"", color = TextMid, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(getCategoryIconBackground(w.mappedCategory), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = getCategoryIcon(w.mappedCategory), contentDescription = null, tint = getCategoryIconTint(w.mappedCategory), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = w.word, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "(${w.partOfSpeech}) /${w.pronunciation}/", color = TextLight, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (score >= 80) BrandGreen.copy(alpha = 0.15f) else BrandAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(
                                            if (score >= 80) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (score >= 80) BrandGreen else BrandAmberDark,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = if (score >= 80) "MASTERED" else "LEARNING",
                                            color = if (score >= 80) BrandGreen else BrandAmberDark,
                                            fontSize = 9.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { VocabTracker.toggleBookmark(w.word, context); bookmarksVersion++ },
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
                                    Text(stringResource(R.string.definition), color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(w.meaning, color = TextDark, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                    Text(stringResource(R.string.category), color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(w.category, color = TextDark, fontSize = 13.sp)
                                if (w.examples.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(stringResource(R.string.example), color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    val ex = w.examples.first()
                                    Text("\"${ex.english}\"", color = TextDark, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                                    val translation = ex.translations.values.firstOrNull()
                                    if (translation != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(translation, color = TextMid, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                PronunciationBar(word = w.word, tts = tts, label = "Listen Pronunciation")
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ─── Category Icon Helpers ────────────────────────────────────────────────────

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
