package com.axel.finanzas.feature.settings

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axel.finanzas.core.database.dao.NotificationRuleDao
import com.axel.finanzas.core.database.entity.NotificationRuleEntity
import com.axel.finanzas.core.notifications.InstalledApp
import com.axel.finanzas.core.notifications.NotificationSettingsRepository
import com.axel.finanzas.core.notifications.NotificationSimulator
import com.axel.finanzas.core.notifications.getInstalledLaunchableApps
import com.axel.finanzas.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: NotificationSettingsRepository,
    private val ruleDao: NotificationRuleDao,
    private val cardRepository: CardRepository,
) : AndroidViewModel(application) {

    val monitoredPackages: StateFlow<Set<String>> = settingsRepository.monitoredPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val rules: StateFlow<List<NotificationRuleEntity>> = ruleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    init {
        viewModelScope.launch {
            _installedApps.value = getInstalledLaunchableApps(application)
        }
    }

    fun isNotificationAccessGranted(): Boolean {
        val app = getApplication<Application>()
        return NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)
    }

    fun toggleMonitored(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) settingsRepository.addMonitoredPackage(packageName) else settingsRepository.removeMonitoredPackage(packageName)
        }
    }

    fun addRule(rule: NotificationRuleEntity) {
        viewModelScope.launch { ruleDao.insert(rule) }
    }

    fun deleteRule(rule: NotificationRuleEntity) {
        viewModelScope.launch { ruleDao.delete(rule) }
    }

    private val _testResult = MutableStateFlow<NotificationSimulator.Outcome?>(null)
    val testResult: StateFlow<NotificationSimulator.Outcome?> = _testResult.asStateFlow()

    /**
     * Simula qué haría el listener real con este texto, sin escribir
     * nada en la base de datos (ver [NotificationSimulator]).
     */
    fun testNotification(packageName: String, text: String) {
        viewModelScope.launch {
            val activeRules = ruleDao.getActiveForPackage(packageName)
            _testResult.value = NotificationSimulator.simulate(text, activeRules, cardRepository)
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }
}
