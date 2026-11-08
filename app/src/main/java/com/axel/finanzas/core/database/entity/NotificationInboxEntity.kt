package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class NotificationInboxStatus { PENDING, RESOLVED, DISCARDED }

/** Notificación que no se pudo interpretar (sin regla, o la regla no capturó todo). */
@Entity(tableName = "notification_inbox")
data class NotificationInboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val rawText: String,
    val postedAt: Instant,
    val status: NotificationInboxStatus = NotificationInboxStatus.PENDING,
    val suggestedAccountId: Long? = null,
    val suggestedAmountCents: Long? = null,
    val resolvedMovementId: Long? = null,
)
