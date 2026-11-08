package com.axel.finanzas.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountSign
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Próximos 30 días") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Regresar")
                    }
                },
            )
        },
    ) { innerPadding ->
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
            item(key = "projection") {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Saldo líquido hoy",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AmountText(amountCents = state.currentLiquidBalanceCents, style = MaterialTheme.typography.titleLarge, animate = false)
                        Text(
                            text = "Proyectado a 30 días",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AmountText(
                            amountCents = state.projectedBalanceCents,
                            sign = if (state.projectedBalanceCents < state.currentLiquidBalanceCents) AmountSign.NEGATIVE else AmountSign.POSITIVE,
                            style = MaterialTheme.typography.headlineSmall,
                            animate = false,
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ComingSoonScreen(title = "Nada programado en 30 días")
                    }
                }
            } else {
                items(state.items, key = { "${it.source}-${it.label}-${it.date}" }) { agendaItem ->
                    AgendaRow(agendaItem)
                }
            }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@Composable
private fun AgendaRow(item: AgendaItem) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = item.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = dateFormatter.format(item.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountText(
                amountCents = item.amountCents,
                sign = if (item.isIncome) AmountSign.POSITIVE else AmountSign.NEGATIVE,
                style = MaterialTheme.typography.titleMedium,
                animate = false,
            )
        }
    }
}
