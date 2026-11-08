package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Tipo completo definido en el esquema, aunque la UI de "Cuentas" solo
 * deja crear CASH/DEBIT/WALLET/SAVINGS directamente. CREDIT se habilita
 * desde la pantalla de Tarjetas, que además le agrega su fila 1:1 en
 * `credit_card_details`.
 *
 * SAVINGS es una cuenta de saldo libre (se le mete y se le saca dinero
 * con movimientos normales) — a diferencia de `savings_goals`, que
 * exige un monto objetivo y solo deja aportar, nunca retirar.
 */
enum class AccountType { CASH, DEBIT, CREDIT, WALLET, SAVINGS }

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val bank: String? = null,
    val last4: String? = null,
    val currentBalanceCents: Long = 0,
    val colorHex: String = "#4F7A6C",
    val icon: String = "wallet",
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
)
