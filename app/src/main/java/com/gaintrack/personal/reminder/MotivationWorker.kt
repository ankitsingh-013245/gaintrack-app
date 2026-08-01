package com.gaintrack.personal.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gaintrack.personal.BuildConfig
import com.gaintrack.personal.GainTrackApplication
import com.gaintrack.personal.data.Motivation
import com.gaintrack.personal.data.SeedPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MotivationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as GainTrackApplication
        val fallback = SeedPlan.fallbackMotivations[java.time.LocalDate.now().dayOfYear % SeedPlan.fallbackMotivations.size]
        val quote = fetchQuote() ?: fallback
        val photo = fetchPexelsPhoto() ?: fallbackPhoto()
        val motivation = quote.copy(imageUrl = photo.first, imageAuthor = photo.second)
        app.repository.setMotivation(motivation)
        if (app.repository.state.value.dailyNotificationEnabled) {
            NotificationHelper.showMotivation(applicationContext, motivation)
        }
        Result.success()
    }

    private fun fetchQuote(): Motivation? = runCatching {
        val connection = URL("https://zenquotes.io/api/random").openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("User-Agent", "GainTrack/1.0")
        val json = JSONArray(connection.inputStream.bufferedReader().use { it.readText() }).getJSONObject(0)
        Motivation(json.getString("q"), json.getString("a"))
    }.getOrNull()

    private fun fetchPexelsPhoto(): Pair<String, String>? {
        if (BuildConfig.PEXELS_API_KEY.isBlank()) return null
        return runCatching {
            val connection = URL("https://api.pexels.com/v1/search?query=fitness%20motivation&orientation=landscape&per_page=15")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Authorization", BuildConfig.PEXELS_API_KEY)
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val photos = root.getJSONArray("photos")
            val photo = photos.getJSONObject(java.time.LocalDate.now().dayOfYear % photos.length())
            photo.getJSONObject("src").getString("large") to photo.getString("photographer")
        }.getOrNull()
    }

    private fun fallbackPhoto(): Pair<String, String> {
        val photos = listOf(
            "https://images.pexels.com/photos/841130/pexels-photo-841130.jpeg?auto=compress&cs=tinysrgb&w=1200" to "Victor Freitas",
            "https://images.pexels.com/photos/1552252/pexels-photo-1552252.jpeg?auto=compress&cs=tinysrgb&w=1200" to "Leon Ardho",
            "https://images.pexels.com/photos/2261482/pexels-photo-2261482.jpeg?auto=compress&cs=tinysrgb&w=1200" to "Li Sun",
        )
        return photos[java.time.LocalDate.now().dayOfYear % photos.size]
    }
}

object MotivationScheduler {
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(6, 30)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<MotivationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_motivation", ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }
}
