package com.gaintrack.personal.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gaintrack.personal.MainActivity
import com.gaintrack.personal.R
import com.gaintrack.personal.data.Motivation
import com.gaintrack.personal.data.OverdueMealSummary
import java.net.URL

object NotificationHelper {
    const val MOTIVATION_CHANNEL = "daily_motivation"
    const val MEAL_CHANNEL = "missed_meals"
    const val SERVICE_CHANNEL = "unlock_companion"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            MOTIVATION_CHANNEL, "Daily motivation", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Your daily quote and health check-in" })
        manager.createNotificationChannel(NotificationChannel(
            MEAL_CHANNEL, "Missed meal reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "One reminder when a planned meal is 30 minutes overdue" })
        manager.createNotificationChannel(NotificationChannel(
            SERVICE_CHANNEL, "Unlock check-in", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps the optional unlock reminder ready"; setShowBadge(false) })
    }

    fun showMealReminder(
        context: Context,
        summary: OverdueMealSummary,
        newlyOverdueCount: Int,
    ) {
        if (summary.meals.isEmpty()) return
        val intent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val mealLabel = if (newlyOverdueCount == 1) "A planned meal is overdue" else {
            "$newlyOverdueCount planned meals are overdue"
        }
        val detail = "You’re ${summary.calories} kcal and ${summary.protein} g protein behind schedule."
        val notification = NotificationCompat.Builder(context, MEAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("😔 $mealLabel")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (permitted) {
            try {
                NotificationManagerCompat.from(context).notify(2001, notification)
            } catch (_: SecurityException) {
                // Notification access can be revoked between permission check and dispatch.
            }
        }
    }

    fun showMotivation(context: Context, motivation: Motivation) {
        val intent = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val bitmap = motivation.imageUrl.takeIf { it.isNotBlank() }?.let { downloadBitmap(it) }
        val picture = bitmap?.let { renderQuote(it, motivation) }
        val style = if (picture != null) {
            NotificationCompat.BigPictureStyle().bigPicture(picture).bigLargeIcon(null as Bitmap?)
                .setSummaryText("— ${motivation.author}")
        } else {
            NotificationCompat.BigTextStyle().bigText("\"${motivation.quote}\"\n— ${motivation.author}")
        }
        val notification = NotificationCompat.Builder(context, MOTIVATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("A strong day starts with one choice")
            .setContentText(motivation.quote)
            .setStyle(style)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .addAction(0, "Open today", intent)
            .build()
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (permitted) {
            try {
                NotificationManagerCompat.from(context).notify(1001, notification)
            } catch (_: SecurityException) {
                // The user can revoke notification access between the check and this call.
            }
        }
    }

    fun serviceNotification(context: Context) = NotificationCompat.Builder(context, SERVICE_CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("GainTrack check-in is ready")
        .setContentText("A gentle card can appear after your first unlock.")
        .setOngoing(true)
        .setSilent(true)
        .build()

    private fun downloadBitmap(url: String): Bitmap? = runCatching {
        URL(url).openStream().use(BitmapFactory::decodeStream)
    }.getOrNull()

    private fun renderQuote(source: Bitmap, motivation: Motivation): Bitmap {
        val width = 1000
        val height = 520
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val srcRatio = source.width.toFloat() / source.height
        val dstRatio = width.toFloat() / height
        val src = if (srcRatio > dstRatio) {
            val newWidth = (source.height * dstRatio).toInt()
            Rect((source.width - newWidth) / 2, 0, (source.width + newWidth) / 2, source.height)
        } else {
            val newHeight = (source.width / dstRatio).toInt()
            Rect(0, (source.height - newHeight) / 2, source.width, (source.height + newHeight) / 2)
        }
        canvas.drawBitmap(source, src, Rect(0, 0, width, height), null)
        canvas.drawColor(Color.argb(135, 0, 0, 0))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
        }
        val lines = wrapText("“${motivation.quote}”", paint, width - 120)
        var y = 170f - ((lines.size - 1) * 30)
        lines.take(5).forEach { line -> canvas.drawText(line, 60f, y, paint); y += 58f }
        paint.textSize = 30f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("— ${motivation.author}", 60f, minOf(y + 20, 480f), paint)
        return output
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines += current; current = word
            } else current = candidate
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }
}
