package com.mk.lingocoach.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mk.lingocoach.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// ─── Models ──────────────────────────────────────────────────────────────────
enum class MessageRole { AI, USER }
data class Mistake(val wrong: String, val correct: String, val explanation: String, val mistakeType: String)
data class ChatMessage(val id: String, val role: MessageRole, val text: String, val isTyping: Boolean = false, val mistakes: List<Mistake> = emptyList())
enum class AILabStep { HOME, VOICE_SELECTION, TONE_SELECTION, CHAT }

// ─── AILabScreen ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AILabScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToVocab: () -> Unit,
    onNavigateToMistakes: () -> Unit
) {
    var currentStep          by remember { mutableStateOf(AILabStep.HOME) }
    var selectedVoice        by remember { mutableStateOf("Female") }
    var selectedTone         by remember { mutableStateOf("Casual") }
    var showEndDialog        by remember { mutableStateOf(false) }
    var sessionId            by remember { mutableStateOf<String?>(null) }
    var openingMessage       by remember { mutableStateOf("") }
    var endSessionSummary    by remember { mutableStateOf<com.mk.lingocoach.network.AILabEndSessionResponse?>(null) }
    var aiLabStatus          by remember { mutableStateOf<com.mk.lingocoach.network.AILabStatusResponse?>(null) }

    // Real user ID — the session UUID assigned during assessment
    val context  = LocalContext.current
    val userId   = remember {
        context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
            .getString("session_id", "") ?: ""
    }

    // Fetch daily session status on load
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.mk.lingocoach.network.AILabApi.getStatus(userId) { status ->
                    aiLabStatus = status
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter      = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier     = Modifier.fillMaxSize()
        )
        Scaffold(
            topBar = {
                when (currentStep) {
                    AILabStep.CHAT -> {
                        // Chat screen top bar — back on LEFT
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick  = { currentStep = AILabStep.HOME },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint     = TextDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                "LingoCoach AI",
                                color      = TextDark,
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier   = Modifier.weight(1f),
                                textAlign  = TextAlign.Center
                            )
                            IconButton(modifier = Modifier.size(36.dp), onClick = {}) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings",
                                    tint = TextDark, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    else -> {
                        // HOME / VOICE_SELECTION / TONE_SELECTION — back on LEFT, title centred
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    when (currentStep) {
                                        AILabStep.HOME             -> onNavigateBack()
                                        AILabStep.VOICE_SELECTION  -> currentStep = AILabStep.HOME
                                        AILabStep.TONE_SELECTION   -> currentStep = AILabStep.VOICE_SELECTION
                                        else                       -> {}
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint     = TextDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = when (currentStep) {
                                    AILabStep.HOME            -> "AI Conversation"
                                    AILabStep.VOICE_SELECTION -> "Choose a Voice"
                                    AILabStep.TONE_SELECTION  -> "Choose Personality"
                                    else                      -> ""
                                },
                                color      = TextDark,
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier   = Modifier.weight(1f).padding(start = 4.dp)
                            )
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
                            1 -> { /* stay */ }
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
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } +
                         fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } +
                         fadeOut(tween(300)))
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        AILabStep.HOME ->
                            HomeStep(
                                aiLabStatus = aiLabStatus,
                                onStart = { currentStep = AILabStep.VOICE_SELECTION }
                            )
                        AILabStep.VOICE_SELECTION ->
                            VoiceSelectionStep(
                                selectedVoice    = selectedVoice,
                                onVoiceSelected  = { selectedVoice = it },
                                onNext           = { currentStep = AILabStep.TONE_SELECTION }
                            )
                        AILabStep.TONE_SELECTION ->
                            ToneSelectionStep(
                                selectedTone    = selectedTone,
                                onToneSelected  = { selectedTone = it },
                                onStartSession  = {
                                    com.mk.lingocoach.network.AILabApi.startSession(
                                        userId      = userId,
                                        topic       = "General conversation",
                                        voiceGender = selectedVoice.lowercase(),
                                        tone        = selectedTone.lowercase()
                                    ) { response ->
                                        if (response != null) {
                                            sessionId      = response.session_id
                                            openingMessage = response.opening_message
                                            currentStep    = AILabStep.CHAT
                                        }
                                    }
                                }
                            )
                        AILabStep.CHAT ->
                            ChatStep(
                                userId         = userId,
                                sessionId      = sessionId,
                                openingMessage = openingMessage,
                                onEndSession   = { showEndDialog = true }
                            )
                    }
                }

                if (showEndDialog) {
                    AlertDialog(
                        onDismissRequest = { showEndDialog = false },
                        title = {
                            Text(if (endSessionSummary != null) "Session Summary" else "End Session?")
                        },
                        text = {
                            if (endSessionSummary != null) {
                                Column {
                                    Text("Vocabulary Learned: ${endSessionSummary!!.vocabulary_learned}")
                                    Text("Grammar Mistakes: ${endSessionSummary!!.grammar_mistakes}")
                                    Spacer(Modifier.height(8.dp))
                                    Text("Strengths: ${endSessionSummary!!.strengths}", fontWeight = FontWeight.Bold)
                                    Text("Weaknesses: ${endSessionSummary!!.weaknesses}", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Your conversation will be summarized and saved. Are you sure you want to end?")
                            }
                        },
                        shape            = RoundedCornerShape(28.dp),
                        containerColor   = CardWhite,
                        confirmButton    = {
                            if (endSessionSummary == null) {
                                Button(
                                    onClick = {
                                        if (sessionId != null) {
                                            com.mk.lingocoach.network.AILabApi.endSession(sessionId!!) { summary ->
                                                if (summary != null) endSessionSummary = summary
                                                else { showEndDialog = false; onNavigateBack() }
                                            }
                                        } else { showEndDialog = false; onNavigateBack() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                                ) { Text("End Session", color = Color.White) }
                            } else {
                                Button(
                                    onClick = { showEndDialog = false; onNavigateBack() },
                                    colors  = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                                ) { Text("Close", color = Color.White) }
                            }
                        },
                        dismissButton = {
                            if (endSessionSummary == null) {
                                TextButton(onClick = { showEndDialog = false }) {
                                    Text("Cancel", color = BrandPurple)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── Home Step ───────────────────────────────────────────────────────────────
@Composable
fun HomeStep(
    aiLabStatus: com.mk.lingocoach.network.AILabStatusResponse? = null,
    onStart: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "btn")

    val isLimited = aiLabStatus != null && aiLabStatus.sessions_remaining <= 0

    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Talk about anything you want.\nPractice naturally with your AI tutor.",
            color = TextMid, fontSize = 15.sp, textAlign = TextAlign.Center)

        // Daily usage counter
        if (aiLabStatus != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isLimited) Color(0xFFFFEBEE) else BrandPurpleSoft,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isLimited)
                        "Daily limit reached (${aiLabStatus.sessions_used_today}/${aiLabStatus.sessions_limit})"
                    else
                        "Sessions today: ${aiLabStatus.sessions_used_today}/${aiLabStatus.sessions_limit}",
                    color = if (isLimited) BrandRed else BrandPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isLimited && aiLabStatus.bonus_sessions == 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Earn 100 XP today to unlock a free bonus session",
                    color = TextLight, fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        if (!isLimited) {
            Button(
                onClick  = onStart,
                shape    = RoundedCornerShape(32.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandPurple, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                modifier = Modifier
                    .height(56.dp).width(240.dp)
                    .scale(pulseScale * buttonScale)
                    .shadow(6.dp, RoundedCornerShape(32.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                            onTap   = { onStart() }
                        )
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Start Session", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        } else {
            // Locked state
            Box(
                modifier = Modifier
                    .height(56.dp).width(240.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = TextLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Locked for today", color = TextLight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Voice Selection Step ─────────────────────────────────────────────────────
@Composable
fun VoiceSelectionStep(selectedVoice: String, onVoiceSelected: (String) -> Unit, onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select the voice your AI tutor will speak with.",
                color = TextMid, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            listOf("Male Voice" to "Natural masculine voice.", "Female Voice" to "Natural feminine voice.").forEach { (title, desc) ->
                val isSelected    = selectedVoice.contains(title.split(" ")[0])
                val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, label = "scale")
                Card(
                    modifier = Modifier.fillMaxWidth().height(130.dp).padding(vertical = 8.dp)
                        .scale(scale).shadow(6.dp, RoundedCornerShape(24.dp))
                        .clickable { onVoiceSelected(title.split(" ")[0]) },
                    shape  = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) BrandPurpleSoft else CardWhite)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White else BrandPurpleSoft),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(desc.uppercase(), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Button(
            onClick  = onNext,
            shape    = RoundedCornerShape(32.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BrandPurple, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(0.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter).padding(24.dp)
                .fillMaxWidth().height(56.dp).shadow(6.dp, RoundedCornerShape(32.dp))
        ) { Text("Continue", fontWeight = FontWeight.Bold) }
    }
}

// ─── Tone Selection Step ──────────────────────────────────────────────────────
@Composable
fun ToneSelectionStep(selectedTone: String, onToneSelected: (String) -> Unit, onStartSession: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("How should your AI tutor talk?", color = TextMid, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            listOf(
                "Casual"       to "Friendly and relaxed conversation style.",
                "Professional" to "Formal and structured responses.",
                "Nerdy"        to "Analytical and educational style.",
                "Warm"         to "Encouraging and supportive style."
            ).forEachIndexed { index, (title, desc) ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index * 100L); visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { 50 }
                ) {
                    val isSelected = selectedTone == title
                    val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, label = "scale")
                    Card(
                        modifier = Modifier.fillMaxWidth().height(130.dp).padding(vertical = 8.dp)
                            .scale(scale).shadow(6.dp, RoundedCornerShape(24.dp))
                            .clickable { onToneSelected(title) },
                        shape  = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) BrandPurpleSoft else CardWhite)
                    ) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White else BrandPurpleSoft),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Face, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(desc.uppercase(), color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Button(
            onClick  = { isLoading = true; onStartSession() },
            shape    = RoundedCornerShape(32.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BrandPurple, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(0.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter).padding(24.dp)
                .fillMaxWidth().height(56.dp).shadow(6.dp, RoundedCornerShape(32.dp)),
            enabled  = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Start Conversation", fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Chat Step ────────────────────────────────────────────────────────────────
@Composable
fun ChatStep(userId: String, sessionId: String?, openingMessage: String = "", onEndSession: () -> Unit) {
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()
    var messages       by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText      by remember { mutableStateOf("") }
    var isListening    by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    val listState      = rememberLazyListState()

    // Show opening message as first bubble immediately
    LaunchedEffect(sessionId) {
        if (openingMessage.isNotBlank() && messages.isEmpty()) {
            messages = listOf(
                ChatMessage(
                    id   = "opening",
                    role = MessageRole.AI,
                    text = openingMessage
                )
            )
        }
    }

    // MediaRecorder state
    var recorder       by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile      by remember { mutableStateOf<File?>(null) }

    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }

    fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startRecording() {
        if (!hasAudioPermission()) { permLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        try {
            val file = File(context.cacheDir, "ailab_voice_${System.currentTimeMillis()}.mp4")
            audioFile = file
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(context)
            else
                @Suppress("DEPRECATION") MediaRecorder()
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder  = rec
            isListening = true
        } catch (e: Exception) {
            Log.e("AILab", "Recording failed", e)
            Toast.makeText(context, "Could not start recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecordingAndSend() {
        try { recorder?.stop(); recorder?.release() } catch (e: Exception) { Log.e("AILab", "Stop failed", e) }
        recorder    = null
        isListening = false
        val file    = audioFile ?: return
        if (!file.exists() || file.length() == 0L) return
        if (sessionId == null) return

        isTranscribing = true
        val typingId = (System.currentTimeMillis() + 1).toString()
        messages = messages + ChatMessage(id = typingId, role = MessageRole.AI, text = "", isTyping = true)

        com.mk.lingocoach.network.AILabApi.submitChat(
            userId    = userId,
            sessionId = sessionId,
            message   = null,
            audioFile = file
        ) { response ->
            messages       = messages.filter { it.id != typingId }
            isTranscribing = false
            if (response != null) {
                val uiMistakes = response.mistakes.map { m ->
                    Mistake(m.wrong, m.correct, m.explanation, m.mistake_type)
                }
                messages = messages + ChatMessage(
                    id       = System.currentTimeMillis().toString(),
                    role     = MessageRole.AI,
                    text     = response.ai_response,
                    mistakes = uiMistakes
                )
            } else {
                messages = messages + ChatMessage(
                    id   = System.currentTimeMillis().toString(),
                    role = MessageRole.AI,
                    text = "Sorry, I couldn't process your voice message."
                )
            }
            scope.launch { file.delete() }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // imePadding makes the whole column resize above the keyboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7FB))
            .imePadding()
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier        = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TypewriterText("Start Talking With Your AI Tutor", modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(
                modifier       = Modifier.weight(1f).fillMaxWidth(),
                state          = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(messages) { _, message -> ChatBubble(message) }
            }
        }

        ChatInputArea(
            inputText      = inputText,
            onInputChange  = { inputText = it },
            isListening    = isListening,
            isTranscribing = isTranscribing,
            onSend         = {
                if (inputText.isNotBlank() && sessionId != null) {
                    val msg = inputText
                    messages  = messages + ChatMessage(System.currentTimeMillis().toString(), MessageRole.USER, msg)
                    inputText = ""
                    val tid   = (System.currentTimeMillis() + 1).toString()
                    messages  = messages + ChatMessage(tid, MessageRole.AI, "", isTyping = true)
                    com.mk.lingocoach.network.AILabApi.submitChat(
                        userId    = userId,
                        sessionId = sessionId,
                        message   = msg,
                        audioFile = null
                    ) { response ->
                        messages = messages.filter { it.id != tid }
                        messages = messages + if (response != null) {
                            ChatMessage(
                                id       = System.currentTimeMillis().toString(),
                                role     = MessageRole.AI,
                                text     = response.ai_response,
                                mistakes = response.mistakes.map { m -> Mistake(m.wrong, m.correct, m.explanation, m.mistake_type) }
                            )
                        } else {
                            ChatMessage(System.currentTimeMillis().toString(), MessageRole.AI, "Sorry, I'm having trouble connecting right now.")
                        }
                    }
                }
            },
            onMicToggle          = {
                if (isListening) stopRecordingAndSend() else startRecording()
            },
            onEndSessionClick    = onEndSession
        )
    }
}

// ─── Typewriter text ─────────────────────────────────────────────────────────
@Composable
fun TypewriterText(text: String, modifier: Modifier = Modifier) {
    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        text.forEachIndexed { index, _ -> delay(40); displayedText = text.substring(0, index + 1) }
    }
    Text(displayedText, fontSize = 18.sp, fontWeight = FontWeight.Medium,
        color = TextLight, textAlign = TextAlign.Center, modifier = modifier)
}

// ─── Chat Bubble ──────────────────────────────────────────────────────────────
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val bubbleShape = if (isUser)
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 280.dp)
                .shadow(2.dp, bubbleShape)
                .background(if (isUser) BrandPurple else Color.White, bubbleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (message.isTyping) TypingIndicator()
            else Text(message.text, color = if (isUser) Color.White else TextDark, fontSize = 15.sp, lineHeight = 22.sp)
        }

        if (isUser && message.mistakes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            message.mistakes.forEach { mistake ->
                var expanded by remember { mutableStateOf(false) }
                val isGrammar = mistake.mistakeType.lowercase().contains("grammar") || mistake.mistakeType.lowercase().contains("correct")
                Column(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGrammar) Color(0xFFFFECEC) else Color(0xFFE8F5E9))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isGrammar) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            tint     = if (isGrammar) BrandRed else BrandGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (isGrammar) "Correction: \"${mistake.correct}\"" else "Perfect! \"${mistake.correct}\"",
                            color      = if (isGrammar) BrandRed else BrandGreen,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (expanded && mistake.explanation.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(mistake.explanation, color = TextMid, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

// ─── Typing Indicator ─────────────────────────────────────────────────────────
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        repeat(3) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    tween(300, delayMillis = index * 120, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ), label = "dot$index"
            )
            Box(modifier = Modifier.size(7.dp).offset(y = offsetY.dp).background(BrandPurple.copy(alpha = 0.7f), CircleShape))
        }
    }
}

// ─── Chat Input Area ──────────────────────────────────────────────────────────
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

    // navigationBarsPadding keeps the bar above the system nav on devices without gesture nav
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Listening / transcribing card ────────────────────────────────
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
                Box(
                    modifier         = Modifier.size(44.dp).background(BrandPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic",
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(bottom = 4.dp)
                    ) {
                        listOf(14f, 22f, 18f, 28f, 16f, 24f, 12f).forEachIndexed { i, base ->
                            val animH by infiniteTransition.animateFloat(
                                initialValue = base * 0.5f, targetValue = base,
                                animationSpec = infiniteRepeatable(
                                    tween(250 + i * 60, easing = FastOutSlowInEasing), RepeatMode.Reverse
                                ), label = "bar$i"
                            )
                            Box(modifier = Modifier.width(3.dp).height(animH.dp)
                                .clip(RoundedCornerShape(2.dp)).background(BrandPurple))
                        }
                    }
                    Text(
                        if (isTranscribing) "Processing..." else "Listening… tap mic to stop",
                        color = BrandPurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Text input ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = TextLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (inputText.isEmpty()) Text("Type a response...", color = TextLight, fontSize = 15.sp)
                BasicTextField(
                    value          = inputText,
                    onValueChange  = onInputChange,
                    textStyle      = TextStyle(color = TextDark, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier       = Modifier.fillMaxWidth(),
                    singleLine     = false,
                    maxLines       = 4
                )
            }
            if (inputText.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier         = Modifier.size(32.dp).background(BrandPurple, CircleShape).clickable { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Send",
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ── Action row ────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val micBg by animateColorAsState(
                if (isListening) Color(0xFFFFECEC) else Color(0xFFF5F5F5), label = "micBg"
            )
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(micBg).clickable { onMicToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop recording" else "Start recording",
                    tint     = if (isListening) BrandRed else TextLight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = "Hint",
                    tint = TextLight, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEEEEE))
                    .clickable { onEndSessionClick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("End Session", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
