package com.mk.lingocoach.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mk.lingocoach.R
import com.mk.lingocoach.network.AssessmentApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// â”€â”€â”€ Design tokens â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private val SetupPurple      = Color(0xFF6A5CFF)
private val SetupPurpleSoft  = Color(0xFFF0EEFF)
private val SetupPurpleDark  = Color(0xFF4A3FCC)
private val SetupTextDark    = Color(0xFF0D0D0D)
private val SetupTextMid     = Color(0xFF3A3A3A)
private val SetupTextLight   = Color(0xFF6B6B6B)
private val SetupCardBg      = Color(0xFFFFFFFF)
private val SetupAmber       = Color(0xFFFFB800)
private val SetupCardBorder  = Color(0xFFEEECFF)

// â”€â”€â”€ Data models â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private data class GoalOption(val id: String, val title: String, val subtitle: String, val icon: ImageVector)
private data class LevelOption(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

private val goalOptions = listOf(
    GoalOption("job_interview", "Job Interviews",         "Speak confidently in interviews",      Icons.Default.Work),
    GoalOption("business",      "Business Communication", "Improve workplace conversations",       Icons.Default.BusinessCenter),
    GoalOption("study_abroad",  "Study Abroad",           "Communicate while studying overseas",   Icons.Default.School),
    GoalOption("travel",        "Travel",                 "Speak easily while travelling",         Icons.Default.Flight),
    GoalOption("daily_life",    "Daily Life",             "Handle everyday situations with ease",  Icons.Default.Home),
    GoalOption("general",       "General Improvement",    "Build fluency across all areas",        Icons.AutoMirrored.Filled.TrendingUp)
)

private val levelOptions = listOf(
    LevelOption("beginner",     "Beginner",      "I'm just getting started",      Icons.Default.Person),
    LevelOption("intermediate", "Intermediate",  "I can have basic conversations", Icons.Default.Mic),
    LevelOption("advanced",     "Advanced",      "I can speak comfortably",        Icons.Default.RecordVoiceOver)
)

private fun normalizeUsernameInput(value: String): String =
    value.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(20)

private fun usernameValidationError(username: String): String? {
    val value = username.trim()
    return when {
        value.length < 3 -> "Username must be at least 3 characters."
        value.length > 20 -> "Username must be 20 characters or less."
        !value.first().isLetter() -> "Username must start with a letter."
        value.any { !(it.isLowerCase() || it.isDigit() || it == '_') } -> "Use lowercase letters, numbers, or underscores only."
        "__" in value -> "Username cannot contain consecutive underscores."
        value.endsWith("_") -> "Username cannot end with an underscore."
        else -> null
    }
}

// â”€â”€â”€ Main Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun UserProfileSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    onExistingUserRestored: () -> Unit = onSetupComplete
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)

    // 3 steps: 1=Goal, 2=Level, 3=Speaking Assessment Intro
    var step         by remember { mutableStateOf(1) }
    val totalSteps    = 3

    var displayName   by remember { mutableStateOf(prefs.getString("display_name", "") ?: "") }
    var username      by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
    var isReturningUserMode by remember { mutableStateOf(false) }
    var existingUserError by remember { mutableStateOf<String?>(null) }
    var isExistingUserLoading by remember { mutableStateOf(false) }
    var usernameAvailabilityError by remember { mutableStateOf<String?>(null) }
    var isUsernameCheckLoading by remember { mutableStateOf(false) }
    val selectedGoals = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (prefs.getString("user_goal", "") ?: "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            )
        }
    }
    var selectedLevel by remember { mutableStateOf(prefs.getString("user_level",   "") ?: "") }

    fun goBack() {
        if (step == 1) onNavigateBack() else step--
    }

    fun saveAndProceed() {
        prefs.edit()
            .putString("user_goal",    selectedGoals.joinToString(","))
            .putString("user_level",   selectedLevel)
            .putBoolean("personalization_done", true)
            .apply()
        onSetupComplete()
    }

    fun restoreExistingUser() {
        val validationError = usernameValidationError(username)
        if (validationError != null) {
            existingUserError = validationError
            return
        }

        existingUserError = null
        isExistingUserLoading = true
        AssessmentApi.findUserByUsername(username) { user ->
            coroutineScope.launch(Dispatchers.Main) {
                isExistingUserLoading = false
                if (user == null) {
                    existingUserError = "No account found for this username."
                    return@launch
                }

                prefs.edit()
                    .putString("session_id", user.session_id)
                    .putString("username", user.username)
                    .putString("display_name", user.user_name)
                    .putBoolean("personalization_done", true)
                    .putBoolean("assessment_completed", true)
                    .apply()

                onExistingUserRestored()
            }
        }
    }

    fun verifyNewUsernameAndContinue() {
        val validationError = usernameValidationError(username)
        if (displayName.trim().isBlank()) {
            usernameAvailabilityError = null
            return
        }
        if (validationError != null) {
            usernameAvailabilityError = validationError
            return
        }

        usernameAvailabilityError = null
        isUsernameCheckLoading = true
        AssessmentApi.checkUsernameAvailability(username) { availability ->
            coroutineScope.launch(Dispatchers.Main) {
                isUsernameCheckLoading = false
                when {
                    availability == null -> usernameAvailabilityError = "Could not verify username. Please try again."
                    !availability.available -> usernameAvailabilityError = "This username is already taken."
                    else -> step++
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()

        // â”€â”€ Content â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            SetupTopBar(step = step, totalSteps = totalSteps, onBack = ::goBack)

            // Step content
            when (step) {
                1 -> StepGoal(
                    selectedGoals  = selectedGoals,
                    onGoalToggled = { goalId ->
                        if (selectedGoals.contains(goalId)) {
                            selectedGoals.remove(goalId)
                        } else {
                            selectedGoals.add(goalId)
                        }
                    },
                    onContinue    = { if (selectedGoals.isNotEmpty()) step++ }
                )
                2 -> StepLevel(
                    selectedLevel  = selectedLevel,
                    onLevelSelected = { selectedLevel = it },
                    onContinue     = { if (selectedLevel.isNotBlank()) step++ }
                )
                3 -> StepSpeakingIntro(onStartAssessment = ::saveAndProceed)
            }
        }
    }
}

// â”€â”€â”€ Top Progress Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun SetupTopBar(step: Int, totalSteps: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back circle button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SetupPurpleSoft)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SetupPurple,
                modifier = Modifier.size(17.dp)
            )
        }

        // Segmented progress bar (fills left to right)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(totalSteps) { idx ->
                val segColor by animateColorAsState(
                    targetValue = when {
                        idx < step - 1 -> SetupPurple          // completed
                        idx == step - 1 -> SetupAmber           // current (amber dot)
                        else            -> Color(0xFFDDDCF0)    // upcoming
                    },
                    animationSpec = tween(300),
                    label = "seg$idx"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segColor)
                )
            }
        }

        // n/total label
        Text(
            "$step/$totalSteps",
            color = SetupTextLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// â”€â”€â”€ Step 1 : Name â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun StepName(
    displayName: String,
    onNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    isReturningUserMode: Boolean,
    existingUserError: String?,
    isExistingUserLoading: Boolean,
    usernameAvailabilityError: String?,
    isUsernameCheckLoading: Boolean,
    onAlreadyUser: () -> Unit,
    onBackToNewUser: () -> Unit,
    onVerifyExistingUser: () -> Unit,
    onContinue: () -> Unit
) {
    val keyboard      = LocalSoftwareKeyboardController.current
    val usernameError = usernameValidationError(username)
    val canContinue   = displayName.trim().isNotBlank() && usernameError == null && !isUsernameCheckLoading
    val canVerifyExisting = usernameError == null && !isExistingUserLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (isReturningUserMode) "Enter your username" else "What's your name?",
                    style = TextStyle(color = SetupTextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = if (isReturningUserMode) onBackToNewUser else onAlreadyUser,
                    enabled = !isExistingUserLoading && !isUsernameCheckLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (isExistingUserLoading || isUsernameCheckLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = SetupPurple
                        )
                    } else {
                        Text(
                            if (isReturningUserMode) "New user?" else "Already a user?",
                            color = SetupPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (isReturningUserMode) {
                    "Verify your username and we'll restore your existing LingoCoach session."
                } else {
                    "Tell us what to call you and choose a unique username."
                },
                style = TextStyle(color = SetupTextMid, fontSize = 15.sp, lineHeight = 22.sp)
            )
            Spacer(Modifier.height(32.dp))

            if (!isReturningUserMode) {
                OutlinedTextField(
                    value          = displayName,
                    onValueChange  = onNameChange,
                    label          = { Text("Full name") },
                    placeholder    = { Text("e.g. Alex Mercer", color = SetupTextLight, fontSize = 16.sp) },
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape          = RoundedCornerShape(16.dp),
                    textStyle      = TextStyle(fontSize = 17.sp, color = SetupTextDark, fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = SetupPurple,
                        unfocusedBorderColor    = Color(0xFFDDDCF0),
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor        = SetupTextDark,
                        unfocusedTextColor      = SetupTextDark,
                        cursorColor             = SetupPurple
                    )
                )

                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value          = username,
                onValueChange  = onUsernameChange,
                label          = { Text(if (isReturningUserMode) "Username" else "How should we remember you?") },
                placeholder    = { Text("alex_mercer", color = SetupTextLight, fontSize = 16.sp) },
                leadingIcon    = {
                    Text("@", color = SetupPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                singleLine     = true,
                isError        = (username.isNotBlank() && usernameError != null) ||
                    existingUserError != null ||
                    usernameAvailabilityError != null,
                supportingText = {
                    val helperText = if (isReturningUserMode) {
                        existingUserError ?: usernameError ?: "Use your saved LingoCoach username."
                    } else {
                        usernameAvailabilityError
                            ?: usernameError
                            ?: "3-20 chars, starts with a letter. Use lowercase, numbers, underscores."
                    }
                    val hasError = usernameError != null || existingUserError != null || usernameAvailabilityError != null
                    Text(
                        text = helperText,
                        color = if (hasError) Color(0xFFD64545) else SetupTextLight,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                },
                modifier       = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                shape          = RoundedCornerShape(16.dp),
                textStyle      = TextStyle(fontSize = 17.sp, color = SetupTextDark, fontWeight = FontWeight.Medium),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction      = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        if (isReturningUserMode && canVerifyExisting) {
                            onVerifyExistingUser()
                        } else if (!isReturningUserMode && canContinue) {
                            onContinue()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = SetupPurple,
                    unfocusedBorderColor    = Color(0xFFDDDCF0),
                    errorBorderColor        = Color(0xFFD64545),
                    focusedContainerColor   = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor     = Color.White,
                    focusedTextColor        = SetupTextDark,
                    unfocusedTextColor      = SetupTextDark,
                    errorTextColor          = SetupTextDark,
                    cursorColor             = SetupPurple
                )
            )
        }

        SetupContinueButton(
            enabled = if (isReturningUserMode) canVerifyExisting else canContinue,
            label   = when {
                isReturningUserMode && isExistingUserLoading -> "Verifying..."
                isReturningUserMode -> "Verify & Continue"
                isUsernameCheckLoading -> "Checking..."
                else -> "Continue"
            },
            onClick = {
                keyboard?.hide()
                if (isReturningUserMode && canVerifyExisting) {
                    onVerifyExistingUser()
                } else if (!isReturningUserMode && canContinue) {
                    onContinue()
                }
            }
        )
    }
}

// â”€â”€â”€ Step 2 : Goal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun StepGoal(
    selectedGoals: List<String>,
    onGoalToggled: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(28.dp))
            Text(
                "What is your goal?",
                style = TextStyle(color = SetupTextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose one or more goals so we can personalise your learning journey.",
                style = TextStyle(color = SetupTextMid, fontSize = 15.sp, lineHeight = 22.sp)
            )
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                goalOptions.forEach { option ->
                    SetupSelectionCard(
                        title    = option.title,
                        subtitle = option.subtitle,
                        icon     = option.icon,
                        selected = selectedGoals.contains(option.id),
                        onClick  = { onGoalToggled(option.id) }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        SetupContinueButton(
            enabled = selectedGoals.isNotEmpty(),
            label   = "Continue",
            onClick = onContinue
        )
    }
}

// â”€â”€â”€ Step 3 : Level â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun StepLevel(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(Modifier.height(28.dp))
            Text(
                "What is your current level?",
                style = TextStyle(color = SetupTextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This will help us assess you better.",
                style = TextStyle(color = SetupTextMid, fontSize = 15.sp, lineHeight = 22.sp)
            )
            Spacer(Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                levelOptions.forEach { option ->
                    SetupSelectionCard(
                        title    = option.title,
                        subtitle = option.subtitle,
                        icon     = option.icon,
                        selected = selectedLevel == option.id,
                        onClick  = { onLevelSelected(option.id) }
                    )
                }
            }
        }

        SetupContinueButton(
            enabled = selectedLevel.isNotBlank(),
            label   = "Continue",
            onClick = onContinue
        )
    }
}

// â”€â”€â”€ Step 4 : Speaking Assessment Intro â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun StepSpeakingIntro(onStartAssessment: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // â”€â”€ Clipboard illustration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer soft glow circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(SetupPurpleSoft.copy(alpha = 0.5f))
                )
                // Clipboard card
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = SetupPurple.copy(0.3f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Clipboard top clip
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(8.dp)
                                .offset(y = (-12).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SetupPurple.copy(0.3f))
                        )
                        // Mic icon
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = SetupPurple,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        // Waveform bars under mic
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(6, 12, 8, 16, 10, 14, 7).forEachIndexed { i, h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (i == 3) SetupPurple
                                            else SetupPurple.copy(alpha = 0.35f)
                                        )
                                )
                            }
                        }
                    }
                }
                // Pencil icon bottom-right of clipboard
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-14).dp, y = (-14).dp)
                        .clip(CircleShape)
                        .background(SetupPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Speaking Assessment",
                style = TextStyle(
                    color      = SetupTextDark,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Let's understand your speaking level",
                style = TextStyle(
                    color     = SetupTextMid,
                    fontSize  = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            )

            Spacer(Modifier.height(28.dp))

            // â”€â”€ Feature bullets â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf(
                        "5 short questions",
                        "Takes about 3 minutes",
                        "Get instant AI feedback"
                    ).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SetupPurpleSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SetupPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(item, color = SetupTextMid, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        SetupContinueButton(
            enabled = true,
            label   = "Start Assessment",
            onClick = onStartAssessment
        )
    }
}

// â”€â”€â”€ Shared: Selection card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun SetupSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue   = if (selected) SetupPurple else Color(0xFFE4E2F8),
        animationSpec = tween(220), label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue   = if (selected) SetupPurpleSoft else SetupCardBg,
        animationSpec = tween(220), label = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation  = if (selected) 8.dp else 2.dp,
                shape      = RoundedCornerShape(16.dp),
                spotColor  = SetupPurple.copy(alpha = 0.15f),
                clip       = false
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon chip
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) SetupPurple.copy(alpha = 0.12f)
                    else Color(0xFFF0EEFF)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = SetupPurple, modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title,    color = SetupTextDark,  fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = SetupTextLight, fontSize = 13.sp, lineHeight = 18.sp)
        }

        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(SetupPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// â”€â”€â”€ Shared: Continue button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun SetupContinueButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape  = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor         = SetupPurple,
            disabledContainerColor = Color(0xFFB8B4E8)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Text(
            label,
            style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
    }
    Spacer(Modifier.height(16.dp))
}

