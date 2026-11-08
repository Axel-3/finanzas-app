package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.MovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {

    @Query("SELECT * FROM movements ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE accountId = :accountId OR linkedAccountId = :accountId ORDER BY occurredAt DESC, id DESC")
    fun observeByAccount(accountId: Long): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun getById(id: Long): MovementEntity?

    @Insert
    suspend fun insert(movement: MovementEntity): Long

    @Update
    suspend fun update(movement: MovementEntity)

    @Delete
    suspend fun delete(movement: MovementEntity)

    /** Para presupuestos y reportes: gasto agrupado por categoría en una ventana de fechas. */
    @Query(
        """
        SELECT categoryId as categoryId, SUM(amountCents) as totalCents
        FROM movements
        WHERE type = 'EXPENSE' AND occurredAt BETWEEN :startMillis AND :endMillis
        GROUP BY categoryId
        """,
    )
    fun observeCategorySpending(startMillis: Long, endMillis: Long): Flow<List<CategorySpending>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM movements WHERE type = 'EXPENSE' AND occurredAt BETWEEN :startMillis AND :endMillis")
    fun observeTotalExpense(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM movements WHERE type = 'INCOME' AND occurredAt BETWEEN :startMillis AND :endMillis")
    fun observeTotalIncome(startMillis: Long, endMillis: Long): Flow<Long>

    /** Para decidir si una cuenta se puede borrar de verdad o solo desactivar (ver [com.axel.finanzas.domain.repository.CardRepository.deleteCard]). */
    @Query("SELECT COUNT(*) FROM movements WHERE accountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int
}
