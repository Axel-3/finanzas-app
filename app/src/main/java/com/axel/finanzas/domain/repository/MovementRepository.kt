package com.axel.finanzas.domain.repository

import com.axel.finanzas.core.database.entity.MovementEntity
import kotlinx.coroutines.flow.Flow

interface MovementRepository {
    fun observeAll(): Flow<List<MovementEntity>>
    suspend fun getById(id: Long): MovementEntity?

    /**
     * Inserta el movimiento Y aplica su efecto en el saldo de la(s)
     * cuenta(s) involucradas, en una sola transacción.
     */
    suspend fun addMovement(movement: MovementEntity): Long

    /** Revierte el efecto del movimiento viejo antes de aplicar el nuevo. */
    suspend fun updateMovement(old: MovementEntity, new: MovementEntity)

    /** Borra el movimiento y revierte su efecto en el saldo. */
    suspend fun deleteMovement(movement: MovementEntity)

    /**
     * Ajuste manual de saldo (ej. corregir el usado de una tarjeta a
     * mano). Pone el saldo directo en el valor nuevo y deja un
     * [com.axel.finanzas.core.database.entity.MovementType.BALANCE_ADJUSTMENT]
     * con el antes/después para trazabilidad. No cuenta como gasto/ingreso.
     */
    suspend fun recordBalanceAdjustment(accountId: Long, newBalanceCents: Long, note: String?): Long
}
