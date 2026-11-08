package com.axel.finanzas.feature.budgets

data class BudgetUi(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIconKey: String,
    val amountCents: Long,
    val spentCents: Long,
) {
    val progress: Float get() = if (amountCents > 0) (spentCents.toFloat() / amountCents.toFloat()).coerceIn(0f, 2f) else 0f
    val isOver80: Boolean get() = amountCents > 0 && spentCents >= amountCents * 0.8
    val isOver100: Boolean get() = amountCents > 0 && spentCents >= amountCents
}
