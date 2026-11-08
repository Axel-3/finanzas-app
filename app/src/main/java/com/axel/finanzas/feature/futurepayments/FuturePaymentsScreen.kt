package com.axel.finanzas.feature.futurepayments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.database.entity.FuturePaymentEntity
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AccountDropdown
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.CategoryDropdown
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturePaymentsScreen(onBack: () -> Unit, viewModel: FuturePaymentsViewModel = hiltViewModel()) {
    val payments by viewModel.payments.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagos futuros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Regresar")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Agregar")
            }
        },
    ) { innerPadding ->
        if (payments.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                ComingSoonScreen(title = "Sin pagos futuros pendientes")
            }
            return@Scaffold
        }
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
            items(payments, key = { it.id }) { payment ->
                FuturePaymentRow(payment = payment, onMarkPaid = { viewModel.markAsPaid(payment) })
            }
        }
    }

    if (showAddSheet) {
        AddFuturePaymentSheet(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddSheet = false },
            onSave = { name, amount, dueDate, accountId, categoryId, reminderDays ->
                viewModel.addFuturePayment(name, amount, dueDate, accountId, categoryId, reminderDays)
                showAddSheet = false
            },
        )
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun FuturePaymentRow(payment: FuturePaymentEntity, onMarkPaid: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = payment.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Vence el ${dateFormatter.format(payment.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AmountText(amountCents = payment.amountCents, style = MaterialTheme.typography.titleMedium, animate = false)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onMarkPaid) { Text("Marcar pagado") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFuturePaymentSheet(
    accounts: List<com.axel.finanzas.core.database.entity.AccountEntity>,
    categories: List<com.axel.finanzas.core.database.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, amountCents: Long, dueDate: LocalDate, accountId: Long?, categoryId: Long?, reminderDaysBefore: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reminderText by rememberSaveable { mutableStateOf("2") }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
    val canSave = name.isNotBlank() && amountCents > 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinTheme.spacing.lg)
                .padding(bottom = FinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            Text(text = "Nuevo pago futuro", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre (ej. Tenencia, Seguro)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Monto (MXN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(onClick = { showDatePicker = true }) {
                Text("Vence el ${dateFormatter.format(dueDate)} · Cambiar")
            }

            AccountDropdown(
                label = "Cuenta (opcional)",
                accounts = accounts,
                selectedId = accountId,
                onSelected = { accountId = it },
            )
            CategoryDropdown(categories = expenseCategories, selectedId = categoryId, onSelected = { categoryId = it })

            OutlinedTextField(
                value = reminderText,
                onValueChange = { input -> reminderText = input.filter(Char::isDigit).take(2) },
                label = { Text("Recordar días antes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(name.trim(), amountCents, dueDate, accountId, categoryId, reminderText.toIntOrNull() ?: 2)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
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
