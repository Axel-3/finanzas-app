package com.axel.finanzas.feature.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.domain.model.MovementUi
import com.axel.finanzas.domain.repository.AccountRepository
import com.axel.finanzas.domain.repository.CategoryRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val movementRepository: MovementRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movements: StateFlow<List<MovementUi>> = combine(
        movementRepository.observeAll(),
        accountRepository.observeAll(),
        categoryRepository.observeActive(),
    ) { movements, accounts, categories ->
        val accountsById = accounts.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }
        movements.map { movement ->
            MovementUi(
                id = movement.id,
                type = movement.type,
                amountCents = movement.amountCents,
                merchant = movement.merchant,
                categoryName = movement.categoryId?.let { categoriesById[it]?.name },
                categoryIconKey = movement.categoryId?.let { categoriesById[it]?.icon },
                accountId = movement.accountId,
                accountName = accountsById[movement.accountId]?.name ?: "Cuenta eliminada",
                linkedAccountName = movement.linkedAccountId?.let { accountsById[it]?.name },
                occurredAt = movement.occurredAt,
                note = movement.note,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastDeleted: MovementEntity? = null

    fun addExpenseOrIncome(
        type: MovementType,
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        merchant: String?,
        note: String?,
        occurredAt: Instant,
    ) {
        viewModelScope.launch {
            movementRepository.addMovement(
                MovementEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    type = type,
                    amountCents = amountCents,
                    merchant = merchant,
                    occurredAt = occurredAt,
                    note = note,
                ),
            )
        }
    }

    fun addTransfer(
        amountCents: Long,
        fromAccountId: Long,
        toAccountId: Long,
        note: String?,
        occurredAt: Instant,
    ) {
        viewModelScope.launch {
            movementRepository.addMovement(
                MovementEntity(
                    accountId = fromAccountId,
                    linkedAccountId = toAccountId,
                    type = MovementType.TRANSFER,
                    amountCents = amountCents,
                    occurredAt = occurredAt,
                    note = note,
                ),
            )
        }
    }

    fun updateMovement(old: MovementEntity, new: MovementEntity) {
        viewModelScope.launch { movementRepository.updateMovement(old, new) }
    }

    /** Borra y guarda una copia para poder deshacer con [undoDelete]. */
    fun deleteMovement(movementId: Long) {
        viewModelScope.launch {
            val entity = movementRepository.getById(movementId) ?: return@launch
            movementRepository.deleteMovement(entity)
            lastDeleted = entity
        }
    }

    fun undoDelete() {
        val toRestore = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            movementRepository.addMovement(toRestore.copy(id = 0))
        }
    }
}
