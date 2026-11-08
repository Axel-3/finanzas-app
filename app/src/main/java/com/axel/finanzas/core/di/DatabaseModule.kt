package com.axel.finanzas.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.axel.finanzas.core.database.AppDatabase
import com.axel.finanzas.core.database.SqlCipherMigrator
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
import com.axel.finanzas.core.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Agrega `account_last4s` (tarjeta física/digital compartiendo saldo) sin
 * tocar ninguna tabla existente — ver nota de version=5 en AppDatabase.kt.
 * Primera migración real del proyecto; de aquí en adelante cada cambio de
 * esquema necesita la suya, YA NO se puede confiar en el fallback
 * destructivo (hay datos reales de usuario que proteger).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `account_last4s` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `last4` TEXT NOT NULL,
                `label` TEXT,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_account_last4s_accountId` ON `account_last4s` (`accountId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_last4s_last4` ON `account_last4s` (`last4`)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Cifrado en reposo con SQLCipher. La llave sale de [DatabaseKeyProvider]
     * (aleatoria, protegida con Android Keystore, nunca pedida al usuario) y
     * [SqlCipherMigrator] cifra el archivo existente EN SU LUGAR la
     * primera vez que corre en un teléfono con datos reales ya guardados
     * en SQLite plano — instalaciones nuevas no necesitan migrar nada,
     * Room crea el archivo ya cifrado desde cero.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // net.zetetic:sqlcipher-android 4.6.1 NO carga su .so sola (a
        // diferencia de versiones viejas con loadLibs(context)) — ninguna
        // clase de la librería llama a System.loadLibrary, le toca a la
        // app. Sin esto: UnsatisfiedLinkError en el primer open, tanto
        // desde el migrador como desde Room/SupportOpenHelperFactory.
        System.loadLibrary("sqlcipher")

        val dbFile = context.getDatabasePath("finanzas.db")
        val keyBytes = DatabaseKeyProvider.getOrCreateKey(context)
        SqlCipherMigrator.migrateIfNeeded(dbFile, keyBytes)

        return Room.databaseBuilder(context, AppDatabase::class.java, "finanzas.db")
            .openHelperFactory(SupportOpenHelperFactory(keyBytes))
            .addMigrations(MIGRATION_4_5)
            // Red de seguridad SOLO para saltos de versión sin migración
            // propia (no debería dispararse nunca desde version=5 en
            // adelante — ver nota en AppDatabase.kt).
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideAccountLast4Dao(db: AppDatabase): AccountLast4Dao = db.accountLast4Dao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideMovementDao(db: AppDatabase): MovementDao = db.movementDao()

    @Provides
    fun provideCreditCardDetailsDao(db: AppDatabase): CreditCardDetailsDao = db.creditCardDetailsDao()

    @Provides
    fun provideNotificationRuleDao(db: AppDatabase): NotificationRuleDao = db.notificationRuleDao()

    @Provides
    fun provideNotificationInboxDao(db: AppDatabase): NotificationInboxDao = db.notificationInboxDao()

    @Provides
    fun provideProcessedNotificationKeyDao(db: AppDatabase): ProcessedNotificationKeyDao = db.processedNotificationKeyDao()

    @Provides
    fun provideRecurringPaymentDao(db: AppDatabase): RecurringPaymentDao = db.recurringPaymentDao()

    @Provides
    fun provideFuturePaymentDao(db: AppDatabase): FuturePaymentDao = db.futurePaymentDao()

    @Provides
    fun provideSavingsGoalDao(db: AppDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
}
