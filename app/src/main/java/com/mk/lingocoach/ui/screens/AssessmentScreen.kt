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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
import com.google.gson.Gson
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.AssessmentResponse
import com.mk.lingocoach.network.LearningPathRequest
import com.mk.lingocoach.network.LearningPathResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Assessment Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AssessmentScreen(
    onNavigateToLearningPath: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    var sessionId by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(1) }
    var currentQuestion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isGeneratingPath by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var textAnswer by remember { mutableStateOf("") }
    var finalResponse by remember { mutableStateOf<AssessmentResponse?>(null) }

    // Start in text mode if permission already denied
    var isTextMode by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
        )
    }

    val localQuestions = remember {
        mapOf(
            1 to "What is your name, where are you from, and what is your favorite hobby?",
            2 to "Tell me about a typical day for you. What is the first thing you do in the morning, and how do you usually get to work or school?",
            3 to "Think about your last birthday or a recent holiday. What did you do, who were you with, and what was the best part of that day?",
            4 to "If a close friend came to visit your city for just one day, where would you take them, and why would you choose those specific places?",
            5 to "Some people think everyone should learn a second language, while others feel it is not necessary anymore because of translation technology. What is your view on this?"
        )
    }

    // Permission launcher — denied → silently switch to text mode
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isTextMode = false
        } else {
            isTextMode = true
            Toast.makeText(context, "Mic unavailable — using text input", Toast.LENGTH_SHORT).show()
        }
    }

    // API response handler
    fun handleResponse(response: AssessmentResponse?) {
        coroutineScope.launch(Dispatchers.Main) {
            isSubmitting = false
            if (response == null) {
                Toast.makeText(context, "Submission failed. Please try again.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (response.assessment_complete) {
                finalResponse = response
            } else {
                currentStep = response.current_step
                currentQuestion = response.next_question
                    ?: localQuestions[response.current_step] ?: ""
                textAnswer = ""
            }
        }
    }

    fun startRecording() {
        try {
            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordedFile = file
            isRecording = true
        } catch (e: Exception) {
            Log.e("Assessment", "Recording failed", e)
            isTextMode = true
            Toast.makeText(context, "Mic unavailable — using text input", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording(onDone: (File?) -> Unit) {
        try {
            mediaRecorder?.apply { stop(); release() }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        onDone(recordedFile)
    }

    // Init session — pass the display name entered in UserProfileSetupScreen
    LaunchedEffect(Unit) {
        val userName = sharedPrefs.getString("display_name", "") ?: ""
        AssessmentApi.createSession(userName) { response ->
            coroutineScope.launch(Dispatchers.Main) {
                if (response != null) {
                    sessionId = response.session_id
                    currentStep = response.current_step
                    currentQuestion = response.next_question.ifBlank { localQuestions[1] ?: "" }
                    // If backend echoes name back, ensure it's saved locally
                    if (response.user_name.isNotBlank()) {
                        sharedPrefs.edit().putString("display_name", response.user_name).apply()
                    }
                    isLoading = false
                } else {
                    errorMessage = "Failed to start session. Check your connection."
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()

        when {
            isLoading -> LoadingView()

            isGeneratingPath -> GeneratingPathView()

            errorMessage.isNotEmpty() -> ErrorView(message = errorMessage) {
                errorMessage = ""
                isLoading = true
                val userName = sharedPrefs.getString("display_name", "") ?: ""
                AssessmentApi.createSession(userName) { response ->
                    coroutineScope.launch(Dispatchers.Main) {
                        if (response != null) {
                            sessionId = response.session_id
                            currentStep = response.current_step
                            currentQuestion = response.next_question.ifBlank { localQuestions[1] ?: "" }
                            isLoading = false
                        } else {
                            errorMessage = "Failed to start session. Please try again."
                            isLoading = false
                        }
                    }
                }
            }

            finalResponse != null -> AssessmentResultView(
                response = finalResponse!!,
                onContinue = {
                    isGeneratingPath = true
                    val request = LearningPathRequest(
                        session_id = finalResponse!!.session_id,
                        user_name  = sharedPrefs.getString("display_name", "") ?: "",
                        tier       = finalResponse!!.assigned_tier ?: "B2 Upper-Intermediate",
                        grammar_score    = finalResponse!!.grammar_score.toInt(),
                        vocabulary_score = finalResponse!!.vocabulary_score.toInt(),
                        coherence_score  = finalResponse!!.coherence_score.toInt(),
                        structural_break = finalResponse!!.structural_break,
                        detected_strength   = finalResponse!!.detected_strength ?: "",
                        detected_weakness   = finalResponse!!.detected_weakness ?: "",
                        recommended_focus   = finalResponse!!.recommended_focus ?: "",
                        user_goal            = sharedPrefs.getString("user_goal", "general") ?: "general",
                        user_level_self_reported = sharedPrefs.getString("user_level", "intermediate") ?: "intermediate"
                    )
                    AssessmentApi.getLearningPath(request) { pathResponse ->
                        coroutineScope.launch(Dispatchers.Main) {
                            isGeneratingPath = false
                            if (pathResponse != null) {
                                val gson = Gson()
                                sharedPrefs.edit()
                                    .putString("assessment_response_json", gson.toJson(finalResponse))
                                    .putString("learning_path_json", gson.toJson(pathResponse))
                                    .putString("session_id", finalResponse!!.session_id)
                                    .putBoolean("assessment_completed", true)
                                    .apply()
                                onNavigateToLearningPath()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to generate learning path. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onBack = onNavigateBack
            )

            else -> AssessmentQuestionView(
                currentStep = currentStep,
                questionText = currentQuestion,
                isRecording = isRecording,
                isSubmitting = isSubmitting,
                isTextMode = isTextMode,
                textAnswer = textAnswer,
                onTextChange = { textAnswer = it },
                onBack = onNavigateBack,
                onMicClick = {
                    if (isRecording) {
                        stopRecording { file ->
                            if (file != null) {
                                isSubmitting = true
                                AssessmentApi.submitVoiceAnswer(sessionId, file) { r ->
                                    handleResponse(r)
                                    try { file.delete() } catch (_: Exception) {}
                                }
                            }
                        }
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) startRecording()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSwitchToText = { isTextMode = true },
                onSwitchToVoice = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onSubmitText = {
                    val trimmed = textAnswer.trim()
                    if (trimmed.length < 5) {
                        Toast.makeText(context, "Please write a little more.", Toast.LENGTH_SHORT).show()
                    } else {
                        isSubmitting = true
                        AssessmentApi.submitTextAnswer(sessionId, trimmed) { r -> handleResponse(r) }
                    }
                }
            )
        }

        // Analysing overlay
        if (isSubmitting && finalResponse == null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6A5CFF), strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Analysing your response…", fontWeight = FontWeight.Bold, color = Color(0xFF1D1D1F), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text("This takes a few seconds", color = Color(0xFF6E6E73), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single Question Screen — voice mic OR text input at the bottom, same top UI
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AssessmentQuestionView(
    currentStep: Int,
    questionText: String,
    isRecording: Boolean,
    isSubmitting: Boolean,
    isTextMode: Boolean,
    textAnswer: String,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onMicClick: () -> Unit,
    onSwitchToText: () -> Unit,
    onSwitchToVoice: () -> Unit,
    onSubmitText: () -> Unit
) {
    val progress = currentStep / 5f
    val completePct = currentStep * 20
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0x12000000)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF1D1D1F), modifier = Modifier.size(20.dp))
            }
            Text(
                "Speaking Assessment",
                style = TextStyle(color = Color(0xFF1D1D1F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0EFFF)).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("$currentStep / 5", color = Color(0xFF6A5CFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // ── Progress ──────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question $currentStep of 5", color = Color(0xFF6E6E73), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("$completePct% Complete", color = Color(0xFF6A5CFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF6A5CFF), trackColor = Color(0xFFE2E2E6), strokeCap = StrokeCap.Round
        )

        Spacer(Modifier.height(28.dp))

        // ── Question card ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(20.dp), false, spotColor = Color(0x1A6A5CFF)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, null, tint = Color(0xFFFFC83D), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ASSESSMENT QUESTION", color = Color(0xFFFFC83D), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "\"$questionText\"",
                    style = TextStyle(color = Color(0xFF1D1D1F), fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Hint card ─────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F3FF)),
            border = BorderStroke(1.dp, Color(0xFFE0DFFF))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF8A7CFF), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Answer in 2–4 sentences. There are no wrong answers. Speak naturally at your own pace.",
                    color = Color(0xFF6A5CFF), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // ── BOTTOM SECTION: Voice vs Text ─────────────────────────────────────
        if (!isTextMode) {
            // Waveform
            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                if (isRecording) {
                    val transition = rememberInfiniteTransition(label = "wave")
                    val phase by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = (2 * Math.PI).toFloat(),
                        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
                        label = "phase"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(15) { index ->
                            val amp = sin(phase + index * 0.4f).absoluteValue
                            Box(
                                modifier = Modifier.width(4.dp).height((10 + 40 * amp).dp)
                                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFF6A5CFF))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Mic button + switch link
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(84.dp)
                        .shadow(16.dp, CircleShape, false, spotColor = Color(0xFF6A5CFF))
                        .clip(CircleShape)
                        .background(if (isRecording) Color(0xFFE53935) else Color(0xFF6A5CFF))
                        .clickable { onMicClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isRecording) "Recording… Tap to Stop" else "Tap to Record",
                    color = Color(0xFF1D1D1F), fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .clickable { onSwitchToText() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.MicOff, null, tint = Color(0xFF6A5CFF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use Text Instead", color = Color(0xFF6A5CFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.weight(0.8f))

            // Estimated time card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x12000000))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color(0xFFFFC83D).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFFFFC83D), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ESTIMATED TIME", color = Color(0xFF6E6E73), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("45–60 seconds", color = Color(0xFF1D1D1F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(Icons.Default.Info, null, tint = Color(0xFF8E8D9F), modifier = Modifier.size(18.dp))
                }
            }

        } else {
            // ── TEXT INPUT (replaces mic section, same position) ──────────────
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        spotColor = Color(0x0D6A5CFF)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F6FF)),
                border = BorderStroke(1.dp, Color(0xFFE0DFFF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, Color(0xFFE2E2E6)), RoundedCornerShape(20.dp))
                            .padding(bottom = 12.dp, end = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = onTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 36.dp),
                            placeholder = { Text("Type your response here...", color = Color(0xFF8E8D9F), fontSize = 15.sp) },
                            minLines = 4,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color(0xFF1D1D1F),
                                unfocusedTextColor = Color(0xFF1D1D1F)
                            ),
                            textStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
                        )

                        val canSend = textAnswer.trim().length >= 5
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (canSend) Color(0xFF6A5CFF) else Color(0xFFD2D2D7))
                                .clickable(enabled = canSend) {
                                    keyboardController?.hide()
                                    onSubmitText()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Submit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSwitchToVoice() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Mic, null, tint = Color(0xFF6A5CFF), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Use Voice Instead", color = Color(0xFF6A5CFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.weight(0.8f))

            // Estimated time card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x12000000))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color(0xFFFFC83D).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFFFFC83D), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ESTIMATED TIME", color = Color(0xFF6E6E73), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("45–60 seconds", color = Color(0xFF1D1D1F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(Icons.Default.Info, null, tint = Color(0xFF8E8D9F), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCCFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF6A5CFF))
            Spacer(Modifier.height(16.dp))
            Text(
                "Initialising Speaking Assessment…",
                fontWeight = FontWeight.Bold, color = Color(0xFF1D1D1F),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun GeneratingPathView() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xE6FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF6A5CFF))
            Spacer(Modifier.height(20.dp))
            Text(
                "Analyzing assessment metrics...",
                fontWeight = FontWeight.Bold, color = Color(0xFF1D1D1F), fontSize = 17.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Generating your personalized learning path",
                color = Color(0xFF6E6E73), fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCCFFFFFF)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = Color(0xFF1D1D1F), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF))) {
                Text("Retry")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AssessmentResultView(
    response: AssessmentResponse,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    // ── Derived scores ──────────────────────────────────────────────────────
    val grammarScore       = response.grammar_score.coerceAtLeast(1f)
    val vocabScore         = response.vocabulary_score.coerceAtLeast(1f)
    val coherenceScore     = response.coherence_score.coerceAtLeast(1f)
    val fluencyScore       = ((grammarScore + coherenceScore) / 2f).coerceIn(1f, 100f)
    val pronunciationScore = ((vocabScore + coherenceScore) / 2f).coerceIn(1f, 100f)
    val proficiencyScore   = ((grammarScore + vocabScore + coherenceScore) / 3f).coerceIn(1f, 100f)
    val assignedTier       = response.assigned_tier ?: "B2 Upper-Intermediate"

    val (gradeEmoji, gradeTitle, gradeSubtitle) = when {
        proficiencyScore >= 85 -> Triple("🎉", "Excellent performance!", "You demonstrated strong language skills with great clarity and confidence.")
        proficiencyScore >= 70 -> Triple("👍", "Good performance!", "You have solid conversational skills with room to refine vocabulary and fluency.")
        proficiencyScore >= 55 -> Triple("📈", "Keep it up!", "You're building a good foundation. Focus on grammar and coherence to improve.")
        else                   -> Triple("💪", "Keep practicing!", "Good start! Regular practice will help you improve quickly.")
    }

    val completedAt = remember {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }

    // Two lowest dimensions → Areas to Improve
    val dimensionScores = remember(response) {
        listOf(
            Triple("Grammar",       grammarScore,       Color(0xFF4CAF50)),
            Triple("Vocabulary",    vocabScore,         Color(0xFFFF9800)),
            Triple("Pronunciation", pronunciationScore, Color(0xFFFF9800)),
            Triple("Fluency",       fluencyScore,       Color(0xFFFF9800)),
            Triple("Coherence",     coherenceScore,     Color(0xFF2196F3))
        ).sortedBy { it.second }.take(2)
    }

    val improvementTips = mapOf(
        "Grammar"       to "Practice verb tenses and sentence structure daily.",
        "Vocabulary"    to "Use a wider range of words in professional and technical contexts.",
        "Pronunciation" to "Work on clearer word pronunciation and stress patterns.",
        "Fluency"       to "Speak for 2 minutes non-stop each day to build natural flow.",
        "Coherence"     to "Use linking words (however, therefore) to connect your ideas."
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(Color(0xFFF0EEFF)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = Color(0xFF6A5CFF), modifier = Modifier.size(20.dp))
            }
            Text("Assessment Result",
                style = TextStyle(color = Color(0xFF1D1D1F), fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF0EEFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color(0xFF6A5CFF), modifier = Modifier.size(18.dp))
            }
        }

        // ── Scrollable body ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .background(Color(0xFFF5F4FF))
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Score gauge card
            Card(
                modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            val sw = 12.dp.toPx()
                            drawArc(Color(0xFFECEAFF), 135f, 270f, false, style = Stroke(sw, cap = StrokeCap.Round))
                            drawArc(Color(0xFF6A5CFF), 135f, 270f * (proficiencyScore / 100f), false,
                                style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${proficiencyScore.toInt()}", color = Color(0xFF1D1D1F),
                                fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                                style = TextStyle(letterSpacing = (-2).sp))
                            Text("/ 100", color = Color(0xFF8E8E93), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF6A5CFF)).padding(horizontal = 20.dp, vertical = 7.dp)) {
                        Text(assignedTier, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("$gradeEmoji $gradeTitle", color = Color(0xFF1D1D1F),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(gradeSubtitle, color = Color(0xFF6E6E73), fontSize = 13.sp,
                        textAlign = TextAlign.Center, lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 8.dp))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Completed on $completedAt", color = Color(0xFF8E8E93), fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Strengths row
            ResultSectionHeader(Icons.Default.Star, Color(0xFFFF9800), "Strengths")
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StrengthScoreCard("Grammar",   grammarScore,   Color(0xFF4CAF50), "G", Modifier.weight(1f))
                StrengthScoreCard("Coherence", coherenceScore, Color(0xFF2196F3), "C", Modifier.weight(1f))
                StrengthScoreCard("Fluency",   fluencyScore,   Color(0xFFFF9800), "~", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Performance Overview — radar + legend
            ResultSectionHeader(Icons.Default.BarChart, Color(0xFF6A5CFF), "Performance Overview")
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    RadarChart(
                        grammar      = grammarScore / 100f,
                        vocabulary   = vocabScore / 100f,
                        coherence    = coherenceScore / 100f,
                        fluency      = fluencyScore / 100f,
                        pronunciation = pronunciationScore / 100f,
                        modifier     = Modifier.size(150.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadarLegendRow("Grammar",       grammarScore,       Color(0xFF4CAF50))
                        RadarLegendRow("Vocabulary",    vocabScore,         Color(0xFFFF9800))
                        RadarLegendRow("Pronunciation", pronunciationScore, Color(0xFFFF9800))
                        RadarLegendRow("Fluency",       fluencyScore,       Color(0xFFFF9800))
                        RadarLegendRow("Coherence",     coherenceScore,     Color(0xFF2196F3))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Areas to Improve
            ResultSectionHeader(Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF2196F3), "Areas to Improve")
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                dimensionScores.forEach { (name, score, color) ->
                    ImprovementCard(name, score, color, improvementTips[name] ?: "", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Recommended Actions
            ResultSectionHeader(Icons.Default.CheckCircle, Color(0xFF6A5CFF), "Recommended Actions")
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF0EEFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF6A5CFF), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("Complete", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(response.recommended_focus ?: "Build vocabulary for your target area",
                            color = Color(0xFF1D1D1F), fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Estimated Time: 15 min/day", color = Color(0xFF8E8E93), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onContinue,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Start Now →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Continue to Lessons CTA
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .shadow(6.dp, RoundedCornerShape(28.dp), clip = false),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Continue to Lessons",
                    style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result UI helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color(0xFF1D1D1F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StrengthScoreCard(
    name: String,
    score: Float,
    color: Color,
    letter: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(letter, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(name, color = Color(0xFF1D1D1F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${score.toInt()}/100", color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(score / 100f).fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp)).background(color)
                )
            }
        }
    }
}

@Composable
private fun RadarLegendRow(name: String, score: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(name, color = Color(0xFF1D1D1F), fontSize = 11.sp)
        }
        Text("${score.toInt()}/100",
            color = Color(0xFF1D1D1F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ImprovementCard(
    name: String,
    score: Float,
    color: Color,
    tip: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (name == "Vocabulary" || name == "Grammar")
                            Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Mic,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(name, color = Color(0xFF1D1D1F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${score.toInt()}/100", color = color,
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(tip, color = Color(0xFF6E6E73), fontSize = 11.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, null,
                    tint = Color(0xFFFF9800), modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("Tip: $tip", color = Color(0xFFFF9800), fontSize = 10.sp, lineHeight = 14.sp,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feedback Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FeedbackCard(
    title: String,
    text: String,
    containerColor: Color,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color(0xFF1D1D1F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    color = Color(0xFF48484A),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Radar Chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RadarChart(
    grammar: Float,
    fluency: Float,
    coherence: Float,
    pronunciation: Float,
    vocabulary: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(size.width, size.height) / 2.8f
        
        val labels = listOf("Grammar", "Vocab", "Fluency", "Pronunciation", "Coherence")
        val scores = listOf(grammar, vocabulary, fluency, pronunciation, coherence)

        // 1. Grid: Concentric pentagons (dashed)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (grid in 1..5) {
            val r = maxR * (grid / 5f)
            val path = androidx.compose.ui.graphics.Path()
            for (i in 0 until 5) {
                val angle = i * 2 * Math.PI / 5 - Math.PI / 2
                val x = cx + r * cos(angle).toFloat()
                val y = cy + r * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path,
                color = Color(0xFFD2D2D7),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
            )
        }

        // 2. Grid: Radial lines (dashed)
        for (i in 0 until 5) {
            val angle = i * 2 * Math.PI / 5 - Math.PI / 2
            drawLine(
                color = Color(0xFFD2D2D7),
                start = Offset(cx, cy),
                end = Offset(cx + maxR * cos(angle).toFloat(), cy + maxR * sin(angle).toFloat()),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // 3. Draw clean labels
        for (i in 0 until 5) {
            val angle = i * 2 * Math.PI / 5 - Math.PI / 2
            val xOffset = cos(angle).toFloat()
            val yOffset = sin(angle).toFloat()
            
            val labelRadius = maxR + 14.dp.toPx()
            val tx = cx + labelRadius * xOffset
            val ty = cy + labelRadius * yOffset
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#8E8E93")
                textSize = 28f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                textAlign = when {
                    xOffset > 0.1f -> android.graphics.Paint.Align.LEFT
                    xOffset < -0.1f -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
            }

            val labelText = labels[i]
            
            val yAdjust = when {
                yOffset > 0.1f -> 12f
                yOffset < -0.1f -> -4f
                else -> 4f
            }
            
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                tx,
                ty + yAdjust,
                textPaint
            )
        }

        // 4. Draw rounded filled polygon
        val scorePath = androidx.compose.ui.graphics.Path()
        for (i in 0 until 5) {
            val r = maxR * scores[i].coerceIn(0.1f, 1f)
            val angle = i * 2 * Math.PI / 5 - Math.PI / 2
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) scorePath.moveTo(x, y) else scorePath.lineTo(x, y)
        }
        scorePath.close()

        // Smooth rounded fill via native canvas
        val fillPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1F6A5CFF") // 12% opacity purple
            style = android.graphics.Paint.Style.FILL
            pathEffect = android.graphics.CornerPathEffect(12.dp.toPx())
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawPath(scorePath.asAndroidPath(), fillPaint)

        // Smooth rounded border stroke
        val strokeEffect = PathEffect.cornerPathEffect(12.dp.toPx())
        drawPath(
            scorePath,
            color = Color(0xFF6A5CFF),
            style = Stroke(width = 3.dp.toPx(), pathEffect = strokeEffect)
        )
    }
}

