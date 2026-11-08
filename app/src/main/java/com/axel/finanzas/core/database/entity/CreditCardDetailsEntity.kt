package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 1:1 con `accounts` donde `type = CREDIT`. Separado de [AccountEntity]
 * para no llenar de columnas nulas las cuentas que no son tarjetas de
 * crédito (débito/efectivo/billetera).
 */
@Entity(
    tableName = "credit_card_details",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CreditCardDetailsEntity(
    @PrimaryKey val accountId: Long,
    val creditLimitCents: Long,
    val cutOffDay: Int,
    val paymentDueDay: Int,
    val alertThresholdPercent: Int = 70,
    val noInterestPaymentCents: Long? = null,
    val minPaymentCents: Long? = null,
    val lastStatementDate: Instant? = null,
)
