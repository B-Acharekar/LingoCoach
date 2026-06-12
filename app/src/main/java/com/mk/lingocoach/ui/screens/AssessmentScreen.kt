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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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

    // Init session
    LaunchedEffect(Unit) {
        AssessmentApi.createSession { response ->
            coroutineScope.launch(Dispatchers.Main) {
                if (response != null) {
                    sessionId = response.session_id
                    currentStep = response.current_step
                    currentQuestion = response.next_question.ifBlank { localQuestions[1] ?: "" }
                    isLoading = false
                } else {
                    errorMessage = "Failed to start session. Check your connection."
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when {
            isLoading -> LoadingView()

            isGeneratingPath -> GeneratingPathView()

            errorMessage.isNotEmpty() -> ErrorView(message = errorMessage) {
                errorMessage = ""
                isLoading = true
                AssessmentApi.createSession { response ->
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
                        tier = finalResponse!!.assigned_tier ?: "B2 Upper-Intermediate",
                        grammar_score = finalResponse!!.grammar_score.toInt(),
                        vocabulary_score = finalResponse!!.vocabulary_score.toInt(),
                        coherence_score = finalResponse!!.coherence_score.toInt(),
                        structural_break = finalResponse!!.structural_break,
                        detected_strength = finalResponse!!.detected_strength ?: "",
                        detected_weakness = finalResponse!!.detected_weakness ?: "",
                        recommended_focus = finalResponse!!.recommended_focus ?: ""
                    )
                    AssessmentApi.getLearningPath(request) { pathResponse ->
                        coroutineScope.launch(Dispatchers.Main) {
                            isGeneratingPath = false
                            if (pathResponse != null) {
                                val gson = Gson()
                                sharedPrefs.edit()
                                    .putString("assessment_response_json", gson.toJson(finalResponse))
                                    .putString("learning_path_json", gson.toJson(pathResponse))
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
    val proficiencyScore = remember(response) {
        val avg = (response.grammar_score + response.vocabulary_score + response.coherence_score) / 3f
        if (avg <= 0f) 72f else avg
    }
    val assignedTier = response.assigned_tier ?: "B2 Upper-Intermediate"

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0x0A000000)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF1D1D1F), modifier = Modifier.size(20.dp))
            }
            Text("Speaking Assessment", style = TextStyle(color = Color(0xFF1D1D1F), fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF6A5CFF))
                    .clickable { onContinue() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Next", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Score circle (270-degree Arc Sweep Gauge)
            Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val strokeWidthPx = 10.dp.toPx()
                    // Draw track arc
                    drawArc(
                        color = Color(0xFFE5E5ED),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                    // Draw progress arc
                    drawArc(
                        color = Color(0xFF6A5CFF),
                        startAngle = 135f,
                        sweepAngle = 270f * (proficiencyScore / 100f),
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text = "${proficiencyScore.toInt()}", 
                        color = Color(0xFF1D1D1F), 
                        fontSize = 44.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(letterSpacing = (-1).sp)
                    )
                    Text(
                        text = "/ 100", 
                        color = Color(0xFF8E8E93), 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Proficiency Level", 
                        color = Color(0xFF6A5CFF), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF6A5CFF))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = assignedTier, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    proficiencyScore >= 85 -> "Excellent clarity with a few awkward word choices."
                    proficiencyScore >= 70 -> "Good conversational flow with moderate vocabulary."
                    else -> "Basic phrasing. Continuous practice is recommended."
                },
                color = Color(0xFF6E6E73), fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Performance Metrics", color = Color(0xFF1D1D1F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                // options/info placeholder icon
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF6A5CFF).copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            RadarChart(
                grammar = (response.grammar_score.coerceAtLeast(1f) / 100f),
                vocabulary = (response.vocabulary_score.coerceAtLeast(1f) / 100f),
                coherence = (response.coherence_score.coerceAtLeast(1f) / 100f),
                fluency = ((response.grammar_score + response.coherence_score) / 200f).coerceIn(0.1f, 1f),
                pronunciation = ((response.vocabulary_score + response.coherence_score) / 200f).coerceIn(0.1f, 1f),
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp))
            )

            Spacer(Modifier.height(28.dp))
            Text("Analysis & Feedback", color = Color(0xFF1D1D1F), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            FeedbackCard(
                title = "Detected Strength",
                text = response.detected_strength ?: "Strong structural breakdown of complex ideas and consistent grammar.",
                containerColor = Color(0xFFF0F9F4),
                iconColor = Color(0xFF34C759),
                icon = Icons.Default.CheckCircle
            )
            Spacer(Modifier.height(12.dp))
            FeedbackCard(
                title = "Detected Weakness",
                text = response.detected_weakness ?: "Repetitive word choice when discussing professional topics.",
                containerColor = Color(0xFFFFF9F0),
                iconColor = Color(0xFFFF9500),
                icon = Icons.Default.Warning
            )
            Spacer(Modifier.height(12.dp))
            FeedbackCard(
                title = "Recommended Focus",
                text = response.recommended_focus ?: "Deepen vocabulary usage for business and technical contexts.",
                containerColor = Color(0xFFF5F3FF),
                iconColor = Color(0xFF5856D6),
                icon = Icons.Default.Star
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(27.dp), clip = false),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5CFF)),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = "Continue to Lessons", 
                    style = TextStyle(
                        color = Color.White, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(Modifier.height(24.dp))
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

