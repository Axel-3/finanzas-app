package com.axel.finanzas.core.backup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {

    suspend fun export(passphrase: String): ByteArray = backupManager.exportEncrypted(passphrase)

    /** Lanza la excepción de BackupCodec/BackupManager tal cual si algo sale mal (contraseña incorrecta, archivo corrupto). */
    suspend fun restore(data: ByteArray, passphrase: String) = backupManager.restoreFromEncrypted(data, passphrase)
}
