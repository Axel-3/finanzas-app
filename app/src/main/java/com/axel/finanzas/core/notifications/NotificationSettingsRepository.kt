package com.axel.finanzas.core.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "finanzas_prefs")
private val MONITORED_PACKAGES_KEY = stringSetPreferencesKey("monitored_packages")
private val SEEDED_RULES_VERSION_KEY = intPreferencesKey("seeded_rules_version")

/**
 * Qué apps de banco escucha el [com.axel.finanzas.core.notifications.FinanzasNotificationListenerService].
 * Configurable desde "Más -> Reglas de notificaciones" — nunca hardcodeado.
 */
@Singleton
class NotificationSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val monitoredPackages: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[MONITORED_PACKAGES_KEY] ?: emptySet() }

    suspend fun setMonitoredPackages(packages: Set<String>) {
        context.dataStore.edit { it[MONITORED_PACKAGES_KEY] = packages }
    }

    suspend fun addMonitoredPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[MONITORED_PACKAGES_KEY] ?: emptySet()
            prefs[MONITORED_PACKAGES_KEY] = current + packageName
        }
    }

    suspend fun removeMonitoredPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[MONITORED_PACKAGES_KEY] ?: emptySet()
            prefs[MONITORED_PACKAGES_KEY] = current - packageName
        }
    }

    /** Ver [com.axel.finanzas.core.database.DefaultBankRules.VERSION]. */
    suspend fun getSeededRulesVersion(): Int = context.dataStore.data.first()[SEEDED_RULES_VERSION_KEY] ?: 0

    suspend fun setSeededRulesVersion(version: Int) {
        context.dataStore.edit { it[SEEDED_RULES_VERSION_KEY] = version }
    }
}
