package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * TRANSFER y CC_PAYMENT no cuentan como gasto/ingreso en reportes;
 * BALANCE_ADJUSTMENT tampoco. Esos dos últimos se habilitan por completo
 * hasta la Fase 3 (necesitan tarjetas), pero el tipo ya existe aquí para
 * no tener que migrar el esquema otra vez.
 */
enum class MovementType { EXPENSE, INCOME, TRANSFER, CC_PAYMENT, BALANCE_ADJUSTMENT }

enum class MovementSource { MANUAL, NOTIFICATION }

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("accountId"), Index("linkedAccountId"), Index("categoryId"), Index("occurredAt")],
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val linkedAccountId: Long? = null,
    val categoryId: Long? = null,
    val type: MovementType,
    val amountCents: Long,
    val merchant: String? = null,
    val occurredAt: Instant,
    val source: MovementSource = MovementSource.MANUAL,
    val rawNotificationText: String? = null,
    val previousBalanceCents: Long? = null,
    val newBalanceCents: Long? = null,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
