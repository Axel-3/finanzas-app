package com.axel.finanzas.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.NotificationInboxDao
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.MovementEntity
import com.axel.finanzas.core.database.entity.MovementSource
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.NotificationInboxEntity
import com.axel.finanzas.core.database.entity.NotificationInboxStatus
import com.axel.finanzas.domain.repository.AccountRepository
import com.axel.finanzas.domain.repository.CategoryRepository
import com.axel.finanzas.domain.repository.MovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxDao: NotificationInboxDao,
    private val movementRepository: MovementRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val pending: StateFlow<List<NotificationInboxEntity>> = inboxDao.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolve(
        entry: NotificationInboxEntity,
        accountId: Long,
        categoryId: Long?,
        type: MovementType,
        amountCents: Long,
        merchant: String?,
    ) {
        viewModelScope.launch {
            val movementId = movementRepository.addMovement(
                MovementEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    type = type,
                    amountCents = amountCents,
                    merchant = merchant,
                    occurredAt = entry.postedAt,
                    source = MovementSource.NOTIFICATION,
                    rawNotificationText = entry.rawText,
                ),
            )
            inboxDao.resolve(entry.id, NotificationInboxStatus.RESOLVED, movementId)
        }
    }

    fun discard(entry: NotificationInboxEntity) {
        viewModelScope.launch { inboxDao.resolve(entry.id, NotificationInboxStatus.DISCARDED, null) }
    }
}
