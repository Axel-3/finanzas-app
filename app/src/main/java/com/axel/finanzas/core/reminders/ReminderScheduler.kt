package com.axel.finanzas.core.reminders

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "payment_reminder_daily"

fun scheduleDaily(workManager: WorkManager) {
    val request = PeriodicWorkRequestBuilder<PaymentReminderWorker>(1, TimeUnit.DAYS).build()
    workManager.enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
