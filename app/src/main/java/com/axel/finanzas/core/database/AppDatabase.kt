package com.axel.finanzas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.axel.finanzas.core.database.dao.AccountDao
import com.axel.finanzas.core.database.dao.AccountLast4Dao
import com.axel.finanzas.core.database.dao.BudgetDao
import com.axel.finanzas.core.database.dao.CategoryDao
import com.axel.finanzas.core.database.dao.CreditCardDetailsDao
import com.axel.finanzas.core.database.dao.FuturePaymentDao
import com.axel.finanzas.core.database.dao.MovementDao
import com.axel.finanzas.core.database.dao.NotificationInboxDao
import com.axel.finanzas.core.database.dao.NotificationRuleDao
import com.axel.finanzas.core.database.dao.ProcessedNotificationKeyDao
import com.axel.finanzas.core.database.dao.RecurringPaymentDao
import com.axel.finanzas.core.database.dao.SavingsGoalDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.AccountLast4Entity
import com.axel.finanzas.core.database.entity.BudgetEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.CreditCardDetailsEntity
import com.axel.finanzas.core.database.entity.FuturePaymentEntity
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.NotificationInboxEntity
import com.axel.finanzas.core.database.entity.NotificationRuleEntity
import com.axel.finanzas.core.database.entity.ProcessedNotificationKeyEntity
import com.axel.finanzas.core.database.entity.RecurringPaymentEntity
import com.axel.finanzas.core.database.entity.SavingsContributionEntity
import com.axel.finanzas.core.database.entity.SavingsGoalEntity

/**
 * version=4: `notification_rules` cambió `typeRegex` (adivinado por
 * palabra clave, resultó incorrecto) por `kind` explícito — ver
 * [com.axel.finanzas.core.database.entity.NotificationRuleKind].
 *
 * version=5: tabla nueva `account_last4s` — una cuenta puede tener
 * tarjeta física Y digital (hasta una tercera) compartiendo el mismo
 * saldo, cada una con su propio last4. Esta es la PRIMERA migración real
 * del proyecto (ver [MIGRATION_4_5] en `DatabaseModule`) — antes de
 * esto, cualquier cambio de esquema dependía de
 * `fallbackToDestructiveMigration()` porque no había datos reales que
 * proteger. Ya los hay (cuentas, movimientos, notificaciones resueltas
 * del usuario), así que de aquí en adelante CADA cambio de esquema
 * necesita su propia `Migration`, nunca confiar en el fallback
 * destructivo para una base de datos que ya está en uso real.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        MovementEntity::class,
        CreditCardDetailsEntity::class,
        NotificationRuleEntity::class,
        NotificationInboxEntity::class,
        ProcessedNotificationKeyEntity::class,
        RecurringPaymentEntity::class,
        FuturePaymentEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class,
        BudgetEntity::class,
        AccountLast4Entity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun accountLast4Dao(): AccountLast4Dao
    abstract fun categoryDao(): CategoryDao
    abstract fun movementDao(): MovementDao
    abstract fun creditCardDetailsDao(): CreditCardDetailsDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun notificationInboxDao(): NotificationInboxDao
    abstract fun processedNotificationKeyDao(): ProcessedNotificationKeyDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun futurePaymentDao(): FuturePaymentDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun budgetDao(): BudgetDao
}
