package com.axel.finanzas.core.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.axel.finanzas.core.designsystem.components.formatCents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "recordatorios_pagos"

@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorios de pagos",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos antes de que venza un pago recurrente o futuro"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun notifyPaymentDue(notificationId: Int, name: String, amountCents: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pago próximo: $name")
            .setContentText("Vence pronto · ${formatCents(amountCents)}")
            .setAutoCancel(true)
            .build()

        // POST_NOTIFICATIONS puede no estar concedido en Android 13+; si no
        // lo está, simplemente no se muestra (nada que romper aquí, el
        // recordatorio no es crítico para el funcionamiento de la app).
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
