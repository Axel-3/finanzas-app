package com.axel.finanzas.widget

import androidx.lifecycle.ViewModel
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.domain.repository.AccountRepository
import com.axel.finanzas.domain.repository.CategoryRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {

    /**
     * Usa la primera cuenta de Efectivo activa (o la primera cuenta que
     * exista si no hay ninguna CASH) y la categoría "Otros gastos"/"Otros
     * ingresos" por defecto — desde el widget no hay espacio para elegir
     * cuenta/categoría, solo el monto.
     */
    suspend fun saveQuickMovement(type: MovementType, amountCents: Long): Boolean {
        if (amountCents <= 0) return false

        val accounts = accountRepository.observeActive().first()
        val account = accounts.firstOrNull { it.type == AccountType.CASH } ?: accounts.firstOrNull() ?: return false

        val categoryType = if (type == MovementType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val categories = categoryRepository.observeActive().first().filter { it.type == categoryType }
        val category = categories.firstOrNull { it.name.startsWith("Otros") } ?: categories.firstOrNull()

        movementRepository.addMovement(
            MovementEntity(
                accountId = account.id,
                categoryId = category?.id,
                type = type,
                amountCents = amountCents,
                note = "Agregado desde el widget",
                occurredAt = Instant.now(),
            ),
        )
        return true
    }
}
