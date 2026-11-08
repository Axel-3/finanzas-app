package com.axel.finanzas.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    // Débito/Crédito viven en la pantalla de Tarjetas (tienen last4, banco,
    // límite, etc.); aquí solo las cuentas "simples".
    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeAll()
        .map { list -> list.filter { it.type == AccountType.CASH || it.type == AccountType.WALLET || it.type == AccountType.SAVINGS } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createAccount(name: String, type: AccountType, initialBalanceCents: Long) {
        viewModelScope.launch {
            accountRepository.create(
                AccountEntity(
                    name = name,
                    type = type,
                    currentBalanceCents = initialBalanceCents,
                    icon = if (type == AccountType.CASH) "cash" else if (type == AccountType.SAVINGS) "savings" else "card",
                ),
            )
        }
    }

    fun toggleActive(account: AccountEntity) {
        viewModelScope.launch {
            if (account.isActive) accountRepository.deactivate(account.id) else accountRepository.reactivate(account.id)
        }
    }
}
