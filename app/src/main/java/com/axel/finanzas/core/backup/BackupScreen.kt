package com.axel.finanzas.core.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.FinCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var exportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            exportUri = uri
            showExportDialog = true
        }
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            restoreUri = uri
            showRestoreDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo y restauración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Regresar")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(
                start = FinTheme.spacing.lg,
                end = FinTheme.spacing.lg,
                top = FinTheme.spacing.lg,
                bottom = FinTheme.spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            item {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(text = "Respaldo cifrado", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Guarda cuentas, movimientos, tarjetas, metas y presupuestos en un " +
                                "archivo cifrado con una contraseña que tú eliges. Sin esa contraseña, " +
                                "el archivo es ilegible — guárdala en un lugar seguro, no hay forma de " +
                                "recuperarla si la olvidas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.height(FinTheme.spacing.sm))
                        Button(
                            onClick = { createDocumentLauncher.launch("finanzas_respaldo.enc") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Crear respaldo")
                        }
                    }
                }
            }
            item {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(text = "Restaurar desde un respaldo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Reemplaza TODOS tus datos actuales con los del archivo. Úsalo en un " +
                                "teléfono nuevo o después de reinstalar, no para mezclar información.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.height(FinTheme.spacing.sm))
                        Button(
                            onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Elegir archivo de respaldo")
                        }
                    }
                }
            }
            statusMessage?.let { message ->
                item {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showExportDialog) {
        PassphraseDialog(
            title = "Contraseña del respaldo",
            confirmLabel = "Exportar",
            onDismiss = { showExportDialog = false; exportUri = null },
            onConfirm = { passphrase ->
                val uri = exportUri
                showExportDialog = false
                exportUri = null
                if (uri != null) {
                    scope.launch {
                        runCatching {
                            val bytes = viewModel.export(passphrase)
                            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        }.onSuccess {
                            statusMessage = "Respaldo creado correctamente."
                        }.onFailure {
                            statusMessage = "No se pudo crear el respaldo: ${it.message}"
                        }
                    }
                }
            },
        )
    }

    if (showRestoreDialog) {
        PassphraseDialog(
            title = "Contraseña del respaldo a restaurar",
            confirmLabel = "Restaurar (reemplaza todo)",
            onDismiss = { showRestoreDialog = false; restoreUri = null },
            onConfirm = { passphrase ->
                val uri = restoreUri
                showRestoreDialog = false
                restoreUri = null
                if (uri != null) {
                    scope.launch {
                        runCatching {
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: error("No se pudo leer el archivo")
                            viewModel.restore(bytes, passphrase)
                        }.onSuccess {
                            statusMessage = "Datos restaurados correctamente."
                        }.onFailure {
                            statusMessage = "No se pudo restaurar: contraseña incorrecta o archivo dañado."
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun PassphraseDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(enabled = passphrase.length >= 4, onClick = { onConfirm(passphrase) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
