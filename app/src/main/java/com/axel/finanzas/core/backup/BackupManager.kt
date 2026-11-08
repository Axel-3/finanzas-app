package com.axel.finanzas.core.backup

import androidx.room.withTransaction
import com.axel.finanzas.core.database.AppDatabase
import com.axel.finanzas.core.database.dao.AccountDao
import com.axel.finanzas.core.database.dao.BudgetDao
import com.axel.finanzas.core.database.dao.CategoryDao
import com.axel.finanzas.core.database.dao.CreditCardDetailsDao
import com.axel.finanzas.core.database.dao.FuturePaymentDao
import com.axel.finanzas.core.database.dao.MovementDao
import com.axel.finanzas.core.database.dao.RecurringPaymentDao
import com.axel.finanzas.core.database.dao.SavingsGoalDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.BudgetEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.database.entity.CreditCardDetailsEntity
import com.axel.finanzas.core.database.entity.FuturePaymentEntity
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementSource
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.RecurrenceFrequency
import com.axel.finanzas.core.database.entity.RecurringPaymentEntity
import com.axel.finanzas.core.database.entity.SavingsContributionEntity
import com.axel.finanzas.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Exporta/restaura TODO lo que guarda la app (menos las reglas de
 * notificaciones y la bandeja de revisión, que no son "datos financieros"
 * y se pueden rearmar) como un JSON, cifrado con [BackupCodec].
 *
 * Restaurar es un reemplazo completo: borra lo que haya y mete lo del
 * respaldo. Pensado para "tengo un teléfono nuevo" o "reinstalé la app",
 * no para mezclar datos de dos momentos distintos.
 */
class BackupManager @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val movementDao: MovementDao,
    private val creditCardDetailsDao: CreditCardDetailsDao,
    private val recurringPaymentDao: RecurringPaymentDao,
    private val futurePaymentDao: FuturePaymentDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val budgetDao: BudgetDao,
) {
    suspend fun exportEncrypted(passphrase: String): ByteArray {
        val json = buildJson()
        return BackupCodec.encrypt(json.toString().toByteArray(Charsets.UTF_8), passphrase)
    }

    /** @throws IllegalArgumentException o una excepción de cifrado si la contraseña es incorrecta. */
    suspend fun restoreFromEncrypted(data: ByteArray, passphrase: String) {
        val jsonText = BackupCodec.decrypt(data, passphrase).toString(Charsets.UTF_8)
        restoreFromJson(JSONObject(jsonText))
    }

    private suspend fun buildJson(): JSONObject {
        val root = JSONObject()
        root.put("version", 1)

        val accounts = accountDao.observeAll().first()
        root.put("accounts", JSONArray(accounts.map { it.toJson() }))

        val categories = categoryDao.observeActive().first()
        root.put("categories", JSONArray(categories.map { it.toJson() }))

        val movements = movementDao.observeAll().first()
        root.put("movements", JSONArray(movements.map { it.toJson() }))

        val cardDetails = creditCardDetailsDao.observeAll().first()
        root.put("creditCardDetails", JSONArray(cardDetails.map { it.toJson() }))

        val recurring = recurringPaymentDao.observeActive().first()
        root.put("recurringPayments", JSONArray(recurring.map { it.toJson() }))

        val future = futurePaymentDao.observeUnpaid().first()
        root.put("futurePayments", JSONArray(future.map { it.toJson() }))

        val goals = savingsGoalDao.observeActive().first()
        root.put("savingsGoals", JSONArray(goals.map { it.toJson() }))

        val contributions = savingsGoalDao.observeAllContributions().first()
        root.put("savingsContributions", JSONArray(contributions.map { it.toJson() }))

        val budgets = budgetDao.observeAll().first()
        root.put("budgets", JSONArray(budgets.map { it.toJson() }))

        return root
    }

    private suspend fun restoreFromJson(root: JSONObject) = database.withTransaction {
        // Orden: primero lo que no depende de nada, al final lo que sí.
        database.clearAllTables()

        val accountIdMap = mutableMapOf<Long, Long>()
        root.getJSONArray("accounts").forEachObject { obj ->
            val oldId = obj.getLong("id")
            val newId = accountDao.insert(obj.toAccountEntity())
            accountIdMap[oldId] = newId
        }

        val categoryIdMap = mutableMapOf<Long, Long>()
        root.getJSONArray("categories").forEachObject { obj ->
            val oldId = obj.getLong("id")
            val newId = categoryDao.insert(obj.toCategoryEntity())
            categoryIdMap[oldId] = newId
        }

        root.getJSONArray("movements").forEachObject { obj ->
            movementDao.insert(obj.toMovementEntity(accountIdMap, categoryIdMap))
        }
        root.getJSONArray("creditCardDetails").forEachObject { obj ->
            val mappedAccountId = accountIdMap[obj.getLong("accountId")] ?: return@forEachObject
            creditCardDetailsDao.upsert(obj.toCreditCardDetailsEntity(mappedAccountId))
        }
        root.getJSONArray("recurringPayments").forEachObject { obj ->
            recurringPaymentDao.insert(obj.toRecurringPaymentEntity(accountIdMap, categoryIdMap))
        }
        root.getJSONArray("futurePayments").forEachObject { obj ->
            futurePaymentDao.insert(obj.toFuturePaymentEntity(accountIdMap, categoryIdMap))
        }

        val goalIdMap = mutableMapOf<Long, Long>()
        root.getJSONArray("savingsGoals").forEachObject { obj ->
            val oldId = obj.getLong("id")
            val newId = savingsGoalDao.insert(obj.toSavingsGoalEntity())
            goalIdMap[oldId] = newId
        }
        root.getJSONArray("savingsContributions").forEachObject { obj ->
            val mappedGoalId = goalIdMap[obj.getLong("goalId")] ?: return@forEachObject
            savingsGoalDao.insertContribution(obj.toSavingsContributionEntity(mappedGoalId))
        }
        root.optJSONArray("budgets")?.forEachObject { obj ->
            val mappedCategoryId = categoryIdMap[obj.getLong("categoryId")] ?: return@forEachObject
            budgetDao.upsert(obj.toBudgetEntity(mappedCategoryId))
        }
    }
}

private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
    for (i in 0 until length()) action(getJSONObject(i))
}

private fun AccountEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name)
    put("bank", bank); put("last4", last4); put("currentBalanceCents", currentBalanceCents)
    put("colorHex", colorHex); put("icon", icon); put("isActive", isActive)
    put("createdAt", createdAt.toEpochMilli())
}

private fun JSONObject.toAccountEntity() = AccountEntity(
    name = getString("name"),
    type = AccountType.valueOf(getString("type")),
    bank = optStringOrNull("bank"),
    last4 = optStringOrNull("last4"),
    currentBalanceCents = getLong("currentBalanceCents"),
    colorHex = getString("colorHex"),
    icon = getString("icon"),
    isActive = getBoolean("isActive"),
    createdAt = Instant.ofEpochMilli(getLong("createdAt")),
)

private fun CategoryEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("icon", icon); put("colorHex", colorHex)
    put("parentCategoryId", parentCategoryId ?: JSONObject.NULL)
    put("type", type.name); put("isDefault", isDefault); put("isActive", isActive)
}

private fun JSONObject.toCategoryEntity() = CategoryEntity(
    name = getString("name"),
    icon = getString("icon"),
    colorHex = getString("colorHex"),
    parentCategoryId = null, // simplificación: no reconstruimos jerarquías de subcategorías al restaurar
    type = CategoryType.valueOf(getString("type")),
    isDefault = getBoolean("isDefault"),
    isActive = getBoolean("isActive"),
)

private fun MovementEntity.toJson() = JSONObject().apply {
    put("accountId", accountId)
    put("linkedAccountId", linkedAccountId ?: JSONObject.NULL)
    put("categoryId", categoryId ?: JSONObject.NULL)
    put("type", type.name); put("amountCents", amountCents)
    put("merchant", merchant); put("occurredAt", occurredAt.toEpochMilli())
    put("source", source.name); put("rawNotificationText", rawNotificationText)
    put("previousBalanceCents", previousBalanceCents ?: JSONObject.NULL)
    put("newBalanceCents", newBalanceCents ?: JSONObject.NULL)
    put("note", note)
    put("createdAt", createdAt.toEpochMilli()); put("updatedAt", updatedAt.toEpochMilli())
}

private fun JSONObject.toMovementEntity(accountIdMap: Map<Long, Long>, categoryIdMap: Map<Long, Long>) = MovementEntity(
    accountId = accountIdMap[getLong("accountId")] ?: error("Cuenta no encontrada al restaurar movimiento"),
    linkedAccountId = optLongOrNull("linkedAccountId")?.let { accountIdMap[it] },
    categoryId = optLongOrNull("categoryId")?.let { categoryIdMap[it] },
    type = MovementType.valueOf(getString("type")),
    amountCents = getLong("amountCents"),
    merchant = optStringOrNull("merchant"),
    occurredAt = Instant.ofEpochMilli(getLong("occurredAt")),
    source = MovementSource.valueOf(getString("source")),
    rawNotificationText = optStringOrNull("rawNotificationText"),
    previousBalanceCents = optLongOrNull("previousBalanceCents"),
    newBalanceCents = optLongOrNull("newBalanceCents"),
    note = optStringOrNull("note"),
    createdAt = Instant.ofEpochMilli(getLong("createdAt")),
    updatedAt = Instant.ofEpochMilli(getLong("updatedAt")),
)

private fun CreditCardDetailsEntity.toJson() = JSONObject().apply {
    put("accountId", accountId); put("creditLimitCents", creditLimitCents)
    put("cutOffDay", cutOffDay); put("paymentDueDay", paymentDueDay)
    put("alertThresholdPercent", alertThresholdPercent)
    put("minPaymentCents", minPaymentCents ?: JSONObject.NULL)
    put("noInterestPaymentCents", noInterestPaymentCents ?: JSONObject.NULL)
    put("lastStatementDate", lastStatementDate?.toEpochMilli() ?: JSONObject.NULL)
}

private fun JSONObject.toCreditCardDetailsEntity(mappedAccountId: Long) = CreditCardDetailsEntity(
    accountId = mappedAccountId,
    creditLimitCents = getLong("creditLimitCents"),
    cutOffDay = getInt("cutOffDay"),
    paymentDueDay = getInt("paymentDueDay"),
    alertThresholdPercent = getInt("alertThresholdPercent"),
    minPaymentCents = optLongOrNull("minPaymentCents"),
    noInterestPaymentCents = optLongOrNull("noInterestPaymentCents"),
    lastStatementDate = optLongOrNull("lastStatementDate")?.let(Instant::ofEpochMilli),
)

private fun RecurringPaymentEntity.toJson() = JSONObject().apply {
    put("name", name); put("amountCents", amountCents); put("frequency", frequency.name)
    put("accountId", accountId); put("categoryId", categoryId ?: JSONObject.NULL)
    put("type", type.name); put("nextDueDate", nextDueDate.toEpochDay())
    put("reminderDaysBefore", reminderDaysBefore); put("isActive", isActive)
}

private fun JSONObject.toRecurringPaymentEntity(accountIdMap: Map<Long, Long>, categoryIdMap: Map<Long, Long>) = RecurringPaymentEntity(
    name = getString("name"),
    amountCents = getLong("amountCents"),
    frequency = RecurrenceFrequency.valueOf(getString("frequency")),
    accountId = accountIdMap[getLong("accountId")] ?: error("Cuenta no encontrada al restaurar pago recurrente"),
    categoryId = optLongOrNull("categoryId")?.let { categoryIdMap[it] },
    type = MovementType.valueOf(getString("type")),
    nextDueDate = LocalDate.ofEpochDay(getLong("nextDueDate")),
    reminderDaysBefore = getInt("reminderDaysBefore"),
    isActive = getBoolean("isActive"),
)

private fun FuturePaymentEntity.toJson() = JSONObject().apply {
    put("name", name); put("amountCents", amountCents); put("dueDate", dueDate.toEpochDay())
    put("accountId", accountId ?: JSONObject.NULL); put("categoryId", categoryId ?: JSONObject.NULL)
    put("reminderDaysBefore", reminderDaysBefore); put("isPaid", isPaid)
}

private fun JSONObject.toFuturePaymentEntity(accountIdMap: Map<Long, Long>, categoryIdMap: Map<Long, Long>) = FuturePaymentEntity(
    name = getString("name"),
    amountCents = getLong("amountCents"),
    dueDate = LocalDate.ofEpochDay(getLong("dueDate")),
    accountId = optLongOrNull("accountId")?.let { accountIdMap[it] },
    categoryId = optLongOrNull("categoryId")?.let { categoryIdMap[it] },
    reminderDaysBefore = getInt("reminderDaysBefore"),
    isPaid = getBoolean("isPaid"),
)

private fun SavingsGoalEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("targetAmountCents", targetAmountCents)
    put("targetDate", targetDate?.toEpochDay() ?: JSONObject.NULL)
    put("colorHex", colorHex); put("icon", icon); put("isActive", isActive)
}

private fun JSONObject.toSavingsGoalEntity() = SavingsGoalEntity(
    name = getString("name"),
    targetAmountCents = getLong("targetAmountCents"),
    targetDate = optLongOrNull("targetDate")?.let(LocalDate::ofEpochDay),
    colorHex = getString("colorHex"),
    icon = getString("icon"),
    isActive = getBoolean("isActive"),
)

private fun SavingsContributionEntity.toJson() = JSONObject().apply {
    put("goalId", goalId); put("amountCents", amountCents)
    put("date", date.toEpochDay()); put("note", note)
}

private fun JSONObject.toSavingsContributionEntity(mappedGoalId: Long) = SavingsContributionEntity(
    goalId = mappedGoalId,
    amountCents = getLong("amountCents"),
    date = LocalDate.ofEpochDay(getLong("date")),
    note = optStringOrNull("note"),
)

private fun BudgetEntity.toJson() = JSONObject().apply {
    put("categoryId", categoryId); put("periodMonth", periodMonth); put("amountCents", amountCents)
}

private fun JSONObject.toBudgetEntity(mappedCategoryId: Long) = BudgetEntity(
    categoryId = mappedCategoryId,
    periodMonth = getString("periodMonth"),
    amountCents = getLong("amountCents"),
)

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
