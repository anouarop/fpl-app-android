package com.shellanddeploy.fpllive.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shellanddeploy.fpllive.FplApp
import com.shellanddeploy.fpllive.R
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.domain.model.Gameweek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Periodic worker that reminds the user of an upcoming gameweek deadline. It runs roughly every
 * six hours and only posts a notification when a deadline is within the next 24 hours and hasn't
 * already been notified (tracked in SharedPreferences).
 */
class GameweekReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as FplApp
        val result = app.repository.bootstrap()
        val bootstrap = (result as? FetchResult.Success)?.data ?: return Result.retry()

        val upcoming = bootstrap.gameweeks.firstOrNull { !it.finished && !it.isCurrent }
            ?: bootstrap.currentGameweek
            ?: return Result.success()

        val now = System.currentTimeMillis()
        val deadline = upcoming.deadlineTimeEpoch * 1000L
        val hoursToDeadline = (deadline - now) / 3_600_000L

        if (deadline <= now || hoursToDeadline > 24) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_NOTIFIED_DEADLINE, 0L) == deadline) return Result.success()

        if (!canPostNotifications(applicationContext)) return Result.success()

        createChannel(applicationContext)
        val title = "${upcoming.name} deadline approaching"
        val text = "The deadline is ${formatDeadline(upcoming)}. Don't forget to save your team!"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        prefs.edit().putLong(KEY_NOTIFIED_DEADLINE, deadline).apply()
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "gameweek_reminders"
        const val NOTIFICATION_ID = 1001
        const val UNIQUE_WORK_NAME = "gameweek_reminders"
        private const val PREFS = "fpllive_reminders"
        private const val KEY_NOTIFIED_DEADLINE = "notified_deadline"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gameweek reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Reminders before FPL gameweek deadlines" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        fun canPostNotifications(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

        private fun formatDeadline(gameweek: Gameweek): String = runCatching {
            Instant.ofEpochMilli(gameweek.deadlineTimeEpoch * 1000L)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm"))
        }.getOrElse { gameweek.deadlineTime }
    }
}
