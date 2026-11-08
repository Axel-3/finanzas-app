package com.axel.finanzas.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.RecurringPaymentDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.RecurrenceFrequency
import com.axel.finanzas.core.database.entity.RecurringPaymentEntity
import com.axel.finanzas.core.database.entity.advance
import com.axel.finanzas.domain.repository.AccountRepository
import com.axel.finanzas.domain.repository.CategoryRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringDao: RecurringPaymentDao,
    private val movementRepository: MovementRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val recurringPayments: StateFlow<List<RecurringPaymentEntity>> = recurringDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecurringPayment(
        name: String,
        amountCents: Long,
        frequency: RecurrenceFrequency,
        accountId: Long,
        categoryId: Long?,
        type: MovementType,
        startDate: LocalDate,
        reminderDaysBefore: Int,
    ) {
        viewModelScope.launch {
            recurringDao.insert(
                RecurringPaymentEntity(
                    name = name,
                    amountCents = amountCents,
                    frequency = frequency,
                    accountId = accountId,
                    categoryId = categoryId,
                    type = type,
                    nextDueDate = startDate,
                    reminderDaysBefore = reminderDaysBefore,
                ),
            )
        }
    }

    /** Registra el movimiento de este ciclo y avanza la fecha al siguiente. */
    fun markAsPaid(payment: RecurringPaymentEntity) {
        viewModelScope.launch {
            movementRepository.addMovement(
                MovementEntity(
                    accountId = payment.accountId,
                    categoryId = payment.categoryId,
                    type = payment.type,
                    amountCents = payment.amountCents,
                    merchant = payment.name,
                    occurredAt = Instant.now(),
                    note = "Pago recurrente",
                ),
            )
            recurringDao.update(payment.copy(nextDueDate = payment.frequency.advance(payment.nextDueDate)))
        }
    }

    fun deactivate(payment: RecurringPaymentEntity) {
        viewModelScope.launch { recurringDao.deactivate(payment.id) }
    }
}
