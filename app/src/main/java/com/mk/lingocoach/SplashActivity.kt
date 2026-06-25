package com.mk.lingocoach

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SplashActivity : AppCompatActivity() {

    private var customSplashReady = false
    private var systemSplashStartedAt = 0L
    private lateinit var analytics: FirebaseAnalytics
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        systemSplashStartedAt = SystemClock.elapsedRealtime()
        logSplashLine("SYSTEM onCreate_enter sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE}")
        analytics = FirebaseAnalytics.getInstance(this)

        logSplashEvent(
            name = "on_create_start",
            details = "saved_state=${savedInstanceState != null}"
        )

        try {
            logSystemSplashDiagnostics("launch_theme_before_install")
            val splashScreen = installSplashScreen()
            logSplashLine("SYSTEM installSplashScreen_done elapsed=${SystemClock.elapsedRealtime() - systemSplashStartedAt}ms")
            splashScreen.setKeepOnScreenCondition {
                val elapsed = SystemClock.elapsedRealtime() - systemSplashStartedAt
                !customSplashReady || elapsed < MIN_SYSTEM_SPLASH_MS
            }

            logSystemSplashDiagnostics("theme_after_install")
            logSplashEvent("system_splash_installed")

            super.onCreate(savedInstanceState)
            logSplashEvent("super_on_create_complete")

            window.statusBarColor = Color.parseColor("#7053FF")
            window.navigationBarColor = Color.parseColor("#7053FF")

            setContentView(R.layout.activity_splash)
            findViewById<View>(R.id.splash_root).post {
                customSplashReady = true
                logSplashLine("SYSTEM release_to_custom elapsed=${SystemClock.elapsedRealtime() - systemSplashStartedAt}ms")
                logCustomSplashViewDiagnostics("custom_splash_ready")
                logSplashEvent("custom_splash_ready")
            }
            logCustomSplashViewDiagnostics("content_view_set")
            logSplashEvent("content_view_set")

            runBackgroundTransition()
            runIntroSequence()
        } catch (e: Exception) {
            logSplashError("on_create_failed", e)
            throw e
        }
    }

    private fun logSystemSplashDiagnostics(stage: String) {
        try {
            val iconValue = TypedValue()
            val backgroundValue = TypedValue()
            val iconResolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                theme.resolveAttribute(android.R.attr.windowSplashScreenAnimatedIcon, iconValue, true)
            } else {
                iconValue.resourceId = R.mipmap.splash_system_plain
                true
            }
            val backgroundResolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                theme.resolveAttribute(android.R.attr.windowSplashScreenBackground, backgroundValue, true)
            } else {
                backgroundValue.resourceId = R.color.splash_purple
                true
            }
            val configuredIconDrawable = runCatching {
                ResourcesCompat.getDrawable(resources, R.mipmap.splash_system_plain, theme)
            }.getOrNull()
            val resolvedIconDrawable = iconValue.resourceId.takeIf { iconResolved && it != 0 }?.let { resourceId ->
                runCatching { ResourcesCompat.getDrawable(resources, resourceId, theme) }.getOrNull()
            }

            val details = listOf(
                "stage=$stage",
                "iconResolved=$iconResolved",
                "iconResId=${iconValue.resourceId}",
                "iconName=${resourceEntryName(iconValue.resourceId)}",
                "iconType=${resolvedIconDrawable.drawableTypeName()}",
                "iconSize=${resolvedIconDrawable.intrinsicSize()}",
                "configuredIconName=${resourceEntryName(R.mipmap.splash_system_plain)}",
                "configuredIconType=${configuredIconDrawable.drawableTypeName()}",
                "configuredIconSize=${configuredIconDrawable.intrinsicSize()}",
                "backgroundResolved=$backgroundResolved",
                "backgroundResId=${backgroundValue.resourceId}",
                "backgroundName=${resourceEntryName(backgroundValue.resourceId)}",
                "densityDpi=${resources.displayMetrics.densityDpi}",
                "fontScale=${resources.configuration.fontScale}"
            ).joinToString(" ")

            logSplashLine("SYSTEM diagnostics $details")
            logSplashEvent("system_splash_diagnostics", details)
            crashlytics.setCustomKey("splash_icon_res_id", iconValue.resourceId)
            crashlytics.setCustomKey("splash_icon_name", resourceEntryName(iconValue.resourceId))
            crashlytics.setCustomKey("splash_icon_type", resolvedIconDrawable.drawableTypeName())
            crashlytics.setCustomKey("splash_icon_size", resolvedIconDrawable.intrinsicSize())
            crashlytics.setCustomKey("splash_configured_icon_type", configuredIconDrawable.drawableTypeName())
            crashlytics.setCustomKey("splash_configured_icon_size", configuredIconDrawable.intrinsicSize())
            crashlytics.setCustomKey("splash_density_dpi", resources.displayMetrics.densityDpi)
        } catch (e: Exception) {
            logSplashError("system_splash_diagnostics_failed", e)
        }
    }

    private fun logCustomSplashViewDiagnostics(stage: String) {
        val root = findViewById<View?>(R.id.splash_root)
        val bot = findViewById<ImageView?>(R.id.splash_bot)
        val branding = findViewById<View?>(R.id.splash_branding)
        val title = findViewById<TextView?>(R.id.splash_title)
        val progress = findViewById<ProgressBar?>(R.id.splash_progress)
        val details = listOf(
            "stage=$stage",
            "root=${root.viewDiagnostics()}",
            "bot=${bot.viewDiagnostics()} drawable=${bot?.drawable.drawableTypeName()} drawableSize=${bot?.drawable.intrinsicSize()}",
            "branding=${branding.viewDiagnostics()}",
            "title=${title.viewDiagnostics()} text=${title?.text}",
            "progress=${progress.viewDiagnostics()} progress=${progress?.progress}",
            "windowStatus=#${window.statusBarColor.toUInt().toString(16)}",
            "windowNav=#${window.navigationBarColor.toUInt().toString(16)}"
        ).joinToString(" ")

        logSplashLine("CUSTOM $details")
        crashlytics.log("SplashActivity:CUSTOM $details")
    }

    private fun View?.viewDiagnostics(): String =
        if (this == null) {
            "null"
        } else {
            "${width}x${height}@(${x.toInt()},${y.toInt()}) vis=$visibility alpha=$alpha bg=${background.drawableTypeName()} bgSize=${background.intrinsicSize()}"
        }
    private fun resourceEntryName(resourceId: Int): String =
        if (resourceId != 0) {
            runCatching { resources.getResourceEntryName(resourceId) }.getOrDefault("unknown")
        } else {
            "none"
        }

    private fun Drawable?.drawableTypeName(): String = this?.javaClass?.simpleName ?: "null"

    private fun Drawable?.intrinsicSize(): String =
        if (this == null) "null" else "${intrinsicWidth}x${intrinsicHeight}"
    private fun runBackgroundTransition() {
        val root = findViewById<View>(R.id.splash_root)
        val purple = Color.parseColor("#7053FF")

        root.setBackgroundColor(Color.BLACK)
        ValueAnimator.ofObject(ArgbEvaluator(), Color.BLACK, purple).apply {
            duration = 520L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                root.setBackgroundColor(color)
                window.statusBarColor = color
                window.navigationBarColor = color
            }
            addListener(
                onStart = { logSplashEvent("background_transition_start") },
                onEnd = { logSplashEvent("background_transition_end") }
            )
            start()
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


    private fun logSplashLine(message: String) {
        Log.w(TAG, message)
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

        logSplashLine(message)
        crashlytics.log(message)

        analytics.logEvent("splash_$name", Bundle().apply {
            putString("android_release", Build.VERSION.RELEASE)
            putLong("android_sdk", Build.VERSION.SDK_INT.toLong())
            putString("device_model", Build.MODEL)
            details?.let { putString("details", it.take(90)) }
        })
    }

    private fun logSplashError(stage: String, error: Exception) {
        Log.e(TAG, "ERROR stage=$stage", error)
        crashlytics.log("SplashActivity:$stage ${error.javaClass.simpleName}: ${error.message}")
        crashlytics.setCustomKey("splash_error_stage", stage)
        crashlytics.setCustomKey("splash_android_sdk", Build.VERSION.SDK_INT)
        crashlytics.setCustomKey("splash_android_release", Build.VERSION.RELEASE)
        crashlytics.recordException(error)
    }

    companion object {
        private const val TAG = "LingoSplash"
        private const val MIN_SYSTEM_SPLASH_MS = 450L
    }
}
