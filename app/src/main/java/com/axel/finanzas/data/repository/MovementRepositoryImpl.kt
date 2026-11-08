package com.axel.finanzas.data.repository

import androidx.room.withTransaction
import com.axel.finanzas.core.database.AppDatabase
import com.axel.finanzas.core.database.dao.AccountDao
import com.axel.finanzas.core.database.dao.MovementDao
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.domain.repository.MovementRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class MovementRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val movementDao: MovementDao,
    private val accountDao: AccountDao,
) : MovementRepository {

    override fun observeAll(): Flow<List<MovementEntity>> = movementDao.observeAll()

    override suspend fun getById(id: Long): MovementEntity? = movementDao.getById(id)

    override suspend fun addMovement(movement: MovementEntity): Long = database.withTransaction {
        val id = movementDao.insert(movement)
        applyBalanceEffect(movement)
        id
    }

    override suspend fun updateMovement(old: MovementEntity, new: MovementEntity) = database.withTransaction {
        reverseBalanceEffect(old)
        movementDao.update(new)
        applyBalanceEffect(new)
    }

    override suspend fun deleteMovement(movement: MovementEntity) = database.withTransaction {
        movementDao.delete(movement)
        reverseBalanceEffect(movement)
    }

    override suspend fun recordBalanceAdjustment(accountId: Long, newBalanceCents: Long, note: String?): Long =
        database.withTransaction {
            val account = accountDao.getById(accountId)
                ?: error("Cuenta $accountId no existe")
            accountDao.setBalance(accountId, newBalanceCents)
            movementDao.insert(
                MovementEntity(
                    accountId = accountId,
                    type = MovementType.BALANCE_ADJUSTMENT,
                    amountCents = Math.abs(newBalanceCents - account.currentBalanceCents),
                    previousBalanceCents = account.currentBalanceCents,
                    newBalanceCents = newBalanceCents,
                    occurredAt = Instant.now(),
                    note = note,
                ),
            )
        }

    /**
     * Efecto en saldo. Para cuentas CASH/DEBIT/WALLET el saldo ES el
     * dinero que tienes (gasto resta, ingreso suma). Para CREDIT el saldo
     * representa deuda usada, así que un cargo (EXPENSE) la SUMA en vez
     * de restarla.
     */
    private suspend fun applyBalanceEffect(movement: MovementEntity) {
        val isCredit = accountDao.getById(movement.accountId)?.type == AccountType.CREDIT
        when (movement.type) {
            MovementType.EXPENSE -> accountDao.adjustBalance(
                movement.accountId,
                if (isCredit) movement.amountCents else -movement.amountCents,
            )
            MovementType.INCOME -> accountDao.adjustBalance(
                movement.accountId,
                if (isCredit) -movement.amountCents else movement.amountCents,
            )
            MovementType.TRANSFER -> {
                accountDao.adjustBalance(movement.accountId, -movement.amountCents)
                movement.linkedAccountId?.let { accountDao.adjustBalance(it, movement.amountCents) }
            }
            MovementType.CC_PAYMENT -> {
                // accountId = la tarjeta que recibe el pago (baja su deuda).
                accountDao.adjustBalance(movement.accountId, -movement.amountCents)
                // linkedAccountId = de dónde salió el dinero, si se conoce (baja ese saldo).
                movement.linkedAccountId?.let { accountDao.adjustBalance(it, -movement.amountCents) }
            }
            MovementType.BALANCE_ADJUSTMENT -> Unit // el saldo ya se fija directo en recordBalanceAdjustment
        }
    }

    private suspend fun reverseBalanceEffect(movement: MovementEntity) {
        val isCredit = accountDao.getById(movement.accountId)?.type == AccountType.CREDIT
        when (movement.type) {
            MovementType.EXPENSE -> accountDao.adjustBalance(
                movement.accountId,
                if (isCredit) -movement.amountCents else movement.amountCents,
            )
            MovementType.INCOME -> accountDao.adjustBalance(
                movement.accountId,
                if (isCredit) movement.amountCents else -movement.amountCents,
            )
            MovementType.TRANSFER -> {
                accountDao.adjustBalance(movement.accountId, movement.amountCents)
                movement.linkedAccountId?.let { accountDao.adjustBalance(it, -movement.amountCents) }
            }
            MovementType.CC_PAYMENT -> {
                accountDao.adjustBalance(movement.accountId, movement.amountCents)
                movement.linkedAccountId?.let { accountDao.adjustBalance(it, movement.amountCents) }
            }
            MovementType.BALANCE_ADJUSTMENT -> {
                // Revertir un ajuste manual significa regresar al saldo anterior que guardó.
                movement.previousBalanceCents?.let { accountDao.setBalance(movement.accountId, it) }
            }
        }
    }
}
