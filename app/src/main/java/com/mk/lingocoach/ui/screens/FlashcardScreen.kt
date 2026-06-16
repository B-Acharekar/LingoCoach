package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    
    var flashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isRevealed by remember { mutableStateOf(false) }

    val userId = remember {
        sharedPrefs.getString("session_id", null) ?: "df31075e-bc40-459f-bbfb-e10c2d3ea34e"
    }

    LaunchedEffect(userId) {
        scope.launch(Dispatchers.IO) {
            AssessmentApi.getFlashcards(userId) { cards ->
                scope.launch(Dispatchers.Main) {
                    flashcards = cards ?: emptyList()
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SRS Review", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFF),
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark
                )
            )
        },
        containerColor = Color(0xFFFAFAFF)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPurple)
            }
        } else if (flashcards.isEmpty() || currentIndex >= flashcards.size) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = BrandPurple, modifier = androidx.compose.ui.Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("All caught up!", color = TextDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("You've reviewed all your due flashcards.", color = TextMid, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Back to Home")
                    }
                }
            }
        } else {
            val currentCard = flashcards[currentIndex]
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${flashcards.size - currentIndex} cards remaining",
                    color = TextMid,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Card View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .background(CardWhite, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentCard.front,
                            color = TextDark,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        AnimatedVisibility(visible = isRevealed) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(32.dp))
                                HorizontalDivider(color = CardBorderColor)
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = currentCard.back,
                                    color = BrandPurple,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isRevealed) {
                    Button(
                        onClick = { isRevealed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Show Answer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("How well did you know this?", color = TextMid, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 0 = Blackout, 1 = Hard, 3 = Good, 5 = Perfect
                        RatingButton("Again\n(0)", BrandRed, Modifier.weight(1f)) {
                            submitReview(currentCard.id, 0, scope) {
                                isRevealed = false; currentIndex++
                            }
                            // Log missed flashcard as a mistake to backend (fire-and-forget)
                            scope.launch(Dispatchers.IO) {
                                AssessmentApi.logMistake(
                                    userId          = userId,
                                    word            = currentCard.front.take(60),
                                    mistakeType     = "FLASHCARD_RETEST",
                                    userSentence    = "(flashcard again)",
                                    correctSentence = currentCard.back.take(120),
                                    explanation     = "Missed during SRS flashcard review"
                                )
                            }
                        }
                        RatingButton("Hard\n(2)", BrandAmberDark, Modifier.weight(1f)) { 
                            submitReview(currentCard.id, 2, scope) {
                                isRevealed = false; currentIndex++
                            }
                        }
                        RatingButton("Good\n(4)", BrandPurpleLight, Modifier.weight(1f)) { 
                            submitReview(currentCard.id, 4, scope) {
                                isRevealed = false; currentIndex++
                            }
                        }
                        RatingButton("Easy\n(5)", BrandGreen, Modifier.weight(1f)) { 
                            submitReview(currentCard.id, 5, scope) {
                                isRevealed = false; currentIndex++
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun submitReview(cardId: String, rating: Int, scope: kotlinx.coroutines.CoroutineScope, onNext: () -> Unit) {
    scope.launch(Dispatchers.IO) {
        AssessmentApi.reviewFlashcard(cardId, rating) { success ->
            scope.launch(Dispatchers.Main) {
                onNext()
            }
        }
    }
}

@Composable
fun RatingButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
