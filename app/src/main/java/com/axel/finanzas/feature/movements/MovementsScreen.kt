package com.axel.finanzas.feature.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountSign
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.iconForKey
import com.axel.finanzas.domain.model.MovementUi
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(viewModel: MovementsViewModel = hiltViewModel()) {
    val movements by viewModel.movements.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Agregar movimiento")
            }
        },
    ) { innerPadding ->
        if (accounts.isEmpty()) {
            // Debería existir al menos "Efectivo" por el seeder; si no, evita
            // una pantalla rota mientras arranca.
            Box(modifier = Modifier.padding(innerPadding)) {
                ComingSoonScreen(title = "Preparando tus cuentas…")
            }
            return@Scaffold
        }

        if (movements.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Aún no tienes movimientos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(FinTheme.spacing.sm))
                    Text(
                        text = "Toca el + para agregar el primero",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
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
                items(movements, key = { it.id }) { movement ->
                    SwipeableMovementRow(
                        movement = movement,
                        onDelete = {
                            viewModel.deleteMovement(movement.id)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Movimiento eliminado",
                                    actionLabel = "Deshacer",
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDelete()
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditMovementSheet(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddSheet = false },
            onSaveExpenseOrIncome = { type, amountCents, accountId, categoryId, merchant, note, occurredAt ->
                viewModel.addExpenseOrIncome(type, amountCents, accountId, categoryId, merchant, note, occurredAt)
                showAddSheet = false
            },
            onSaveTransfer = { amountCents, fromId, toId, note, occurredAt ->
                viewModel.addTransfer(amountCents, fromId, toId, note, occurredAt)
                showAddSheet = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMovementRow(movement: MovementUi, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FinTheme.extendedColors.expenseContainer, MaterialTheme.shapes.large)
                    .padding(horizontal = FinTheme.spacing.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar",
                    tint = FinTheme.extendedColors.onExpenseContainer,
                )
            }
        },
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            MovementRow(movement)
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
private fun MovementRow(movement: MovementUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconForKey(movement.categoryIconKey ?: "misc"),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(FinTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movement.merchant?.takeIf { it.isNotBlank() }
                    ?: movement.categoryName
                    ?: if (movement.type == MovementType.TRANSFER) "Transferencia" else "Movimiento",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val dateLabel = timeFormatter.format(movement.occurredAt.atZone(ZoneId.systemDefault()))
            val subtitle = if (movement.type == MovementType.TRANSFER) {
                "${movement.accountName} → ${movement.linkedAccountName ?: "?"} · $dateLabel"
            } else {
                "${movement.categoryName ?: "Sin categoría"} · ${movement.accountName} · $dateLabel"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(
            amountCents = movement.amountCents,
            sign = when (movement.type) {
                MovementType.EXPENSE -> AmountSign.NEGATIVE
                MovementType.INCOME -> AmountSign.POSITIVE
                else -> AmountSign.NEUTRAL
            },
            style = MaterialTheme.typography.titleMedium,
            animate = false,
        )
    }
}
