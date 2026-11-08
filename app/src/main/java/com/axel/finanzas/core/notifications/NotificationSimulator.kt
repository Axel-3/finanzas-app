package com.axel.finanzas.core.notifications

import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.NotificationRuleEntity
import com.axel.finanzas.core.database.entity.NotificationRuleKind
import com.axel.finanzas.core.notifications.parser.NotificationParser
import com.axel.finanzas.domain.repository.CardRepository

/**
 * Corre EXACTAMENTE la misma lógica de decisión que
 * [FinanzasNotificationListenerService.onNotificationPosted] (qué regla
 * matchea, si hay una tarjeta activa con ese last4, si el tipo de
 * movimiento tiene sentido para esa cuenta) pero de SOLO LECTURA — no
 * crea movimientos ni entradas en "Por revisar". Pensado para pegar
 * texto real de notificaciones desde "Reglas de notificaciones" y
 * confirmar que una regla lo interpreta bien, sin arriesgar la base de
 * datos real escribiendo algo de prueba.
 */
object NotificationSimulator {

    sealed interface Outcome {
        /** Ninguna regla activa de ese paquete logró interpretar el texto. */
        data class SinRegla(val reglasProbadas: Int) : Outcome

        /** Una regla matcheó, pero caería a "Por revisar" igual que en producción. */
        data class PorRevisar(
            val bankName: String,
            val amountCents: Long,
            val kind: NotificationRuleKind,
            val merchant: String?,
            val last4: String?,
            val razon: String,
        ) : Outcome

        /** Se asignaría solo a una cuenta existente, sin pasar por "Por revisar". */
        data class SeAsignariaSolo(
            val bankName: String,
            val amountCents: Long,
            val kind: NotificationRuleKind,
            val accountName: String,
        ) : Outcome
    }

    suspend fun simulate(
        text: String,
        rules: List<NotificationRuleEntity>,
        cardRepository: CardRepository,
    ): Outcome {
        var matchedRule: NotificationRuleEntity? = null
        var parsed: com.axel.finanzas.core.notifications.parser.ParsedNotification? = null
        for (rule in rules) {
            val result = NotificationParser.parse(text, rule)
            if (result != null) {
                matchedRule = rule
                parsed = result
                break
            }
        }
        val rule = matchedRule ?: return Outcome.SinRegla(rules.size)
        val result = requireNotNull(parsed)

        val last4 = result.last4
        val account = last4?.let { cardRepository.findActiveByLast4(it) }
        if (account == null) {
            val razon = if (last4 == null) {
                "Este formato no trae últimos 4 dígitos"
            } else {
                "Ninguna tarjeta activa termina en $last4"
            }
            return Outcome.PorRevisar(rule.bankName, result.amountCents, result.kind, result.merchant, last4, razon)
        }

        val typeTieneSentido = when (result.kind) {
            NotificationRuleKind.CARGO -> true
            NotificationRuleKind.DEPOSITO -> account.type != AccountType.CREDIT
            NotificationRuleKind.PAGO_TARJETA, NotificationRuleKind.DEVOLUCION -> true
        }
        if (!typeTieneSentido) {
            return Outcome.PorRevisar(
                rule.bankName,
                result.amountCents,
                result.kind,
                result.merchant,
                last4,
                "Ese tipo de movimiento no tiene sentido para \"${account.name}\"",
            )
        }

        return Outcome.SeAsignariaSolo(rule.bankName, result.amountCents, result.kind, account.name)
    }
}
