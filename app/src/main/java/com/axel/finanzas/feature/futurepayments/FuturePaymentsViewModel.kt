package com.axel.finanzas.feature.futurepayments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.FuturePaymentDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.FuturePaymentEntity
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementType
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
class FuturePaymentsViewModel @Inject constructor(
    private val futurePaymentDao: FuturePaymentDao,
    private val movementRepository: MovementRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val payments: StateFlow<List<FuturePaymentEntity>> = futurePaymentDao.observeUnpaid()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFuturePayment(
        name: String,
        amountCents: Long,
        dueDate: LocalDate,
        accountId: Long?,
        categoryId: Long?,
        reminderDaysBefore: Int,
    ) {
        viewModelScope.launch {
            futurePaymentDao.insert(
                FuturePaymentEntity(
                    name = name,
                    amountCents = amountCents,
                    dueDate = dueDate,
                    accountId = accountId,
                    categoryId = categoryId,
                    reminderDaysBefore = reminderDaysBefore,
                ),
            )
        }
    }

    fun markAsPaid(payment: FuturePaymentEntity) {
        viewModelScope.launch {
            val movementId = if (payment.accountId != null) {
                movementRepository.addMovement(
                    MovementEntity(
                        accountId = payment.accountId,
                        categoryId = payment.categoryId,
                        type = MovementType.EXPENSE,
                        amountCents = payment.amountCents,
                        merchant = payment.name,
                        occurredAt = Instant.now(),
                        note = "Pago futuro",
                    ),
                )
            } else {
                null
            }
            futurePaymentDao.update(payment.copy(isPaid = true, linkedMovementId = movementId))
        }
    }
}
