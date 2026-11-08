package com.axel.finanzas.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Llave de 256 bits para cifrar `finanzas.db` con SQLCipher (ver
 * [com.axel.finanzas.core.database.SqlCipherMigrator] y `DatabaseModule`).
 *
 * NO es lo mismo que el PIN/huella de [SecurityPreferencesRepository] —
 * ese bloquea la UI de la app; esto protege el archivo de la base de
 * datos en sí si alguien lo copia directo del teléfono (por ejemplo con
 * `adb` + `run-as`). Por eso nunca se le pide al usuario: es una llave
 * aleatoria generada una sola vez y guardada cifrada con Android
 * Keystore — el hardware seguro del teléfono, que no se puede copiar
 * junto con el archivo. Sin acceso a ESE teléfono desbloqueado, la
 * llave es inútil aunque alguien tenga el archivo de preferencias Y la
 * base de datos.
 */
object DatabaseKeyProvider {
    private const val PREFS_FILE_NAME = "finanzas_db_key"
    private const val KEY_PREF_NAME = "db_key_hex"
    private const val KEY_SIZE_BYTES = 32

    @Volatile
    private var cached: ByteArray? = null

    @Synchronized
    fun getOrCreateKey(context: Context): ByteArray {
        cached?.let { return it }

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        val existingHex = prefs.getString(KEY_PREF_NAME, null)
        val keyBytes = if (existingHex != null) {
            hexToBytes(existingHex)
        } else {
            val generated = ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
            prefs.edit().putString(KEY_PREF_NAME, bytesToHex(generated)).apply()
            generated
        }

        cached = keyBytes
        return keyBytes
    }

    fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte() }
}
