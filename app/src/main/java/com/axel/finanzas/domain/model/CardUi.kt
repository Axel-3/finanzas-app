package com.axel.finanzas.domain.model

import com.axel.finanzas.core.database.entity.AccountType

/** Un número de tarjeta extra (física/digital/etc.) que comparte el saldo de la misma [CardUi]. */
data class CardExtraLast4(val id: Long, val last4: String, val label: String?)

/** [com.axel.finanzas.core.database.entity.AccountEntity] + sus datos de
 * tarjeta de crédito si aplica, ya combinados para la UI. */
data class CardUi(
    val accountId: Long,
    val alias: String,
    val bank: String?,
    val last4: String?,
    val type: AccountType,
    val balanceCents: Long,
    val isActive: Boolean,
    val creditLimitCents: Long? = null,
    val cutOffDay: Int? = null,
    val paymentDueDay: Int? = null,
    val alertThresholdPercent: Int? = null,
    val extraLast4s: List<CardExtraLast4> = emptyList(),
) {
    val isCredit: Boolean get() = type == AccountType.CREDIT

    val usagePercent: Float
        get() = if (isCredit && creditLimitCents != null && creditLimitCents > 0) {
            (balanceCents.toFloat() / creditLimitCents.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isOverAlertThreshold: Boolean
        get() = isCredit && alertThresholdPercent != null && usagePercent >= (alertThresholdPercent / 100f)
}
