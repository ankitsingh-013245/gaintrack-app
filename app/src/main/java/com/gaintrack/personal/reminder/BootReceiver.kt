package com.gaintrack.personal.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.gaintrack.personal.GainTrackApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        MotivationScheduler.schedule(context)
        MealReminderScheduler.schedule(context)
        val app = context.applicationContext as GainTrackApplication
        if (app.repository.state.value.unlockReminderEnabled && Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(context, Intent(context, UnlockReminderService::class.java))
        }
    }
}
