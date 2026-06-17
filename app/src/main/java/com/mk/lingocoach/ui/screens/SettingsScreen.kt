package com.mk.lingocoach.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mk.lingocoach.R
import com.mk.lingocoach.notifications.NotificationScheduler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mk.lingocoach.data.model.appLanguageLabel
import com.mk.lingocoach.data.model.appLanguages
import com.mk.lingocoach.data.repository.LanguagePreferencesRepository
import kotlinx.coroutines.launch

// ─── Settings Design Tokens ───────────────────────────────────────────────────
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

// ─── Fluency levels list ──────────────────────────────────────────────────────
private val fluencyLevels = listOf(
    "Beginner / A1", "Elementary / A2", "Intermediate / B1",
    "Upper-Intermediate / B2", "Advanced / C1", "Professional / Business"
)

// ─── Native language list ─────────────────────────────────────────────────────
// ─── AI tutor voice profiles ──────────────────────────────────────────────────
private val voiceProfiles = listOf(
    "Male – British Accent", "Male – American Accent",
    "Female – British Accent", "Female – American Accent",
    "Female – Australian Accent"
)

// ─── Main Settings Screen ─────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val languageRepository = remember(context) { LanguagePreferencesRepository(context) }
    val scroll  = rememberScrollState()
    val mirroredLanguageCode = context
        .getSharedPreferences("language_preferences_mirror", Context.MODE_PRIVATE)
        .getString("selected_language", "system") ?: "system"

    // ── Persisted state ──────────────────────────────────────────────────────
    var displayName    by remember { mutableStateOf(prefs.getString("display_name", "Alex Mercer") ?: "Alex Mercer") }
    var targetFluency  by remember { mutableStateOf(prefs.getString("target_fluency", "Professional / Business") ?: "Professional / Business") }
    var appLanguageCode by remember { mutableStateOf(mirroredLanguageCode) }
    var voiceProfile   by remember { mutableStateOf(prefs.getString("voice_profile", "Male – British Accent") ?: "Male – British Accent") }
    var dailyReminder  by remember { mutableStateOf(prefs.getBoolean("daily_reminder", true)) }
    var offlineCache   by remember { mutableStateOf(prefs.getBoolean("offline_cache", false)) }

    // ── Dialog state ─────────────────────────────────────────────────────────
    var showNameDialog     by remember { mutableStateOf(false) }
    var showFluencyDialog  by remember { mutableStateOf(false) }
    var showLangDialog     by remember { mutableStateOf(false) }
    var showVoiceDialog    by remember { mutableStateOf(false) }
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showLogoutDialog   by remember { mutableStateOf(false) }

    // ── Helper: persist a boolean ─────────────────────────────────────────────
    fun saveBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String)   = prefs.edit().putString(key, value).apply()

    // ── Helper: persist + sync one field to backend ───────────────────────────
    fun saveAndSync(key: String, value: String) {
        saveStr(key, value)
        val mapping = mapOf(
            "display_name"    to "display_name",
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
            // ── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = SettingsPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings),
                    style = TextStyle(color = SettingsTextDark, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                )
            }

            // ── Scrollable content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .background(SettingsBg)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Avatar + Profile card ─────────────────────────────────────
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(SettingsPurpleSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SettingsPurple,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(SettingsPurple)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable { showNameDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Change Avatar",
                            color = SettingsPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showNameDialog = true }
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = SettingsDivider)
                        Spacer(Modifier.height(12.dp))

                        // Display Name row
                        SettingsInfoRow(
                            label = stringResource(R.string.name),
                            value = displayName,
                            onClick = { showNameDialog = true }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        // Target Fluency row
                        SettingsInfoRow(
                            label = stringResource(R.string.skill_level),
                            value = targetFluency,
                            onClick = { showFluencyDialog = true }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        SettingsInfoRow(
                            label = stringResource(R.string.app_language),
                            value = appLanguageLabel(appLanguageCode),
                            onClick = { showLangDialog = true }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionHeader(stringResource(R.string.app_settings).uppercase())
                Spacer(Modifier.height(8.dp))

                // ── App & Learning Preferences card ──────────────────────────
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

                // ── Legal card ────────────────────────────────────────────────
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsLinkRow(label = stringResource(R.string.privacy)) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lingocoach.app/privacy")))
                        }
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsLinkRow(label = "Terms of Service") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lingocoach.app/terms")))
                        }
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsLinkRow(
                            label = "Data Export (CSV)",
                            icon = Icons.Default.Download
                        ) {
                            // Export user data as CSV
                            val csvData = buildString {
                                append("Field,Value\n")
                                append("Display Name,$displayName\n")
                                append("Target Fluency,$targetFluency\n")
                                append("App Language,${appLanguageLabel(appLanguageCode)}\n")
                                append("Daily Reminder,$dailyReminder\n")
                                append("Offline Cache,$offlineCache\n")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csvData)
                                putExtra(Intent.EXTRA_SUBJECT, "LingoCoach Data Export")
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Data as CSV"))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Delete Data & Account button ─────────────────────────────
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

    // ── Dialogs ───────────────────────────────────────────────────────────────

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

    // Target Fluency picker
    if (showFluencyDialog) {
        SettingsPickerDialog(
            title   = stringResource(R.string.skill_level),
            options = fluencyLevels,
            selected = targetFluency,
            onDismiss = { showFluencyDialog = false },
            onSelect  = { targetFluency = it; saveAndSync("target_fluency", it); showFluencyDialog = false }
        )
    }

    // Native Language picker
    if (showLangDialog) {
        AppLanguagePickerDialog(
            selectedCode = appLanguageCode,
            onDismiss = { showLangDialog = false },
            onConfirm = { code ->
                val previousCode = appLanguageCode
                appLanguageCode = code
                saveAndSync("native_language", code)
                context.getSharedPreferences("language_preferences_mirror", Context.MODE_PRIVATE)
                    .edit()
                    .putString("selected_language", code)
                    .apply()
                scope.launch {
                    languageRepository.saveSelectedLanguage(code)
                }
                if (code != previousCode) {
                    if (code != "system") {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(code)
                        )
                    } else {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    }
                }
                showLangDialog = false
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
            title   = "Log Out",
            message = "Are you sure you want to log out of your account?",
            confirmLabel = "Log Out",
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
            title   = "Delete Account",
            message = "This will permanently delete all your progress and account data. This action cannot be undone.",
            confirmLabel = "Delete",
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
private fun AppLanguagePickerDialog(
    selectedCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var draftCode by remember(selectedCode) { mutableStateOf(selectedCode) }
    val filteredLanguages = remember(query) {
        val term = query.trim()
        if (term.isBlank()) {
            appLanguages
        } else {
            appLanguages.filter { language ->
                language.name.contains(term, ignoreCase = true) ||
                    language.nativeName.contains(term, ignoreCase = true) ||
                    language.code.contains(term, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.app_language),
                        color = SettingsTextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onConfirm(draftCode) }) {
                        Text(
                            stringResource(R.string.done),
                            color = SettingsPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_languages), color = SettingsTextLight) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SettingsTextLight) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = SettingsTextLight
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SettingsPurple,
                        unfocusedBorderColor = SettingsDivider,
                        focusedTextColor = SettingsTextDark,
                        unfocusedTextColor = SettingsTextDark,
                        cursorColor = SettingsPurple
                    )
                )

                Spacer(Modifier.height(10.dp))

                if (filteredLanguages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_languages_found, query),
                            color = SettingsTextLight,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filteredLanguages, key = { it.code }) { language ->
                            val selected = language.code == draftCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { draftCode = language.code }
                                    .background(if (selected) SettingsPurpleSoft else Color.Transparent)
                                    .padding(horizontal = 24.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(language.flagEmoji, fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        language.name,
                                        color = if (selected) SettingsPurple else SettingsTextDark,
                                        fontSize = 15.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                    Text(
                                        language.nativeName,
                                        color = SettingsTextLight,
                                        fontSize = 12.sp
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SettingsPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Reusable sub-components ──────────────────────────────────────────────────

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
        Text(label, color = SettingsTextDark, fontSize = 15.sp)
        Text(value, color = SettingsTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        Text(label, color = SettingsTextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
            Text(label, color = SettingsTextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
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
        Text(label, color = SettingsTextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Icon(icon, contentDescription = null, tint = SettingsTextLight, modifier = Modifier.size(20.dp))
    }
}

// ─── Dialog Components ────────────────────────────────────────────────────────

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
                    modifier = Modifier.fillMaxWidth(),
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
                        Text("Cancel", color = SettingsTextLight)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

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
                ) { Text("Cancel", color = SettingsTextLight) }
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
                    TextButton(onClick = onDismiss) { Text("Cancel", color = SettingsTextLight) }
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

