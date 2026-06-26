package com.mk.lingocoach.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mk.lingocoach.R
import com.mk.lingocoach.config.AppConfig
import com.mk.lingocoach.notifications.NotificationScheduler
import androidx.core.content.FileProvider
import com.mk.lingocoach.data.model.appLanguages
import com.mk.lingocoach.data.repository.LanguagePreferencesRepository
import com.mk.lingocoach.data.repository.AppLocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// --- Settings Design Tokens ---------------------------------------------------
private val SettingsBg        = Color(0xFFF5F4FF)
private val SettingsCardBg    = Color(0xFFFFFFFF)
private val SettingsPurple    = Color(0xFF6A5CFF)
private val SettingsPurpleSoft = Color(0xFFF0EEFF)
private val SettingsTextDark  = Color(0xFF0D0D0D)
private val SettingsTextMid   = Color(0xFF3A3A3A)
private val SettingsTextLight = Color(0xFF6B6B6B)
private val SettingsDivider   = Color(0xFFEEEEEE)
private val SettingsSectionLabel = Color(0xFF9E9E9E)
private val SettingsGreen     = Color(0xFF4CAF50)
private val SettingsRed       = Color(0xFFE53935)

// --- Native language list -----------------------------------------------------
// --- AI tutor voice profiles --------------------------------------------------
private val voiceProfiles = listOf(
    "Male - British Accent", "Male - American Accent",
    "Female - British Accent", "Female - American Accent",
    "Female - Australian Accent"
)

private fun normalizeSettingsUsername(value: String): String =
    value.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(20)

private fun csvCell(value: Any?): String {
    val text = value?.toString().orEmpty()
    return "\"${text.replace("\"", "\"\"")}\""
}

private fun settingsUsernameError(username: String): String? {
    val value = username.trim()
    return when {
        value.isBlank() -> "Choose a username to save and restore your progress."
        value.length < 3 -> "Username must be at least 3 characters."
        value.length > 20 -> "Username must be 20 characters or less."
        !value.first().isLetter() -> "Username must start with a letter."
        value.any { !(it.isLowerCase() || it.isDigit() || it == '_') } -> "Use lowercase letters, numbers, or underscores only."
        "__" in value -> "Username cannot contain consecutive underscores."
        value.endsWith("_") -> "Username cannot end with an underscore."
        else -> null
    }
}

// --- Main Settings Screen -----------------------------------------------------
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val languageRepository = remember(context) { LanguagePreferencesRepository(context) }
    val scroll  = rememberScrollState()
    val scope = rememberCoroutineScope()
    val mirroredLanguageCode = context
        .getSharedPreferences("language_preferences_mirror", Context.MODE_PRIVATE)
        .getString("selected_language", "system") ?: "system"

    // -- Persisted state ------------------------------------------------------
    var displayName    by remember { mutableStateOf(prefs.getString("display_name", "Alex Mercer") ?: "Alex Mercer") }
    var username       by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
    var targetFluency  by remember { mutableStateOf(prefs.getString("target_fluency", "Professional / Business") ?: "Professional / Business") }
    var appLanguageCode by remember { mutableStateOf(mirroredLanguageCode) }
    var voiceProfile   by remember { mutableStateOf(prefs.getString("voice_profile", "Male - British Accent") ?: "Male - British Accent") }
    var dailyReminder  by remember { mutableStateOf(prefs.getBoolean("daily_reminder", true)) }
    var offlineCache   by remember { mutableStateOf(prefs.getBoolean("offline_cache", false)) }

    // -- Dialog state ---------------------------------------------------------
    var showNameDialog     by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showFluencyDialog  by remember { mutableStateOf(false) }
    var showVoiceDialog    by remember { mutableStateOf(false) }
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var usernameSaveError by remember { mutableStateOf<String?>(null) }
    var isUsernameSaving by remember { mutableStateOf(false) }
    val currentAppLanguageLabel = localizedSettingsLanguageLabel(appLanguageCode)
    val fluencyLevelOptions = localizedFluencyLevels()

    // -- Helper: persist a boolean ---------------------------------------------
    fun saveBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String)   = prefs.edit().putString(key, value).apply()

    // -- Helper: persist + sync one field to backend ---------------------------
    fun saveAndSync(key: String, value: String) {
        saveStr(key, value)
        val mapping = mapOf(
            "display_name"    to "display_name",
            "username"        to "username",
            "native_language" to "native_language",
            "target_fluency"  to "target_fluency",
            "voice_profile"   to "voice_profile"
        )
        val backendKey = mapping[key] ?: return
        val uid = prefs.getString("session_id", null) ?: return
        com.mk.lingocoach.network.AssessmentApi.patchUserProfile(uid, mapOf(backendKey to value))
    }

    fun saveBoolAndSync(key: String, value: Boolean) {
        saveBool(key, value)
        if (key == "daily_reminder") {
            val uid = prefs.getString("session_id", null) ?: return
            com.mk.lingocoach.network.AssessmentApi.patchUserProfile(uid, mapOf("daily_reminder" to value))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundTexture()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            CommonTopBar(
                title = stringResource(R.string.settings),
                onBack = onNavigateBack,
                onSettings = null
            )
            // -- Scrollable content --------------------------------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .background(SettingsBg)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // -- Profile card ----------------------------------------------
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {

                        // Display Name row
                        SettingsInfoRow(
                            label = stringResource(R.string.name),
                            value = displayName,
                            onClick = { showNameDialog = true }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        SettingsInfoRow(
                            label = stringResource(R.string.username),
                            value = if (username.isBlank()) stringResource(R.string.add_username) else "@$username",
                            onClick = { showUsernameDialog = true }
                        )
                        if (username.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            SettingsProgressReminder(onClick = { showUsernameDialog = true })
                        }
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        // Target Fluency row
                        SettingsInfoRow(
                            label = stringResource(R.string.skill_level),
                            value = localizedFluencyLevel(targetFluency),
                            onClick = { showFluencyDialog = true }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        SettingsInfoRow(
                            label = stringResource(R.string.app_language),
                            value = currentAppLanguageLabel,
                            onClick = onNavigateToLanguage
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionHeader(stringResource(R.string.app_settings).uppercase())
                Spacer(Modifier.height(8.dp))

                // -- App & Learning Preferences card --------------------------
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Daily Reminder toggle
                        SettingsToggleRow(
                            label = stringResource(R.string.daily_reminder),
                            icon = null,
                            checked = dailyReminder,
                            onCheckedChange = { 
                                dailyReminder = it
                                saveBoolAndSync("daily_reminder", it)
                                NotificationScheduler.scheduleDailyReminders(context)
                            }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(horizontal = 16.dp))
                        // Offline Vocab Cache toggle
                        SettingsToggleRow(
                            label = stringResource(R.string.vocab_builder),
                            icon = null,
                            checked = offlineCache,
                            onCheckedChange = { offlineCache = it; saveBool("offline_cache", it) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionHeader(stringResource(R.string.privacy).uppercase())
                Spacer(Modifier.height(8.dp))

                // -- Legal card ------------------------------------------------
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsLinkRow(label = stringResource(R.string.privacy)) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.privacyPolicyUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        }
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsLinkRow(
                            label = stringResource(R.string.data_export_csv),
                            icon = Icons.Default.Download
                        ) {
                            if (prefs.getString("display_name", "").isNullOrBlank() || username.isBlank()) {
                                Toast.makeText(context, "Add username and name first", Toast.LENGTH_SHORT).show()
                                return@SettingsLinkRow
                            }

                            // Export user data as CSV
                            val csvData = buildString {
                                append("Name,Username,Skill\n")
                                append(csvCell(displayName))
                                append(",")
                                append(csvCell(username))
                                append(",")
                                append(csvCell(targetFluency))
                                append("\n")
                            }
                            val exportFile = File(context.cacheDir, "lingocoach_profile_export.csv")
                            exportFile.writeText(csvData)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                exportFile
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.lingocoach_data_export))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(intent, context.getString(R.string.export_data_as_csv)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // -- Delete Data & Account button -----------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFEBEE))
                        .clickable { showDeleteDialog = true }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = SettingsRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.delete),
                            color = SettingsRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // -- Dialogs ---------------------------------------------------------------

    // Edit Display Name
    if (showNameDialog) {
        var draft by remember { mutableStateOf(displayName) }
        SettingsEditDialog(
            title   = stringResource(R.string.name),
            value   = draft,
            onChange = { draft = it },
            onDismiss = { showNameDialog = false },
            onConfirm = {
                displayName = draft.trim().ifBlank { displayName }
                saveAndSync("display_name", displayName)
                showNameDialog = false
            }
        )
    }

    if (showUsernameDialog) {
        SettingsUsernameDialog(
            value = username,
            isSaving = isUsernameSaving,
            backendError = usernameSaveError,
            onDismiss = {
                if (!isUsernameSaving) {
                    usernameSaveError = null
                    showUsernameDialog = false
                }
            },
            onConfirm = { value ->
                val uid = prefs.getString("session_id", null)
                if (uid == null) {
                    usernameSaveError = "Could not find your profile. Please sign in again."
                    return@SettingsUsernameDialog
                }
                isUsernameSaving = true
                usernameSaveError = null
                com.mk.lingocoach.network.AssessmentApi.patchUserProfileDetailed(uid, mapOf("username" to value)) { result ->
                    scope.launch(Dispatchers.Main) {
                        isUsernameSaving = false
                        if (result.ok) {
                            username = value
                            saveStr("username", value)
                            usernameSaveError = null
                            showUsernameDialog = false
                        } else {
                            usernameSaveError = result.message.ifBlank { "Could not save username. Please try again." }
                        }
                    }
                }
            }
        )
    }

    // Target Fluency picker
    if (showFluencyDialog) {
        SettingsPickerDialog(
            title   = stringResource(R.string.skill_level),
            options = fluencyLevelOptions.map { it.second },
            selected = localizedFluencyLevel(targetFluency),
            onDismiss = { showFluencyDialog = false },
            onSelect  = { label ->
                val storedValue = fluencyLevelOptions.firstOrNull { it.second == label }?.first ?: label
                targetFluency = storedValue
                saveAndSync("target_fluency", storedValue)
                showFluencyDialog = false
            }
        )
    }

    // Voice Profile picker
    if (showVoiceDialog) {
        SettingsPickerDialog(
            title   = "AI Tutor Voice Profile",
            options = voiceProfiles,
            selected = voiceProfile,
            onDismiss = { showVoiceDialog = false },
            onSelect  = { voiceProfile = it; saveAndSync("voice_profile", it); showVoiceDialog = false }
        )
    }

    // Logout confirmation
    if (showLogoutDialog) {
        SettingsConfirmDialog(
            title   = stringResource(R.string.logout),
            message = stringResource(R.string.logout_confirm_message),
            confirmLabel = stringResource(R.string.logout),
            confirmColor = SettingsPurple,
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                prefs.edit()
                    .putBoolean("onboarding_completed", false)
                    .putBoolean("assessment_completed", false)
                    .putBoolean("lang_selected", false)
                    .putBoolean("personalization_done", false)
                    .apply()
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    // Delete account confirmation
    if (showDeleteDialog) {
        SettingsConfirmDialog(
            title   = stringResource(R.string.delete_account),
            message = stringResource(R.string.delete_account_confirm_message),
            confirmLabel = stringResource(R.string.delete),
            confirmColor = SettingsRed,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                prefs.edit().clear().apply()
                showDeleteDialog = false
                onLogout()
            }
        )
    }

}

@Composable
private fun localizedSettingsLanguageLabel(code: String): String {
    val language = appLanguages.firstOrNull { it.code == code } ?: appLanguages.first()
    return "${language.flagEmoji} ${localizedAppLanguageName(code)}"
}

// --- Reusable sub-components --------------------------------------------------

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SettingsCardBg)
    ) { content() }
}


@Composable
private fun SettingsProgressReminder(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SettingsPurpleSoft)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = SettingsPurple, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.add_username_save_restore),
            color = SettingsTextMid,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text,
        color = SettingsSectionLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsInfoRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SettingsTextDark,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            color = SettingsTextDark,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsArrowRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SettingsTextDark,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SettingsTextLight, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    icon: ImageVector?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = SettingsTextLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = label,
                color = SettingsTextDark,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = SettingsPurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCCCCCC)
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    icon: ImageVector = Icons.AutoMirrored.Filled.OpenInNew,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SettingsTextDark,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Icon(icon, contentDescription = null, tint = SettingsTextLight, modifier = Modifier.size(20.dp))
    }
}

// --- Dialog Components --------------------------------------------------------

@Composable
private fun SettingsEditDialog(
    title: String,
    value: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .imePadding()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column {
                Text(title, color = SettingsTextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = SettingsPurple,
                        unfocusedBorderColor = SettingsDivider,
                        focusedTextColor     = SettingsTextDark,
                        unfocusedTextColor   = SettingsTextDark
                    )
                )
                Spacer(Modifier.height(20.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = SettingsTextLight)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.Bold) }
                }            }
        }
    }
}


@Composable
private fun SettingsUsernameDialog(
    value: String,
    isSaving: Boolean,
    backendError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by remember(value) { mutableStateOf(value) }
    val localError = settingsUsernameError(draft)
    val error = localError ?: backendError

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .imePadding()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column {
                Text(stringResource(R.string.username), color = SettingsTextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_username_restore_desc),
                    color = SettingsTextMid,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = normalizeSettingsUsername(it)
                    },
                    singleLine = true,
                    leadingIcon = { Text("@", color = SettingsPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    isError = error != null,
                    supportingText = {
                        Text(
                            error ?: stringResource(R.string.settings_username_rules),
                            color = if (error != null) SettingsRed else SettingsTextLight,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SettingsPurple,
                        unfocusedBorderColor = SettingsDivider,
                        errorBorderColor = SettingsRed,
                        focusedTextColor = SettingsTextDark,
                        unfocusedTextColor = SettingsTextDark,
                        cursorColor = SettingsPurple
                    )
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text(stringResource(R.string.cancel), color = SettingsTextLight)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (localError == null) onConfirm(draft.trim()) },
                        enabled = localError == null && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text(stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun localizedFluencyLevels(): List<Pair<String, String>> = listOf(
    "Beginner / A1" to stringResource(R.string.fluency_beginner_a1),
    "Elementary / A2" to stringResource(R.string.fluency_elementary_a2),
    "Intermediate / B1" to stringResource(R.string.fluency_intermediate_b1),
    "Upper-Intermediate / B2" to stringResource(R.string.fluency_upper_intermediate_b2),
    "Advanced / C1" to stringResource(R.string.fluency_advanced_c1),
    "Professional / Business" to stringResource(R.string.fluency_professional_business)
)

@Composable
private fun localizedFluencyLevel(value: String): String =
    localizedFluencyLevels().firstOrNull { it.first == value }?.second ?: value

@Composable
private fun SettingsPickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(vertical = 24.dp)
        ) {
            Column {
                Text(
                    title,
                    color = SettingsTextDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(12.dp))
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .background(if (option == selected) SettingsPurpleSoft else Color.Transparent)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            option,
                            color = if (option == selected) SettingsPurple else SettingsTextDark,
                            fontSize = 15.sp,
                            fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (option == selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SettingsPurple, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp)
                ) { Text(stringResource(R.string.cancel), color = SettingsTextLight) }
            }
        }
    }
}

@Composable
private fun SettingsConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column {
                Text(title, color = SettingsTextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(message, color = SettingsTextMid, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = SettingsTextLight) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(confirmLabel, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}










