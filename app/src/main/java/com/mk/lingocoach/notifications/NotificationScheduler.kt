one package com.mk.lingocoach.notifications

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    
    private const val MORNING_REMINDER_TAG = "morning_reminder"
    private const val EVENING_REMINDER_TAG = "evening_reminder"

    fun scheduleDailyReminders(context: Context) {
        val prefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
        val dailyReminderEnabled = prefs.getBoolean("daily_reminder", true)
        
        if (dailyReminderEnabled) {
            scheduleMorningReminder(context)
            scheduleEveningReminder(context)
        } else {
            cancelDailyReminders(context)
        }
    }

    private fun scheduleMorningReminder(context: Context) {
        val initialDelay = calculateInitialDelay(10, 0) // 10:00 AM
        
        val morningWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(MORNING_REMINDER_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MORNING_REMINDER_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            morningWorkRequest
        )
    }

    private fun scheduleEveningReminder(context: Context) {
        val initialDelay = calculateInitialDelay(19, 0) // 7:00 PM
        
        val eveningWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(EVENING_REMINDER_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EVENING_REMINDER_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            eveningWorkRequest
        )
    }

    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time has passed today, schedule for tomorrow
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        return targetTime.timeInMillis - currentTime.timeInMillis
    }

    fun cancelDailyReminders(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelAllWorkByTag(MORNING_REMINDER_TAG)
            cancelAllWorkByTag(EVENING_REMINDER_TAG)
        }
    }
}
