package com.axel.finanzas.core.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val repository: SecurityPreferencesRepository,
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = repository.isLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasPinSet: StateFlow<Boolean> = repository.hasPinSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setPin(pin: String) {
        viewModelScope.launch { repository.setPin(pin) }
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLockEnabled(enabled) }
    }
}
