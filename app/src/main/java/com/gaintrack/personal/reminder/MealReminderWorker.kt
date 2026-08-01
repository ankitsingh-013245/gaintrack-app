package com.gaintrack.personal.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gaintrack.personal.GainTrackApplication
import com.gaintrack.personal.data.mealReminderKey
import com.gaintrack.personal.data.overdueMeals
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MealReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as GainTrackApplication
        val state = app.repository.state.value
        if (!state.profile.onboardingComplete || !state.dailyNotificationEnabled) {
            return Result.success()
        }
        val now = LocalDateTime.now()
        val summary = overdueMeals(state, now)
        val freshKeys = summary.meals
            .map { mealReminderKey(now.toLocalDate(), it.id) }
            .filterNot { it in state.notifiedMealKeys }
            .toSet()
        if (freshKeys.isNotEmpty()) {
            NotificationHelper.showMealReminder(applicationContext, summary, freshKeys.size)
            app.repository.markMealsNotified(freshKeys)
        }
        return Result.success()
    }
}

object MealReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<MealReminderWorker>(
            15,
            TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "missed_meal_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
