package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Dedup: evita registrar dos veces la misma notificación. */
@Entity(tableName = "processed_notification_keys", indices = [Index("notificationKey", unique = true)])
data class ProcessedNotificationKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationKey: String,
    val processedAt: Instant = Instant.now(),
)
