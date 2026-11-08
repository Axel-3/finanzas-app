package com.axel.finanzas.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.database.entity.AccountEntity
import com.axel.finanzas.core.database.entity.AccountType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.SectionHeader
import androidx.compose.material3.Icon

/** "Más": cuentas simples, accesos a las secciones secundarias, y lo que falta para fases futuras. */
@Composable
fun SettingsScreen(
    onNavigateToNotificationRules: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToFuturePayments: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = FinTheme.spacing.lg,
            end = FinTheme.spacing.lg,
            top = FinTheme.spacing.lg,
            bottom = FinTheme.spacing.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.xl),
    ) {
        item(key = "accounts_header") {
            SectionHeader(title = "Cuentas", actionLabel = "+ Nueva", onActionClick = { showAddDialog = true })
        }
        items(accounts, key = { it.id }) { account ->
            AccountRow(account = account, onToggleActive = { viewModel.toggleActive(account) })
        }

        item(key = "sections_header") {
            SectionHeader(title = "Más secciones")
        }
        item(key = "nav_rows") {
            Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.md)) {
                NavRow(label = "Reglas de notificaciones", onClick = onNavigateToNotificationRules)
                NavRow(label = "Pagos recurrentes", onClick = onNavigateToRecurring)
                NavRow(label = "Pagos futuros", onClick = onNavigateToFuturePayments)
                NavRow(label = "Calendario de pagos", onClick = onNavigateToCalendar)
                NavRow(label = "Presupuestos", onClick = onNavigateToBudgets)
                NavRow(label = "Reportes y exportar", onClick = onNavigateToReports)
                NavRow(label = "Seguridad (PIN / huella)", onClick = onNavigateToSecurity)
                NavRow(label = "Respaldo y restauración", onClick = onNavigateToBackup)
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balanceCents ->
                viewModel.createAccount(name, type, balanceCents)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AccountRow(account: AccountEntity, onToggleActive: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    text = accountTypeLabel(account.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountText(amountCents = account.currentBalanceCents, animate = false, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(FinTheme.spacing.sm))
            Switch(checked = account.isActive, onCheckedChange = { onToggleActive() })
        }
    }
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "Efectivo"
    AccountType.DEBIT -> "Débito"
    AccountType.CREDIT -> "Crédito"
    AccountType.WALLET -> "Billetera digital"
    AccountType.SAVINGS -> "Ahorros"
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, initialBalanceCents: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var balanceText by remember { mutableStateOf("") }
    val creatableTypes = remember { listOf(AccountType.CASH, AccountType.SAVINGS, AccountType.WALLET) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    creatableTypes.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = creatableTypes.size),
                        ) {
                            Text(accountTypeLabel(option))
                        }
                    }
                }
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input -> balanceText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Saldo inicial (MXN)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val cents = (balanceText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                    onConfirm(name.trim(), type, cents)
                },
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
