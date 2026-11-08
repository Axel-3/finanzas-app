package com.axel.finanzas.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.domain.model.CardUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardSheet(
    editing: CardUi? = null,
    onDismiss: () -> Unit,
    onSaveDebit: (alias: String, bank: String, last4: String, initialBalanceCents: Long) -> Unit = { _, _, _, _ -> },
    onSaveCredit: (
        alias: String,
        bank: String,
        last4: String,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
        initialBalanceCents: Long,
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateDebit: (accountId: Long, alias: String, bank: String, last4: String) -> Unit = { _, _, _, _ -> },
    onUpdateCredit: (
        accountId: Long,
        alias: String,
        bank: String,
        last4: String,
        limitCents: Long,
        cutOffDay: Int,
        paymentDueDay: Int,
        alertThresholdPercent: Int,
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDelete: (accountId: Long) -> Unit = {},
    onAddExtraLast4: (accountId: Long, last4: String, label: String?) -> Unit = { _, _, _ -> },
    onRemoveExtraLast4: (id: Long) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState()
    val isEditing = editing != null
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var extraLast4Input by remember { mutableStateOf("") }
    var extraLabelInput by remember { mutableStateOf("") }

    var type by rememberSaveable { mutableStateOf(editing?.type ?: AccountType.DEBIT) }
    var alias by rememberSaveable { mutableStateOf(editing?.alias ?: "") }
    var bank by rememberSaveable { mutableStateOf(editing?.bank ?: "") }
    var last4 by rememberSaveable { mutableStateOf(editing?.last4 ?: "") }
    var balanceText by rememberSaveable { mutableStateOf("") }
    var limitText by rememberSaveable { mutableStateOf(editing?.creditLimitCents?.let { (it / 100.0).toString() } ?: "") }
    var cutOffText by rememberSaveable { mutableStateOf(editing?.cutOffDay?.toString() ?: "") }
    var dueText by rememberSaveable { mutableStateOf(editing?.paymentDueDay?.toString() ?: "") }
    var alertText by rememberSaveable { mutableStateOf(editing?.alertThresholdPercent?.toString() ?: "70") }

    val isCredit = type == AccountType.CREDIT
    val balanceCents = (balanceText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val limitCents = (limitText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val cutOffDay = cutOffText.toIntOrNull()
    val dueDay = dueText.toIntOrNull()
    val alertPercent = alertText.toIntOrNull() ?: 70

    val canSave = alias.isNotBlank() &&
        last4.length == 4 && last4.all(Char::isDigit) &&
        (!isCredit || (limitCents > 0 && cutOffDay != null && cutOffDay in 1..31 && dueDay != null && dueDay in 1..31))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinTheme.spacing.lg)
                .padding(bottom = FinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            Text(text = if (isEditing) "Editar tarjeta" else "Nueva tarjeta", style = MaterialTheme.typography.titleLarge)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(AccountType.DEBIT, AccountType.CREDIT).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = { if (!isEditing) type = option },
                        enabled = !isEditing,
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    ) {
                        Text(if (option == AccountType.DEBIT) "Débito" else "Crédito")
                    }
                }
            }

            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Alias (ej. \"Nu Morada\")") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bank,
                onValueChange = { bank = it },
                label = { Text("Banco") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = last4,
                onValueChange = { input -> last4 = input.filter(Char::isDigit).take(4) },
                label = { Text("Últimos 4 dígitos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (editing != null) {
                Column {
                    Text(
                        text = "Otras tarjetas con el mismo saldo (física, digital, etc.)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(FinTheme.spacing.xs))
                    editing.extraLast4s.forEach { extra ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "•••• ${extra.last4}" + (extra.label?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRemoveExtraLast4(extra.id) }) {
                                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Quitar")
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = extraLast4Input,
                            onValueChange = { input -> extraLast4Input = input.filter(Char::isDigit).take(4) },
                            label = { Text("Últimos 4") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(FinTheme.spacing.sm))
                        OutlinedTextField(
                            value = extraLabelInput,
                            onValueChange = { extraLabelInput = it },
                            label = { Text("Etiqueta (opcional)") },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                onAddExtraLast4(editing.accountId, extraLast4Input, extraLabelInput.trim().ifBlank { null })
                                extraLast4Input = ""
                                extraLabelInput = ""
                            },
                            enabled = extraLast4Input.length == 4,
                        ) {
                            Icon(imageVector = Icons.Outlined.Add, contentDescription = "Agregar")
                        }
                    }
                }
            }

            if (isCredit) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input -> limitText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Límite de crédito (MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isEditing) {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { input -> balanceText = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("Saldo usado actual (MXN, opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(FinTheme.spacing.md)) {
                    OutlinedTextField(
                        value = cutOffText,
                        onValueChange = { input -> cutOffText = input.filter(Char::isDigit).take(2) },
                        label = { Text("Día de corte") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = dueText,
                        onValueChange = { input -> dueText = input.filter(Char::isDigit).take(2) },
                        label = { Text("Día límite de pago") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = alertText,
                    onValueChange = { input -> alertText = input.filter(Char::isDigit).take(3) },
                    label = { Text("Alertar al % de uso") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (!isEditing) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input -> balanceText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Saldo actual (MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isEditing) {
                Text(
                    text = "El saldo no se cambia aquí — usa \"Ajustar saldo\" para que quede registrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    when {
                        isEditing && isCredit -> onUpdateCredit(
                            editing!!.accountId, alias.trim(), bank.trim(), last4, limitCents, cutOffDay!!, dueDay!!, alertPercent,
                        )
                        isEditing -> onUpdateDebit(editing!!.accountId, alias.trim(), bank.trim(), last4)
                        isCredit -> onSaveCredit(alias.trim(), bank.trim(), last4, limitCents, cutOffDay!!, dueDay!!, alertPercent, balanceCents)
                        else -> onSaveDebit(alias.trim(), bank.trim(), last4, balanceCents)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditing) "Guardar cambios" else "Guardar")
            }

            if (isEditing) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Eliminar tarjeta", color = FinTheme.extendedColors.expense)
                }
            }
        }
    }

    if (showDeleteConfirm && editing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar \"${editing.alias}\"?") },
            text = {
                Text(
                    "Esto no se puede deshacer. Si la tarjeta ya tiene movimientos registrados, " +
                        "no se podrá borrar — en ese caso desactívala en vez de eliminarla.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(editing.accountId)
                }) {
                    Text("Eliminar", color = FinTheme.extendedColors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}
