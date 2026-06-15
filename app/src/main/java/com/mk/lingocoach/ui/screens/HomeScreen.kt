package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.CurrentModule
import com.mk.lingocoach.network.CurrentSublesson
import com.mk.lingocoach.network.Mistake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─── Shared Design Tokens (same across Home + Lesson) ────────────────────────
internal val BrandPurple      = Color(0xFF6A5CFF)
internal val BrandPurpleLight = Color(0xFF8A79FF)
internal val BrandPurpleSoft  = Color(0xFFF0EEFF)
internal val BrandAmber       = Color(0xFFFFB300)
internal val BrandAmberDark   = Color(0xFFFF6D00)
internal val BrandRed         = Color(0xFFE53935)
internal val BrandGreen       = Color(0xFF4CAF50)
internal val TextDark         = Color(0xFF1A1040)
internal val TextMid          = Color(0xFF5A5370)
internal val TextLight        = Color(0xFF9B96B0)
internal val CardWhite        = Color(0xFFFFFEFF)
internal val CardBorderColor  = Color(0x18000000)

@Composable
fun HomeScreen(
    onNavigateToLesson: (sublessonId: String) -> Unit,
    onNavigateToVocab: () -> Unit = {},
    onNavigateToMistakes: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToDuel: () -> Unit = {}
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val sharedPrefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scrollState   = rememberScrollState()

    var selectedTab   by remember { mutableStateOf(0) }
    var learningPath  by remember { mutableStateOf<CurrentLearningPathResponse?>(null) }
    var mistakes      by remember { mutableStateOf<List<Mistake>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var isVocabLoaded by remember { mutableStateOf(VocabTracker.isLoaded) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            if (!VocabTracker.isLoaded) {
                VocabTracker.init(context)
                scope.launch(Dispatchers.Main) {
                    isVocabLoaded = true
                }
            }
        }
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getCurrentLearningPath(userId) { path ->
                scope.launch(Dispatchers.Main) { learningPath = path; isLoading = false }
            }
            AssessmentApi.getMistakes(userId) { m ->
                scope.launch(Dispatchers.Main) { mistakes = m ?: emptyList() }
            }
        }
    }

    val activeModule    = learningPath?.modules?.firstOrNull { it.status == "current" }
    val activeLesson    = activeModule?.lessons?.firstOrNull { it.status == "current" }
    val activeSublesson = activeLesson?.sublessons?.firstOrNull { it.status == "current" }
    val streak = learningPath?.streak ?: 7
    val tier   = learningPath?.tier ?: "B2 Level"

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background ──────────────────────────────────────────────────────
        Image(
            painter      = painterResource(R.drawable.background),
            contentDescription = null,
            modifier     = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ── Content ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Header (Alex, Streak, Settings) ─────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Good morning, Alex",
                            color = TextMid,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Home",
                            style = TextStyle(
                                color = TextDark,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Streak chip
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF8E1), RoundedCornerShape(20.dp))
                                .border(0.5.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "$streak",
                                    color = BrandAmberDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        // Settings Button
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── Daily Stats Section ─────────────────────────────────────
                Column {
                    Text(
                        "Daily Stats",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    HomeDailyStatsCard(tier = tier, streak = streak)
                }

                // ── Learning Path header ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Learning Path",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "View Map",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                // ── Current Module Card ──────────────────────────────────────
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .shadow(4.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(CardWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandPurple,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (activeModule != null) {
                    HomeCurrentModuleCard(
                        module         = activeModule,
                        activeSublesson = activeSublesson,
                        onContinue     = {
                            val id = activeSublesson?.id ?: ""
                            if (id.isNotBlank()) onNavigateToLesson(id)
                        }
                    )
                } else {
                    HomeNoModuleCard()
                }

                // ── Quick Feature Grid (Timely Duel / AI Lab) ───────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimelyDuelCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDuel
                    )
                    HomeFeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GraphicEq,
                        title = "AI Lab",
                        subtitle = "PRONUNCIATION",
                        backgroundColor = BrandPurple,
                        textColor = Color.White,
                        iconColor = Color.White,
                        onClick = { }
                    )
                }

                // ── Vocab Builder Card ───────────────────────────────────────
                HomeVocabBuilderCard(onClick = onNavigateToVocab)

                // ── Mistake Vault Card ───────────────────────────────────────
                HomeMistakeVaultCard(mistakes = mistakes, onClick = onNavigateToMistakes)

                // ── Activity Section ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activity",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Details",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                HomeSpeakingStats()

                Spacer(Modifier.height(16.dp))
            }

            // ── Bottom Navigation ────────────────────────────────────────────
            HomeBottomNav(
                selectedTab   = selectedTab,
                onTabSelected = { 
                    selectedTab = it
                    when (it) {
                        2 -> onNavigateToVocab()
                        3 -> onNavigateToMistakes()
                    }
                }
            )
        }
    }
}

// ─── Daily Stats Card Component ──────────────────────────────────────────────
@Composable
fun HomeDailyStatsCard(tier: String, streak: Int) {
    val isVocabLoaded = VocabTracker.isLoaded
    val vocabProgress = if (isVocabLoaded) VocabTracker.getOverallProgressPercent() / 100f else 0.45f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Capsules Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapsuleProgressIndicator("LESSONS", 0.65f)
                CapsuleProgressIndicator("GRAMMAR", 0.85f)
                CapsuleProgressIndicator("VOCAB", vocabProgress)
                CapsuleProgressIndicator("PRONUNC.", 0.90f)
                CapsuleProgressIndicator("FLUENCY", 0.50f)
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0x0D000000))
            Spacer(modifier = Modifier.height(16.dp))

            // Bottom stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overall level
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Overall", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(tier, color = BrandPurple, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                // Vertical divider
                Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color(0x1A000000)))

                // Accuracy
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Accuracy", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("85%", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Vertical divider
                Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color(0x1A000000)))

                // Streak
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Streak", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("$streak Days", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun CapsuleProgressIndicator(label: String, progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0EEFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .align(Alignment.BottomCenter)
                    .background(BrandPurple)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextLight,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Current Module Card Component ───────────────────────────────────────────
@Composable
fun HomeCurrentModuleCard(
    module: CurrentModule,
    activeSublesson: CurrentSublesson?,
    onContinue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContinue() }
            .shadow(6.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented circular progress ring with play icon in center
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.5.dp.toPx()
                    val numSegments = 6
                    val sweepAngle = 360f / numSegments - 12f
                    for (i in 0 until numSegments) {
                        val startAngle = i * (360f / numSegments)
                        drawArc(
                            color = if (i < 4) BrandPurple else Color(0xFFDDDAFF),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CURRENT MODULE",
                    color = BrandPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    module.title,
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    activeSublesson?.let { "Lesson ${it.order}: ${it.title}" } ?: module.description,
                    color = TextMid,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Action arrow button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HomeNoModuleCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗺️", fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Complete the assessment to start your path!",
                color = TextMid,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Quick Actions Feature Card (Vocab Duel / AI Lab) ────────────────────────
@Composable
fun HomeFeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                title,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = textColor.copy(alpha = 0.70f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─── Timely Duel Card Component ───────────────────────────────────────────────
@Composable
fun TimelyDuelCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val amberGold  = Color(0xFFFFCA28)
    val amberDeep  = Color(0xFFFF8F00)
    val darkBrown  = Color(0xFF3E2000)

    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(amberGold, amberDeep)))
            .clickable { onClick() }
    ) {
        // Crossed-swords Canvas decoration (top-right background)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = 4f
            val alpha = 0.18f
            val cx = size.width - 40f
            val cy = 40f
            val len = 70f
            val swordColor = androidx.compose.ui.graphics.Color(0xFF000000.toInt()).copy(alpha = alpha)
            // Sword 1: top-left to bottom-right
            drawLine(swordColor, start = androidx.compose.ui.geometry.Offset(cx - len * 0.6f, cy - len * 0.6f),
                end = androidx.compose.ui.geometry.Offset(cx + len * 0.4f, cy + len * 0.4f), strokeWidth = sw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
            // Sword 2: top-right to bottom-left
            drawLine(swordColor, start = androidx.compose.ui.geometry.Offset(cx + len * 0.4f, cy - len * 0.6f),
                end = androidx.compose.ui.geometry.Offset(cx - len * 0.6f, cy + len * 0.4f), strokeWidth = sw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
            // Hilt cross for sword 1
            val midX1 = (cx - len * 0.6f + cx + len * 0.4f) / 2f
            val midY1 = (cy - len * 0.6f + cy + len * 0.4f) / 2f
            drawLine(swordColor, start = androidx.compose.ui.geometry.Offset(midX1 - 14f, midY1 - 14f),
                end = androidx.compose.ui.geometry.Offset(midX1 + 14f, midY1 + 14f), strokeWidth = sw * 1.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }

        // Card content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Icon: stacked sword + clock
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, contentDescription = null,
                    tint = darkBrown, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("Timely Duel", color = darkBrown, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text("BATTLE AGAINST TIME", color = darkBrown.copy(alpha = 0.65f),
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
    }
}

// ─── Vocab Builder Card Component ────────────────────────────────────────────
@Composable
fun HomeVocabBuilderCard(onClick: () -> Unit = {}) {
    val isLoaded = VocabTracker.isLoaded
    val progressPercent = if (isLoaded) VocabTracker.getOverallProgressPercent() else 45
    val wordsCountText = if (isLoaded) VocabTracker.getOverallWordsCountText() else "225 / 500 words"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Vocab Builder",
                            color = TextDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = wordsCountText.uppercase(),
                            color = TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(BrandPurpleSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${progressPercent}% MASTERED",
                        color = BrandPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = BrandPurple,
                trackColor = BrandPurpleSoft
            )
        }
    }
}

// ─── Mistake Vault Card Component ────────────────────────────────────────────
@Composable
fun HomeMistakeVaultCard(mistakes: List<Mistake>, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFECEC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = BrandRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Mistake Vault",
                        color = TextDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Review recent pronunciation slips",
                        color = TextLight,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Activity / Speaking Stats Chart Card Component ──────────────────────────
@Composable
fun HomeSpeakingStats() {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val heights = listOf(0.30f, 0.50f, 0.20f, 0.65f, 1.0f, 0.40f, 0.15f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weekly Overview", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Your speaking performance", color = TextLight, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("+12%", color = BrandGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { idx, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((60 * heights[idx]).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (idx == 4) BrandPurple else Color(0xFFDDDAFF)
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            day,
                            color = if (idx == 4) BrandPurple else TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0x0D000000))
            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MISTAKES CORRECTED", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("42", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text("+5", color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("LESSONS DONE", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("12", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text("Avg. pace", color = BrandPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────
@Composable
fun HomeBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFF))
            .border(
                width = 0.5.dp,
                color = Color(0x1A000000)
            )
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNavItem("HOME", Icons.Default.Home, selectedTab == 0) { onTabSelected(0) }
            HomeNavItem("AI LAB", Icons.Default.Science, selectedTab == 1) { onTabSelected(1) }
            HomeNavItem("VOCAB", Icons.Default.Book, selectedTab == 2) { onTabSelected(2) }
            HomeNavItem("VAULT", Icons.Default.VerifiedUser, selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun HomeNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFCA28)) // Yellow capsule pill highlight
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = TextDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = TextLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (isSelected) TextDark else TextLight,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
        )
    }
}

// ─── Legacy kept composables (backward compat) ────────────────────────────────
@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("See All", color = BrandPurple, fontSize = 12.sp, modifier = Modifier.clickable { })
    }
}

@Composable
fun ProgressStatRow(label: String, percentage: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextMid, fontSize = 12.sp)
        }
        Text(percentage, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) = HomeNavItem(label, icon, isSelected, onClick)
