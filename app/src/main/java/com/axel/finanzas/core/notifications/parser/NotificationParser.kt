package com.axel.finanzas.core.notifications.parser

import com.axel.finanzas.core.database.entity.NotificationRuleEntity

/**
 * Interpreta el texto de una notificación según una
 * [NotificationRuleEntity]. Función pura (sin Android, sin Room) para
 * que sea fácil de probar — ver `NotificationParserTest`, que usa
 * formatos reales confirmados de notificaciones de Nu, BBVA, Mercado
 * Pago, RappiCard y Citibanamex.
 */
object NotificationParser {

    fun parse(text: String, rule: NotificationRuleEntity): ParsedNotification? {
        val amountMatch = safeFind(rule.amountRegex, text) ?: return null
        val amountCents = amountMatch.groupValues.getOrNull(1)?.let(::parseAmountToCents) ?: return null

        val merchant = rule.merchantRegex
            ?.let { safeFind(it, text) }
            ?.groupValues?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val last4 = rule.last4Regex
            ?.let { safeFind(it, text) }
            ?.groupValues?.getOrNull(1)
            ?.takeIf { it.length == 4 && it.all(Char::isDigit) }

        val balanceCents = rule.balanceRegex
            ?.let { safeFind(it, text) }
            ?.groupValues?.getOrNull(1)
            ?.let(::parseAmountToCents)

        return ParsedNotification(
            amountCents = amountCents,
            kind = rule.kind,
            merchant = merchant,
            last4 = last4,
            balanceCents = balanceCents,
        )
    }

    private fun safeFind(pattern: String, text: String): MatchResult? =
        runCatching { Regex(pattern, RegexOption.IGNORE_CASE).find(text) }.getOrNull()

    /** "$1,234.56" / "1234.56" / "1,200.00" -> centavos. Nunca negativo. */
    private fun parseAmountToCents(raw: String): Long? {
        val cleaned = raw.replace(",", "").replace("$", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100)
    }
}
