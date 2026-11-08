package com.axel.finanzas.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.BudgetDao
import com.axel.finanzas.core.database.dao.CategoryDao
import com.axel.finanzas.core.database.dao.MovementDao
import com.axel.finanzas.core.database.entity.BudgetEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetDao: BudgetDao,
    private val movementDao: MovementDao,
    categoryDao: CategoryDao,
) : ViewModel() {

    private val currentMonthKey = MONTH_KEY_FORMATTER.format(LocalDate.now())

    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryDao.observeActiveByType(CategoryType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetUi>> = combine(
        budgetDao.observeForMonth(currentMonthKey),
        categoryDao.observeActiveByType(CategoryType.EXPENSE),
        movementDao.observeCategorySpending(monthStartMillis(), monthEndMillis()),
    ) { budgetEntities, categories, spending ->
        val categoriesById = categories.associateBy { it.id }
        val spentByCategory = spending.associate { it.categoryId to it.totalCents }
        budgetEntities.mapNotNull { budget ->
            val category = categoriesById[budget.categoryId] ?: return@mapNotNull null
            BudgetUi(
                budgetId = budget.id,
                categoryId = budget.categoryId,
                categoryName = category.name,
                categoryIconKey = category.icon,
                amountCents = budget.amountCents,
                spentCents = spentByCategory[budget.categoryId] ?: 0L,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBudget(categoryId: Long, amountCents: Long) {
        viewModelScope.launch {
            budgetDao.upsert(BudgetEntity(categoryId = categoryId, periodMonth = currentMonthKey, amountCents = amountCents))
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch { budgetDao.delete(budgetId) }
    }

    private fun monthStartMillis(): Long {
        val firstOfMonth = LocalDate.now().withDayOfMonth(1)
        return firstOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun monthEndMillis(): Long {
        val firstOfNextMonth = LocalDate.now().withDayOfMonth(1).plusMonths(1)
        return firstOfNextMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
    }
}
