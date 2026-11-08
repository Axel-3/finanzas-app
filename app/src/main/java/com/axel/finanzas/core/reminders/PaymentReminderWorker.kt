package com.axel.finanzas.core.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.axel.finanzas.core.database.dao.FuturePaymentDao
import com.axel.finanzas.core.database.dao.RecurringPaymentDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Corre una vez al día (ver [scheduleDaily]). Revisa recurrentes y pagos
 * futuros cuya fecha límite cae dentro de su `reminderDaysBefore` y
 * manda una notificación local por cada uno.
 */
@HiltWorker
class PaymentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recurringDao: RecurringPaymentDao,
    private val futurePaymentDao: FuturePaymentDao,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        recurringDao.observeActive().first().forEach { payment ->
            val daysUntilDue = today.until(payment.nextDueDate).days
            if (daysUntilDue in 0..payment.reminderDaysBefore) {
                notifier.notifyPaymentDue(
                    notificationId = "recurring-${payment.id}".hashCode(),
                    name = payment.name,
                    amountCents = payment.amountCents,
                )
            }
        }

        futurePaymentDao.observeUnpaid().first().forEach { payment ->
            val daysUntilDue = today.until(payment.dueDate).days
            if (daysUntilDue in 0..payment.reminderDaysBefore) {
                notifier.notifyPaymentDue(
                    notificationId = "future-${payment.id}".hashCode(),
                    name = payment.name,
                    amountCents = payment.amountCents,
                )
            }
        }

        return Result.success()
    }
}
