package com.mk.lingocoach.config

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mk.lingocoach.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.mk.lingocoach.R

object AppConfig {
    private const val TAG = "AppConfig"

    private const val DEFAULT_BACKEND_BASE_URL = "https://lingoai-backend-zej0.onrender.com"
    private const val DEFAULT_PRIVACY_POLICY_URL = "https://lingocoach.app/privacy"
    private const val DEFAULT_TERMS_OF_SERVICE_URL = "https://lingocoach.app/terms"
    private const val DEFAULT_ONESIGNAL_APP_ID = "46957d03-f3f9-435c-b76c-e5cd0b8089b5"

    private const val KEY_BACKEND_BASE_URL = "backend_base_url"
    private const val KEY_PRIVACY_POLICY_URL = "privacy_policy_url"
    private const val KEY_TERMS_OF_SERVICE_URL = "terms_of_service_url"
    private const val KEY_ONESIGNAL_APP_ID = "onesignal_app_id"
    private const val KEY_DAILY_REMINDER_MORNING_HOUR = "daily_reminder_morning_hour"
    private const val KEY_DAILY_REMINDER_EVENING_HOUR = "daily_reminder_evening_hour"
    private const val KEY_MINIMUM_SUPPORTED_VERSION_CODE = "minimum_supported_version_code"
    private const val KEY_FORCE_UPDATE_ENABLED = "force_update_enabled"
    private const val KEY_MAINTENANCE_MODE_ENABLED = "maintenance_mode_enabled"

    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    val backendBaseUrl: String
        get() = remoteString(KEY_BACKEND_BASE_URL, DEFAULT_BACKEND_BASE_URL).trimEnd('/')

    val privacyPolicyUrl: String
        get() = remoteString(KEY_PRIVACY_POLICY_URL, DEFAULT_PRIVACY_POLICY_URL)

    val termsOfServiceUrl: String
        get() = remoteString(KEY_TERMS_OF_SERVICE_URL, DEFAULT_TERMS_OF_SERVICE_URL)

    val oneSignalAppId: String
        get() = remoteString(KEY_ONESIGNAL_APP_ID, DEFAULT_ONESIGNAL_APP_ID)

    val dailyReminderMorningHour: Int
        get() = remoteConfig.getLong(KEY_DAILY_REMINDER_MORNING_HOUR).takeIf { it in 0..23 }?.toInt() ?: 10

    val dailyReminderEveningHour: Int
        get() = remoteConfig.getLong(KEY_DAILY_REMINDER_EVENING_HOUR).takeIf { it in 0..23 }?.toInt() ?: 19

    val minimumSupportedVersionCode: Long
        get() = remoteConfig.getLong(KEY_MINIMUM_SUPPORTED_VERSION_CODE).coerceAtLeast(1L)

    val forceUpdateEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_FORCE_UPDATE_ENABLED)

    val maintenanceModeEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_MAINTENANCE_MODE_ENABLED)

    fun initialize(context: Context) {
        FirebaseApp.initializeApp(context)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        val minimumFetchIntervalSeconds = if (BuildConfig.DEBUG) 0L else 3600L
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds
            }
        )
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { updated ->
                Log.d(TAG, "Remote Config activated. Updated=$updated")
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Remote Config fetch failed; using defaults.", error)
                FirebaseCrashlytics.getInstance().recordException(error)
            }
    }

    private fun remoteString(key: String, fallback: String): String {
        return remoteConfig.getString(key).takeIf { it.isNotBlank() } ?: fallback
    }

}
