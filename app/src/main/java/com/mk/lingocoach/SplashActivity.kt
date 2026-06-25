package com.mk.lingocoach

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#7053FF")
        window.navigationBarColor = Color.parseColor("#7053FF")

        setContentView(R.layout.activity_splash)
        runIntroSequence()
    }

    private fun runIntroSequence() {
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
            start()
        }

        progress.postDelayed({
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
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }
                )
                start()
            }
        }, 500L)
    }
}