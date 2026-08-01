package com.gaintrack.personal.reminder

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gaintrack.personal.GainTrackApplication
import com.gaintrack.personal.MainActivity
import com.gaintrack.personal.data.SeedPlan
import com.gaintrack.personal.data.foodFor
import com.gaintrack.personal.data.workoutFor
import java.time.LocalDate
import java.time.LocalTime

class UnlockReminderService : Service() {
    private var overlay: View? = null
    private lateinit var windowManager: WindowManager

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) maybeShowCard()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        startForeground(2001, NotificationHelper.serviceNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!(application as GainTrackApplication).repository.state.value.unlockReminderEnabled) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        dismiss()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun maybeShowCard() {
        if (!Settings.canDrawOverlays(this) || overlay != null) return
        val now = LocalTime.now()
        if (now.isBefore(LocalTime.of(7, 0))) return
        val prefs = getSharedPreferences("unlock_card", MODE_PRIVATE)
        val today = LocalDate.now()
        val last = prefs.getString("last_date", "")
        val repository = (application as GainTrackApplication).repository
        val snapshot = repository.snapshot(today)
        val plannedFood = repository.state.value.foodFor(today)
        val importantMissed = now.isAfter(LocalTime.of(21, 15)) &&
            snapshot.completedFood.size < plannedFood.size - 2
        if (last == today.toString() && !importantMissed) return
        showCard(importantMissed)
        prefs.edit().putString("last_date", today.toString()).apply()
    }

    private fun showCard(missed: Boolean) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 34, 44, 30)
            background = roundedDrawable("#FFFFFF", 28f)
            elevation = 18f
        }
        container.addView(TextView(this).apply {
            text = if (missed) "A small check-in before the day ends" else "Good morning — ready for one steady step?"
            textSize = 19f; setTextColor(Color.parseColor("#17201C")); setTypeface(typeface, Typeface.BOLD)
        })
        container.addView(TextView(this).apply {
            val state = (application as GainTrackApplication).repository.state.value
            text = "${state.motivation.quote}\n— ${state.motivation.author}"
            textSize = 14f; setTextColor(Color.parseColor("#4E5B55"))
            setPadding(0, 16, 0, 18)
        })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        fun action(label: String, action: () -> Unit) = Button(this).apply {
            text = label; isAllCaps = false; setOnClickListener { action(); dismiss() }
        }
        buttons.addView(action("Later") {})
        if (missed && (application as GainTrackApplication).repository.state.value.workoutFor(LocalDate.now()) != null) {
            buttons.addView(action("Skip workout") {
                (application as GainTrackApplication).repository.setWorkoutStatus(LocalDate.now(), false, true)
            })
        }
        buttons.addView(action("Log now") {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        container.addView(buttons)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 90
            horizontalMargin = .035f
        }
        overlay = container
        windowManager.addView(container, params)
    }

    private fun roundedDrawable(color: String, radius: Float) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(color)); cornerRadius = radius
            setStroke(1, Color.parseColor("#DFE6E1"))
        }

    private fun dismiss() {
        overlay?.let { runCatching { windowManager.removeView(it) } }
        overlay = null
    }
}
