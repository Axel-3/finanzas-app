package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.NotificationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules ORDER BY bankName ASC")
    fun observeAll(): Flow<List<NotificationRuleEntity>>

    @Query("SELECT * FROM notification_rules WHERE packageName = :packageName AND isActive = 1 ORDER BY priority DESC")
    suspend fun getActiveForPackage(packageName: String): List<NotificationRuleEntity>

    @Insert
    suspend fun insert(rule: NotificationRuleEntity): Long

    @Update
    suspend fun update(rule: NotificationRuleEntity)

    @Delete
    suspend fun delete(rule: NotificationRuleEntity)

    @Query("SELECT COUNT(*) FROM notification_rules")
    suspend fun count(): Int

    /** Usado solo para resembrar [com.axel.finanzas.core.database.DefaultBankRules] cuando sube de versión — ver [com.axel.finanzas.core.database.DefaultDataSeeder]. */
    @Query("DELETE FROM notification_rules")
    suspend fun deleteAll()
}
