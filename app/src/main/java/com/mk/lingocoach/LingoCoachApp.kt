package com.mk.lingocoach

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mk.lingocoach.ads.LingoCoachAds
import com.mk.lingocoach.config.AppConfig
import com.mk.lingocoach.data.repository.AppThemeManager
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class LingoCoachApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AppThemeManager.applyStoredMode(this)
        AppConfig.initialize(this)
        LingoCoachAds.initialize(this)

        OneSignal.Debug.logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.NONE

        try {
            OneSignal.initWithContext(this, AppConfig.oneSignalAppId)
        } catch (e: Exception) {
            Log.e("LingoCoachApp", "Failed to initialize OneSignal: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}
