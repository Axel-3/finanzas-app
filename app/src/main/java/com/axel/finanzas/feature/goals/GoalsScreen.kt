package com.axel.finanzas.feature.goals

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.AnimatedProgressBar
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.formatCents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val goals by viewModel.goals.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var contributingTo by remember { mutableStateOf<GoalUi?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Agregar meta")
            }
        },
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                ComingSoonScreen(title = "Aún no tienes metas de ahorro")
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
            items(goals, key = { it.id }) { goal ->
                GoalRow(
                    goal = goal,
                    onContribute = { contributingTo = goal },
                    onDelete = { viewModel.deleteGoal(goal.id) },
                )
            }
        }
    }

    if (showAddSheet) {
        AddGoalSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, amount, date ->
                viewModel.addGoal(name, amount, date)
                showAddSheet = false
            },
        )
    }

    contributingTo?.let { goal ->
        ContributeDialog(
            goal = goal,
            onDismiss = { contributingTo = null },
            onConfirm = { amount, note ->
                viewModel.addContribution(goal.id, amount, note)
                contributingTo = null
            },
        )
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun GoalRow(goal: GoalUi, onContribute: () -> Unit, onDelete: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = goal.name, style = MaterialTheme.typography.titleSmall)
                goal.targetDate?.let {
                    Text(
                        text = "Meta: ${dateFormatter.format(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(FinTheme.spacing.sm))
            AmountText(amountCents = goal.currentAmountCents, style = MaterialTheme.typography.headlineSmall, animate = false)
            Text(
                text = "de ${formatCents(goal.targetAmountCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FinTheme.spacing.sm))
            AnimatedProgressBar(progress = goal.progress, color = FinTheme.extendedColors.income)
            goal.perMonthCents?.let { perMonth ->
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Text(
                    text = "Ahorra ${formatCents(perMonth)} al mes para llegar a tiempo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(FinTheme.spacing.sm))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) { Text("Eliminar") }
                Spacer(Modifier.width(FinTheme.spacing.sm))
                Button(onClick = onContribute) { Text("+ Aportar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, amountCents: Long, targetDate: LocalDate?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
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
            Text(text = "Nueva meta de ahorro", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre (ej. Vacaciones)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Monto objetivo (MXN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showDatePicker = true }) {
                Text(targetDate?.let { "Fecha límite: ${dateFormatter.format(it)} · Cambiar" } ?: "Agregar fecha límite (opcional)")
            }
            Button(
                onClick = { onSave(name.trim(), amountCents, targetDate) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        targetDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ContributeDialog(goal: GoalUi, onDismiss: () -> Unit, onConfirm: (amountCents: Long, note: String?) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aportar a ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.md)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monto (MXN)") },
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
            Button(enabled = amountCents > 0, onClick = { onConfirm(amountCents, note.ifBlank { null }) }) { Text("Aportar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
