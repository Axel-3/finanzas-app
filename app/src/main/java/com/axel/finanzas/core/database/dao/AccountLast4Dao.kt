package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.axel.finanzas.core.database.entity.AccountLast4Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountLast4Dao {
    @Query("SELECT * FROM account_last4s WHERE accountId = :accountId ORDER BY id ASC")
    fun observeForAccount(accountId: Long): Flow<List<AccountLast4Entity>>

    @Query("SELECT * FROM account_last4s")
    fun observeAll(): Flow<List<AccountLast4Entity>>

    @Insert
    suspend fun insert(entity: AccountLast4Entity): Long

    @Query("DELETE FROM account_last4s WHERE id = :id")
    suspend fun delete(id: Long)
}
