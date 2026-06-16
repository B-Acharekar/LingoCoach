package com.mk.lingocoach.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LearningPathScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (sublessonId: String) -> Unit = {}
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scrollState = rememberScrollState()

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: ""
    }

    var learningPath by remember {
        mutableStateOf<CurrentLearningPathResponse?>(AppCache.learningPath)
    }
    var isLoading by remember { mutableStateOf(learningPath == null) }
    var selectedCoach by remember {
        mutableStateOf(sharedPrefs.getString("selected_coach", "Amélie") ?: "Amélie")
    }

    // Load from cache then refresh if stale
    LaunchedEffect(userId) {
        AppCache.loadFromDisk(context)
        val cached = AppCache.learningPath
        if (cached != null) { learningPath = cached; isLoading = false }
        if (AppCache.isLearningPathStale() && userId.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                AssessmentApi.getCurrentLearningPath(userId) { path ->
                    if (path != null) {
                        AppCache.learningPath   = path
                        AppCache.learningPathAt = System.currentTimeMillis()
                        AppCache.saveToDisk(context)
                        scope.launch(Dispatchers.Main) { learningPath = path; isLoading = false }
                    } else {
                        scope.launch(Dispatchers.Main) { isLoading = false }
                    }
                }
            }
        } else { isLoading = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter      = painterResource(R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier     = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Scrollable body (no top bar) ──────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Page Title ────────────────────────────────────────────────
                Text(
                    "Learning Path",
                    style = TextStyle(
                        color = TextDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.height(4.dp))

                // ── Subtitle ──────────────────────────────────────────────────
                Text(
                    "Your Journey",
                    style = TextStyle(
                        color = BrandPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Text(
                    "Personalized route to fluency",
                    style = TextStyle(
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                )

                Spacer(Modifier.height(20.dp))

                if (isLoading) {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandPurple,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (learningPath == null) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Map, null, tint = BrandPurple,
                                modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Complete the assessment to unlock your path",
                                color = TextMid, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    val modules = learningPath?.modules ?: emptyList()

                    // Header stats row
                    val totalLessons = modules.sumOf { it.lessons.size }
                    val doneLessons  = modules.sumOf { m ->
                        m.lessons.count { it.status == "completed" }
                    }
                    LpStatsRow(
                        tier = learningPath?.tier ?: "—",
                        xp   = learningPath?.xp ?: 0,
                        streak = learningPath?.streak ?: 0,
                        lessonsTotal = totalLessons,
                        lessonsDone  = doneLessons
                    )

                    // Module cards
                    modules.forEach { module ->
                        LpModuleCard(
                            module = module,
                            onSublessonTap = { subId -> onNavigateToLesson(subId) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Action Buttons at the end ─────────────────────────────────
                if (learningPath != null) {
                    // Determine the first lesson/sublesson
                    val firstModule = learningPath?.modules?.firstOrNull()
                    val firstLesson = firstModule?.lessons?.firstOrNull()
                    val firstSub = firstLesson?.sublessons?.firstOrNull()

                    // Start Your Journey Button
                    Button(
                        onClick = {
                            if (firstSub != null) {
                                onNavigateToLesson(firstSub.id)
                            } else {
                                Toast.makeText(context, "No lessons available yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        Text(
                            "Start Your Journey",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Go to Home Button
                    OutlinedButton(
                        onClick = onNavigateToHome,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandPurple
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, BrandPurple)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Go to Home",
                            style = TextStyle(
                                color = BrandPurple,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Stats row ────────────────────────────────────────────────────────────────
@Composable
private fun LpStatsRow(
    tier: String, xp: Int, streak: Int,
    lessonsTotal: Int, lessonsDone: Int
) {
    val progress = if (lessonsTotal > 0) lessonsDone / lessonsTotal.toFloat() else 0f
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tier
                Column {
                    Text("LEVEL", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(tier, color = BrandPurple, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                // XP
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("XP", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$xp", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                // Streak
                Column(horizontalAlignment = Alignment.End) {
                    Text("STREAK", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, null, tint = BrandAmberDark,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("$streak days", color = BrandAmberDark,
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$lessonsDone / $lessonsTotal lessons", color = TextMid, fontSize = 12.sp)
                Text("${(progress * 100).toInt()}%", color = BrandPurple,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color      = BrandPurple,
                trackColor = BrandPurpleSoft
            )
        }
    }
}

// ─── Module card ──────────────────────────────────────────────────────────────
@Composable
private fun LpModuleCard(
    module: CurrentModule,
    onSublessonTap: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(module.status == "current") }
    val isCurrent   = module.status == "current"
    val isCompleted = module.status == "completed"
    val isLocked    = module.status == "locked"

    val borderColor = when {
        isCurrent   -> BrandPurple
        isCompleted -> BrandGreen
        else        -> Color(0xFFE0DFFA)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
            .shadow(if (isCurrent) 6.dp else 2.dp, RoundedCornerShape(20.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .animateContentSize(tween(200))
            .clickable { if (!isLocked) expanded = !expanded },
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) CardWhite.copy(alpha = 0.6f) else CardWhite
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Module header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(when {
                            isCompleted -> BrandGreen.copy(0.12f)
                            isCurrent   -> BrandPurple.copy(0.12f)
                            else        -> Color(0xFFF0F0F0)
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isCompleted -> Icons.Default.CheckCircle
                            isCurrent   -> Icons.Default.PlayArrow
                            else        -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = when {
                            isCompleted -> BrandGreen
                            isCurrent   -> BrandPurple
                            else        -> TextLight
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(module.level.uppercase(), color = TextLight,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (isCurrent) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(BrandPurple, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CURRENT", color = Color.White,
                                    fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Text(module.title, color = if (isLocked) TextLight else TextDark,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(module.description, color = TextLight, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = TextLight, modifier = Modifier.size(20.dp)
                )
            }

            // Expanded: lesson list
            AnimatedVisibility(
                visible = expanded && !isLocked,
                enter   = expandVertically(tween(200)),
                exit    = shrinkVertically(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(4.dp))
                    module.lessons.forEach { lesson ->
                        LpLessonRow(
                            title       = lesson.title,
                            description = lesson.description,
                            status      = lesson.status,
                            order       = lesson.order,
                            sublessons  = lesson.sublessons,
                            onTap       = { subId -> onSublessonTap(subId) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Lesson row inside module ─────────────────────────────────────────────────
@Composable
private fun LpLessonRow(
    title: String, description: String, status: String,
    order: Int,
    sublessons: List<com.mk.lingocoach.network.CurrentSublesson>,
    onTap: (String) -> Unit
) {
    val isCurrent   = status == "current"
    val isCompleted = status == "completed"
    val isLocked    = status == "locked"

    val activeSub = sublessons.firstOrNull { it.status == "current" }
        ?: sublessons.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) BrandPurpleSoft else Color.Transparent
            )
            .clickable(enabled = !isLocked) {
                activeSub?.let { onTap(it.id) }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number / status circle
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(when {
                    isCompleted -> BrandGreen
                    isCurrent   -> BrandPurple
                    else        -> Color(0xFFEEEEEE)
                }),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, null, tint = Color.White,
                    modifier = Modifier.size(14.dp))
            } else if (isLocked) {
                Icon(Icons.Default.Lock, null, tint = TextLight,
                    modifier = Modifier.size(12.dp))
            } else {
                Text("$order", color = Color.White,
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,
                color = if (isLocked) TextLight else TextDark,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (description.isNotBlank()) {
                Text(description, color = TextLight, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (isCurrent) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(BrandPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ─── Coach card ───────────────────────────────────────────────────────────────
@Composable
private fun LpCoachCard(
    name: String, description: String, avatarRes: Int,
    isSelected: Boolean, onSelect: () -> Unit, onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.width(170.dp).clickable { onSelect() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 0.5.dp,
            color = if (isSelected) BrandPurple else Color(0xFFEEEEEE)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(avatarRes),
                    contentDescription = name,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(name, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(description, color = TextLight, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth().height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleSoft),
                shape  = RoundedCornerShape(17.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, null,
                    tint = BrandPurple, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Preview", color = BrandPurple,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
