package com.axel.finanzas.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.formatCents
import com.axel.finanzas.domain.model.CardUi

/**
 * Ajuste manual de saldo. Queda guardado como un movimiento
 * BALANCE_ADJUSTMENT con el antes/después (ver [com.axel.finanzas.data.repository.MovementRepositoryImpl]);
 * no cuenta como gasto ni ingreso en reportes.
 */
@Composable
fun BalanceAdjustmentDialog(
    card: CardUi,
    onDismiss: () -> Unit,
    onConfirm: (newBalanceCents: Long, note: String?) -> Unit,
) {
    var newBalanceText by remember { mutableStateOf((card.balanceCents / 100.0).toString()) }
    var note by remember { mutableStateOf("") }
    val newBalanceCents = (newBalanceText.toDoubleOrNull()?.times(100))?.toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar saldo de ${card.alias}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.md)) {
                Text(
                    text = "Saldo actual: ${formatCents(card.balanceCents)}" +
                        if (card.isCredit) " (usado)" else "",
                )
                OutlinedTextField(
                    value = newBalanceText,
                    onValueChange = { input -> newBalanceText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text(if (card.isCredit) "Nuevo saldo usado (MXN)" else "Nuevo saldo (MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = newBalanceCents != null,
                onClick = { onConfirm(newBalanceCents!!, note.ifBlank { null }) },
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
