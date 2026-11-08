package com.axel.finanzas.data.repository

import com.axel.finanzas.core.database.dao.AccountDao
import com.axel.finanzas.core.database.dao.AccountLast4Dao
import com.axel.finanzas.core.database.dao.CreditCardDetailsDao
import com.axel.finanzas.core.database.dao.MovementDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.AccountLast4Entity
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.CreditCardDetailsEntity
import com.axel.finanzas.domain.model.CardExtraLast4
import com.axel.finanzas.domain.model.CardUi
import com.axel.finanzas.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val creditCardDetailsDao: CreditCardDetailsDao,
    private val movementDao: MovementDao,
    private val accountLast4Dao: AccountLast4Dao,
) : CardRepository {

    override fun observeCards(): Flow<List<CardUi>> =
        combine(
            accountDao.observeAll(),
            creditCardDetailsDao.observeAll(),
            accountLast4Dao.observeAll(),
        ) { accounts, details, extraLast4s ->
            val detailsByAccountId = details.associateBy { it.accountId }
            val extrasByAccountId = extraLast4s.groupBy { it.accountId }
            accounts
                .filter { it.type == AccountType.DEBIT || it.type == AccountType.CREDIT }
                .map { account ->
                    val cardDetails = detailsByAccountId[account.id]
                    CardUi(
                        accountId = account.id,
                        alias = account.name,
                        bank = account.bank,
                        last4 = account.last4,
                        type = account.type,
                        balanceCents = account.currentBalanceCents,
                        isActive = account.isActive,
                        creditLimitCents = cardDetails?.creditLimitCents,
                        cutOffDay = cardDetails?.cutOffDay,
                        paymentDueDay = cardDetails?.paymentDueDay,
                        alertThresholdPercent = cardDetails?.alertThresholdPercent,
                        extraLast4s = extrasByAccountId[account.id]?.map {
                            CardExtraLast4(id = it.id, last4 = it.last4, label = it.label)
                        } ?: emptyList(),
                    )
                }
        }

    override suspend fun createDebitCard(alias: String, bank: String, last4: String, initialBalanceCents: Long): Long =
        accountDao.insert(
            AccountEntity(
                name = alias,
                type = AccountType.DEBIT,
                bank = bank,
                last4 = last4,
                currentBalanceCents = initialBalanceCents,
                icon = "card",
            ),
        )

    override suspend fun createCreditCard(
        alias: String,
        bank: String,
        last4: String,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
        initialBalanceCents: Long,
    ): Long {
        val accountId = accountDao.insert(
            AccountEntity(
                name = alias,
                type = AccountType.CREDIT,
                bank = bank,
                last4 = last4,
                currentBalanceCents = initialBalanceCents,
                icon = "card",
            ),
        )
        creditCardDetailsDao.upsert(
            CreditCardDetailsEntity(
                accountId = accountId,
                creditLimitCents = limitCents,
                cutOffDay = cutOffDay,
                paymentDueDay = paymentDueDay,
                alertThresholdPercent = alertThresholdPercent,
            ),
        )
        return accountId
    }

    override suspend fun updateCreditCardDetails(
        accountId: Long,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
        minPaymentCents: Long?,
        noInterestPaymentCents: Long?,
    ) {
        creditCardDetailsDao.upsert(
            CreditCardDetailsEntity(
                accountId = accountId,
                creditLimitCents = limitCents,
                cutOffDay = cutOffDay,
                paymentDueDay = paymentDueDay,
                alertThresholdPercent = alertThresholdPercent,
                minPaymentCents = minPaymentCents,
                noInterestPaymentCents = noInterestPaymentCents,
            ),
        )
    }

    override suspend fun updateCardBasics(accountId: Long, alias: String, bank: String, last4: String) {
        val account = accountDao.getById(accountId) ?: return
        accountDao.update(account.copy(name = alias, bank = bank, last4 = last4))
    }

    override suspend fun updateCreditLimits(
        accountId: Long,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
    ) {
        val existing = creditCardDetailsDao.getByAccountId(accountId)
        creditCardDetailsDao.upsert(
            CreditCardDetailsEntity(
                accountId = accountId,
                creditLimitCents = limitCents,
                cutOffDay = cutOffDay,
                paymentDueDay = paymentDueDay,
                alertThresholdPercent = alertThresholdPercent,
                minPaymentCents = existing?.minPaymentCents,
                noInterestPaymentCents = existing?.noInterestPaymentCents,
                lastStatementDate = existing?.lastStatementDate,
            ),
        )
    }

    override suspend fun deleteCard(accountId: Long): Boolean {
        if (movementDao.countForAccount(accountId) > 0) return false
        accountDao.delete(accountId)
        return true
    }

    override suspend fun findActiveByLast4(last4: String): AccountEntity? =
        accountDao.findActiveByLast4(last4)

    override suspend fun setActive(accountId: Long, isActive: Boolean) {
        if (isActive) accountDao.reactivate(accountId) else accountDao.deactivate(accountId)
    }

    override suspend fun addExtraLast4(accountId: Long, last4: String, label: String?): Long =
        accountLast4Dao.insert(AccountLast4Entity(accountId = accountId, last4 = last4, label = label))

    override suspend fun removeExtraLast4(id: Long) {
        accountLast4Dao.delete(id)
    }
}
