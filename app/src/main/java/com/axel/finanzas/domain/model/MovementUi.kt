package com.axel.finanzas.domain.model

import com.axel.finanzas.core.database.entity.MovementType
import java.time.Instant

/**
 * [com.axel.finanzas.core.database.entity.MovementEntity] solo guarda
 * ids; esto es lo mismo pero con los nombres de cuenta/categoría ya
 * resueltos, para no hacer ese join en cada pantalla.
 */
data class MovementUi(
    val id: Long,
    val type: MovementType,
    val amountCents: Long,
    val merchant: String?,
    val categoryName: String?,
    val categoryIconKey: String?,
    val accountId: Long,
    val accountName: String,
    val linkedAccountName: String?,
    val occurredAt: Instant,
    val note: String?,
) {
    val isExpense: Boolean get() = type == MovementType.EXPENSE
}
