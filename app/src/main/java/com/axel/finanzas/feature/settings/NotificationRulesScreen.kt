package com.axel.finanzas.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.database.entity.NotificationRuleKind
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.formatCents
import com.axel.finanzas.core.notifications.NotificationSimulator
import android.content.Intent
import android.provider.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationRulesScreen(onBack: () -> Unit, viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val monitored by viewModel.monitoredPackages.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val context = LocalContext.current
    val granted = viewModel.isNotificationAccessGranted()

    val bankOptions = remember(rules) { rules.map { it.packageName to it.bankName }.distinct() }
    var selectedBank by remember(bankOptions) { mutableStateOf(bankOptions.firstOrNull()) }
    var testText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reglas de notificaciones") },
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
            item(key = "permission") {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = if (granted) "Acceso a notificaciones activo" else "Falta activar el acceso a notificaciones",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(FinTheme.spacing.xs))
                        Text(
                            text = "Android exige darlo desde Ajustes del sistema, no se puede desde la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!granted) {
                            Spacer(Modifier.height(FinTheme.spacing.sm))
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }) {
                                Text("Abrir ajustes")
                            }
                        }
                    }
                }
            }

            item(key = "apps_header") {
                Text(text = "Apps a escuchar", style = MaterialTheme.typography.titleSmall)
            }
            items(installedApps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = app.packageName in monitored,
                        onCheckedChange = { checked -> viewModel.toggleMonitored(app.packageName, checked) },
                    )
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.widthIn(max = 260.dp),
                    )
                }
            }

            item(key = "test_header") {
                Text(text = "Probar con una notificación real", style = MaterialTheme.typography.titleSmall)
            }
            item(key = "test_card") {
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Pega el texto de una notificación real y elige de qué banco es. " +
                                "No se guarda nada, solo muestra qué haría la app si fuera real.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(FinTheme.spacing.sm))

                        if (bankOptions.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = selectedBank?.second ?: "Selecciona",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Banco") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    bankOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.second) },
                                            onClick = {
                                                selectedBank = option
                                                expanded = false
                                            },
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(FinTheme.spacing.sm))
                        }

                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            label = { Text("Texto de la notificación") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        Spacer(Modifier.height(FinTheme.spacing.sm))
                        Button(
                            onClick = {
                                selectedBank?.let { (packageName, _) -> viewModel.testNotification(packageName, testText) }
                            },
                            enabled = selectedBank != null && testText.isNotBlank(),
                        ) {
                            Text("Probar")
                        }

                        testResult?.let { outcome ->
                            Spacer(Modifier.height(FinTheme.spacing.sm))
                            TestResultView(outcome)
                        }
                    }
                }
            }

            item(key = "rules_header") {
                Text(text = "Reglas por banco (${rules.size})", style = MaterialTheme.typography.titleSmall)
            }
            if (rules.isEmpty()) {
                item(key = "no_rules") {
                    Text(
                        text = "Todavía no hay ninguna regla. Se agregan en cuanto haya " +
                            "texto real de las notificaciones de tus bancos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(rules, key = { it.id }) { rule ->
                FinCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = rule.bankName, style = MaterialTheme.typography.titleSmall)
                            if (rule.isExample) {
                                Text(
                                    text = "ejemplo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FinTheme.extendedColors.expense,
                                )
                            }
                        }
                        Text(
                            text = rule.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Monto: ${rule.amountRegex}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(FinTheme.spacing.xs))
                        TextButton(onClick = { viewModel.deleteRule(rule) }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
}

private fun kindLabel(kind: NotificationRuleKind): String = when (kind) {
    NotificationRuleKind.CARGO -> "gasto"
    NotificationRuleKind.DEPOSITO -> "depósito"
    NotificationRuleKind.PAGO_TARJETA -> "pago de tarjeta"
    NotificationRuleKind.DEVOLUCION -> "devolución"
}

@Composable
private fun TestResultView(outcome: NotificationSimulator.Outcome) {
    when (outcome) {
        is NotificationSimulator.Outcome.SinRegla -> {
            Text(
                text = "Ninguna de las ${outcome.reglasProbadas} reglas activas de ese banco " +
                    "interpretó este texto. Así se vería en la vida real: caería a \"Por revisar\" " +
                    "sin monto sugerido, porque ni siquiera se extrajo el monto.",
                style = MaterialTheme.typography.bodySmall,
                color = FinTheme.extendedColors.expense,
            )
        }
        is NotificationSimulator.Outcome.PorRevisar -> {
            Column {
                Text(
                    text = "${outcome.bankName}: se interpretó como ${kindLabel(outcome.kind)} de " +
                        "${formatCents(outcome.amountCents)}, pero caería a \"Por revisar\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Razón: ${outcome.razon}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                outcome.merchant?.let {
                    Text(
                        text = "Comercio/detalle extraído: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        is NotificationSimulator.Outcome.SeAsignariaSolo -> {
            Text(
                text = "${outcome.bankName}: se asignaría solo como ${kindLabel(outcome.kind)} de " +
                    "${formatCents(outcome.amountCents)} en \"${outcome.accountName}\", sin pasar por " +
                    "\"Por revisar\".",
                style = MaterialTheme.typography.bodyMedium,
                color = FinTheme.extendedColors.income,
            )
        }
    }
}
