package com.axel.finanzas.feature.home

import androidx.compose.ui.graphics.vector.ImageVector
import com.axel.finanzas.domain.model.CardUi
import java.time.Instant

data class HomeMovementUi(
    val merchant: String,
    val category: String,
    val icon: ImageVector,
    val amountCents: Long,
    val isExpense: Boolean,
    val occurredAt: Instant,
    val accountLabel: String,
)

data class HomeUpcomingUi(val name: String, val amountCents: Long, val dueDateLabel: String)

data class HomeUiState(
    val cashBalanceCents: Long = 0,
    val debitBalanceCents: Long = 0,
    val savingsBalanceCents: Long = 0,
    val creditUsedCents: Long = 0,
    val creditAvailableCents: Long = 0,
    val monthExpensesCents: Long = 0,
    val monthIncomeCents: Long = 0,
    val monthExpensesCashCents: Long = 0,
    val monthExpensesCardCents: Long = 0,
    val monthIncomeCashCents: Long = 0,
    val monthIncomeCardCents: Long = 0,
    val cards: List<CardUi> = emptyList(),
    val upcomingPayments: List<HomeUpcomingUi> = emptyList(),
    val recentMovements: List<HomeMovementUi> = emptyList(),
)
