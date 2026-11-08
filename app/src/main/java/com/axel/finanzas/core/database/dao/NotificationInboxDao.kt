package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.NotificationInboxEntity
import com.axel.finanzas.core.database.entity.NotificationInboxStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationInboxDao {
    @Query("SELECT * FROM notification_inbox WHERE status = 'PENDING' ORDER BY postedAt DESC")
    fun observePending(): Flow<List<NotificationInboxEntity>>

    @Query("SELECT COUNT(*) FROM notification_inbox WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(entry: NotificationInboxEntity): Long

    @Update
    suspend fun update(entry: NotificationInboxEntity)

    @Query("UPDATE notification_inbox SET status = :status, resolvedMovementId = :movementId WHERE id = :id")
    suspend fun resolve(id: Long, status: NotificationInboxStatus, movementId: Long?)
}
