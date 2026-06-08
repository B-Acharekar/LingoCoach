package com.kazuki.lingocoach.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07051A),
                        Color(0xFF0F0C2F),
                        Color(0xFF1B1748)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp) // Leave space for bottom nav
        ) {
            // 1. Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image placeholder
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF635BFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bhushan",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Progress Badge
                        Box(
                            modifier = Modifier
                                .background(Color(0x24635BFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "72% Advanced",
                                style = TextStyle(
                                    color = Color(0xFF8A79FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x0EFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x0EFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                // 2. Ready to Speak Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF635BFF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Ready to speak?",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Our AI tutor is waiting to help you refine your accent and natural phrasing in real-time.",
                            style = TextStyle(
                                color = Color(0xFFE0DFFF),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Speak",
                                    tint = Color(0xFF635BFF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start AI Conversation",
                                    color = Color(0xFF635BFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // 3. Practice Lessons
                SectionHeader(title = "Practice Lessons")
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LessonItem(title = "Mock Interviews", iconLetter = "💼")
                    LessonItem(title = "IELTS Speaking", iconLetter = "🎓")
                    LessonItem(title = "Daily Lessons", iconLetter = "📅")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Vocab Pack
                SectionHeader(title = "Vocab Pack")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VocabCard(
                        modifier = Modifier.weight(1f),
                        title = "Idioms",
                        sub = "321 Active Words",
                        bgColor = Color(0x0EFFFFFF)
                    )
                    VocabCard(
                        modifier = Modifier.weight(1f),
                        title = "Homophones",
                        sub = "Become master",
                        bgColor = Color(0x0EFFFFFF)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                VocabCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Synonyms",
                    sub = "Study Vocab",
                    bgColor = Color(0x16635BFF),
                    hasButton = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Mistake Vault
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mistake Vault",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Vault",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x0EFFFFFF)
                    ),
                    border = BorderStroke(1.dp, Color(0x15FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(50.dp)
                                .background(Color(0xFFE53935), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "\"I have gone to the store yesterday.\"",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Grammar · Today 02:45pm",
                                style = TextStyle(
                                    color = Color(0xFF8E8D9F),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x06FFFFFF))
                            .clickable { }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Review All Mistakes",
                            color = Color(0xFF8A79FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. My Progress
                SectionHeader(title = "My Progress")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x0EFFFFFF)
                    ),
                    border = BorderStroke(1.dp, Color(0x15FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(70.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 0.72f },
                                    modifier = Modifier.size(70.dp),
                                    color = Color(0xFF8A79FF),
                                    trackColor = Color(0x1AFFFFFF),
                                    strokeWidth = 6.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "72%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                ProgressStatRow(label = "Grammar", percentage = "88%", dotColor = Color(0xFF635BFF))
                                ProgressStatRow(label = "Vocabulary", percentage = "65%", dotColor = Color(0xFF00C853))
                                ProgressStatRow(label = "Fluency", percentage = "78%", dotColor = Color(0xFFFFAB00))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "You spoke for 1.5 hours this week. Great job!",
                            color = Color(0xFF8E8D9F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7. AI-Powered Insights
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF141235)
                    ),
                    border = BorderStroke(1.dp, Color(0x208A79FF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF635BFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Deep Analysis",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Natural Speech Pattern Tuning",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Our AI has identified a specific recurring filler word pattern. Ready to neutralize your 'um's and 'ah's?",
                            style = TextStyle(
                                color = Color(0xFF8E8D9F),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Explore full report →",
                            color = Color(0xFF8A79FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {  }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 8. Daily Challenge
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x0EFFFFFF)
                    ),
                    border = BorderStroke(1.dp, Color(0x15FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🤝",
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Challenge",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFF8E8D9F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete a 5-minute debate session with the AI on 'The Ethics of Urban Spaces'.",
                            color = Color(0xFF8E8D9F),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { 0.45f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF635BFF),
                                trackColor = Color(0x1AFFFFFF),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Difficulty: Hard",
                                color = Color(0xFFFFAB00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 9. Bottom Navigation Bar Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xEE08051E)) // Solid translucent matching theme background
                .border(BorderStroke(0.5.dp, Color(0x15FFFFFF)))
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                BottomNavItem(
                    label = "Vocabulary",
                    icon = Icons.AutoMirrored.Filled.List,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                BottomNavItem(
                    label = "Vault",
                    icon = Icons.Default.Star,
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                BottomNavItem(
                    label = "Analytics",
                    icon = Icons.Default.Info,
                    isSelected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "See All",
            color = Color(0xFF8A79FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {  }
        )
    }
}

@Composable
fun LessonItem(title: String, iconLetter: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x0EFFFFFF)
        ),
        border = BorderStroke(1.dp, Color(0x10FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x12FFFFFF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconLetter,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "➔",
                color = Color(0xFF8E8D9F),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun VocabCard(
    modifier: Modifier = Modifier,
    title: String,
    sub: String,
    bgColor: Color,
    hasButton: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        border = BorderStroke(1.dp, Color(0x10FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sub,
                color = Color(0xFFB0AFFF),
                fontSize = 12.sp
            )
            if (hasButton) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF635BFF)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Study Vocab",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressStatRow(label: String, percentage: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color(0xFF8E8D9F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = percentage,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF8A79FF) else Color(0xFF555469),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF8A79FF) else Color(0xFF555469),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
