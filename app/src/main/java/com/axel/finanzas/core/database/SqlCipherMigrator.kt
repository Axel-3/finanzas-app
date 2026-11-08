package com.axel.finanzas.core.database

import com.axel.finanzas.core.security.DatabaseKeyProvider
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * Cifra `finanzas.db` EN SU LUGAR la primera vez que corre en un
 * teléfono que ya tenía la base de datos en SQLite plano. Instalaciones
 * nuevas no pasan por aquí: Room crea el archivo ya cifrado desde cero
 * vía `SupportOpenHelperFactory` en `DatabaseModule`.
 *
 * Diseño a prueba de que se interrumpa a la mitad (teléfono se apaga,
 * la app se cierra, etc.): el archivo original NUNCA se toca hasta que
 * el export completo ya se escribió en un archivo temporal aparte. Si
 * algo falla entre medio, el archivo original sigue intacto y sin
 * cifrar, y el próximo arranque simplemente lo vuelve a intentar.
 */
object SqlCipherMigrator {

    private val PLAINTEXT_SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun migrateIfNeeded(dbFile: File, keyBytes: ByteArray) {
        if (!isPlaintextSqlite(dbFile)) return

        val tempEncrypted = File(dbFile.parentFile, "finanzas_encrypting_tmp.db")
        tempEncrypted.delete()
        // ATTACH DATABASE da "unable to open database" (code 14) en este
        // binding si el archivo destino no existe todavía — hay que
        // crearlo vacío primero desde Java, ATTACH no hace su propio
        // O_CREAT aquí (confirmado probando en dispositivo real).
        tempEncrypted.createNewFile()

        // Sin password/key -> se abre tal cual, como el SQLite plano que es.
        val plainDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            // Room deja la base en modo WAL; pasar a DELETE antes de
            // adjuntar fusiona el -wal pendiente al archivo principal (sin
            // esto el export podría no ver escrituras recientes).
            plainDb.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }

            val hexKey = DatabaseKeyProvider.bytesToHex(keyBytes)
            plainDb.execSQL("ATTACH DATABASE '${tempEncrypted.absolutePath}' AS encrypted KEY x'$hexKey'")
            plainDb.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { it.moveToFirst() }
            plainDb.execSQL("DETACH DATABASE encrypted")
        } finally {
            plainDb.close()
        }

        // Todo el export ya está a salvo en tempEncrypted — ahora sí se
        // reemplaza el original, incluyendo sus sidecars de WAL.
        dbFile.delete()
        File(dbFile.parentFile, "${dbFile.name}-wal").delete()
        File(dbFile.parentFile, "${dbFile.name}-shm").delete()
        check(tempEncrypted.renameTo(dbFile)) { "No se pudo renombrar la base de datos cifrada a su nombre final" }
    }

    private fun isPlaintextSqlite(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() < PLAINTEXT_SQLITE_HEADER.size) return false
        val header = ByteArray(PLAINTEXT_SQLITE_HEADER.size)
        dbFile.inputStream().use { it.read(header) }
        return header.contentEquals(PLAINTEXT_SQLITE_HEADER)
    }
}
