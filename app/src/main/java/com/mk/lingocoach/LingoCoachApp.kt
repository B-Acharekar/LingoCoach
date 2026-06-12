package com.mk.lingocoach

import android.app.Application
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class LingoCoachApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable OneSignal verbose logging for debug
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // OneSignal Initialization
        // Note: Replace "YOUR_ONESIGNAL_APP_ID" with your actual OneSignal App ID.
        try {
            OneSignal.initWithContext(this, "YOUR_ONESIGNAL_APP_ID")
        } catch (e: Exception) {
            Log.e("LingoCoachApp", "Failed to initialize OneSignal: ${e.message}", e)
        }
    }
}
