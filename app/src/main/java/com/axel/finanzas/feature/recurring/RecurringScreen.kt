package com.axel.finanzas.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.RecurrenceFrequency
import com.axel.finanzas.core.database.entity.RecurringPaymentEntity
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AccountDropdown
import com.axel.finanzas.core.designsystem.components.AmountSign
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.CategoryDropdown
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onBack: () -> Unit, viewModel: RecurringViewModel = hiltViewModel()) {
    val payments by viewModel.recurringPayments.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagos recurrentes") },
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
                ComingSoonScreen(title = "Sin pagos recurrentes todavía")
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
                RecurringRow(
                    payment = payment,
                    onMarkPaid = { viewModel.markAsPaid(payment) },
                    onDeactivate = { viewModel.deactivate(payment) },
                )
            }
        }
    }

    if (showAddSheet) {
        AddRecurringSheet(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddSheet = false },
            onSave = { name, amount, freq, accountId, categoryId, type, startDate, reminderDays ->
                viewModel.addRecurringPayment(name, amount, freq, accountId, categoryId, type, startDate, reminderDays)
                showAddSheet = false
            },
        )
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun frequencyLabel(frequency: RecurrenceFrequency): String = when (frequency) {
    RecurrenceFrequency.WEEKLY -> "Semanal"
    RecurrenceFrequency.BIWEEKLY -> "Quincenal"
    RecurrenceFrequency.MONTHLY -> "Mensual"
    RecurrenceFrequency.YEARLY -> "Anual"
}

@Composable
private fun RecurringRow(payment: RecurringPaymentEntity, onMarkPaid: () -> Unit, onDeactivate: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = payment.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${frequencyLabel(payment.frequency)} · próximo: ${dateFormatter.format(payment.nextDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AmountText(
                    amountCents = payment.amountCents,
                    sign = if (payment.type == MovementType.INCOME) AmountSign.POSITIVE else AmountSign.NEGATIVE,
                    style = MaterialTheme.typography.titleMedium,
                    animate = false,
                )
            }
            Spacer(Modifier.height(FinTheme.spacing.sm))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDeactivate) { Text("Desactivar") }
                Spacer(Modifier.width(FinTheme.spacing.sm))
                Button(onClick = onMarkPaid) { Text("Marcar pagado") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringSheet(
    accounts: List<com.axel.finanzas.core.database.entity.AccountEntity>,
    categories: List<com.axel.finanzas.core.database.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amountCents: Long,
        frequency: RecurrenceFrequency,
        accountId: Long,
        categoryId: Long?,
        type: MovementType,
        startDate: LocalDate,
        reminderDaysBefore: Int,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(MovementType.EXPENSE) }
    var frequency by rememberSaveable { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reminderText by rememberSaveable { mutableStateOf("2") }

    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val relevantCategories = categories.filter {
        it.type == if (type == MovementType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    }
    val canSave = name.isNotBlank() && amountCents > 0 && accountId != null && categoryId != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinTheme.spacing.lg)
                .padding(bottom = FinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            Text(text = "Nuevo pago recurrente", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre (ej. Renta, Netflix)") },
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(MovementType.EXPENSE, MovementType.INCOME).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = { type = option; categoryId = null },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    ) {
                        Text(if (option == MovementType.EXPENSE) "Gasto" else "Ingreso")
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

            Text(text = "Frecuencia", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RecurrenceFrequency.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = frequency == option,
                        onClick = { frequency = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = RecurrenceFrequency.entries.size),
                    ) {
                        Text(frequencyLabel(option))
                    }
                }
            }

            AccountDropdown(label = "Cuenta", accounts = accounts, selectedId = accountId, onSelected = { accountId = it })
            CategoryDropdown(categories = relevantCategories, selectedId = categoryId, onSelected = { categoryId = it })

            OutlinedTextField(
                value = reminderText,
                onValueChange = { input -> reminderText = input.filter(Char::isDigit).take(2) },
                label = { Text("Recordar días antes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        amountCents,
                        frequency,
                        accountId!!,
                        categoryId,
                        type,
                        LocalDate.now(),
                        reminderText.toIntOrNull() ?: 2,
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
        }
    }
}
