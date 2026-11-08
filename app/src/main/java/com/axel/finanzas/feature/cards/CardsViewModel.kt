package com.axel.finanzas.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.NotificationInboxDao
import com.axel.finanzas.domain.model.CardUi
import com.axel.finanzas.domain.repository.CardRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val movementRepository: MovementRepository,
    notificationInboxDao: NotificationInboxDao,
) : ViewModel() {

    val cards: StateFlow<List<CardUi>> = cardRepository.observeCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingInboxCount: StateFlow<Int> = notificationInboxDao.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun createDebitCard(alias: String, bank: String, last4: String, initialBalanceCents: Long) {
        viewModelScope.launch { cardRepository.createDebitCard(alias, bank, last4, initialBalanceCents) }
    }

    fun createCreditCard(
        alias: String,
        bank: String,
        last4: String,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
        initialBalanceCents: Long,
    ) {
        viewModelScope.launch {
            cardRepository.createCreditCard(
                alias, bank, last4, limitCents, cutOffDay, paymentDueDay, alertThresholdPercent, initialBalanceCents,
            )
        }
    }

    fun adjustBalance(accountId: Long, newBalanceCents: Long, note: String?) {
        viewModelScope.launch { movementRepository.recordBalanceAdjustment(accountId, newBalanceCents, note) }
    }

    fun setActive(accountId: Long, isActive: Boolean) {
        viewModelScope.launch { cardRepository.setActive(accountId, isActive) }
    }

    fun updateDebitCard(accountId: Long, alias: String, bank: String, last4: String) {
        viewModelScope.launch { cardRepository.updateCardBasics(accountId, alias, bank, last4) }
    }

    fun updateCreditCard(
        accountId: Long,
        alias: String,
        bank: String,
        last4: String,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
    ) {
        viewModelScope.launch {
            cardRepository.updateCardBasics(accountId, alias, bank, last4)
            cardRepository.updateCreditLimits(accountId, limitCents, cutOffDay, paymentDueDay, alertThresholdPercent)
        }
    }

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun deleteCard(accountId: Long) {
        viewModelScope.launch {
            val deleted = cardRepository.deleteCard(accountId)
            if (!deleted) {
                _deleteBlockedMessage.value = "Esta tarjeta ya tiene movimientos registrados, no se puede borrar. Puedes desactivarla."
            }
        }
    }

    fun clearDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    fun addExtraLast4(accountId: Long, last4: String, label: String?) {
        viewModelScope.launch { cardRepository.addExtraLast4(accountId, last4, label) }
    }

    fun removeExtraLast4(id: Long) {
        viewModelScope.launch { cardRepository.removeExtraLast4(id) }
    }
}
