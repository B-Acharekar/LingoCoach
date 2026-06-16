package com.mk.lingocoach.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.mk.lingocoach.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Models ─────────────────────────────────────────────────────────────────
enum class MessageRole { AI, USER }
data class Mistake(val wrong: String, val correct: String, val explanation: String, val mistakeType: String)
data class ChatMessage(val id: String, val role: MessageRole, val text: String, val isTyping: Boolean = false, val mistakes: List<Mistake> = emptyList())
enum class AILabStep { HOME, VOICE_SELECTION, TONE_SELECTION, CHAT }

// ─── AILabScreen ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AILabScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToVocab: () -> Unit,
    onNavigateToMistakes: () -> Unit
) {
    var currentStep by remember { mutableStateOf(AILabStep.HOME) }
    var selectedVoice by remember { mutableStateOf("Female") }
    var selectedTone by remember { mutableStateOf("Casual") }
    var showEndDialog by remember { mutableStateOf(false) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var endSessionSummary by remember { mutableStateOf<com.mk.lingocoach.network.AILabEndSessionResponse?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Scaffold(
        topBar = {
                // Only show the chat-style top bar in CHAT step; elsewhere show plain title
                if (currentStep == AILabStep.CHAT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentStep = AILabStep.HOME },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back", tint = TextDark,
                                modifier = Modifier.size(20.dp))
                        }
                        Text(
                            "LingoCoach AI",
                            color = TextDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(modifier = Modifier.size(36.dp), onClick = {}) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings",
                                tint = TextDark, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Conversation", color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        IconButton(
                            onClick = {
                                when (currentStep) {
                                    AILabStep.HOME -> onNavigateBack()
                                    AILabStep.VOICE_SELECTION -> currentStep = AILabStep.HOME
                                    AILabStep.TONE_SELECTION -> currentStep = AILabStep.VOICE_SELECTION
                                    AILabStep.CHAT -> currentStep = AILabStep.HOME
                                }
                            },
                            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                                tint = TextDark, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
        bottomBar = {
            HomeBottomNav(
                selectedTab = 1,
                onTabSelected = { index ->
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> {} // Stay
                        2 -> onNavigateToVocab()
                        3 -> onNavigateToMistakes()
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeOut(animationSpec = tween(300))
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    AILabStep.HOME -> HomeStep(onStart = { currentStep = AILabStep.VOICE_SELECTION })
                    AILabStep.VOICE_SELECTION -> VoiceSelectionStep(
                        selectedVoice = selectedVoice,
                        onVoiceSelected = { selectedVoice = it },
                        onNext = { currentStep = AILabStep.TONE_SELECTION }
                    )
                    AILabStep.TONE_SELECTION -> ToneSelectionStep(
                        selectedTone = selectedTone,
                        onToneSelected = { selectedTone = it },
                        onStartSession = {
                            com.mk.lingocoach.network.AILabApi.startSession(
                                userId = "test_user_123",
                                topic = "General conversation",
                                voiceGender = selectedVoice.lowercase(),
                                tone = selectedTone.lowercase()
                            ) { response ->
                                if (response != null) {
                                    sessionId = response.session_id
                                    currentStep = AILabStep.CHAT
                                } else {
                                    // Handle error, e.g. show toast
                                }
                            }
                        }
                    )
                    AILabStep.CHAT -> ChatStep(
                        sessionId = sessionId,
                        onEndSession = { showEndDialog = true }
                    )
                }
            }
            
            if (showEndDialog) {
                AlertDialog(
                    onDismissRequest = { showEndDialog = false },
                    title = { Text(if (endSessionSummary != null) "Session Summary" else "End Session?") },
                    text = {
                        if (endSessionSummary != null) {
                            Column {
                                Text("Vocabulary Learned: ${endSessionSummary!!.vocabulary_learned}")
                                Text("Grammar Mistakes: ${endSessionSummary!!.grammar_mistakes}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Strengths: ${endSessionSummary!!.strengths}", fontWeight = FontWeight.Bold)
                                Text("Weaknesses: ${endSessionSummary!!.weaknesses}", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Your conversation will be summarized and saved. Are you sure you want to end?")
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = CardWhite,
                    confirmButton = {
                        if (endSessionSummary == null) {
                            Button(onClick = {
                                if (sessionId != null) {
                                    com.mk.lingocoach.network.AILabApi.endSession(sessionId!!) { summary ->
                                        if (summary != null) {
                                            endSessionSummary = summary
                                        } else {
                                            showEndDialog = false
                                            onNavigateBack()
                                        }
                                    }
                                } else {
                                    showEndDialog = false
                                    onNavigateBack()
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = BrandRed)) {
                                Text("End Session", color = Color.White)
                            }
                        } else {
                            Button(onClick = { showEndDialog = false; onNavigateBack() }, colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
                                Text("Close", color = Color.White)
                            }
                        }
                    },
                    dismissButton = {
                        if (endSessionSummary == null) {
                            TextButton(onClick = { showEndDialog = false }) { Text("Cancel", color = BrandPurple) }
                        }
                    }
                )
            }
        }
    }
}
}

// ─── Home Step ──────────────────────────────────────────────────────────────
@Composable
fun HomeStep(onStart: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "buttonScale")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "AI Conversation",
            color = TextDark,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Talk about anything you want.\nPractice naturally with your AI tutor.",
            color = TextMid,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPurple,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier.height(56.dp).width(240.dp).scale(pulseScale * buttonScale).shadow(6.dp, RoundedCornerShape(32.dp)).pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onStart() }
                )
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start Session", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// ─── Voice Selection Step ───────────────────────────────────────────────────
@Composable
fun VoiceSelectionStep(selectedVoice: String, onVoiceSelected: (String) -> Unit, onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Choose Voice", color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select the voice your AI tutor will speak with.", color = TextMid, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            val voices = listOf("Male Voice" to "Natural masculine voice.", "Female Voice" to "Natural feminine voice.")
            voices.forEach { (title, desc) ->
                val isSelected = selectedVoice.contains(title.split(" ")[0])
                val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, label = "scale")
                val containerColor = if (isSelected) BrandPurpleSoft else CardWhite

                Card(
                    modifier = Modifier.fillMaxWidth().height(130.dp).padding(vertical = 8.dp).scale(scale).shadow(6.dp, RoundedCornerShape(24.dp)).clickable { onVoiceSelected(title.split(" ")[0]) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color.White else BrandPurpleSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(desc.uppercase(), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedVisibility(visible = true, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(6.dp, RoundedCornerShape(32.dp))
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Tone Selection Step ────────────────────────────────────────────────────
@Composable
fun ToneSelectionStep(selectedTone: String, onToneSelected: (String) -> Unit, onStartSession: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Choose Personality", color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("How should your AI tutor talk?", color = TextMid, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            val tones = listOf(
                "Casual" to "Friendly and relaxed conversation style.",
                "Professional" to "Formal and structured responses.",
                "Nerdy" to "Analytical and educational style.",
                "Warm" to "Encouraging and supportive style."
            )
            
            tones.forEachIndexed { index, pair ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    visible = true
                }
                
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { 50 }
                ) {
                    val (title, desc) = pair
                    val isSelected = selectedTone == title
                    val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, label = "scale")
                    val containerColor = if (isSelected) BrandPurpleSoft else CardWhite

                    Card(
                        modifier = Modifier.fillMaxWidth().height(130.dp).padding(vertical = 8.dp).scale(scale).shadow(6.dp, RoundedCornerShape(24.dp)).clickable { onToneSelected(title) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color.White else BrandPurpleSoft), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Face, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(desc.uppercase(), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        var isLoading by remember { mutableStateOf(false) }

        AnimatedVisibility(visible = true, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
            Button(
                onClick = {
                    isLoading = true
                    onStartSession()
                },
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(6.dp, RoundedCornerShape(32.dp)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Start Conversation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Chat Step ──────────────────────────────────────────────────────────────
@Composable
fun ChatStep(sessionId: String?, onEndSession: () -> Unit) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isAiSpeaking by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7FB))
    ) {
        // ── Message list ──────────────────────────────────────────────────
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                TypewriterText("Start Talking With Your AI Tutor", modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(messages) { _, message ->
                    ChatBubble(message)
                }
            }
        }

        ChatInputArea(
            inputText = inputText,
            onInputChange = { inputText = it },
            isListening = isListening,
            isTranscribing = isTranscribing,
            onSend = {
                if (inputText.isNotBlank() && sessionId != null) {
                    val messageToSend = inputText
                    messages = messages + ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        role = MessageRole.USER,
                        text = messageToSend
                    )
                    inputText = ""
                    val typingId = (System.currentTimeMillis() + 1).toString()
                    messages = messages + ChatMessage(id = typingId, role = MessageRole.AI, text = "", isTyping = true)
                    com.mk.lingocoach.network.AILabApi.submitChat(
                        userId = "test_user_123",
                        sessionId = sessionId,
                        message = messageToSend,
                        audioFile = null
                    ) { response ->
                        messages = messages.filter { it.id != typingId }
                        if (response != null) {
                            val uiMistakes = response.mistakes.map { m ->
                                Mistake(m.wrong, m.correct, m.explanation, m.mistake_type)
                            }
                            messages = messages + ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                role = MessageRole.AI,
                                text = response.ai_response,
                                mistakes = uiMistakes
                            )
                        } else {
                            messages = messages + ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                role = MessageRole.AI,
                                text = "Sorry, I'm having trouble connecting right now."
                            )
                        }
                    }
                }
            },
            onMicToggle = { isListening = !isListening },
            onEndSessionClick = onEndSession
        )
    }
}

@Composable
fun TypewriterText(text: String, modifier: Modifier = Modifier) {
    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        text.forEachIndexed { index, _ ->
            delay(40)
            displayedText = text.substring(0, index + 1)
        }
    }
    Text(
        displayedText,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = TextLight,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    // ── Bubble shape: round all corners, flatten the "tail" corner
    val bubbleShape = if (isUser)
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // ── Main bubble ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 280.dp)
                .shadow(2.dp, bubbleShape)
                .background(
                    if (isUser) BrandPurple else Color.White,
                    bubbleShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (message.isTyping) {
                TypingIndicator()
            } else {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else TextDark,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // ── Mistake correction chips (below user bubble) ─────────────────
        if (isUser && message.mistakes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            message.mistakes.forEach { mistake ->
                var expanded by remember { mutableStateOf(false) }
                val isGrammar = mistake.mistakeType.lowercase().contains("grammar")
                    || mistake.mistakeType.lowercase().contains("correct")

                Column(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGrammar) Color(0xFFFFECEC) else Color(0xFFE8F5E9))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGrammar) {
                            Icon(Icons.Default.Close, contentDescription = null,
                                tint = BrandRed, modifier = Modifier.size(13.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null,
                                tint = BrandGreen, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(5.dp))
                        val label = if (isGrammar)
                            "Correction: \"${mistake.correct}\""
                        else
                            "Perfect! \"${mistake.correct}\""
                        Text(
                            label,
                            color = if (isGrammar) BrandRed else BrandGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (expanded && mistake.explanation.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            mistake.explanation,
                            color = TextMid,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        repeat(3) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, delayMillis = index * 120, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offsetY.dp)
                    .background(BrandPurple.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

// ─── Chat Input Area ────────────────────────────────────────────────────────
// Layout (matching reference image):
//
//  ┌─────────────────────────────────────────────┐
//  │  [mic icon]  Listening...          waveform  │  ← listening card (visible when active)
//  │  Translate: "..."                            │
//  └─────────────────────────────────────────────┘
//  ┌─────────────────────────────────────────────┐
//  │ [⌨]  Type a response...                     │  ← text input row
//  └─────────────────────────────────────────────┘
//   [🎙✕]  [💡]                    [End Session]   ← action row
@Composable
fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onMicToggle: () -> Unit,
    isTranscribing: Boolean,
    onEndSessionClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Listening card ────────────────────────────────────────────────
        if (isListening || isTranscribing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic icon circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BrandPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic",
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Animated waveform bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        val barHeights = listOf(14f, 22f, 18f, 28f, 16f, 24f, 12f)
                        barHeights.forEachIndexed { i, base ->
                            val animH by infiniteTransition.animateFloat(
                                initialValue = base * 0.5f,
                                targetValue = base,
                                animationSpec = infiniteRepeatable(
                                    tween(250 + i * 60, easing = FastOutSlowInEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "bar$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(animH.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(BrandPurple)
                            )
                        }
                    }
                    Text(
                        if (isTranscribing) "Transcribing..." else "Listening...",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (inputText.isNotEmpty()) {
                        Text(
                            "Translate: \"$inputText\"",
                            color = TextLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ── Text input field ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null,
                tint = TextLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (inputText.isEmpty()) {
                    Text("Type a response...", color = TextLight, fontSize = 15.sp)
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(color = TextDark, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4
                )
            }
            if (inputText.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(BrandPurple, CircleShape)
                        .clickable { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Send",
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ── Action row: mic off · hint · end session ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic toggle
            val micBg by animateColorAsState(
                if (isListening) Color(0xFFFFECEC) else Color(0xFFF5F5F5),
                label = "micBg"
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(micBg)
                    .clickable { onMicToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isListening) Icons.Default.MicOff else Icons.Default.MicOff,
                    contentDescription = "Mic toggle",
                    tint = if (isListening) BrandRed else TextLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Hint / lightbulb
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = "Hint",
                    tint = TextLight, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.weight(1f))

            // End Session pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEEEEE))
                    .clickable { onEndSessionClick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "End Session",
                    color = TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

