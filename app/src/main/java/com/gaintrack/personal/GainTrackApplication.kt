package com.gaintrack.personal

import android.app.Application
import com.gaintrack.personal.data.GainRepository
import com.gaintrack.personal.reminder.MotivationScheduler
import com.gaintrack.personal.reminder.NotificationHelper
import com.gaintrack.personal.reminder.MotivationWorker
import com.gaintrack.personal.reminder.MealReminderScheduler
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class GainTrackApplication : Application() {
    lateinit var repository: GainRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GainRepository(this)
        NotificationHelper.createChannels(this)
        MotivationScheduler.schedule(this)
        MealReminderScheduler.schedule(this)
        WorkManager.getInstance(this).enqueueUniqueWork(
            "startup_motivation_refresh",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MotivationWorker>().build(),
        )
    }
}
