package com.gaintrack.personal

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaintrack.personal.data.ExerciseLog
import com.gaintrack.personal.data.GainRepository
import com.gaintrack.personal.reminder.MotivationScheduler
import com.gaintrack.personal.reminder.MealReminderScheduler
import com.gaintrack.personal.reminder.UnlockReminderService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository: GainRepository = (application as GainTrackApplication).repository
    val state = repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.state.value)

    fun setUnlockReminder(enabled: Boolean) {
        val context = getApplication<Application>()
        repository.setUnlockReminder(enabled)
        if (enabled && Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(context, Intent(context, UnlockReminderService::class.java))
        } else if (!enabled) {
            context.stopService(Intent(context, UnlockReminderService::class.java))
        }
    }

    fun overlaySettingsIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${getApplication<Application>().packageName}")
    )

    fun setDailyNotifications(enabled: Boolean) {
        repository.setDailyNotification(enabled)
        MotivationScheduler.schedule(getApplication())
        MealReminderScheduler.schedule(getApplication())
    }

    fun logSet(date: LocalDate, workout: String, exerciseId: String, set: Int, reps: Int, weight: Double) =
        repository.saveExerciseSet(ExerciseLog(date.toString(), workout, exerciseId, set, reps, weight))
}
