package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.axel.finanzas.core.database.entity.CreditCardDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDetailsDao {
    @Query("SELECT * FROM credit_card_details WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): CreditCardDetailsEntity?

    @Query("SELECT * FROM credit_card_details")
    fun observeAll(): Flow<List<CreditCardDetailsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(details: CreditCardDetailsEntity)

    @Update
    suspend fun update(details: CreditCardDetailsEntity)
}
