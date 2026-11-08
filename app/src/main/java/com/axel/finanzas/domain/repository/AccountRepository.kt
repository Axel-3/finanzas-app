package com.axel.finanzas.domain.repository

import com.axel.finanzas.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeActive(): Flow<List<AccountEntity>>
    fun observeAll(): Flow<List<AccountEntity>>
    suspend fun getById(id: Long): AccountEntity?
    suspend fun create(account: AccountEntity): Long
    suspend fun update(account: AccountEntity)
    suspend fun deactivate(id: Long)
    suspend fun reactivate(id: Long)
}
