package com.axel.finanzas.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.FuturePaymentDao
import com.axel.finanzas.core.database.dao.RecurringPaymentDao
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.designsystem.components.iconForKey
import com.axel.finanzas.domain.repository.AccountRepository
import com.axel.finanzas.domain.repository.CardRepository
import com.axel.finanzas.domain.repository.CategoryRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private data class HomeBaseData(
    val accounts: List<com.axel.finanzas.core.database.entity.AccountEntity>,
    val movements: List<com.axel.finanzas.core.database.entity.MovementEntity>,
    val categories: List<com.axel.finanzas.core.database.entity.CategoryEntity>,
    val cards: List<com.axel.finanzas.domain.model.CardUi>,
    val recurring: List<com.axel.finanzas.core.database.entity.RecurringPaymentEntity>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    movementRepository: MovementRepository,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
    recurringPaymentDao: RecurringPaymentDao,
    futurePaymentDao: FuturePaymentDao,
) : ViewModel() {

    // kotlinx.coroutines.flow.combine solo tiene overloads tipados hasta 5
    // flows; con 6 fuentes distintas hay que anidar uno más.
    private val baseFlow = combine(
        accountRepository.observeActive(),
        movementRepository.observeAll(),
        categoryRepository.observeActive(),
        cardRepository.observeCards(),
        recurringPaymentDao.observeActive(),
    ) { accounts, movements, categories, cards, recurring ->
        HomeBaseData(accounts, movements, categories, cards, recurring)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseFlow,
        futurePaymentDao.observeUnpaid(),
    ) { base, future ->
        val (accounts, movements, categories, cards, recurring) = base
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.now()
        val categoriesById = categories.associateBy { it.id }
        val accountsById = accounts.associateBy { it.id }

        val activeCards = cards.filter { it.isActive }
        // Cada tipo por separado — mezclarlos fue el problema de confusión
        // que "Disponible" tenía antes. El saldo de débito aquí es la suma
        // de TODAS las tarjetas de débito (el carrusel de abajo ya las
        // muestra una por una); Ahorros es una cuenta de saldo libre, a
        // diferencia de "Metas de ahorro" que exige un monto objetivo y
        // solo deja aportar, nunca retirar.
        val cashBalance = accounts.filter { it.type == AccountType.CASH }.sumOf { it.currentBalanceCents }
        val debitBalance = accounts.filter { it.type == AccountType.DEBIT }.sumOf { it.currentBalanceCents }
        val savingsBalance = accounts.filter { it.type == AccountType.SAVINGS }.sumOf { it.currentBalanceCents }
        val creditUsed = accounts.filter { it.type == AccountType.CREDIT }.sumOf { it.currentBalanceCents }
        val creditAvailable = activeCards
            .filter { it.isCredit }
            .sumOf { (it.creditLimitCents ?: 0L) - it.balanceCents }

        val thisMonthMovements = movements.filter { YearMonth.from(it.occurredAt.atZone(zone)) == currentMonth }

        fun isCashAccount(accountId: Long) = accountsById[accountId]?.type == AccountType.CASH

        val monthExpenses = thisMonthMovements.filter { it.type == MovementType.EXPENSE }
        val monthIncome = thisMonthMovements.filter { it.type == MovementType.INCOME }

        val monthExpensesCash = monthExpenses.filter { isCashAccount(it.accountId) }.sumOf { it.amountCents }
        val monthExpensesCard = monthExpenses.filterNot { isCashAccount(it.accountId) }.sumOf { it.amountCents }
        val monthIncomeCash = monthIncome.filter { isCashAccount(it.accountId) }.sumOf { it.amountCents }
        val monthIncomeCard = monthIncome.filterNot { isCashAccount(it.accountId) }.sumOf { it.amountCents }

        val recentMovements = movements.take(5).map { movement ->
            HomeMovementUi(
                merchant = movement.merchant?.takeIf { it.isNotBlank() }
                    ?: categoriesById[movement.categoryId]?.name
                    ?: "Movimiento",
                category = categoriesById[movement.categoryId]?.name ?: "Sin categoría",
                icon = iconForKey(categoriesById[movement.categoryId]?.icon ?: "misc"),
                amountCents = movement.amountCents,
                isExpense = movement.type == MovementType.EXPENSE,
                occurredAt = movement.occurredAt,
                accountLabel = accountsById[movement.accountId]?.name ?: "",
            )
        }

        val today = LocalDate.now()
        val dateLabelFormatter = DateTimeFormatter.ofPattern("d MMM")
        val upcoming = (
            recurring.map { Triple(it.name, it.amountCents, it.nextDueDate) } +
                future.map { Triple(it.name, it.amountCents, it.dueDate) }
            )
            .filter { !it.third.isBefore(today) }
            .sortedBy { it.third }
            .take(5)
            .map { (name, amount, date) -> HomeUpcomingUi(name, amount, dateLabelFormatter.format(date)) }

        HomeUiState(
            cashBalanceCents = cashBalance,
            debitBalanceCents = debitBalance,
            savingsBalanceCents = savingsBalance,
            creditUsedCents = creditUsed,
            creditAvailableCents = creditAvailable,
            monthExpensesCents = monthExpenses.sumOf { it.amountCents },
            monthIncomeCents = monthIncome.sumOf { it.amountCents },
            monthExpensesCashCents = monthExpensesCash,
            monthExpensesCardCents = monthExpensesCard,
            monthIncomeCashCents = monthIncomeCash,
            monthIncomeCardCents = monthIncomeCard,
            cards = activeCards,
            upcomingPayments = upcoming,
            recentMovements = recentMovements,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
