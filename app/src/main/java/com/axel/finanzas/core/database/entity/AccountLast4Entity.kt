package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Últimos 4 dígitos ADICIONALES que comparten el saldo de una cuenta: un
 * mismo banco a veces da tarjeta física Y digital (o hasta una tercera)
 * para la MISMA cuenta/saldo, cada una con su propio número pero
 * descontando del mismo lugar. El `last4` "titular" sigue viviendo en
 * [AccountEntity.last4] (el que se ve en la tarjeta); esta tabla son los
 * números EXTRA que deben resolver al mismo `accountId` cuando
 * [com.axel.finanzas.core.database.dao.AccountDao.findActiveByLast4]
 * busca a cuál tarjeta asignar una notificación.
 */
@Entity(
    tableName = "account_last4s",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("last4", unique = true)],
)
data class AccountLast4Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val last4: String,
    /** Opcional, para distinguir cuál es cuál (ej. "Física", "Digital"). */
    val label: String? = null,
)
