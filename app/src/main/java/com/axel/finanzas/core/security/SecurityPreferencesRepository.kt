package com.axel.finanzas.core.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore by preferencesDataStore(name = "finanzas_security")
private val LOCK_ENABLED_KEY = booleanPreferencesKey("lock_enabled")
private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")

/**
 * Bloqueo de la app (PIN/huella) — NO es lo mismo que cifrar la base de
 * datos (eso lo maneja SQLCipher, ver [com.axel.finanzas.core.database.SqlCipherMigrator]).
 * Esto evita que alguien que toma el teléfono ya desbloqueado abra la app.
 */
@Singleton
class SecurityPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isLockEnabled: Flow<Boolean> = context.securityDataStore.data.map { it[LOCK_ENABLED_KEY] ?: false }
    val hasPinSet: Flow<Boolean> = context.securityDataStore.data.map { !it[PIN_HASH_KEY].isNullOrEmpty() }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[LOCK_ENABLED_KEY] = enabled }
    }

    suspend fun setPin(rawPin: String) {
        context.securityDataStore.edit { it[PIN_HASH_KEY] = hash(rawPin) }
    }

    suspend fun verifyPin(rawPin: String): Boolean {
        val stored = context.securityDataStore.data.map { it[PIN_HASH_KEY] }.first()
        return stored != null && stored == hash(rawPin)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
