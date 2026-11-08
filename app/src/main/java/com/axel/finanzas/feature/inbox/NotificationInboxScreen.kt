package com.axel.finanzas.feature.inbox

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.database.entity.CategoryType
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.database.entity.NotificationInboxEntity
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AccountDropdown
import com.axel.finanzas.core.designsystem.components.CategoryDropdown
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.formatCents
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(onBack: () -> Unit, viewModel: InboxViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var resolving by remember { mutableStateOf<NotificationInboxEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Por revisar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Regresar")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (pending.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                ComingSoonScreen(title = "No hay nada por revisar")
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
            items(pending, key = { it.id }) { entry ->
                InboxEntryRow(
                    entry = entry,
                    onResolve = { resolving = entry },
                    onDiscard = { viewModel.discard(entry) },
                )
            }
        }
    }

    resolving?.let { entry ->
        ResolveInboxEntrySheet(
            entry = entry,
            accounts = accounts,
            categories = categories,
            onDismiss = { resolving = null },
            onConfirm = { accountId, categoryId, type, amountCents, merchant ->
                viewModel.resolve(entry, accountId, categoryId, type, amountCents, merchant)
                resolving = null
            },
        )
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
private fun InboxEntryRow(entry: NotificationInboxEntity, onResolve: () -> Unit, onDiscard: () -> Unit) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = dateFormatter.format(entry.postedAt.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FinTheme.spacing.xs))
            Text(text = entry.rawText, style = MaterialTheme.typography.bodyMedium)
            entry.suggestedAmountCents?.let {
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Text(
                    text = "Monto detectado: ${formatCents(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(FinTheme.spacing.sm))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDiscard) { Text("Descartar") }
                Spacer(Modifier.width(FinTheme.spacing.sm))
                Button(onClick = onResolve) { Text("Clasificar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolveInboxEntrySheet(
    entry: NotificationInboxEntity,
    accounts: List<com.axel.finanzas.core.database.entity.AccountEntity>,
    categories: List<com.axel.finanzas.core.database.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, categoryId: Long?, type: MovementType, amountCents: Long, merchant: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var type by remember { mutableStateOf(MovementType.EXPENSE) }
    var accountId by remember { mutableStateOf(entry.suggestedAccountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var amountText by remember {
        mutableStateOf(entry.suggestedAmountCents?.let { (it / 100.0).toString() } ?: "")
    }
    var merchant by remember { mutableStateOf("") }

    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val relevantCategories = categories.filter {
        it.type == if (type == MovementType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    }
    val canSave = amountCents > 0 && accountId != null && categoryId != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinTheme.spacing.lg)
                .padding(bottom = FinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
        ) {
            Text(text = "Clasificar notificación", style = MaterialTheme.typography.titleLarge)
            Text(
                text = entry.rawText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            AccountDropdown(
                label = "Cuenta",
                accounts = accounts,
                selectedId = accountId,
                onSelected = { accountId = it },
            )
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

            Button(
                onClick = { onConfirm(accountId!!, categoryId, type, amountCents, merchant.ifBlank { null }) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Crear movimiento")
            }
        }
    }
}
