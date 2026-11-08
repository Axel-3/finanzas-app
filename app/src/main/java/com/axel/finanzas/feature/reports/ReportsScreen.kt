package com.axel.finanzas.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountSign
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.AnimatedProgressBar
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.SectionHeader
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
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
            verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.xl),
        ) {
            item(key = "month_summary") {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                text = "Gastos del mes",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AmountText(
                                amountCents = state.totalExpenseThisMonth,
                                sign = AmountSign.NEGATIVE,
                                style = MaterialTheme.typography.titleLarge,
                                animate = false,
                            )
                        }
                        Column {
                            Text(
                                text = "Ingresos del mes",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AmountText(
                                amountCents = state.totalIncomeThisMonth,
                                sign = AmountSign.POSITIVE,
                                style = MaterialTheme.typography.titleLarge,
                                animate = false,
                            )
                        }
                    }
                }
            }

            item(key = "monthly_header") { SectionHeader(title = "Últimos 6 meses") }
            item(key = "monthly_chart") {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MonthlyBarChart(data = state.monthlyTotals)
                        Spacer(Modifier.height(FinTheme.spacing.sm))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            state.monthlyTotals.forEach { month ->
                                Text(
                                    text = month.month.month.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "category_header") { SectionHeader(title = "Gasto por categoría este mes") }
            if (state.categorySlices.isEmpty()) {
                item(key = "no_categories") {
                    Text(
                        text = "Sin gastos categorizados este mes todavía",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val maxAmount = state.categorySlices.maxOf { it.amountCents }.coerceAtLeast(1)
                items(state.categorySlices) { slice ->
                    CategorySliceRow(slice = slice, fraction = slice.amountCents.toFloat() / maxAmount)
                }
            }

            item(key = "export") {
                Button(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.buildCsv()
                            shareCsv(context, csv)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exportar movimientos a CSV")
                }
            }
        }
    }
}

@Composable
private fun CategorySliceRow(slice: CategorySlice, fraction: Float) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = slice.name, style = MaterialTheme.typography.bodyLarge)
                AmountText(amountCents = slice.amountCents, style = MaterialTheme.typography.titleSmall, animate = false)
            }
            Spacer(Modifier.height(FinTheme.spacing.xs))
            AnimatedProgressBar(progress = fraction, color = runCatching { android.graphics.Color.parseColor(slice.colorHex) }
                .map { androidx.compose.ui.graphics.Color(it) }
                .getOrDefault(FinTheme.extendedColors.expense))
        }
    }
}
