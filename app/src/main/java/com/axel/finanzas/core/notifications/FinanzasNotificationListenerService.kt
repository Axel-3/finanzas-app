package com.axel.finanzas.core.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.axel.finanzas.core.database.dao.NotificationInboxDao
import com.axel.finanzas.core.database.dao.NotificationRuleDao
import com.axel.finanzas.core.database.dao.ProcessedNotificationKeyDao
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementSource
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.NotificationInboxEntity
import com.axel.finanzas.core.database.entity.NotificationRuleKind
import com.axel.finanzas.core.database.entity.ProcessedNotificationKeyEntity
import com.axel.finanzas.core.notifications.parser.NotificationParser
import com.axel.finanzas.domain.repository.CardRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Escucha SOLO las apps elegidas en "Más -> Reglas de notificaciones"
 * ([NotificationSettingsRepository]). Requiere conceder el permiso
 * especial "Acceso a notificaciones" en Ajustes del sistema — eso no se
 * puede pedir con un diálogo normal.
 *
 * Flujo por notificación: ¿app monitoreada? -> ¿ya la procesamos? (dedup)
 * -> ¿hay una regla de ese banco que la interprete? -> ¿el last4
 * coincide con una tarjeta activa? -> ¿el tipo de movimiento se pudo
 * determinar? Si CUALQUIERA de esos pasos falla, la notificación cae a
 * "Por revisar" en vez de adivinar.
 */
@AndroidEntryPoint
class FinanzasNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var settingsRepository: NotificationSettingsRepository
    @Inject lateinit var ruleDao: NotificationRuleDao
    @Inject lateinit var inboxDao: NotificationInboxDao
    @Inject lateinit var processedKeyDao: ProcessedNotificationKeyDao
    @Inject lateinit var cardRepository: CardRepository
    @Inject lateinit var movementRepository: MovementRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val text = extractText(sbn) ?: return

        serviceScope.launch {
            val monitored = settingsRepository.monitoredPackages.first()
            if (packageName !in monitored) return@launch

            // El insert es la única fuente de verdad (atómico vía el índice
            // único + IGNORE) — un exists() aparte dejaba una ventana de
            // carrera si Android dispara onNotificationPosted dos veces casi
            // al mismo tiempo para la misma notificación (pasó de verdad:
            // BBVA y Nu llegaron duplicados en "Por revisar" el mismo ms).
            val dedupKey = "${sbn.key}:${text.hashCode()}"
            val insertedId = processedKeyDao.insert(ProcessedNotificationKeyEntity(notificationKey = dedupKey))
            if (insertedId == -1L) return@launch

            val rules = ruleDao.getActiveForPackage(packageName)
            val parsed = rules.firstNotNullOfOrNull { rule -> NotificationParser.parse(text, rule) }

            if (parsed == null) {
                sendToInbox(packageName, text, suggestedAccountId = null, suggestedAmountCents = null)
                return@launch
            }

            val account = parsed.last4?.let { cardRepository.findActiveByLast4(it) }
            if (account == null) {
                sendToInbox(packageName, text, suggestedAccountId = null, suggestedAmountCents = parsed.amountCents)
                return@launch
            }

            // El kind viene explícito de la regla (ver NotificationRuleKind) — ya
            // no se adivina por palabras clave. Aun así validamos que el tipo
            // tenga sentido para la cuenta encontrada antes de aplicarlo solo.
            val movementType = when (parsed.kind) {
                NotificationRuleKind.CARGO -> MovementType.EXPENSE
                NotificationRuleKind.DEPOSITO ->
                    if (account.type == AccountType.CREDIT) null else MovementType.INCOME
                NotificationRuleKind.PAGO_TARJETA, NotificationRuleKind.DEVOLUCION ->
                    if (account.type == AccountType.CREDIT) MovementType.CC_PAYMENT else MovementType.INCOME
            }

            if (movementType == null) {
                sendToInbox(packageName, text, suggestedAccountId = account.id, suggestedAmountCents = parsed.amountCents)
                return@launch
            }

            movementRepository.addMovement(
                MovementEntity(
                    accountId = account.id,
                    type = movementType,
                    amountCents = parsed.amountCents,
                    merchant = parsed.merchant,
                    occurredAt = Instant.now(),
                    source = MovementSource.NOTIFICATION,
                    rawNotificationText = text,
                ),
            )
        }
    }

    private suspend fun sendToInbox(
        packageName: String,
        text: String,
        suggestedAccountId: Long?,
        suggestedAmountCents: Long?,
    ) {
        inboxDao.insert(
            NotificationInboxEntity(
                packageName = packageName,
                rawText = text,
                postedAt = Instant.now(),
                suggestedAccountId = suggestedAccountId,
                suggestedAmountCents = suggestedAmountCents,
            ),
        )
    }

    private fun extractText(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        return listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
