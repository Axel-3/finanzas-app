package com.axel.finanzas.data.repository

import com.axel.finanzas.core.database.dao.AccountDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun observeActive(): Flow<List<AccountEntity>> = accountDao.observeActive()
    override fun observeAll(): Flow<List<AccountEntity>> = accountDao.observeAll()
    override suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)
    override suspend fun create(account: AccountEntity): Long = accountDao.insert(account)
    override suspend fun update(account: AccountEntity) = accountDao.update(account)
    override suspend fun deactivate(id: Long) = accountDao.deactivate(id)
    override suspend fun reactivate(id: Long) = accountDao.reactivate(id)
}
