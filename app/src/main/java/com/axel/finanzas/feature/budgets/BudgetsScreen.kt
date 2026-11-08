package com.axel.finanzas.feature.budgets

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.AnimatedProgressBar
import com.axel.finanzas.core.designsystem.components.CategoryDropdown
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.formatCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(onBack: () -> Unit, viewModel: BudgetsViewModel = hiltViewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos del mes") },
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
        if (budgets.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                ComingSoonScreen(title = "Sin presupuestos este mes")
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
            items(budgets, key = { it.budgetId }) { budget ->
                BudgetRow(budget = budget, onDelete = { viewModel.deleteBudget(budget.budgetId) })
            }
        }
    }

    if (showAddSheet) {
        AddBudgetSheet(
            categories = categories,
            onDismiss = { showAddSheet = false },
            onSave = { categoryId, amount ->
                viewModel.setBudget(categoryId, amount)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun BudgetRow(budget: BudgetUi, onDelete: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = budget.categoryName, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FinTheme.spacing.xs)) {
                AmountText(amountCents = budget.spentCents, style = MaterialTheme.typography.titleMedium, animate = false)
                Text(
                    text = "de ${formatCents(budget.amountCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(FinTheme.spacing.sm))
            AnimatedProgressBar(
                progress = budget.progress,
                color = when {
                    budget.isOver100 -> FinTheme.extendedColors.expense
                    budget.isOver80 -> FinTheme.extendedColors.expense
                    else -> FinTheme.extendedColors.income
                },
            )
            if (budget.isOver100) {
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Text(
                    text = "Ya pasaste el presupuesto de este mes",
                    style = MaterialTheme.typography.labelSmall,
                    color = FinTheme.extendedColors.expense,
                )
            } else if (budget.isOver80) {
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Text(
                    text = "Vas arriba del 80% del presupuesto",
                    style = MaterialTheme.typography.labelSmall,
                    color = FinTheme.extendedColors.expense,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetSheet(
    categories: List<com.axel.finanzas.core.database.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amountCents: Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var categoryId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id) }
    var amountText by rememberSaveable { mutableStateOf("") }
    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val canSave = categoryId != null && amountCents > 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinTheme.spacing.lg)
                .padding(bottom = FinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            Text(text = "Nuevo presupuesto", style = MaterialTheme.typography.titleLarge)
            CategoryDropdown(categories = categories, selectedId = categoryId, onSelected = { categoryId = it })
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Monto mensual (MXN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(categoryId!!, amountCents) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
        }
    }
}
