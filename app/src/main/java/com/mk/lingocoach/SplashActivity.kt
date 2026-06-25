package com.mk.lingocoach

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SplashActivity : AppCompatActivity() {

    private lateinit var analytics: FirebaseAnalytics
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        analytics = FirebaseAnalytics.getInstance(this)

        logSplashEvent(
            name = "on_create_start",
            details = "saved_state=${savedInstanceState != null}"
        )

        try {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { false }

            logSplashEvent("system_splash_installed")

            super.onCreate(savedInstanceState)
            logSplashEvent("super_on_create_complete")

            window.statusBarColor = Color.parseColor("#7053FF")
            window.navigationBarColor = Color.parseColor("#7053FF")

            setContentView(R.layout.activity_splash)
            logSplashEvent("content_view_set")

            runIntroSequence()
        } catch (e: Exception) {
            logSplashError("on_create_failed", e)
            throw e
        }
    }

    private fun runIntroSequence() {
        try {
            logSplashEvent("intro_sequence_start")

            val branding = findViewById<View>(R.id.splash_branding)
            val progress = findViewById<ProgressBar>(R.id.splash_progress)

            branding.alpha = 0f
            branding.translationY = 18f * resources.displayMetrics.density
            progress.alpha = 0f
            progress.progress = 0

            branding.visibility = View.VISIBLE
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(branding, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(branding, View.TRANSLATION_Y, branding.translationY, 0f)
                )
                duration = 500L
                interpolator = DecelerateInterpolator()
                addListener(
                    onStart = { logSplashEvent("branding_animation_start") },
                    onEnd = { logSplashEvent("branding_animation_end") }
                )
                start()
            }

            progress.postDelayed({
                try {
                    logSplashEvent("progress_animation_start")

                    progress.visibility = View.VISIBLE
                    progress.animate()
                        .alpha(1f)
                        .setDuration(120L)
                        .start()

                    ValueAnimator.ofInt(0, 100).apply {
                        duration = 1500L
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            progress.progress = animator.animatedValue as Int
                        }
                        addListener(
                            onEnd = {
                                try {
                                    logSplashEvent("navigate_main_start")
                                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                    logSplashEvent("navigate_main_complete")
                                    finish()
                                } catch (e: Exception) {
                                    logSplashError("navigate_main_failed", e)
                                    throw e
                                }
                            }
                        )
                        start()
                    }
                } catch (e: Exception) {
                    logSplashError("progress_animation_failed", e)
                    throw e
                }
            }, 500L)
        } catch (e: Exception) {
            logSplashError("intro_sequence_failed", e)
            throw e
        }
    }

    private fun logSplashEvent(name: String, details: String? = null) {
        val message = buildString {
            append("SplashActivity:")
            append(name)
            append(" sdk=")
            append(Build.VERSION.SDK_INT)
            append(" release=")
            append(Build.VERSION.RELEASE)
            if (details != null) {
                append(" ")
                append(details)
            }
        }

        Log.d(TAG, message)
        crashlytics.log(message)

        analytics.logEvent("splash_$name", Bundle().apply {
            putString("android_release", Build.VERSION.RELEASE)
            putLong("android_sdk", Build.VERSION.SDK_INT.toLong())
            putString("device_model", Build.MODEL)
            details?.let { putString("details", it.take(90)) }
        })
    }

    private fun logSplashError(stage: String, error: Exception) {
        Log.e(TAG, "SplashActivity:$stage", error)
        crashlytics.log("SplashActivity:$stage ${error.javaClass.simpleName}: ${error.message}")
        crashlytics.setCustomKey("splash_error_stage", stage)
        crashlytics.setCustomKey("splash_android_sdk", Build.VERSION.SDK_INT)
        crashlytics.setCustomKey("splash_android_release", Build.VERSION.RELEASE)
        crashlytics.recordException(error)
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}
