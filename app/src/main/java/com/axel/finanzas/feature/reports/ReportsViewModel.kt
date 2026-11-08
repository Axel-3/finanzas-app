package com.axel.finanzas.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.CategoryDao
import com.axel.finanzas.core.database.dao.MovementDao
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class ReportsUiState(
    val categorySlices: List<CategorySlice> = emptyList(),
    val monthlyTotals: List<MonthTotal> = emptyList(),
    val totalExpenseThisMonth: Long = 0,
    val totalIncomeThisMonth: Long = 0,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val movementDao: MovementDao,
    categoryDao: CategoryDao,
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> = combine(
        movementDao.observeAll(),
        categoryDao.observeActive(),
    ) { movements, categories ->
        val categoriesById = categories.associateBy { it.id }
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.now()

        val thisMonthExpenses = movements.filter {
            it.type == MovementType.EXPENSE && YearMonth.from(it.occurredAt.atZone(zone)) == currentMonth
        }
        val totalExpenseThisMonth = thisMonthExpenses.sumOf { it.amountCents }
        val totalIncomeThisMonth = movements
            .filter { it.type == MovementType.INCOME && YearMonth.from(it.occurredAt.atZone(zone)) == currentMonth }
            .sumOf { it.amountCents }

        val categorySlices = thisMonthExpenses
            .groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val category = categoryId?.let { categoriesById[it] }
                CategorySlice(
                    name = category?.name ?: "Sin categoría",
                    amountCents = group.sumOf { it.amountCents },
                    colorHex = category?.colorHex ?: "#6B6660",
                )
            }
            .sortedByDescending { it.amountCents }

        val monthlyTotals = (5 downTo 0).map { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo.toLong())
            val inMonth = movements.filter { YearMonth.from(it.occurredAt.atZone(zone)) == month }
            MonthTotal(
                month = month,
                expenseCents = inMonth.filter { it.type == MovementType.EXPENSE }.sumOf { it.amountCents },
                incomeCents = inMonth.filter { it.type == MovementType.INCOME }.sumOf { it.amountCents },
            )
        }

        ReportsUiState(
            categorySlices = categorySlices,
            monthlyTotals = monthlyTotals,
            totalExpenseThisMonth = totalExpenseThisMonth,
            totalIncomeThisMonth = totalIncomeThisMonth,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    /** CSV completo de todos los movimientos (no solo del mes), para respaldo/análisis externo. */
    suspend fun buildCsv(): String {
        val movements = movementDao.observeAll().first()
        return buildCsvFromSnapshot(movements)
    }

    private fun buildCsvFromSnapshot(movements: List<MovementEntity>): String {
        val header = "fecha,tipo,monto_mxn,comercio,nota,fuente\n"
        val rows = movements.joinToString("\n") { m ->
            val date = m.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val amount = "%.2f".format(m.amountCents / 100.0)
            listOf(date, m.type.name, amount, m.merchant.orEmpty(), m.note.orEmpty(), m.source.name)
                .joinToString(",") { csvEscape(it) }
        }
        return header + rows
    }

    private fun csvEscape(value: String): String =
        if (value.contains(",") || value.contains("\"")) "\"${value.replace("\"", "\"\"")}\"" else value
}
