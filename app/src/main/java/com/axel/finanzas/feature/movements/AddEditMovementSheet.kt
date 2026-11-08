package com.axel.finanzas.feature.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.CategoryEntity
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AccountDropdown
import com.axel.finanzas.core.designsystem.components.CategoryDropdown
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMovementSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSaveExpenseOrIncome: (type: MovementType, amountCents: Long, accountId: Long, categoryId: Long?, merchant: String?, note: String?, occurredAt: Instant) -> Unit,
    onSaveTransfer: (amountCents: Long, fromAccountId: Long, toAccountId: Long, note: String?, occurredAt: Instant) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    var type by rememberSaveable { mutableStateOf(MovementType.EXPENSE) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    var destinationAccountId by rememberSaveable { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var merchant by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var occurredAt by remember { mutableStateOf(Instant.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val relevantCategories = categories.filter {
        it.type == if (type == MovementType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    }

    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val isTransfer = type == MovementType.TRANSFER
    val canSave = amountCents > 0 &&
        accountId != null &&
        (if (isTransfer) destinationAccountId != null && destinationAccountId != accountId else categoryId != null)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinTheme.spacing.lg)
                .padding(bottom = FinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            Text(text = "Nuevo movimiento", style = MaterialTheme.typography.titleLarge)

            val movementOptions = remember {
                listOf(MovementType.EXPENSE, MovementType.INCOME, MovementType.TRANSFER)
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                movementOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            categoryId = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = movementOptions.size),
                    ) {
                        Text(
                            when (option) {
                                MovementType.EXPENSE -> "Gasto"
                                MovementType.INCOME -> "Ingreso"
                                else -> "Transferencia"
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Monto (MXN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            AccountDropdown(
                label = if (isTransfer) "Desde" else "Cuenta",
                accounts = accounts,
                selectedId = accountId,
                onSelected = { accountId = it },
            )

            if (isTransfer) {
                AccountDropdown(
                    label = "Hacia",
                    accounts = accounts.filter { it.id != accountId },
                    selectedId = destinationAccountId,
                    onSelected = { destinationAccountId = it },
                )
            } else {
                CategoryDropdown(
                    categories = relevantCategories,
                    selectedId = categoryId,
                    onSelected = { categoryId = it },
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio o concepto (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            val dateLabel = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm")
                .format(occurredAt.atZone(ZoneId.systemDefault()))
            TextButton(onClick = { showDatePicker = true }) {
                Text("Fecha: $dateLabel · Cambiar")
            }

            val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
            Button(
                onClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (isTransfer) {
                        onSaveTransfer(amountCents, accountId!!, destinationAccountId!!, note.ifBlank { null }, occurredAt)
                    } else {
                        onSaveExpenseOrIncome(
                            type,
                            amountCents,
                            accountId!!,
                            categoryId,
                            merchant.ifBlank { null },
                            note.ifBlank { null },
                            occurredAt,
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = occurredAt.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val currentTime = LocalTime.now()
                        occurredAt = pickedDate.atTime(currentTime).atZone(ZoneId.systemDefault()).toInstant()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
