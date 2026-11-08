package com.axel.finanzas.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountSign
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.AnimatedProgressBar
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.formatCents
import com.axel.finanzas.domain.model.CardUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onNavigateToInbox: () -> Unit,
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsState()
    val pendingCount by viewModel.pendingInboxCount.collectAsState()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var adjustingCard by remember { mutableStateOf<CardUi?>(null) }
    // Por id, no por copia: así el sheet ve los extraLast4s recién
    // agregados/quitados sin tener que cerrarlo y reabrirlo.
    var editingCardId by remember { mutableStateOf<Long?>(null) }
    val editingCard = cards.firstOrNull { it.accountId == editingCardId }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deleteBlockedMessage) {
        deleteBlockedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteBlockedMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Agregar tarjeta")
            }
        },
    ) { innerPadding ->
        if (cards.isEmpty() && pendingCount == 0) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                ComingSoonScreen(title = "Aún no tienes tarjetas")
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
            if (pendingCount > 0) {
                item(key = "inbox_banner") {
                    InboxBanner(count = pendingCount, onClick = onNavigateToInbox)
                }
            }
            items(cards, key = { it.accountId }) { card ->
                CardRow(
                    card = card,
                    onAdjustBalance = { adjustingCard = card },
                    onToggleActive = { viewModel.setActive(card.accountId, !card.isActive) },
                    onEdit = { editingCardId = card.accountId },
                )
            }
        }
    }

    if (showAddSheet) {
        AddEditCardSheet(
            onDismiss = { showAddSheet = false },
            onSaveDebit = { alias, bank, last4, balance ->
                viewModel.createDebitCard(alias, bank, last4, balance)
                showAddSheet = false
            },
            onSaveCredit = { alias, bank, last4, limit, cutOff, due, alert, balance ->
                viewModel.createCreditCard(alias, bank, last4, limit, cutOff, due, alert, balance)
                showAddSheet = false
            },
        )
    }

    editingCard?.let { card ->
        AddEditCardSheet(
            editing = card,
            onDismiss = { editingCardId = null },
            onUpdateDebit = { accountId, alias, bank, last4 ->
                viewModel.updateDebitCard(accountId, alias, bank, last4)
                editingCardId = null
            },
            onUpdateCredit = { accountId, alias, bank, last4, limit, cutOff, due, alert ->
                viewModel.updateCreditCard(accountId, alias, bank, last4, limit, cutOff, due, alert)
                editingCardId = null
            },
            onDelete = { accountId ->
                viewModel.deleteCard(accountId)
                editingCardId = null
            },
            onAddExtraLast4 = { accountId, last4, label -> viewModel.addExtraLast4(accountId, last4, label) },
            onRemoveExtraLast4 = { id -> viewModel.removeExtraLast4(id) },
        )
    }

    adjustingCard?.let { card ->
        BalanceAdjustmentDialog(
            card = card,
            onDismiss = { adjustingCard = null },
            onConfirm = { newBalance, note ->
                viewModel.adjustBalance(card.accountId, newBalance, note)
                adjustingCard = null
            },
        )
    }
}

@Composable
private fun InboxBanner(count: Int, onClick: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = FinTheme.extendedColors.expense,
            )
            Spacer(Modifier.width(FinTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$count ${if (count == 1) "notificación" else "notificaciones"} por revisar", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "No se pudieron asignar solas a un movimiento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.IconButton(onClick = onClick) {
                Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = "Ver bandeja")
            }
        }
    }
}

@Composable
private fun CardRow(card: CardUi, onAdjustBalance: () -> Unit, onToggleActive: () -> Unit, onEdit: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = card.alias, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${card.bank ?: ""} •••• ${card.last4 ?: "----"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (card.extraLast4s.isNotEmpty()) {
                        Text(
                            text = "+ " + card.extraLast4s.joinToString(", ") {
                                "•••• ${it.last4}" + (it.label?.let { label -> " ($label)" } ?: "")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = card.isActive, onCheckedChange = { onToggleActive() })
            }
            Spacer(Modifier.height(FinTheme.spacing.md))
            Row(verticalAlignment = Alignment.Bottom) {
                AmountText(
                    amountCents = card.balanceCents,
                    sign = if (card.isCredit) AmountSign.NEGATIVE else AmountSign.NEUTRAL,
                    style = MaterialTheme.typography.headlineSmall,
                    animate = false,
                )
                Spacer(Modifier.width(FinTheme.spacing.sm))
                Text(
                    text = if (card.isCredit) "usado" else "disponible",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (card.isCredit && card.creditLimitCents != null) {
                Spacer(Modifier.height(FinTheme.spacing.sm))
                AnimatedProgressBar(
                    progress = card.usagePercent,
                    color = if (card.isOverAlertThreshold) FinTheme.extendedColors.expense else FinTheme.extendedColors.income,
                )
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Límite ${formatCents(card.creditLimitCents)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (card.cutOffDay != null && card.paymentDueDay != null) {
                        Text(
                            text = "Corte día ${card.cutOffDay} · Límite día ${card.paymentDueDay}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (card.isOverAlertThreshold) {
                    Spacer(Modifier.height(FinTheme.spacing.xs))
                    Text(
                        text = "Uso arriba del ${card.alertThresholdPercent}% de alerta",
                        style = MaterialTheme.typography.labelSmall,
                        color = FinTheme.extendedColors.expense,
                    )
                }
            }

            Spacer(Modifier.height(FinTheme.spacing.sm))
            Row {
                androidx.compose.material3.TextButton(onClick = onAdjustBalance) {
                    Text("Ajustar saldo")
                }
                androidx.compose.material3.TextButton(onClick = onEdit) {
                    Text("Editar")
                }
            }
        }
    }
}
