package com.axel.finanzas.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.FuturePaymentDao
import com.axel.finanzas.core.database.dao.RecurringPaymentDao
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val currentLiquidBalanceCents: Long = 0,
    val projectedBalanceCents: Long = 0,
    val items: List<AgendaItem> = emptyList(),
)

/**
 * Agenda de los próximos 30 días (recurrentes + pagos futuros) en vez de
 * una cuadrícula de calendario visual — para el alcance de esta fase
 * alcanza y es más simple de usar en una lista con el dedo. Si más
 * adelante hace falta una cuadrícula real, se puede agregar aparte.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    recurringDao: RecurringPaymentDao,
    futurePaymentDao: FuturePaymentDao,
    accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<CalendarUiState> = combine(
        recurringDao.observeActive(),
        futurePaymentDao.observeUnpaid(),
        accountRepository.observeActive(),
    ) { recurring, future, accounts ->
        val today = LocalDate.now()
        val horizon = today.plusDays(30)

        val recurringItems = recurring
            .filter { !it.nextDueDate.isAfter(horizon) }
            .map {
                AgendaItem(
                    date = it.nextDueDate,
                    label = it.name,
                    amountCents = it.amountCents,
                    isIncome = it.type == MovementType.INCOME,
                    source = AgendaItem.Source.RECURRING,
                )
            }
        val futureItems = future
            .filter { !it.dueDate.isAfter(horizon) }
            .map {
                AgendaItem(
                    date = it.dueDate,
                    label = it.name,
                    amountCents = it.amountCents,
                    isIncome = false,
                    source = AgendaItem.Source.FUTURE_PAYMENT,
                )
            }

        val items = (recurringItems + futureItems).sortedBy { it.date }

        val liquidBalance = accounts
            .filter { it.type != AccountType.CREDIT }
            .sumOf { it.currentBalanceCents }

        val netEffect = items.sumOf { if (it.isIncome) it.amountCents else -it.amountCents }

        CalendarUiState(
            currentLiquidBalanceCents = liquidBalance,
            projectedBalanceCents = liquidBalance + netEffect,
            items = items,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())
}
