package com.axel.finanzas.core.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val repository: SecurityPreferencesRepository,
) : ViewModel() {

    /**
     * null mientras no sabemos todavía si el bloqueo está activo (DataStore
     * no ha emitido su primer valor). El gate NUNCA debe mostrar el
     * contenido real mientras esto sea null — mostrarlo sería un flash de
     * contenido sin protección antes de saber si debía estar bloqueado.
     */
    val isLockEnabled: StateFlow<Boolean?> = repository.isLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun verifyPin(pin: String): Boolean = repository.verifyPin(pin)
}
