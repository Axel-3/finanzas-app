package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.RecurringPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun observeActive(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    suspend fun getById(id: Long): RecurringPaymentEntity?

    @Insert
    suspend fun insert(entity: RecurringPaymentEntity): Long

    @Update
    suspend fun update(entity: RecurringPaymentEntity)

    @Query("UPDATE recurring_payments SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
