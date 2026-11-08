package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.FuturePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuturePaymentDao {
    @Query("SELECT * FROM future_payments WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun observeUnpaid(): Flow<List<FuturePaymentEntity>>

    @Query("SELECT * FROM future_payments WHERE id = :id")
    suspend fun getById(id: Long): FuturePaymentEntity?

    @Insert
    suspend fun insert(entity: FuturePaymentEntity): Long

    @Update
    suspend fun update(entity: FuturePaymentEntity)
}
