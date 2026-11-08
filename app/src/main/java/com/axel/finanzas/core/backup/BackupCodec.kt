package com.axel.finanzas.core.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM con clave derivada de la contraseña por PBKDF2. Formato del
 * archivo: [1 byte versión][16 bytes salt][12 bytes IV][ciphertext+tag].
 * Todo en un solo archivo para que respaldo/restauración no dependan de
 * guardar la sal o el IV en otro lado.
 */
object BackupCodec {
    private const val VERSION: Byte = 1
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 310_000

    fun encrypt(plainText: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plainText)

        return byteArrayOf(VERSION) + salt + iv + cipherText
    }

    /** @throws IllegalArgumentException si el archivo no tiene el formato esperado */
    fun decrypt(encrypted: ByteArray, passphrase: String): ByteArray {
        require(encrypted.size > 1 + SALT_SIZE + IV_SIZE) { "Archivo de respaldo inválido o corrupto" }
        require(encrypted[0] == VERSION) { "Versión de respaldo no soportada" }

        val salt = encrypted.copyOfRange(1, 1 + SALT_SIZE)
        val iv = encrypted.copyOfRange(1 + SALT_SIZE, 1 + SALT_SIZE + IV_SIZE)
        val cipherText = encrypted.copyOfRange(1 + SALT_SIZE + IV_SIZE, encrypted.size)
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        // Si la contraseña es incorrecta, GCM revienta aquí con
        // AEADBadTagException (falla de autenticación) en vez de devolver
        // basura silenciosamente — es justo lo que queremos.
        return cipher.doFinal(cipherText)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
