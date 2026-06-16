package com.mk.lingocoach.ui.screens

import android.content.Context
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R

@Composable
fun LearningPathRoadmapScreen(
    onNavigateToLearningPath: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF0EEFF), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BrandPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    "Learning Path",
                    style = TextStyle(
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF0EEFF), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = BrandPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Scrollable Content ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                // ── Title ─────────────────────────────────────────────────
                Text(
                    "Your Journey",
                    style = TextStyle(
                        color = BrandPurple,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Personalized route to fluency",
                    style = TextStyle(
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // ── Roadmap Modules ───────────────────────────────────────
                RoadmapModule(
                    level = "LEVEL 1",
                    title = "Foundations",
                    description = "Master the essentials",
                    icon = Icons.Default.Circle,
                    status = "current"
                )

                RoadmapConnector()

                RoadmapModule(
                    level = "LEVEL 2",
                    title = "Daily Coffee Chat",
                    description = "Everyday expressions",
                    icon = Icons.Default.Circle,
                    status = "locked"
                )

                RoadmapConnector()

                RoadmapModule(
                    level = "LEVEL 3",
                    title = "Professional Pitch",
                    description = "Business communication",
                    icon = Icons.Default.Circle,
                    status = "locked"
                )

                RoadmapConnector()

                RoadmapModule(
                    level = "MASTER",
                    title = "Cultural Mastery",
                    description = "Advanced fluency",
                    icon = Icons.Default.Circle,
                    status = "locked"
                )

                Spacer(Modifier.height(80.dp))
            }

            // ── Bottom Button ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onNavigateToLearningPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text(
                        "Start Learning",
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
            }
        }
    }
}

@Composable
private fun RoadmapModule(
    level: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: String
) {
    val isCurrent = status == "current"
    val isLocked = status == "locked"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isCurrent) 6.dp else 3.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) CardWhite.copy(alpha = 0.6f) else CardWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) BrandPurple.copy(0.15f)
                        else Color(0xFFF0F0F0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrent) BrandPurple else TextLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    level.uppercase(),
                    color = if (isLocked) TextLight else BrandPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    color = if (isLocked) TextLight else TextDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    color = TextLight,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun RoadmapConnector() {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(40.dp)
            .background(Color(0xFFE0E0E0))
    )
}

