package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    /**
     * Matchea tanto el last4 "titular" (`accounts.last4`) como cualquier
     * last4 extra de tarjeta física/digital que comparta el mismo saldo
     * (`account_last4s` — ver [com.axel.finanzas.core.database.entity.AccountLast4Entity]).
     */
    @Query(
        """
        SELECT * FROM accounts WHERE isActive = 1 AND (
            last4 = :last4 OR id IN (SELECT accountId FROM account_last4s WHERE last4 = :last4)
        ) LIMIT 1
        """,
    )
    suspend fun findActiveByLast4(last4: String): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    /** Baja lógica: nunca se borra una cuenta con movimientos asociados (FK RESTRICT). */
    @Query("UPDATE accounts SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("UPDATE accounts SET isActive = 1 WHERE id = :id")
    suspend fun reactivate(id: Long)

    @Query("UPDATE accounts SET currentBalanceCents = currentBalanceCents + :deltaCents WHERE id = :id")
    suspend fun adjustBalance(id: Long, deltaCents: Long)

    @Query("UPDATE accounts SET currentBalanceCents = :newBalanceCents WHERE id = :id")
    suspend fun setBalance(id: Long, newBalanceCents: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    /** Solo se llama cuando ya se confirmó que la cuenta no tiene movimientos propios (FK RESTRICT en `movements.accountId`). */
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)
}
