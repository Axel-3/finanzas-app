package com.axel.finanzas.domain.repository

import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.domain.model.CardUi
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    /** Cuentas DEBIT + CREDIT (activas e inactivas) con sus datos de tarjeta combinados. */
    fun observeCards(): Flow<List<CardUi>>

    suspend fun createDebitCard(alias: String, bank: String, last4: String, initialBalanceCents: Long): Long

    suspend fun createCreditCard(
        alias: String,
        bank: String,
        last4: String,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
        initialBalanceCents: Long,
    ): Long

    suspend fun updateCreditCardDetails(
        accountId: Long,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
        minPaymentCents: Long?,
        noInterestPaymentCents: Long?,
    )

    /** Alias/banco/últimos 4 — igual para débito y crédito. */
    suspend fun updateCardBasics(accountId: Long, alias: String, bank: String, last4: String)

    /**
     * Límite/corte/fecha límite/alerta desde la pantalla de edición —
     * conserva `minPaymentCents`/`noInterestPaymentCents` existentes (se
     * capturan aparte, por corte; esta pantalla no los toca).
     */
    suspend fun updateCreditLimits(
        accountId: Long,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
    )

    /**
     * Borra la tarjeta por completo SOLO si no tiene movimientos propios
     * (`accountId`, no `linkedAccountId` — ese se desvincula solo, ver
     * [com.axel.finanzas.core.database.entity.MovementEntity]). Devuelve
     * `false` sin borrar nada si tiene movimientos; en ese caso la UI debe
     * ofrecer [setActive] (desactivar) en vez de borrar.
     */
    suspend fun deleteCard(accountId: Long): Boolean

    /** Para que el parser de notificaciones asigne un cargo a su tarjeta. */
    suspend fun findActiveByLast4(last4: String): AccountEntity?

    suspend fun setActive(accountId: Long, isActive: Boolean)

    /** Agrega un número de tarjeta extra (física/digital) que comparte el saldo de [accountId]. */
    suspend fun addExtraLast4(accountId: Long, last4: String, label: String?): Long

    suspend fun removeExtraLast4(id: Long)
}
