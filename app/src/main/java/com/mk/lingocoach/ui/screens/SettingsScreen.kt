package com.mk.lingocoach.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
private val nativeLanguages = listOf(
    "English – US", "English – UK", "Spanish", "French", "German",
    "Portuguese", "Hindi", "Arabic", "Mandarin", "Japanese", "Korean",
    "Italian", "Russian", "Turkish", "Vietnamese"
)

// ─── Language code map for locale switching ───────────────────────────────────
private val nativeLanguageCodeMap = mapOf(
    "English – US"  to "en",
    "English – UK"  to "en-GB",
    "Spanish"       to "es",
    "French"        to "fr",
    "German"        to "de",
    "Portuguese"    to "pt",
    "Hindi"         to "hi",
    "Arabic"        to "ar",
    "Mandarin"      to "zh",
    "Japanese"      to "ja",
    "Korean"        to "ko",
    "Italian"       to "it",
    "Russian"       to "ru",
    "Turkish"       to "tr",
    "Vietnamese"    to "vi"
)

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
    val scroll  = rememberScrollState()

    // ── Persisted state ──────────────────────────────────────────────────────
    var displayName    by remember { mutableStateOf(prefs.getString("display_name", "Alex Mercer") ?: "Alex Mercer") }
    var targetFluency  by remember { mutableStateOf(prefs.getString("target_fluency", "Professional / Business") ?: "Professional / Business") }
    var nativeLang     by remember { mutableStateOf(prefs.getString("native_language", "English – US") ?: "English – US") }
    var email          by remember { mutableStateOf(prefs.getString("user_email", "alex.mercer@email.com") ?: "alex.mercer@email.com") }
    var voiceProfile   by remember { mutableStateOf(prefs.getString("voice_profile", "Male – British Accent") ?: "Male – British Accent") }
    var dailyReminder  by remember { mutableStateOf(prefs.getBoolean("daily_reminder", true)) }
    var linkedAccounts by remember { mutableStateOf(prefs.getBoolean("linked_accounts", true)) }
    var offlineCache   by remember { mutableStateOf(prefs.getBoolean("offline_cache", false)) }

    // ── Dialog state ─────────────────────────────────────────────────────────
    var showNameDialog     by remember { mutableStateOf(false) }
    var showFluencyDialog  by remember { mutableStateOf(false) }
    var showLangDialog     by remember { mutableStateOf(false) }
    var showVoiceDialog    by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
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
                        contentDescription = "Back",
                        tint = SettingsPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Settings",
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
                            label = "Display Name",
                            value = displayName,
                            onClick = { showNameDialog = true }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        // Target Fluency row
                        SettingsInfoRow(
                            label = "Target Fluency",
                            value = targetFluency,
                            onClick = { showFluencyDialog = true }
                        )
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(vertical = 8.dp))
                        // Native Language row
                        SettingsInfoRow(
                            label = "Native Language",
                            value = nativeLang,
                            onClick = { showLangDialog = true }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionHeader("ACCOUNT & SECURITY")
                Spacer(Modifier.height(8.dp))

                // ── Account & Security card ───────────────────────────────────
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Email row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Email", color = SettingsTextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(email, color = SettingsTextLight, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SettingsGreen, modifier = Modifier.size(22.dp))
                        }
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(horizontal = 16.dp))
                        // Change Password
                        SettingsArrowRow(label = "Change Password", onClick = { showPasswordDialog = true })
                        HorizontalDivider(color = SettingsDivider, modifier = Modifier.padding(horizontal = 16.dp))
                        // Linked Accounts toggle
                        SettingsToggleRow(
                            label = "Linked Accounts",
                            icon = Icons.Default.Link,
                            checked = linkedAccounts,
                            onCheckedChange = { linkedAccounts = it; saveBool("linked_accounts", it) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionHeader("APP & LEARNING PREFERENCES")
                Spacer(Modifier.height(8.dp))

                // ── App & Learning Preferences card ──────────────────────────
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Daily Reminder toggle
                        SettingsToggleRow(
                            label = "Daily Reminder Push",
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
                            label = "Offline Vocab Cache",
                            icon = null,
                            checked = offlineCache,
                            onCheckedChange = { offlineCache = it; saveBool("offline_cache", it) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionHeader("LEGAL")
                Spacer(Modifier.height(8.dp))

                // ── Legal card ────────────────────────────────────────────────
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsLinkRow(label = "Privacy Policy") {
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
                                append("Email,$email\n")
                                append("Target Fluency,$targetFluency\n")
                                append("Native Language,$nativeLang\n")
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
                            "Delete All Data & Account",
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
            title   = "Display Name",
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
            title   = "Target Fluency",
            options = fluencyLevels,
            selected = targetFluency,
            onDismiss = { showFluencyDialog = false },
            onSelect  = { targetFluency = it; saveAndSync("target_fluency", it); showFluencyDialog = false }
        )
    }

    // Native Language picker
    if (showLangDialog) {
        SettingsPickerDialog(
            title   = "Native Language",
            options = nativeLanguages,
            selected = nativeLang,
            onDismiss = { showLangDialog = false },
            onSelect  = { selection ->
                nativeLang = selection
                saveAndSync("native_language", selection)
                // Apply locale change immediately
                val code = nativeLanguageCodeMap[selection]
                if (!code.isNullOrEmpty()) {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(code)
                    )
                    // Mirror for cold-start restore
                    context.getSharedPreferences("language_preferences_mirror", Context.MODE_PRIVATE)
                        .edit().putString("selected_language", code).apply()
                } else {
                    // "system" / English – restore default
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    context.getSharedPreferences("language_preferences_mirror", Context.MODE_PRIVATE)
                        .edit().putString("selected_language", "system").apply()
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

    // Change Password dialog
    if (showPasswordDialog) {
        SettingsChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { showPasswordDialog = false }
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
                    .putBoolean("profile_setup_done", false)
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
private fun SettingsChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var current  by remember { mutableStateOf("") }
    var newPass  by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
                Text("Change Password", color = SettingsTextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = SettingsPurple,
                    unfocusedBorderColor = SettingsDivider,
                    focusedTextColor     = SettingsTextDark,
                    unfocusedTextColor   = SettingsTextDark
                )
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("Current Password") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = fieldColors, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPass, onValueChange = { newPass = it },
                    label = { Text("New Password") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = fieldColors, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it },
                    label = { Text("Confirm New Password") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = fieldColors, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = SettingsRed, fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = SettingsTextLight) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when {
                                current.isBlank() -> errorMsg = "Enter your current password"
                                newPass.length < 6 -> errorMsg = "New password must be at least 6 characters"
                                newPass != confirm -> errorMsg = "Passwords do not match"
                                else -> { errorMsg = null; onConfirm() }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Update", color = Color.White, fontWeight = FontWeight.Bold) }
                }
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
