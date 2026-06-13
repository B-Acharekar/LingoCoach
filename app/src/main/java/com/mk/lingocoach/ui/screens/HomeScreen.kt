package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.List
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
fun HomeScreen(onNavigateToLesson: (sublessonId: String) -> Unit) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val sharedPrefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scrollState   = rememberScrollState()

    var selectedTab   by remember { mutableStateOf(0) }
    var learningPath  by remember { mutableStateOf<CurrentLearningPathResponse?>(null) }
    var mistakes      by remember { mutableStateOf<List<Mistake>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
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
    val xp     = learningPath?.xp     ?: 0
    val streak = learningPath?.streak ?: 0
    val tier   = learningPath?.tier   ?: "BEGINNER"

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
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Header ──────────────────────────────────────────────────
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
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0x33FFAB00), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 12.sp)
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "$streak",
                                    color = BrandAmberDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        // Settings
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x14000000))
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextMid,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

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

                Spacer(Modifier.height(12.dp))

                // ── Current Module Card ──────────────────────────────────────
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
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

                Spacer(Modifier.height(16.dp))

                // ── Quick Feature Grid ───────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeFeatureCard(
                        modifier = Modifier.weight(1f),
                        topSymbol = "⚔️",
                        title = "Vocab Duel",
                        subtitle = "QUICK PVP MATCH",
                        gradient = Brush.linearGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFFF6D00))
                        ),
                        onClick = {}
                    )
                    HomeFeatureCard(
                        modifier = Modifier.weight(1f),
                        topSymbol = "+",
                        title = "AI Lab",
                        subtitle = "PRONUNCIATION",
                        gradient = Brush.linearGradient(
                            listOf(Color(0xFF5C6BC0), Color(0xFF9575CD))
                        ),
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Vocab Builder ────────────────────────────────────────────
                HomeVocabBuilderCard()

                Spacer(Modifier.height(22.dp))

                Spacer(Modifier.height(16.dp))

                // ── Mistake Vault ────────────────────────────────────────────
                HomeMistakeVaultCard(mistakes = mistakes)

                Spacer(Modifier.height(16.dp))

                // ── Speaking Stats ───────────────────────────────────────────
                HomeSpeakingStats(tier = tier, xp = xp)

                Spacer(Modifier.height(24.dp))
            }

            // ── Bottom Navigation ────────────────────────────────────────────
            HomeBottomNav(
                selectedTab   = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

// ─── Current Module Card ─────────────────────────────────────────────────────
@Composable
fun HomeCurrentModuleCard(
    module: CurrentModule,
    activeSublesson: CurrentSublesson?,
    onContinue: () -> Unit
) {
    val progressAnim by animateFloatAsState(
        targetValue = if (module.status == "current") 0.35f else 1f,
        animationSpec = tween(900),
        label = "moduleProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .clickable { onContinue() }
            .padding(16.dp)
    ) {
        Column {
            // "CURRENT MODULE" badge
            Text(
                "CURRENT MODULE",
                color = BrandPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular play icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF5C6BC0), BrandPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
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
                        activeSublesson?.let { "Lesson ${it.order}: ${it.title}" }
                            ?: module.description.take(45),
                        color = TextMid,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Arrow button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress
            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandPurple,
                trackColor = BrandPurpleSoft,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(progressAnim * 100).toInt()}% complete",
                color = TextLight,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun HomeNoModuleCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(20.dp),
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

// ─── Feature Cards (Vocab Duel / AI Lab) ─────────────────────────────────────
@Composable
fun HomeFeatureCard(
    modifier: Modifier = Modifier,
    topSymbol: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(160.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Symbol in top-left
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    topSymbol,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─── Vocab Builder Card ───────────────────────────────────────────────────────
@Composable
fun HomeVocabBuilderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .clickable { }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandPurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📖", fontSize = 22.sp)
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
                            "Business Context Pack",
                            color = TextMid,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text("Progress", color = TextLight, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))

            LinearProgressIndicator(
                progress = { 0.45f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandPurple,
                trackColor = BrandPurpleSoft,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("225 / 500 words", color = TextLight, fontSize = 11.sp)
                Box(
                    modifier = Modifier
                        .background(BrandPurpleSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "45% MASTERED",
                        color = BrandPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ─── Mistake Vault Card ──────────────────────────────────────────────────────
@Composable
fun HomeMistakeVaultCard(mistakes: List<Mistake>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .clickable { }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFECEC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(24.dp)
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
                            "Pronunciation & Grammar slips",
                            color = TextMid,
                            fontSize = 11.sp
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mistakes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFECEC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${mistakes.size} LOGGED",
                                color = BrandRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (mistakes.isEmpty()) {
                Text(
                    "No mistakes yet! Keep it up. 🎉",
                    color = BrandGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Review lessons to keep your record flawless.",
                    color = TextLight,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    "Review recent pronunciation slips",
                    color = TextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Practice and unlock your corrections",
                    color = BrandRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Speaking Stats Card ─────────────────────────────────────────────────────
@Composable
fun HomeSpeakingStats(tier: String, xp: Int) {
    val days    = listOf("M", "T", "W", "T", "F", "S", "S")
    val heights = listOf(0.30f, 0.50f, 0.20f, 0.65f, 1.0f, 0.40f, 0.0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Speaking Stats", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Activity this week", color = TextLight, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("+12%", color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bar chart + mic FAB row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { idx, day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height((50 * heights[idx]).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (idx == 4) BrandPurple else Color(0xFFDDDAFF)
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(day, color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Mic FAB
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BrandPurple, BrandPurpleLight))
                        )
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
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
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNavItem("Home",   Icons.Default.Home,                    selectedTab == 0) { onTabSelected(0) }
            HomeNavItem("In-Talk", Icons.Default.Chat,                   selectedTab == 1) { onTabSelected(1) }
            HomeNavItem("Vocab",  Icons.AutoMirrored.Filled.List,        selectedTab == 2) { onTabSelected(2) }
            HomeNavItem("Vault",  Icons.Default.Lock,                    selectedTab == 3) { onTabSelected(3) }
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) BrandPurple else TextLight,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (isSelected) BrandPurple else TextLight,
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

// ─── Old Bottom Nav (kept for compat) ────────────────────────────────────────
@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) = HomeNavItem(label, icon, isSelected, onClick)
