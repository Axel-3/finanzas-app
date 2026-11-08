package com.axel.finanzas.core.database

import com.axel.finanzas.core.database.dao.AccountDao
import com.axel.finanzas.core.database.dao.CategoryDao
import com.axel.finanzas.core.database.dao.NotificationRuleDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.notifications.NotificationSettingsRepository
import javax.inject.Inject

/**
 * Para que la app no abra vacía la primera vez: categorías comunes, una
 * cuenta de Efectivo y las reglas de notificación de [DefaultBankRules].
 * Corre en cada arranque (ver [seedIfNeeded]), disparado desde
 * FinanzasApp.onCreate — categorías y cuenta de Efectivo solo se crean
 * si no hay ninguna (son datos reales del usuario, nunca se pisan), pero las
 * reglas de notificación SÍ se resiembran por completo cada vez que
 * [DefaultBankRules.VERSION] sube, para que una corrección de regex en
 * el código le llegue a un teléfono que ya las sembró antes. Hoy no hay
 * UI para editar una regla individual (solo borrarla), así que no hay
 * personalización del usuario que proteger al resembrar — si eso cambia,
 * esta lógica necesita revisarse.
 */
class DefaultDataSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val notificationRuleDao: NotificationRuleDao,
    private val notificationSettingsRepository: NotificationSettingsRepository,
) {
    suspend fun seedIfNeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(defaultCategories())
        }
        if (accountDao.count() == 0) {
            accountDao.insert(
                AccountEntity(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    currentBalanceCents = 0,
                    colorHex = "#4F7A6C",
                    icon = "cash",
                ),
            )
        }

        val seededVersion = notificationSettingsRepository.getSeededRulesVersion()
        if (notificationRuleDao.count() == 0 || seededVersion != DefaultBankRules.VERSION) {
            notificationRuleDao.deleteAll()
            DefaultBankRules.all().forEach { notificationRuleDao.insert(it) }
            notificationSettingsRepository.setSeededRulesVersion(DefaultBankRules.VERSION)
        }
    }

    private fun defaultCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Despensa", icon = "shopping_bag", colorHex = "#C97B5F", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Comida", icon = "dining", colorHex = "#C97B5F", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Transporte", icon = "car", colorHex = "#BC8468", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Servicios", icon = "bolt", colorHex = "#BC8468", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Entretenimiento", icon = "games", colorHex = "#C97B5F", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Salud", icon = "health", colorHex = "#BC8468", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Hogar", icon = "home", colorHex = "#BC8468", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Ropa", icon = "shirt", colorHex = "#C97B5F", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Otros gastos", icon = "misc", colorHex = "#BC8468", type = CategoryType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Nómina", icon = "payroll", colorHex = "#5E9478", type = CategoryType.INCOME, isDefault = true),
        CategoryEntity(name = "Ventas", icon = "sales", colorHex = "#5E9478", type = CategoryType.INCOME, isDefault = true),
        CategoryEntity(name = "Regalos", icon = "gift", colorHex = "#5E9478", type = CategoryType.INCOME, isDefault = true),
        CategoryEntity(name = "Otros ingresos", icon = "misc", colorHex = "#5E9478", type = CategoryType.INCOME, isDefault = true),
    )
}
