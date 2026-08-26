package com.shellanddeploy.fpllive.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules local reminders for upcoming FPL gameweek deadlines.
 *
 * The FPL public API has no push-notification capability, so reminders are scheduled locally via
 * WorkManager. [GameweekReminderWorker] runs periodically and posts a notification when a deadline
 * is within 24 hours.
 */
interface ReminderScheduler {
    fun setEnabled(enabled: Boolean)
}

object NoOpReminderScheduler : ReminderScheduler {
    override fun setEnabled(enabled: Boolean) = Unit
}

class WorkManagerReminderScheduler(private val context: Context) : ReminderScheduler {

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            GameweekReminderWorker.createChannel(context)
            val request = PeriodicWorkRequestBuilder<GameweekReminderWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                GameweekReminderWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(GameweekReminderWorker.UNIQUE_WORK_NAME)
        }
    }
}
